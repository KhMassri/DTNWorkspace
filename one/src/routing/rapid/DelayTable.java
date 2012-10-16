/* 
 * Copyright 2010 Institute of Telematics, Karlsruhe Institute of Technology (KIT)
 * Released under GPLv3. See LICENSE.txt for details. 
 * 
 * Christoph P. Mayer - mayer[at]kit[dot]edu
 * Wolfgang Heetfeld
 *
 * Version 0.1 - released 13. Oct 2010
 */
package routing.rapid;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import routing.Rapid;


import core.Connection;
import core.DTNHost;
import core.SimClock;

/**
 * Class for storing and manipulating delay entries for the RAPID
 * router module.
 */
public class DelayTable {
	
	private int transferCounter;
	private double avgTransferOpportunity;
	private int MATRIX_DIMENSION = 0;
	//The host of this delay table
	private DTNHost host;
	//Delay entries for a specified message id
	private HashMap<String, DelayEntry> delayTable;
	//Meeting entry for meeting of two hosts (DTNHosts)
	private MeetingEntry[][] meetingEntry;
	/** IDs of the messages that are known to have reached the final destination */
	private Set<String> ackedMessageIds;
	
	/**
	 * Constructor. Creates a delay table with a specified matrix dimension
	 * for the meeting time matrix
	 */
	public DelayTable(DTNHost host, int matrixDim) {
		init(host, matrixDim);
	}
	
	/**
	 * Constructor. Creates a delay table
	 */
	public DelayTable(DTNHost host) {
		init(host, 5);
	}
	
	private void init(DTNHost host, int matrixDim) {
		this.host = host;
		int transmitSpeed = host.getInterfaces().get(0).getTransmitSpeed();
		double transmitRange = host.getInterfaces().get(0).getTransmitRange();
		double movementSpeed = host.getMovementModel().getAvgSpeed();
		double transmitTime = movementSpeed / transmitRange;
		
		this.MATRIX_DIMENSION = 0;
		delayTable = new HashMap<String, DelayEntry>();
		meetingEntry = new MeetingEntry[MATRIX_DIMENSION][MATRIX_DIMENSION];
		recomputeMatrix(matrixDim);
		
		// initialize variables with default values 
		transferCounter = 1;
		
		//assert the initial average transfer opportunity is not equals to null  
		if ((transmitTime==0) || (transmitSpeed==0)) avgTransferOpportunity = 1;
		else avgTransferOpportunity = (int) (transmitTime * transmitSpeed);
		ackedMessageIds = new HashSet<String>();
	}
	
	/**
	 * Adding a new entry into the delay table
	 * @param entry The delay entry
	 */
	public void addEntry(DelayEntry entry) {
		delayTable.put(entry.getMessage().getId(), entry);
	}
	
	/**
	 * Removing the specified entry from the delay table
	 * @param entry The delay entry for removing
	 */
	public void removeEntry(DelayEntry entry) {
		removeEntry(entry.getMessage().getId());
	}
	
	/**
	 * Removing the entry for the specified message from 
	 * the delay table
	 * @param id The id of the Message
	 * @return The success of the removal
	 */
	public boolean removeEntry(String id) {
		return (delayTable.remove(id)!=null);
	}
	
	/**
	 * Returns the delay entry of the specified message or
	 * null if no entry exists
	 * @param id The id of the message
	 * @return The delay entry
	 */
	public DelayEntry getDelayEntryByMessageId(String id) {
		return delayTable.get(id);
	}
	
	/**
	 * Returns the delay entries of this delay table 
	 * @return the current delay entries
	 */
	public Set<Entry<String, DelayEntry>> getDelayEntries() {
		return delayTable.entrySet();
	} 
	
	/**
	 * Creates a dummy entry for the actual meeting. This entry will be overwritten 
	 * at the end of the meeting.
	 * @param con The current connection
	 */
	public void dummyUpdateConnection(Connection con) {
		final double DUMMY_AVG_MEETING_TIME = 1.0;
		updateAvgMeetingTime(host.getAddress(), con.getOtherNode(host).getAddress(), DUMMY_AVG_MEETING_TIME, SimClock.getTime(), true);
		
	}
	
