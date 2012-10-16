/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import core.Settings;

/**
 * Router that will deliver messages only to the final recipient.
 */
public class DirectDelivery extends DTNRouter {

	public DirectDelivery(Settings s) {
		super(s);
	}
	
	protected DirectDelivery(DirectDelivery r) {
		super(r);
	}
	
	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // can't start a new transfer
		}
				
		if (exchangeDeliverableMessages()!=null) 
			return;
	}
	
	@Override
	public MessageRouter replicate() {
		return new DirectDelivery(this);
	}
}
