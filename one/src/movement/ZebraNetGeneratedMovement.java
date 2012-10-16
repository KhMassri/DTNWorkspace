package movement;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import core.Coord;
import core.Settings;
import core.SimClock;


/**
 * ZebraNet Generation movement model
 * 
 * implemented following:
 * the generation step implemented by ZebraNet project during the first simulation in January 2004
 * 
 * @author Alessandro Vernata
 */
public class ZebraNetGeneratedMovement extends MovementModel {
	
	private class ZebraStep {
		public Coord pos;
		public Double speed;
		public Double time;
		
		public ZebraStep(Coord pos, Double speed, Double time) {
			this.pos = pos;
			this.speed = speed;
			this.time = time;
		}
	}

	/** Seed to generate movement model for zebras */
	public static final String ZEBRA_BASE_FILE = "data/ZebraNet/baseoutUTM1.txt";
	
	public static final List<Double> DIST_LIST = new ArrayList<Double>();
	public static final List<Double> DIRECTION_LIST = new ArrayList<Double>();
	public static final List<Double> DELTA_LIST = new ArrayList<Double>();
	
	private Integer zebraId;
	private Coord lastWaypoint;
	private List<ZebraStep> movementStep;
	private Integer currentIndex;
	
	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param s The Settings object where the settings are read from
	 */
	public ZebraNetGeneratedMovement(Settings settings) {
		super(settings);
		
		this.zebraId = 0;
		generateLists();
		generateTrace();
	}
	
	/**
	 * Copy constructor. 
	 * @param sm The ZebraNetGeneratedMovement prototype
	 */
	public ZebraNetGeneratedMovement(ZebraNetGeneratedMovement proto) {
		super(proto);
		
		this.zebraId = proto.zebraId+1;
		generateTrace();
	}
	
	@Override
	public Coord getInitialLocation() {
		return this.lastWaypoint.clone();
	}
	

	@Override
	public Path getPath() {
		if (this.currentIndex >= this.movementStep.size())
			this.currentIndex = 0;
			
		ZebraStep current = this.movementStep.get(this.currentIndex);
		if (current.time >  SimClock.getTime())
			return null;
		
		this.currentIndex++;
		
		
		if (current.pos.getX() <= 0 || current.pos.getY() <= 0)
			throw new IllegalArgumentException("Some parameter for ZebraNetGenerator are not correct, invalid pos is generated: "+current.pos);
		
		if (current.pos.getX() >= getMaxX() || current.pos.getY() >= getMaxY())
			throw new IllegalArgumentException("Some parameter for ZebraNetGenerator are not correct, invalid pos is generated: "+current.pos);;
		
		Path p = new Path(current.speed);
		p.addWaypoint(lastWaypoint.clone());
		p.addWaypoint(current.pos.clone());	
	
		
		this.lastWaypoint = current.pos.clone();
		return p;
	}
	
	@Override
	public ZebraNetGeneratedMovement replicate() {
		return new ZebraNetGeneratedMovement(this);
	}

	
	/**
	 * Generate Trace for the current node
	 */	
	private void generateTrace() {
		this.movementStep = new ArrayList<ZebraStep>();
		this.currentIndex = 0;
		
		//this.lastWaypoint = new Coord(rng.nextDouble() * 1000,
		//						      rng.nextDouble() * 1000);
		this.lastWaypoint = new Coord(rng.nextDouble() * getMaxX(),
								      rng.nextDouble() * getMaxY());
		
        double start_dir = rng.nextDouble() * (2*Math.PI);
        
        double current_x = this.lastWaypoint.getX();
        double current_y = this.lastWaypoint.getY();
        double current_direction = start_dir;
        
        double offset = rng.nextDouble() * 4;
        
        for (Double move : ZebraNetGeneratedMovement.DIST_LIST){
        	double move_distance = ZebraNetGeneratedMovement.DIST_LIST.get(rng.nextInt(ZebraNetGeneratedMovement.DIST_LIST.size()));
        
            double next_x = current_x + move_distance * Math.sin(current_direction);
            double next_y = current_y + move_distance * Math.cos(current_direction);
            
            double speed = move_distance / 8;
            
            // boundary effects
            if (next_x > getMaxX())
                next_x -= getMaxX();
            if (next_x < 0) 
                next_x += getMaxX();
            if (next_y > getMaxY())
                next_y -= getMaxY();
            if (next_y < 0)
                next_y += getMaxY();
            
            double time = (move+1)*8+offset;
            this.movementStep.add(new ZebraStep(new Coord(next_x, next_y), speed, time));
            
            current_direction += ZebraNetGeneratedMovement.DELTA_LIST.get(rng.nextInt(ZebraNetGeneratedMovement.DELTA_LIST.size()));
        }
        
        if (this.movementStep.size() == 0)
        	throw new IllegalArgumentException("Generation step creats no traces for this node");
	}
	
	
	/**
	 * Method called once to generate the lists of diretions and distance
	 * in movement model generator
	 */
	private void generateLists() {		
		if (!new File(ZEBRA_BASE_FILE).exists()) 
			throw new IllegalArgumentException("Generation files for Zebra movement doesn't exists");

		ZebraNetGeneratedMovement.DIST_LIST.clear();
        ZebraNetGeneratedMovement.DIRECTION_LIST.clear();
    	double x0 = 0;
    	double y0 = 0;
		int lineNumber = 0;
		
		try {
		    Scanner scanner = new Scanner(new FileInputStream(ZEBRA_BASE_FILE));
		
		    try {
		    	double x1 = 0;
		    	double y1 = 0;
		    	double distance, direction;
		    	
		        while (scanner.hasNextLine()){		
		        	lineNumber++;
		        	String[] arrayLine = scanner.nextLine().split("\\s+"); 
		        	if (arrayLine.length != 11)
		        		throw new IllegalArgumentException("Generation files for Zebra movement is not well formatted at line: "+lineNumber);
		        	x1 = Double.parseDouble(arrayLine[9]) / (6 * (1000 / getMaxX()));
		        	y1 = Double.parseDouble(arrayLine[10])/ (6 * (1000 / getMaxY()));	
		        	if (lineNumber > 1){
		        		distance = Math.sqrt( Math.pow((x1-x0),2) + Math.pow((y1-y0),2));
		                direction = Math.atan2(y1-y0,x1-x0);
		                ZebraNetGeneratedMovement.DIST_LIST.add(distance);
		                ZebraNetGeneratedMovement.DIRECTION_LIST.add(direction);
		        	}
		        	x0 = x1;
		        	y0 = y1;
		        	
		        }
		        
		    }
		    finally { scanner.close(); }
		}
		catch (Exception e) { 
			return; 
		}
		
		
		ZebraNetGeneratedMovement.DELTA_LIST.clear();
		lineNumber = 0;
		for (int index = 0; index < ZebraNetGeneratedMovement.DIRECTION_LIST.size(); index++) {
			if (lineNumber > 0)
				ZebraNetGeneratedMovement.DELTA_LIST.add(ZebraNetGeneratedMovement.DIRECTION_LIST.get(lineNumber) - 
														 ZebraNetGeneratedMovement.DIRECTION_LIST.get(lineNumber-1));	
			lineNumber++;
		}
	}
}
