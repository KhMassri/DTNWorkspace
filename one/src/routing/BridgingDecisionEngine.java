package routing;

import core.*;

public class BridgingDecisionEngine implements RoutingDecisionEngine

{
		
	public BridgingDecisionEngine(Settings s)
	{
				
	}
	
	public BridgingDecisionEngine(BridgingDecisionEngine snf)
	{
		
	}
	
	public RoutingDecisionEngine replicate()
	{
		return new BridgingDecisionEngine(this);
	}
	
	public void connectionDown(DTNHost thisHost, DTNHost peer){}

	public void connectionUp(DTNHost thisHost, DTNHost peer){}

	public void doExchangeForNewConnection(Connection con, DTNHost peer)
	{
		
	}

	public boolean isFinalDest(Message m, DTNHost aHost)
	{
		
		return m.getTo() == aHost;
	}

	public boolean newMessage(Message m)
	{
		//change to
		return true;
	}

	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld)
	{
		return m.getTo() == hostReportingOld;
	}

	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost)
	{
			return true;
	}

	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return true;
		//return m.getTo() != thisHost;
	}

	public boolean shouldSendMessageToHost(Message m,DTNHost me, DTNHost otherHost)
	{
		if(m.getTo() != me && !m.getHops().contains(otherHost)) return true;
		
		
		return false;
	}

	public boolean shouldSortOldestMessages(){
		return false;
	}

	public int compareToSort(Message msg1, Message msg2){
		
		/*
		 * < 0 means msg1 is more important
		 */
		return 0;
		
		
		
	}



}