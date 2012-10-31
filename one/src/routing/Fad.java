package routing;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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




public class Fad extends DTNRouter {

	/** Router's setting namespace ({@value})*/
	public static final String FAD_TO_SINK_NS = "Fad";
	/**  */
	public static final String ALPHA = "alpha";
	public static final String GAMMA = "gamma";

	private double alpha;
	private double gamma;

	private double threshold;
	
public static final String SECONDS_FOR_TIME_OUT ="secondsForTimeOut";
	
	

	/** the value of nrof seconds for time out -setting */
	private int secondsForTimeOut;
	
	private Map<DTNHost, Double> delProb;
	private Map<DTNHost, Double> lastUpdate;
	
	
	private String ftStr="FaultToleranceValue";
	private Comparator<Message> msgComparator;
	
	
	public Fad(Settings settings) 
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
	protected Fad(Fad r) 
	{
		super(r);
		this.alpha=r.alpha;
		this.gamma=r.gamma;
		threshold=(2-2*alpha)/(2-alpha);
		this.secondsForTimeOut = r.secondsForTimeOut;
		msgComparator =new MessageComparator();
		delProb=new HashMap<DTNHost,Double>();
		lastUpdate=new HashMap<DTNHost,Double>();
		
	
		
	}
	
	public void changedConnection(Connection con) 
	{
		DTNHost other=con.getOtherNode(getHost());
		
		/* Skip Generator router*/
		if(other.getRouter().hello().equals("GeneratorRouter"))
			return;
		
		if(con.isUp()&&other.getRouter().hello().equals("DTNRouter"))
		{
			delProb.put(other,(1-alpha)*getDelProbOf(other)+alpha);
			lastUpdate.put(other,SimClock.getTime());
			}
		}
	
	
	@Override
	public void update() {
		
		super.update();
		
		timeOutUpdate();
		
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		if(this.exchangeDeliverableMessages() != null)
			return;
		
		tryOtherMessages();		
	}
	
	
	
	private Connection tryOtherMessages() {
		
		Collection<Message> msgCollection = getMessageCollection();
		if(msgCollection.size()==0)return null;
				
		/*
		 * sorting the messages according to the fault tolerance value
		 */
		List<Message> messages=new ArrayList<Message>();
		messages.addAll(msgCollection);
		
		
		
		Collections.sort(messages,msgComparator);
		
		double curFt;
		double newFt;
				
	
		
			
		for(Message m:messages){
			
			
			DTNHost to = m.getTo();
			
			for(Connection con:getConnections()){
				
				DTNHost oth=con.getOtherNode(getHost());
				/* Skip Generator router*/
				if(oth.getRouter().hello().equals("GeneratorRouter"))
					continue;
				
				if(!oth.getRouter().hello().equals("DTNRouter"))
				continue;
				
				Fad other=(Fad)oth.getRouter();
				
			
			if (other.hasMessage(m.getId()) ||  
					(getDelProbOf(to)>=threshold *(other.getDelProbOf(to)))) {
				continue; 
			}
			
			 // the current fault tolerance value of the message.
			
			 curFt=(Double)m.getProperty(ftStr);
			 
			 if(curFt>=gamma)   // gamma: fault tolerant threshold
				 break;
			 
			 // the new fault tolerance value for the forwarded msg
			 newFt=1-(1-curFt)*(1-getDelProbOf(to));
			 Message msg=m.replicate();
			 msg.updateProperty(ftStr, newFt);
			 
			 if(startTransfer(msg, con)!=RCV_OK)
				 continue;
			 
			 // updating the FT value after forwarding
			 
			 m.updateProperty(ftStr, 1-(1-curFt)*(1-other.getDelProbOf(to)));
			 
			 // updating the delivery probability after transfer occurs
			 
			 delProb.put(to,(1-alpha)*getDelProbOf(to) + alpha*other.getDelProbOf(to));
			 lastUpdate.put(to,SimClock.getTime());
			 return con;

			}
		
		}
		return null;
	}	
	
	
	
	private double getDelProbOf(DTNHost to) {
		
		if(delProb.containsKey(to))
			return delProb.get(to);
		
		return 0;
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
		
		if (retVal == DENIED_OLD && other == m.getTo())
		{
			/* final recipient has already received the msg -> delete it */
			this.deleteMessage(m.getId(), false);
			return retVal;
			}
		
		if (retVal == RCV_OK)
		{ // started transfer
			addToSendingConnections(con);
			if((other==m.getTo()||
					(((Fad)other.getRouter()).getDelProbOf(m.getTo())>0.9))&&
					!isSending(m.getId()))
				this.deleteMessage(m.getId(), false);
			return retVal;
	
			}
		
		return retVal;
		}
	
	
	private void timeOutUpdate() {
		
		for(Connection con:this.getConnections())
		{
			DTNHost other = con.getOtherNode(getHost());
			
			if(other.getRouter().hello().equals("DTNRouter"))
				if (SimClock.getTime() - lastUpdate.get(other) >= secondsForTimeOut)
					{
					delProb.put(other,(1-alpha)*getDelProbOf(other)+alpha);
					lastUpdate.put(other,SimClock.getTime());
					
					}
			
				}
				
		for(Entry<DTNHost, Double> e:delProb.entrySet()){
			DTNHost node=e.getKey();
			if (SimClock.getTime() - lastUpdate.get(node) < secondsForTimeOut)
			continue;
			delProb.put(node,(1-alpha)*getDelProbOf(node));
			lastUpdate.put(node,SimClock.getTime());
			}
		
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
		
		//top.addMoreInfo(ri1);
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
		Fad r = new Fad(this);
		return r;
	}

}
