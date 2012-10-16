package movement;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import core.Coord;
import core.Settings;

/**
 * ZebraNet real traces movement model
 * 
 * implemented following:
 * the real traces from ZebraNet project during the second simulation in summer of 2005.
 * Only 4 node of this type are allowed
 * 
 * @author Alessandro Vernata
 */
public class ZebraNetTracesMovement extends MovementModel {
	
	private class ZebraStep {
		public Coord pos;
		public Double speed;
		
		public ZebraStep(Coord pos, Double speed) {
			this.pos = pos;
			this.speed = speed;
		}
		
	}
	
	/** Seed to generate movement model for zebras */
	public static final String ZEBRA_BASE_FILE = "data/ZebraNet/baseoutUTM2.txt";
	
	/** Possibile zebra's ids contained in the traces **/
	public static final Integer[] ZEBRA_IDS = {1,6,8,10,14};
	
	private Integer zebraIndex;
	private Coord lastWaypoint;
	private List<ZebraStep> movementStep;
	private Integer currentIndex;
	
	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param s The Settings object where the settings are read from
	 */
	public ZebraNetTracesMovement(Settings settings) {
		super(settings);
		
		this.zebraIndex = 0;
		readTrace();
	}
	
	/**
	 * Copy constructor. 
	 * @param sm The ZebraNetGeneratedMovement prototype
	 */
	public ZebraNetTracesMovement(ZebraNetTracesMovement proto) {
		super(proto);
		
		this.zebraIndex = proto.zebraIndex+1;
		if (this.zebraIndex >= ZebraNetTracesMovement.ZEBRA_IDS.length)
			throw new IllegalArgumentException("More zebras instanziated than the one contained in the traces");
		readTrace();
	}
	
	@Override
	public Path getPath() {
		return null; //TODO: fix
	}
	
	@Override
	public ZebraNetTracesMovement replicate() {
		return new ZebraNetTracesMovement(this);
	}

	@Override
	public Coord getInitialLocation() {
		return this.lastWaypoint.clone();
	}
	
	
	private void readTrace() {
		currentIndex = 0;
		movementStep = new ArrayList<ZebraStep>();
		if (!new File(ZEBRA_BASE_FILE).exists()) 
			throw new IllegalArgumentException("Reading of file for Zebra movement doesn't exists");
		
		
		try {
		    Scanner scanner = new Scanner(new FileInputStream(ZEBRA_BASE_FILE));
		
		    double lowX = Double.MAX_VALUE;
		    double lowY = Double.MAX_VALUE;
		    
		    try {
		    	int absoulteLineNumber = 0;
		    	int lineNumber = 0;
		    	double x0 = 0;
		    	double y0 = 0;
		        while (scanner.hasNextLine()){	
		        	absoulteLineNumber++;
		        	String[] arrayLine = scanner.nextLine().split("\\s+"); 
		        	if (arrayLine.length != 20)
		        		throw new IllegalArgumentException("Traces from Zebra movement is not well formatted at line: "+absoulteLineNumber);
		        	
		        	int nodeId = Integer.parseInt(arrayLine[2]);
		        	if (nodeId != ZEBRA_IDS[this.zebraIndex])
		        		continue;
		        	lineNumber++;
		        	
		        	int month = Integer.parseInt(arrayLine[5]);
		        	int day = Integer.parseInt(arrayLine[6]);        	
		        	int hour = Integer.parseInt(arrayLine[8]);
		        	int minute = Integer.parseInt(arrayLine[9]);
		        	int t1 = month*(60*60*24*30) + day*(60*60*24) + hour*(60*60) + minute * (60);
		        	
		        	double x1 = Double.parseDouble(arrayLine[11]);
		        	if (x1 < lowX)
		        		lowX = x1;
		        	double y1 = Double.parseDouble(arrayLine[13]);		 
		        	if (y1 < lowY)
		        		lowY = y1;
		        	
		        	if (lineNumber == 1){
		        		this.lastWaypoint = new Coord(x1, y1);
		        	}
		        	else {
		        		double distance = Math.sqrt( Math.pow((x1-x0),2) + Math.pow((y1-y0),2));
		        		double speed = distance / t1;
		        		movementStep.add(new ZebraStep(new Coord(x1, y1), speed));
		        	}
		        	x0 = x1;
		        	y0 = y1;
		        }
		        
		        if (movementStep.size() == 0)
		        	throw new IllegalArgumentException("No traces has been readed");
		        
		        this.lastWaypoint.translate(-lowX, -lowY);
		        for (ZebraStep step : this.movementStep){
		        	step.pos.translate(-lowX, -lowY);
		        }
		        
		    }
		    catch (NullPointerException ex) {
		    	throw ex;
		    }
		    finally { scanner.close(); }
		}
		catch (Exception e) { 
			return; 
		}
	}

}
