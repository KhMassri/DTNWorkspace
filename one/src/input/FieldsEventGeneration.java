package input;

import core.Coord;

/**
 * Interface for event queues. Any class that is not a movement model or a 
 * routing module but wishes to provide events for the simulation (like creating
 * messages) must implement this interface and register itself to the 
 * simulator. This interface in particular notify the fact that events are node 
 * dependent, and so its creation is explicitly called by nodes.
 * 
 * See the {@link EventQueueHandler} class for configuration 
 * instructions.
 */
public interface FieldsEventGeneration {

	/**
	 * Create the event from the node specified in fromId paramter
	 * @return The next event
	 */
	public void createEvent(Integer fromId);

	/**
	 * Verify if the location is inside the monitored area
	 * @return True if it is inside, False otherwise
	 */
	public boolean isInside(Coord location);
	
	/**
	 * Return the Monitored Areas, two coords for each rectangle area
	 * @return The Monitored area
	 */
	public Coord[] getArea();
}
