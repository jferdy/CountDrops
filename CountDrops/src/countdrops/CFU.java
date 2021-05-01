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
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Measurements;
import ij.plugin.filter.EDM;
import ij.plugin.filter.MaximumFinder;
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

	private final int MovingAverageWidth = 5;
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
			ImageProcessor mask = roi!=null?roi.getMask():null;
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

	public void  write() {
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
		if(roi==null) return(false);
		return(roi.contains(x,y));
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
	
	/*
	public CFU split(int pt1,int pt2) {
		saved = false;
		Polygon p = roi.getPolygon();
		int nb = p.npoints;

		int nb1,nb2,first,last;
		first = pt1<pt2?pt1:pt2;
		last  = pt1<pt2?pt2:pt1;
		nb1= last-first+1;
		nb2 = nb-nb1+2;		
		if(nb1<4 || nb2<4) return null; //cannot create polygon with less than 4 points
		
		//adds point in between first and last
		double mindist = Math.sqrt(Math.pow(p.xpoints[first]-p.xpoints[first+1],2.0)+Math.pow(p.ypoints[first]-p.ypoints[first+1],2.0));
		double dist    = Math.sqrt(Math.pow(p.xpoints[first]-p.xpoints[last],2.0)+Math.pow(p.ypoints[first]-p.ypoints[last],2.0));
		mindist = mindist<1.0?1.0:mindist;
		int nbsup = (int)(dist/mindist);		
		if(nbsup>50) nbsup=50;
		mindist = dist/(1.0+nbsup);
		double deltax = ((double) (p.xpoints[last]-p.xpoints[first]))/((double) nbsup);
		double deltay = ((double) (p.ypoints[last]-p.ypoints[first]))/((double) nbsup);
				
		System.out.println(dist+" "+mindist+" "+nbsup+" "+deltax+" "+deltay);
		
		//contour of new CFU
		int[] x1 = new int[nb1+nbsup];
		int[] y1 = new int[nb1+nbsup];
		//modified contour of old CFU		
		int[] x2 = new int[nb2+nbsup];
		int[] y2 = new int[nb2+nbsup];
				
		//segment of contour between first and last		
		for(int i=0;i<nbsup;i++) {			
			int x = p.xpoints[first]+ (int) ((i+1)*deltax);
			int y = p.ypoints[first]+ (int) ((i+1)*deltay);
			x1[i] = x;
			y1[i] = y;
			x2[i] = x;
			y2[i] = y;
		}
				
		int j=nbsup;
		for(int i=last;i>=first;i--) {
			x1[j] = p.xpoints[i];
			y1[j] = p.ypoints[i];
			j++;
		}
		j=nbsup;
		for(int i=last;i<nb;i++) {
			x2[j] = p.xpoints[i];
			y2[j] = p.ypoints[i];
			j++;
		}for(int i=0;i<=first;i++) {
			x2[j] = p.xpoints[i];
			y2[j] = p.ypoints[i];
			j++;
		}
		
		
		//smoothing of the two split CFUs
//		double[] xma = movingAverage(x1,MovingAverageWidth);
//		double[] yma = movingAverage(y1,MovingAverageWidth);
//		for(int i=0;i<nb1+nbsup;i++) {
//			x1[i]=(int) xma[i];
//			y1[i]=(int) yma[i];
//		}
//		xma = movingAverage(x2,MovingAverageWidth);
//		yma = movingAverage(y2,MovingAverageWidth);
//		for(int i=0;i<nb2+nbsup;i++) {
//			x2[i]=(int) xma[i];
//			y2[i]=(int) yma[i];
//		}
		
		ShapeRoi p1 = new ShapeRoi(new Polygon(x1,y1,nb1+nbsup));
		ShapeRoi p2 = new ShapeRoi(new Polygon(x2,y2,nb2+nbsup));

		saved = false;

		//new cfu
		CFU cfu1 = new CFU(this,p1);
		
		//contour of current CFU must be updated here so that split can be called recursively below!
		this.deleteRoiFile();
		this.setRoi(p2);
		this.write();
				
		saved = true;		
		return(cfu1);
	}
	*/
	
	/*
	public CFU split() {
		//System.out.println("split "+CFUName+"...");    
		Polygon p = roi.getPolygon();

		int nb = p.npoints;
		int delta = (MovingAverageWidth-1)/2;
		if(delta<0) delta=0;


		//smoothing contour by moving average before splitting
		double[] x = movingAverage(p.xpoints,2*delta+1);
		double[] y = movingAverage(p.ypoints,2*delta+1);

		int    pt1,pt2;
		
		double[] curv  = new double[nb];
		double[] alpha = new double[nb];
		double[] perimeter = new double[nb];

		//looking for maximum curvature point
		pt1 = -1;
		curv[0]  = 3*Math.PI;
		perimeter[0] = 0;		
		for(int i=0;i<nb;i++) {    
			int j = i-delta*2; //point A 
			int k = i+delta*2; //point B
			if(j<0)   j += nb;
			if(k>=nb) k -= nb;

			//perimeter
			if(i>0) perimeter[i] = perimeter[i-1] + Math.sqrt(Math.pow(x[i]-x[i-1],2.0)+Math.pow(y[i]-y[i-1],2.0));
			
			//middle of [AB]
			double xm = (x[j]+x[k])/2.0;
			double ym = (y[j]+y[k])/2.0;


			if(!p.contains(xm,ym)) { 
				//The middle of [AB] falls outside the polygon. 
				//Portion of contour is thus convex and curvature is computed.

				//computes angles <ACB>
				double[] restmp = angles(x,y,i,2*delta);											
				//curvature
				curv[i] = restmp[0];	            					
				//angle of the bisector
				alpha[i] = Math.PI+restmp[1];	    
				while(alpha[i]>2*Math.PI) alpha[i] -= 2*Math.PI;	    
				
				//is curvature at point C greater than curvature at point pt1 ?
				if(pt1<0  || curv[i]<curv[pt1]) {
					//update pt1
					pt1 = i;
				}
			} else {
				//portion of contour is concave: curvature is not computed 
				curv[i]  = 3*Math.PI;
				alpha[i] = 3*Math.PI;	        	
			}
		}
		
		if(pt1<0) {
			//no concave point -> split in two halves
			double d = -1;
			pt2=-1;
			for(int i=0;i<nb;i++) {
				int j = i + nb/2;
				if(j<0)   j+=nb;
				if(j>=nb) j-=nb;
				double dtmp = Math.sqrt(Math.pow(x[i]-x[j],2.0)+Math.pow(y[i]-y[j],2.0));
				if(d<0 || dtmp<d) {
					pt1=i;
					pt2=j;
					d=dtmp;
				}
			}
			return(split(pt1,pt2));
		}
		
		//looking for point D, opposite to C on contour
		pt2 = -1;		
		double scer2 = -1;
		//double scer3 = -1;
		for(int i=0;i<nb;i++) {
			if(Math.abs(i-pt1)>3) {
				//middle of [CD]
				double xm = (x[pt1]+x[i])/2.0;
				double ym = (y[pt1]+y[i])/2.0;

				if(p.contains(xm,ym)) { 
					//translation
					double xtr = x[i]-x[pt1];
					double ytr = y[i]-y[pt1];
					//angle
					double l2 = Math.sqrt(Math.pow(xtr,2.0)+Math.pow(ytr,2.0));
					double theta2 = Math.acos(xtr/l2);
					if(ytr<0) theta2 = 2*Math.PI-theta2;
					
					//SCER					
					double scertmp = 0;
					scertmp += Math.pow((theta2-alpha[pt1]),2.0);  //[DC] must align on bisector of <ACB>					
					scertmp += Math.sqrt(l2);
					scertmp += curv[i];										
					if(curv[i] < 2*Math.PI && (scer2<0 || scertmp<scer2)) {
						pt2 = i;
						scer2 = scertmp;	            
					}					
				}
			}
		}
		//if(pt2<0) pt2=pt3;				
		//System.out.println("pt1: "+pt1+" pt2: "+pt2);
		if(pt2<0) {
			pt2 = pt1+nb/2;
			if(pt2<0)   pt2+=nb;
			if(pt2>=nb) pt2-=nb;			
		}
		
		CFU cfu1 = split(pt1,pt2);
		
		//recursive call to split if new CFU is too small
		//this prevents the function to be stuck when only a few pixels have been
		//separated from the main body of the CFU.
		//recursive call is not performed if current CFU is too small
		/*
		if(this.getArea()>20 && cfu1.getArea()<=this.getArea()/100.0) {
				//can end up being stuck in infinite loop!
				cfu1 = this.split();
			} else {
				cfu1.write();			
		}*/
		//if(cfu1.getArea()<20) return null;
		//return cfu1;
	//}
	


	/*
	 * public ArrayList<CFU> splitToMax(ImageProcessor ip,boolean light,boolean
	 * bubble) { saved = true; //split to Max creates new CFU but does not change
	 * the original one
	 * 
	 * //maxima à l'intérieur du contour // String opt =
	 * "noise=2 output=[Point Selection] exclude"; // if(light) opt = opt+" light";
	 * // IJ.run("Find Maxima...", opt); // Polygon ptMax =
	 * imp.getRoi().getPolygon(); //position of the maxima
	 * 
	 * 
	 * 
	 * //résultats parfois bizarre: coordonnées parfois à l'extérieur du ROI!
	 * MaximumFinder mf = new MaximumFinder(); ip.setRoi(this.getRoi()); //???
	 * Polygon ptMax = mf.getMaxima(ip,2.0,true); //noise = 2.0 ?
	 * 
	 * //ip.setRoi(ptMax); //System.out.println(this.CFUName+": "+ptMax.npoints);
	 * 
	 * ArrayList<CFU> cfu = new ArrayList<CFU>();
	 * 
	 * //if one maximum, nothing to split if(ptMax.npoints<=1) { cfu.add(this);
	 * return(cfu); }
	 * 
	 * Polygon contourCFU = roi.getPolygon(); //original contour of the CFU
	 * 
	 * if(bubble) { //split using blubble int inside = 0; for(int
	 * i=0;i<ptMax.npoints;i++) { int x = ptMax.xpoints[i]; int y =
	 * ptMax.ypoints[i]; if(roi.contains(x,y)) { //the maxima seem to fall sometime
	 * outside contour... double d =
	 * Math.sqrt(Math.pow(contourCFU.xpoints[0]-x,2.0)+Math.pow(contourCFU.ypoints[0
	 * ]-y,2.0)); for(int j=1;j<contourCFU.npoints;j++) { double dbis =
	 * Math.sqrt(Math.pow(contourCFU.xpoints[j]-x,2.0)+Math.pow(contourCFU.ypoints[j
	 * ]-y,2.0)); if(dbis<d) d = dbis; } if(d>1) {//new CFU must be more than two
	 * pixel wide! //d=3; OvalRoi ov = new OvalRoi(x-d,y-d,2*d,2*d); ShapeRoi p =
	 * new ShapeRoi(ov.getPolygon()); CFU cfu2 = new CFU(this,p); cfu.add(cfu2);
	 * inside++; } } }
	 * System.out.println(this.getCFUName()+"->"+ptMax.npoints+"/"+inside); } else {
	 * //split without bubble ArrayList<ArrayList<Double[]>> points = new
	 * ArrayList<ArrayList<Double[]>>(); for(int j=0;j<ptMax.npoints;j++) {
	 * points.add(new ArrayList<Double[]>()); } for(int
	 * i=0;i<contourCFU.npoints;i++) { //walks along original contour //the point to
	 * attribute int x = contourCFU.xpoints[i]; int y = contourCFU.ypoints[i];
	 * 
	 * //distance to the first maximum double d =
	 * Math.sqrt(Math.pow(x-ptMax.xpoints[0],2.0)+Math.pow(y-ptMax.ypoints[0],2.0));
	 * int center = 0; for(int j=1;j<ptMax.npoints;j++) { //distance to other maxima
	 * double d2 =
	 * Math.sqrt(Math.pow(x-ptMax.xpoints[j],2.0)+Math.pow(y-ptMax.ypoints[j],2.0));
	 * if(d2<d) { d = d2; center = j; } } //computes angle (used to put points in
	 * the right order) double theta = Math.acos((x-ptMax.xpoints[center])/d);
	 * if(y<ptMax.ypoints[center]) theta *= -1;
	 * 
	 * //the point is added to the contour that corresponds to the closest maximum
	 * Double[] pt = new Double[3]; pt[0] = (double) x; pt[1] = (double) y; pt[2] =
	 * theta; points.get(center).add(pt); }
	 * 
	 * for(int j=0;j<ptMax.npoints;j++) { if(points.get(j).size()>0) { //sort points
	 * according to theta Collections.sort(points.get(j), new Comparator<Double[]>()
	 * {
	 * 
	 * @Override public int compare(Double[] pt2, Double[] pt1) { if(pt1[2]<pt2[2])
	 * return 1; return 2; } }); //extract x and y from array int nb =
	 * points.get(j).size(); int[] x = new int[nb]; int[] y = new int[nb]; for(int
	 * i=0;i<nb;i++) { x[i] = (int) ((double) points.get(j).get(i)[0]); y[i] = (int)
	 * ((double) points.get(j).get(i)[1]); }
	 * 
	 * cfu.add(new CFU(this,new ShapeRoi(new Polygon(x,y,nb)))); } } } //end split
	 * without bubble
	 * 
	 * if(cfu.size()<=0) { return null; } return(cfu); }
	 */
	}
