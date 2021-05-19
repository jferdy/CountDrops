package countdrops;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.util.EventObject;

public class ViewWellEvent extends EventObject {
	private static final long serialVersionUID = 1L;
	Well well = null;
    String action = "";
    AutoDetect autoDetect = null;
    
	private int locationX = 0;
    private int locationY = 0;
    private Dimension windowSize = null; 
    private GraphicsDevice screen = null;
    
    private String  sourceClass = "Object"; 
    private int     slice = -1;
    private int     circleRadius = 12;
    private int     doWandTolerance = 40;
    private int     doWandMaxArea = 50;
	private boolean closeWhenMoving = false;
    private boolean doWand = true;      //CheckBox create with DoWand
    private boolean showWellContour = false;      //CheckBox create with DoWand
    private boolean changeType = false;  //CheckBox change type on click
    private int     selectedType = 0;    //selected CFU type 
    private double  canvasMagnification = 1.0; //magnification
    private boolean Xreversed = false;
    private boolean Yreversed = false;
    
	public ViewWellEvent(ViewWell vw,Point loc) {
    	super(vw);
    	sourceClass="ViewWell";
    	if(vw!=null) well = vw.getWell();
    	action   = "?";
    	locationX = (int) loc.getX();
    	locationY = (int) loc.getY();
    }
    
	public ViewWellEvent(Object o,Point loc) {
		super(o);
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
    public String getSourceClass( ) {
    	return(sourceClass);
    }
    public Point getLocation() {return new Point(locationX,locationY);}
    public int getX() {return locationX;}
    public int getY() {return locationY;}
    public Well getWell() {return well;}
    
    public boolean isXreversed( ) {
    	return(Xreversed);	
    }
    public void setXreversed(boolean b) {
    	Xreversed = b;    	
    }
    
    public boolean isYreversed( ) {
    	return(Yreversed);	
    }
    public void setYreversed(boolean b) {
    	Yreversed = b;    	
    }

    public void setDoWandTolerance(int t) {
    	doWandTolerance = t;
    }
    public void setDoWandMaxArea(int m) {
    	doWandMaxArea = m;
    }
    public void setCircleRadius(int r) {
    	circleRadius = r;
    }
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
	
	public int getCircleRadius() {
		return(circleRadius);	
	}
	
	public int getDoWandTolerance()   {
		return(doWandTolerance);	
	}
	public int getDoWandMaxArea()   {
		return(doWandMaxArea);	
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