	/**
	 * Updates the average transfer opportunity and the average meeting time
	 * @param con The current connection
	 * @param meetingTime The meeting time of the hosts
	 */
	public void updateConnection(Connection con, double meetingTime) {
		updateAvgTransferOpportunity(con.getTotalBytesTransferred());
		updateAvgMeetingTime(host.getAddress(), con.getOtherNode(host).getAddress(), meetingTime);
	
		transferCounter++;		
	}
	
	/**
	 * Updates the average meeting time
	 * @param from The index of the first DTNHost
	 * @param to The index of the second DTNHost
	 * @param meetingTime The current meeting time
	 */
	public void updateAvgMeetingTime(int from, int to, double meetingTime) {
		updateAvgMeetingTime(from, to, meetingTime, SimClock.getTime());
	}
		
	/**
	 * Sets the average meeting time
	 * @param from The index of the first DTNHost
	 * @param to The index of the second DTNHost
	 * @param meetingTime The current meeting time
	 * @param timestamp The time stamp of this update
	 * @param weight The weight of this update
	 */
	public void setAvgMeetingTime(int from, int to, double meetingTime, double timestamp, int weight) {
		//recompute matrix if needed
		if ((from >= MATRIX_DIMENSION) || (to >= MATRIX_DIMENSION)) recomputeMatrix(Math.max(from, to)+1);
		
		//create or update entry [from][to]
		if (meetingEntry[from][to] == null) meetingEntry[from][to] = new MeetingEntry(meetingTime, timestamp);
		else meetingEntry[from][to].set(meetingTime, timestamp, weight);
		
		//create or update entry [to][from]
		if (meetingEntry[to][from] == null) meetingEntry[to][from] = new MeetingEntry(meetingTime, timestamp);
		else meetingEntry[to][from].set(meetingTime, timestamp, weight);
	}
	
	public void updateAvgMeetingTime(int from, int to, double meetingTime, double timestamp) {
		updateAvgMeetingTime(from, to, meetingTime, timestamp, false);
	}
	
	/**
	 * Updates the average meeting time
	 * @param from The index of the first DTNHost
	 * @param to The index of the second DTNHost
	 * @param meetingTime The current meeting time
	 * @param timestamp The time stamp of this update
	 * @param dummy Entry is only a dummy entry
	 */
	public void updateAvgMeetingTime(int from, int to, double meetingTime, double timestamp, boolean dummy) {
		//recompute matrix if needed
		if ((from >= MATRIX_DIMENSION) || (to >= MATRIX_DIMENSION)) recomputeMatrix(Math.max(from, to)+1);
		
		if (dummy) {
			//only create the dummy entry if no entry exists for this meeting
			if (meetingEntry[from][to] != null) return;
			assert (meetingEntry[to][from] == null); 
			
			//create dummy entry [from][to] 
			meetingEntry[from][to] = new MeetingEntry(meetingTime, timestamp, 0, true);
			
			//create dummy entry [to][from]
			meetingEntry[to][from] = new MeetingEntry(meetingTime, timestamp, 0, true);
		}
		else {
			//create or update entry [from][to]
			if ((meetingEntry[from][to] == null) || (meetingEntry[from][to].isDummy())) meetingEntry[from][to] = new MeetingEntry(meetingTime, timestamp);
			else meetingEntry[from][to].update(meetingTime, timestamp);
			
			//create or update entry [to][from]
			if ((meetingEntry[to][from] == null) || (meetingEntry[to][from].isDummy())) meetingEntry[to][from] = new MeetingEntry(meetingTime, timestamp);
			else meetingEntry[to][from].update(meetingTime, timestamp);
		}
	}
	
	public int getMeetingMatrixDimension() {
		return MATRIX_DIMENSION;
	}
	
	public Map<DTNHost, Double> getMeetingMap(int from) {
		HashMap<DTNHost, Double> map = new HashMap<DTNHost, Double>();
		Rapid r = ((Rapid) host.getRouter());
		
		for (int i = 0; i < MATRIX_DIMENSION; i++) {
			MeetingEntry entry = getIndirectMeetingEntry(from, i);
			if (entry != null) {
				assert(r.getHostMapping().containsKey(i));
				DTNHost host = r.getHostMapping().get(i);
				map.put(host, r.getMeetingProb(host));  
			}
		}
		
		return map;
	}
	
	public int getMeetingCount(int from) {
		int count = 0;
		
		for (int i = 0; i < MATRIX_DIMENSION; i++) {
			MeetingEntry entry = getMeetingEntry(from, i);
			if (entry != null) count = count + entry.getWeight(); 
		}
		
		return count;
	}
	
