package countdrops;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JOptionPane;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;

public class Plate {
	//settings
	private PlateSettings settings;
	
	//default parameters for CFU detection
	private double   sensitivityInf = 0.05;
	private double[] sensitivitySup = {0.1,0.5,0.1};
	private boolean  lightBackground = true;
	private int      sizeMin = 20;
	private double   circMin = 0.2;

	private boolean Xreversed = false;
	private boolean Yreversed = false;
	
	private ArrayList<Well> wells = new ArrayList<Well>();

	public Plate(PlateSettings s) throws Exception {		
		settings = s;				
		settings.setPathAndNameFromFields();
		//TODO check that plate does not already exist?
				
		if(createDirectory()) {
			settings.save();	
			setWell();
		} else {
			throw new Exception("Failed to create new plate!");
		}
	}
	
	public Plate(String configFile) throws Exception {
		File f = new File(configFile);
		if(!f.exists()) throw new Exception("Cannot create plate: file "+configFile+" does not exist.");
		
		settings = new PlateSettings();
		settings.setPath(f.getParent()+File.separator);
		settings.setFileName(f.getName());
		
		settings.read();
		
		setWell();
	}

	// does the plate directory exists ?
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	protected boolean checkIfPlateExists() {
		System.out.println("check if "+settings.getPath()+" already exists");
		File theDir = new File(settings.getPath());	
		return(theDir.exists());
	}

	public PlateSettings getSettings() {
		return settings;
	}

	public void setSettings(PlateSettings settings) {
		this.settings = settings;
	}

	public  String   getImage() {return(settings.getImage());}
	public  String   getPath()  {return(settings.getPath());}
	public  String   getName()  {return(settings.getName());}
	public  int      getNROWS() {return(settings.getNROWS());}
	public  int      getNCOLS() {return(settings.getNCOLS());}
	public  int      getNWELLS() {if(wells!=null) return(wells.size()); return(0);}
	public  int      getNFIELDS() {return(settings.getNFIELDS());}
	public  double   getVolume() {return(settings.getVolume());}
	public  int      getDilutionScheme() {return(settings.getDilutionScheme());}
	public  double[] getDilution() {return(settings.getDilution());}
	public  int      getNCFUTYPES() {return(settings.getNCFUTYPES());}
	
	public  ArrayList<String> getCFUType() {return(settings.getCFUType());}
	public  ArrayList<String> getCFUColor() {return(settings.getCFUColor());}
	public  ArrayList<String> getCFUKey() {return(settings.getCFUKey());}
	
	public int getBoxX(int i) {return(settings.getBoxX(i));}
	public int getBoxY(int i) {return(settings.getBoxY(i));}
	
	public  boolean  getLightBackground() {return(lightBackground);}
	public  int      getSizeMin() {return(sizeMin);}
	public  double   getCircMin() {return(circMin);}
	public  double   getSensitivityInf() {return(sensitivityInf);}
	public  double   getSensitivitySup(int i) {if(i<0||i>2) return(-1); return(sensitivitySup[i]);}        
	public String[] getField (int index) {
		if(index<0 || index>=getNFIELDS()) return(null);	
		String[] res = new String[3];
		res[0] = settings.getFieldsType().get(index);
		res[1] = settings.getFieldsName().get(index);
		res[2] = settings.getFieldsValue().get(index);
		return(res);
	}    
	public String[] getCFUDescription(int index) {
		if(index<0 || index>=getNCFUTYPES()) return(null);	
		String[] res = new String[3];
		res[0] = settings.getCFUType().get(index);
		res[1] = settings.getCFUColor().get(index);
		res[2] = settings.getCFUKey().get(index);
		return(res);
	}    
	
	public String getFieldsType (int index) {
		if(index<0 || index>getNFIELDS()) return null;
		return(settings.getFieldsType().get(index));
	}
	public String getFieldsName(int index) {
		if(index<0 || index>getNFIELDS()) return null;
		return(settings.getFieldsName().get(index));
	}
	
	public String getFieldsValue (int index) {
		if(index<0 || index>getNFIELDS()) return null;
		return(settings.getFieldsValue().get(index));
	}    	
	
	public String getCFUType (int index) {
		if(index<0 || index>getNCFUTYPES()) return null;
		return(settings.getCFUType().get(index));
	}
	
	public int getFieldsTypeAsInt(int i) {
		if("int".equals(getFieldsType(i))) return 1;
		if("boolean".equals(getFieldsType(i))) return 2;
		if("float".equals(getFieldsType(i))) return 3;
		if("str".equals(getFieldsType(i))) return 4;
		return -1;
	}

	public boolean isXreversed() {return(Xreversed);}
	public boolean isYreversed() {return(Yreversed);}
	
