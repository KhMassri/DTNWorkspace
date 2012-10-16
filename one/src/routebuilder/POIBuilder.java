package routebuilder;

import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import core.Coord;
import movement.map.MapNode;

public class POIBuilder extends RouteBuilder
{

	public POIBuilder(String filename)
	{
		super(filename);
	}
	
	protected void initGUI()
	{
		this.mapview = new MapView(this.map, route, false);
		
		this.routeText = new JTextArea("POINT (");
		routeText.setWrapStyleWord(true);
		routeText.setEditable(false);
		
		saveAsButton = new JButton("Save As...");
		saveButton = new JButton("Save");
		loadButton = new JButton("Load...");
		appendButton = new JButton("Append...");
		saveAsButton.addActionListener(this);
		saveButton.addActionListener(this);
		loadButton.addActionListener(this);
		appendButton.addActionListener(this);
		filePathField = new JTextField(20);
		filePathField.setEditable(false);
		
		this.mapview.addMouseListener(this);
		
		//this.guiControls = new GUIControls(this,this.mapview);
		this.main = new BuilderWindow(mapview, routeText, saveAsButton, saveButton, 
				filePathField, loadButton, appendButton);

		
		// if user closes the main window, call closeSim()
		this.main.addWindowListener(new WindowAdapter() {
			
			public void windowClosing(WindowEvent e)  {
					System.exit(0);
			}
		});

		this.main.setVisible(true);
	}
	
public void mouseClicked(MouseEvent e) {
		
		java.awt.Point p = e.getPoint();
		double distTo;
		
		Coord clickP = new Coord(mapview.windowToWorldX(p.x), mapview.windowToWorldY(p.y));
		
		// Check to see if we're unselecting a node
		if(route.size() > 0)
		{
			for(Iterator<MapNode> i = route.iterator(); i.hasNext(); )
			{
				MapNode n = i.next();
				distTo = n.getLocation().distance(clickP);
				if(distTo < mapview.scaleWindowToWorld(10))
				{
					i.remove();
					routeText.setText(this.makeText());
					mapview.updateField();
					return;
				}
			}
		}
		
		MapNode selected = null;
		
		for(MapNode n : map.getNodes())
		{
			distTo = n.getLocation().distance(clickP);
			if(distTo < mapview.scaleWindowToWorld(10))
			{
				selected = n;
			}
		}
		
		if(selected != null)
		{
			route.add(selected);
			routeText.setText(this.makeText());
			mapview.updateField();
		}
	}
	
	protected String makeText()
	{
		StringBuilder str = new StringBuilder();
		
		int size = route.size();
		for(int i = 0; i < size; i++)
		{
			MapNode n = route.get(i);
			str.append("POINT (");
			str.append(n.getLocation().getX());
			str.append(' ');
			str.append(n.getLocation().getY());
			str.append(")\n");
		}
		
		return str.toString();
	}
	
	protected void readFileIntoRoute(File file)
	{
		try
		{
			Scanner scanner = new Scanner(file);
			scanner.useDelimiter("[ \\(\\),\n]+");
			
			//else System.out.println("First token: " + s);
			while(scanner.hasNext())
			{
				double x, y;
				
				String s = scanner.next();
				if(!s.equals("POINT"))
				{
					System.err.print("Line does not start with 'POINT'. Skipping. Tokens missed: ");
					System.err.println(scanner.next(".*"));
					continue;
				}
				
				if(scanner.hasNextDouble())
					x = scanner.nextDouble();
				else if(scanner.hasNext())
				{
					System.err.println("Unexpected token: '" + scanner.next() + '\'');
					continue;
				}
				else continue;
				if(scanner.hasNextDouble())
					y = scanner.nextDouble();
				else continue;
				
				//System.out.println("x: " + x + " y: " + y);
				MapNode n = map.getNodeByCoord(new Coord(x, y));
				if(n == null) System.err.println("No node in map for coord: " + x + ',' + ' ' + y);
				else 
					route.add(n);
			}
			
			routeText.setText(this.makeText());
			mapview.updateField();
		}
		catch(IOException ioe) {ioe.printStackTrace();}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		if(args.length < 1)
		{
			System.err.println("Please specify a map filename on the command line: " + args[0]);
		}
		new POIBuilder(args[0]);

	}

}
