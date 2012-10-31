package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;


/**Author Khalil Massri
 * Implementation of Scar router as described in 
 * <I>Opportunistic MobileSensor Data Collection with SCAR</I> by
 * Bence Pasztor et al.
 * @version 1.0
 */

public class ScarToSink extends DTNRouter 
{
	
    /** Router's setting namespace ({@value})*/
	public static final String SCAR_TO_SINK_NS = "ScarToSink";
	/**  */
	public static final String BAT_EXCH_LIFE = "batExchLife";
	private int batExchLife;
	
	public static final String BAT_TIME_LIFE = "batTimeLife";
	private int batTimeLife;
	
	public static final String GAMMA = "gamma";
	private double gamma;
	
	public static final String NROFBACKUP = "nrOfBackUp";
	private int nrOfBackUp;
	
	boolean deleteDelivered=true;
	/**the number of backups for each msg id*/
	Map<String,Integer> backUps; 
	
	/**the context values and the delivery probability  */
	int tx,rx,t;
	double bat=1;
	double cdc;
	double coloc;
	private double delProb;
	/**  */
	double omega,rt,qt;
	
	int nrOfCon,preNoOfCon;
	private double lastNUpdate;
	
	private List<DTNHost> neighb;
	private Set<Integer> Nt;
	private Set<Integer> Nt_1;
	
	private Comparator<DTNHost> neighbComparator;
	private Comparator<Message> msgComparator;
	

