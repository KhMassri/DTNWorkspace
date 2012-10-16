package movement.grid;

import java.io.Serializable;
import java.util.List;

import movement.map.MapNode;

import core.Coord;

public class GridMap implements Serializable {
	
	private static final long serialVersionUID = 7526471155622776147L;

	private Coord offset;
	private Coord minBound;
	private Coord maxBound;
	
	private List<GridCell> cells;
	/** is this map data mirrored after reading */
	private boolean isMirrored;
	
	
	public GridMap(List<GridCell> cells) {
		this.offset = new Coord(0,0);
		this.cells = cells;
		this.isMirrored = false;
		setBounds();
	}
	
	/**
	 * Returns all the map cells in a list
	 * @return all the map cells in a list
	 */
	public List<GridCell> getCells() {
		return this.cells;
	}
	
	/**
	 * Returns a Cell at given coordinates or null if there's no Cell
	 * in the location of the coordinate
	 * @param c The coordinate
	 * @return The cell in that location or null if it doesn't exist
	 */
	public GridCell getNodeByCoord(Coord c) {
		GridCell result = null;
		for (GridCell cell : getCells()){
			if (cell.isInside(c)){
				result = cell;
				break;
			}
		}
 	
		return result;
	}
	
	/**
	 * Returns the upper left corner coordinate of the map
	 * @return the upper left corner coordinate of the map
	 */
	public Coord getMinBound() {
		return this.minBound;
	}

	/**
	 * Returns the lower right corner coordinate of the map
	 * @return the lower right corner coordinate of the map
	 */
	public Coord getMaxBound() {
		return this.maxBound;
	}
	
	/**
	 * Returns true if this map has been mirrored after reading
	 * @return true if this map has been mirrored after reading
	 * @see #mirror()
	 */
	public boolean isMirrored() {
		return this.isMirrored;
	}
	
	/**
	 * Translate whole map by dx and dy
	 * @param dx The amount to translate X coordinates
	 * @param dy the amount to translate Y coordinates
	 */
	public void translate(double dx, double dy) {
		for (GridCell cell : cells) {
			cell.translate(dx, dy);
		}
		
		minBound.translate(dx, dy);
		maxBound.translate(dx, dy);
		offset.translate(dx, dy);
	}
	
	/**
	 * Returns the offset that has been caused by translates made to 
	 * this map (does NOT take into account mirroring).
	 * @return The current offset
	 */
	public Coord getOffset() {
		return this.offset;
	}
	
	/**
	 * Mirrors all map coordinates around X axis (x'=x, y'=-y). 
	 */
	public void mirror() {
		assert !isMirrored : "Map data already mirrored";
	
		for (GridCell cell : cells) {
			cell.translate(0,- ((cell.getLowLeft().getY()*2) + (cell.getTopLeft().getY() - cell.getLowLeft().getY())) );
			//for (Coord c : cell.getPoints())
			//	c.setLocation(c.getX(), -c.getY());
		}
		setBounds();
		this.isMirrored = true;
	}

	private void setBounds() {
		double minX, minY, maxX, maxY;
		minX = minY = Double.MAX_VALUE;
		maxX = maxY = -Double.MAX_VALUE;
		
		for (GridCell cell : this.cells) {
			if (cell.getLowLeft().getX() < minX) {
				minX = cell.getLowLeft().getX();
			}
			if (cell.getLowRight().getX() > maxX) {
				maxX = cell.getLowRight().getX();
			}
			if (cell.getLowLeft().getY() < minY) {
				minY = cell.getLowLeft().getY();
			}
			if (cell.getTopLeft().getY() > maxY) {
				maxY = cell.getTopLeft().getY();
			}
		}
		minBound = new Coord(minX, minY);
		maxBound = new Coord(maxX, maxY);
	}
	
	/**
	 * Returns a String representation of the map
	 * @return a String representation of the map
	 */
	@Override
	public String toString() {
		return this.cells.toString();
	}
	
}
