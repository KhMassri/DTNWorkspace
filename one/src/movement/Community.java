package movement;

import movement.grid.GridCell;
import movement.grid.GridMap;
import core.Coord;
import core.Settings;

/**
 * Community movement model
 * 
 * implemented following:
 * "The Delay Fault-Tolerant Mobile Sensor Network for Pervasive Information Gathering"
 * paper by Yu Wang and Hongyi Wu.
 * 
 * @author Alessandro Vernata
 */
public class Community extends GridBasedMovement implements SwitchableMovement{

	/** Community based movement model's settings namespace ({@value})*/
	public static final String COMMUNITY_BASE_MOVEMENT_NS = 
		"Community";
	
	public static final String COMMUNITYI_PROBABILITY_EXIT_BOUNDARY = 
		"exitBoundaryProbability";
	
	public static final String COMMUNITYI_INTERNAL_MOVEMENT_MODEL = 
		"internalMovementModel";
	
	private Coord lastWaypoint;
	
	private MovementModel internalMovementModel;
	private GridCell homeLocation;
	private GridCell currentLocation;
	
	private double exitBoundaryProbability;
	
	public Community(Settings settings) {
		super(settings);

		this.readParameter();
		
		this.internalMovementModel.setMaxX((int)getMap().getMaxBound().getX());
		this.internalMovementModel.setMaxY((int)getMap().getMaxBound().getY());
		this.homeLocation =  getMap().getCells().get( rng.nextInt(getMap().getCells().size()) );
	}
	
	private Community(Community comm) {
		super(comm);
		
		
		this.internalMovementModel   = comm.internalMovementModel;
		this.exitBoundaryProbability = comm.exitBoundaryProbability;

		this.homeLocation    = getMap().getCells().get(rng.nextInt(getMap().getCells().size())).clone();
		this.currentLocation = this.homeLocation.clone();
	}
	
	@Override
	public Community replicate() {
		return new Community(this);
	}

	@Override
	public Coord getLastLocation() {
		return this.lastWaypoint;
	}
	
	@Override
	public void setLocation(Coord lastWaypoint) {
		this.lastWaypoint = lastWaypoint;
	}

	@Override
	public boolean isReady() {
		return true;
	}
	
	@Override
	public Coord getInitialLocation() {
		assert rng != null : "MovementModel not initialized!";
		rng.setSeed(new java.util.Random().nextLong());
		
		//Return initial location in his community
		double x_coord = homeLocation.getLowLeft().getX() + 
						(rng.nextDouble() * (homeLocation.getLowRight().getX()-homeLocation.getLowLeft().getX()));
		double y_coord = homeLocation.getLowLeft().getY() + 
						(rng.nextDouble() * (homeLocation.getTopLeft().getY()-homeLocation.getLowLeft().getY()));
		
		Coord c = new Coord(x_coord, y_coord);
		this.lastWaypoint = c;
		return c;
	}
	
