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

import core.SimClock;

/**
 * Class for storing and manipulating meeting time entries for the RAPID router module.
 */
public class MeetingEntry {
	
	private double timestamp;
	private int weight;
	private double avgMeetingTime;
	private boolean dummy;
		
	/**
	 * Constructor. Creates a meeting time entry
	 * @param meetingTime The actual meeting time
	 */
	public MeetingEntry(double meetingTime) {
		this.weight = 0;
		this.timestamp = 0;
		this.avgMeetingTime = 0;
		this.dummy = false;
	
		update(meetingTime, SimClock.getTime());
	}
	
	/**
	 * Constructor. Creates a meeting time entry
	 * @param meetingTime The actual meeting time
	 * @param timestamp The actual timestamp
	 */
	public MeetingEntry(double meetingTime, double timestamp) {
		this.weight = 0;
		this.timestamp = 0;
		this.avgMeetingTime = 0;
		this.dummy = false;
	
		update(meetingTime, timestamp);
	}
	
	/**
	 * Copy Constructor. Creates a meeting time entry
	 * @param meetingTime The actual meeting time
	 * @param timestamp The actual timestamp
	 * @param weight The number of meetings
	 */
	public MeetingEntry(double meetingTime, double timestamp, int weight, boolean dummy) {
		this.avgMeetingTime = meetingTime;
		this.timestamp = timestamp;
		this.weight = weight;
		this.dummy = dummy;
	}
	
	/**
	 * Copy Constructor. Creates a meeting time entry
	 * @param entry The meeting entry to copy from
	 */
	public MeetingEntry(MeetingEntry entry) {
		this.avgMeetingTime = entry.avgMeetingTime;
		this.timestamp = entry.timestamp;
		this.weight = entry.weight;
		this.dummy = entry.dummy;
	}
	
	/**
	 * Sets the average meeting time 
	 * @param meetingTime The current meeting time
	 * @param timestamp The time stamp of this entry
	 * @param weight The weight of this entry
	 */
	public void set(double meetingTime, double timestamp, int weight) {
		this.weight = weight;
		this.timestamp = timestamp;
		this.avgMeetingTime = meetingTime;
	}
	
	/**
	 * Updates the average meeting time by recomputing it with the current
	 * meeting time of the met host
	 * @param meetingTime The current meeting time
	 * @param timestamp The time stamp of this entry
	 */
	public void update(double meetingTime, double timestamp) {
		avgMeetingTime = (((weight * avgMeetingTime) + meetingTime) / (weight + 1));
		weight++;
		this.timestamp = timestamp;
	}
	
	/**
	 * Returns true if the actual meeting time entry was created or last updated
	 * before the given time. Otherwise returns false.
	 * @param time A time value
	 * @return true or false
	 */
	public boolean isOlderThan(double time) {		
		return (timestamp < time);
	}
	
	/**
	 * Returns true if the actual meeting time entry was created or last updated
	 * at the same time. Otherwise returns false.
	 * @param time A time value
	 * @return true or false
	 */
	public boolean isAsOldAs(double time) {		
		return (timestamp == time); 
	}
	
	/**
	 * Returns the average meeting time of an entry
	 * @return the average meeting time
	 */
	public double getAvgMeetingTime() {
		return avgMeetingTime;
	}
	
	/**
	 * Returns the creation time or the last time this meeting time entry was 
	 * updated
	 * @return the time this entry was updated last
	 */
	public Double getLastUpdate() {	
		return timestamp;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MeetingEntry) {
			MeetingEntry entry = (MeetingEntry) o;
			return ((this.avgMeetingTime == entry.avgMeetingTime) && (this.timestamp == entry.timestamp)); 
		}
		return false;
	}
	
	public int getWeight() {
		return weight;
	}
	
	public boolean isDummy() {
		return dummy;
	}
	
	/**
	 * Print the entry to the command line
	 */
	public void print() {
		System.out.println(timestamp+"\t\t\t\t"+avgMeetingTime);
	}
	
}
