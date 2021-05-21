package countdrops;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.ImageStatistics;

/*
 * This class resembles ImageWindow in that it has both a ImpagePlus and a ImageCanvas
 * It can thus be incorporated into a JFrame, because ImageCanvas is a Component.
 * 
 * 
 * Index of selected CFUs are stored in an ArrayList and changes in that list are notified
 * to listeners. Other changes in CFU (adding, removing or editing) are also notified by the  
 * same way. This allows the main interface to react to changes in ImageWell. The main use of this
 * is to reflect changes made in ImageWell in the cfuTable.
 *  
 */


public class ImageWell {
	//data storage
	private Plate plate = null;
	private Well  well  = null;	
	
	private ArrayList<Integer> selectedCFU = new ArrayList<Integer>(); 
	
	private Boolean isMute = false;
	private ArrayList<ImageWellListener> listeners = new ArrayList<ImageWellListener>();
	
	//display
	private ImagePlus   imp = null;
	private Overlay     overlay = null;
	private ImageCanvas canvas  = null;
	
	private Boolean showAllCFU = true;
	private Boolean isClickable = true; 
	//private Boolean changeTypeWhenClicked = false;
		
	//variable used to select by mouse dragging
	private Boolean mouseDragged = false; 
	private Point startDrag;
	private Point endDrag;
	
	private int cfuRadius = 10;
	
	//popup menu to fire when a CFU is right clicked
	//maybe all action that involve shortcuts with [CTRL] should be added here??
	private PopupMenu     cfuPopup;
	
	//doWand options to create new CFUs
	private Boolean createCFUwithMagicWand = false;
    private double  doWandTolerance = 40.0;
    private double  doWandMaxArea = 50.0;
    private String  doWandMode = "8-connected";    
    private boolean showWellContour = false; 
    private int     currentCFUType = -1;
    
	// key pressed
	// private KeyStrokeAction keyStrokeAction;
	private Boolean CTRLpressed = false;
	private Boolean SHIFTpressed = false;
	private Boolean ALTpressed = false;

	Cursor changeTypeCursor,splitCursor;
	
	private void setCursor(int index) {		
		if(index>-1) {					
			if(CTRLpressed && SHIFTpressed && !ALTpressed) {
				//change type
		        canvas.setCursor(changeTypeCursor);
				return;
			}
			if(CTRLpressed && ALTpressed) {
				//split
				canvas.setCursor(splitCursor); // cursor is on a CFU and CTRL+ALT are pressed
				return;
			}
			canvas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // cursor is on a CFU
			return;
		} else {
			canvas.setCursor(Cursor.getDefaultCursor()); //cursor is not on a CFU
		}
	}
	
