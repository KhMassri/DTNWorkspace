package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;

public class ProphetToSink extends DTNRouter {

	/** delivery predictability initialization constant*/
	public static final double P_INIT = 0.75;
	/** delivery predictability transitivity scaling constant default value */
	public static final double DEFAULT_BETA = 0.25;
	/** delivery predictability aging constant */
	public static final double GAMMA = 0.98;
	
	/** Prophet router's setting namespace ({@value})*/ 
	public static final String PROPHETTOSINK_NS = "ProphetToSink";
	/**
	 * Number of seconds in time unit -setting id ({@value}).
	 * How many seconds one time unit is when calculating aging of 
	 * delivery predictions. Should be tweaked for the scenario.*/
	public static final String SECONDS_IN_UNIT_S ="secondsInTimeUnit";
	
	/**
	 * Transitivity scaling constant (beta) -setting id ({@value}).
	 * Default value for setting is {@link #DEFAULT_BETA}.
	 */
	public static final String BETA_S = "beta";

	/** the value of nrof seconds in time unit -setting */
	private int secondsInTimeUnit;
	/** value of beta setting */
	private double beta;

	/** delivery predictabilities */
	private Map<DTNHost, Double> preds;
	/** predictability to meet any sink node*/
	private Double sinksPred;
	
	/** last delivery predictability update (sim)time */
	private double lastAgeUpdate;
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public ProphetToSink(Settings s) {
		super(s);
		Settings prophetSettings = new Settings(PROPHETTOSINK_NS);
		secondsInTimeUnit = prophetSettings.getInt(SECONDS_IN_UNIT_S);
		if (prophetSettings.contains(BETA_S)) {
			beta = prophetSettings.getDouble(BETA_S);
		}
		else {
			beta = DEFAULT_BETA;
		}

		initPreds();
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ProphetToSink(ProphetToSink r) {
		super(r);
		this.secondsInTimeUnit = r.secondsInTimeUnit;
		this.beta = r.beta;
		initPreds();
	}
	
	/**
	 * Initializes predictability hash
	 */
	private void initPreds() {
		this.preds = new HashMap<DTNHost, Double>();
		this.sinksPred=0.0;
		
	}

	@Override
	public void changedConnection(Connection con) {
		
		// nothing to do if the connection with a generator
		
		if(con.getOtherNode(getHost()).getRouter().hello().equals("GeneratorRouter"))
			return;
			
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			updateDeliveryPredFor(otherHost);
			if(!otherHost.isSink())
			updateTransitivePreds(otherHost);
		}
	}
	
	/**
	 * Updates delivery predictions for a host.
	 * <CODE>P(a,b) = P(a,b)_old + (1 - P(a,b)_old) * P_INIT</CODE>
	 * @param host The host we just met
	 */
	private void updateDeliveryPredFor(DTNHost host) {
		if(host.isSink())
			this.sinksPred=this.sinksPred+(1-this.sinksPred)*P_INIT;
		else{
		double oldValue = getPredFor(host);
		double newValue = oldValue + (1 - oldValue) * P_INIT;
		preds.put(host, newValue);
		}
		
	}
	
	/**
	 * Returns the current prediction (P) value for a host or 0 if entry for
	 * the host doesn't exist.
	 * @param host The host to look the P for
	 * @return the current P value
	 */
	public double getPredFor(DTNHost host) {
		ageDeliveryPreds(); // make sure preds are updated before getting
		if(host.isSink())return this.sinksPred;
		
		if (preds.containsKey(host)) {
			return preds.get(host);
		}
		else {
			return 0;
		}
	}
	
	public double getSinksPred(){
		return this.sinksPred;
		}
	
	/**
	 * Updates transitive (A->B->C) delivery predictions.
	 * <CODE>P(a,c) = P(a,c)_old + (1 - P(a,c)_old) * P(a,b) * P(b,c) * BETA
	 * </CODE>
	 * @param host The B host who we just met
	 */
	private void updateTransitivePreds(DTNHost host) {
		
		
		MessageRouter otherRouter = host.getRouter();
		assert otherRouter instanceof ProphetToSink : "PRoPHET To sinks only works " + 
			" with other routers of same type";
		
		double pForHost = getPredFor(host); // P(a,b)
		Map<DTNHost, Double> othersPreds = 
			((ProphetToSink)otherRouter).getDeliveryPreds();
		double otherSinksPred=((ProphetToSink)otherRouter).getSinksPred();
		
		for (Map.Entry<DTNHost, Double> e : othersPreds.entrySet()) {
			if (e.getKey() == getHost()) {
				continue; // don't add yourself
			}
			
			double pOld = getPredFor(e.getKey()); // P(a,c)_old
			double pNew = pOld + ( 1 - pOld) * pForHost * e.getValue() * beta;
			preds.put(e.getKey(), pNew);
		}
		this.sinksPred = this.sinksPred + ( 1 - this.sinksPred) * pForHost *otherSinksPred* beta;
	}

