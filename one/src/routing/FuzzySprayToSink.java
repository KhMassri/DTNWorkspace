package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import fuzzy.RulesBase;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;

/**Author Khalil Massri
 * Implementation of Fuzzy Spray router as described in 
 * <I>Fuzzy-Spray: Efficient Routing in Delay Tolerant Ad-hoc Network
	Based on Fuzzy Decision Mechanism</I> by
 * A. Mathurapoj, C. Pornavalai.
 * @version 1.0
 */

public class FuzzySprayToSink extends DTNRouter {

	/** Router's setting namespace ({@value})*/
	public static final String FUZZYSPRAY_NS = "FuzzySprayToSink";
	/**  */

	private Set<String> ackedMessageIds;

	
	
	

	
	
	List<DTNHost> neighb;
	private String FTCStr="FTCValue";
	private Comparator<Message> msgComparator;
	private RulesBase comFuzzy;
	
	
	public FuzzySprayToSink(Settings settings) 
	{
		super(settings);
		Settings fuzzySpraySettings = new Settings(FUZZYSPRAY_NS);
		 	
		}
	
	
	/**
	 * Copy constructor. Creates a new router based on the given prototype.
	 * @param r The router prototype where setting values are copied from
	 */
	protected FuzzySprayToSink(FuzzySprayToSink r) 
	{
		super(r);
		neighb=new ArrayList<DTNHost>();
		ackedMessageIds=new HashSet<String>();
		msgComparator =new MessageComparator();
		comFuzzy=new RulesBase();

	}
	
	public void changedConnection(Connection con) 
	{
		DTNHost other=con.getOtherNode(getHost());
		if(con.isUp()){
			if(other.getRouter().hello().equals("GeneratorRouter"))
				return;
			
			if(other.isSink()){
				ackedMessageIds.addAll(((SinkRouter)other.getRouter()).getSinkedMessages());
				this.deleteAckedMessages();
			
			}
			else
			{
				neighb.add(other);
				if (con.isInitiator(getHost()))
				{
				FuzzySprayToSink otherRouter=(FuzzySprayToSink)other.getRouter();
				this.ackedMessageIds.addAll(otherRouter.ackedMessageIds);
				otherRouter.ackedMessageIds.addAll(this.ackedMessageIds);
				deleteAckedMessages();
				otherRouter.deleteAckedMessages();
				}
			}
		
		}
		else if(!other.isSink())
			if(!other.getRouter().hello().equals("MessageGeneratorRouter"))
				neighb.remove(other);
		
	}
	
	
	private void deleteAckedMessages() {
		
		for(String id:ackedMessageIds)
			if(this.hasMessage(id)&&!this.isSending(id))
				this.deleteMessage(id, false);
			
	}


	@Override
	public void update() {
		super.update();
		
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
		
		
		Connection con=null;
		
		for(Message m:messages){
				
		for(DTNHost h:neighb){
			if(((FuzzySprayToSink)h.getRouter()).isTransferring())
				continue;
 
			con=getConOf(h);
			 if(con==null||h.getRouter().hasMessage(m.getId()))
				 continue;
			 if(startTransfer(m, con)!=RCV_OK)
				 continue;
			 m.updateProperty(FTCStr,(Integer)m.getProperty(FTCStr)+1 );
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
			ackedMessageIds.add(m.getAppID());
			this.deleteMessage(m.getId(), false);
			return retVal;
			}
		
		if (retVal == RCV_OK)
		{ // started transfer
			addToSendingConnections(con);
			if(other.isSink()&&!isSending(m.getId())){
				ackedMessageIds.add(m.getAppID());
				this.deleteMessage(m.getId(), false);
				}
			return retVal;
	
			}
		
		return retVal;
		}
	
	
	public boolean createNewMessage(Message msg) {
		makeRoomForNewMessage(msg.getSize());

		msg.setTtl(this.msgTtl);
		msg.addProperty(FTCStr, new Integer(1));
		return super.createNewMessage(msg);
	}
	
	
	
	public Message messageTransferred(String id, DTNHost from) {
		Message msg = super.messageTransferred(id, from);
		if(msg.getProperty(FTCStr)==null)
			msg.addProperty(FTCStr, new Integer(1));
		else
			msg.updateProperty(FTCStr,(Integer)msg.getProperty(FTCStr)+1 );
		
		return msg;
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

		double p1,p2;
		
		public int compare(Message m1,Message m2) {
			// delivery probability of tuple1's message with tuple1's connection
			int ftc1 = (Integer)m1.getProperty(FTCStr);
			int ms1 = m1.getSize();
			comFuzzy.setInput(ftc1, ms1);
			 p1= 1 - comFuzzy.getOutput();
			
			int ftc2 = (Integer)m2.getProperty(FTCStr);
			int ms2 = m2.getSize();
			comFuzzy.setInput(ftc2, ms2);
			 p2= 1 - comFuzzy.getOutput();
			
			if (p1>p2) {
				return -1;
			}
			else if(p1<p2){
				return 1;
			}
			else return 0;
		}
	}	
	
	@Override
	public RoutingInfo getRoutingInfo() {
		
		RoutingInfo top = super.getRoutingInfo();

		RoutingInfo ri = new RoutingInfo("Messages FT-->");
		
		for (Message m:this.getMessageCollection()) {
			ri.addMoreInfo(new RoutingInfo(m.getId()+"  "+ m.getProperty(FTCStr)));
		}
		
		top.addMoreInfo(ri);
		return top;
	}
	
	@Override
	public MessageRouter replicate() {
		FuzzySprayToSink r = new FuzzySprayToSink(this);
		return r;
	}	
	
	
	
	
	
	
	
	
	

	
	
}