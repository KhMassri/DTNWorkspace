package routing;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

/**Author Khalil Massri
 * Implementation of FAD router as described in 
 * <I>Delay/Fault-Tolerant Mobile Sensor Network (DFT-MSN): A New Paradigm
	for Pervasive Information Gathering 
 *Yu Wang, Hongyi Wu</I>
 * @version 1.0
 */




public class FadToSink extends DTNRouter {

	/** Router's setting namespace ({@value})*/
	public static final String FAD_TO_SINK_NS = "FadToSink";
	/**  */
	public static final String ALPHA = "alpha";
	public static final String GAMMA = "gamma";

	private double alpha;
	private double gamma;

	private double threshold;
	
public static final String SECONDS_FOR_TIME_OUT ="secondsForTimeOut";
	
	

	/** the value of nrof seconds for time out -setting */
	private int secondsForTimeOut;
	
	private double delProb=0;
	private double lastUpdate;
	List<DTNHost> neighb;
	private String ftStr="FaultToleranceValue";
	private Comparator<DTNHost> neighbComparator;
	private Comparator<Message> msgComparator;
	
	
	public FadToSink(Settings settings) 
	{
		super(settings);
		Settings fadSettings = new Settings(FAD_TO_SINK_NS);
		 alpha= fadSettings.getDouble(ALPHA);
		 gamma= fadSettings.getDouble(GAMMA);

		 secondsForTimeOut = fadSettings.getInt(SECONDS_FOR_TIME_OUT);
		
		}
	
	
	/**
	 * Copy constructor. Creates a new router based on the given prototype.
	 * @param r The router prototype where setting values are copied from
	 */
	protected FadToSink(FadToSink r) 
	{
		super(r);
		this.alpha=r.alpha;
		this.gamma=r.gamma;
		threshold=(2-2*alpha)/(2-alpha);
		this.secondsForTimeOut = r.secondsForTimeOut;
		neighb=new ArrayList<DTNHost>();
		this.lastUpdate = SimClock.getTime();
		neighbComparator = new NeihbComparator();
		msgComparator =new MessageComparator();
	
		
	}
	
	public void changedConnection(Connection con) 
	{
		DTNHost other=con.getOtherNode(getHost());
		if(con.isUp())
		{
			if(other.getRouter().hello().equals("DTNRouter"))
				neighb.add(other);
			
			else if(other.isSink())
			{
				delProb=(1-alpha)*delProb + alpha;
				this.lastUpdate = SimClock.getTime();
				}
			
				
			}
		
		else if(other.getRouter().hello().equals("DTNRouter"))
			neighb.remove(other);
			
		Collections.sort(neighb,neighbComparator);
		
		
		}
	
	
	@Override
	public void update() {
		super.update();
		timeOutUpdate();
		
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		if(this.tryMessagesForSinks() != null)
			return;
		
		tryOtherMessages();		
	}
	
	
	
	private Connection tryOtherMessages() {
		
		Collection<Message> msgCollection = getMessageCollection();
		if(msgCollection.size()==0)return null;
				
		if(neighb.size()==0)return null;
		
		
		
		List<Message> messages=new ArrayList<Message>();
		messages.addAll(msgCollection);
		Collections.sort(messages,msgComparator);
		
		double curFt;
		double newFt;
		Connection con=null;
		
		for(Message m:messages){
				
		for(DTNHost h:neighb){
			if((this.getDelProb()>=threshold *(((FadToSink)h.getRouter()).getDelProb())))
				continue;
 
			con=getConOf(h);
			 if(con==null||h.getRouter().hasMessage(m.getId()))
				 continue;
			 
			 curFt=(Double)m.getProperty(ftStr);
			 newFt=1-(1-curFt)*(1-delProb);
			 Message msg=m.replicate();
			 msg.updateProperty(ftStr, newFt);
			 
			 if(startTransfer(msg, con)!=RCV_OK)
				 continue;
			 
			 m.updateProperty(ftStr, 1-(1-curFt)*(1-((FadToSink)h.getRouter()).getDelProb()));
			 delProb=(1-alpha)*delProb + alpha*((FadToSink)h.getRouter()).getDelProb();
			 this.lastUpdate = SimClock.getTime();
			 return con;

			}
		
		}
		return con;
	}	
	
	
	
