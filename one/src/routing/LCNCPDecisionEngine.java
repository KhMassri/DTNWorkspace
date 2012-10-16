
package routing;

import java.util.*;

import report.EncodingReport;

import Jama.Matrix;

import core.*;
/*
 * 
 * implement the encp
 * 
 */
public class LCNCPDecisionEngine implements RoutingDecisionEngine
{
	/** identifier for the initial number of copies setting ({@value})*/ 
	public static final String NROF_COPIES_S = "nrofCopies";
	/** Message property key for the remaining available copies of a message */
	public static final String MSG_COUNT_PROP = "SprayAndWait.copies";
	
	public static final String NROF_GENERATIONS_S = "nrofGenerations";
	public static final String NROF_INJECTIONS_S = "nrofInjections";
	public static final String NROF_PODS_S = "nrofPods";
	

	Set<Integer> AllLC;
	Matrix[] decodingMatrices;
	Set<String> encodedPods;
	int[] rows;
	int[] nrofPodCarrier;
	int peers = 0;

	
	protected int initialNrofCopies;
	protected int G;
	protected int K;
	protected int P;
	int pkt;
	int pods;
	int g=1;

	
	
	public LCNCPDecisionEngine(Settings s)
	{
		initialNrofCopies = new Settings("LCNCP").getInt(NROF_COPIES_S);
		G = new Settings("LCNCP").getInt(NROF_GENERATIONS_S);
		K = new Settings("LCNCP").getInt(NROF_INJECTIONS_S);
		P = new Settings("LCNCP").getInt(NROF_PODS_S);

		
	}
	
	public LCNCPDecisionEngine(LCNCPDecisionEngine lcncp)
	{
		this.initialNrofCopies = lcncp.initialNrofCopies;
		this.G = lcncp.G;
		this.K = lcncp.K;
		this.P = lcncp.P;
		this.pkt=1;
		this.pods=0;
		AllLC=new HashSet<Integer>();
		decodingMatrices = new Matrix[this.P];
		rows = new int[this.P];
		nrofPodCarrier = new int[this.P];
		encodedPods = new HashSet<String>();
		for(int i=0;i<decodingMatrices.length;i++)
			decodingMatrices[i]=new Matrix(this.K,this.G);
				
	
	}
	
	public RoutingDecisionEngine replicate()
	{
		return new LCNCPDecisionEngine(this);
	}
	
	public void connectionDown(DTNHost thisHost, DTNHost peer){}

	public void connectionUp(DTNHost thisHost, DTNHost peer){
		/*
		 * count how many peer has a pod packet of podi
		 */
		if(thisHost.getAddress()==1)
		{
			for(int i=0;i<this.P;i++)
			{
				if(encodedPods.contains("Pod"+i))
					continue;

				for(Message m:peer.getRouter().getMessageCollection())
				{
					String[] id = m.getId().split(":");
					int pod = Integer.parseInt(id[0].substring(1));
					if(pod==i)
					{
						this.nrofPodCarrier[pod]++;
						break;
					}
						
				}	
			}
		}
	
		
		
		
	}

	public void doExchangeForNewConnection(Connection con, DTNHost peer)
	{
		
		
	}

	public boolean isFinalDest(Message m, DTNHost aHost)
	{
		Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
		nrofCopies = (int)Math.ceil(nrofCopies/2.0);
		m.updateProperty(MSG_COUNT_PROP, nrofCopies);
		
		if(m.getTo() == aHost)
		{
			String[] id = m.getId().split(":");
			String lc = id[1];
			int pod = Integer.parseInt(id[0].substring(1));
			insertRowInto(pod,lc);
			
			if(decodingMatrices[pod].rank()>=G && !encodedPods.contains("Pod"+pod))
			{
				encodedPods.add("Pod"+pod);
				
				for(NetworkInterface c: aHost.getInterfaces())
					for(ConnectionListener l:c.getClisteners())
						if(l instanceof EncodingReport )
							((EncodingReport)l).encodingDone(aHost, pod,this.nrofPodCarrier[pod]);
				
			
				
			}
			
			return true;
		}
		
		return false;
	}

	public boolean newMessage(Message m)
	{
		
		if(pkt > K) 
			if (pods >= P-1)
				return false;
			else
				{
				pkt = 1;
				pods++;
				
				g=1;
				AllLC.clear();
				
				}
		
		
		m.setID("P"+pods+":"+getE());
		pkt++;
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
		
		String[] mId = m.getId().split(":");
		String mPodId = mId[0];
		
		for(Message msg:otherHost.getRouter().getMessageCollection()){
			
			String[] msgId = msg.getId().split(":");
			String msgPodId = msgId[0];
			if(msgPodId.equals(mPodId))
			{
				return false;
			}
			
		}
		
		int nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
		if(nrofCopies > 1 ) return true;
		
	
		/*
		 * if I'm the source station then don't keep the last copy
		 */
		if(me.getAddress()==0)
			return true;
		
		
		
		return false;
	}



	public boolean shouldSortOldestMessages(){
		return true;
	}

	public int compareToSort(Message msg1, Message msg2){
		
		return (Integer)msg2.getProperty(MSG_COUNT_PROP) - (Integer)msg1.getProperty(MSG_COUNT_PROP);
		
	}


	
String getRandomLC()
	{
		int r;
		do {
			
			r= 1 + new Random().nextInt((int)Math.pow(2,G) - 1);
		}
		while(AllLC.contains(r));
		
		String s = Integer.toBinaryString(r);
		while(s.length()<G)s="0"+s;
		AllLC.add(r);					
		return s;	
					
		}
	
void insertRowInto(int pod,String s){
	
	if(s.length() > decodingMatrices[pod].getColumnDimension())
		return;
	
   int i=0;
	for(byte b:s.getBytes())
		decodingMatrices[pod].set(rows[pod],i++, (double)(b-48));
	
			
	rows[pod]++;
	
	
}
	
String getE(){
	
	
	if(this.g > (int)Math.pow(2,this.G-1))
		return getRandomLC();
	
	String s = Integer.toBinaryString(g);
	while(s.length()<G)s="0"+s;
	AllLC.add(g);
	g=g*2;
	
	
	return s;
	
	}
	
	
	
	

}