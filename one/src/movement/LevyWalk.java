package movement;

import core.Coord;
import core.Settings;
import core.SimClock;


/**
 * Levy Walk movement model
 * 
 * implemented following:
 * "On the Levy-walk Nature of Human Mobility: Do Humans Walk like Monkeys?"
 * paper by Injong Rhee et al.
 * 
 * @author Alessandro Vernata
 */
public class LevyWalk extends MovementModel implements SwitchableMovement {
	
	/** Grid based movement model's settings namespace ({@value})*/
	public static final String LEVY_WALK_BASE_MOVEMENT_NS = "LevyWalk";
	
	public static final String LEVY_WALK_ALPHA = 
		"alpha";
	public static final String LEVY_WALK_BETA = 
		"beta";
	public static final String LEVY_WALK_GAMMA = 
		"gamma";
	public static final String LEVY_WALK_K = 
		"k";
	public static final String LEVY_WALK_MAX_DISTANCE =
		"maxDistance";
	
	public static final String STATE_RUNNING =
		"Running";	
	public static final String STATE_READY=
		"Ready";
	public static final String STATE_WAITING=
		"Waiting";
	
	private Coord lastWaypoint;
	private double minDistance;
	private Double nextResumeTime;
	
	private double maxDistance;	
	private double levyWalkAlpha;
	private double levyWalkBeta;
	private double levyWalkGamma;
	private double levyWalkK;
	
	private double distance;       // l   Uses p(l) distribution:    p(l) ∼	1/l^(1+α), 0 < α < 2.
	private double direction;      // θ   Uses Uniform Distribution: 0-360
	private double moveTime;	   // ∆tf Related to moveLenght:     ∆tf = kl^(1−ρ), 0 ≤ ρ ≤ 1
	private double pauseTime;      // ∆tp Uses ψ(∆tp) distribution	 ψ(∆tp) ∼ 1/∆tp^(1+β)

	private String state;
	
	public LevyWalk(Settings settings) {
		super(settings);
		this.state = LevyWalk.STATE_READY;
		this.nextResumeTime = 0.0;
		readParameter();
	}
	
	public LevyWalk(LevyWalk proto) {
		super(proto);
		
		minDistance = proto.minDistance;
		maxDistance = proto.maxDistance;
		this.nextResumeTime = proto.nextResumeTime;
		
		levyWalkAlpha = proto.levyWalkAlpha;
		levyWalkBeta = proto.levyWalkBeta;
		levyWalkGamma = proto.levyWalkGamma;
		levyWalkK = proto.levyWalkK;
		this.state = LevyWalk.STATE_READY;
	}
	
	@Override
	public Coord getInitialLocation() {
		assert rng != null : "MovementModel not initialized!";
		rng.setSeed( new java.util.Random().nextLong());
		double x = rng.nextDouble() * getMaxX();
		double y = rng.nextDouble() * getMaxY();
		Coord c = new Coord(x,y);

		this.lastWaypoint = c;
		return c;
	}
	
	@Override
	public Path getPath() {
		if (this.state.equals(LevyWalk.STATE_RUNNING)) {
			nextResumeTime = SimClock.getTime() + this.pauseTime;
			this.state = LevyWalk.STATE_WAITING;
		}
		
		if (state.equals(LevyWalk.STATE_WAITING)) {
			if (SimClock.getTime() > this.nextResumeTime)
				this.state = LevyWalk.STATE_READY;
			else
				return null;
		}
		
		if (this.state.equals(LevyWalk.STATE_READY)) {
			this.state = LevyWalk.STATE_RUNNING;
			double maxX = getMaxX();
			double maxY = getMaxY();
			
			Coord dest = null;
			double speed = 0;
			int counter = 0;
			while (counter < 100) {
				counter++;
				this.generateLevyTuple();
				double x = lastWaypoint.getX() + distance * Math.cos(this.direction);
				double y = lastWaypoint.getY() + distance * Math.sin(this.direction);
			
				dest = new Coord(x,y);
				speed = distance / this.moveTime;
			
				if (x > 0 && y > 0 && x < maxX && y < maxY) {
					break;
				}
			}
			if (counter == 100)
				throw new RuntimeException("Deadlock in LevyWalk Movement.getPath");
	
			Path p = new Path(speed); 
			p.addWaypoint(lastWaypoint.clone());
			p.addWaypoint(dest);
			
			this.lastWaypoint = dest;
			return p;
		}
		else
			return null;

	}
	
	@Override
	public LevyWalk replicate() {
		return new LevyWalk(this);
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
	protected double generateWaitTime() {
		if (rng == null || this.pauseTime == Double.NaN) {
			return 0;
		}
		return this.pauseTime;
	}
	
	private void generateLevyTuple() {
		double tmpRndm = 0;
		double max     = 0;
		
		// Generating Direction - Uniform Distribution
		this.direction = rng.nextDouble() * 360;
		
		// Generating Distance - p(l) ∼ 1/l^(1+α), 0 < α < 2.
		double distGap = maxDistance - minDistance;
		tmpRndm = rng.nextDouble();
		max = Math.pow(0.01d,-1.0d / (1.0d+this.levyWalkAlpha));
		this.distance =  distGap * (Math.pow(tmpRndm,-1.0d / (1.0d+this.levyWalkAlpha)) / max) + minDistance; 
		
		// Generating Waiting ψ(∆tp) ∼ 1/∆tp^(1+β)
		double waitGap = maxWaitTime - minWaitTime;
		tmpRndm = rng.nextDouble();
		max = Math.pow(0.01d,-1.0d / (1.0d+this.levyWalkBeta));
		this.pauseTime =  waitGap * (Math.pow(tmpRndm,-1.0d / (1.0d+this.levyWalkBeta)) / max) + minWaitTime; 		
		
		// Generating Move Time ∆tf = kl^(1−ρ), 0 ≤ ρ ≤ 1
		this.moveTime =  this.levyWalkK * Math.pow(this.distance, 1 - this.levyWalkGamma);
	}
	
	
	private void readParameter() {
		Settings settings = new Settings(LEVY_WALK_BASE_MOVEMENT_NS);		

		minDistance = 0;
		maxDistance = settings.getInt(LEVY_WALK_MAX_DISTANCE);
		
		levyWalkAlpha = settings.getDouble(LEVY_WALK_ALPHA);
		levyWalkBeta = settings.getDouble(LEVY_WALK_BETA);
		levyWalkGamma = settings.getDouble(LEVY_WALK_GAMMA);
		levyWalkK = settings.getDouble(LEVY_WALK_K);
	}

}
