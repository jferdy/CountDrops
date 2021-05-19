package countdrops;

import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JOptionPane;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.NewImage;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;
import ij.measure.Measurements;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.EDM;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

public class Well {
	private String  name;
	//private String  plate;
	//private String  image;
	
	private PlateSettings settings;
	
	private int     X;
	private int     Y;
	private int     rowInPlate,colInPlate;
	private int     D;

	private OvalRoi roi;

	private double   dilution = 0;   //dilution factor (log10)
	private double   volume = 5;     //volume (microL)	
	
	private boolean   empty;
	private boolean   ignore;
	private boolean[] nonCountable;

	private boolean saved = false;
	
	private ArrayList<CFU> cfuList = new ArrayList<CFU>();

	public Well(PlateSettings s,String xname,int x,int y,double d,double dilution,double volume) {
		settings = s;
		
		name  = xname;
		//plate = settings.getName();
		//image = settings.getImage();
		//path  = settings.getPath()+name+File.separator;			

		this.dilution = dilution;
		this.volume = volume;

		X=x;
		Y=y;
		rowInPlate = -1;
		colInPlate = -1;

		D=(int)d;
		roi = new OvalRoi(X-D/2,Y-D/2,D,D);

		empty = false; //true;
		ignore = false; 
		nonCountable = new boolean[settings.getNCFUTYPES()+1];
		for(int i=0;i<settings.getNCFUTYPES()+1;i++) nonCountable[i] = false;
	}
	
	public Well(PlateSettings s, String configFile) {
		settings = s;		
		rowInPlate = -1;
		colInPlate = -1;	

		ArrayList<String> content = new ArrayList<String>();
		try {
			//System.out.println("reading "+configFile);
			FileInputStream fis = new FileInputStream(configFile);
			Scanner scanner = new Scanner(fis);

			//reading file line by line using Scanner in Java
			while(scanner.hasNextLine()){
				content.add(scanner.nextLine());
			}    
			scanner.close();
		}
		catch(Exception e) {
			System.out.println("Error while reading "+configFile);
		}

		read(configFile);		
	}

	public void read(String configFile) {		
		ArrayList<String> content = new ArrayList<String>();
		try {
			//System.out.println("reading "+configFile);
			FileInputStream fis = new FileInputStream(configFile);
			Scanner scanner = new Scanner(fis);

			//reading file line by line using Scanner in Java
			while(scanner.hasNextLine()){
				content.add(scanner.nextLine());
			}    
			scanner.close();
		}
		catch(Exception e) {
			System.out.println("Error while reading "+configFile);
		}

		//int sep = configFile.lastIndexOf("/");
		//path = configFile.substring(0,sep+1);	
		saved = true;
		int pos = 0;
		int nb = 2;
		
		ArrayList<String> tags = new ArrayList<String>();
		tags.add("NAME");
		tags.add("PLATE");
		tags.add("IMAGE");
		tags.add("PATH");
		
		try {
			while(nb==2) {
				//the only tag that must be read in file is well's name
				//other tags are obtained from PlateSettings, and read here only for backward compatibility
				String[] cells = content.get(pos).split(";");
				nb = cells.length;
				if(nb==2) {
					if(cells[0].equals("NAME")) name  = cells[1];
					pos++;
				}				
			}
		}
		catch (Exception e) {
			System.out.println("Error while reading well's name in "+configFile);
			saved = false;
		}

		try{
			String[] f = content.get(pos).split(";"); pos++;
			//System.out.println(f[0]+" "+f[1]+" "+f[2]);
			X = Integer.parseInt(f[0]);
			Y = Integer.parseInt(f[1]);
			D = Integer.parseInt(f[2]);
			roi = new OvalRoi(X-D/2,Y-D/2,D,D);
		}
		catch (Exception e) {
			System.out.println("Error while well position in "+configFile);
			saved = false;
		}

		try {
			String[] f = content.get(pos).split(";");
			volume   = Double.parseDouble(f[0]);
			dilution = Double.parseDouble(f[1]);
			pos++;
		}
		catch (Exception e) {
			System.out.println("Problem while reading volume and/or dilution in "+configFile);
			System.out.println("Volume has been fixed to 5 and dilution to 0");
			volume = 5;
			dilution = 0;
			saved = false;
		}

		empty = content.get(pos).equals("true"); pos++; //0 or 1

		nonCountable = new boolean[settings.getNCFUTYPES()+1];

		try {
			String[] f = content.get(pos).split(";"); pos++;
			for(int i=0;i<settings.getNCFUTYPES()+1;i++) {
				if(i<f.length) {
					nonCountable[i] = f[i].equals("true");
				} else {
					nonCountable[i] = false;
				}
			}
		}
		catch(Exception e) {
			System.out.println("Problem while reading non countable CFU type from "+configFile);	    
		}
		try {
			ignore = content.get(pos).equals("true"); pos++; //0 or 1
		}
		catch(Exception e) {
			System.out.println("Problem while reading whether WELL should be ignored from "+configFile);
			System.out.println("Assuming  WELL is not ignored.");
			ignore = false;
			saved = false;
		}

		try {
			//loads CFU from roi file
			loadRoiFiles();
		}
		catch(Exception e) {
			System.out.println("Error while loading roi files for well "+name);
			saved = false;
		}
				
	}