	public  void  setLightBackground(boolean t) {lightBackground=t;}
	public  void  setSizeMin(int x) {sizeMin = x;}
	public  void  setCircMin(double x) {circMin = x;}
	public  void  setSensitivityInf(double x) {sensitivityInf = x;}
	public  void  setSensitivitySup(int i,double x) {if(i>=0 && i<=2) sensitivitySup[i] = x;}        

	public void setImage(String txt) {settings.setImage(txt);}
	public void setPath(String txt)  {settings.setPath(txt);}       

	public void setFieldsValue(String[] values) {
		if(values.length != getNFIELDS()) return;
		for(int i=0;i<getNFIELDS();i++) {
			settings.setFieldsValue(i,values[i]);
		}
		settings.setPathAndNameFromFields();		
	}

	public void setField (int index,String value) {
		if(index<0 || index>getNFIELDS()) return;
		settings.setFieldsValue(index,value);
	}

	public void editField(String[] ft,String[] fn,String[] f) {
		for(int i=0;i<getNFIELDS();i++) {
			settings.setFieldsType(i,ft[i]);
			settings.setFieldsName(i,fn[i]);
			settings.setFieldsValue(i,f[i]);
			if(settings.getName()=="") {
				settings.setName(f[i]);
			} else {
				settings.setName(settings.getName()+"_"+f[i]);
			}
		}	
	}

	public boolean createDirectory() {
		File theDir = new File(settings.getPath());
		// if the plate directory does not exist, create it
		if (!theDir.exists()) {
			System.out.println("creating directory: " + settings.getPath());
			try{
				theDir.mkdirs(); //mkdir does not work if the image directory is missing !! Only mkdirs will work
				return true;
			} 
			catch(SecurityException se){
				return false;
			}
		}
		return false;
	}

	public void setBox(int[] x,int[] y) {
		for(int i=0;i<3;i++) {
			settings.setBoxX(i,x[i]);
			settings.setBoxY(i,y[i]);
		}	
	}
		
	public double getDilution(int row,int col) {
		switch(getDilutionScheme()) {
		case 1:
			return(getDilution()[0]); //fixed dilution
		case 2:
			return(getDilution()[row]); //dilution by row
		case 3:
			return(getDilution()[col]); //dilution by col	    
		case 4:
			return(getDilution()[row*getNCOLS()+col]); //dilution by well	    
		default:
			return 0.0;
		}
	}    
	
	public void setWell() {
		if(settings.getPath()==null) {
			System.out.println("Plate "+settings.getName()+" has null path. Cannot read wells' data!");
			return;
		}
		
		double d1 = Math.sqrt(Math.pow(getBoxX(0)-getBoxX(3),2)+Math.pow(getBoxY(0)-getBoxY(3),2))/((double)getNCOLS()-1.0);
		double d2 = Math.sqrt(Math.pow(getBoxX(0)-getBoxX(1),2)+Math.pow(getBoxY(0)-getBoxY(1),2))/((double)getNROWS()-1.0);		
		double d = (d1+d2)/2.0;
		
		for(int j=0;j<getNROWS();j++) {
			for(int i=0;i<getNCOLS();i++) {
				String str = PlateSettings.getRowLetterFromInt(j)+(i+1);
				File   configFile = new File(settings.getPath()+str+"/config.cfg");
				Well   w = null;
				if(configFile.exists()) {
					//well directory exists
					w = new Well(settings,settings.getPath()+str+"/config.cfg");
				} else {
					//well directory does not exist: compute well coordinates
					double x = getBoxX(0) + (getBoxX(3)-getBoxX(0))*i/((double)getNCOLS()-1.0) + (getBoxX(1)-getBoxX(0))*j/((double) getNROWS()-1.0);
					double y = getBoxY(0) + (getBoxY(3)-getBoxY(0))*i/((double)getNCOLS()-1.0) + (getBoxY(1)-getBoxY(0))*j/((double) getNROWS()-1.0);				
					//creation of new well (settings.getPath()+str is the path to Well directory!)
					w = new Well(settings,str,(int)x,(int)y,d,getDilution(j,i),getVolume());					
					w.write();
				}
				//sets coordinates in plate
				w.setColInPlate(i);
				w.setRowInPlate(j);
				//add well to list
				wells.add(w);		
			}
		}
		
		//guess if plate is reversed (e.g. A1 on the right side and A12 on the left)
		if(getWell(0,0).getX()>getWell(0,1).getX()) Xreversed = true;
		if(getWell(0,0).getX()<getWell(1,0).getX()) Yreversed = true;
	}    

	public void updateWellDilutionAndVolume() {
		for(int j=0;j<getNROWS();j++) {
			for(int i=0;i<getNCOLS();i++) {
				Well w = getWell(j,i); 
				w.setDilution(getDilution(j,i));
				w.setVolume(getVolume());
				w.write();
			}				
		}
			
	}
	
