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

public class FuzzySpray extends DTNRouter {

	/** Router's setting namespace ({@value})*/
	public static final String FUZZYSPRAY_NS = "FuzzySpray";
	/**  */

	private Set<String> ackedMessageIds;


	private String FTCStr="FTCValue";
	private Comparator<Message> msgComparator;
	private RulesBase comFuzzy;
	
	
	public FuzzySpray(Settings settings) 
	{
		super(settings);
		Settings fuzzySpraySettings = new Settings(FUZZYSPRAY_NS);
		 	
		}
	
	
	/**
	 * Copy constructor. Creates a new router based on the given prototype.
	 * @param r The router prototype where setting values are copied from
	 */
	protected FuzzySpray(FuzzySpray r) 
	{
		super(r);
		ackedMessageIds=new HashSet<String>();
		msgComparator =new MessageComparator();
		comFuzzy=new RulesBase();

	}
	
	public void changedConnection(Connection con) 
	{
		DTNHost other=con.getOtherNode(getHost());
		
		/* Skip Generator router*/
		if(other.getRouter().hello().equals("GeneratorRouter"))
			return;
		
		
		if(con.isUp()&& other.getRouter().hello().equals("DTNRouter"))
			{
				if (con.isInitiator(getHost()))
				{
				FuzzySpray otherRouter=(FuzzySpray)other.getRouter();
				this.ackedMessageIds.addAll(otherRouter.ackedMessageIds);
				otherRouter.ackedMessageIds.addAll(this.ackedMessageIds);
				deleteAckedMessages();
				otherRouter.deleteAckedMessages();
				}
			}
		
		
		
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
		
		if(this.exchangeDeliverableMessages() != null)
			return;
		
		tryOtherMessages();		
	}
	
	
	
	private Connection tryOtherMessages() {
		
		Collection<Message> msgCollection = getMessageCollection();
		if(msgCollection.size()==0)return null;
				
		
		
		List<Message> messages=new ArrayList<Message>();
		messages.addAll(msgCollection);
		Collections.sort(messages,msgComparator);
		
		
		
		
		for(Message m:messages){
				
		for(Connection con:getConnections()){
			DTNHost oth=con.getOtherNode(getHost());
			
			/* Skip Generator router*/
			if(oth.getRouter().hello().equals("GeneratorRouter"))
				continue;
			
			if(!oth.getRouter().hello().equals("DTNRouter"))
				continue;
			FuzzySpray other = (FuzzySpray)oth.getRouter();
			
			if(other.isTransferring())
				continue;
 
			 if(other.hasMessage(m.getId()))
				 continue;
			 
			 if(startTransfer(m, con)!=RCV_OK)
				 continue;
			 m.updateProperty(FTCStr,(Integer)m.getProperty(FTCStr)+1 );
			 return con;

			}
		
		}
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
		
		if (retVal == DENIED_OLD && m.getTo()==other)
		{
			/* final recipient has already received the msg -> delete it */
			ackedMessageIds.add(m.getAppID());
			this.deleteMessage(m.getId(), false);
			return retVal;
			}
		
		if (retVal == RCV_OK)
		{ // started transfer
			addToSendingConnections(con);
			if(m.getTo()==other && !isSending(m.getId())){
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

		RoutingInfo ri = new RoutingInfo("Messages FT-->");
		
		for (Message m:this.getMessageCollection()) {
			int ftc2 = (Integer)m.getProperty(FTCStr);
			int ms2 = m.getSize();
			comFuzzy.setInput(ftc2, ms2);
			double p2= 1 - comFuzzy.getOutput();
			ri.addMoreInfo(new RoutingInfo(m.getId()+"  "+ m.getProperty(FTCStr)+ " "+ p2));
		}
		
		top.addMoreInfo(ri);
		return top;
	}
	
	@Override
	public MessageRouter replicate() {
		FuzzySpray r = new FuzzySpray(this);
		return r;
	}	
	
	
	
	
	
	
	
	
	

	
	
}