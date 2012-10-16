package routebuilder;

import input.WKTMapReader;

import java.util.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import javax.swing.*;

import core.*;
import movement.map.*;

public class RouteBuilder extends MouseAdapter implements ActionListener
{
	protected MapView mapview;
	protected routebuilder.BuilderWindow main;
	protected SimMap map;
	protected JTextArea routeText;
	protected JButton saveAsButton;
	protected JButton saveButton;
	protected JButton loadButton;
	protected JButton appendButton;
	protected JTextField filePathField;
	protected List<MapNode> route;
	
	protected File fileForSaving;
	
	public RouteBuilder(String filename)
	{
		WKTMapReader r = new WKTMapReader(true);
		
		try
		{
			r.addPaths(new File(filename), 0);
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(-1);
		}
		
		this.map = r.getMap();
		checkMapConnectedness(map.getNodes());
		
		route = new ArrayList<MapNode>();
		fileForSaving = null;
		
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
			    public void run() {
					try {
						initGUI();
					} catch (AssertionError e) {
						e.printStackTrace();
					}
			    }
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * Initializes the GUI
	 */
	protected void initGUI() {	
		this.mapview = new MapView(this.map, route, true);
		
		this.routeText = new JTextArea("LINESTRING (");
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
			MapNode n = route.get(route.size() - 1);
			distTo = n.getLocation().distance(clickP);
			if(distTo < mapview.scaleWindowToWorld(10))
			{
				route.remove(route.size() - 1);
				routeText.setText(this.makeText());
				mapview.updateField();
				return;
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
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		if(args.length < 1)
		{
			System.err.println("Please specify a map filename on the command line: " + args[0]);
		}
		new RouteBuilder(args[0]);

	}

  
  /**
   * Returns the parent frame (window) of the gui.
   * @return The parent frame
   */
  public BuilderWindow getParentFrame() {
  	return this.main;
  }
	
	protected String makeText()
	{
		StringBuilder str = new StringBuilder();
		str.append("LINESTRING (");
		int size = route.size();
		for(int i = 0; i < size; i++)
		{
			MapNode n = route.get(i);
			str.append(n.getLocation().getX());
			str.append(' ');
			str.append(n.getLocation().getY());
			if(i != size-1)
			{
				str.append(',');
				str.append(' ');
			}
		}
		
		str.append(')');
		return str.toString();
	}

	/**
	 * Checks that all map nodes can be reached from all other map nodes
	 * @param nodes The list of nodes to check
	 * @throws SettingsError if all map nodes are not connected
	 */
	private void checkMapConnectedness(List<MapNode> nodes) {
		Set<MapNode> visited = new HashSet<MapNode>();
		Queue<MapNode> unvisited = new LinkedList<MapNode>();
		MapNode firstNode = nodes.get(0);
		MapNode next = null;
		
		visited.add(firstNode);
		unvisited.addAll(firstNode.getNeighbors());
		
		while ((next = unvisited.poll()) != null) {
			visited.add(next);
			for (MapNode n: next.getNeighbors()) {
				if (!visited.contains(n) && ! unvisited.contains(n)) {
					unvisited.add(n);
				}
			}
		}
		
		if (visited.size() != nodes.size()) { // some node couldn't be reached
			MapNode disconnected = null;
			for (MapNode n : nodes) { // find an example node
				if (!visited.contains(n)) {
					disconnected = n;
					break;
				}
			}
			throw new SettingsError("SimMap is not fully connected. Only " + 
					visited.size() + " out of " + nodes.size() + " map nodes " +
					"can be reached from " + firstNode + ". E.g. " + 
					disconnected + " can't be reached");
		}
	}
	
	protected void saveFile()
	{
		if(fileForSaving == null) return;
		try
		{
			PrintWriter out = new PrintWriter(fileForSaving);
			out.println(this.makeText());
			out.flush();
			out.close();
			out = null;
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	protected void readFileIntoRoute(File file)
	{
		try
		{
			Scanner scanner = new Scanner(file);
			scanner.useDelimiter("[ \\(\\),\n]+");
			String s = scanner.next();
			if(!s.equals("LINESTRING"))
			{
				System.err.println("Text does not start with 'LINESTRING'. Starting token: " + s);
				return;
			}
			//else System.out.println("First token: " + s);
			while(scanner.hasNext())
			{
				double x, y;
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
				if(n == null)
					System.err.println("No map node for coord: " +x+','+' '+y);
				else
					route.add(n);
			}
			
			routeText.setText(this.makeText());
			mapview.updateField();
		}
		catch(IOException ioe) {ioe.printStackTrace();}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object o = e.getSource();
		if(o == saveAsButton || (o == saveButton && fileForSaving == null))
		{
			JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if(chooser.showDialog(main, "Save") == JFileChooser.APPROVE_OPTION)
			{
				this.fileForSaving = chooser.getSelectedFile();
				filePathField.setText(fileForSaving.getAbsolutePath());
				saveFile();
			}
			
		}
		else if(o == saveButton)
		{
			saveFile();
		}
		else if(o == loadButton)
		{
			JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if(chooser.showDialog(main, "Load") == JFileChooser.APPROVE_OPTION)
			{
				fileForSaving = chooser.getSelectedFile();
				filePathField.setText(fileForSaving.getAbsolutePath());
				this.route.clear();
				this.readFileIntoRoute(fileForSaving);
			}
		}
		else if(o == appendButton)
		{
			JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if(chooser.showDialog(main, "Load") == JFileChooser.APPROVE_OPTION)
			{
				this.readFileIntoRoute(chooser.getSelectedFile());
			}
			
		}
	}
}
