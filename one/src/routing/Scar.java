package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

public class Scar extends DTNRouter 
{
	
    /** Router's setting namespace ({@value})*/
	public static final String SCAR_NS = "Scar";
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
	private Map<Integer,Double> delProbOf;
	/**  */
	double omega,rt,qt;
	
	int nrOfCon,preNoOfCon;
	private double lastNUpdate;
	
	private List<DTNHost> neighb;
	private Set<Integer> Nt;
	private Set<Integer> Nt_1;
	
	private Comparator<Message> msgComparator;
	

	private Map<Integer,Integer> hopsToAll;
	private Set<String> ackedMessages;
	/**number of backups for each generated msg*/
	
	

	
	/**
	 * Constructor. Creates a new prototype router based on the settings in
	 * the given Settings object.
	 * @param settings The settings object
	 */
	public Scar(Settings settings) 
	{
		super(settings);
		Settings scarSettings = new Settings(SCAR_NS);
		
		 batExchLife= scarSettings.getInt(BAT_EXCH_LIFE);
		 batTimeLife= scarSettings.getInt(BAT_TIME_LIFE);
		 gamma=scarSettings.getDouble(GAMMA);
		 nrOfBackUp=scarSettings.getInt(NROFBACKUP);
		
		
		}
	
	
	/**
	 * Copy constructor. Creates a new router based on the given prototype.
	 * @param r The router prototype where setting values are copied from
	 */
	protected Scar(Scar r) 
	{
		super(r);
		this.batExchLife=r.batExchLife;
		this.batTimeLife=r.batTimeLife;
		this.gamma=r.gamma;
		this.nrOfBackUp=r.nrOfBackUp;
		
		backUps=new HashMap<String,Integer>();
		hopsToAll = new HashMap<Integer,Integer>();
		delProbOf=new HashMap<Integer,Double>();
		ackedMessages=new HashSet<String>();
		Random noise1 =new Random(0);
		Random noise2 =new Random(1);
		rt=noise1.nextDouble()/10;
		qt=noise2.nextDouble()/10;
		neighb=new ArrayList<DTNHost>();
		Nt=new HashSet<Integer>();
		Nt_1=new HashSet<Integer>();
		msgComparator =new MessageComparator();
		lastNUpdate= SimClock.getTime();



		}
	
    @Override
	public void changedConnection(Connection con) 
	{
		DTNHost other=con.getOtherNode(getHost());
		
		if(!other.getRouter().hello().equals("DTNRouter"))
			return;
		
		/* Skip Generator router*/
		if(con.getOtherNode(getHost()).getRouter().hello().equals("GeneratorRouter"))
			return;
		

		if(con.isUp())
		{
			Nt.add(other.getAddress());
			if(!delProbOf.containsKey(other.getAddress()))
				delProbOf.put(other.getAddress(),0.0);
			
			neighb.add(other);
			
			if(con.isInitiator(getHost()))
				exchangeAckedMessages(other);
			
		}
		
		else neighb.remove(other);
		
	
	}
	
    
    private void exchangeAckedMessages(DTNHost other) {
		
    	this.ackedMessages.addAll(((Scar)other.getRouter()).ackedMessages);
    	((Scar)other.getRouter()).ackedMessages.addAll(ackedMessages);
    	removeOldMessages();
    	((Scar)other.getRouter()).removeOldMessages();
    	
		
	}


	Map<Integer,Integer> getHopsToAll(){
    	
    	return hopsToAll;
    	
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
		if(ackedMessages.size()==0)
			return;
		for(Message m:getMessageCollection())
			if(ackedMessages.contains(m.getId()))
				temp.add(m.getId());

		for(String s:temp)
			if(!isSending(s))
				deleteMessage(s, false);
		
		}

	
	
	public void update()
	{
		if(bat==0)return;
		
		super.update();
		
		updateCdc();
		updateAllHops();
		
		predict();
		
		if (!canStartTransfer() ||isTransferring()) 
		{
			return; // nothing to transfer or is currently transferring
			}
		if(this.exchangeDeliverableMessages()!=null)
			return;
		
		if(this.tryEmergency()!=null)
			return;
		
		tryOtherMessages();
			

		}
	
