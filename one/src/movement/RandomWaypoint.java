/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import core.Coord;
import core.Settings;

/**
 * Random waypoint movement model. Creates zig-zag paths within the
 * simulation area.
 */
public class RandomWaypoint extends MovementModel implements SwitchableMovement  {
	
	/** RandomWaypoint based movement model's settings namespace ({@value})*/
	public static final String RANDOMWAYPOINT_BASE_MOVEMENT_NS = 
		"RandomWayPoint";
	
	/** how many waypoints should there be per path */
	private static final int PATH_LENGTH = 1;
	private Coord lastWaypoint;
	
	public RandomWaypoint(Settings settings) {
		super(settings);
	}
	
	protected RandomWaypoint(RandomWaypoint rwp) {
		super(rwp);
	}
	
	/**
	 * Returns a possible (random) placement for a host
	 * @return Random position on the map
	 */
	@Override
	public Coord getInitialLocation() {
		assert rng != null : "MovementModel not initialized!";
		Coord c = randomCoord();

		this.lastWaypoint = c;
		return c;
	}
	
	
	
	@Override
	public Path getPath() {
		Path p;
		p = new Path(generateSpeed());
		p.addWaypoint(lastWaypoint.clone());
		Coord c = lastWaypoint;
		
		for (int i=0; i<PATH_LENGTH; i++) {
			c = randomCoord();
			p.addWaypoint(c);	
		}
		
		this.lastWaypoint = c;
		return p;
	}
	
	@Override
	public RandomWaypoint replicate() {
		return new RandomWaypoint(this);
	}
	
	protected Coord randomCoord() {
		return new Coord(rng.nextDouble() * getMaxX(),
				rng.nextDouble() * getMaxY());
	}

	@Override
	public void setLocation(Coord lastWaypoint) {
		this.lastWaypoint = lastWaypoint;
	}

	@Override
	public Coord getLastLocation() {
		return this.lastWaypoint;
	}

	@Override
	public boolean isReady() {
		return false;
	}
}
