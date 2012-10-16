/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 * 
 * 
 * Contribution From Sapienza - DIS
 */
package movement;

import core.Coord;
import core.Settings;

/**
 * A dummy stationary "movement" model where nodes do not move.
 * Might be useful for simulations with only external connection events. 
 */
public class StationaryMovement extends MovementModel {
	/** HOw the nodes are located in the area */
	public static final String MODE_S = "mode";
	
	/** Per node group setting for setting the location ({@value}) */
	public static final String LOCATION_S = "nodeLocation";
	/** Nord-Sud-Ovest-East **/
	public static final String DIRECTION_S = "direction";
	/** value of the offset **/
	public static final String OFFSET_S = "offset";	
	
	/** Low-Left point of the perimeter **/
	private static final String LOW_LEFT_S = "lowLeft";
	/** Top-Right point of the perimeter **/
	private static final String TOP_RIGHT_S = "topRight";
	
	private static Coord currLoc; /** The location of the nodes */
	private Coord loc;

	private int mode;  /*0 - use direction, offsetand nodeLocation
					     1 - use random placement in field
					   */
	// mode 0
	private double offset;
	private double direction;
	// mode 1
	private int[] lowLeft;
	private int[] topRight;
	
	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param s The Settings object where the settings are read from
	 */
	public StationaryMovement(Settings s) {
		super(s);

		//TODO: put control on right number of parameters		
		this.mode = s.getInt(MODE_S);
		if (this.mode == 0){
			this.lowLeft = null;
			this.topRight = null;

			this.direction = s.getDouble(DIRECTION_S);
			this.offset = s.getDouble(OFFSET_S);
			
			int coords[];
			coords = s.getCsvInts(LOCATION_S, 2);
			this.loc = new Coord(coords[0],coords[1]);
			StationaryMovement.currLoc = this.loc.clone();
		}
		else{
			this.lowLeft = s.getCsvInts(LOW_LEFT_S,2);
			this.topRight = s.getCsvInts(TOP_RIGHT_S,2);
			
			this.direction = 0;
			this.offset = 0;
			
			this.loc = getRandomPosInField();
		}

		

	}
	
	/**
	 * Copy constructor. 
	 * @param sm The StationaryMovement prototype
	 */
	public StationaryMovement(StationaryMovement sm) {
		super(sm);
		
		this.mode = sm.mode;
		
		if (this.mode == 0) {
			this.direction = sm.direction;
			this.offset = sm.offset;
			
			Coord startLoc = StationaryMovement.currLoc.clone();
			
			double x1 = startLoc.getX() + this.offset*Math.cos( Math.toRadians(this.direction));
			double y1 = startLoc.getY() + this.offset*Math.sin( Math.toRadians(this.direction));
			this.loc = new Coord(x1, y1);
			

			if(currLoc.getY()+10 >= this.getMaxY())
				StationaryMovement.currLoc.translate(10,10-currLoc.getY());

			else
				StationaryMovement.currLoc.translate(this.offset*Math.cos( Math.toRadians(this.direction)),
												 this.offset*Math.sin( Math.toRadians(this.direction)));
							
		}
		else{
			this.direction = sm.direction;
			this.offset = sm.offset;
			this.topRight = sm.topRight;
			this.lowLeft = sm.lowLeft;
			this.loc = getRandomPosInField();
		}
	}
	
	/**
	 * Returns the only location of this movement model
	 * @return the only location of this movement model
	 */
	@Override
	public Coord getInitialLocation() {
		return loc;
	}
	
	/**
	 * Returns a single coordinate path (using the only possible coordinate)
	 * @return a single coordinate path
	 */
	@Override
	public Path getPath() {
		Path p = new Path(0);
		p.addWaypoint(loc);
		return p;
	}
	
	@Override
	public double nextPathAvailable() {
		return Double.MAX_VALUE;	// no new paths available
	}
	
	@Override
	public StationaryMovement replicate() {
		return new StationaryMovement(this);
	}
	
	private Coord getRandomPosInField() {
		double x = rng.nextDouble() * (this.topRight[0] - this.lowLeft[0]) + this.lowLeft[0];
		double y = rng.nextDouble() * (this.topRight[1] - this.lowLeft[1]) + this.lowLeft[1];
		return new Coord(x,y);
	}

}