	//listeners
	private MouseListener canvasMouseListener = new MouseListener() {
		public void mouseEntered (MouseEvent e) {}
		public void mouseExited (MouseEvent e) {}
        public void mousePressed(MouseEvent e) {        	
        	startDrag = e.getPoint();
        }
        
		public void mouseReleased(MouseEvent evt) {
			if(!isClickable) return;
			
			if(mouseDragged) {
				//mouse has been dragged: select all CFUs inside rectangle		    	
				selectCFU(getDraggedRoi(evt));
				showAllCFU = false;
				mouseDragged = false;
					
				overlay.clear();
				drawCFU();					
				return;				
			} 

			//nothing happens if click falls outside image
			if(!isInsideImage(evt)) return; 
			
			//gets CFU index from click coordinates
			int index =	whichCFUIsPointed(evt);
			
			if(index>-1) {
				//a CFU has been clicked on
				//Right click triggers popup menu
				if(SwingUtilities.isRightMouseButton(evt)) {	
					deselectAllCFU();
					selectCFU(index);	    			
					cfuPopup.show(getImageCanvas(), evt.getX(),evt.getY());							
					return;
				}

				//Left click does other things

				//the type is changed to current type
				if(CTRLpressed && SHIFTpressed && !ALTpressed) {
					CFU cfu = well.getCFU(index);
					if(!cfu.isSaved()) return; //cfu cannot be edited if saving is still in process

					if(currentCFUType>-1) {
						int i = currentCFUType;
						cfu.setCFUType(well.getCFUType(i));
					} else {
						cfu.unsetCFUType();
					}
					for (ImageWellListener hl : listeners) {
						hl.CFUedited();	    			
					}
					
					drawCFU();
					return;
				}

				//clicked CFU is split
				if(CTRLpressed && !SHIFTpressed && ALTpressed) {
					CFU cfu = well.getCFU(index);
					if(!cfu.isSaved()) return; //cfu cannot be edited if saving is still in process
					splitCFU(cfu); 
					return;
				}

				// select / unselect this specific CFU
				if(CTRLpressed && !SHIFTpressed && !ALTpressed) {
					if(!selectedCFU.contains(index)) {
						//the clicked CFU is not selected: it is added to the list
						selectCFU(index);
					} else {
						//the clicked CFU is already selected: it is now unselected
						deselectCFU(index);    				
					}
					showAllCFU = false;
					drawCFU();
					return;
				}

				//default : the clicked CFU is selected in place of all previously selected CFUs	 	    		
				deselectAllCFU();
				selectCFU(index);
				showAllCFU = false;
				drawCFU();
				return;
			} 


			//the click was not in a CFU: a new CFU created
			//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			CFU[] cfu = createCFUfromClick(startDrag);			
			if(cfu==null) return;

			canvas.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			for(int i=0;i<cfu.length;i++) {
				cfu[i].write();
				well.addCFU(cfu[i]);
			}
			canvas.setCursor(Cursor.getDefaultCursor());

			//the newly created CFU are selected (so that SUPPR deletes newly created CFUs)
			selectedCFU.clear();
			for(int i=0;i<cfu.length;i++) {
				selectedCFU.add(well.getNbCFU()-1-i);	 
			}
			
			if(!showAllCFU) {
				//the whole display is updated
				showAllCFU = true;                   //all CFU must be displayed so that user can easily follow what he is doing				
				drawCFU();	    		
			} else {
				//the display does not need to be updated; the newly created CFU are drawn.
				drawNewCFU(cfu.length);
			}
			
			if(!isMute) {
				//sends event to listeners
				for (ImageWellListener hl : listeners) {
					hl.CFUadded();					
					hl.SelectionHasChanged();	    			
				}
			}  		
		}
		
		@Override
		public void mouseClicked(MouseEvent arg0) {}
	   
	};
	
	private MouseMotionListener canvasMouseMotionListener = new MouseMotionListener() {
		public void mouseDragged(MouseEvent e) {
			if(!isClickable) return;
			if(startDrag==null) return;
			
			Roi roi = getDraggedRoi(e);
			//distance between the two points (length of the diagonal)
			double dist = Math.sqrt(Math.pow(roi.getLength(),2.0)+2.0*roi.getStatistics().area);
			if(!mouseDragged && dist>40) {
				//mouse is dragged if the two points are distant enough
				mouseDragged = true;
			}
			
		    if(mouseDragged) { 		    	
		    	//mouseDragged = true;		    	
		    	selectCFU(roi);
		    	showAllCFU = false;
		    	drawCFU();	
		    	roi.setStrokeColor(Color.white);
		    	overlay.add(roi);
		    	imp.updateAndDraw();
		    }
		}	    
		
		public void mouseMoved(MouseEvent e) {
	    	if(!isClickable) return;  //nothing happens if isClickable set to false
	    	if(!isInsideImage(e)) return; //nothing happens if click falls outside image
	    	//gets CFU index from click coordinates
	    	int index =	whichCFUIsPointed(e);
	    	setCursor(index);
		}
	};
	
	//action listener hooked on popup menu
	private ActionListener cfuActionListener = new ActionListener() {  
		public void actionPerformed(ActionEvent e) {
			if(selectedCFU.size()<=0) return; //actions here operate only on selected CFU
			
			String action = e.getActionCommand();
			if (action == "DELETE") {
				deleteSelectedCFU();				
			}
			if (action == "SPLIT") {
				splitCFU();
			}

		};
	};
	