	/**
	 * Ages all entries in the delivery predictions.
	 * <CODE>P(a,b) = P(a,b)_old * (GAMMA ^ k)</CODE>, where k is number of
	 * time units that have elapsed since the last time the metric was aged.
	 * @see #SECONDS_IN_UNIT_S
	 */
	private void ageDeliveryPreds() {
		double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) / 
			secondsInTimeUnit;
		
		if (timeDiff == 0) {
			return;
		}
		
		double mult = Math.pow(GAMMA, timeDiff);
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			e.setValue(e.getValue()*mult);
		}
		this.sinksPred = this.sinksPred * mult;
		
		
		this.lastAgeUpdate = SimClock.getTime();
	}
	
	/**
	 * Returns a map of this router's delivery predictions
	 * @return a map of this router's delivery predictions
	 */
	private Map<DTNHost, Double> getDeliveryPreds() {
		ageDeliveryPreds(); // make sure the aging is done
		return this.preds;
	}
	
	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		
		if(tryMessagesForSinks()!=null) return;
		
		tryOtherMessages();		
	}
	
	/**
	 * Tries to send all other messages to all connected hosts ordered by
	 * their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages = 
			new ArrayList<Tuple<Message, Connection>>(); 
	
		Collection<Message> msgCollection = getMessageCollection();
		
		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			
			if(other.getRouter().hello().equals("GeneratorRouter"))
				continue;
			
			if(other.isSink())
				{tryMessagesForSinks();
				return null;
				}
			ProphetToSink othRouter = (ProphetToSink)other.getRouter();
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}
			if (this.getSinksPred() >= othRouter.getSinksPred() - 0.07) {
				// the other node has higher probability of delivery
				continue;
			}
			
			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}
				messages.add(new Tuple<Message, Connection>(m,con));
			}			
		}
		
		if (messages.size() == 0) {
			return null;
		}
		
		// sort the message-connection tuples
		Collections.sort(messages, new TupleComparator());
		return tryMessagesForConnected(messages);	// try to send messages
	}
	
	/**
	 * Comparator for Message-Connection-Tuples that orders the tuples by
	 * their delivery probability by the host on the other side of the 
	 * connection (GRTRMax)
	 */
	private class TupleComparator implements Comparator 
		<Tuple<Message, Connection>> {

		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			// delivery probability of tuple1's message with tuple1's connection
			double p1 = ((ProphetToSink)tuple1.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple1.getKey().getTo());
			// -"- tuple2...
			double p2 = ((ProphetToSink)tuple2.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple2.getKey().getTo());

			// bigger probability should come first
			if (p2-p1 == 0) {
				/* equal probabilities -> let queue mode decide */
				return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
			}
			else if (p2-p1 < 0) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}
	
	protected int startTransfer(Message m, Connection con)
	{
	int retVal;
	if (!con.isReadyForTransfer())
	{
		return TRY_LATER_BUSY;
		}
	retVal = con.startTransfer(getHost(), m);
	
	if (retVal == DENIED_OLD && m.getTo().isSink()&& con.getOtherNode(getHost()).isSink())
	{
		/* final recipient has already received the msg -> delete it */
		
		
		this.deleteMessage(m.getId(), false);
		return retVal;
		}
	
	if (retVal == RCV_OK)
	{ // started transfer
		addToSendingConnections(con);
		return retVal;
			}

	return retVal;
	}
	
	
	
	@Override
	public RoutingInfo getRoutingInfo() {
		ageDeliveryPreds();
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(preds.size() + 
				" delivery prediction(s)");
		ri.addMoreInfo(new RoutingInfo(String.format("ToSink : %.6f",this.sinksPred)));
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			DTNHost host = e.getKey();
			//Double value = e.getValue();
			
			ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f", 
					host, ((ProphetToSink)host.getRouter()).getSinksPred())));
		}
		
		top.addMoreInfo(ri);
		return top;
	}
	
	@Override
	public MessageRouter replicate() {
		ProphetToSink r = new ProphetToSink(this);
		return r;
	}

	

}
