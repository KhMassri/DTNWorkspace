package movement;

import input.WKTGridReader;
import input.WKTMapReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import movement.grid.GridMap;
import movement.map.SimMap;
import core.Coord;
import core.Settings;
import core.SimError;

public class GridBasedMovement extends MovementModel implements SwitchableMovement {
	/** sim map for the model */
	private GridMap map = null;
	
	/** Grid based movement model's settings namespace ({@value})*/
	public static final String GRID_BASE_MOVEMENT_NS = "GridBasedMovement";
	/** number of grid files -setting id ({@value})*/
	public static final String NROF_FILES_S = "nrofGridFiles";
	/** map file -setting id ({@value})*/
	public static final String FILE_S = "gridFile";
	
	
	/**
	 * Creates a new GridBasedMovement based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public GridBasedMovement(Settings settings) {
		super(settings);
		map = readMap();
	}
	
	protected GridBasedMovement(GridBasedMovement gbm) {
		super(gbm);
		
		map = gbm.map;
	}

	@Override
	public void setLocation(Coord lastWaypoint) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Coord getLastLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Path getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Coord getInitialLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MovementModel replicate() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	/**
	 * Returns the SimMap this movement model uses
	 * @return The SimMap this movement model uses
	 */
	public GridMap getMap() {
		return map;
	}
	
	/**
	 * Reads a sim map from location set to the settings, mirrors the map and
	 * moves its upper left corner to origo.
	 * @return A new SimMap based on the settings
	 */
	private GridMap readMap() {
		GridMap gridMap;
		Settings settings = new Settings(GRID_BASE_MOVEMENT_NS);
		WKTGridReader r = new WKTGridReader();
		
		try {
			int nrofMapFiles = settings.getInt(NROF_FILES_S);

			for (int i = 1; i <= nrofMapFiles; i++ ) {
				String pathFile = settings.getSetting(FILE_S + i);
				r.addCells(new File(pathFile));
			}
		} catch (IOException e) {
			throw new SimError(e.toString(),e);
		}

		gridMap = r.getMap();
		// mirrors the map (y' = -y) and moves its upper left corner to origo
		gridMap.mirror();
		Coord offset = gridMap.getMinBound().clone();		
		gridMap.translate(-offset.getX(), -offset.getY());

		return gridMap;
	}
}
