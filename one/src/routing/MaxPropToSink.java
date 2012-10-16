/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import routing.maxprop.MaxPropDijkstra;
import routing.maxprop.MeetingProbabilitySet;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.Tuple;

/**
 * Implementation of MaxProp router as described in 
 * <I>MaxProp: Routing for Vehicle-Based Disruption-Tolerant Networks</I> by
 * John Burgess et al.
 * @version 1.0
 * 
 * Extension of the protocol by adding a parameter alpha (default 1)
 * By new connection, the delivery likelihood is increased by alpha
 * and divided by 1+alpha.  Using the default results in the original 
 * algorithm.  Refer to Karvo and Ott, <I>Time Scales and Delay-Tolerant Routing 
 * Protocols</I> Chants, 2008 
 */
public class MaxPropToSink extends DTNRouter {
    /** Router's setting namespace ({@value})*/
	public static final String MAXPROP_TO_SINK_NS = "MaxPropToSink";
	/**
	 * Meeting probability set maximum size -setting id ({@value}).
	 * The maximum amount of meeting probabilities to store.  */
	public static final String PROB_SET_MAX_SIZE_S = "probSetMaxSize";
    /** Default value for the meeting probability set maximum size ({@value}).*/
    public static final int DEFAULT_PROB_SET_MAX_SIZE = 100;
    private static int probSetMaxSize;

	/** probabilities of meeting hosts */
	private MeetingProbabilitySet probs;
	/** meeting probabilities of all hosts from this host's point of view 
	 * mapped using host's network address */
	private Map<Integer, MeetingProbabilitySet> allProbs;
	/** the cost-to-node calculator */
	private MaxPropDijkstra dijkstra;	
	/** IDs of the messages that are known to have reached the final dst */
	private Set<String> ackedMessageIds;
	/** mapping of the current costs for all messages. This should be set to
	 * null always when the costs should be updated (a host is met or a new
	 * message is received) */
	private Map<Integer, Double> costsForMessages;
	/** From host of the last cost calculation */
	private DTNHost lastCostFrom;
		
	/** Over how many samples the "average number of bytes transferred per
	 * transfer opportunity" is taken */
	

	/** The alpha parameter string*/
	public static final String ALPHA_S = "alpha";

	/** The alpha variable, default = 1;*/
	private double alpha;

	/** The default value for alpha */
	public static final double DEFAULT_ALPHA = 1.0;

	/**
	 * Constructor. Creates a new prototype router based on the settings in
	 * the given Settings object.
	 * @param settings The settings object
	 */
	public MaxPropToSink(Settings settings) {
		super(settings);
		Settings maxPropSettings = new Settings(MAXPROP_TO_SINK_NS);		
		if (maxPropSettings.contains(ALPHA_S)) {
			alpha = maxPropSettings.getDouble(ALPHA_S);
		} else {
			alpha = DEFAULT_ALPHA;
		}

        Settings mpSettings = new Settings(MAXPROP_TO_SINK_NS);
        if (mpSettings.contains(PROB_SET_MAX_SIZE_S)) {
            probSetMaxSize = mpSettings.getInt(PROB_SET_MAX_SIZE_S);
        } else {
            probSetMaxSize = DEFAULT_PROB_SET_MAX_SIZE;
        }
	}
	