	public int getNbWell () {
		return(wells.size());
	}

	public int getNbCFUs () {
		int nb = 0;
		for(int i=0;i<wells.size();i++) {
			nb += wells.get(i).getNbCFU();
		}
		return nb;
	}

	public boolean hasNACFU() {
		for(int i=0;i<wells.size();i++) {
			if(wells.get(i).hasNACFU()) return true;
		}
		return false;
	}

	public boolean hasWellToInspect() {
		for(int i=0;i<wells.size();i++) {
			if(wells.get(i).isToInspect()) return true;
		}
		return false;
	}

	public boolean hasComments() {
		for(int i=0;i<wells.size();i++) {
			if(wells.get(i).hasComments()) return true;
		}
		return false;
	}
	
	public void setProblems(boolean b) {
		settings.setProblems(b);
	}	

	public boolean hasProblems() {
		return settings.hasProblems();	
	}
	
	public boolean hasNonCountable() {
		for(int i=0;i<wells.size();i++) {
			if(wells.get(i).hasNonCountable()) return true;
		}
		return false;
	}

	public void read() {
		//String configFile = settings.getPath()+"config.cfg";
		try{
			settings.read();
		} catch(Exception ex) {
			
		}
	}

	public void draw(Overlay overlay) {
		overlay.clear();
		for(int i=0;i<wells.size();i++) {
			Well w = wells.get(i);
			w.draw(overlay);	    
		}	
	}

	public void drawOutline(Overlay overlay) {		
		PolygonRoi p = new PolygonRoi(settings.getBoxX(),settings.getBoxY(),4,PolygonRoi.POLYGON);
		p.setStrokeWidth(20.0);
		p.setStrokeColor(Color.gray);				
		overlay.add(p);
		Line l = new Line(settings.getBoxX()[0],settings.getBoxY()[0],settings.getBoxX()[2],settings.getBoxY()[2]);
		l.setStrokeWidth(10.0);
		l.setStrokeColor(Color.gray);
		overlay.add(l);
		l = new Line(settings.getBoxX()[1],settings.getBoxY()[1],settings.getBoxX()[3],settings.getBoxY()[3]);
		l.setStrokeWidth(10.0);
		l.setStrokeColor(Color.gray);
		overlay.add(l);
	}

	private double[] getCoordinates(int x,int y) {
		double a1 = (getBoxX(3)-getBoxX(0))/(getNCOLS()-1);
		double a2 = (getBoxY(3)-getBoxY(0))/(getNCOLS()-1);
		double b1 = (getBoxX(1)-getBoxX(0))/(getNROWS()-1);
		double b2 = (getBoxY(1)-getBoxY(0))/(getNROWS()-1);
		double c1 = x-getBoxX(0);
		double c2 = y-getBoxY(0);
		// System.out.println(a1+" "+b1+" "+c1);
		// System.out.println(a2+" "+b2+" "+c2);

		double[] coord = new double[2];
		if(a1==0 && b1!=0) {
			coord[1] = c1/b1;
			coord[0] = (c2-b2*coord[1])/a2;
			return(coord);
		}

		if(a2==0 && b2!=0) {
			coord[1] = c2/b2;
			coord[0] = (c1-b1*coord[1])/a1;
			return(coord);
		}

		if(a1!=0 && a2!=0) {
			coord[1] = (c1/a1 - c2/a2)/(b1/a1 - b2/a2);
			coord[0] = (c1-b1*coord[1])/a1;
			return(coord);
		}

		return(null); //NaN??
	}

	public Well getWell(int pos) {
		if(pos<0 || pos>=wells.size()) return(null);
		return(wells.get(pos));
	}
	public Well getWell(int row,int col) {
		//returns well at given row and col
		int pos = col + row*getNCOLS();
		return(getWell(pos));
	}

	public Well contains(int x,int y) {
		double[] ij = getCoordinates(x,y);

		int i = (int)(ij[0]+0.5);
		int j = (int)(ij[1]+0.5);

		//parcours plaque en ligne puis colonne
		for(int k=i-1;k<=i+1;k++) {
			for(int l=j-1;l<=j+1;l++) {
				Well w = getWell(l,k);
				if(w!=null) {
					if(w.contains(x,y)) {
						return(w);
					}
				}
			}
		}
		return null;
	}

	public void readConfigDetectCFU () {
		String configFile = settings.getPath()+"detectCFU.cfg";
		readConfigDetectCFU (configFile);
	}

