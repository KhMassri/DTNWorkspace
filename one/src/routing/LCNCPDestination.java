
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
public class LCNCPDestination implements RoutingDecisionEngine
{

	private  int G = new Settings("NCP").getInt("nrofGenerations");
	private  int K = new Settings("NCP").getInt("nrofInjections");
	private  int P = new Settings("NCP").getInt("nrofPods");


	Matrix[] decodingMatrices;
	Set<String> encodedPods;
	

	int[] rows;
	int[] nrofPodCarrier;



	public LCNCPDestination(Settings s)
	{
	}

	public LCNCPDestination(LCNCPDestination lcncp)
	{

		decodingMatrices = new Matrix[P];
		rows = new int[P];
		nrofPodCarrier = new int[P];
		encodedPods = new HashSet<String>();
		for(int i=0;i<decodingMatrices.length;i++)
			decodingMatrices[i]=new Matrix(3*K,G);


	}

	public RoutingDecisionEngine replicate()
	{
		return new LCNCPDestination(this);
	}

	public void connectionDown(DTNHost thisHost, DTNHost peer){}

	public void connectionUp(DTNHost thisHost, DTNHost peer){
		


	}

	public void doExchangeForNewConnection(Connection con, DTNHost peer)
	{

		/*
		 * count how many peer has a pod packet of podi
		 */
		for(int i=0;i<P;i++)
		{
			if(encodedPods.contains("P"+i))
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

	public boolean isFinalDest(Message m, DTNHost aHost)
	{
		String[] id = m.getId().split(":");
		String lc = id[1];
		int pod = Integer.parseInt(id[0].substring(1));
		insertRowInto(pod,lc);

		if(decodingMatrices[pod].rank()>=G && !encodedPods.contains("P"+pod))
		{
			encodedPods.add("P"+pod);

			for(NetworkInterface c: aHost.getInterfaces())
				for(ConnectionListener l:c.getClisteners())
					if(l instanceof EncodingReport )
						((EncodingReport)l).encodingDone(aHost, pod,this.nrofPodCarrier[pod]);



		}

		return true;

	}

	public boolean newMessage(Message m)
	{
		return false;
	}

	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld)
	{
		return m.getTo() == hostReportingOld;
	}

	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost)
	{

		return false;
	}

	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return m.getTo() != thisHost;
	}

	public boolean shouldSendMessageToHost(Message m,DTNHost me, DTNHost otherHost)
	{
		return false;

	}



	public boolean shouldSortOldestMessages(){
		return false;
	}

	public int compareToSort(Message msg1, Message msg2){

		return 0;

	}

	void insertRowInto(int pod,String s){

		if(s.length() > decodingMatrices[pod].getColumnDimension())
			return;

		int i=0;
		for(byte b:s.getBytes())
			decodingMatrices[pod].set(rows[pod],i++, (double)(b-48));


				rows[pod]++;


	}


	public Set<String> getEncodedPods() {
		return encodedPods;
	}
	

}