	/**
	 * Copy constructor. Creates a new router based on the given prototype.
	 * @param r The router prototype where setting values are copied from
	 */
	protected MaxPropToSink(MaxPropToSink r) {
		super(r);
		this.alpha = r.alpha;
		this.probs = new MeetingProbabilitySet(probSetMaxSize, this.alpha);
		this.allProbs = new HashMap<Integer, MeetingProbabilitySet>();
		this.dijkstra = new MaxPropDijkstra(this.allProbs);
		this.ackedMessageIds = new HashSet<String>();
		
	}	

	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) { // new connection
			this.costsForMessages = null; // invalidate old cost estimates
			
			if (con.isInitiator(getHost())) {
				/* initiator performs all the actions on behalf of the
				 * other node too (so that the meeting probs are updated
				 * for both before exchanging them) */
				
				DTNHost otherHost = con.getOtherNode(getHost());
				
				// if it is the generator host nothing to do
				
				if(otherHost.getRouter().hello().equals("GeneratorRouter"))
					return;
				
				/* In case the other host is a sink*/
				if(otherHost.isSink()){
					
					probs.updateMeetingProbFor(otherHost.getAddress());
					return;
				}
				this.costsForMessages = null;
				MessageRouter mRouter = otherHost.getRouter();

				assert mRouter instanceof MaxPropToSink : "MaxProp only works "+ 
				" with other routers of same type";
				MaxPropToSink otherRouter = (MaxPropToSink)mRouter;
				
				/* exchange ACKed message data */
				this.ackedMessageIds.addAll(otherRouter.ackedMessageIds);
				otherRouter.ackedMessageIds.addAll(this.ackedMessageIds);
				deleteAckedMessages();
				otherRouter.deleteAckedMessages();
				
				/* update both meeting probabilities */
				probs.updateMeetingProbFor(otherHost.getAddress());
				otherRouter.probs.updateMeetingProbFor(getHost().getAddress());
				
				/* exchange the transitive probabilities */
				this.updateTransitiveProbs(otherRouter.allProbs);
				otherRouter.updateTransitiveProbs(this.allProbs);
				this.allProbs.put(otherHost.getAddress(),
						otherRouter.probs.replicate());
				otherRouter.allProbs.put(getHost().getAddress(),
						this.probs.replicate());
			}
		}
		
	}

	/**
	 * Updates transitive probability values by replacing the current 
	 * MeetingProbabilitySets with the values from the given mapping
	 * if the given sets have more recent updates.
	 * @param p Mapping of the values of the other host
	 */
	private void updateTransitiveProbs(Map<Integer, MeetingProbabilitySet> p) {
		for (Map.Entry<Integer, MeetingProbabilitySet> e : p.entrySet()) {
			MeetingProbabilitySet myMps = this.allProbs.get(e.getKey()); 
			if (myMps == null || 
				e.getValue().getLastUpdateTime() > myMps.getLastUpdateTime() ) {
				this.allProbs.put(e.getKey(), e.getValue().replicate());
			}
		}
	}
	
	/**
	 * Deletes the messages from the message buffer that are known to be ACKed
	 */
	private void deleteAckedMessages() {
		for (String id : this.ackedMessageIds) {
			if (this.hasMessage(id) && !isSending(id)) {
				this.deleteMessage(id, false);
			}
		}
	}
	
	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);
		/* was this node the final recipient of the message? */
		if (isDeliveredMessage(m)) {
			this.ackedMessageIds.add(id);
		}
		return m;
	}
	
	/**
	 * Method is called just before a transfer is finalized 
	 * at {@link ActiveRouter#update()}. MaxProp makes book keeping of the
	 * delivered messages so their IDs are stored.
	 * @param con The connection whose transfer was finalized
	 */
	@Override
	protected void transferDone(Connection con) {
		Message m = con.getMessage();
		/* was the message delivered to the final recipient? */
		if (m.getTo().isSink()&&con.getOtherNode(getHost()).isSink()) { 
			this.ackedMessageIds.add(m.getId()); // yes, add to ACKed messages
			this.deleteMessage(m.getId(), false); // delete from buffer
		}
	}
	
	
	/**
	 * Returns the next message that should be dropped, according to MaxProp's
	 * message ordering scheme (see {@link MaxPropTupleComparator}). 
	 * @param excludeMsgBeingSent If true, excludes message(s) that are
	 * being sent from the next-to-be-dropped check (i.e., if next message to
	 * drop is being sent, the following message is returned)
	 * @return The oldest message or null if no message could be returned
	 * (no messages in buffer or all messages in buffer are being sent and
	 * exludeMsgBeingSent is true)
	 */
    @Override
	protected Message getOldestMessage(boolean excludeMsgBeingSent) {
		Collection<Message> messages = this.getMessageCollection();
		List<Message> validMessages = new ArrayList<Message>();

		for (Message m : messages) {	
			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
			}
			validMessages.add(m);
		}
		
		Collections.sort(validMessages,new MaxPropComparator()); 
		
		return validMessages.get(validMessages.size()-1); // return last message
	}
	
	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		if(this.tryMessagesForSinks()!=null)return;
				
		tryOtherMessages();	
	}
	
	/**
	 * Returns the message delivery cost between two hosts from this host's
	 * point of view. If there is no path between "from" and "to" host, 
	 * Double.MAX_VALUE is returned. Paths are calculated only to hosts
	 * that this host has messages to.
	 * @param from The host where a message is coming from
	 * @param to The host where a message would be destined to
	 * @return The cost of the cheapest path to the destination or 
	 * Double.MAX_VALUE if such a path doesn't exist
	 */
	public double getCost(DTNHost from) {
		
		/* check if the cached values are OK */
		if (this.costsForMessages == null || lastCostFrom != from) {
			/* cached costs are invalid -> calculate new costs */
			this.allProbs.put(getHost().getAddress(), this.probs);
			
			int fromIndex = from.getAddress();
			
			/* calculate paths only to nodes we have messages to 
			 * (optimization) */
			Set<Integer> toSet = new HashSet<Integer>();
			toSet.add(0);
						
			this.costsForMessages = dijkstra.getCosts(fromIndex, toSet);
			this.lastCostFrom = from; // store source host for caching checks
		}
		
		if (costsForMessages.containsKey(0)) {
			return costsForMessages.get(0);
		}
		else {
			/* there's no known path to the given host */
			return Double.MAX_VALUE;
		}
	}
	
	/**
	 * Tries to send all other messages to all connected hosts ordered by
	 * hop counts and their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Connection> connections = new ArrayList<Connection>();
		List<Message> validMessages = new ArrayList<Message>();
		List<Tuple<Message, Connection>> messages =	new ArrayList<Tuple<Message, Connection>>(); 

		
			validMessages.addAll(getMessageCollection());
			Collections.sort(validMessages,new MaxPropComparator());
		
		/* for all connected hosts that are not transferring at the moment,
		 * collect all the messages that could be sent */
		
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			
			// skip the generators
			if(other.getRouter().hello().equals("GeneratorRouter"))
				continue;
			
			if(other.isSink())  
				{tryMessagesForSinks();
				return null;
				}
			MaxPropToSink othRouter = (MaxPropToSink)other.getRouter();
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}
			
			connections.add(con);
		}
		
		Collections.sort(connections,new MaxPropConnectionComparator());
		
		for(Connection con:connections){
			DTNHost other = con.getOtherNode(getHost());
			MaxPropToSink othRouter = (MaxPropToSink)other.getRouter();
			
			for (Message m : validMessages) {
				/* skip messages that the other host has or that have
				 * passed the other host */
				
				if (othRouter.hasMessage(m.getId()) || m.getHops().contains(other))
					continue; 
	
				messages.add(new Tuple<Message, Connection>(m,con));
			}			
		}
		
		if (messages.size() == 0) {
			return null;
		}
		
		return tryMessagesForConnected(messages);	
	}
	
	
	/**
	 * Message comparator for the MaxProp routing module. 
	 * Messages that have a hop count smaller than the given
	 * threshold are given priority and they are ordered by their hop count.
	 * Other messages are ordered by their delivery cost.
	 */
	private class MaxPropComparator implements Comparator<Message> {
		
		public int compare(Message msg1, Message msg2) {
			
			int hopc1 = msg1.getHopCount();
			int hopc2 = msg2.getHopCount();

			
			
			if (hopc1 < hopc2) {
				return -1; // message1 should be first
			}
			else if (hopc2 < hopc1) {
				return 1; // message2 -"-
			}
			
			return compareByQueueMode(msg1, msg2);
				
		}		
	}
	
	/**
	 * Message-Connection tuple comparator for the MaxProp routing 
	 * module. Uses {@link MaxPropComparator} on the messages of the tuples
	 * setting the "from" host for that message to be the one in the connection
	 * tuple (i.e., path is calculated starting from the host on the other end 
	 * of the connection).
	 */
	private class MaxPropConnectionComparator 
			implements Comparator <Connection>  {
		
		public int compare(Connection con1,Connection con2) {
			
			DTNHost from1 = con1.getOtherNode(getHost());
			DTNHost from2 = con2.getOtherNode(getHost());
			double p1 = getCost(from1);
			double p2 = getCost(from2);
			if(p1<p2)return -1;
			else if(p1>p2)return 1;
			else return 0;
			
		}
	}
	
	
	@Override
	public RoutingInfo getRoutingInfo() {
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(probs.getAllProbs().size() + 
				" meeting probabilities");
		
		/* show meeting probabilities for this host */
		for (Map.Entry<Integer, Double> e : probs.getAllProbs().entrySet()) {
			Integer host = e.getKey();
			Double value = e.getValue();			
			ri.addMoreInfo(new RoutingInfo(String.format("host %d : %.6f", 
					host, value)));
		}
			
		top.addMoreInfo(ri);
		return top;
	}
	
	@Override
	public MessageRouter replicate() {
		MaxPropToSink r = new MaxPropToSink(this);
		return r;
	}
}