package gui.playfield;

import java.awt.Color;
import java.awt.Graphics2D;

import core.Coord;

public class AreaGraphic  extends PlayFieldGraphic {
	private Coord lowLeft;
	private double width;
	private double height;
	private final Color BORDER_COLOR = Color.BLUE;
	private final Color BG_COLOR = Color.WHITE;
		
	public AreaGraphic(Coord lowLeft, Coord topRight) {
		this.lowLeft = lowLeft;
		this.width = topRight.getX()-lowLeft.getX();
		this.height = topRight.getY()-lowLeft.getY();
	}	
	

	@Override
	public void draw(Graphics2D g2) {
		g2.setColor(BORDER_COLOR);
		g2.setBackground(BG_COLOR);
		

		g2.drawLine(scale(lowLeft.getX()), scale(lowLeft.getY()),
				    scale(lowLeft.getX()+this.width), scale(lowLeft.getY()));
		
		g2.drawLine(scale(lowLeft.getX()), scale(lowLeft.getY()),
			    scale(lowLeft.getX()), scale(lowLeft.getY()+this.height));
		
		g2.drawLine(scale(lowLeft.getX()), scale(lowLeft.getY()+this.height),
			    scale(lowLeft.getX()+this.width), scale(lowLeft.getY()+this.height));
		
		g2.drawLine(scale(lowLeft.getX()+this.width), scale(lowLeft.getY()),
			    scale(lowLeft.getX()+this.width), scale(lowLeft.getY()+this.height));

		
	}	

}