	public ImageWell(ImagePlus fullImage,Plate xpl,Well xw) {				
		plate = xpl;
		well = xw;
		
		//retrieve an image plus corresponding to the well
		imp = well.getImagePlus(fullImage);
		overlay = new Overlay();
		imp.setTitle(plate.getImage()+" -- "+plate.getName()+" -- "+well.getName());			
		//imp.setOverlay(overlay);
		
		canvas = new ImageCanvas(imp);		
		canvas.setOverlay(overlay);
		
		//cursors
		changeTypeCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
		splitCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
		
		//create and populate popup menu	    
	    cfuPopup=new PopupMenu();	        	    
	    MenuItem cfuPopupItem_del =new MenuItem("Delete CFU");
	    cfuPopupItem_del.setActionCommand("DELETE");
	    cfuPopupItem_del.addActionListener(cfuActionListener);
	    cfuPopup.add(cfuPopupItem_del); 

	    MenuItem cfuPopupItem_split =new MenuItem("Split CFU");
	    cfuPopupItem_split.setActionCommand("SPLIT");
	    cfuPopupItem_split.addActionListener(cfuActionListener);
	    cfuPopup.add(cfuPopupItem_split);

	    canvas.add(cfuPopup);
	    
		//remove default listeners
		for(MouseListener o1 : canvas.getMouseListeners()) canvas.removeMouseListener(o1);		
		for(MouseMotionListener o2 : canvas.getMouseMotionListeners()) canvas.removeMouseMotionListener(o2);	
		for(KeyListener o3 : canvas.getKeyListeners()) canvas.removeKeyListener(o3);
		
		//add custom listeners
		canvas.addMouseListener(canvasMouseListener);
		canvas.addMouseMotionListener(canvasMouseMotionListener);
		
		setDefaultCFURadius();		
	}
	
	//******** getting and setting ******/
	public Overlay getOverlay() { return overlay;}
	public ImagePlus getImagePlus() {return imp;}
	public ImageCanvas getImageCanvas() {return canvas;}
	public ArrayList<Integer> getSelectedCFU() {return selectedCFU;}	
	public Well getWell() { return well;}
	public Plate getPlate() {return plate;}
	
	public void setDoWand(Boolean b) {createCFUwithMagicWand = b;}
	public void setShowWellContour(Boolean b) {showWellContour = b;}
	public void setMouseDragged (Boolean b) {mouseDragged = b;}
	public void setIsMute(Boolean b) {isMute = b;}
	public void setCurrentCFUType(int i) {
		if(i<0 || i>well.getNCFUTYPES()) {
			currentCFUType = -1;
		} else {
			currentCFUType = i;
		}
	}	
	public void setCFURadius(int r) {cfuRadius = r;}
	public void setDoWandTolerance(int t) {doWandTolerance = t;}
	public void setDoWandMaxArea(int t) {doWandMaxArea = t;}
	public void setDefaultCFURadius() {cfuRadius = imp.getWidth()/25;;}
	public Boolean isMute() { return isMute;}
	public int getNbCFU() {return well.getNbCFU();}
	public int getNbSelectedCFU() {return selectedCFU.size();}
	public int getCurrentCFUType() { return currentCFUType; }
	public int getCFURadius() {return cfuRadius;}
	public double getDoWandTolerance() {return doWandTolerance;}
	public double getDoWandMaxArea() {return doWandMaxArea;}
	public void addListener(ImageWellListener toAdd) {
        listeners.add(toAdd);
    }
	public ArrayList<ImageWellListener> getListeners() {
		return listeners;
	}

		
	public boolean isShowAllCFU() { return showAllCFU; }
	public boolean isShowWellContour() {return showWellContour;}
	
	public void setIsClickable(boolean b) { isClickable = b; }
	public void setShowAllCFU(boolean b) { showAllCFU = b; }
	public void setCTRLpressed(boolean b) { 
		CTRLpressed = b;
		int index = whichCFUIsPointed();
		setCursor(index);
		}
	public void setALTpressed(boolean b) { 
		ALTpressed = b; 
		int index = whichCFUIsPointed();
		setCursor(index);
	}
	public void setSHIFTpressed(boolean b) { 
		SHIFTpressed = b; 
		int index = whichCFUIsPointed();
		setCursor(index);
	}
	
