package input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import core.Coord;
import core.Settings;
import core.SettingsError;
import core.SimClock;

public class AreaMonitoringMessageEventGenerator  extends MessageEventGenerator implements FieldsEventGeneration{
	
	public static final String MONITORED_AREA_LOW_LEFT = "lowLeft";
	public static final String MONITORED_AREA_TOP_RIGHT = "topRight";
	
	private List<Integer> toIds;
	private Integer fromId;
	
	private Coord lowLeftAngle;
	private Coord topRightAngle;
	
	public AreaMonitoringMessageEventGenerator(Settings s) {
		super(s);
		this.toIds = new ArrayList<Integer>();
		
		double[] ll = s.getCsvDoubles(MONITORED_AREA_LOW_LEFT);
		double[] tr = s.getCsvDoubles(MONITORED_AREA_TOP_RIGHT);
		
		if (ll.length != 2)
			throw new SettingsError("Low left angle (" + ll.toString() + 
			") is in incorrect fromat");
		
		if (tr.length != 2)
			throw new SettingsError("Top Right angle (" + ll.toString() + 
			") is in incorrect fromat");
		
		this.lowLeftAngle = new Coord(ll[0], ll[1]);
		this.topRightAngle = new Coord(tr[0], tr[1]);
		
		if (toHostRange == null) {
			throw new SettingsError("Destination host (" + TO_HOST_RANGE_S + 
					") must be defined");
		}
		for (int i = toHostRange[0]; i < toHostRange[1]; i++) {
			toIds.add(i);
		}
		Collections.shuffle(toIds, rng);
		
		// Don't Generate Messages
		this.nextEventsTime = Double.MAX_VALUE;
	}

	
	/** 
	 * Returns the next message creation event
	 * @see input.EventQueue#nextEvent()
	 */
	public ExternalEvent nextEvent() {
		int responseSize = 0; /* no responses requested */
		int from;
		int to;
		
		from = this.fromId;
		to = this.toIds.get(rng.nextInt(this.toIds.size()));
		
		if (to == from) /* skip self */
			throw new RuntimeException("Trying to send the messages to itself");
		
		

		MessageCreateEvent mce = new MessageCreateEvent(from, to, getID(), 
				drawMessageSize(), responseSize, this.nextEventsTime);
		
		/* no messages left */
		this.nextEventsTime = Double.MAX_VALUE;
		
		return mce;
	}
	
	@Override
	public boolean isInside(Coord point){	
		return point.getX() < this.topRightAngle.getX() && point.getX() > this.lowLeftAngle.getX() &&
			   point.getY() < this.topRightAngle.getY() && point.getY() > this.lowLeftAngle.getY();
	}
	
	@Override
	public void createEvent(Integer fromId) {
		this.fromId = fromId;
		this.nextEventsTime = SimClock.getTime(); //TODO: Check at run-time
	}


	@Override
	public Coord[] getArea() {
		Coord[] area = new Coord[2];
		area[0] = this.lowLeftAngle;
		area[1] = this.topRightAngle;
		return area;
	}
}