	public void readConfigDetectCFU (String configFile) {
		System.out.println("Reading "+configFile);
		try {
			FileInputStream fis = new FileInputStream(configFile);
			Scanner scanner = new Scanner(fis);
			ArrayList<String> content = new ArrayList<String>();

			//reading file line by line using Scanner in Java
			while(scanner.hasNextLine()){
				content.add(scanner.nextLine());
			}    
			scanner.close();

			this.lightBackground = (content.get(0).split(";")[1]).equals("true");
			this.sizeMin = Integer.parseInt(content.get(1).split(";")[1]);
			this.circMin = Double.parseDouble(content.get(2).split(";")[1]);
			this.sensitivityInf = Double.parseDouble(content.get(3).split(";")[1]);
			this.sensitivitySup[0] = Double.parseDouble(content.get(4).split(";")[1]);
			this.sensitivitySup[1] = Double.parseDouble(content.get(5).split(";")[1]);
			this.sensitivitySup[2] = Double.parseDouble(content.get(6).split(";")[1]);

			// light backgroud: true
			// min size: 100
			// min cicularity: 0.2
			// lower sensitivity: 0.05
			// upper sensitivity from: 0.1
			// to: 0.5
			// by: 0.1
		}
		catch(FileNotFoundException e) {
			System.out.println("Error while reading "+configFile);
		}	

	}

	public void writeConfigDetectCFU() {
		try{
			PrintWriter writer = new PrintWriter(settings.getPath()+"detectCFU.cfg", "UTF-8");
			//image file, settings.getName()

			writer.println("light background;"+lightBackground);
			writer.println("minimum CFU size;"+sizeMin);
			writer.println("minimum circularity;"+circMin);
			writer.println("lower sensitivity;"+sensitivityInf);
			writer.println("upper sensitivity (first value);"+sensitivitySup[0]);
			writer.println("upper sensitivity (last value);"+sensitivitySup[1]);
			writer.println("upper sensitivity (increment);"+sensitivitySup[2]);

			writer.close();
		}
		catch(IOException se){
		}
	}

	public boolean askConfigDetectCFU () {

		//automatic detection of CFU
		GenericDialog dlgAutoDetect = new GenericDialog("New plate - Auto detection of CFU");
		//if(values[i]!="") z = Integer.parseInt(values[i]);
		dlgAutoDetect.addCheckbox("Light background",lightBackground);
		dlgAutoDetect.addNumericField("minimum CFU size",sizeMin,0);
		dlgAutoDetect.addNumericField("minimum CFU circularity",circMin,2);
		dlgAutoDetect.addNumericField("sensitivity inf",sensitivityInf,2);
		dlgAutoDetect.addNumericField("sensitivity sup (first value)",sensitivitySup[0],2);
		dlgAutoDetect.addNumericField("sensitivity sup (last value)",sensitivitySup[1],2);
		dlgAutoDetect.addNumericField("sensitivity sup (by step)",sensitivitySup[2],2);

		dlgAutoDetect.addMessage("Press cancel if you don't want to run auto-detection of CFU.");
		dlgAutoDetect.showDialog();

		if(dlgAutoDetect.wasCanceled()) return(false);

		lightBackground = dlgAutoDetect.getNextBoolean();
		sizeMin = (int) dlgAutoDetect.getNextNumber();
		circMin = dlgAutoDetect.getNextNumber();
		sensitivityInf = dlgAutoDetect.getNextNumber();
		sensitivitySup[0] = dlgAutoDetect.getNextNumber();
		sensitivitySup[1] = dlgAutoDetect.getNextNumber();
		sensitivitySup[2] = dlgAutoDetect.getNextNumber();

		return(true);
	}
	public void detectCFU(ImagePlus imp,double sensitivityInf,double[] sensitivitySup,boolean lightBackground,int sizeMin,double circMin) {
		this.sensitivityInf = sensitivityInf;
		this.sensitivitySup = sensitivitySup;
		this.lightBackground = lightBackground;
		this.sizeMin = sizeMin;
		this.circMin = circMin;

		detectCFU(imp);
	}

	public void detectCFU(ImagePlus imp) {
		imp.setHideOverlay(true);	
		//autoDetectCFU ad = new autoDetectCFU(null,this,null);
	}

	public void exportCounts(PrintWriter writer,boolean headers) {
		//col names
		if(headers) {
			settings.writeCountsFileHeaders(writer);
		}

		//data
		for(int i=0;i<wells.size();i++) {
			Well w = wells.get(i);
			w.exportCounts(writer);
		}
	}

	public void exportCounts() {
		String out = settings.getPath()+"../COUNTS_"+settings.getName()+".csv";
		try {
			PrintWriter writer = new PrintWriter(out, "UTF-8");
			exportCounts(writer,true);
			writer.close();
		}
		catch(IOException e) {
			JOptionPane.showMessageDialog(null,"Failed to write CFU counts in "+out+"!", "Write CFU counts for plate "+settings.getName(), JOptionPane.ERROR_MESSAGE);
			System.out.println("Failed to write CFU counts in "+out);
		}
	}
}
