package routing;

import core.Settings;

/**
 * Router that will deliver messages only to the final recipient.
 */
public class DirectDeliveryToSink extends DTNRouter {

	public DirectDeliveryToSink(Settings s) {
		super(s);
	}
	
	protected DirectDeliveryToSink(DirectDeliveryToSink r) {
		super(r);
	}
	
	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // can't start a new transfer
		}
				
		if (tryMessagesForSinks()!=null) 
			return;
	}
	
	@Override
	public MessageRouter replicate() {
		return new DirectDeliveryToSink(this);
	}
}
