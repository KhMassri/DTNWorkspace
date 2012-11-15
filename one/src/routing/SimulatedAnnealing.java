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
import java.util.Set;

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

	private MeetingProbabilitySet probs;
	double cdc;
	private double lastNUpdate;
	private Set<Integer> Nt;
	private Set<Integer> Nt_1;

	private String FTCStr="FTCValue";


	public SimulatedAnnealing(Settings settings) 
	{
		super(settings);
		Settings SimulatedAnnealingSettings = new Settings(SimulatedAnnealing_NS);
		if(SimulatedAnnealingSettings.contains(ALPHA))
			alpha= SimulatedAnnealingSettings.getDouble(ALPHA);
		else
			alpha = 1;

	}


	/**
	 * Copy constructor. Creates a new router based on the given prototype.
	 * @param r The router prototype where setting values are copied from
	 */
	protected SimulatedAnnealing(SimulatedAnnealing r) 
	{
		super(r);
		this.alpha=r.alpha;
		this.probs = new MeetingProbabilitySet(80, alpha);

		/*to computer change degree of connections*/
		Nt=new HashSet<Integer>();
		Nt_1=new HashSet<Integer>();


	}

	public void changedConnection(Connection con) 
	{
		DTNHost otherHost=con.getOtherNode(getHost());
		MessageRouter oth = otherHost.getRouter();

		if(con.isUp()&&oth.hello().equals("DTNRouter"))

		{
			SimulatedAnnealing other = (SimulatedAnnealing)oth;
			/*
			 * Update meeting probability
			 * */
			if(con.isInitiator(getHost()))
			{
				probs.updateMeetingProbFor(otherHost.getAddress());
				other.probs.updateMeetingProbFor(this.getHost().getAddress());

				/*update the neighbers set*/
				Nt.add(otherHost.getAddress());
				other.Nt.add(this.getHost().getAddress());

				updateCdc();
				other.updateCdc();
				/*
				 *Heating process 
				 * */
				for(Message m:this.getMessageCollection())
					m.incTemp();

				for(Message m:other.getMessageCollection())
					m.incTemp();

			}

		}

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

				double ua=0,ub=0;

				ua = (this.probs.getProbFor(to.getAddress()) + 2*cdc)/3;
				ub = (other.probs.getProbFor(to.getAddress()) + 2*other.cdc)/3;


				if(ua < ub)
				{
					if(startTransfer(m, con)!=RCV_OK)
						continue;

					if(!(m.getState().equals("MAX")))
						this.deleteMessage(m.getId(), false);
					else
						m.setState("LM");

					m.updateProperty(FTCStr,(Integer)m.getProperty(FTCStr)+1 );


					return con;	
				}



				if(m.getState().equals("DOWN") || m.getState().equals("MAX"))
				{
					if (m.getHops().contains(oth))
						continue;


					boolean send = Math.exp((ub - ua)/ (0.1*m.getTemp())) >= Math.random();


					if(send) 
						if(startTransfer(m, con)!=RCV_OK)
							continue;

					if(m.getState().equals("DOWN"))
						this.deleteMessage(m.getId(), false);
					// local maximum case,,, keep a LM replica
					else
						m.setState("LM");

					m.updateProperty(FTCStr,(Integer)m.getProperty(FTCStr)+1 );
					return con;

				}
			}
		}
		return null;
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


	public boolean createNewMessage(Message m) {

		m.addProperty(FTCStr, new Integer(1));

		return super.createNewMessage(m);
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

		if(m.getState().equals("UP"))
			m.setTemp(0);
		else if(m.getState().equals("DOWN"))
			m.decTemp();

		else if(m.getState().equals("MAX")){
			m.setState("DOWN");
			m.decTemp();
			m.updateProperty(FTCStr,(Integer)m.getProperty(FTCStr)+1 );

		}


		return m;
	}


	private void updateCdc() {



		if(SimClock.getTime()>=lastNUpdate+500){
			double curCdc;
			Set<Integer> union = new HashSet<Integer>();
			union.addAll(Nt_1);union.addAll(Nt);
			curCdc=union.size()-(Nt.size()+Nt_1.size()-union.size());
			if(curCdc!=0)
				curCdc=curCdc/union.size();

			cdc = (1 - 0.85)*cdc + 0.85*curCdc;
			//cdc = curCdc;


			Nt_1.removeAll(Nt_1);
			Nt_1.addAll(Nt);
			Nt.removeAll(Nt);
			lastNUpdate=SimClock.getTime();	





		}

	}

	@Override
	public MessageRouter replicate() {
		SimulatedAnnealing r = new SimulatedAnnealing(this);
		return r;
	}

}
