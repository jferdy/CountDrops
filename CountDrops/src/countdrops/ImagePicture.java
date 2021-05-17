package countdrops;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.JFrame;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.plugin.AVI_Reader;

public class ImagePicture implements ViewWellListener {
	private CountDrops gui;
	
	private Picture p;

	private ImagePlus imp = null;
	private Overlay     overlay = null;
	private ImageCanvas canvas  = null;
	private ImageWindow window = null;

	private PlateTriangle triangle = null;  //the triangle used to create a new plate
	
	private Plate selectedPlate = null;
	private ArrayList<Well>     openedWell   = new ArrayList<Well>();
	private ArrayList<ViewWell> openedViewWell = new ArrayList<ViewWell>();

	private WindowListener windowListener = new WindowListener() {
		public void windowClosed(WindowEvent e) { close();}
		public void windowOpened(WindowEvent e) {}
		public void windowDeactivated(WindowEvent e) {}
		public void windowIconified(WindowEvent e) {}
		public void windowClosing(WindowEvent e) {}
		public void windowActivated(WindowEvent e) {}
		public void windowDeiconified(WindowEvent e) {}
	};

	private Boolean isClickable = true; 

	private KeyListener canvasKeyListener = new KeyListener() {	
		public void keyPressed(KeyEvent evt) {
			//System.out.println("Vlan");

			boolean CTRLpressed=false;
			boolean ALTpressed=false;
			//boolean SHIFTpressed=false;

			if(evt.isControlDown()) {
				CTRLpressed = true;
			} 
			if(evt.isAltDown()) {
				ALTpressed = true;
			}
//			if(evt.isShiftDown()) {
//				SHIFTpressed = true;
//			}


			switch (evt.getKeyCode()) {
			case KeyEvent.VK_ESCAPE: {
				close();
				break;
			}

			case KeyEvent.VK_UP:
			case KeyEvent.VK_KP_UP:
				if (ALTpressed && !CTRLpressed) {		
					//System.out.println("In");
					zoomIn();
				}
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_KP_DOWN:
				if (ALTpressed && !CTRLpressed) {
					System.out.println("Out");
					zoomOut();
				}
				break;
			default : {
				break;
			}
			}
		}

		public void keyReleased(KeyEvent arg0) {
			// TODO Auto-generated method stub

		}

		public void keyTyped(KeyEvent arg0) {
			// TODO Auto-generated method stub		
		}
	};
	
	private MouseListener canvasMouseListener = new MouseListener() {
		public void mouseEntered (MouseEvent e) {}
		public void mouseExited (MouseEvent e) {}
		public void mousePressed (MouseEvent e) {
			if(isClickable && triangle!=null) {
				// plate is about to be created : pick up a corner of the triangle
				overlay.clear();
				selectTriangleCorner(e);
			}
			return;
		}    
		public void mouseReleased (MouseEvent e) {
			if(isClickable && triangle!=null) {
				// plate is about to be created : pick up a corner of the triangle
				overlay.clear();
				unselectTriangleCorner();
			}
			return;			
		}
		public void mouseClicked(java.awt.event.MouseEvent e) {
			if(isClickable) {
				if(triangle==null) {
					// pick up a well in a plate
					getWellFromPosition(e);
				} else {
					// a plate is about to be created : pick up a corner of the triangle
					overlay.clear();
					selectTriangleCorner(e);
				}
			}
			return;
		}
	};			

	private MouseMotionListener canvasMouseMotionListener = new MouseMotionListener() {
		public void mouseDragged(MouseEvent e) {
			if(!isClickable || triangle==null) return;
			if(triangle.getSelectedPoint()<0) return;
			
			overlay.clear();
			moveTriangleCorner(e);
		}	    
		
		public void mouseMoved(MouseEvent e) {}
	};