	@Override
	public Path getPath() {
		/* Whenever a node reaches the boundary of its zone, 
		 * it moves out with a probability of 20%, and bounces 
		 * back with a probability of 80%	
		 */
		
		((SwitchableMovement) internalMovementModel).setLocation(this.lastWaypoint.clone());
		GridMap map = getMap();
		


		Path proposed = internalMovementModel.getPath();
		if (proposed == null) 
			return null;

			
		if (proposed.getCoords().size() != 2) 
			throw new IllegalArgumentException("Community internal Movement Model have to return only 2 coord (start, end)");
		Path path = new Path(proposed.getSpeed());
		path.addWaypoint(lastWaypoint.clone());
		
		Coord dest = proposed.getCoords().get(1);

		/* Check Bound and add dest to he waypoint*/
		if (dest.getX() > map.getMaxBound().getX())
			dest.setLocation(map.getMaxBound().getX(), dest.getY());
		if (dest.getX() < map.getMinBound().getX())
			dest.setLocation(map.getMinBound().getX(), dest.getY());
		
		if (dest.getY() > map.getMaxBound().getY())
			dest.setLocation(dest.getX(), map.getMaxBound().getY());	
		if (dest.getY() < map.getMinBound().getY())
			dest.setLocation(dest.getX(), map.getMinBound().getY());	
		
		//path.addWaypoint(dest);
		
		
		/* Check if no boundery is crossed or i return to my home */
		if (this.currentLocation.isInside(dest)){
			this.lastWaypoint = dest.clone();
			path.addWaypoint(dest);
			return path;
		}

		
		/* Check Other r Home Community */
		GridCell cellDest = getMap().getNodeByCoord(dest);
		if (cellDest == null)
			throw new RuntimeException("Returned cell is null, an error occured");
		
		
		if (cellDest.equals(this.homeLocation)){
			this.lastWaypoint = dest.clone();
			path.addWaypoint(dest);
			this.currentLocation = cellDest.clone();
			return path;
		}
		
		
		if (rng.nextDouble() < this.exitBoundaryProbability){
			
			Coord middlePoint = null;
			double ua = 0;
			double ub = 0;
			Coord p3 = null;
			Coord p4 = null;
			
			// First Line
			Coord p1 = lastWaypoint.clone();
			Coord p2 = dest.clone();
			
			
			// Test Bottom
			p3 = currentLocation.getLowLeft();
			p4 = currentLocation.getLowRight();
			
			ua = this.evaulateUA(p1,p2,p3,p4);
			ub = this.evaulateUB(p1,p2,p3,p4);
			
			if (ua >= 0 && ua <= 1 && ub >= 0 && ua <= 1){
				double newX = p1.getX() + ua * (p2.getX() - p1.getX());
				double newY = p1.getY() + ua * (p2.getY() - p1.getY());
				middlePoint = new Coord(newX, newY);
			}
			// Test Left
			if (middlePoint == null){
				p3 = currentLocation.getLowLeft();
				p4 = currentLocation.getTopLeft();
				
				ua = this.evaulateUA(p1,p2,p3,p4);
				ub = this.evaulateUB(p1,p2,p3,p4);
				
				if (ua >= 0 && ua <= 1 && ub >= 0 && ua <= 1){
					double newX = p1.getX() + ua * (p2.getX() - p1.getX());
					double newY = p1.getY() + ua * (p2.getY() - p1.getY());
					middlePoint = new Coord(newX, newY);
				}
			}
			// Test Right
			if (middlePoint == null){
				p3 = currentLocation.getLowRight();
				p4 = currentLocation.getTopRight();
				
				ua = this.evaulateUA(p1,p2,p3,p4);
				ub = this.evaulateUB(p1,p2,p3,p4);
				
				if (ua >= 0 && ua <= 1 && ub >= 0 && ua <= 1){
					double newX = p1.getX() + ua * (p2.getX() - p1.getX());
					double newY = p1.getY() + ua * (p2.getY() - p1.getY());
					middlePoint = new Coord(newX, newY);
				}
			}
			// Test Top
			if (middlePoint == null){
				p3 = currentLocation.getTopLeft();
				p4 = currentLocation.getTopRight();
				
				ua = this.evaulateUA(p1,p2,p3,p4);
				ub = this.evaulateUB(p1,p2,p3,p4);
				
				if (ua >= 0 && ua <= 1 && ub >= 0 && ua <= 1){
					double newX = p1.getX() + ua * (p2.getX() - p1.getX());
					double newY = p1.getY() + ua * (p2.getY() - p1.getY());
					middlePoint = new Coord(newX, newY);
				}
			}
			
			path.addWaypoint(middlePoint);
			path.addWaypoint(lastWaypoint.clone());
			return path;
		}
		else{
			this.lastWaypoint = dest.clone();
			path.addWaypoint(dest);
			this.currentLocation = cellDest.clone();
			return path;
		}

		
	}
	
	
	public boolean isInsideHome(Coord pos) {
		return this.homeLocation.isInside(pos);
	}
	
	private void readParameter() {
		Settings settings = new Settings(COMMUNITY_BASE_MOVEMENT_NS);		

		
		exitBoundaryProbability = settings.getDouble(COMMUNITYI_PROBABILITY_EXIT_BOUNDARY);
		
		String iMM = settings.getSetting(COMMUNITYI_INTERNAL_MOVEMENT_MODEL);
		if (iMM.toLowerCase().equals(LevyWalk.LEVY_WALK_BASE_MOVEMENT_NS.toLowerCase())) {
			this.internalMovementModel = new LevyWalk(settings);
		}
		else if (iMM.toLowerCase().equals(RandomWaypoint.RANDOMWAYPOINT_BASE_MOVEMENT_NS.toLowerCase())) {
			this.internalMovementModel = new RandomWaypoint(settings);
		}
		else if (iMM.toLowerCase().equals(RandomWalk.RANDOMWALK_BASE_MOVEMENT_NS.toLowerCase())) {
			this.internalMovementModel = new RandomWalk(settings);
		}		
		else {
			throw new IllegalArgumentException("Internal Movement Model not recognized for Community movement model.");
		}
		
	}
	
	private double evaulateUA(Coord p1, Coord p2, Coord p3, Coord p4){
		double num = (p4.getX() - p3.getX()) * (p1.getY() - p3.getY()) - (p4.getY() - p3.getY()) * (p1.getX() - p3.getX());
		double den = (p4.getY() - p3.getY()) * (p2.getX() - p1.getX()) - (p4.getX() - p3.getX()) * (p2.getY() - p1.getY());
		
		return num / den;	
	}
	
	private double evaulateUB(Coord p1, Coord p2, Coord p3, Coord p4){
		
		double num = (p2.getX() - p1.getX()) * (p1.getY() - p3.getY()) - (p2.getY() - p1.getY()) * (p1.getX() - p3.getX());
		double den = (p4.getY() - p3.getY()) * (p2.getX() - p1.getX()) - (p4.getX() - p3.getX()) * (p2.getY() - p1.getY());
		
		return num / den;
		
	}
	
}
