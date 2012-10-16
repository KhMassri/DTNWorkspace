package routebuilder;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.*;

import javax.swing.*;

import movement.map.MapNode;
import movement.map.SimMap;

import core.*;

public class MapView extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Color PATH_COLOR = Color.LIGHT_GRAY;
	private static final Color NODE_COLOR = Color.RED;
	private static final Color BG_COLOR = Color.WHITE;
	private static final Color ROUTE_COLOR = Color.BLUE;
	private static final Color ROUTE_NODE_COLOR = Color.GREEN;
	
	private SimMap map;
	
	private Collection<MapNode> nodesToDraw;
	
	private boolean drawlines;
	private double scale;
	private double offsetX;
	private double offsetY;
	
	public MapView(SimMap m, Collection<MapNode> nodesDataStructure, boolean drawPath)
	{
		this.map = m;
		this.nodesToDraw = nodesDataStructure;
		this.drawlines = drawPath;
		scale = 0.1;
		Coord min = map.getMinBound(), max = map.getMaxBound();
		
		offsetX = -min.getX() * scale;
		offsetY = max.getY() * scale;
		//System.out.println("Map Offset: " + offsetX + ' ' + offsetY + ' ' + min + ' ' + max);
		updateFieldSize();
		
		this.setBackground(BG_COLOR);
		this.setMaximumSize(null);
	}

	/**
	 * Draws the play field. To be called by Swing framework or directly if
	 * different context than screen is desired
	 * @param g The graphics context to draw the field to
	 */
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setBackground(BG_COLOR);

		// clear old playfield graphics
		g2.clearRect(0, 0, this.getWidth(), this.getHeight());
		AffineTransform save = g2.getTransform();
		g2.translate(offsetX, offsetY);
		g2.scale(scale, -scale);
		
	
		
		g2.setBackground(BG_COLOR);
		// draws all edges between map nodes (bidirectional edges twice)
		Coord c, c2;
		int dotsize = Math.max(3, (int)(3/scale));
		for (MapNode n : map.getNodes()) 
		{
			c = n.getLocation();
			
			// draw a line to adjacent nodes
			for (MapNode n2 : n.getNeighbors()) 
			{
				c2 = n2.getLocation();
			
				g2.setColor(PATH_COLOR);
				g2.drawLine((int)c2.getX(), (int)c2.getY(), (int)c.getX(), (int)c.getY());
			
				g2.setColor(NODE_COLOR);
				g2.fillOval((int)c2.getX(), (int)c2.getY(), dotsize, dotsize);
			}
			g2.setColor(NODE_COLOR);
			g2.drawOval((int)c.getX(), (int)c.getY(), dotsize, dotsize);
		}
		
		Iterator<MapNode> i = this.nodesToDraw.iterator();
		dotsize = Math.max(5, (int)(5/scale));
		if(i.hasNext())
		{
			MapNode n = i.next();
			c = n.getLocation();
			g2.setColor(ROUTE_NODE_COLOR);
			g2.fillRect((int)c.getX(), (int)c.getY(), dotsize, dotsize);
			
			
			for(; i.hasNext(); )
			{
				n = i.next();
				c2 = n.getLocation();
				if(drawlines)
				{
					g2.setColor(ROUTE_COLOR);
					g2.drawLine((int)c2.getX(), (int)c2.getY(), (int)c.getX(), (int)c.getY());
				}
				
				g2.setColor(ROUTE_NODE_COLOR);
				g2.fillRect((int)c.getX(), (int)c.getY(), dotsize, dotsize);
				c = c2;
			}
			g2.setColor(Color.CYAN);
			g2.fillRect((int)c.getX(), (int)c.getY(), dotsize, dotsize);
		}
		
		// draw reference scale
//		this.refGraphic.draw(g2);
		g2.setTransform(save);
	}
	
	
	/**
	 * Sets the zooming/scaling factor
	 * @param scale The new scale
	 */
	public void setScale(double scale, Point ref) {
		//System.out.println("Mouse pos: " + ref.x + " " + ref.y);
		
		double myX = (ref.x - offsetX) / this.scale, 
					myY = (offsetY - ref.y) / this.scale;
		
		this.scale = scale;
		this.offsetX = ref.x - myX * scale;
		this.offsetY = ref.y + myY * scale;
		
		
		this.updateFieldSize();

	}
	
	public void setScale(double scale)
	{
		this.scale = scale;
		this.updateFieldSize();
	}
	
	public void translate(int dx, int dy)
	{
		offsetX += dx;
		offsetY += dy;
		updateFieldSize();
	}
	
	public double getScale() { return scale; }
	
	/**
	 * Schedule the play field to be drawn
	 */
	public void updateField() {
		this.repaint();
	}
	
	/**
	 * Updates the playfields (graphical) size to match the world's size
	 * and current scale/zoom.
	 */ 
	private void updateFieldSize()
	{
		Coord max = map.getMaxBound();
		Dimension minSize = new Dimension(
				(int)(max.getX() * scale + offsetX),
				(int)(max.getY() * scale + offsetY));
		this.setMinimumSize(minSize);
		this.setPreferredSize(minSize);
		this.setSize(minSize);
	}
	
	/*private double worldToWindowX(double value) { return scale*value - offsetX; }
	private double worldToWindowY(double value) { return -scale*value + offsetY; }*/
	public double windowToWorldX(double value) { return (value - offsetX) / scale; }
	public double windowToWorldY(double value) { return (offsetY - value) / scale; }
	public double scaleWindowToWorld(double value)
	{
		return value / scale; }
	//private int scale(double value) {return (int)(scale*value); }
}