	private PlateDilutionSettingsListener plateDilutionSettingsListener = new PlateDilutionSettingsListener() {
		@Override
		public void DimentionHasChanged(PlateDilutionSettingsEvent evt) {
			if(triangle!=null) {
				triangle.setNCOLS(evt.getNCOLS());
				triangle.setNROWS(evt.getNROWS());
				drawTriangle();
			}
		}		
	};

	public ImagePicture(Picture xp,CountDrops g) throws Exception {
		p = xp;
		gui =g;
		
		if(p.getFileName().endsWith("avi")) {
			//AVI files loaded as virtual stacks pose a problem: the plate disappears after a ViewWell 
			//instance is created. Virtual stacks are disabled here by virtual set to false.					
			imp = AVI_Reader.open(p.getPath()+p.getFileName(),false);
		} else {			
			imp = IJ.openImage(p.getPath()+p.getFileName());
		}
		if(imp==null) throw new Exception("Failed to open image "+p.getPath()+p.getFileName());
		overlay = new Overlay();
		imp.setOverlay(overlay);

		canvas = new ImageCanvas(imp);
		canvas.setOverlay(overlay);

		//remove all default listeners
		for(MouseListener o1 : canvas.getMouseListeners()) canvas.removeMouseListener(o1);		
		for(MouseMotionListener o2 : canvas.getMouseMotionListeners()) canvas.removeMouseMotionListener(o2);	
		for(KeyListener o3 : canvas.getKeyListeners()) canvas.removeKeyListener(o3);
		//add new custom listeners
		canvas.addMouseListener(canvasMouseListener);
		canvas.addMouseMotionListener(canvasMouseMotionListener);		
		canvas.addKeyListener(canvasKeyListener);			
		 
	}

	public Overlay getOverlay() { return overlay;}
	public ImagePlus getImagePlus() {return imp;}
	public ImageCanvas getImageCanvas() {return canvas;}
	public ImageWindow getImageWindow() {return window;}

	public PlateDilutionSettingsListener getPlateDilutionSettingsListener() {
		return plateDilutionSettingsListener;
	}

	public void setPlateDilutionSettingsListener(
			PlateDilutionSettingsListener plateDilutionSettingsListener) {
		this.plateDilutionSettingsListener = plateDilutionSettingsListener;
	}

	public void setIsClickable(Boolean b) { isClickable = b; }

	public PlateTriangle getTriangle() {
		return triangle;
	}
	
	public void setTriangle(PlateTriangle t) {
		triangle = t;
		if(triangle!=null) {
			drawTriangle();			
		} else {
			overlay.clear();
			imp.repaintWindow();
		}
	}

	public void unselectTriangleCorner() {		
		triangle.setSelectedPoint(-1);
		drawTriangle();
	}

	public void selectTriangleCorner(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();

		x = canvas.offScreenX(x);
		y = canvas.offScreenY(y);
				
		triangle.setSelectedPoint(x,y);
		drawTriangle();
	}

	public void moveTriangleCorner(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();

		x = canvas.offScreenX(x);
		y = canvas.offScreenY(y);
				
		triangle.moveSelectedPoint(x,y);
		drawTriangle();
	}

	public void drawTriangle() {
		overlay.clear();
		for(int i=0;i<p.getNbPlates();i++) {
			p.getPlate(i).drawOutline(overlay);
		}
		triangle.draw(overlay);	
		imp.repaintWindow();
	}
	
	public void getWellFromPosition(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();

		x = canvas.offScreenX(x);
		y = canvas.offScreenY(y);

		if(selectedPlate == null) {
			//selectedPlate is not initialized: look for plate and well
			for(int i=0;i<p.getNbPlates();i++) {    		
				selectedPlate = p.getPlate(i);

				Well w = selectedPlate.contains(x,y);    	    
				if(w!=null) {
					openViewWell(w);
					return;
				}
			}
		} else {
			//selectedPlate is initialized: just look for well
			Well w = selectedPlate.contains(x,y);    	    
			if(w!=null) {
				openViewWell(w);
				return;
			}
		}
	}