	public PlateSettings getSettings() {return settings;}
	
	public OvalRoi getRoi() {
		return(roi);
	}    
	public String getName() {
		return(name);
	}
	public String getPath() {		
		return settings.getPath()+name+File.separator;
	}
	public String getPlate() {
		return(settings.getName());
	}
	public String getImage() {
		return(settings.getImage());
	}
	public String getTitle() {
		return(settings.getImage()+" -- "+settings.getName()+" -- "+name);
	}    
	public int getX() {
		return(X);
	}
	public int getY() {
		return(Y);
	}
	public int getD() {
		return(D);
	}
	public double getDilution() {
		return(dilution);
	}
	public double getVolume() {
		return(volume);
	}    
	public int getNCFUTYPES() {
		return(settings.getNCFUTYPES());
	}
	public boolean isNonCountable(int i) {
		if(i<0 || i>settings.getNCFUTYPES()) return(false);
		return(nonCountable[i]);
	}
	public boolean hasNonCountable() {
		for(int i=0;i<settings.getNCFUTYPES()+1;i++) {
			if(nonCountable[i]) return(true);
		}
		return(false);
	}
	public boolean hasCFU() {
		for(int i=0;i<settings.getNCFUTYPES()+1;i++) {
			if(getNbCFU(i)>0) return true;
		}
		return(false);
	}
	public String getCFUType(int i) {
		if(i<0 || i>=settings.getNCFUTYPES()) return("NA");
		return(settings.getCFUType(i));
	}    
	public String getCFUColor(int i) {
		if(i<0 || i>=settings.getNCFUTYPES()) return("white");
		return(settings.getCFUColor(i));
	}
	
	public String getCFUColor(String type) {
		return(settings.getCFUColor(type));
	}
	
	public String getCFUKey(int i) {
		if(i<0 || i>=settings.getNCFUTYPES()) return(null);
		return(settings.getCFUKey(i));
	}    
	public boolean isEmpty() {
		return(empty);
	}
	public boolean guessIfEmpty() {
		if(empty) return true;
		if(getNbCFU()>0) return false;
		if(this.hasNonCountable()) return false;
		return true;
	}
	public boolean isIgnored() {
		return(ignore);
	}
	
	public boolean isSaved() {
		return(saved);
	}
	
	public boolean hasNACFU() {		
		for(int i=0;i<getNbCFU();i++) {
			if(cfuList.get(i).getCFUType().equals("NA")) return true;
		}
		return false;	
	}
	
	public boolean hasCfuOutsideWell() {
		Rectangle rect = roi.getBounds();
		double bbw = rect.getWidth()+D/4;
		double bbh = rect.getHeight()+D/4;

		for(int i=0;i<getNbCFU();i++) {
			int x = getCFU(i).getX();
			int y = getCFU(i).getY();
			x+=getX()-bbw/2;
			y+=getY()-bbh/2;
			if(!isInsideWell(x,y)) return true;
		}
		return false;
	}
	
	public boolean hasComments() {
		Path p = FileSystems.getDefault().getPath(getPath()+"/comments.txt");
		return (Files.exists(p, LinkOption.NOFOLLOW_LINKS));
	}
	
	public Comments getComments() {		
		Comments comments = new Comments(getPath());
		return(comments);
	}
	
	public int getRowInPlate() {
		return(rowInPlate);
	}
	public int getColInPlate() {
		return(colInPlate);
	}    
	
	public void setEmpty(boolean t) {
		if(getNbCFU()>0 || hasNonCountable()) empty=false;
		else empty=t;
		//setStroke();
	} 
	   
	public void setEmpty() {
		setEmpty(true);
	}

	public void setIgnored(boolean t) {
		ignore = t;
		saved = false;
		}

