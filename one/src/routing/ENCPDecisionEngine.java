
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
public class ENCPDecisionEngine implements RoutingDecisionEngine
{
	/** identifier for the initial number of copies setting ({@value})*/ 
	public static final String NROF_COPIES_S = "nrofCopies";
	/** Message property key for the remaining available copies of a message */
	
	public static final String NROF_GENERATIONS_S = "nrofGenerations";
	public static final String NROF_INJECTIONS_S = "nrofInjections";
	public static final String NROF_PODS_S = "nrofPods";
	

	GaloisField gf;
	Set<Integer> relays;
	Map<Integer,Tuple<Integer,Integer>>sprayList;
	Matrix[] decodingMatrices;
	Set<String> encodedPods;
	int[] rows;
	int[] nrofPodCarrier;
	int peers = 0;

	
	protected int L;
	protected int G;
	protected int K;
	protected int P;
	
	int[] s={K,K}; //source Pods sended packets counter K to 0
	int pkt;
	int pods;
	int g=1;

	
	
	public ENCPDecisionEngine(Settings s)
	{
		L = new Settings("ENCP").getInt(NROF_COPIES_S);
		G = new Settings("ENCP").getInt(NROF_GENERATIONS_S);
		K = new Settings("ENCP").getInt(NROF_INJECTIONS_S);
		P = new Settings("ENCP").getInt(NROF_PODS_S);

		
	}
	
	public ENCPDecisionEngine(ENCPDecisionEngine ENCP)
	{
		this.L = ENCP.L;
		this.G = ENCP.G;
		this.K = ENCP.K;
		this.P = ENCP.P;
		this.pkt=1;
		this.pods=0;

		/*
		 * spray list for each <pod,<i,L>> 
		 */
		sprayList = new HashMap<Integer,Tuple<Integer,Integer>>();
		relays = new HashSet<Integer>();
		gf=GaloisField.getInstance();
		
		decodingMatrices = new Matrix[this.P];
		rows = new int[this.P];
		nrofPodCarrier = new int[this.P];
		encodedPods = new HashSet<String>();
		
		for(int i=0;i<decodingMatrices.length;i++)
			decodingMatrices[i]=new Matrix(this.K,this.G);
		
				
	
	}
	
	public RoutingDecisionEngine replicate()
	{
		return new ENCPDecisionEngine(this);
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
//		Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
//		nrofCopies = (int)Math.ceil(nrofCopies/2.0);
//		m.updateProperty(MSG_COUNT_PROP, nrofCopies);
		
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
				
				}
		
		
		m.setID("P"+pods+":"+getLC());
		m.addProperty("L", L);
		m.addProperty("Index",pkt);
		pkt++;
		return true;
	}

	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld)
	{
		return m.getTo() == hostReportingOld;
	}

	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost)
	{
		
		if(m.getTo() == otherHost) return true;
		
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
		
//		int nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
//		if(nrofCopies > 1 ) return true;
//		
	
		return false;
	}



	public boolean shouldSortOldestMessages(){
		return false;
	}

	public int compareToSort(Message msg1, Message msg2){
		
		return 0;
		
	}



	
void insertRowInto(int pod,String s){
	
	String[] co = s.split(",");
	if(co.length > decodingMatrices[pod].getColumnDimension())
		return;
	
   int i=0;
	for(String e:co)
		decodingMatrices[pod].set(rows[pod],i++, Integer.parseInt(e));
	
			
	rows[pod]++;
	
	
}

/*
 * generate K coefficients as a psudo source packet 
 */

String getLC(){
	String s="";
	
	for(int i=1;i<=G;i++){
		int alfa = new Random().nextInt(256);
		s += alfa+","; 
	}
		
	return s;
	
	}
	
	
	
	

}