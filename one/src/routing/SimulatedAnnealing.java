package routing;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import routing.maxprop.MeetingProbabilitySet;



import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;

/**Author Khalil Massri
 * Implementation of SimulatedAnnealing router 
 * @version 1.0
 */




public class SimulatedAnnealing extends DTNRouter {

	/** Router's setting namespace ({@value})*/
	public static final String SimulatedAnnealing_NS = "SimulatedAnnealing";
	/**  */
	public static final String ALPHA = "alpha";
	private double alpha;
	private double threshold;
	public static final String SECONDS_FOR_TIME_OUT ="secondsForTimeOut";
	/** the value of nrof seconds for time out -setting */
	private int secondsForTimeOut;
	private Map<DTNHost, Double> delProb;
	private Map<DTNHost, Double> lastUpdate;



	private MeetingProbabilitySet probs;


	public SimulatedAnnealing(Settings settings) 
	{
		super(settings);
		Settings SimulatedAnnealingSettings = new Settings(SimulatedAnnealing_NS);
		alpha= SimulatedAnnealingSettings.getDouble(ALPHA);
		secondsForTimeOut = SimulatedAnnealingSettings.getInt(SECONDS_FOR_TIME_OUT);

	}


	/**
	 * Copy constructor. Creates a new router based on the given prototype.
	 * @param r The router prototype where setting values are copied from
	 */
	protected SimulatedAnnealing(SimulatedAnnealing r) 
	{
		super(r);
		this.probs = new MeetingProbabilitySet(150, 1);

		this.alpha=r.alpha;
		threshold=(2-2*alpha)/(2-alpha);
		this.secondsForTimeOut = r.secondsForTimeOut;
		delProb=new HashMap<DTNHost,Double>();
		lastUpdate=new HashMap<DTNHost,Double>();


	}

	public void changedConnection(Connection con) 
	{
		DTNHost otherHost=con.getOtherNode(getHost());
		SimulatedAnnealing other = (SimulatedAnnealing)otherHost.getRouter();

		if(con.isUp()&&other.hello().equals("DTNRouter"))
		{
			delProb.put(otherHost,(1-alpha)*getDelProbOf(otherHost)+alpha);
			lastUpdate.put(otherHost,SimClock.getTime());

			for(Message m:this.getMessageCollection())
				m.incTemp();

					if(con.isInitiator(getHost()))
					{
						probs.updateMeetingProbFor(otherHost.getAddress());
						other.probs.updateMeetingProbFor(getHost().getAddress());
					}



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


		List<Message> messages=new ArrayList<Message>();
		Collection<Message> msgCollection = getMessageCollection();

		for(Connection con:getConnections())

		{			
			DTNHost oth=con.getOtherNode(getHost());
			if(!oth.getRouter().hello().equals("DTNRouter"))
				continue;

			SimulatedAnnealing other=(SimulatedAnnealing)oth.getRouter();

			for (Message m : msgCollection) {
				/* skip messages that the other host has or that have
				 * passed the other host */
				if (other.hasMessage(m.getId())) {
					continue; 
				}
				messages.add(m);
			}			


			if (messages.size() == 0) {
				return null;
			}

			/* sort the messages */ 
			Collections.sort(messages, new MessageComparator());


			for(Message m:messages)
			{
				DTNHost to = m.getTo();

//				if (getDelProbOf(to) < threshold *(other.getDelProbOf(to))) 
				if(this.probs.getProbFor(to.getAddress()) < other.probs.getProbFor(to.getAddress()))
				{
					if(startTransfer(m, con)!=RCV_OK)
						continue;

					if(!(m.getState().equals("MAX")))
						this.deleteMessage(m.getId(), false);
					// local maximum case,,, keep a LM replica

					else{
						//System.out.println("Rep"+(this.probs.getProbFor(to.getAddress())- other.probs.getProbFor(to.getAddress())));
						m.setState("LM");
					}
					
					return con;	
				}

				// msg in the cooling process

				else if(m.getState().equals("DOWN") || m.getState().equals("MAX"))
				{
					
					
					double d = Math.exp(
							(		other.probs.getProbFor(to.getAddress()) - 
									this.probs.getProbFor(to.getAddress())
									)	/ (m.getTemp())
							);
//
//					System.out.println(other.probs.getProbFor(to.getAddress()) - 
//							this.probs.getProbFor(to.getAddress())
//							+" exp "+d);


//				if(Math.exp((other.getDelProbOf(to)-this.getDelProbOf(to)) / (m.getTemp())) >= Math.random())
					if(d >= Math.random())

						if(startTransfer(m, con)!=RCV_OK)
							continue;
						
						if(m.getState().equals("DOWN"))
							this.deleteMessage(m.getId(), false);
						// local maximum case,,, keep a LM replica

						else{
						//	System.out.println("Rep"+(this.probs.getProbFor(to.getAddress())- other.probs.getProbFor(to.getAddress())));
							m.setState("LM");
						}
												
					return con;

				}
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

		
			int recvCheck = super.checkReceiving(m); 
			
			if (recvCheck == RCV_OK) {
				/* don't accept a message that has already traversed this node */
				if (m.getState().equals("DOWN") && m.getHops().contains(getHost())) {
					recvCheck = DENIED_OLD;
				}
			}
			
			return recvCheck;
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
			if((other==m.getTo() && !isSending(m.getId())))
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



	protected Message getOldestMessage(boolean excludeMsgBeingSent) {

		Collection<Message> msgCollection = getMessageCollection();
		List<Message> messages=new ArrayList<Message>();
		messages.addAll(msgCollection);
		Collections.sort(messages,new MessageComparator());
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

		public int compare(Message msg1, Message msg2) {
			int hopc1 = msg1.getHopCount();
			int hopc2 = msg2.getHopCount();
			//System.out.println(msg1+"   "+msg2);

			if (msg1 == msg2) {
				return 0;
			}


			/*  one with lower hop count should
			 * be sent first */

			if (hopc1 == hopc2) {
				return 0;// compareByQueueMode(msg1, msg2);
			}
			else {
				return hopc1 - hopc2;	
			}


		}
	}	


	@Override
	public RoutingInfo getRoutingInfo() {

		RoutingInfo top = super.getRoutingInfo();



		RoutingInfo ri1 = new RoutingInfo("DelProb =-->\n" +probs);


		RoutingInfo ri = new RoutingInfo("Message State-->");
		for (Message m:this.getMessageCollection()) {
			ri.addMoreInfo(new RoutingInfo(String.format("%s : %s : %f", 
					m.getId(),m.getState(),m.getTemp())));
		}

		top.addMoreInfo(ri1);
		top.addMoreInfo(ri);
		return top;
	}

	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);
		
	//	System.out.println(m+"  "+m.getState()+"  tmp" + m.getTemp()+"  from  "+from.getAddress()+"  to  " + this.getHost().getAddress()+ " is delivered  "+ (this.getHost().getAddress()==m.getTo().getAddress()));


		if(m.getState().equals("UP"))
			m.setTemp(0);
		
//		m.getTemp()*0.5 * (
//				(((SimulatedAnnealing)(from.getRouter())).probs.getProbFor(m.getTo().getAddress())) / 
//				(((SimulatedAnnealing)(this)).probs.getProbFor(m.getTo().getAddress()))
//				)
//				);
		else if(m.getState().equals("DOWN"))
			m.decTemp();
		else if(m.getState().equals("MAX"))
			m.setState("DOWN");


		return m;
	}


	@Override
	public MessageRouter replicate() {
		SimulatedAnnealing r = new SimulatedAnnealing(this);
		return r;
	}

}