	public MeetingEntry getMeetingEntry(int from, int to) {
		if ((from >= MATRIX_DIMENSION) || (to >= MATRIX_DIMENSION)) return null;
		return meetingEntry[from][to];
	}
		
	public MeetingEntry getIndirectMeetingEntry(int from, int to) {
		MeetingEntry entry = getMeetingEntry(from, to);
		
		if (entry != null) return entry;
		//find an indirect meeting entry (3 hops) if one exists
		else {
			MeetingEntry maxEntry = new MeetingEntry(-1, 0);
			MeetingEntry entry0;
			MeetingEntry entry1;
			MeetingEntry entry2;
			for (int i=0; i<MATRIX_DIMENSION; i++) {
				if (hasMet(from, i) /*|| hasMeeting(from, i)*/) {
					//find 2 hop neighbor
					if (hasMet(i, to) /*|| hasMeeting(i, to)*/) {
						entry0 = getMeetingEntry(from, i);
						entry1 = getMeetingEntry(i, to);
						entry = createMeetingEntry(entry0, entry1);
						if ((entry.getAvgMeetingTime()*entry.getWeight()) > (maxEntry.getAvgMeetingTime()*maxEntry.getWeight())) {
							maxEntry = new MeetingEntry(entry);
						}						
					}
					//find 3 hop neighbor
					else {
						for (int k=0; k<MATRIX_DIMENSION; k++) {
							if (hasMet(i, k) /*|| hasMeeting(i, k)*/) {
								if (hasMet(k, to) /*|| hasMeeting(k, to)*/) {
									entry0 = getMeetingEntry(from, i);
									entry1 = getMeetingEntry(i, k);
									entry2 = getMeetingEntry(k, to);
									entry = createMeetingEntry(entry0, entry1, entry2);
									if ((entry.getAvgMeetingTime()*entry.getWeight()) > (maxEntry.getAvgMeetingTime()*maxEntry.getWeight())) {
										maxEntry = new MeetingEntry(entry);
									}	
								}
							}
						}
					}
				}
			}
			if ((maxEntry.getAvgMeetingTime()*maxEntry.getWeight()) > -1) return maxEntry;
		}
		
		return null; 
	}
	
//	private MeetingEntry getDummyMeetingEntry(int from, int to) {
//		//if the nodes with address "from" and "to" are actually meeting for the first
//		//time the entry doesn't exists yet (will be created at disconnecting) -> return 
//		//dummy entry.
//		final double DUMMY_AVG_MEETING_TIME = 1.0;		
//		MeetingEntry entry;
//		
//		if ((from>=0) && (from<MATRIX_DIMENSION) && (to>=0) && (to<MATRIX_DIMENSION)) {
//			entry = meetingEntry[from][to];
//			if (entry != null) return entry;
//		} 
//			
//		return (new MeetingEntry(DUMMY_AVG_MEETING_TIME));
//	}
	
	private boolean hasMet(int from, int to) {
		if ((from >= MATRIX_DIMENSION) || (to >= MATRIX_DIMENSION)) return false;
		return (meetingEntry[from][to]!=null);
	}
	
//	private boolean hasMeeting(int from, int to) {
//		DTNHost dtn_from = ((RapidRouter) host.getRouter()).getHostMapping().get(from);
//		if (dtn_from == null) return false;
//
//		for (Connection con : dtn_from.getConnections()) {
//			if (con.getOtherNode(dtn_from).getAddress() == to) return true;
//		}
//		return false;
//	}
	
	private double getMinimum(double[] values) {
		if ((values == null) || (values.length == 0)) return -1;
		double min = values[0];
		
		for (int i = 1; i < values.length; i++) {
			min = Math.min(min, values[i]);
		}
		
		return min;
	}
	
	private int getMinimum(int[] values) {
		if ((values == null) || (values.length == 0)) return -1;
		int min = values[0];
		
		for (int i = 1; i < values.length; i++) {
			min = Math.min(min, values[i]);
		}
		
		return min;
	}
	