	public void setNonCountable(int i,boolean b) {
		if(i<0 || i>settings.getNCFUTYPES()) return;
		if(b && getNbCFU(i)>0) {
			//i=0 means NA
			//cannot set to NC because some CFU have been counted
			System.out.println(i+": cannot set to NC because some CFU have been counted");
		}
		if(getNbCFU(i)>0) {
			//non countable must be false if some CFUs have been counted for this type
			nonCountable[i]=false;
			saved = false;
			return;
		}
		nonCountable[i]=b;
		saved = false;
	}    
	public void setRowInPlate(int i) {
		rowInPlate = i;
		saved = false;
	}
	public void setColInPlate(int i) {
		colInPlate = i;
		saved = false;
	}
	public void setDilution(double z) {
		if(z>=0) {
			dilution = z;
			saved = false;
		}
	}
	public void setVolume(double z) {
		if(z>0) {
			volume = z;
			saved = false;
		}
	}    

	public boolean isToInspect() {
		//returns true if data are missing, i.e
		// * some CFUs are NA
		// * well has no CFU and no NC but is not empty
		if(ignore) return false;
		if(empty)  return false;
		if(hasNACFU()) return true;
		if(getNbCFU()>0) return false;
		
		boolean test = false;
		for(int i=0;i<=settings.getNCFUTYPES();i++) test = test || nonCountable[i];		
		if(test) return false;
		
		return true;		
	}
	
	public void setStroke() {		
		int width = 10;
		if(this.isLocked()) width = 30;
		roi.setStrokeWidth(width);

		if(ignore) {
			roi.setStrokeColor(Color.gray);
			return;
		}
		if(empty) {
			//empty wells are displayed in green
			roi.setStrokeColor(Color.green);
			return;
		}
		
		boolean test = false;//getNbCFU()>0;// && !hasNACFU(); //if uncommented, well will be displayed in white as long as some CFU have NA type...
		for(int i=0;i<=settings.getNCFUTYPES();i++) test = test || nonCountable[i];
		if(test) {
			//some types are NC
			roi.setStrokeColor(Color.red);
			return;
		}
		if(!test & getNbCFU()>0) {
			//non empty wells are displayed in yellow
			roi.setStrokeColor(Color.yellow);
			return;
		} 
		//wells that not empty and have no CFU, or wells that still have NA CFU are still to be inspected
		roi.setStrokeColor(Color.white);		
	}

	public void move(int dx,int dy) {
		X+=dx;
		Y+=dy;
		for(int i=0;i<cfuList.size();i++) cfuList.get(i).move(dx,dy);	   
		saved = false;
		
		write();
	}

	public boolean isInsideWell(int x,int y) {
		//uses coordinates of ImagePicture
		double d = Math.sqrt(Math.pow(x-getX(),2.0)+Math.pow(y-getY(),2.0));
		if(d>D/2.0) return false;
		return true;
	}

//	public boolean isInsideWell(ImageCanvas ic,CFU cfu) {
//		//uses coordinates of imageWell
//		ImageCanvas ic = img.getCanvas();
//		double d = D*ic.getMagnification();
//		Rectangle bounds = ic.getSrcRect();		
//		int x = (int) (bounds.getCenterX() * ic.getMagnification() - d/2.0);
//		int y = (int) (bounds.getCenterY() * ic.getMagnification() - d/2.0);
//		double dist = Math.pow(cfu.getX()*ic.getMagnification()-x,2.0)+Math.pow(cfu.getY()*ic.getMagnification()-y,2.0);
//		dist = Math.sqrt(dist);
//		if(dist>d/2.0) return(false);
//		return(true);
//	}
	
	public void draw(ImagePlus imp) {
		setStroke();
		imp.setRoi(roi);
	}

	public void draw(ImageCanvas ic) { //draw well contour on well image				
		double d = D*ic.getMagnification();					
		Rectangle bounds = ic.getSrcRect(); //ic.getBounds(); // I don't really get why getBOunds and getSrcRect do not return the same thing
		int x = (int) (bounds.getCenterX() * ic.getMagnification() - d/2.0);
		int y = (int) (bounds.getCenterY() * ic.getMagnification() - d/2.0);
		OvalRoi contour = new OvalRoi(x,y,d,d);			
		contour.setStrokeColor(Color.gray);
		ic.getOverlay().add(contour);
	}