	//******** function to display image and CFUs **************/ 
	public void show() {
		//display the image using the canvas defined in constructor		
		ImageWindow iw = new ImageWindow(imp,canvas);		
		iw.setVisible(true);		
	}
	
	 public void setSlice(int sl) {    		    	
		 if(sl<1 || sl>imp.getNSlices()) return;
		 canvas.getImage().setSlice(sl);    	
		 canvas.getImage().updateImage();
		 canvas.repaint();
	 }
	 
    public void nextSlice() {    	
    	int sl = canvas.getImage().getCurrentSlice();
    	if(sl<imp.getNSlices()) {
    		sl++;
    	} else {
    		sl=1;	    
    	}
    	canvas.getImage().setSlice(sl);    	
    	canvas.getImage().updateImage();
    	canvas.repaint();
    }

    public void prevSlice() {		
    	int sl = canvas.getImage().getCurrentSlice();
    	if(sl>1) {
    		sl--;
    	} else {
    		sl=imp.getNSlices();
    	}    	
    	canvas.getImage().setSlice(sl);
    	canvas.getImage().updateImage();
    	canvas.repaint();
    }

    public void clearOverlay() {
    	overlay.clear();
    	canvas.repaint();
    	imp.updateAndRepaintWindow();    	
    }
    
    public void drawWellContour() {
    	overlay.clear();
		if(showWellContour) {    
			well.draw(canvas);
    	}		
		canvas.repaint();
		imp.updateAndRepaintWindow();
    }
    
    public void drawNewCFU(int nb) {						
		//only last CFU (i.e newly created) is added to display
    	for(int i=0;i<nb;i++) {
    		CFU cfu = well.getCFU(getNbCFU()-1-i);
    		if(cfu!=null) cfu.draw(canvas);
    	}
		canvas.repaint();
		imp.updateAndRepaintWindow();		
	}
    
	
	public void drawCFU() {
		//all CFUs are displayed
		drawWellContour();
		if(showAllCFU) {
			//display all CFUs
	    	for(int i=0;i<well.getNbCFU();i++) {
	    		CFU cfu = well.getCFU(i);
	    		cfu.draw(canvas);    		
	    		}
		} else {
			//display only selected CFUs
			for(int i=0;i<selectedCFU.size();i++) {
				CFU cfu = well.getCFU(selectedCFU.get(i)); 
				cfu.draw(canvas);
			}
		}
    	canvas.repaint();
    	imp.updateAndRepaintWindow();    	
	}

	public void zoom(double scale) {
		if(imp == null) return;
		if(canvas == null) canvas = imp.getCanvas();

		double newMag = canvas.getMagnification()*scale;
		if(newMag<0 || newMag>32) return;
		
		int w = (int)Math.round(imp.getWidth()*newMag);
		int h = (int)Math.round(imp.getHeight()*newMag);
		Dimension newSize = new Dimension(w,h);
		
		canvas.setMagnification(newMag);
		canvas.setSize(newSize);			
		drawCFU();
	}

	public void zoomIn() {
		zoom(1.2);
	}

	public void zoomOut() {
		zoom(0.8333333);
	}

	//*** functions to select CFUs ************//	
	public Roi getDraggedRoi(MouseEvent e) {
		//endDrag = canvas.getCursorLoc();
		endDrag = e.getPoint();
		int x0 = (int) startDrag.getX();
		int y0 = (int) startDrag.getY();
		int x1 = (int) endDrag.getX();
		int y1 = (int) endDrag.getY();

		int x,y,w,h;
		w = x1-x0;
		h = y1-y0;

		if(w>0) {
			x = x0;	    	
		} else {
			x = x1;	    	
			w = -w;
		}
		if(h>0) {
			y = y0;	    	
		} else {
			y = y1;
			h = -h;
		}		
		
		Roi roi = new Roi(x,y,w,h);
		return(roi);
	}

	public Boolean isSelected(int index) {
		return selectedCFU.contains(index);
	}
	
	public int whichCFUIsPointed() {
		Point p = canvas.getMousePosition();
		if(p==null) return -1;
		return whichCFUIsPointed(p.x,p.y);	
	}
	
