package countdrops;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.util.EventObject;

public class ViewWellEvent extends EventObject {
	private static final long serialVersionUID = 1L;
	Well well;
    String action;
    AutoDetect autoDetect;
    
	private int locationX;
    private int locationY;
    private Dimension windowSize; 
    private GraphicsDevice screen = null;
    
    private int     slice = -1;
	private boolean closeWhenMoving = false;
    private boolean doWand = false;      //CheckBox create with DoWand
    private boolean showWellContour = false;      //CheckBox create with DoWand
    private boolean changeType = false;  //CheckBox change type on click
    private int     selectedType = 0;    //selected CFU type 
    private double  canvasMagnification = 1.0; //magnification
    
	public ViewWellEvent(Well w,Point loc) {
    	super(w);
    	well = w;
    	action   = "?";
    	locationX = (int) loc.getX();
    	locationY = (int) loc.getY();
    }
    
    public void setAction(String s) {action = s;}    
    public String getAction() {return action;}
    public void setLocation(Point loc) {
    	locationX= (int) loc.getX();
    	locationY= (int) loc.getY();
    	}
    public Point getLocation() {return new Point(locationX,locationY);}
    public int getX() {return locationX;}
    public int getY() {return locationY;}
    public Well getWell() {return well;}
    
    public boolean isDoWand() {
		return doWand;
	}
	public void setDoWand(boolean doWand) {
		this.doWand = doWand;
	}

    public boolean isShowWellContour() {
		return showWellContour;
	}
	public void setShowWellContour(boolean b) {
		this.showWellContour = b;
	}

	public boolean isCloseWhenMoving() {return closeWhenMoving;}
	public void setCloseWhenMoving(boolean closeWhenMoving) {
		this.closeWhenMoving = closeWhenMoving;
	}

	public boolean isChangeType() {
		return changeType;
	}
	public void setChangeType(boolean changeType) {
		this.changeType = changeType;
	}

	public int getSlice() {
		return slice;	
	}
	public void setSlice(int sl) {
		slice =sl;
	}
	
	public int getSelectedType() {
		return selectedType;
	}

	public void setSelectedType(int selectedType) {
		this.selectedType = selectedType;
	}
	public void setAutoDetect(AutoDetect ad) {
		autoDetect = ad;
	}
	
	public AutoDetect getAutoDetect() {
		return autoDetect;
	}

	public double getCanvasMagnification() {
		return canvasMagnification;
	}

	public void setCanvasMagnification(double canvasMagnification) {
		this.canvasMagnification = canvasMagnification;
	}
    public Dimension getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(Dimension size) {
		this.windowSize = size;
	}
	
    public GraphicsDevice getScreen() {
		return screen;
	}

	public void setScreen(GraphicsDevice screen) {
		this.screen = screen;
	}
   
}
