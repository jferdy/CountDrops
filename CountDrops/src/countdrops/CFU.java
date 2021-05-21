package countdrops;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Measurements;
import ij.plugin.filter.EDM;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

public class CFU {
	private String  CFUName;
	private String  wellName;
	private String  plateName;
	private String  imageName;
	private String  path;
	
	private PlateSettings settings;
	
	private int X = -1;
	private int Y = -1;
	//private PolygonRoi roi = null;
	private ShapeRoi roi = null;
	private ShapeRoi scaledRoi = null;
	private String  CFUType = "NA";

	private boolean saved = false;
	
	private double   area;
	private double   perimeter;
	private double   circularity;
	private double[] feret;

	//private final int MovingAverageWidth = 5;
	private double magnification = 1.0;
	
	public CFU(Well w,ShapeRoi r) {
		wellName  = w.getName();
		plateName = w.getPlate();
		imageName = w.getImage();
		path = w.getPath();
		
		settings = w.getSettings();
		
		this.setRoi(r);
		resetStats();
	}

	public CFU(Well w,ShapeRoi r,String type) {
		wellName  = w.getName();
		plateName = w.getPlate();
		imageName = w.getImage();
		path = w.getPath();
		
		settings = w.getSettings();
		
		CFUType = type;
		this.setRoi(r);
		resetStats();
	}

	public CFU(CFU x,ShapeRoi r) {
		settings = x.getSettings();
		
		wellName  = x.getWellName();
		plateName = x.getPlateName();
		imageName = x.getImageName();
		path      = x.getPath();
		
		CFUType   = x.getCFUType();
		
		this.setRoi(r);
		resetStats();
	}

	public CFU(Well w,String configFile) {
		settings = w.getSettings();
		
		ArrayList<String> content = new ArrayList<String>();
		try {
			FileInputStream fis = new FileInputStream(configFile);
			Scanner scanner = new Scanner(fis);	   
			while(scanner.hasNextLine()){
				content.add(scanner.nextLine());
			}    
			scanner.close();
		}
		catch(FileNotFoundException e) {
			System.out.println("Error while reading "+configFile);
		}	
		int sep = configFile.lastIndexOf("/");
		int dot = configFile.lastIndexOf(".");	
		CFUName = configFile.substring(sep+1,dot);

		if(content.size()<4) {
			System.out.println(configFile+" has incorrect content!");
			return;
		}

		CFUType = content.get(0).replaceAll("CFUType;(.*)", "$1");

		wellName  = w.getName();
		plateName = w.getPlate();
		imageName = w.getImage();
		path = w.getPath();

		int nb = content.size()-1;
		if(nb>0) {
			int[] x = new int[nb];
			int[] y = new int[nb];
			for(int i=0;i<nb;i++) {
				String[] f = content.get(i+1).split(";");
				x[i] = Integer.parseInt(f[0]);
				y[i] = Integer.parseInt(f[1]);
			}
			PolygonRoi p = new PolygonRoi(x,y,content.size()-1,4); //type 4?? corresponds to polygon after doWand
			setRoi(new ShapeRoi(p));
		} else {
			setRoi(null);
		}
			
		resetStats();
		saved = true;
	}

	public PlateSettings getSettings() {return settings;}
	public String   getCFUName() {return(CFUName);}
	public String   getWellName() {return(wellName);}
	public String   getPlateName() {return(plateName);}
	public String   getImageName() {return(imageName);}
	public String   getPath() {return(path);}
	public int      getX() {return(X);}
	public int      getY() {return(Y);}
	public ShapeRoi getRoi() {return(roi);}
	public String   getCFUType() {return(CFUType);}
	public String   getCFUColor() {return(settings.getCFUColor(CFUType));}
	public double   getArea() {return(area);}
	public double   getPerimeter() {return(perimeter);}
	public double   getCircularity() {return(circularity);}    
	public double[] getFeret() {return(feret);}
	public double   getFeret(int i) {if(feret==null) return -1; if(i<0 || i>= feret.length) return -1; return(feret[i]);}
	public boolean  isSaved() {return saved;}
	
	public void  setCFUName(String x) {CFUName=x; saved = false;}
	public void  setWellName(String x) {wellName=x; saved = false;}
	public void  setPlateName(String x) {plateName=x; saved = false;}
	public void  setImageName(String x) {imageName=x; saved = false;}
	public void  setPath(String x) {path=x; saved = false;}
	public void  setX(int x) {X=x; saved = false;}
	public void  setY(int x) {Y=x; saved = false;}
	public void  setSaved(boolean b) {saved=b;}
	
