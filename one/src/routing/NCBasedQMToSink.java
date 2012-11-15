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

/**Author Khalil Massri
 
 * @version 1.0
 */




public class NCBasedQMToSink extends DTNRouter {

	/** Router's setting namespace ({@value})*/
	public static final String NCBasedQMToSink_NS = "NCBasedQMToSink";
	/**  */
	


	/** the value of nrof seconds for time out -setting */
	
	List<DTNHost> neighb;
	
	private Comparator<Message> msgComparator;
	private int coding = 0;
	private String FTCStr="FTCValue";

	public NCBasedQMToSink(Settings settings) 
	{
		super(settings);
		Settings fadSettings = new Settings(NCBasedQMToSink_NS);
		
		coding = fadSettings.getInt("coding");

		

	}


	/**
	 * Copy constructor. Creates a new router based on the given prototype.
	 * @param r The router prototype where setting values are copied from
	 */
	protected NCBasedQMToSink(NCBasedQMToSink r) 
	{
		super(r);
		
		this.coding = r.coding;
		
		neighb=new ArrayList<DTNHost>();
		
		msgComparator =new MessageComparator();


	}

	public void changedConnection(Connection con) 
	{
		DTNHost other=con.getOtherNode(getHost());
		if(con.isUp())
		{
			if(other.getRouter().hello().equals("DTNRouter"))
				neighb.add(other);

			


		}

		else if(other.getRouter().hello().equals("DTNRouter"))
			neighb.remove(other);

	


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
				con=getConOf(h);
				if(con==null||h.getRouter().hasMessage(m.getId())) //FIXME hasMessage or has its code.
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


/*
	protected int checkReceiving(Message m) {
		
		 // if the incomming msg has hopCount larger than the oldest msg in
		 // the buffer then reject it 
		 
		Collection<Message> msgCollection = getMessageCollection();
		List<Message> messages=new ArrayList<Message>();
		messages.addAll(msgCollection);
		Collections.sort(messages,msgComparator);
		Message oldest = null;
		if(messages.size()>0)
			oldest = messages.get(messages.size()-1);

		if(oldest==null)
			return super.checkReceiving(m);

		if(oldest.getHopCount() < m.getHopCount())
			return MessageRouter.DENIED_NO_SPACE;

		return super.checkReceiving(m);


	}*/

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
			if(other.isSink() && !isSending(m.getId()))
				this.deleteMessage(m.getId(), false);
			return retVal;

		}

		return retVal;
	}


	

	public boolean createNewMessage(Message msg) {
		//makeRoomForNewMessage(msg.getSize());

		msg.setTtl(this.msgTtl);
		msg.addProperty(FTCStr, new Integer(1));
		return super.createNewMessage(msg);

	}


	protected boolean makeRoomForMessage(int size){
		if (size > this.getBufferSize()) {
			return false; // message too big for the buffer
		}

		/*Encode all possible messages*/
		

		
		/* delete messages from the buffer until there's enough space */
		if(this.getFreeBufferSize() < size && coding == 1)
			encode(size);
		
		while (this.getFreeBufferSize() < size) {

			Message m = getOldestMessage(true); // don't remove msgs being sent

			if (m == null) {
				return false; // couldn't remove any more messages
			}			

			/* delete message from the buffer as "drop" */

			deleteMessage(m.getId(),true);
			//freeBuffer += m.getSize();
		}

		return true;
	}


	protected Message getOldestMessage(boolean excludeMsgBeingSent) {


		Collection<Message> messagecol = this.getMessageCollection();
		List<Message> messages = new ArrayList<Message>();

		for (Message m : messagecol) {	
			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
			}
			messages.add(m);
		}

		Collections.sort(messages,msgComparator);
		return messages.get(messages.size()-1);	
	}



	private void encode(int size){
		
		int i;

		do
		{
			Collection<Message> messagecol = this.getMessageCollection();
			List<Message> messages = new ArrayList<Message>();


			for (Message m : messagecol) {	
				if (isSending(m.getId())) {
					continue; // skip the message(s) that router is sending
				}
				messages.add(m);
			}

			Collections.sort(messages,msgComparator);
			Message oldest=null;
			Message old = null;
			i=messages.size();

			while(--i >=  0)
			{
				Message current = messages.get(i);
				if(current.getId().contains("&"))
					continue;
				if((Integer)current.getProperty(FTCStr) > 2)
					if(oldest == null)
						oldest =current;
					else
					{
						old = current;
						this.removeFromMessages(old.getId());
						old.setID(old.getId()+"&"+oldest.getId());
						this.addToMessages(old, false);
						this.deleteMessage(oldest.getId(), false);
						break;
					}
			}

		}while(this.getFreeBufferSize()<size && i>0);

	}

	private class MessageComparator implements Comparator<Message> {

		public int compare(Message m1,Message m2) {
			// delivery probability of tuple1's message with tuple1's connection
			int p1 = (Integer)m1.getProperty(FTCStr);
			int p2 = (Integer)m2.getProperty(FTCStr);
			
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
			ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f",m.getId(), m.getHopCount())));
		}

		top.addMoreInfo(ri);
		return top;
	}

	public boolean hasMessage(String id) {
		if(id.contains("&"))
		{
			String[] ids = id.split("&");
			for(Message m : this.getMessageCollection())
				if(m.getId().contains(ids[0])||m.getId().contains(ids[1]))
					return true;
		}

		return super.hasMessage(id);

	}

	public Message messageTransferred(String id, DTNHost from) {
		Message msg = super.messageTransferred(id, from);
		if(msg.getProperty(FTCStr)==null)
			msg.addProperty(FTCStr, new Integer(1));
		else
			msg.updateProperty(FTCStr,(Integer)msg.getProperty(FTCStr)+1 );
		
		return msg;
	}
	

	@Override
	public MessageRouter replicate() {
		NCBasedQMToSink r = new NCBasedQMToSink(this);
		return r;
	}

}