	public void show() {
		try {
			window = new ImageWindow(imp,canvas);
			window.setVisible(true);			
			window.addWindowListener(windowListener);			
		} catch ( Exception e) {		
		}
	}

	public void close() {
		//close ViewWell instance when the ImageWindow that created them is closing
		for(int i=0;i<openedWell.size();i++) {
			openedViewWell.get(i).close();
		}
		openedWell.clear();
		openedViewWell.clear();

		window.setVisible(false);		
		window = null;				
	}

	public void openViewWell(Well w) {		
		
		  openViewWell(w,null);
		 	
		//openViewWell(w,null);
	}
	
	public void openViewWell(Well w,ViewWellEvent evt) {		
		if(w==null) return;

		if(w.isLocked()) {

			Object[] options = { "NO", "YES" };
			int i = JOptionPane.showOptionDialog(null, 
					"Well "+w.getName()+" has been locked, probably because it is currently being edited! Do you want to unlock and proceed?", 
					"Well inspection",
					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
					null, options, options[0]);			
			if (i==1) {
				w.unlock();
			}		    		    		    
		}
		if(!w.isLocked()) {
			SampleStatistics stat = gui.getStatistics(w);
			
			ImageWell iw = new ImageWell(imp,selectedPlate,w);
			
			if(evt==null ) { //create ViewWellEvent
			  Point loc = null; 
			  GraphicsDevice device;
			  if(openedViewWell.size()>0) { 
				  device = openedViewWell.get(openedViewWell.size()-1).getGraphicsConfiguration().getDevice();
				  loc = openedViewWell.get(openedViewWell.size()-1).getLocation(); 
			  } else { 
				  device = this.getImageWindow().getGraphicsConfiguration().getDevice();
				  loc = this.getImageWindow().getLocation(); 				  
			  } 
			  
			  evt = new ViewWellEvent(w,loc);
			  evt.setScreen(device);
			}
			  
			
			evt.setXreversed(selectedPlate.isXreversed());
			evt.setYreversed(selectedPlate.isYreversed());
				
//			GraphicsDevice device; 			
//			if(openedViewWell.size()>0) {
//				//vw on the same screen than the last opened ViewWell
//				device = openedViewWell.get(openedViewWell.size()-1).getGraphicsConfiguration().getDevice();
//			} else {			
//				//vw on the same screen than plate image
//				device = this.getImageWindow().getGraphicsConfiguration().getDevice(); 
//			}
//			evt.setScreen(device);
			
			
			ViewWell  vw = new ViewWell(iw,evt,stat);
			
			vw.addListener(this); //ImagePicture listens to ViewWell
			vw.addListener(gui);  //main gui also listens to adjust ExperimentTree to changes in CFU number
			
			
			/*
			 * GraphicsDevice device; if(openedViewWell.size()>0) { //vw on the same screen
			 * than the last opened ViewWell device =
			 * openedViewWell.get(openedViewWell.size()-1).getGraphicsConfiguration().
			 * getDevice(); } else { //vw on the same screen than plate image device =
			 * this.getImageWindow().getGraphicsConfiguration().getDevice(); }
			 * //device.setFullScreenWindow( vw );
			 * 
			 * //move vw to the right screen int width =
			 * device.getDefaultConfiguration().getBounds().width; int height =
			 * device.getDefaultConfiguration().getBounds().height; vw.setLocation( ((width
			 * / 2) - (vw.getSize().width / 2)) +
			 * device.getDefaultConfiguration().getBounds().x, ((height / 2) -
			 * (vw.getSize().height / 2)) + device.getDefaultConfiguration().getBounds().y
			 * );
			 */	        
			
			openedWell.add(w);
			openedViewWell.add(vw);

			overlay.clear();
			drawPlate(selectedPlate);
		}
	}

	public void selectPlate(int i) {
		if(i<0 || i>=p.getNbPlates()) return;
		selectedPlate = p.getPlate(i);
			
		drawPlate(selectedPlate);		
	}