	public void  setCFUType(String type) {//,String color) {
		saved = false;
		CFUType=type;		
		this.write();
	}

	public void  unsetCFUType() {
		saved = false;
		CFUType = "NA";		
		this.write();
	}	

	//public void  setRoi(PolygonRoi r) {
	public void  setRoi(ShapeRoi r) {
		saved = false;
		roi=r;		
		scaledRoi=r;
		magnification=1.0;
		resetCFUName();
		resetStats();
	}
	
	public void setImagePlus(ImagePlus imp) {
		if(roi!=null) roi.setImage(imp);		
	}
	
	public void move(int dx,int dy) {
		saved = false;
		this.deleteRoiFile();

		//move centroid
		X-=dx;
		Y-=dy;

		//move contour
		Polygon p = roi.getPolygon();
		for(int i=0;i<p.npoints;i++) {
			p.xpoints[i]-=dx;
			p.ypoints[i]-=dy;
		}
		//updates contour, name etc.
		
		//setRoi(new PolygonRoi(p,4));
		setRoi(new ShapeRoi(p));

		//save roi file
		this.write();

	}

	public void resetCFUName() {
		saved = false;
		double[] centroid = roi.getContourCentroid(); // ImageJ 1.50b or > !!
		X = (int)centroid[0];
		Y = (int)centroid[1];
		CFUName = ""+X+"-"+Y;
	}
	
	public void resetStats() {		
		ImageStatistics stat = roi.getStatistics();
		area  = stat.area;
		perimeter = roi.getLength();
		circularity = 4*Math.PI*area/(perimeter*perimeter);
		feret = roi.getFeretValues();
	}

	public double[] getBrightness(ImagePlus imp) {
		int sl = imp.getCurrentSlice();

		double[] br = new double[imp.getNSlices()];
		for(int i=0;i<imp.getNSlices();i++) {
			imp.setSlice(i+1);

			ImageProcessor ip = imp.getProcessor();
			ImageProcessor mask;
			try {
				mask = roi.getMask();
			} catch (Exception e) {
				mask = null;
			}
			Rectangle r = roi!=null?roi.getBounds():new Rectangle(0,0,ip.getWidth(),ip.getHeight());
			double sum = 0;
			int count = 0;
			for (int y=0; y<r.height; y++) {
				for (int x=0; x<r.width; x++) {
					if (mask==null||mask.getPixel(x,y)!=0) {
						count++;
						sum += ip.getPixelValue(x+r.x, y+r.y);
					}
				}
			}
			br[i] = (double) sum / ((double) count);

			//return to the slice initially selected
			imp.setSlice(sl);

			//System.out.println(""+this.getCFUName()+" "+i+" "+br[i]+"\n");
		}

		return(br);
	}

	public void  forceWrite() {
		saved = false;
		write();
	}
	
	public void  write() {
		//System.out.println(CFUName+" is to save?");
		if(saved) return; //nothing to save!
		if(roi==null) return; //nothing to save!!
		
		try {
			PrintWriter writer = new PrintWriter(path+CFUName+".roi");
			writer.println("CFUType;"+CFUType);
			Polygon p = roi.getPolygon();
			for(int i=0;i<p.npoints;i++) {
				writer.println(p.xpoints[i]+";"+p.ypoints[i]);
			}
			writer.close();
			saved = true;
			//System.out.println(CFUName+" is saved in "+path+CFUName+".roi");
		}
		catch(IOException se){
		}
	}

	public void deleteRoiFile() {
		File f = new File(path+CFUName+".roi");
		if(f.exists()) f.delete();
	}

	public void setStroke(double lw,boolean fill) {
		setStroke(roi,lw,fill);	
	}
	
	public void setStroke(ShapeRoi r,double lw,boolean fill) {	
		Color color = PlateSettings.convertStringToColor(settings.getCFUColor(CFUType));		

		if(fill) {
			Color color2 = new Color(255,255,255,25);
			r.setFillColor(color2);
		} else {
			r.setFillColor(null);
		}
		r.setStrokeColor(color);
		r.setStrokeWidth(lw);
	}

	/*
	public void draw(ImagePlus imp) {
		if(roi==null) return;
		setStroke(1.0,false);
		imp.setRoi(this.roi);
	}

	public void draw(Overlay ov) {
		if(roi==null) return;
		setStroke(1.0,false);		
		ov.add(this.roi);		
	}
*/
	
