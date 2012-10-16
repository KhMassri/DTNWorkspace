package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;

/* This Generator router will generate messages randomly and will relay the message to the
 * first other router meets him and delete its message from is buffer. the other router will treat
 * this message as if it created by him and add all other routing information related to this message
 * 
 */

public class GeneratorRouter extends ActiveRouter {

	
	public GeneratorRouter(Settings s) {
		super(s);
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected GeneratorRouter(GeneratorRouter r) {
		super(r);
	}
	
	@Override
	protected int checkReceiving(Message m) {
				return TRY_LATER_BUSY;
	}
		
	
	public void changedConnection(Connection con) 
	{
		if(con.isUp()){
		DTNHost other=con.getOtherNode(getHost());
		if(other.getRouter().hello().equals("DTNRouter"))
			transferMessagesTo(other);
		
		}
		
		
		
		
	}
	
	synchronized void transferMessagesTo(DTNHost other){
		/*
		 * make the other node like it create the generator messages
		 * 
		 */
	
		for(Message m:this.getMessageCollection())
			
			if(other.getRouter().createNewMessage(m)){
				m.addNodeOnPath(other);
				this.deleteMessage(m.getId(),false);
				break;
			}
				
		
		
		
	}
		
		
	@Override
	public void update() {
		
	}
	
	@Override
//	protected void transferDone(Connection con) {
		/* don't leave a copy for the sender */
	//	this.deleteMessage(con.getMessage().getId(), false);
	//}
	
	
	public String hello(){return "GeneratorRouter";}
		
	@Override
	public GeneratorRouter replicate() {
		return new GeneratorRouter(this);
	}
	
	
	/* The same as ACtiveRouter with the reset of TTL in order
	 * to have uniform behaviour in the simulation
	 */
	/*
	public int startTransfer(Message m, Connection con) {
		int retVal;
		
		
		
		if (!con.isReadyForTransfer()) {
			return TRY_LATER_BUSY;
		}
		
		retVal = con.startTransfer(getHost(), m);
		
		if (retVal == RCV_OK) { // started transfer
			
			/*  modify the TTL both in the owned messaged
			 *  and in the one stored in the connection
			 */
		/*	con.getMessage().setTtl(msgTtl);
			m.setTtl(msgTtl);
			addToSendingConnections(con);
		}
		else if (deleteDelivered && retVal == DENIED_OLD)
			if((m.getTo()==con.getOtherNode(getHost()))||
					(m.getTo().isSink() && con.getOtherNode(getHost()).isSink()))
			this.deleteMessage(m.getId(), false);
		
		
		return retVal;
	}
	
	*/

}
	