	private Connection getConOf(DTNHost h) {
		
		
		for(Connection con:getConnections())
			if(con.getOtherNode(getHost()).equals(h))
				return con;
		
		return null;
	}


	
	protected int checkReceiving(Message m) {
		if(m.getProperty(ftStr)==null)
			return super.checkReceiving(m);
		
		Message old=this.getOldestMessage(true);
		if(old==null)
			return super.checkReceiving(m);
		if((Double)old.getProperty(ftStr)<(Double)m.getProperty(ftStr))
			return MessageRouter.DENIED_NO_SPACE;
		
		return super.checkReceiving(m);
		
		
	}
	protected int startTransfer(Message m, Connection con)
	{
		int retVal;
		if (!con.isReadyForTransfer())
		{
			return TRY_LATER_BUSY;
			}
		retVal = con.startTransfer(getHost(), m);
		DTNHost other= con.getOtherNode(getHost());
		
		if (retVal == DENIED_OLD && other.isSink())
		{
			/* final recipient has already received the msg -> delete it */
			this.deleteMessage(m.getId(), false);
			return retVal;
			}
		
		if (retVal == RCV_OK)
		{ // started transfer
			addToSendingConnections(con);
			if(other.isSink()||(((FadToSink)other.getRouter()).getDelProb()>0.9)&&!isSending(m.getId()))
				this.deleteMessage(m.getId(), false);
			return retVal;
	
			}
		
		return retVal;
		}
	
	
	private void timeOutUpdate() {
		
		
		if (SimClock.getTime() - this.lastUpdate < secondsForTimeOut)
			return;
		
		for(Connection con:this.getConnections())
			if(con.getOtherNode(getHost()).isSink()){
				delProb=(1-alpha)*delProb + alpha;
				this.lastUpdate = SimClock.getTime();
				return;
				}
		
		delProb=(1-alpha)*delProb;
		this.lastUpdate = SimClock.getTime();
	}
	
	
	public boolean createNewMessage(Message msg) {
		makeRoomForNewMessage(msg.getSize());

		msg.setTtl(this.msgTtl);
		msg.addProperty(ftStr, new Double(0));
		return super.createNewMessage(msg);
		
	}
	
	protected Message getOldestMessage(boolean excludeMsgBeingSent) {
		
		Collection<Message> msgCollection = getMessageCollection();
		List<Message> messages=new ArrayList<Message>();
		messages.addAll(msgCollection);
		Collections.sort(messages,msgComparator);
		Message old=null;
		
		for(int i=messages.size()-1;i>0;i--){
			old = messages.get(i);
			if (excludeMsgBeingSent && isSending(old.getId()))
				continue;
			return old;
		}
		
		return old;	
	}
	
	
	
	
	public double getDelProb(){return delProb;}
	
	private class NeihbComparator implements Comparator<DTNHost> {

	public int compare(DTNHost h1,DTNHost h2) {
		// delivery probability of tuple1's message with tuple1's connection
		double p1 = ((FadToSink)h1.getRouter()).getDelProb();
		// -"- tuple2...
		double p2 = ((FadToSink)h2.getRouter()).getDelProb();

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
			double p1 = (Double)m1.getProperty(ftStr);
			double p2 = (Double)m2.getProperty(ftStr);
			
			if (p1>p2) {
				return 1;
			}
			else if(p1<p2){
				return -1;
			}
			else return 0;
		}
	}	
	
	@Override
	public RoutingInfo getRoutingInfo() {
		
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri1 = new RoutingInfo("DelProb =--> "+delProb);

		RoutingInfo ri = new RoutingInfo("Messages FT-->");
		
		for (Message m:this.getMessageCollection()) {
			ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f", 
					m.getId(), m.getProperty(ftStr))));
		}
		
		top.addMoreInfo(ri1);
		top.addMoreInfo(ri);
		return top;
	}
	
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);
		if(m.getProperty(ftStr)==null)
			m.addProperty(ftStr, new Double(0));
		return m;
	}
	
	
	@Override
	public MessageRouter replicate() {
		FadToSink r = new FadToSink(this);
		return r;
	}

}