	public void draw(Overlay ov) { //draw well contour on plate image
		setStroke();
		
		ov.add(roi);

		TextRoi.setFont("Arial",24,Font.PLAIN);
		
		
		String str = "";
		
		if(getNbCFU()>0) str+=" "+getNbCFU();
				
		double x = getX()+((double) getD())*1.2*Math.cos(-3.0*Math.PI/4.0)/2.0;
		double y = getY()+((double) getD())*1.2*Math.sin(-3.0*Math.PI/4.0)/2.0;

		if(!str.equals("")) {			
			TextRoi txt = new TextRoi(x,y,str);																
			txt.setJustification(TextRoi.RIGHT);
			txt.setFillColor(Color.white);
			TextRoi.setColor(Color.black);			
			
			ov.add(txt);			
		}

		if(hasCfuOutsideWell()) {
			OvalRoi pt = new OvalRoi(x-24,y+24+12,24,24);
			pt.setFillColor(Color.red);						
			ov.add(pt);			
			OvalRoi pt2 = new OvalRoi(x-24,y+24+12,24,24);
			pt.setStrokeColor(Color.black);
			ov.add(pt2);			
		}

		if(hasNACFU()) {			
			TextRoi txt = new TextRoi(x+6,y,"NA");								
			txt.setJustification(TextRoi.LEFT);
			txt.setFillColor(Color.RED);						
			ov.add(txt);			
		}
		if(hasComments()) {			
			OvalRoi pt = new OvalRoi(x,y+24+12,24,24);
			pt.setFillColor(Color.green);						
			ov.add(pt);			
			OvalRoi pt2 = new OvalRoi(x,y+24+12,24,24);
			pt.setStrokeColor(Color.black);
			ov.add(pt2);			
		}
	}
	
	public boolean isLocked() {
		File lock = new File(getPath()+"lock");
		return(lock.exists());
	}

