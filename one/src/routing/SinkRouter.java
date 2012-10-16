/**
 * 
 */
package routing;




import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import movement.MovementModel;

import core.Connection;
import core.DTNHost;
import core.DTNSim;
import core.Message;
import core.Settings;

/**
 * @author khalil
 *
 */
public class SinkRouter extends ActiveRouter {

	private static Set<String> sinkedMessages=new HashSet<String>();
	
	
	/**
	 * @param s
	 */
	public SinkRouter(Settings s) {
		super(s);
	}

	protected SinkRouter(SinkRouter r) {
		super(r);
	}

	static {
		DTNSim.registerForReset(SinkRouter.class.getCanonicalName());				
		reset();
	}
	
	public void changedConnection(Connection con) {
		return;
	}
			
	
	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		
		}	
	}
		
	public Message messageTransferred(String id, DTNHost from){	
		Message m = super.messageTransferred(id, from);
		sinkedMessages.add(m.getId());
		return m;
	}
	
	protected int checkReceiving(Message m) {
		if(sinkedMessages.contains(m.getId()))
			return DENIED_OLD;
				
		int result = super.checkReceiving(m);
		
		if (result == RCV_OK)
			if (SinkRouter.insert(m))
				return result;
			else
				return DENIED_OLD;
		return result;
	}
	
	public static synchronized boolean insert(Message m){
		if (sinkedMessages.contains(m.getId()))
			return false;
		sinkedMessages.add(m.getId());	
		return true;
	}
	
	public Set<String> getSinkedMessages() {
		return sinkedMessages;
	}
	
	public static void reset() {
		sinkedMessages.clear();
	}
	
	public String hello(){
		return "SinkRouter";
	}
	
	@Override
	public MessageRouter replicate() {
		return new SinkRouter(this);
	}

}