	public void drawPlate(Plate pl) {
		if(pl==null) return;		
		pl.draw(overlay);				
		imp.updateAndRepaintWindow();		
	}

	public void drawPlate(int i) {
		if(i<0 || i>=p.getNbPlates()) return;
		drawPlate(p.getPlate(i));
	}

	public void newViewWellAsked(ViewWellEvent evt) {
		if(selectedPlate==null) return;
		
		Well w = evt.getWell();
		Point loc = new Point(evt.getX(),evt.getY());
		//System.out.print(loc.getX()+" "+loc.getY()+" -> ");
		
//		boolean closeWhenMoving = evt.isCloseWhenMoving();
//		boolean doWand = evt.isDoWand();
//		boolean changeType = evt.isChangeType();
//		int selectedType = evt.getSelectedType();
//		double mag = evt.getCanvasMagnification();
//		Dimension windowSize = evt.getWindowSize();
		
		if(openedWell.contains(w)) {
			//opens a new ViewWell instance only if the clicked well belongs to selectedPlate !
			int row = w.getRowInPlate();
			int col = w.getColInPlate();			
			if(evt.getAction().equals("MOVERIGHT")) {
				col++;
				if(col>=selectedPlate.getNCOLS()) return;
			}
			if(evt.getAction().equals("MOVELEFT")) {
				col--;
				if(col<0) return;
			}
			if(evt.getAction().equals("MOVEUP")) {
				row--;
				if(row<0) return;
			}
			if(evt.getAction().equals("MOVEDOWN")) {
				row++;
				if(row>selectedPlate.getNROWS()) return;
			}
			Well w2 = selectedPlate.getWell(row,col);
			if(w2!=null) {				
				if(!evt.isCloseWhenMoving()) {
					loc.setLocation(loc.getX()+100,loc.getY()+50);
				}
				//open a new ViewWell instance
				openViewWell(w2,evt);
				
			} else {
				GenericDialog gd2 = new GenericDialog("Well inspection");
				gd2.addMessage("w2 is null!");
				gd2.showDialog();
			}

		}
	}

	public void viewWellCopyState(ViewWellEvent evt) {
		//TODO must copy state of well to other wells of the selected plate
		//A small issue here: what should I do if a state is copied on a well which is under inspection (and therefore locked?)
		//It is probably safer to ignore this well, but at least this should be checked first and signaled to user then.

		Well w1 = evt.getWell();
		int minRow = w1.getRowInPlate();
		int maxRow = w1.getRowInPlate();
		int minCol = w1.getColInPlate();
		int maxCol = w1.getColInPlate();
		if(! evt.getAction().equals("COPYTOROW")) {
			minRow=0;
			maxRow=selectedPlate.getNROWS()-1;
		}
		if(! evt.getAction().equals("COPYTOCOL")) {
			minCol=0;
			maxCol=selectedPlate.getNCOLS()-1;
		}
		String msg = "";
		for(int i=minRow;i<=maxRow;i++) {
			for(int j=minCol;j<=maxCol;j++) {
				Well w2 = selectedPlate.getWell(i,j);
				if(w1!=w2) {
					if(!w2.isLocked()) {
						//target well is not locked: states are copied from w1 to w2
						//non countable state must be copied first so that empty state can be modified adequately
						for(int k=0;k<=w1.getNCFUTYPES();k++)
							w2.setNonCountable(k,w1.isNonCountable(k));
						w2.setEmpty(w1.isEmpty());
						w2.setIgnored(w1.isIgnored());						
						w2.write();
					} else {
						//target is locked: nothing can be copied but a warning will be issued.
						if(msg.length()>0) msg =msg+", ";
						msg = msg+w2.getName();
					}
				}
			}
		}
		overlay.clear();
		drawPlate(selectedPlate);
		if(msg.length()>0) {
			//some wells have not been updated because they were locked
			JOptionPane.showMessageDialog(null,"The following well(s) have not been updated because they were locked:"+msg, "Well inspection", JOptionPane.WARNING_MESSAGE);
			System.out.println("Well locked!");
		}
	}

