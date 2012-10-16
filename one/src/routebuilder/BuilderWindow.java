/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routebuilder;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Main window for the program. Takes care of layouting the main components
 * in the window.
 */
public class BuilderWindow extends JFrame implements MouseMotionListener, 
		MouseListener, MouseWheelListener, ChangeListener{
	private static final long serialVersionUID = 1L;
	private static final String WINDOW_TITLE = "ONE - Route and POI Builder";
	private static final int WIN_XSIZE = 1200;
	private static final int WIN_YSIZE = 575;
	// log panel's initial weight in the split panel 
	private static final double SPLIT_PANE_LOG_WEIGHT = 0.2;

	private JScrollPane playFieldScroll;
	private javax.swing.JSpinner zoomSelector;
	private MapView view;
	
	private Point start;

	public BuilderWindow(MapView view, JTextArea routetxt, JButton saveAsButton,
			JButton saveButton, JTextField filePathField, JButton loadButton,
			JButton appendButton) {
		super(WINDOW_TITLE);
		JFrame.setDefaultLookAndFeelDecorated(true);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		JPanel leftPane = new JPanel();
		leftPane.setLayout(new BoxLayout(leftPane,BoxLayout.PAGE_AXIS));
		JSplitPane fieldAreaSplit;

		setLayout(new BorderLayout());
		
		this.zoomSelector = new JSpinner(new SpinnerNumberModel(0.1, 0.001, Double.POSITIVE_INFINITY, 0.001));

		zoomSelector.addChangeListener(this);
		
		this.view = view;
		playFieldScroll = new JScrollPane(view);
		playFieldScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		playFieldScroll.getVerticalScrollBar().setUnitIncrement(50);
		playFieldScroll.getHorizontalScrollBar().setUnitIncrement(50);
		
		start = new Point();
		//mapVPort = playFieldScroll.getViewport();
		
		view.addMouseMotionListener(this);
		view.addMouseListener(this);
		view.addMouseWheelListener(this);

		fieldAreaSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				leftPane, new JScrollPane(routetxt));
		fieldAreaSplit.setResizeWeight(1-SPLIT_PANE_LOG_WEIGHT);
		fieldAreaSplit.setOneTouchExpandable(true);

		setPreferredSize(new Dimension(WIN_XSIZE, WIN_YSIZE));

		JPanel controls = new JPanel();
		controls.setLayout(new FlowLayout());
		controls.add(zoomSelector);
		
		
		controls.add(filePathField);
		
		
		controls.add(saveAsButton);
		controls.add(saveButton);
		controls.add(loadButton);
		controls.add(appendButton);
		
		leftPane.add(controls);
		leftPane.add(playFieldScroll);
		
		fieldAreaSplit.setOneTouchExpandable(true);
		fieldAreaSplit.setResizeWeight(0.60);        
		this.getContentPane().add(fieldAreaSplit);

		pack();
		fieldAreaSplit.setDividerLocation(0.8);
	}

	/**
	 * Returns a reference of the play field scroll panel
	 * @return a reference of the play field scroll panel
	 */
	 public JScrollPane getPlayFieldScroll() {
		return this.playFieldScroll;
	}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		Point now = e.getPoint();
		view.translate(now.x - start.x, now.y - start.y);
		view.updateField();
		
		start.setLocation(now);
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		start.setLocation(e.getPoint());
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		int delta = e.getWheelRotation();
		
		SpinnerNumberModel model = (SpinnerNumberModel) this.zoomSelector.getModel();
		double curZoom = model.getNumber().doubleValue();
		Number newValue = new Double(curZoom + model.getStepSize().doubleValue() * delta * curZoom*50); 
		
		// if the min number is greater than the new value (1 returned), set to min
		if(model.getMinimum().compareTo(newValue) > 0)
		{
			newValue = model.getNumber().doubleValue();
		}
			
		Point ref = e.getPoint();
		this.view.setScale(newValue.doubleValue(), ref);
		
		model.setValue(newValue);
	}

	public void stateChanged(ChangeEvent e)
	{
		if(e.getSource() == this.zoomSelector)
			updateZoomScale(true);
	}
	
	/**
	 * Updates zoom scale to the one selected by zoom chooser
	 * @param centerView If true, the center of the viewport should remain
	 * the same
	 */
	private void updateZoomScale(boolean centerView) {
		double scale = ((SpinnerNumberModel)zoomSelector.getModel()).getNumber().doubleValue();
		this.view.setScale(scale);
	}
	
	@Override public void mouseReleased(MouseEvent e){}
	@Override	public void mouseEntered(MouseEvent e){}
	@Override	public void mouseExited(MouseEvent e){}
	@Override public void mouseMoved(MouseEvent e){}
	@Override	public void mouseClicked(MouseEvent e){}

	
}
