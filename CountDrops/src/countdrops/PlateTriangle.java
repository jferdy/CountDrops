package countdrops;

import java.awt.Color;
import java.awt.Font;

import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.TextRoi;

public class PlateTriangle {
	//PlateSettings settings;
	int NROWS = -1;
	int NCOLS = -1;
	//ImagePlus     imp;	
	
	private int   width = -1;
	private int   height = -1;
	private int[] x = new int[3];
	private int[] y = new int[3];
	
	private double thresholdDistance = -1;
	
	private PolygonRoi roi = null;
	private int selectedPoint = -1; //the point the user wants to move
	
	public PlateTriangle(PlateSettings s,ImagePlus imp) {
		NROWS = s.getNROWS();
		NCOLS = s.getNCOLS();
		
		width = imp.getWidth();		
		height = imp.getHeight();
	
		x[0] = width/4;
		y[0] = height/4;
		x[1] = x[0];
		y[1] = y[0]+height/2;
		x[2] = x[0]+width/2;
		y[2] = y[0]+height/2;
		
		roi = new PolygonRoi(x,y,3,PolygonRoi.POLYGON); //type of Roi??		
		
		thresholdDistance = imp.getWidth()/20;
	}

	public int getSelectedPoint() {
		return selectedPoint; 
	}
	
	public void setSelectedPoint(int i) {
		//selects the point the user wants to move
		if(i<0 || i>2) selectedPoint = -1;
		selectedPoint = i;
	}
	
	public boolean setSelectedPoint(int xClick,int yClick) {
		//selects the point the user wants to move from mouse click coordinates		
		for(int i=0;i<3;i++) {
			Double d = Math.sqrt(Math.pow((float)(x[i]-xClick),2.0)+Math.pow((float)(y[i]-yClick),2.0));
			if(d<=thresholdDistance) {
				selectedPoint = i;
				return true;
			}
		}
		selectedPoint = -1;
		return false;		
	}

	public boolean moveSelectedPoint(int xClick,int yClick) {
		if(selectedPoint<0 || selectedPoint>2) return false;
		x[selectedPoint] = xClick;
		y[selectedPoint] = yClick;
		roi = new PolygonRoi(x,y,3,PolygonRoi.POLYGON); //TODO wouldn't it be possible to update coordinates, instead of creating a new roi??		
		return true;
	}
	
	public void draw(Overlay ov) {			
		//draws the triangle
		roi.setStrokeColor(Color.WHITE);		
		ov.add(roi);		
		
		//display wells name
		for(int i=0;i<3;i++) {
			String s = "A1";			
			switch(i) {
			case 1:
				// H1 in the case of standard 96 well plates
				s = PlateSettings.getRowLetterFromInt(NROWS-1)+"1";
				break;
			case 2:
				// H12 in the case of standard 96 well plates
				s = PlateSettings.getRowLetterFromInt(NROWS-1)+NCOLS;
				break;
			}
			
			TextRoi.setFont("Arial",width/20,Font.PLAIN);
			TextRoi txt = new TextRoi(0,0,s);	
			txt.setStrokeColor(Color.WHITE);
			int txtwidth = s.length()*TextRoi.getSize()*3/4;
			
			if(x[i]+thresholdDistance/2+txtwidth>width) {
				txt.setJustification(TextRoi.RIGHT);
				txt.setLocation(x[i]-thresholdDistance/2,y[i]-thresholdDistance/2);				
			} else {
				txt.setJustification(TextRoi.LEFT);
				txt.setLocation(x[i]+thresholdDistance/2,y[i]-thresholdDistance/2);				
			}			
			ov.add(txt);
		}
		int sw = 2; //stroke width??
		if(selectedPoint>=0 && selectedPoint<3) {
			//draws the circle if a corner has been clicked			
			OvalRoi circ = new OvalRoi(x[selectedPoint]-thresholdDistance/2, y[selectedPoint]-thresholdDistance/2,thresholdDistance,thresholdDistance);
			circ.setStrokeColor(Color.WHITE);
			ov.add(circ);			
			circ = new OvalRoi(x[selectedPoint]-thresholdDistance/2+sw, y[selectedPoint]-thresholdDistance/2+sw,thresholdDistance-2*sw,thresholdDistance-2*sw);
			circ.setStrokeColor(Color.GRAY);
			ov.add(circ);
		} else {
			//draw wells otherwise (code is borrowed from Plate.setWell)
			if(NROWS>1 && NCOLS>1) {
				double d1 = Math.sqrt(Math.pow(x[1]-x[2],2)+Math.pow(y[1]-y[2],2))/((double)NCOLS-1.0);
				double d2 = Math.sqrt(Math.pow(x[0]-x[1],2)+Math.pow(y[0]-y[1],2))/((double)NROWS-1.0);		
				double wd = (d1+d2)/2.0;
						
				//computes position of the fourth corner (borrowed from PlateSettings.setBox 
				int centerX = (x[0]+x[2])/2;
				int centerY = (y[0]+y[2])/2;
				int x3 = centerX+(centerX-x[1]);
				int y3 = centerY+(centerY-y[1]);

				
				for(int j=0;j<getNROWS();j++) {
					for(int i=0;i<getNCOLS();i++) {
						//double x = getBoxX(0) + (getBoxX(3)-getBoxX(0))*i/((double)getNCOLS()-1.0) + (getBoxX(1)-getBoxX(0))*j/((double) getNROWS()-1.0);
						//double y = getBoxY(0) + (getBoxY(3)-getBoxY(0))*i/((double)getNCOLS()-1.0) + (getBoxY(1)-getBoxY(0))*j/((double) getNROWS()-1.0);				

						double wx = x[0] + (x3-x[0])*i/((double)getNCOLS()-1.0) + (x[1]-x[0])*j/((double)NROWS-1.0);
						double wy = y[0] + (y3-y[0])*i/((double)getNCOLS()-1.0) + (y[1]-y[0])*j/((double)NROWS-1.0);
						OvalRoi w = new OvalRoi(wx-wd/2.0,wy-wd/2.0,wd,wd);
						w.setStrokeColor(Color.WHITE);
						ov.add(w);
						w = new OvalRoi(wx-wd/2.0+sw,wy-wd/2.0+sw,wd-2*sw,wd-2*sw);
						w.setStrokeColor(Color.GRAY);
						ov.add(w);
					}
				}
			}
		}		
	}
	
	public int[] getX() {
		return x;
	}
	
	public int[] getY() {
		return y;
	}

	public int getNROWS() {
		return NROWS;
	}

	public void setNROWS(int nROWS) {
		NROWS = nROWS;
	}

	public int getNCOLS() {
		return NCOLS;
	}

	public void setNCOLS(int nCOLS) {
		NCOLS = nCOLS;
	}
}