	public int whichCFUIsPointed(MouseEvent e) {
		if(e==null) return -1;
		
		int x = e.getX();
		int y = e.getY();
		return whichCFUIsPointed(x,y);
	}
	
	public int whichCFUIsPointed(int x,int y) {		
    	x = canvas.offScreenX(x);
    	y = canvas.offScreenY(y);
    	    	
    	for(int i=0;i<well.getNbCFU();i++) {
    		CFU cfu = well.getCFU(i);    
    		//System.out.print(i+" ");
    		if(cfu.contains(x, y)) {
    			return i;    		
    		}
    	}
    	return -1;
	}
		
	public void selectCFU(int i) {
		if(i<0 || i>= well.getNbCFU()) return;
		if(!selectedCFU.contains(i)) {
			selectedCFU.add(i);
			showAllCFU = false;		
		}
		if(!isMute) {
			//sends event to listeners
			for (ImageWellListener hl : listeners) hl.SelectionHasChanged();
		}
	}

	public void selectCFU(Roi roi) {		
		ArrayList<Integer> newSelectedCFU = new ArrayList<Integer>();  
		for(int i=0;i<well.getNbCFU();i++) {
			CFU cfu = well.getCFU(i);
			if(roi.contains(canvas.screenX(cfu.getX()),canvas.screenY(cfu.getY()))) {
				if(!newSelectedCFU.contains(i)) newSelectedCFU.add(i);
			}
		}
		
		//test whether selection has changed 
		Boolean change = false;
		for(Integer i : newSelectedCFU) {
			if(! selectedCFU.contains(i)) change = true;
		}
		if(newSelectedCFU.size() != selectedCFU.size()) change = true;		
				
		//send notification if selection has changed
		if(change) {
			//update selection
			deselectAllCFU();
			selectedCFU.clear();
			selectedCFU=newSelectedCFU;

			showAllCFU = false;
			if(!isMute) {
				//sends event to listeners
				for (ImageWellListener hl : listeners) hl.SelectionHasChanged();
			}
		}		
	}
	public void selectAllCFU() {
		selectedCFU.clear();
		for(int i=0;i<well.getNbCFU();i++) selectedCFU.add(i);
		showAllCFU = false;
		if(!isMute) {
			//sends event to listeners
			for (ImageWellListener hl : listeners) hl.SelectionHasChanged();
		}
	}
	public void deselectAllCFU() {
		selectedCFU.clear();
		showAllCFU = false;
		if(!isMute) {
			//sends event to listeners
			for (ImageWellListener hl : listeners) hl.SelectionHasChanged();
		}		
	}
	public void deselectCFU(int i) {
		if(i<0 || i>= well.getNbCFU()) return;		
		for(int j=selectedCFU.size()-1;j>=0;j--) {
			if(selectedCFU.get(j)==i) selectedCFU.remove(j);
		}	
		showAllCFU = false;
		if(!isMute) {
			//sends event to listeners
			for (ImageWellListener hl : listeners) hl.SelectionHasChanged();
		}
	}	

	//check that coordinates fall inside image
	public Boolean isInsideImage(MouseEvent e) {		
		return isInsideImage(e.getPoint());
	}
	
	public Boolean isInsideImage(Point pt) {	
    	int x = canvas.offScreenX(pt.x);
    	int y = canvas.offScreenY(pt.y);
		if(x>imp.getWidth()) return false;
		if(y>imp.getHeight()) return false;
		return true;
	}
	