	public boolean setMagnification(double m) {
		if(Math.abs(m-magnification)>0.0001) { //magnification has changed
			magnification = m;
			return true;
		}
		return false;	
	}
	
	public void draw(ImageCanvas ic) {
		if(roi==null) return;
		if(setMagnification(ic.getMagnification())) {		
			Polygon p = roi.getPolygon();
			if(p.npoints<3) return;

			int[] x = new int[p.npoints];
			int[] y = new int[p.npoints];
			for(int i=0;i<p.npoints;i++) {
				x[i] = (int) (p.xpoints[i]*ic.getMagnification());
				y[i] = (int) (p.ypoints[i]*ic.getMagnification());
			}
			scaledRoi = new ShapeRoi(new Polygon (x,y,x.length));
		} 

		setStroke(scaledRoi,1.0,false);
		ic.getOverlay().add(scaledRoi);		
	}
	
	public void fill(Overlay ov) {
		//le contour n'apparaît plus!
		setStroke(2.0,false);	
		ov.add(this.roi);
	}

	public boolean contains(int x,int y) {
		boolean inside;
		try {			
			inside = roi.contains(x,y);
			//System.out.print("("+x+","+y+") ["+roi.getBounds().getX()+","+roi.getBounds().getMaxX()+","+roi.getBounds().getY()+","+roi.getBounds().getMaxY()+"]"+inside+"\n");
		} catch(Exception e) {
			System.out.print("Cannot test if point is inside CFU "+CFUName+"!\n");
			inside = false;	
		}		
		return(inside);
	}

	public double overlap(CFU cfu) {
		//if bounding boxes are not overlapping returns zero
		//probably much faster than actually computing overlap!
		Rectangle bounds1 = roi.getBounds();
		Rectangle bounds2 = cfu.getRoi().getBounds();
		if((bounds1.getMinX() > bounds2.getMaxX() || bounds2.getMinX() > bounds1.getMaxX()) && (bounds1.getMinY() > bounds2.getMaxY() || bounds2.getMinY() > bounds1.getMaxY())) return 0.0;
		
		//bounding boxes are overlapping: actual overlap must be computed
		ShapeRoi union = new ShapeRoi(roi);		
		union.or(cfu.getRoi());
				
		
		//the union of the two polygons has a surface which is comprised between
		//* the sum of the two surfaces (when the two polygons do not overlap at all)
		//* the largest of the two surfaces (when the one polygon is included in the other)		
		double totSurf = cfu.getArea()+this.getArea(); 
		double maxSurf = cfu.getArea();
		if(this.getArea()>maxSurf) maxSurf = this.getArea();
		double z = union.getStatistics().area;
		z = (totSurf-z)/(totSurf-maxSurf);
		return z;		
	}
	
	public boolean equals(CFU x) {
		if(x==null) return false;

		//test identity of polygons by their basic statistics
		// i.e. centroid position, area and Feret values
		if(X != x.getX()) return false;
		if(Y != x.getY()) return false;
		if(area != x.getArea()) return false;
		for(int i=0;i<feret.length;i++) if(feret[i]!=x.getFeret(i)) return false;

		return true;
	}

	private double[] movingAverage(int[] x,int window) {
		int nb = x.length;
				
		
		if(nb<4*window) window = 1; //no smoothing if CFU is too small
		
		int delta = (window- 1) / 2;     //number of points on each side of focal point		
		if(delta<0) delta=0;
		
		//window = 2*delta+1;			
		window = 4*delta+1;		
		double pds[] = new double[window];
		if(delta>0) {
			double pdstotal = 0.0;
			int j=0;
			double s = delta/2.0;
			for(int i=-2*delta;i<=2*delta;i++) {
				pds[j] = Math.exp(-Math.pow(i/s,2.0))/(s*Math.sqrt(2.0*Math.PI));
				pdstotal+=pds[j];
				j++;
			}
			for(j=0;j<pds.length;j++) pds[j]/=pdstotal;
		} else {
			pds[0]=1.0;
		}
		
		double[] xs = new double[nb];
		for(int i=0;i<nb;i++) {
			xs[i] = 0.0;
			for(int j=-2*delta;j<=2*delta;j++) {
				int k = i+j;
				if(k<0) k+=nb;
				if(k>=nb) k-=nb;
				
				xs[i] += ((double) x[k])*pds[j+2*delta];
			}
			//xs[i] /= (double) window;
		}
		
		return(xs);
	}
	
