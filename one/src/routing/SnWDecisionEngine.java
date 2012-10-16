/*
 * @(#)SnWDecisionEngine.java
 *
 * Copyright 2010 by University of Pittsburgh, released under GPLv3.
 * 
 */
package routing;

import java.util.*;

import core.*;


/**
 * An implementation of the Spray and Wait Routing protocol using the
 * Decision Engine framework.
 * 
 * @author PJ Dillon, University of Pittsburgh
 */
public class SnWDecisionEngine implements RoutingDecisionEngine
{
	/** identifier for the initial number of copies setting ({@value})*/ 
	public static final String NROF_COPIES_S = "nrofCopies";
	/** Message property key for the remaining available copies of a message */
	public static final String MSG_COUNT_PROP = "SprayAndWait.copies";

	
	protected int initialNrofCopies;
	
	
	public SnWDecisionEngine(Settings s)
	{
		initialNrofCopies = new Settings("SnW").getInt(NROF_COPIES_S);
		
		
	}
	
	public SnWDecisionEngine(SnWDecisionEngine snf)
	{
		this.initialNrofCopies = snf.initialNrofCopies;
		
	}
	
	public RoutingDecisionEngine replicate()
	{
		return new SnWDecisionEngine(this);
	}
	
	public void connectionDown(DTNHost thisHost, DTNHost peer){}

	public void connectionUp(DTNHost thisHost, DTNHost peer){}

	public void doExchangeForNewConnection(Connection con, DTNHost peer)
	{
		
		
	}

	public boolean isFinalDest(Message m, DTNHost aHost)
	{
		Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
		nrofCopies = (int)Math.ceil(nrofCopies/2.0);
		m.updateProperty(MSG_COUNT_PROP, nrofCopies);
		
		return m.getTo() == aHost;
	}

	public boolean newMessage(Message m)
	{
		m.addProperty(MSG_COUNT_PROP, initialNrofCopies);
		return true;
	}

	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld)
	{
		return m.getTo() == hostReportingOld;
	}

	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost)
	{
		int nrofCopies;
		
		if(m.getTo() == otherHost) return true;
		
		nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
		
		if(nrofCopies > 1)
			nrofCopies /= 2;
		else
			return true;

		m.updateProperty(MSG_COUNT_PROP, nrofCopies);
		
		return false;
	}

	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return m.getTo() != thisHost;
	}

	public boolean shouldSendMessageToHost(Message m,DTNHost me, DTNHost otherHost)
	{
		if(m.getTo() == otherHost) return true;
		
		int nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
		if(nrofCopies > 1) return true;
		
		
		
		return false;
	}



	public boolean shouldSortOldestMessages(){
		return false;
	}

	public int compareToSort(Message msg1, Message msg2){
		
		return 0;
		
	}



}