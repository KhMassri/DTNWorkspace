package routing;


import core.Settings;

/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time. It consider the destination as a group
 */
public class Epidemic extends DTNRouter {
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public Epidemic(Settings s) {
		super(s);
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected Epidemic(Epidemic r) {
		super(r);
	}
			
	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}
		
		// Try first the messages that can be delivered to final recipient
		if (this.exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}
		
		// then try any/all message to any/all connection
		this.tryAllMessagesToAllConnections();
	}
	
	
	@Override
	public MessageRouter replicate() {
		return new Epidemic(this);
	}

}