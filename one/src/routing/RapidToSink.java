/* 
 * Copyright 2010 Institute of Telematics, Karlsruhe Institute of Technology (KIT)
 * Released under GPLv3. See LICENSE.txt for details. 
 * 
 * Christoph P. Mayer - mayer[at]kit[dot]edu
 * Wolfgang Heetfeld
 *
 * Version 0.1 - released 13. Oct 2010
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import routing.rapid.DelayEntry;
import routing.rapid.DelayTable;
import routing.rapid.MeetingEntry;

import core.*;

/**
 * RAPID router
 */
public class RapidToSink extends DTNRouter {
	// timestamp for meeting a host in seconds
	private double timestamp;
	// delay table which contains meta data
	private DelayTable delayTable;
	// utility algorithm (minimizing average delay | minimizing missed deadlines |
	// minimizing maximum delay)
	private Map<Integer,DTNHost> hostMapping;	
	
	private final UtilityAlgorithm ALGORITHM = UtilityAlgorithm.AVERAGE_DELAY;
	private static final double INFINITY = 99999;
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public RapidToSink(Settings s) {
		super(s);
		
		delayTable = null;
		timestamp = 0.0;
		hostMapping = new HashMap<Integer, DTNHost>();
	}
	
	@Override
	public void init(DTNHost host, List<MessageListener> mListeners) {
		super.init(host, mListeners);
		
		delayTable = new DelayTable(super.getHost());
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected RapidToSink(RapidToSink r) {
		super(r);
		
		delayTable = r.delayTable;
		timestamp = r.timestamp;
		hostMapping = r.hostMapping;
	}
	
	@Override
	public void changedConnection(Connection con) {	
		DTNHost other=con.getOtherNode(getHost());
		
		if(other.getRouter().hello().equals("GeneratorRouter"))
			return;
		
		if (con.isUp()) {	/* new connection */
			//simulate control channel on connection up without sending any data
			timestamp = SimClock.getTime();	
			
			if (!other.isSink()) {
				//synchronize all delay table entries
				synchronizeDelayTables(con);
				
				//synchronize all meeting time entries 
				synchronizeMeetingTimes(con);
				
				updateDelayTableStat(con); 
				
				//synchronize acked message ids
				synchronizeAckedMessageIDs(con);
				
				deleteAckedMessages();
				
				//map DTNHost to their address
				doHostMapping(con);
				
				delayTable.dummyUpdateConnection(con);
			}
			else{
				synchronizeAckedMessageIDs(con);
				deleteAckedMessages();
				delayTable.dummyUpdateConnection(con);
				
			}
		}
		else {	/* connection went down */			
			//update connection
			double time = SimClock.getTime()-timestamp;
			
			delayTable.updateConnection(con, time);
			
			//synchronize acked message ids
			synchronizeAckedMessageIDs(con);
			
			//update set of messages that are known to have reached the destination 
			deleteAckedMessages();
			updateAckedMessageIds();
			
			//synchronize all delay table entries
			if(!other.isSink())
				synchronizeDelayTables(con);
		}
			
	}
	
	
	private void doHostMapping(Connection con) {
		DTNHost host = getHost();
		DTNHost otherHost = con.getOtherNode(host);
		RapidToSink otherRouter = ((RapidToSink) otherHost.getRouter());
		
		// propagate host <-> address mapping
		hostMapping.put(host.getAddress(), host);
		hostMapping.put(otherHost.getAddress(), otherHost);
		hostMapping.putAll(otherRouter.hostMapping);
		otherRouter.hostMapping.putAll(hostMapping);
	}
	
	private void updateDelayTableStat(Connection con) {
		DTNHost otherHost = con.getOtherNode(getHost());
		RapidToSink otherRouter = ((RapidToSink) otherHost.getRouter());
		int from = otherHost.getAddress();
		
		MeetingEntry entry = otherRouter.getDelayTable().getMeetingEntry(from, 0);
		if(entry == null)return;
		for (Message m : getMessageCollection())
			delayTable.getDelayEntryByMessageId(m.getId()).setChanged(true);
	}
	
	private void synchronizeDelayTables(Connection con) {
		DTNHost otherHost = con.getOtherNode(getHost());
		RapidToSink otherRouter = (RapidToSink) otherHost.getRouter();
		DelayEntry delayEntry = null;
		DelayEntry otherDelayEntry = null;
		
		//synchronize all delay table entries
		for (Entry<String, DelayEntry> entry1 : delayTable.getDelayEntries()) {
			Message m = entry1.getValue().getMessage();
			delayEntry = delayTable.getDelayEntryByMessageId(m.getId());
			assert(delayEntry != null);
			otherDelayEntry = otherRouter.delayTable.getDelayEntryByMessageId(m.getId());
			
			if (delayEntry.getDelays() == null) continue;
			//for all hosts check delay entries and create new if they doesn't exist
			//Entry<DTNHost host, Tuple<Double delay, Double lastUpdate>>
			for (Entry<DTNHost, Tuple<Double, Double>> entry : delayEntry.getDelays()) {
				DTNHost myHost = entry.getKey();
				Double myDelay = entry.getValue().getKey();
				Double myTime = entry.getValue().getValue();

				//create a new host entry if host entry at other host doesn't exist
				if ((otherDelayEntry == null) || (!otherDelayEntry.contains(myHost))) {
					//parameters: 
					//m The message 
					//myHost The host which contains a copy of this message
					//myDelay The estimated delay
					//myTime The entry was last changed
					otherRouter.updateDelayTableEntry(m, myHost, myDelay, myTime);
				}
				else {
					//check last update time of other hosts entry and update it 
					if (otherDelayEntry.isOlderThan(myHost, delayEntry.getLastUpdate(myHost))) {
						//parameters: 
						//m The message 
						//myHost The host which contains a copy of this message
						//myDelay The estimated delay
						//myTime The entry was last changed 
						
						otherRouter.updateDelayTableEntry(m, myHost, myDelay, myTime);
					}
					
					if ((otherDelayEntry.isAsOldAs(myHost, delayEntry.getLastUpdate(myHost))) && (delayEntry.getDelayOf(myHost) > otherDelayEntry.getDelayOf(myHost))) {
						//parameters: 
						//m The message 
						//myHost The host which contains a copy of this message
						//myDelay The estimated delay
						//myTime The entry was last changed 
						
						otherRouter.updateDelayTableEntry(m, myHost, myDelay, myTime);
					}
				}
			}
		}
	}
	
	private void synchronizeMeetingTimes(Connection con) {
		DTNHost otherHost = con.getOtherNode(getHost());
		RapidToSink otherRouter = (RapidToSink) otherHost.getRouter();
		MeetingEntry meetingEntry = null;
		MeetingEntry otherMeetingEntry = null;
		
		//synchronize all meeting time entries
		for (int i = 0; i < delayTable.getMeetingMatrixDimension(); i++) {
			for (int k = 0; k < delayTable.getMeetingMatrixDimension(); k++) {
				meetingEntry = delayTable.getMeetingEntry(i, k);
				
				if (meetingEntry != null) {
					otherMeetingEntry = otherRouter.delayTable.getMeetingEntry(i, k);
					//create a new meeting entry if meeting entry at other host doesn't exist
					if (otherMeetingEntry == null){
						otherRouter.delayTable.setAvgMeetingTime(i, k, meetingEntry.getAvgMeetingTime(), meetingEntry.getLastUpdate(), meetingEntry.getWeight());
					}
					else {
						//check last update time of other hosts entry and update it 
						if (otherMeetingEntry.isOlderThan(meetingEntry.getLastUpdate())) {
							otherRouter.delayTable.setAvgMeetingTime(i, k, meetingEntry.getAvgMeetingTime(), meetingEntry.getLastUpdate(), meetingEntry.getWeight());
						}
						
						if ((otherMeetingEntry.isAsOldAs(meetingEntry.getLastUpdate())) && (meetingEntry.getAvgMeetingTime() > otherMeetingEntry.getAvgMeetingTime())) {							
							otherRouter.delayTable.setAvgMeetingTime(i, k, meetingEntry.getAvgMeetingTime(), meetingEntry.getLastUpdate(), meetingEntry.getWeight());
						}
					}
				}
			}
		}
	}
	
	private void synchronizeAckedMessageIDs(Connection con) {
		DTNHost otherHost = con.getOtherNode(getHost());
		
		if(otherHost.isSink()){
			delayTable.addAllAckedMessageIds(((SinkRouter)otherHost.getRouter()).getSinkedMessages());
			return;
		}
		
		RapidToSink otherRouter = (RapidToSink) otherHost.getRouter();
		
		delayTable.addAllAckedMessageIds(otherRouter.delayTable.getAllAckedMessageIds());
		otherRouter.delayTable.addAllAckedMessageIds(delayTable.getAllAckedMessageIds());
		
		assert(delayTable.getAllAckedMessageIds().equals(otherRouter.delayTable.getAllAckedMessageIds()));
	}
	
	/**
	 * Deletes the messages from the message buffer that are known to be ACKed and also delete the 
	 * message entries of the ACKed messages in the delay table 
	 */
	private void deleteAckedMessages() {
		for (String id : delayTable.getAllAckedMessageIds()) {
			if (this.hasMessage(id) && !isSending(id)) {
				//delete messages from the message buffer
				this.deleteMessage(id, false);	
			}
			
			//delete messages from the delay table
			if (delayTable.getDelayEntryByMessageId(id) != null) assert(delayTable.removeEntry(id)==true);
		}
	}
	
	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);
		
