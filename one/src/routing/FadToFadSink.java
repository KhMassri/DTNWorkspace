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




public class FadToFadSink extends DTNRouter {

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

	private double delProb=0;
	private double lastUpdate;
	List<DTNHost> neighb;
	private String ftStr="FaultToleranceValue";
	private Comparator<DTNHost> neighbComparator;
	private Comparator<Message> msgComparator;
	private int coding = 0;


	public FadToFadSink(Settings settings) 
	{
		super(settings);
		Settings fadSettings = new Settings(FAD_TO_SINK_NS);
		alpha= fadSettings.getDouble(ALPHA);
		gamma= fadSettings.getDouble(GAMMA);
		coding = fadSettings.getInt("coding");

		secondsForTimeOut = fadSettings.getInt(SECONDS_FOR_TIME_OUT);

	}


	/**
	 * Copy constructor. Creates a new router based on the given prototype.
	 * @param r The router prototype where setting values are copied from
	 */
	protected FadToFadSink(FadToFadSink r) 
	{
		super(r);
		this.alpha=r.alpha;
		this.gamma=r.gamma;
		this.coding = r.coding;
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
				if((this.getDelProb()>=threshold *(((FadToFadSink)h.getRouter()).getDelProb())))
					continue;

				con=getConOf(h);
				if(con==null||h.getRouter().hasMessage(m.getId())) //FIXME hasMessage or has its code.
					continue;
				
				String[] ids = m.getId().split("&");
				if(ids.length>1)
					if(h.getRouter().hasMessage(ids[0])||h.getRouter().hasMessage(ids[1])||h.getRouter().hasMessage(ids[1]+"&"+ids[0])) //FIXME hasMessage or has its code.
						continue;

				curFt=(Double)m.getProperty(ftStr);
				newFt=1-(1-curFt)*(1-delProb);
				Message msg=m.replicate();
				msg.updateProperty(ftStr, newFt);
				msg.setAsBackUp();

				if(startTransfer(msg, con)!=RCV_OK)
					continue;
				

				if(m.isMaster()){
					m.setAsBackUp();
					m.updateProperty(ftStr, 1-(1-curFt)*(1-((FadToFadSink)h.getRouter()).getDelProb()));
				}
				else
					this.deleteMessage(m.getId(), false);
				
				delProb=(1-alpha)*delProb + alpha*((FadToFadSink)h.getRouter()).getDelProb();
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

		Collection<Message> msgCollection = getMessageCollection();
		List<Message> messages=new ArrayList<Message>();
		messages.addAll(msgCollection);
		Collections.sort(messages,msgComparator);
		Message oldest = null;
		if(messages.size()>0)
			oldest = messages.get(messages.size()-1);

		if(oldest==null)
			return super.checkReceiving(m);

		if((Double)oldest.getProperty(ftStr)<=(Double)m.getProperty(ftStr))
			//if(oldest.getHopCount()<m.getHopCount())
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
			if(other.isSink()||(((FadToFadSink)other.getRouter()).getDelProb()>0.9)&&!isSending(m.getId()))
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
		//makeRoomForNewMessage(msg.getSize());

		msg.setTtl(this.msgTtl);
		msg.addProperty(ftStr, new Double(0));
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
				//if(current.getHopCount()>1)
				if((Double)current.getProperty(ftStr) > 0.1 && !current.isMaster())
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
			//int p1 = m1.getHopCount(); 
			//int p2 = m2.getHopCount();

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
	public double getDelProb(){return delProb;}

	private class NeihbComparator implements Comparator<DTNHost> {

		public int compare(DTNHost h1,DTNHost h2) {
			// delivery probability of tuple1's message with tuple1's connection
			double p1 = ((FadToFadSink)h1.getRouter()).getDelProb();
			// -"- tuple2...
			double p2 = ((FadToFadSink)h2.getRouter()).getDelProb();

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


	@Override
	public MessageRouter replicate() {
		FadToFadSink r = new FadToFadSink(this);
		return r;
	}

}