	private MeetingEntry createMeetingEntry(MeetingEntry ... entries) {
		if ((entries == null) || (entries.length == 0)) return null;
		double[] meetingTimes = new double[entries.length];
		double[] meetingUpdates = new double[entries.length];;
		int[] meetingWeights = new int[entries.length];;
		
		for (int i = 0; i < entries.length; i++) {
			meetingTimes[i] = entries[i].getAvgMeetingTime();
			meetingUpdates[i] = entries[i].getLastUpdate();
			meetingWeights[i] = entries[i].getWeight();
		}
		
		return (new MeetingEntry(getMinimum(meetingTimes), getMinimum(meetingUpdates), getMinimum(meetingWeights), false));
	}
	
	/**
	 * Recompute the new dimension of the matrix (the new dimension has to 
	 * be lager than the old one) and copies old entries. All new entries 
	 * were initialized with 'null'. 
	 * @param dimension The new size of the matrix
	 */
	private void recomputeMatrix(int dimension) {
		assert (dimension >= MATRIX_DIMENSION);
		//create matrix of new dimension 
		MeetingEntry[][] entry = new MeetingEntry[dimension][dimension];
		
		//copy old entries into new matrix
		for (int i = 0; i < MATRIX_DIMENSION; i++) {
			for (int k = 0; k < MATRIX_DIMENSION; k++) { 
				entry[i][k] = meetingEntry[i][k];
			}
		}
		
		//initialize new entries with 'null'
		for (int i = MATRIX_DIMENSION; i < dimension; i++) {
			for (int k = MATRIX_DIMENSION; k < dimension; k++) {
				entry[i][k] = null;
			}
		}
		
		MATRIX_DIMENSION = dimension;
		meetingEntry = entry;
	}
	
	/**
	 * Updates the average transfer opportunity by recomputing it with the
	 * current transfer opportunity of the node   
	 * @param transferOpportunity The current transfer opportunity
	 */
	private void updateAvgTransferOpportunity(int transferOpportunity) {
		this.avgTransferOpportunity = (((transferCounter * this.avgTransferOpportunity) + transferOpportunity) / (transferCounter + 1));
	}
	
	/**
	 * Returns the average transfer opportunity of this nodes delay table 
	 * @return the current transfer opportunity 
	 */
	public double getAvgTransferOpportunity() {
		return Math.max(avgTransferOpportunity, 0.0000001);
	}
	
	public void addAllAckedMessageIds(Collection<String> ackedMessageIds) {
		this.ackedMessageIds.addAll(ackedMessageIds);
	}
	
	public Collection<String> getAllAckedMessageIds() {
		return ackedMessageIds;
	}
	
	public void addAckedMessageIds(String ackedMessageId) {
		this.ackedMessageIds.add(ackedMessageId);
	}

	public void removeAllAckedMessageIds(Collection<String> ackedMessageIds) {
		this.ackedMessageIds.removeAll(ackedMessageIds);
	}
	
	public void setChanged(boolean changed) {
		for (Entry<String, DelayEntry> entry : getDelayEntries()) {
			entry.getValue().setChanged(changed);
		}
	}
	
	public boolean delayHasChanged(String id) {
		DelayEntry entry = getDelayEntryByMessageId(id);
		if (entry == null) return true;
		return entry.hasChanged();
	}
	
	/**
	 * Print for every message this host contains the delay entries to the command line
	 */
	public void printDelays() {
		System.out.println("delay table of DTNHost: "+host);
		System.out.println("Message\t\t\tDTNHost\t\t\tdelay value\t\t\tlast update time");
		for (Entry<String, DelayEntry> entry : getDelayEntries()) {
			entry.getValue().print();
		}
	}
	
	/**
	 * Print for every host this host knows the meeting entries to the command line
	 */
	public void printMeetings() {
		System.out.println("meeting table of DTNHost: "+host);
		System.out.println("DTNHost Address\t\t\ttimestamp\t\t\taverage meeting time");
		int from = host.getAddress();
		for (int i = 0; i < getMeetingMatrixDimension(); i++) {
			if (from != i) {
				if (getMeetingEntry(from, i) !=null) {
					System.out.print(i+"\t\t\t\t");
					getMeetingEntry(from, i).print();
				}
				else System.out.println(i+"\t\t\t\tnull\t\t\t\tnull");
			}
			
		}
	}
	
	/**
	 * Print all acked message ids of this host to the command line
	 */
	public void printAckedMessageIds() {
		System.out.println("acked message ids of DTNHost: "+host);
		System.out.println("acked message ids");
		for (String msgIds : getAllAckedMessageIds()) {
			System.out.println(msgIds);
		}
	}
}