		/* was this node the final recipient of the message? */
		if (isDeliveredMessage(m)) {
			delayTable.addAckedMessageIds(id);
		}
		
		return m;
	}
	
	/**
	 * Delete old message ids from the acked message id set which time to live was passed 
	 */
	private void updateAckedMessageIds() {
		ArrayList<String> removableIds=new ArrayList<String>();
		
		for (String id : delayTable.getAllAckedMessageIds()) {
			Message m = this.getMessage(id);
			if ((m != null) && (m.getTtl() <= 0)) removableIds.add(id);
		}
		
		if (removableIds.size() > 0) delayTable.removeAllAckedMessageIds(removableIds);
	}
	
	/**
	 * Updates an delay table entry for given message and host
	 * @param m The message 
	 * @param host The host which contains a copy of this message
	 * @param delay The estimated delay
	 * @param time The entry was last changed
	 */
	public void updateDelayTableEntry(Message m, DTNHost host, double delay, double time) {
		DelayEntry delayEntry;
		
		if ((delayEntry = delayTable.getDelayEntryByMessageId(m.getId())) == null) {
			delayEntry = new DelayEntry(m);
			delayTable.addEntry(delayEntry);
		}
		assert((delayEntry != null) && (delayTable.getDelayEntryByMessageId(m.getId()) != null));
		
		if (delayEntry.contains(host)) {
			delayEntry.setHostDelay(host, delay, time);	
		}
		else {
			delayEntry.addHostDelay(host, delay, time);
		}
	}
	
	@Override 
	public boolean createNewMessage(Message m) {
		boolean stat = super.createNewMessage(m);
		
		//if message was created successfully add the according delay table entry
		if (stat) {
			updateDelayTableEntry(m, getHost(), estimateDelay(m, getHost(), true), SimClock.getTime());			
		}
		
		return stat;
	}
	
	@Override
	public int receiveMessage(Message m, DTNHost from) {
		int stat = super.receiveMessage(m, from);

		//if message was received successfully add the according delay table entry
		if (stat == 0) {
			
			DTNHost host = getHost();
			double time = SimClock.getTime();
			double delay = estimateDelay(m, host, true);
			updateDelayTableEntry(m, host, delay, time);
			((RapidToSink) from.getRouter()).updateDelayTableEntry(m, host, delay, time);
		}
		
		return stat;
	}
	
	/**
	 * Returns the current marginal utility (MU) value for a connection of two
	 * nodes
	 * @param msg One message of the message queue
	 * @param con The connection
	 * @param host The host which contains a copy of this message
	 * @return the current MU value
	 */
	private double getMarginalUtility(Message msg, Connection con, DTNHost host) {
		final RapidToSink otherRouter = (RapidToSink) (con.getOtherNode(host).getRouter());
		
		return getMarginalUtility(msg, otherRouter, host);
	}
	
	/**
	 * Returns the current marginal utility (MU) value for a host of a specified
	 * router
	 * @param msg One message of the message queue
	 * @param router The router which the message could be send to
	 * @param host The host which contains a copy of this message
	 * @return the current MU value
	 */
	private double getMarginalUtility(Message msg, RapidToSink router, DTNHost host) {
		double marginalUtility = 0.0;	 
		double utility = 0.0;			// U(i): The utility of the message (packet) i
		double utilityOld = 0.0;  
		
		assert(this!=router);
		utility = router.computeUtility(msg, host, true);
		utilityOld = this.computeUtility(msg, host, true);
		
		// s(i): The size of message i
		// delta(U(i)) / s(i)
		if ((utilityOld == -INFINITY) && (utility != -INFINITY)) marginalUtility = (Math.abs(utility) / msg.getSize());
		else if ((utility == -INFINITY) && (utilityOld != -INFINITY)) marginalUtility = (Math.abs(utilityOld) / msg.getSize());
		else if ((utility == utilityOld) && (utility != -INFINITY)) marginalUtility = (Math.abs(utility) / msg.getSize());
		else marginalUtility = (utility - utilityOld) / msg.getSize();

		return marginalUtility;
	}
	
	private double computeUtility(Message msg, DTNHost host, boolean recompute) {
		double utility = 0.0;			// U(i): The utility of the message (packet) i
		double packetDelay = 0.0;		// D(i): The expected delay of message i
		
		switch (ALGORITHM) {
			// minimizing average delay
			// U(i) = -D(i)
			case AVERAGE_DELAY:	{	
				packetDelay = estimateDelay(msg, host, recompute);
				utility = -packetDelay;
				break;
			}
			// minimizing missed deadlines
			// L(i)   : The life time of message (packet) i
			// T(i)   : The time since creation of message i
			// a(i)   : A random variable that determines the remaining time to deliver message i
			// P(a(i)): The probability that the message will be delivered within its deadline
			//
	 		//  	   / P(a(i) < L(i) - T(i)) , L(i) > T(i)
			// U(i) = <|
			// 		   \           0   		   , otherwise
			case MISSED_DEADLINES: {
				//life time in seconds
				double lifeTime = msg.getTtl() * 60;
				//creation time in seconds
				double timeSinceCreation = SimClock.getTime() - msg.getCreationTime();
				double remainingTime = computeRemainingTime(msg);
				if (lifeTime > timeSinceCreation) {
					// compute remaining time by using metadata
					if (remainingTime < lifeTime - timeSinceCreation) utility = lifeTime - timeSinceCreation - remainingTime; 
					else utility = 0;
				}
				else utility = 0;
				packetDelay = computePacketDelay(msg, host, remainingTime);
				break;
			}
			// minimizing maximum delay
			// S   : Set of all message in buffer of X
			//
			// 		   / -D(i) , D(i) >= D(j) for all j element of S
			// U(i) = <|
			// 		   \   0   , otherwise
			case MAXIMUM_DELAY: {
				packetDelay = estimateDelay(msg, host, recompute);
				Collection<Message> msgCollection = getMessageCollection();
				for (Message m : msgCollection) {
					if (m.equals(msg)) continue;	//skip 
					if (packetDelay < estimateDelay(m, host, recompute)) {
						packetDelay = 0;
						break;
					} 
				}
				utility = -packetDelay;
				break;
			}
			default: 
		} 
		
		return utility;
	}
	
	/*
	 * Estimate-Delay Algorithm
	 * 
	 * 1) Sort packets in Q in decreasing order of T(i). Let b(i) be the sum of 
	 * sizes of packets that precede i, and B the expected transfer opportunity 
	 * in bytes between X and Z.
	 * 
	 * 2. X by itself requires b(i)/B meetings with Z to deliver i. Compute the 
	 * random variable MX(i) for the corresponding delay as
	 * MX(i) = MXZ + MXZ + . . . b(i)/B times (4)
	 *  
	 * 3. Let X1 , . . . , Xk ⊇ X be the set of nodes possessing a replica of i. 
	 * Estimate remaining time a(i) as  
	 * a(i) = min(MX1(i), . . . , MXk(i))
	 * 
	 * 4. Expected delay D(i) = T(i) + E[a(i)]
	 */
	/**
	 * Returns the expected delay for a message if this message was sent to 
	 * a specified host
	 * @param msg The message which will be transfered
	 * @return the expected packet delay
	 */
	private double estimateDelay(Message msg, DTNHost host, boolean recompute) {	
		double remainingTime = 0.0;						//a(i): random variable that determines the	remaining time to deliver message i
		double packetDelay = 0.0;						//D(i): expected delay of message i
		
		//if delay table entry for this message doesn't exist or the delay table
		//has changed recompute the delay entry
		if ((recompute) && ((delayTable.delayHasChanged(msg.getId())) || (delayTable.getDelayEntryByMessageId(msg.getId()).getDelayOf(host) == null) /*|| (delayTable.getEntryByMessageId(msg.getId()).getDelayOf(host) == INFINITY))*/)) { 
			// compute remaining time by using metadata
			remainingTime = computeRemainingTime(msg);
			packetDelay = Math.min(INFINITY, computePacketDelay(msg, host, remainingTime));
			
			//update delay table
			updateDelayTableEntry(msg, host, packetDelay, SimClock.getTime());
		}
		else packetDelay = delayTable.getDelayEntryByMessageId(msg.getId()).getDelayOf(host);
		
		return packetDelay;
	}
	
	private double computeRemainingTime(Message msg) {
		double transferTime = INFINITY;					//MX(i):random variable for corresponding transfer time delay
		double remainingTime = 0.0;						//a(i): random variable that determines the	remaining time to deliver message i
		
		remainingTime = computeTransferTime(msg, msg.getTo());
		if (delayTable.getDelayEntryByMessageId(msg.getId()) != null) {
			//Entry<DTNHost host, Tuple<Double delay, Double lastUpdate>>
			for (Entry<DTNHost, Tuple<Double, Double>> entry : delayTable.getDelayEntryByMessageId(msg.getId()).getDelays()) {
				DTNHost host = entry.getKey();
				if (host == getHost()) continue;	// skip
				transferTime = ((RapidToSink) host.getRouter()).computeTransferTime(msg, msg.getTo()); //MXm(i)	with m element of [0...k]
				remainingTime = Math.min(transferTime, remainingTime);	//a(i) = min(MX0(i), MX1(i), ... ,MXk(i)) 
			}
		}
		
		return remainingTime;
	}
	
	private double computeTransferTime(Message msg, DTNHost host) {
		Collection<Message> msgCollection = getMessageCollection();
//		List<Tuple<Message, Double>> list=new ArrayList<Tuple<Message,Double>>();
		double transferOpportunity = 0;					//B:    expected transfer opportunity in bytes between X and Z
		double packetsSize = 0.0;						//b(i): sum of sizes of packets that precede the actual message
		double transferTime = INFINITY;					//MX(i):random variable for corresponding transfer time delay
		double meetingTime = 0.0;						//MXZ:  random variable that represent the meeting Time between X and Y
		
		// packets are already sorted in decreasing order of receive time
		// sorting packets in decreasing order of time since creation is optional
//		// sort packets in decreasing order of time since creation
//		double timeSinceCreation = 0;
//		for (Message m : msgCollection) {
//			timeSinceCreation = SimClock.getTime() - m.getCreationTime();
//			list.add(new Tuple<Message, Double>(m, timeSinceCreation));
//		}
//		Collections.sort(list, new TupleComparator2());
		
		// compute sum of sizes of packets that precede the actual message 
		for (Message m : msgCollection) {
//		for (Tuple<Message, Double> t : list) {
//			Message m = t.getKey();
			if (m.equals(msg)) continue; //skip
			packetsSize = packetsSize + m.getSize();	
		}
		
		// compute transfer time  
		transferOpportunity = delayTable.getAvgTransferOpportunity();	//B in bytes
		
		MeetingEntry entry = delayTable.getIndirectMeetingEntry(this.getHost().getAddress(), host.getAddress());
		// a meeting entry of null means that these hosts have never met -> set transfer time to maximum
		if (entry == null) transferTime = INFINITY;		//MX(i)
		else {
			meetingTime = entry.getAvgMeetingTime();	//MXZ
			transferTime = meetingTime * Math.ceil(packetsSize / transferOpportunity);	// MX(i) = MXZ * ceil[b(i) / B]
		}		

		return transferTime;
	}
	

	private double computePacketDelay(Message msg, DTNHost host, double remainingTime) {		
		double timeSinceCreation = 0.0;					//T(i): time since creation of message i 
		double expectedRemainingTime = 0.0;				//A(i): expected remaining time E[a(i)]=A(i)
		double packetDelay = 0.0;						//D(i): expected delay of message i
		
		//TODO E[a(i)]=Erwartungswert von a(i) mit a(i)=remainingTime
		expectedRemainingTime = remainingTime;
		
		// compute packet delay
		timeSinceCreation = SimClock.getTime() - msg.getCreationTime();
		packetDelay = timeSinceCreation + expectedRemainingTime;
		
		return packetDelay;
	}
	
	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; 
		}
		
		// Try messages that can be delivered to final recipient
		if (this.tryMessagesForSinks() != null) {
			return; 
		}
		 
		// Otherwise do RAPID-style message exchange
		if (tryOtherMessages() != null) {
			return;
		}
	}
	
	private Tuple<Tuple<Message, Connection>, Double> tryOtherMessages() {
		List<Tuple<Tuple<Message, Connection>, Double>> messages = new ArrayList<Tuple<Tuple<Message, Connection>, Double>>();			
		Collection<Message> msgCollection = getMessageCollection();
		
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			
			if(other.getRouter().hello().equals("GeneratorRouter"))
				continue;
			
			if(other.isSink()){
			this.tryMessagesForSinks();
			return null;
			}
			
			RapidToSink otherRouter = (RapidToSink) other.getRouter();

			if (otherRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			double mu=0.0;
			for (Message m : msgCollection) {
				if (otherRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one already has
				}
				
				mu=getMarginalUtility(m, con, getHost());
				if ((mu) <= 0) {
					continue; // skip messages with a marginal utility smaller or equals to 0.
				}
				
				Tuple<Message, Connection> t1=new Tuple<Message, Connection>(m, con);
				Tuple<Tuple<Message, Connection>, Double> t2=new Tuple<Tuple<Message, Connection>, Double>(t1, mu);
				messages.add(t2);
			}
		}
		delayTable.setChanged(false);
		if (messages == null) return null;
		
		Collections.sort(messages, new TupleComparator1());
		return tryTupleMessagesForConnected(messages);	// try to send messages
	}
	
	/**
	 * Tries to send messages for the connections that are mentioned
	 * in the Tuples in the order they are in the list until one of
	 * the connections starts transferring or all tuples have been tried.
	 * @param tuples The tuples to try
	 * @return The tuple whose connection accepted the message or null if
	 * none of the connections accepted the message that was meant for them.
	 */
	private Tuple<Tuple<Message, Connection>, Double> tryTupleMessagesForConnected(
			List<Tuple<Tuple<Message, Connection>, Double>> tuples) {
		if (tuples.size() == 0) {
			return null;
		}
		
		for (Tuple<Tuple<Message, Connection>, Double> t : tuples) {
			Message m = (t.getKey()).getKey();
			Connection con = (t.getKey()).getValue();
			if (startTransfer(m, con) == RCV_OK) {
				return t;
			}
		}
		
		return null;
	}
		

	public DelayTable getDelayTable() { 
		return delayTable;
	}
	
	@Override
	public MessageRouter replicate() {
		return new RapidToSink(this);
	}
	
	/**
	 * Comparator for Message-Connection-Double-Tuples that orders the tuples
	 * by their double value
	 */
	private class TupleComparator1 implements Comparator<Tuple<Tuple<Message, Connection>, Double>> {

		public int compare(Tuple<Tuple<Message, Connection>, Double> tuple1, Tuple<Tuple<Message, Connection>, Double> tuple2) {
			// tuple1...
			double mu1 = tuple1.getValue();
			// tuple2...
			double mu2 = tuple2.getValue();

			// bigger value should come first
			if (mu2-mu1 == 0) {
				/* equal values -> let queue mode decide */
				return compareByQueueMode((tuple1.getKey()).getKey(), (tuple2.getKey()).getKey());
			}
			else if (mu2-mu1 < 0) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}
	
	private enum UtilityAlgorithm {
	    AVERAGE_DELAY,
	    MISSED_DEADLINES,
	    MAXIMUM_DELAY;
	}
	
	public Map<Integer, DTNHost> getHostMapping() {
		return hostMapping;
	}
	
	public double getMeetingProb(DTNHost host) {
		MeetingEntry entry = delayTable.getIndirectMeetingEntry(getHost().getAddress(), host.getAddress());
		if (entry != null) { 
			double prob = (entry.getAvgMeetingTime() * entry.getWeight()) / SimClock.getTime();			
			return prob;
		}
		return 0.0;
	}

}
