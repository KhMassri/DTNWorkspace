
package routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;

/**
 * Implementation of Spray and wait router as depicted in 
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently
 * Connected Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 *
 */
public class GridRW extends DTNRouter {
	/** SprayAndWait router's settings name space ({@value})*/ 
	public static final String SPRAYANDWAIT_NS = "GridRW";
	/** identifier for the initial number of copies setting ({@value})*/ 
	public static final String NROF_COPIES = "nrofCopies";
	/** identifier for the binary-mode setting ({@value})*/ 
	public static final String BINARY_MODE = "binaryMode";
	/** Message property key */
	public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "." +
		"copies";
	
	protected int initialNrofCopies;
	protected int spltDist;
	

	public GridRW(Settings s) {
		super(s);
		Settings snwSettings = new Settings(SPRAYANDWAIT_NS);
		
		initialNrofCopies = snwSettings.getInt(NROF_COPIES);
		spltDist = snwSettings.getInt("spltDist");

	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected GridRW(GridRW r) {
		super(r);
		this.initialNrofCopies = r.initialNrofCopies;
		this.spltDist = r.spltDist;
		
	}
	
	@Override
	public int receiveMessage(Message m, DTNHost from) {
		return super.receiveMessage(m, from);
	}
	
	
	
	@Override 
	public boolean createNewMessage(Message msg) {
		makeRoomForNewMessage(msg.getSize());

		msg.setTtl(this.msgTtl);
		msg.addProperty(MSG_COUNT_PROPERTY, new Integer(initialNrofCopies));
		return super.createNewMessage(msg);
	}
	
	
	
	
	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		if (exchangeDeliverableMessages()!=null) 
			return;
		
		
		List<Message> copiesLeft = new ArrayList<Message>(this.getMessageCollection()); //sortByQueueMode(getMessagesWithCopiesLeft());
		List<Connection> connections = getConnections();
		int n = connections.size();
		
		if (copiesLeft.size() > 0 && n > 0) 
			
		for(Message m : copiesLeft)
		{
			
				
			Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROPERTY);
			Integer initNrofCopies = nrofCopies;
			Message msg=m.replicate();
			
			
				nrofCopies = (int)Math.ceil(nrofCopies/2.0);
				int halfTtl = (int)(m.getTtl()/2.0 +0.5);
				
				

			
			if(this.getHost().getLocation().distance(m.getTo().getLocation()) < spltDist)
				if(nrofCopies >=1)
				{				
					msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
					
					if(initNrofCopies!=1)
						msg.setTtl(halfTtl+1);
				}
			

			int gess = new Random().nextInt(n);

			Connection con = connections.get(gess);
			
			if(this.isSending(m.getId()) || startTransfer(msg, con)!=RCV_OK)
					continue;	
//			System.out.println(this.getHost()+"  Sent TTL =  "+msg.getTtl());
			
			return;

					
			
			}
	}
	
	
	@Override
	protected void transferDone(Connection con) {
		
		String msgId = con.getMessage().getId();
		/* get this router's copy of the message */
		Message msg = getMessage(msgId);

		if (msg == null) { // message has been dropped from the buffer after..
			return; // ..start of transfer -> no need to reduce amount of copies
		}
		
		

		
		if(con.getOtherNode(getHost())==msg.getTo())
			{
			this.deleteMessage(msgId, false);
			return;
			}
		
		Integer nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROPERTY);
		Integer iniCopies = nrofCopies;
		nrofCopies = (int)Math.ceil(nrofCopies/2.0);
		int halfTtl = msg.getTtl()/2 +1;
		

		if(this.getHost().getLocation().distance(msg.getTo().getLocation()) < spltDist)
		{
				if(nrofCopies >=1)
				{
					msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
					//msg.setTtl(con.getMessage().getTtl()+1);
					
				}
				
				if(iniCopies == 1)
					this.deleteMessage(msg.getId(), false);
				else {
					msg.setTtl(halfTtl);
//					System.out.println("  "+this.getHost()+"  splitted TTL = "+msg.getTtl());
				}
		}
		else
			this.deleteMessage(msg.getId(), false);
		
		
			
	}
	
	@Override
	public MessageRouter replicate() {
		return new GridRW(this);
	}
}