	private int curHopsToSink=Integer.MAX_VALUE;
	private Set<String> sinkedMessages;
	/**number of backups for each generated msg*/
	
	

	
	/**
	 * Constructor. Creates a new prototype router based on the settings in
	 * the given Settings object.
	 * @param settings The settings object
	 */
	public ScarToSink(Settings settings) 
	{
		super(settings);
		Settings scarSettings = new Settings(SCAR_TO_SINK_NS);
		
		 batExchLife= scarSettings.getInt(BAT_EXCH_LIFE);
		 batTimeLife= scarSettings.getInt(BAT_TIME_LIFE);
		 gamma=scarSettings.getDouble(GAMMA);
		 nrOfBackUp=scarSettings.getInt(NROFBACKUP);
		
		
		}
	
	
	/**
	 * Copy constructor. Creates a new router based on the given prototype.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ScarToSink(ScarToSink r) 
	{
		super(r);
		this.batExchLife=r.batExchLife;
		this.batTimeLife=r.batTimeLife;
		this.gamma=r.gamma;
		this.nrOfBackUp=r.nrOfBackUp;
		
		backUps=new HashMap<String,Integer>();
		Random noise1 =new Random(0);
		Random noise2 =new Random(1);
		rt=noise1.nextDouble()/10;
		qt=noise2.nextDouble()/10;
		neighb=new ArrayList<DTNHost>();
		Nt=new HashSet<Integer>();
		Nt_1=new HashSet<Integer>();
		neighbComparator = new NeihbComparator();
		msgComparator =new MessageComparator();
		lastNUpdate= SimClock.getTime();



		}
	
    @Override
	public void changedConnection(Connection con) 
	{
		DTNHost other=con.getOtherNode(getHost());
		
		if(other.getRouter().hello().equals("GeneratorRouter"))
			return;

		if(con.isUp())
		{
			Nt.add(other.getAddress());
			if(other.isSink())
			{
				sinkedMessages=new HashSet<String>(((SinkRouter)other.getRouter()).getSinkedMessages());
				removeOldMessages();
				}
			else neighb.add(other);
			}
		else 
			{
			if(!other.isSink())
			neighb.remove(other);
		}
		
		updateHops();
		
	Collections.sort(neighb,neighbComparator);
	
	}
	
	
	private void updateCdc() {
		
		
		
		if(SimClock.getTime()>=lastNUpdate+100){
			
			Set<Integer> union = new HashSet<Integer>();
			union.addAll(Nt_1);union.addAll(Nt);
			cdc=union.size()-(Nt.size()+Nt_1.size()-union.size());
			if(cdc!=0)
				cdc=cdc/union.size();

			Nt_1.removeAll(Nt_1);
			Nt_1.addAll(Nt);
			Nt.removeAll(Nt);
			lastNUpdate=SimClock.getTime();	
			
			
		}
			
	}


	private void removeOldMessages()
	{
		Set<String> temp=new HashSet<String>();
		if(sinkedMessages.size()==0)
			return;
		for(Message m:getMessageCollection())
			if(sinkedMessages.contains(m.getId()))
				temp.add(m.getId());
		for(String s:temp)
			this.deleteMessage(s, false);
		
		}

	private void updateHops()
	{
		curHopsToSink=Integer.MAX_VALUE;
		for(Connection e:this.getConnections())
			{
			DTNHost other=e.getOtherNode(getHost());
			
			//skip generator nodes
			
			if(other.getRouter().hello().equals("GeneratorRouter"))
				continue;
			
			if(other.isSink())
			{	
				curHopsToSink=1;
				break;
				}
			
			int d=((ScarToSink)other.getRouter()).getHopsToSink();
			if(d<curHopsToSink-1)
				curHopsToSink=d+1;
		}
		
	}
	
	public void update()
	{
		if(bat==0)return;
		
		super.update();
		updateCdc();
		predict();
		
		if (!canStartTransfer() ||isTransferring()) 
		{
			return; // nothing to transfer or is currently transferring
			}
		if(this.tryMessagesForSinks()!=null)
			return;
		if(this.tryEmergency()!=null)
			return;
		
		tryOtherMessages();
			
		
		}
	
	public void predict()
	{
		double meDelProb;
		t++;
		
		
		bat=1-((tx+rx+0.0)/batExchLife + (t+0.0)/batTimeLife);
		if(bat<0)bat=0;
		
		coloc=1.0/curHopsToSink;
		
		meDelProb=(cdc+3*coloc+bat)/5.0;
		delProb=delProb+(omega/(omega+rt))*(meDelProb-delProb);
		omega=omega+qt-omega*omega/(omega+rt);
		
	}
	
	private Connection tryEmergency()
	{
		if(bat>0.05)return null;
		if(neighb.size()==0)return null;
		
		Collection<Message> msgCollection = getMessageCollection();
		if(msgCollection.size()==0)return null;
				
		List<Message> messages=new ArrayList<Message>();
		messages.addAll(msgCollection);
		Collections.sort(messages,msgComparator);
		Connection con=null;
		
		
		for(Message m:messages){
			if(!m.isMaster())
				continue;
			
			for(DTNHost h:neighb){
				 con=getConOf(h);
				 if(con==null||h.getRouter().hasMessage(m.getId()))
					 continue;
				 
				 if(startTransfer(m, con)==RCV_OK)
					 return con;
				 }
			
			}
		return con;
	}
	
	private Connection tryOtherMessages()
	{
		Collection<Message> msgCollection = getMessageCollection();
		if(msgCollection.size()==0)return null;
				
		if(neighb.size()==0)return null;
		
		
		List<Message> messages=new ArrayList<Message>();
		messages.addAll(msgCollection);
		Collections.sort(messages,msgComparator);
		
		Connection con=null;
		
		for(Message m:messages)
		{
			for(DTNHost h:neighb)
			{
				if (((ScarToSink)h.getRouter()).getDelProb() - this.getDelProb() < gamma)
				continue;
			
			 con=getConOf(h);
			 if(con==null||h.getRouter().hasMessage(m.getId()))
				 continue;
			 
			 if(startTransfer(m, con)==RCV_OK)
				 return con;
			 }
		
		}
		
		return con;
		
				
			
		}
	
	
	public boolean createNewMessage(Message msg)
	{
		makeRoomForNewMessage(msg.getSize());
		msg.setTtl(this.msgTtl);
		boolean stat= super.createNewMessage(msg);
		if(stat)
			backUps.put(msg.getId(),nrOfBackUp);
		return stat;
		}
	
	
	public Message messageTransferred(String id, DTNHost from)
	{
		Message m = super.messageTransferred(id, from);
		rx++;
		
		
		return m;
		}
	
		
	protected void transferDone(Connection con)
	{
		tx++;
		}
	
	public int getHopsToSink()
	{
		return curHopsToSink;
		}
	
		
	public double getDelProb()
	{
		return delProb;
		}
	
	
	
		
	protected int startTransfer(Message m, Connection con)
	{
		int retVal;
		if (!con.isReadyForTransfer())
		{
			return TRY_LATER_BUSY;
			}
		retVal = con.startTransfer(getHost(), m);
		
		if (retVal == DENIED_OLD && con.getOtherNode(getHost()).isSink())
		{
			/* final recipient has already received the msg -> delete it */
			