	private double[] angles(double[] x,double[] y,int pos,int delta) {
		int nb = x.length;
		double[] res = new double[2];
		res[0] = 0;
		res[1] = 0;
		
		for(int i=1; i<=delta;i++) {			
			int j = pos+i;
			int k = pos-i;
			if(j>=nb) j-=nb;
			if(k<0) k+=nb;
			
			double x1 = x[j]-x[pos];
			double x2 = x[k]-x[pos];
			double y1 = y[j]-y[pos];
			double y2 = y[k]-y[pos];

			double l1 = Math.sqrt(Math.pow(x1,2.0)+Math.pow(y1,2.0));
			double theta1 = Math.acos(x1/l1);
			if(y1<0) theta1 = 2*Math.PI-theta1;

			double l2 = Math.sqrt(Math.pow(x2,2.0)+Math.pow(y2,2.0));
			double theta2 = Math.acos(x2/l2);
			if(y2<0) theta2 = 2*Math.PI-theta2;

			//first angle is the angle between the two sides of the triangle (proxy of curvature)
			//second angle gives the direction of the bisector
			res[0] += Math.abs(theta2-theta1);
			res[1] += (theta1+theta2)/2.0;
		}
		res[0]/=delta;
		res[1]/=delta;
	    return(res);
	}

	public CFU[] splitWatershed( ) {
		//bounding box
		Rectangle bb = this.getRoi().getBounds();
		
		//create mask (background is black, object is white)
		ImageProcessor ip = this.getRoi().getMask();
		
		//watershed segmentation
		EDM edm = new EDM();
		edm.toWatershed(ip);
		
		//set threshold to select white areas
		ip.setThreshold(125,255,ImageProcessor.NO_LUT_UPDATE);
		
		//particle analysis
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE+ParticleAnalyzer.ADD_TO_MANAGER+ParticleAnalyzer.INCLUDE_HOLES,//
				Measurements.AREA,
				null, //new ResultsTable(),
				1.0,
				ip.getWidth()*ip.getHeight(), //max size
				0.0,
				2.0);
		// Constructs a ParticleAnalyzer.
		// 	    Parameters:
		// 	    options - a flag word created by Oring SHOW_RESULTS, EXCLUDE_EDGE_PARTICLES, etc.
		// 	    measurements - a flag word created by ORing constants defined in the Measurements interface
		// 	    rt - a ResultsTable where the measurements will be stored
		// 	    minSize - the smallest particle size in pixels
		// 	    maxSize - the largest particle size in pixels
		// 	    minCirc - minimum circularity
		// 	    maxCirc - maximum circularity


		RoiManager roima = new RoiManager(true);
		pa.setRoiManager(roima);
		
		boolean success = pa.analyze(new ImagePlus("mask",ip),ip);
		if(!success) return null;
		
		Roi[] vroi = roima.getRoisAsArray();
		roima.reset();
		roima.close();

		if(vroi.length<=1) return(null); //if only one ROI has been detected, the CFU has not been split!
		
		CFU[] newcfu = new CFU[vroi.length];
		for(int i=0;i<vroi.length;i++) {
			//create new CFU
			Polygon p = vroi[i].getPolygon();
			p.translate(bb.x,bb.y);
			newcfu[i] = new CFU(this,new ShapeRoi(p));	    
		}		
		return newcfu;
	}
	
	public CFU[] splitInTwoHalves( ) {
		Rectangle bb = roi.getBounds();
		int h = bb.height;
		int w = bb.width;
		int dx,dy;
		if(h>w) {
			h/=2;
			dx = 0;
			dy = h;
		} else {
			w/=2;
			dx = w;
			dy = 0;
		}
		ShapeRoi r1 = new ShapeRoi(new Roi(bb.x,bb.y,w,h));
		ShapeRoi r2 = new ShapeRoi(new Roi(bb.x+dx,bb.y+dy,w,h));
		
		CFU[] newcfu = new CFU[2];
		r1 = r1.and(roi);
		r2 = r2.and(roi);
		
		newcfu[0] = new CFU(this,r1);
		newcfu[1] = new CFU(this,r2);
		
		return newcfu;
	}

	public CFU[] split() {
		CFU[] newcfu = this.splitWatershed();
		if(newcfu == null) newcfu = this.splitInTwoHalves(); //watershed hasn't split anything: cut the CFU in two halves
		return(newcfu);
	}
	
}
