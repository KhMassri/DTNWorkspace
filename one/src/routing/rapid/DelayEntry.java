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

import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import core.DTNHost;
import core.Message;
import core.SimClock;
import core.Tuple;

/**
 * Class for storing and manipulating delay entries for a specified message of 
 * the RAPID router module.
 */
public class DelayEntry {
	
	private Message msg = null;
	//HashMap<DTNHost host, Tuple<Double delay, Double lastUpdate>>
	private HashMap<DTNHost, Tuple<Double, Double>> delays = null;
	private boolean changed;
	
	/**
	 * Constructor. Creates a delay entry for a specified message
	 * @param msg The message of the entry
	 */
	public DelayEntry(Message msg) {
		this.msg = msg;
		this.delays = new HashMap<DTNHost, Tuple<Double, Double>>();
		this.changed = false;
	}
	
	/**
	 * Adding a new host delay for this message delay entry
	 * @param host The host which contains a copy of this message 
	 * @param delay The delay
	 * @param delay The time
	 */
	public void addHostDelay(DTNHost host, double delay, double time) {
		assert(!delays.containsKey(host));
		delays.put(host, new Tuple<Double, Double>(delay, time));
		
		changed = true;
	}
	
	/**
	 * Adding a new host delay for this message delay entry
	 * @param host The host 
	 * @param delay The delay
	 */
	public void addHostDelay(DTNHost host, double delay) {
		addHostDelay(host, delay, SimClock.getTime());
	}
	
	/**
	 * Setting the delay of the specified host for this message 
	 * delay entry
	 * @param host The host which contains a copy of this message
	 * @param delay The delay
	 */
	public void setHostDelay(DTNHost host, double delay, double time) {
		assert(delays.containsKey(host));
		delays.put(host, new Tuple<Double, Double>(delay, time));
		
		changed = true;
	}
	
	/**
	 * Removing a existing host delay from this message delay entry
	 * @param host The host which contains a copy of this message
	 * @param delay The delay
	 */
	public void removeHostDelay(DTNHost host) {
		delays.remove(host);
		
		changed = true;
	}
	
	/**
	 * Returns the message of this entry
	 * @return the message
	 */
	public Message getMessage() {
		return msg;
	}
	
	/**
	 * Returns the delay values of this delay entry 
	 * @return the current delay values 
	 * Entry<DTNHost host, Tuple<Double delay, Double lastUpdate>>
	 */
	public Set<Entry<DTNHost, Tuple<Double, Double>>> getDelays() {
		return delays.entrySet();
	}
	
	/**
	 * Returns the delay value of the given host or null
	 * if no entry exists for it 
	 * @param host The host 
	 * @return the current delay value
	 */
	public Double getDelayOf(DTNHost host) {
		if (delays.get(host) == null) return null;
		return delays.get(host).getKey();
	}
	
	/**
	 * Returns true if the actual delay host entry was created or last updated
	 * before the given time. Otherwise returns false.
	 * @param host A host of the actual entry
	 * @param time A time value
	 * @return true or false
	 */
	public boolean isOlderThan(DTNHost host, double time) {
		if (!contains(host)) return true;
		
		return (getLastUpdate(host) < time);
	}
	
	/**
	 * Returns true if the actual delay host entry was created or last updated
	 * at the given time. Otherwise returns false.
	 * @param host A host of the actual entry
	 * @param time A time value
	 * @return true or false
	 */
	public boolean isAsOldAs(DTNHost host, double time) {
		if (!contains(host)) return false;
		
		return (getLastUpdate(host) == time);
	}
	
	public boolean contains(DTNHost host) {
		return (getDelayOf(host) != null);
	}
	
	/**
	 * Returns the creation time or the last time this delay host entry was 
	 * updated or null if the host not exists
	 * @param host A host of the actual entry
	 * @return the time this entry was updated last
	 */
	public Double getLastUpdate(DTNHost host) {
		if (!contains(host)) return null;
		
		return (delays.get(host).getValue());
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}
	
	public boolean hasChanged() {
		return changed;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof DelayEntry) {
			DelayEntry entry = (DelayEntry) o;
			boolean status = ((this.msg.compareTo(entry.msg) == 0) && (equalsCheck(entry.delays)));
			return status;
		}
		return false;
	}
	
	private boolean equalsCheck(HashMap<DTNHost, Tuple<Double, Double>> map) {
		for (Entry<DTNHost, Tuple<Double, Double>> entry : this.getDelays()) {
			Tuple<Double, Double> t = map.get(entry.getKey());
			if (t == null) return false;
			if (!t.getKey().equals(entry.getValue().getKey())) return false;
			if (!t.getValue().equals(entry.getValue().getValue())) return false;
		}
		
		return true;
	}
	
	/**
	 * Print for every host the entries to the command line
	 */
	public void print() {
		//Entry<DTNHost host, Tuple<Double delay, Double lastUpdate>>
		for (Entry<DTNHost, Tuple<Double, Double>> entry : getDelays()) {
			System.out.println(msg+"  \t\t"+entry.getKey()+"\t\t\t"+entry.getValue().getKey()+"\t\t\t\t"+entry.getValue().getValue());
		}
	}
}