	// function to create new CFUs
    public CFU[] createCFUfromClick(Point pt) {
		if(pt==null) return null;
		if(!isInsideImage(pt)) return null;
		
		int x = pt.x;
		int y = pt.y;		
    	x = canvas.offScreenX(x);
    	y = canvas.offScreenY(y);
    	

    	ShapeRoi p;
    	CFU cfu[];
    	if(createCFUwithMagicWand) {
    		//do Wand at clicked point
    		IJ.doWand(imp,x,y,doWandTolerance,doWandMode);
    		
    		if(imp.getRoi() == null) return(null);
    		ImageStatistics statroi = imp.getRoi().getStatistics();
    		Double imgarea = (double) (imp.getHeight() * imp.getWidth());
    		if(100.0*statroi.area/imgarea > doWandMaxArea) {
    				imp.deleteRoi(); //otherwise roi keeps being displayed
    				return(null); //the roi is too big relative to image !
                    			  //this is probably because user has clicked outside a CFU and the whole
	                 			  //background has been selected.    				
    		}
    			
    		p = new ShapeRoi(imp.getRoi().getPolygon());
    		imp.deleteRoi(); //otherwise roi keeps being displayed
    		
    		for(int i=this.getNbCFU()-1;i>=0;i--) {
    			ShapeRoi p2 = new ShapeRoi(well.getCFU(i).getRoi()); 
    			p2.and(p); //should return null if no intersection
    			if(p2!=null && p2.getBounds().getHeight()>0 && p2.getBounds().getWidth()>0) {
    				ImageStatistics stat = p2.getStatistics();
    				double area  = stat.area;
    				System.out.print(area/well.getCFU(i).getArea()+"\n");
    				if(area/well.getCFU(i).getArea() > 0.95) {    					
    					//half of CFU i is covered by new contour
    					deselectCFU(i);
    					well.deleteCFU(i);
    					if(!isMute) {
    						//sends event to listeners
    						for (ImageWellListener hl : listeners) {
    							hl.CFUremoved();						    			
    						}
    					}  		
    				}
    			}
    		}    		    		
    		CFU newcfu = new CFU(well,p,well.getCFUType(currentCFUType));
    		cfu = newcfu.splitWatershed();
    		if(cfu == null) {
    			cfu = new CFU[1];
    			cfu[0]=newcfu;
    		}
    		
    	} else {
    		//select a circle around clicked point
    		int s = cfuRadius;
    		OvalRoi ov = new OvalRoi(x-s/2,y-s/2,s,s);
    		p = new ShapeRoi(ov.getPolygon());
    		cfu = new CFU[1];
    		cfu[0] = new CFU(well,p,well.getCFUType(currentCFUType));
    	}
    	     	    	
    	return cfu;
    }

    public boolean splitCFU() {
		//only the last CFU in selection will be split
		CFU cfu = well.getCFU(selectedCFU.get(selectedCFU.size()-1)) ;
		return splitCFU(cfu);
    }
    
    public boolean splitCFU(CFU cfu) {
		if(cfu==null) return false;
		if(!cfu.isSaved()) return false;
		
		int nb = well.splitCFU(cfu);				
		boolean oldShowAllCFU = showAllCFU;
		
		this.deselectAllCFU();
		for(int i=0;i<nb;i++) selectedCFU.add(well.getNbCFU()-1-i);	 //the newly created CFUs are selected
		showAllCFU = oldShowAllCFU;
		drawCFU();		
		if(!isMute) {
			//sends event to listeners
			for (ImageWellListener hl : listeners) {
				hl.CFUadded();
				hl.SelectionHasChanged();	    			
			}
		}
		return true;
    }
    
    //functions to delete CFU
    public void deleteSelectedCFU() {
    	if(!well.deleteCFU(selectedCFU)) return;    	
    	selectedCFU.clear();    	
    	drawCFU();
    	if(!isMute) {
    		for (ImageWellListener hl : listeners) hl.CFUremoved();
    	}
    }
    
    public void deleteAllCFU() {
    	well.deleteAllCFU();
    	selectedCFU.clear();
    	drawCFU();
    	
    	if(!isMute) {
    		for (ImageWellListener hl : listeners) hl.CFUremoved();
    	}    	    	
    }    
    	
    //functions to edit CFU
    public void changeTypeForSelectedCFU(String key) {
    	Boolean changed = well.changeCFUType(key,selectedCFU);
    	if(changed) {
    		drawCFU();
    		if(!isMute) {
    			for (ImageWellListener hl : listeners) hl.CFUedited();
    		}
    	}
    }
    public void unsetTypeForSelectedCFU() {
    	Boolean changed = well.unsetCFUType(selectedCFU);
    	if(changed) {
    		drawCFU();
    		if(!isMute) {
    			for (ImageWellListener hl : listeners) hl.CFUedited();
    		}
    	}
    }
}