	public void lock() {
		//check if well has been lock by another instance
		File lock = new File(getPath()+"lock");
		if(!lock.exists()) {
			try {
				lock.createNewFile();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null,"Error while locking "+name+" directory! "+e.getMessage(), "Inspecting Well", JOptionPane.ERROR_MESSAGE);
				System.out.println("Failed to lock well!");
				return;
			}
		}
	}
	public void unlock() {
		//check if well has been lock by another instance
		File lock = new File(getPath()+"lock");
		if(lock.exists()) {
			try {
				lock.delete();
			} catch (Exception e) {
				IJ.error("Error while unlocking "+name+" directory!", e.getMessage());
				return;
			}
		}
	}
	
	public void setImagePlusToCFU(ImagePlus imp) {
		for(int i=0;i<this.getNbCFU();i++) {
			this.getCFU(i).setImagePlus(imp);
		}
	}
	
	public Rectangle getBounds(ImagePlus impOrig) {
		if(impOrig==null) return(null);

		//select well bounding box
		Rectangle rect = roi.getBounds();
		//IJ.error("x="+rect.getX()+" y="+rect.getY()+" w="+rect.getWidth()+" h="+rect.getHeight());

		double bbX = rect.getX()-D/8;
		double bbY = rect.getY()-D/8;
		if(bbX<0) bbX=0;
		if(bbY<0) bbY=0;
		double bbw = rect.getWidth()+D/4;
		double bbh = rect.getHeight()+D/4;
		if(bbX+bbw>impOrig.getWidth())  bbw=impOrig.getWidth()-bbX;
		if(bbY+bbh>impOrig.getHeight()) bbh=impOrig.getHeight()-bbY;
		
		rect.setLocation((int)bbX,(int)bbY);
		rect.setSize((int)bbw,(int)bbh);

		return rect;
	}
	
	public ImagePlus getImagePlus(ImagePlus impOrig) {
		if(impOrig==null) return(null);
		Rectangle rect = getBounds(impOrig);
		
		
		int nsl = impOrig.getNSlices();
		int csl = impOrig.getCurrentSlice();

		//create new image from selection	
		ImagePlus imp = NewImage.createRGBImage(this.getTitle(),(int)(rect.getWidth()),(int)(rect.getHeight()),nsl, NewImage.FILL_BLACK);
		impOrig.setRoi(rect);
		for(int i=1;i<=nsl;i++) {	    
			impOrig.setSlice(i);			
			ImageProcessor ip = impOrig.getProcessor().crop();
			imp.setSlice(i);	
			imp.setProcessor(ip);
		}
		impOrig.deleteRoi();
		impOrig.setSlice(csl);
		imp.setSlice(csl);

		imp.updateImage();

		setImagePlusToCFU(imp);
		return(imp);
	}        
	
	public void write() {
		if(saved) return; //data have already been saved !
		try{
			//if directory does not exist, create it
			//System.out.println(path);
			File theDir = new File(getPath());
			if (!theDir.exists()) {
				//System.out.println("creating directory: "+path);
				try{
					theDir.mkdir();
				} 
				catch(SecurityException se){
					return;
				}
			}

			//write data
			PrintWriter writer = new PrintWriter(getPath()+"config.cfg", "UTF-8");
			writer.println("NAME;"+name);

			writer.println(X+";"+Y+";"+D);
			writer.println(volume+";"+dilution);
			writer.println(empty);
			String str = ""+nonCountable[0]; //NA are nont countable!
			for(int i=1;i<settings.getNCFUTYPES()+1;i++) str = str+";"+nonCountable[i];	    
			writer.println(str);
			writer.print(ignore);
			writer.close();
			saved = true;
		}
		catch(IOException se){
		}		
	}

	public void save() {
		write();
		for(int i=0;i<this.getNbCFU();i++) {
			this.getCFU(i).write();
		}		
	}
	
	public CFU getCFU(int index) {
		if(index<0 || index>=cfuList.size()) return(null);
		return(cfuList.get(index));
	}

	public void addCFU(CFU cfu) {
		if(cfu!=null) {
			cfuList.add(cfu);
			empty = false;
			saved = false;
			cfu.write();
		}
	}

	public boolean addCFU(CFU cfu,double maxOverlap,int minSize,double minCirc) {
		//test if CFU reaches min size and circularity
		if(cfu.getArea()<minSize || cfu.getCircularity()<minCirc) {
			return(false);
		}

		//test whether new CFU overlaps older CFU
		boolean test = true;
		for(int k=0;k<getNbCFU() && test;k++) {
			double ov = this.getCFU(k).overlap(cfu);
			test = ov < maxOverlap;			
		}

		if(test) {
			addCFU(cfu);
			return(true);
		}

		return(false);
	}

	public boolean deleteCFU(int index) {
		if(index<0 || index>=cfuList.size()) return false;	
		CFU cfu = cfuList.get(index);
		if(!cfu.isSaved()) return false; //cfu cannot be edited if saving is still in process
		
		//System.out.println("delete "+name+" "+cfu.getCFUName());
		cfu.deleteRoiFile();
		cfuList.remove(index);
		if(getNbCFU()<=0) {
			if(!hasNonCountable()) empty=true;
		} else {
			empty=false;
		}
		saved = false;
		return true;
	}
	
	public boolean deleteCFU(ArrayList<Integer> listIndex) {
		boolean someCFUdeleted = false;
		for(int index=getNbCFU()-1;index>=0;index--) {			
			if(listIndex.contains(index)) {
				CFU cfu = cfuList.get(index);
				if(cfu.isSaved()) {
					someCFUdeleted = true;
					System.out.println("delete "+name+" "+cfu.getCFUName());
					cfu.deleteRoiFile();
					cfuList.remove(index);
				}
			}
		}
		if(getNbCFU()<=0) {
			if(!hasNonCountable()) empty=true;
		} else {
			empty=false;
		}
		if(someCFUdeleted) saved = false;
		return someCFUdeleted;	
	}
	
	public boolean deleteAllCFU() {
		if(getNbCFU()<=0) return false;
		
		for(int index=getNbCFU()-1;index>=0;index--) {			
				CFU cfu = cfuList.get(index);
				if(cfu.isSaved()) {
					//System.out.println("delete "+name+" "+cfu.getCFUName());
					cfu.deleteRoiFile();
					cfuList.remove(index);
				}
		}
		if(getNbCFU()<=0) {
			if(!hasNonCountable()) empty=true;
		} else {
			empty=false;
		}
		saved = false;
		return true;
	}
	
	public boolean deleteCFU(int sizeMin,double circMin) {
		boolean someCFUdeleted = false;
		for(int i=0;i<getNbCFU();i++) {
			CFU cfu = cfuList.get(i);			
			if(cfu.getArea()<sizeMin && cfu.getCircularity() < circMin && cfu.isSaved()) {
				someCFUdeleted = someCFUdeleted || deleteCFU(i);			
			}
		}
		if(getNbCFU()<=0) {
			empty=!hasNonCountable();
		} else {
			empty=false;
		}
		if(someCFUdeleted) saved = false;
		return someCFUdeleted;
	}

	public boolean changeCFUType(String key,ArrayList<Integer> selectedCFU) {
		if(selectedCFU==null) return false;
		if(selectedCFU.size()<=0) return false; //no selection to change
		
		int i=0;
		while(i<settings.getNCFUTYPES() && ! key.equals(settings.getCFUKey(i).toUpperCase())) i++;
		if(i>=settings.getNCFUTYPES()) return false; //type is not recognized
		
		boolean someCFUchanged = false;
		for(Integer index : selectedCFU) {
			CFU cfu = getCFU(index);
			if(cfu!=null && cfu.isSaved()) {
				cfu.setCFUType(settings.getCFUType(i));
				someCFUchanged = true;
			}
		}
		
		if(someCFUchanged) {
			//type i should not be NC if some CFU have been changed!
			nonCountable[i] = false;
			saved = false;
		}
		return someCFUchanged;
	}
	
	public boolean unsetCFUType() {
		boolean someCFUchanged = false;
		for(int i=0;i<getNbCFU();i++) {
			CFU cfu = cfuList.get(i);
			if(cfu.isSaved()) {
				someCFUchanged = true;
				cfu.unsetCFUType();
			}
		}
		if(someCFUchanged) {
			nonCountable[0] = false;
			saved = false;
		}
		return someCFUchanged;
	}

	public boolean unsetCFUType(ArrayList<Integer> index) {
		boolean someCFUchanged = false;
		for(int i : index) {
			CFU cfu = cfuList.get(i);
			if(cfu!=null && cfu.isSaved() && !cfu.getCFUType().equals("NA")) {
				someCFUchanged = true;
				cfu.unsetCFUType();
			}
		}
		if(someCFUchanged) {
			nonCountable[0] = false;
			saved = false;
		}
		return someCFUchanged;
	}

	public int getNbCFU() {
		if(cfuList==null) return(0);
		return(cfuList.size());
	}

	public int getNbCFU(int j) {
		// returns -2 if well is not empty and no CFU have been counted (which should never happen)
		// returns -1 if CFU type is not countable
		
		//TODO check that negative values are correctly interpreted elsewhere!
		
		if(isEmpty()) return(0);					
		if(cfuList==null) return(-2); //well is not empty of no CFU have been counted!
		
		String type = "NA";
		if(j>0 && j<=settings.getNCFUTYPES()) type = settings.getCFUType(j-1);  //WARNING j=0 is NA!!		
		if(isNonCountable(j)) return(-1);
		
		int nb=0;
		for(int i=0;i<cfuList.size();i++) {
			CFU cfu = cfuList.get(i);
			if(cfu.getCFUType().equals(type)) nb++;
		}
				
		return(nb);
	}

	public void saveCFU() {
		if(cfuList==null) return;
		for(int i=0;i<cfuList.size();i++) {
			cfuList.get(i).write();
		}
	}
	
	public int splitCFU(CFU cfu) {
		int index = 0;
		while(index < getNbCFU() & !getCFU(index).equals(cfu)) index++;
		return(splitCFU(index));		
	}
	
	public int splitCFU(int index) {
		if(index<0 || index>=cfuList.size()) return(0);

		CFU cfu1 = cfuList.get(index);
		if(cfu1==null || !cfu1.isSaved()) return(0);
					
		CFU[] newcfu = cfu1.split();
		if(newcfu!=null) {
			for(int i=0;i<newcfu.length;i++) {
				addCFU(newcfu[i]);
				newcfu[i].write();
			}
			this.deleteCFU(index);
			return(newcfu.length);
		}
		
		return(0);
	}
	
	public int getNbRoiFiles() {
		//well directory
		File folder = new File(getPath());
		if(!folder.exists()) return -1;

		// create new filename filter
		FilenameFilter fileNameFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir,String name) {
				File f = new File(dir,name);
				if(!f.isFile()) return false;

				if(name.lastIndexOf('.')>0)
				{
					// get last index for '.' char
					int lastIndex = name.lastIndexOf('.');                  
					// get extension
					String str = name.substring(lastIndex);                  
					// match path name extension
					if(str.equals(".roi"))
					{
						return true;
					}
				}
				return false;
			}
		};


		File[] listOfFiles = folder.listFiles(fileNameFilter);
		return(listOfFiles.length);
	}

	public File[] listRoiFiles() {
		//well directory
		File folder = new File(getPath());
		if(!folder.exists()) {
			System.out.println("Folder "+getPath()+" does not exist!");
			return null;
		}

		// create new filename filter
		FilenameFilter fileNameFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir,String name) {
				File f = new File(dir,name);
				if(!f.isFile()) return false;

				if(name.lastIndexOf('.')>0)
				{
					// get last index for '.' char
					int lastIndex = name.lastIndexOf('.');                  
					// get extension
					String str = name.substring(lastIndex);                  
					// match path name extension (only roi files should be scanned!)
					if(str.equals(".roi"))
					{
						return true;
					}
				}
				return false;
			}
		};


		File[] listOfFiles = folder.listFiles(fileNameFilter);
		if(listOfFiles.length<=0) return null;

		return(listOfFiles);
	}
	public void deleteRoiFiles() {
		File[] listOfFiles = listRoiFiles();
		if(listOfFiles==null || listOfFiles.length<=0) return;

		for(int i=0;i<listOfFiles.length;i++) {
			if(listOfFiles[i].exists()) listOfFiles[i].delete();
		}
	}
	public void loadRoiFiles() {
		File[] listOfFiles = listRoiFiles();		
		if(listOfFiles==null) return;

		if(cfuList!=null) cfuList.clear();
		for(int i=0;i<listOfFiles.length;i++) {
			String txt = listOfFiles[i].getName();
			CFU cfu = new CFU(this,this.getPath()+txt);
			if(cfu!=null) cfuList.add(cfu);		
		}
		setEmpty();
	}

	public int[] getResult() {		
		File[] listOfFiles = listRoiFiles();
		//returns NULL if no Roi files
		if(listOfFiles==null || listOfFiles.length<=0) return null;

		//Otherwise, scans Roi files to count number of CFU for each type
		int[] result = new int[settings.getNCFUTYPES()+1];
		for(int i=0;i<result.length;i++) result[i]=0;

		for(int i=0;i<listOfFiles.length;i++) {
			String roiFile = listOfFiles[i].getName();
			//System.out.println(getName()+" "+roiFile);

			//reads first line of roi file
			ArrayList<String> content = new ArrayList<String>();
			try {
				FileInputStream fis = new FileInputStream(getPath()+roiFile);
				Scanner scanner = new Scanner(fis);	   
				content.add(scanner.nextLine());
				scanner.close();

				String type = content.get(0);
				type = type.replaceAll("CFUType;(.*)", "$1");

				//increments counts
				int j=0;
				while(j<settings.getNCFUTYPES() && !settings.getCFUType(j).equals(type)) {
					j++;
				}
				if(j>-1 && j<result.length) result[j]++;

			}
			catch(Exception e) {
				System.out.println("Error while reading "+roiFile);
			}
		}
		return(result);
	}

	public int whichCFU(int x,int y) {
		if(cfuList == null) return(-1);

		for(int i=0;i<cfuList.size();i++) {
			if(cfuList.get(i).contains(x,y)) return(i);
		}
		return(-1);
	}

	public ImagePlus convertImageToGrayLevels(ImagePlus imp,int slice,boolean light) {
		//copy current slice in new image		
		ImagePlus impCpy = NewImage.createRGBImage(imp.getShortTitle(),imp.getWidth(),imp.getHeight(),1,NewImage.FILL_BLACK);						
		ImageProcessor ipCpy = imp.getStack().getProcessor(slice).duplicate();
		//ImageProcessor ipCpy = new ColorProcessor(imp.getImage());	
		
		//substract background
		BackgroundSubtracter bs = new BackgroundSubtracter();	
		bs.rollingBallBackground(ipCpy,50.0,false,light,false,false,true);
		impCpy.setProcessor(ipCpy);
		
		//convert to graylevel
		ImageConverter impConv = new ImageConverter(impCpy);
		impConv.convertToGray32();		
		
		return(impCpy);		
	}
	

	public int detectCFU(ImagePlus imp,int slice,double gBlurSigma,double enhanceContrast,double threshold,double minThreshold,boolean lightBackground,int minSize,double minCirc,boolean excludeOutsideWell, String defaultCFUtype) {
		ImagePlus impCpy = new ImagePlus("",imp.getStack().getProcessor(slice));		
		
		//convert to graylevel
		ImageConverter impConv = new ImageConverter(impCpy);
		impConv.convertToGray32();		
		
		//enhance contrast
		if(enhanceContrast>0) {
			ContrastEnhancer enh = new ContrastEnhancer();
			enh.stretchHistogram(impCpy, enhanceContrast);			                                            
		}
		
		//add gaussian blur
		if(gBlurSigma>0) {
			GaussianBlur gb = new GaussianBlur();
			gb.blurGaussian(impCpy.getProcessor(), gBlurSigma);
		}		
		
		// set threshold using default autothreshold
		// the CFU should eventually turned to white on a black background
		// one problem with this method arises when well is empty: the threshold is then very low
		// (or very high depending on the background) and false positives are generated 
		// autoThreshold is considered meaningful when above 10 and below 245
	    impCpy.getProcessor().setAutoThreshold(AutoThresholder.Method.Default,!lightBackground);
	    if(threshold<0) {
	    	threshold = impCpy.getProcessor().getAutoThreshold();
	    	if(threshold<=255*minThreshold || threshold>=255*(1.0-minThreshold)) return(0);
	    }
	    	
	    
	    if(lightBackground) {
	    	impCpy.getProcessor().setThreshold(0.0, threshold, ImageProcessor.NO_LUT_UPDATE);	
	    } else {
	    	impCpy.getProcessor().setThreshold(threshold, 255.0, ImageProcessor.NO_LUT_UPDATE);	
	    }	    	   
	    
	    
	    //impCpy.getProcessor().autoThreshold();	    
	    ByteProcessor mask = impCpy.getProcessor().createMask();
	    
		//watershed
		EDM edm = new EDM();
		edm.toWatershed(mask);

		impCpy.setProcessor(mask);
		impCpy.getProcessor().setThreshold(125,255,ImageProcessor.NO_LUT_UPDATE);
		
			
		//particle analysis
		ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE+ParticleAnalyzer.ADD_TO_MANAGER+ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES,//+ParticleAnalyzer.INCLUDE_HOLES,//
				Measurements.AREA,
				null, 
				minSize,
				imp.getWidth()*imp.getHeight()/2.0, //max size
				minCirc,
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
		
		boolean success = pa.analyze(impCpy,impCpy.getProcessor());
		if(!success) return(0);
		
		
		Roi[] vroi = roima.getRoisAsArray();
		roima.reset();
		roima.close();			
		
		if(vroi==null || vroi.length<=0) {
			setEmpty();
			return(0); //nothing has been detected
		}
		
		int cmpt = 0;
		double wellX = impCpy.getWidth()/2.0;
		double wellY = impCpy.getHeight()/2.0;
		for(int i=0;i<vroi.length;i++) {
			//create new CFU
			Polygon p = vroi[i].getPolygon();						
			boolean includeCFU = true;
			CFU cfu = new CFU(this,new ShapeRoi(p));
			cfu.setCFUType(defaultCFUtype);
			if(excludeOutsideWell) {				
				double dist = Math.pow(wellX-cfu.getX(), 2.0)+Math.pow(wellY-cfu.getY(), 2.0);
				dist = Math.sqrt(dist);
				if(dist>D/2.0) includeCFU = false;
			}
			if(includeCFU) {							
				addCFU(cfu);
				cmpt++;
			}
		}		

		return(cmpt);		
	}
		
	public void exportCounts(PrintWriter writer) {
		//printer headers 
		for(int i=0;i<getSettings().getNFIELDS();i++) {			
			writer.print(getSettings().getFieldsValue().get(i)+";");		
		}
		
		writer.print(getName()+";"+getVolume()+";"+getDilution());
		if(isIgnored()) {
			//well is ignored: count will be NA in result file.
			for(int j=0;j<=getNCFUTYPES();j++) writer.print(";");
		} else {
			if(isEmpty()) {
				//well has been declared empty
				for(int j=0;j<=getNCFUTYPES();j++) writer.print(";0");
			} else {
				//well has not been declared empty
				int[] result = getResult();
				if(result!=null) {
					//result is not null: Well has some CFU
					for(int j=0;j<result.length;j++) {
						if(j<getNCFUTYPES()+1) {
							if(!isNonCountable(j+1)) {
								writer.print(";"+result[j]);
							} else {
								writer.print(";Inf");
							}
						}
					}
				} else {
					//result is null but well is not empty. Two possible cases: either some types are noncountable (NC), and then something
					//should be written, or no types are NC, which means that nothing has been done for that well
					if(hasNonCountable()) {
						//case 1 -- some CFU types are NC
						//save an empty line whith Inf for NA if NA is NC (meaning that the color of uncountable CFU could not be determined)
						//save a line of zero with Inf for noncountable types otherwise.
						//a weird case would be that both NA and another type is NC...
						boolean NAisNC = isNonCountable(0); //careful here : NA are in position zero in nonCountable vector!!
						for(int j=0;j<getNCFUTYPES()+1;j++) {
							if(!isNonCountable(j+1)) {
								if(NAisNC) writer.print(";"); else writer.print(";0"); 
							} else {			    
								writer.print(";Inf");
							}
						}
					} else {
						//case 2 -- no types are NC: write an empty line 
						for(int j=0;j<=getNCFUTYPES();j++) writer.print(";");
					}
				}	
			}
		}
		writer.println("");
	}
}