	public void closeViewWell(Well w) {
		if(openedWell.contains(w)) {
			int i = openedWell.indexOf(w);
			openedViewWell.set(i,null); //might be useful to trigger gc?? 
			openedViewWell.remove(i);
			openedWell.remove(i);	
		}
	}
	
	public void viewWellHasClosed(ViewWellEvent evt) {
		//remove Well and ViewWell from the lists openedWell and openedViewWell
		Well w = evt.getWell();
		closeViewWell(w);
		overlay.clear();
		drawPlate(selectedPlate);
		System.gc();
	}

	public void viewWellChange(ViewWellEvent evt) {
		//a well has changed: redraw the plate
		overlay.clear();
		drawPlate(selectedPlate);
	}

	@Override
	public void autoDetectRow(ViewWellEvent evt) {
		final AutoDetect ad = evt.getAutoDetect();
		final Well w1 = evt.getWell();
		final int slice = evt.getSlice();
		final int row = w1.getRowInPlate();
		
		final ProgressDialog pgDlg = new  ProgressDialog(ad,"Loading experiment",selectedPlate.getNCOLS());
		pgDlg.setVisible(true);
		CountDrops.setEnableComponents(ad,false);
		CountDrops.setWaitCursor(ad);
		
		SwingWorker<Integer, String> sw = new SwingWorker<Integer, String>(){
			@Override
			protected Integer doInBackground() throws Exception {
				int cmpt = 0;
				try {
					for(int col=0;col<selectedPlate.getNCOLS();col++) {
						Well w2 = selectedPlate.getWell(row,col);			
						publish("Looking for CFUs in well "+w2.getName());
						if(w1==w2 || !w2.isLocked()) {
							ad.apply(w2.getImagePlus(getImagePlus()),w2,slice);
							cmpt++;
						}
						
						setProgress(col+1);
						Thread.sleep(100);
					}
				} catch(Exception ex) {}
				return cmpt;
			}
			
			@Override
			protected void process(List<String> txt) {
				try{							
					pgDlg.setText(txt.get(txt.size()-1));
				} catch(Exception ex) {

				}
			}
			
			@Override			
			public void done(){
				if(SwingUtilities.isEventDispatchThread()) {											
					if(pgDlg!=null) pgDlg.setVisible(false);
					CountDrops.setDefaultCursor(ad);
					CountDrops.setEnableComponents(ad,true);
				}
			}   

		};
		sw.addPropertyChangeListener(new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent event) {
				if("progress".equals(event.getPropertyName())){
					if(SwingUtilities.isEventDispatchThread())								
						pgDlg.setProgress();
				}            
			}         
		});
		sw.execute();
		
	}

	@Override
	public void autoDetectColumn(ViewWellEvent evt) {
		final AutoDetect ad = evt.getAutoDetect();
		final int slice = evt.getSlice();
		final Well w1 = evt.getWell();
		
		final ProgressDialog pgDlg = new  ProgressDialog(ad,"Loading experiment",selectedPlate.getNROWS());
		pgDlg.setVisible(true);
		CountDrops.setEnableComponents(ad,false);
		CountDrops.setWaitCursor(ad);
		
		SwingWorker<Integer, String> sw = new SwingWorker<Integer, String>(){
			@Override
			protected Integer doInBackground() throws Exception {
				int cmpt = 0;
				int col = w1.getColInPlate();
				for(int row=0;row<selectedPlate.getNROWS();row++) {
					Well w2 = selectedPlate.getWell(row,col);
					publish("Looking for CFUs in well "+w2.getName());
					if(w1==w2) {
						ad.apply(w1.getImagePlus(getImagePlus()),w1,slice);											
						cmpt++;
					} else { 
						if(!w2.isLocked()) {
							ad.apply(w2.getImagePlus(getImagePlus()),w2,slice);
							cmpt++;
						}
					}
					setProgress(row+1);
					Thread.sleep(100);
				}
				return cmpt;
			}
			@Override
			protected void process(List<String> txt) {
				try{							
					pgDlg.setText(txt.get(txt.size()-1));
				} catch(Exception ex) {

				}
			}
			
			@Override			
			public void done(){
				if(SwingUtilities.isEventDispatchThread()) {											
					if(pgDlg!=null) pgDlg.setVisible(false);
					CountDrops.setDefaultCursor(ad);
					CountDrops.setEnableComponents(ad,true);
					
				}
			}   

		};
		sw.addPropertyChangeListener(new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent event) {
				if("progress".equals(event.getPropertyName())){
					if(SwingUtilities.isEventDispatchThread())								
						pgDlg.setProgress();
				}            
			}         
		});
		sw.execute();
	}

	@Override
	public void autoDetectPlate(ViewWellEvent evt) {
		final AutoDetect ad = evt.getAutoDetect();
		final int slice = evt.getSlice();
		final Well w1 = evt.getWell();
				
		final ProgressDialog pgDlg = new  ProgressDialog(ad,"Loading experiment",selectedPlate.getNCOLS()*selectedPlate.getNROWS());
		pgDlg.setVisible(true);
		CountDrops.setEnableComponents(ad,false);
		CountDrops.setWaitCursor(ad);
		
		SwingWorker<Integer, String> sw = new SwingWorker<Integer, String>(){
			@Override
			protected Integer doInBackground() throws Exception {
				int i = 0;
				int cmpt = 0;
				for(int row=0;row<selectedPlate.getNROWS();row++) {
					for(int col=0;col<selectedPlate.getNCOLS();col++) {
						i++;
						Well w2 = selectedPlate.getWell(row,col);
						publish("Looking for CFUs in well "+w2.getName());
						if(w1==w2 || !w2.isLocked()) {
							ad.apply(w2.getImagePlus(getImagePlus()),w2,slice);
						}
						setProgress(i);
						Thread.sleep(100);
					}
				}
				return cmpt;
			}
			
			@Override
			protected void process(List<String> txt) {
				try{							
					pgDlg.setText(txt.get(txt.size()-1));
				} catch(Exception ex) {

				}
			}

			@Override			
			public void done(){
				if(SwingUtilities.isEventDispatchThread()) {											
					if(pgDlg!=null) pgDlg.setVisible(false);
					CountDrops.setDefaultCursor(ad);
					CountDrops.setEnableComponents(ad,true);
				}
			}   

	};
	sw.addPropertyChangeListener(new PropertyChangeListener(){
		public void propertyChange(PropertyChangeEvent event) {
			if("progress".equals(event.getPropertyName())){
				if(SwingUtilities.isEventDispatchThread())								
					pgDlg.setProgress();
			}            
		}         
	});
	sw.execute();
	}

	public void zoom(double scale) {
		if(imp == null) return;
		if(canvas == null) canvas = imp.getCanvas();

		double newMag = canvas.getMagnification()*scale;
		if(newMag<0 || newMag>32) return;

		int w = (int)Math.round(imp.getWidth()*newMag);
		int h = (int)Math.round(imp.getHeight()*newMag);

		int x,y;
		if(selectedPlate!=null) {
			x = selectedPlate.getBoxX(0);
			y = selectedPlate.getBoxY(0);
		} else {
			x = w/2;
			y = h/2;
		}
		
		Dimension newSize = new Dimension(w,h);	 			
		canvas.setMagnification(newMag);
		canvas.setSize(newSize);
		canvas.setLocation(-canvas.screenX(x)*3/4,-canvas.screenY(y)*3/4);
		canvas.repaint();
		drawPlate(selectedPlate);
		
	}

	public void zoomIn() {
		zoom(1.2);
	}

	public void zoomOut() {
		zoom(0.8333333);
	}

}
