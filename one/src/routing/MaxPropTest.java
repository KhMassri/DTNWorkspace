/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


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
public class MaxPropTest extends DTNRouter {
    /** Router's setting namespace ({@value})*/
	public static final String MAXPROP_NS = "MaxPropTest";
	/**
	 * Meeting probability set maximum size -setting id ({@value}).
	 * The maximum amount of meeting probabilities to store.  */
	public static final String PROB_SET_MAX_SIZE_S = "probSetMaxSize";
    
   
	/** IDs of the messages that are known to have reached the final dst */
	private Set<String> ackedMessageIds;
	
	


	/**
	 * Constructor. Creates a new prototype router based on the settings in
	 * the given Settings object.
	 * @param settings The settings object
	 */
	public MaxPropTest(Settings settings) {
		super(settings);
		
	}
	
	/**
	 * Copy constructor. Creates a new router based on the given prototype.
	 * @param r The router prototype where setting values are copied from
	 */
	protected MaxPropTest(MaxPropTest r) {
		super(r);
		
		this.ackedMessageIds = new HashSet<String>();
		
	}	

	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) { // new connection
			
			if (con.isInitiator(getHost())) {
				/* initiator performs all the actions on behalf of the
				 * other node too (so that the meeting probs are updated
				 * for both before exchanging them) */
				
				DTNHost otherHost = con.getOtherNode(getHost());
				/* In case the other host is a sink*/
				if(otherHost.isSink()){
					
					return;
				}
				
				MessageRouter mRouter = otherHost.getRouter();

				assert mRouter instanceof MaxPropTest : "MaxProp only works "+ 
				" with other routers of same type";
				MaxPropTest otherRouter = (MaxPropTest)mRouter;
				
				/* exchange ACKed message data */
				this.ackedMessageIds.addAll(otherRouter.ackedMessageIds);
				otherRouter.ackedMessageIds.addAll(this.ackedMessageIds);
				deleteAckedMessages();
				otherRouter.deleteAckedMessages();
				
				
			}
		}
		
	}

	
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
	
	
	@Override
	protected void transferDone(Connection con) {
		Message m = con.getMessage();
		/* was the message delivered to the final recipient? */
		if (m.getTo().isSink()&&con.getOtherNode(getHost()).isSink()) { 
			this.ackedMessageIds.add(m.getId()); // yes, add to ACKed messages
			this.deleteMessage(m.getId(), false); // delete from buffer
		}
	}
	
	
	
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
			if(other.isSink())  
				{tryMessagesForSinks();
				return null;
				}
			MaxPropTest othRouter = (MaxPropTest)other.getRouter();
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}
			
			connections.add(con);
		}
		
		
		
		for(Connection con:connections){
			DTNHost other = con.getOtherNode(getHost());
			MaxPropTest othRouter = (MaxPropTest)other.getRouter();
			
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
	
	
	
	@Override
	public RoutingInfo getRoutingInfo() {
		RoutingInfo top = super.getRoutingInfo();
		
		
		return top;
	}
	
	@Override
	public MessageRouter replicate() {
		MaxPropTest r = new MaxPropTest(this);
		return r;
	}
}