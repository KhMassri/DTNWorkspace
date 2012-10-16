/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 * <P><strong>Note:</strong> if some statistics could not be created (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class MessageStatsReport extends Report implements MessageListener {
	private Map<String, Double> creationTimes;
	private List<Double> latencies;
	private List<Integer> hopCounts;
	private List<Double> msgBufferTime;
	private List<Double> rtt; // round trip times
	private Map<String, Integer> copies;
	private Map<String, Double> msgCustodianTime;
	private Map<String, Integer> totalCopies;

	private Set<String> delivered;
	
	private int nrofDropped;
	private int nrofRemoved;
	private int nrofStarted;
	private int nrofAborted;
	private int nrofRelayed;
	private int nrofCreated;
	private int nrofResponseReqCreated;
	private int nrofResponseDelivered;
	private int nrofDelivered;
	
	/**
	 * Constructor.
	 */
	public MessageStatsReport() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.creationTimes = new HashMap<String, Double>();
		this.latencies = new ArrayList<Double>();
		this.msgBufferTime = new ArrayList<Double>();
		this.hopCounts = new ArrayList<Integer>();
		this.rtt = new ArrayList<Double>();
		this.copies = new HashMap<String,Integer>();
		this.msgCustodianTime = new HashMap<String,Double>();
		this.totalCopies = new HashMap<String,Integer>();
		this.delivered = new HashSet<String>();
		 
		this.nrofDropped = 0;
		this.nrofRemoved = 0;
		this.nrofStarted = 0;
		this.nrofAborted = 0;
		this.nrofRelayed = 0;
		this.nrofCreated = 0;
		this.nrofResponseReqCreated = 0;
		this.nrofResponseDelivered = 0;
		this.nrofDelivered = 0;
	}

	
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		if (dropped) {
			this.nrofDropped++;
		}
		else {
			this.nrofRemoved++;
		}
		
		this.msgBufferTime.add(getSimTime() - m.getReceiveTime());
		
		if(msgCustodianTime.containsKey(m.getId()))
			msgCustodianTime.put(m.getId(),msgCustodianTime.get(m.getId()) + getSimTime() - m.getReceiveTime());
		else
			msgCustodianTime.put(m.getId(),getSimTime() - m.getReceiveTime());
			
	}

	
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		this.nrofAborted++;
	}

	
	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean finalTarget) {
		if (isWarmupID(m.getId())) {
			return;
		}

		this.nrofRelayed++;
		if (finalTarget) {
			this.latencies.add(getSimTime() - 
				this.creationTimes.get(m.getId()) );
			this.nrofDelivered++;
			this.hopCounts.add(m.getHops().size() - 1);
			this.delivered.add(m.getId());
			if (m.isResponse()) {
				this.rtt.add(getSimTime() -	m.getRequest().getCreationTime());
				this.nrofResponseDelivered++;
			}
		}
				
		if(!delivered.contains(m.getId()))
			if(from.getRouter().hasMessage(m.getId()))
			copies.put(m.getId(), copies.get(m.getId())+1);
		
		if(from.getRouter().hasMessage(m.getId()) && m.getTo().getAddress()!=to.getAddress())
			totalCopies.put(m.getId(), totalCopies.get(m.getId())+1);
		
	}


	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}
		
		this.creationTimes.put(m.getId(), getSimTime());
		this.nrofCreated++;
		this.copies.put(m.getId(), 1);
		this.totalCopies.put(m.getId(), 1);
		
		if (m.getResponseSize() > 0) {
			this.nrofResponseReqCreated++;
		}
	}
	
	
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}

		this.nrofStarted++;
	}
	

	@Override
	public void done() {
		write("Message stats for scenario " + getScenarioName() + 
				"\nsim_time: " + format(getSimTime()));
		double deliveryProb = 0; // delivery probability
		double responseProb = 0; // request-response success probability
		double overHead = Double.NaN;	// overhead ratio
		double totalNrofCopies=0;
		double avgNrofCopies;
		double totalNrofTotalCopies=0;
		double avgNrofTotalCopies;
		
		if (this.nrofCreated > 0) {
			deliveryProb = (1.0 * this.nrofDelivered) / this.nrofCreated;
		}
		if (this.nrofDelivered > 0) {
			overHead = (1.0 * (this.nrofRelayed - this.nrofDelivered)) /
				this.nrofDelivered;
		}
		if (this.nrofResponseReqCreated > 0) {
			responseProb = (1.0* this.nrofResponseDelivered) / 
				this.nrofResponseReqCreated;
		}
		
		for(Entry<String, Integer> e: copies.entrySet())
			totalNrofCopies+=e.getValue();
		avgNrofCopies = totalNrofCopies/copies.size();
				
		for(Entry<String, Integer> e: totalCopies.entrySet())
			totalNrofTotalCopies+=e.getValue();
				
		avgNrofTotalCopies = totalNrofTotalCopies/totalCopies.size();

		String statsText = "created: " + this.nrofCreated + 
			"\nstarted: " + this.nrofStarted + 
			"\nrelayed: " + this.nrofRelayed +
			"\naborted: " + this.nrofAborted +
			"\ndropped: " + this.nrofDropped +
			"\nremoved: " + this.nrofRemoved +
			"\ndelivered: " + this.nrofDelivered +
			"\ndelivery_prob: " + format(deliveryProb) +
			"\nresponse_prob: " + format(responseProb) + 
			"\noverhead_ratio: " + format(overHead) + 
			"\nlatency_avg: " + getAverage(this.latencies) +
			"\nlatency_med: " + getMedian(this.latencies) + 
			"\nhopcount_avg: " + getIntAverage(this.hopCounts) +
			"\nhopcount_med: " + getIntMedian(this.hopCounts) + 
			"\ncopies_avg: " + avgNrofCopies+
			"\ntotal_copies_avg: " + avgNrofTotalCopies+
			"\nbuffertime_avg: " + getAverage(this.msgBufferTime) +
			"\nbuffertime_med: " + getMedian(this.msgBufferTime) +
			"\ncustodiantime_avg: " + getAverage(this.msgCustodianTime) +
			"\nrtt_avg: " + getAverage(this.rtt) +
			"\nrtt_med: " + getMedian(this.rtt)
			;
		
		write(statsText);
		super.done();
	}

	
	
}