			if(backUps.containsKey(m.getId()))
				backUps.remove(m.getId());
			this.deleteMessage(m.getId(), false);
			return retVal;
			}
		
		if (retVal == RCV_OK)
		{ // started transfer
			addToSendingConnections(con);
			if(con.getOtherNode(getHost()).isSink())
				{
				if(backUps.containsKey(m.getId()))
					backUps.remove(m.getId());
				this.deleteMessage(m.getId(), false);
				return retVal;
				}
			
			if(backUps.containsKey(m.getId()))
				{
				int t=backUps.get(m.getId());
				if(t==nrOfBackUp)
				this.getMessage(m.getId()).setAsBackUp();
				backUps.put(m.getId(),(t-1));
				if(backUps.get(m.getId())==0)
				{
					this.deleteMessage(m.getId(), false);
					backUps.remove(m.getId());
					}
				return retVal;
				}
			/*just forwarding...*/
			
			this.deleteMessage(m.getId(), false);
			return retVal;
			}
		return retVal;
		}
	
	
private Connection getConOf(DTNHost h) {
		
		
		for(Connection con:getConnections())
			if(con.getOtherNode(getHost()).equals(h))
				return con;
		
		return null;
	}
	
	
	private class NeihbComparator implements Comparator<DTNHost> {

		public int compare(DTNHost h1,DTNHost h2) {
			// delivery probability of tuple1's message with tuple1's connection
			double p1 = ((ScarToSink)h1.getRouter()).getDelProb();
			// -"- tuple2...
			double p2 = ((ScarToSink)h2.getRouter()).getDelProb();

			if (p1>p2) {
				return -1;
			}
			else if(p1<p2){
				return 1;
			}
			else return 0;
		}
	}
	
	
	
	private class MessageComparator implements Comparator<Message> {

		public int compare(Message m1,Message m2) {
			// delivery probability of tuple1's message with tuple1's connection
			
			
			if (m1.isMaster() && !m2.isMaster()) {
				return -1;
			}
			else if(m2.isMaster() && !m1.isMaster()){
				return 1;
			}
			else return 0;
		}
	}	
	
	/**
	 * Returns the oldest (by receive time) message in the message buffer 
	 * (that is not being sent if excludeMsgBeingSent is true).
	 * @param excludeMsgBeingSent If true, excludes message(s) that are
	 * being sent from the oldest message check (i.e. if oldest message is
	 * being sent, the second oldest message is returned)
	 * @return The oldest message or null if no message could be returned
	 * (no messages in buffer or all messages in buffer are being sent and
	 * exludeMsgBeingSent is true)
	 */
	protected Message getOldestMessage(boolean excludeMsgBeingSent) {
		Collection<Message> messages = this.getMessageCollection();
		Message oldest = null;
		for (Message m : messages) {
			
			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
			}
			
			if(m.isMaster())
				continue;
			
			if (oldest == null ) {
				oldest = m;
			}
			else if (oldest.getReceiveTime() > m.getReceiveTime()) {
				oldest = m;
			}
		}
		
		return oldest;
	}
	
	public RoutingInfo getRoutingInfo() {
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo("Context for"+getHost()+" is ");
		ri.addMoreInfo(new RoutingInfo(String.format("%.4f>> %.4f  %.4f  %4f",
				delProb,cdc,coloc,bat)));
		top.addMoreInfo(ri);
		for(Message m:this.getMessageCollection())
			top.addMoreInfo(new RoutingInfo(m.getId()+"Master->"+m.isMaster()));
		
		return top;
	}
		
	public MessageRouter replicate() {
		ScarToSink r = new ScarToSink(this);
		return r;
	}
	
}
