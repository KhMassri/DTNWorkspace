/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package gui.playfield;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import movement.grid.GridCell;
import movement.grid.GridMap;
import movement.map.MapNode;
import movement.map.SimMap;
import core.Coord;

/**
 * PlayfieldGraphic for SimMap visualization
 *
 */
public class MapGraphic extends PlayFieldGraphic {
	private SimMap simMap;
	private GridMap gridMap;
	private final Color PATH_COLOR = Color.LIGHT_GRAY;
	private final Color BG_COLOR = Color.WHITE;
		
	public MapGraphic(SimMap simMap) {
		this.simMap = simMap;
	}	
	
	public MapGraphic(GridMap gridMap) {
		this.gridMap = gridMap;
	}
	
	// TODO: draw only once and store to buffer
	@Override
	public void draw(Graphics2D g2) {
		Coord c,c2;
		
		if (simMap == null && gridMap == null) {
			return;
		}
		
		g2.setColor(PATH_COLOR);
		g2.setBackground(BG_COLOR);
		
		if (simMap != null){
			// draws all edges between map nodes (bidirectional edges twice)
			for (MapNode n : simMap.getNodes()) {
				c = n.getLocation();
				
				// draw a line to adjacent nodes
				for (MapNode n2 : n.getNeighbors()) {
					c2 = n2.getLocation();
					g2.drawLine(scale(c2.getX()), scale(c2.getY()),
							scale(c.getX()), scale(c.getY()));
				}
			}
		}
		
		g2.setColor(Color.RED);
		if (gridMap != null){
			// draws the polygon
			for (GridCell cell : gridMap.getCells()) {
				List<Coord> listPoint = cell.getPoints();
				for (int i = 0; i < listPoint.size(); i++) {
					Coord p1 = listPoint.get(i);
					Coord p2 = listPoint.get( (i+1) % (listPoint.size()));
					
					g2.drawLine(scale(p2.getX()), scale(p2.getY()),
							scale(p1.getX()), scale(p1.getY()));					
				}
			}
		}
	}	

}