	private void updateAllHops() {

		hopsToAll.clear();
		
		for(Connection con:this.getConnections()){
			DTNHost oth=con.getOtherNode(getHost());
			if(!oth.getRouter().hello().equals("DTNRouter"))
				continue;
			Scar other=(Scar)oth.getRouter();
			
			hopsToAll.put(oth.getAddress(),1);
			
			for(Entry<Integer,Integer> e:other.getHopsToAll().entrySet())
			{
				
				if(e.getKey()==getHost().getAddress())
					continue;
				if(this.getHopsTo(e.getKey()) > e.getValue()+1)
					hopsToAll.put(e.getKey(), e.getValue()+1);
			}
			
									
		}
		
		
		
		
	}


	public void predict()
	{
		double meDelProb;
		t++;
		
		
		bat=1-((tx+rx+0.0)/batExchLife + (t+0.0)/batTimeLife);
		if(bat<0)bat=0;
		
		for(Entry<Integer,Double> e: delProbOf.entrySet()){
			Integer to = e.getKey();
			coloc=1.0/getHopsTo(to);
			meDelProb=(cdc+3*coloc+bat)/5.0;
			
			double curDelProb=getDelProbOf(to);
			delProbOf.put(to, curDelProb+(omega/(omega+rt))*(meDelProb-curDelProb));
			omega=omega+qt-omega*omega/(omega+rt);
			
		}
		
			
		
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
			Integer to = m.getTo().getAddress();
			
			Collections.sort(neighb,new NeihbComparator(to));
			
			for(DTNHost h:neighb)
			{
				if (((Scar)h.getRouter()).getDelProbOf(to) - this.getDelProbOf(to) < gamma)
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
		if(m.getTo()==getHost())
			ackedMessages.add(m.getId());
		
		return m;
		}
	
		
	protected void transferDone(Connection con)
	{
		tx++;
		
		}
	
	public int getHopsTo(Integer other)
	{
		if(hopsToAll.containsKey(other))
			return hopsToAll.get(other);
		
		return Integer.MAX_VALUE;	
	}
	
	
	
	
		
	public double getDelProbOf(Integer other)
	{
		if (delProbOf.containsKey(other))
			return delProbOf.get(other);
		
		else return 0; 
	}
	
	
	
		
	protected int startTransfer(Message m, Connection con)
	{
		int retVal;
		if (!con.isReadyForTransfer())
		{
			return TRY_LATER_BUSY;
		}
		retVal = con.startTransfer(getHost(), m);
		
		if (retVal == DENIED_OLD && con.getOtherNode(getHost())== m.getTo())
		{
			/* final recipient has already received the msg -> delete it */
			
			if(backUps.containsKey(m.getId()))
				backUps.remove(m.getId());
			if(!isSending(m.getId()))
				deleteMessage(m.getId(), false);
			
			return retVal;
		}
		
		if (retVal == RCV_OK)
		{ // started transfer
			addToSendingConnections(con);
			if(con.getOtherNode(getHost())== m.getTo())
			{
				if(backUps.containsKey(m.getId()))
					backUps.remove(m.getId());
				if(!isSending(m.getId()))
					deleteMessage(m.getId(), false);
				
				return retVal;
			}
			
			if(backUps.containsKey(m.getId()))
			{
				int t=backUps.get(m.getId());
				if(t==nrOfBackUp)
					getMessage(m.getId()).setAsBackUp();
				
				backUps.put(m.getId(),(t-1));
				
				if(backUps.get(m.getId())<= 0)
				{
					if(!isSending(m.getId()))
						deleteMessage(m.getId(), false);
					backUps.remove(m.getId());
				}
				return retVal;
			}
			
			
			/*just forwarding...*/
			
			deleteMessage(m.getId(), false);
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
		
		Integer to;
		
		NeihbComparator(Integer to){
			this.to=to;		
			
		}

		public int compare(DTNHost h1,DTNHost h2) {
			// delivery probability of tuple1's message with tuple1's connection
			double p1 = ((Scar)h1.getRouter()).getDelProbOf(to);
			// -"- tuple2...
			double p2 = ((Scar)h2.getRouter()).getDelProbOf(to);

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
		RoutingInfo ri = new RoutingInfo("Hops--- ");
		ri.addMoreInfo(new RoutingInfo(hopsToAll));
		top.addMoreInfo(ri);
		
		RoutingInfo r2 = new RoutingInfo("ackedMessages--- ");
		r2.addMoreInfo(new RoutingInfo(ackedMessages));
		top.addMoreInfo(r2);
		
		for(Message m:this.getMessageCollection())
			top.addMoreInfo(new RoutingInfo(m.getId()+"Master->"+m.isMaster()));
		
		return top;
	}
		
	public MessageRouter replicate() {
		Scar r = new Scar(this);
		return r;
	}
	
}
