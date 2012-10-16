package movement.grid;

import input.WKTReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import movement.map.MapNode;
import movement.map.MapRoute;
import movement.map.SimMap;
import core.Coord;
import core.SettingsError;

public class GridCell implements  Cloneable, Comparable<GridCell> {

	private Coord center;	
	private Coord lowLeft;
	private Coord topLeft;
	private Coord lowRight;
	private Coord topRight;
	

	public GridCell(List<Coord> coords) {
		if (coords.size() != 5)
			throw new IllegalArgumentException("GridCell must be composed by 5 points");
			
		if (! coords.get(0).equals(coords.get(coords.size()-1)))
			throw new IllegalArgumentException("First and Last Coord have to be the same");

		this.lowRight = coords.get(0);
		this.lowLeft = coords.get(1);
		this.topLeft = coords.get(2);
		this.topRight = coords.get(3);
		
		this.center = new Coord( (lowLeft.distance(lowRight)/2d)+lowLeft.getX(), 
								 (lowLeft.distance(topLeft)/2d)+lowLeft.getY());		
	}
	
	public Coord getLowLeft() {
		return this.lowLeft;
	}
	
	public Coord getTopLeft() {
		return this.topLeft;
	}
	
	public Coord getLowRight() {
		return this.lowRight;
	}
	
	public Coord getTopRight() {
		return this.topRight;
	}
	
	public Coord getCenter() {
		return this.center;
	}
	
	public List<Coord> getPoints(){
		List<Coord> points = new ArrayList<Coord>();
		points.add(this.lowRight);
		points.add(this.lowLeft);
		points.add(this.topLeft);
		points.add(this.topRight);
		return points;
	}
	
	/**
	 * Verify if a point is inside this cell
	 * @param the point to verify
	 */
	public boolean isInside(Coord point) {
		return this.lowLeft.getX() <= point.getX() && point.getX() <= lowRight.getX() &&
			   this.lowLeft.getY() <= point.getY() && point.getY() <= topLeft.getY();
	}
	
	/**
	 * Moves the cell by dx and dy
	 * @param dx How much to move the point in X-direction
	 * @param dy How much to move the point in Y-direction
	 */
	public void translate(double dx, double dy) {
		this.center.translate(dx, dy);
		this.lowLeft.translate(dx, dy);
		this.topLeft.translate(dx, dy);
		this.lowRight.translate(dx, dy);
		this.topRight.translate(dx, dy);
	}
	
	/**
	 * Returns a text representation of cell (by center and lowLeft corner)
	 * @return a text representation of the cell
	 */
	@Override
	public String toString() {
		return String.format("(%.2f,%.2f)",center,lowLeft);
	}
	
	/**
	 * Returns a clone of this cell
	 */
	@Override
	public GridCell clone() {
		GridCell clone = null;
		try {
			clone = (GridCell) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return clone;
	}
	
	/**
	 * Checks if this center is equal to other center
	 * @param c The other cell
	 * @return True if cells are the same
	 */
	public boolean equals(GridCell c) {
		if (c == this) {
			return true;
		}
		else {
			return (center == c.center && center == c.center);
		}
	}

	@Override
	public boolean equals(Object o) {
		return equals((GridCell) o);
	}

	/**
	 * Returns a hash code for this coordinate
	 * (actually a hash of the String made of the coordinates)
	 */
	@Override
	public int hashCode() {
		return (center+","+lowLeft).hashCode();
	}
	
	/**
	 * Compares two cell by their center
	 * @param o The other MapNode
	 */
	@Override
	public int compareTo(GridCell o) {
		return this.getCenter().compareTo((o).getCenter());
	}
	

	/**
	 * Reads cells from files defined in Settings
	 * @param fileName name of the file where to read routes
	 * @param type Type of the route
	 * @param map SimMap where corresponding map nodes are found
	 * @return A list of MapCells that were read
	 */
	public static List<GridCell> readCells(String fileName,  GridMap map) {
		List<GridCell> cells = new ArrayList<GridCell>();
		WKTReader reader = new WKTReader();
		List<List<Coord>> polygons;
		File cellFile = null;
		//boolean mirror = map.isMirrored();
		double xOffset = map.getOffset().getX();
		double yOffset = map.getOffset().getY();
		
		try {
			cellFile = new File(fileName);
			polygons = reader.readPolygon(cellFile);
		}
		catch (IOException ioe){
			throw new SettingsError("Couldn't read GridCell-data file " + 
					fileName + 	" (cause: " + ioe.getMessage() + ")");
		}
		
		for (List<Coord> poly : polygons) {			
			List<Coord> cell = new ArrayList<Coord>();
			for (Coord c : poly) {
				// make coordinates match sim map data
				//if (mirror) 
				//	c.setLocation(c.getX(), -c.getY());
				
				c.translate(xOffset, yOffset);
				cell.add(c.clone());
			}
			
			cells.add(new GridCell(cell));
		}
		
		return cells;
	}
	
}
