
package routing;

import java.util.*;

import core.*;


public class WnSDecisionEngine implements RoutingDecisionEngine
{
	/** identifier for the initial number of copies setting ({@value})*/ 
	public static final String NROF_COPIES_S = "nrofCopies";
	public static final String SP_S = "spltDist";

	/** Message property key for the remaining available copies of a message */
	public static final String MSG_COUNT_PROP = "WnS.copies";

	
	protected int initialNrofCopies;
	protected int sp;
	
	
	public WnSDecisionEngine(Settings s)
	{
		initialNrofCopies = new Settings("WnS").getInt(NROF_COPIES_S);
		sp = new Settings("WnS").getInt(SP_S);
		
	}
	
	public WnSDecisionEngine(WnSDecisionEngine snf)
	{
		this.initialNrofCopies = snf.initialNrofCopies;
		this.sp = snf.sp;
		
	}
	
	public RoutingDecisionEngine replicate()
	{
		return new WnSDecisionEngine(this);
	}
	
	public void connectionDown(DTNHost thisHost, DTNHost peer){}

	public void connectionUp(DTNHost thisHost, DTNHost peer){}

	public void doExchangeForNewConnection(Connection con, DTNHost peer)
	{
		
		
	}

	public boolean isFinalDest(Message m, DTNHost aHost)
	{/* the receiving host*/
		
		Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
		nrofCopies = (int)Math.ceil(nrofCopies/2.0);
		m.updateProperty(MSG_COUNT_PROP, nrofCopies);
		
		m.setTtl(m.getTtl()/2);
		
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
		m.setTtl(m.getTtl()/2);
		
		return false;
	}

	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return m.getTo() != thisHost;
	}

	public boolean shouldSendMessageToHost(Message m, DTNHost me,DTNHost otherHost)
	{
		if(m.getTo() == otherHost) return true;
		
		int nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
		double distBwt = me.getLocation().distance(m.getTo().getLocation());
		
		if(nrofCopies > 1 && distBwt < sp) return true;
		
		
		return false;
	}


	public boolean shouldSortOldestMessages(){
		return true;
	}

	public int compareToSort(Message msg1, Message msg2){
		
		/*
		 * < 0 means msg1 is more important
		 */
		return ((Integer)msg2.getProperty(MSG_COUNT_PROP) - (Integer)msg1.getProperty(MSG_COUNT_PROP));
		
		
		
	}




}
