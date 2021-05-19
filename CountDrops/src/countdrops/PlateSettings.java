package countdrops;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JOptionPane;


public class PlateSettings {
	private String path = null;

	//names
	private  String fileName = null;  //name of the config file
	private  String image = null;     //image name (to be read in the config file)
	private  String name  = null;     //plate name (to be read in the config file)		  
	

	//content of config file
	private int NROWS = -1;
	private int NCOLS = -1;

	private int NFIELDS = -1;    
	private ArrayList<String> FieldsType = new ArrayList<String>();
	private ArrayList<String> FieldsName =  new ArrayList<String>();
	private ArrayList<String> FieldsValue =  new ArrayList<String>();
	private ArrayList<String> FieldsDescription =  new ArrayList<String>();

	private int NCFUTYPES = -1;
	private ArrayList<String> CFUType = new ArrayList<String>();
	private ArrayList<String> CFUColor = new ArrayList<String>();
	private ArrayList<String> CFUKey = new ArrayList<String>();
	private ArrayList<String> CFUDescription = new ArrayList<String>();

	private  double   volume = -1.0;           //volume in each well
	private  int      dilutionScheme = -1;     //1: fixed; 2: by row; 3: by column; 4: by well
	private  double[] dilution = null;         //dilution values

	//plate bounding box
	private int[] boxX = null;
	private int[] boxY = null;


	//problems
	private boolean problems = false;
	
	public PlateSettings() {		
	}

	public PlateSettings(String xpath,String xfileName) throws Exception, IOException {
		path  =xpath;
		fileName = xfileName;
		read();
	}
	public PlateSettings(PlateSettings s) {
		NROWS = s.getNROWS();
		NCOLS = s.getNCOLS();

		NFIELDS = s.getNFIELDS();    
		FieldsType.clear();  
		FieldsName.clear(); 
		FieldsValue.clear();
		FieldsDescription.clear(); 
		for(int i=0;i<NFIELDS;i++) {
			FieldsType.add(s.getFieldsType().get(i));  
			FieldsName.add(s.getFieldsName().get(i)); 
			FieldsValue.add(s.getFieldsValue().get(i));
			FieldsDescription.add(s.getFieldsDescription().get(i)); 			
		}

		NCFUTYPES = s.getNCFUTYPES();
		CFUType.clear();
		CFUColor.clear();
		CFUKey.clear();
		CFUDescription.clear();
		for(int i=0;i<NCFUTYPES;i++) {
			CFUType.add(s.getCFUType().get(i));
			CFUColor.add(s.getCFUColor().get(i));
			CFUKey.add(s.getCFUKey().get(i));
			CFUDescription.add(s.getCFUDescription().get(i));			
		}

		volume = s.getVolume();        //volume in each well
		dilutionScheme = s.getDilutionScheme(); //1: fixed; 2: by row; 3: by column; 4: by well
		dilution = new double[s.getDilution().length];
		for(int i=0;i<dilution.length;i++) {
			dilution[i] = s.getDilution()[i];
		}
	}

	public void setDefault()  {
		NROWS = 8;
		NCOLS = 12;

		NFIELDS = 2; 
		FieldsType.clear();
		FieldsName.clear(); 
		FieldsValue.clear(); 
		FieldsDescription.clear();
		
		FieldsType.add("integer");
		FieldsName.add("Plate");
		FieldsValue.add("53");
		FieldsDescription.add("Name of the plate which contains the biological samples");

		FieldsType.add("integer");
		FieldsName.add("Replicate");
		FieldsValue.add("1");
		FieldsDescription.add("Plating replicate");

		NCFUTYPES = 2;
		CFUType.clear(); 
		CFUColor.clear();
		CFUKey.clear();
		CFUDescription.clear();
		
		CFUType.add("Bacteria");
		CFUColor.add("#FF0000");
		CFUKey.add("b");
		CFUDescription.add("A bacteria you want to count");
		
		CFUType.add("Contaminant");
		CFUColor.add("#0000BB"); //this is not red!
		CFUKey.add("c");
		CFUDescription.add("A contaminant you'd prefer not to count");

		volume = 5.0;        //volume in each well
		dilutionScheme = 1; //1: fixed; 2: by row; 3: by column; 4: by well
		dilution = new double[1];
		dilution[0] = 1.0;

	}

	public void setProblems(boolean b) {problems = b;}
	public boolean hasProblems() {return problems;}
	
	public int getNROWS() {
		return NROWS;
	}

	public static String getRowLetterFromInt(int i) {
		char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
		if (i > 25) {
			int k= i/26;
			int l= i - k*25;
			String res = getRowLetterFromInt(k)+getRowLetterFromInt(l);	    
			return res;
		}
		return Character.toString(alphabet[i]);
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

	public double getVolume(){
		return volume;
	}

	public void setVolume(double x) {
		if(x<0) return;
		volume  = x;
	}
	public int getDilutionScheme() {
		return dilutionScheme;
	}

	public void setDilutionScheme(int i) {
		if(i < 1 || i > 4) return;
		dilutionScheme = i;
	}

	public double[] getDilution() {
		return dilution;
	}

	public void setDilution(double[] dil) {
		if(dil==null) return;

		dilution = new double[dil.length];
		for(int i=0;i<dil.length;i++) dilution[i] = dil[i];		
	}

	public int getNFIELDS() {
		return NFIELDS;
	}

	public void setNFIELDS(int nFIELDS) {
		NFIELDS = nFIELDS;
	}

	public ArrayList<String> getFieldsType() {
		return FieldsType;
	}

	public void setFieldsType(ArrayList<String> fieldsType) {
		FieldsType = fieldsType;
	}

	public void setFieldsType(String[] fieldsType) {
		FieldsType.clear();
		for(int i=0;i<fieldsType.length;i++) FieldsType.add(fieldsType[i]);		
	}

	public void setFieldsType(int index, String value) {
		if(index<0 || index>=FieldsName.size()) return;
		FieldsType.set(index,value);		
	}

	public ArrayList<String> getFieldsName() {
		return FieldsName;
	}

	public void setFieldsName(ArrayList<String> fieldsName) {
		FieldsName = fieldsName;
	}

	public void setFieldsName(String[] fieldsName) {
		FieldsName.clear();
		for(int i=0;i<fieldsName.length;i++) FieldsName.add(fieldsName[i]);		
	}

	public void setFieldsName(int index, String value) {
		if(index<0 || index>=FieldsName.size()) return;
		FieldsName.set(index,value);		
	}

	public ArrayList<String> getFieldsValue() {
		return FieldsValue;
	}

	public void setFieldsValue(ArrayList<String> fieldsValue) {
		FieldsValue = fieldsValue;
	}

	public void setFieldsValue(String[] fieldsValue) {
		FieldsValue.clear();
		for(int i=0;i<fieldsValue.length;i++) FieldsValue.add(fieldsValue[i]);		
	}

	public void setFieldsValue(int index, String value) {
		if(index<0 || index>=FieldsValue.size()) return;
		FieldsValue.set(index,value);		
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}


	public ArrayList<String> getFieldsDescription() {
		return FieldsDescription;
	}

	public void setFieldsDescription(ArrayList<String> fieldsDescription) {
		FieldsDescription = fieldsDescription;
	}

	public void setFieldsDescription(String[] fieldsDescription) {
		FieldsDescription.clear(); 
		for(int i=0;i<fieldsDescription.length;i++) FieldsDescription.add(fieldsDescription[i]);
	}

	public ArrayList<String> getCFUDescription() {
		return CFUDescription;
	}
	
	public String getCFUDescription(int i) {
		if(i<0 || i>=CFUDescription.size()) return null;
		return CFUDescription.get(i);
	}
	
	public void setCFUDescription(ArrayList<String> cFUDescription) {
		CFUDescription = cFUDescription;
	}

	public void setCFUDescription(String[] cFUDescription) {
		CFUDescription.clear(); 
		for(int i=0;i<cFUDescription.length;i++) CFUDescription.add(cFUDescription[i]);
	}

	public int getNCFUTYPES() {
		return NCFUTYPES;
	}

	public void setNCFUTYPES(int nCFUTYPES) {
		NCFUTYPES = nCFUTYPES;
	}

	public ArrayList<String> getCFUType() {
		return CFUType;
	}
	
	public String getCFUType(int i) {
		if(i<0 || i>=CFUColor.size()) return null;
		return CFUType.get(i);
	}
	
	public void setCFUType(ArrayList<String> cFUType) {
		CFUType = cFUType;
	}

	public void setCFUType(String[] cFUType) {
		CFUType.clear(); 
		for(int i=0;i<cFUType.length;i++) CFUType.add(cFUType[i]);
	}

	public ArrayList<String> getCFUColor() {
		return CFUColor;
	}

	public String getCFUColor(int i) {
		if(i<0 || i>=CFUColor.size()) return null;
		return CFUColor.get(i);
	}

	public String getCFUColor(String type) {
		for(int i=0;i<CFUColor.size();i++) {
			if(type.equals(CFUType.get(i))) {
				return(CFUColor.get(i));
			}
		}
		return "#FFFFFF";		
	}

	public void setCFUColor(ArrayList<String> cFUColor) {
		CFUColor = cFUColor;
	}

	public void setCFUColor(String[] cFUColor) {
		CFUColor.clear(); 
		for(int i=0;i<cFUColor.length;i++) CFUColor.add(cFUColor[i]);
	}

	public ArrayList<String> getCFUKey() {
		return CFUKey;
	}

	public String getCFUKey(int i) {
		if(i<0 || i>=CFUKey.size()) return null;
		return CFUKey.get(i);
	}
	
	public void setCFUKey(ArrayList<String> cFUKey) {
		CFUKey = cFUKey;
	}

	public void setCFUKey(String[] cFUKey) {
		CFUKey.clear(); 
		for(int i=0;i<cFUKey.length;i++) CFUKey.add(cFUKey[i]);
	}

	public void setCFUKey(char[] cFUKey) {
		CFUKey.clear(); 
		for(int i=0;i<cFUKey.length;i++) CFUKey.add(""+cFUKey[i]);
	}

	public void setPathAndNameFromFields() {
		//Careful here: path must be set before name, because getPathFromFields needs old name to produce new path!!
		path = getPathFromFields(FieldsValue);
		name = getNameFromFields(FieldsValue);
	}

	public String getNameFromFields(ArrayList<String> fields) {
		String n = fields.get(0);
		for(int i=1;i<fields.size();i++) {
					n += "_" + fields.get(i);	    
		}
		return n;
	}
	
	public String getPathFromFields(ArrayList<String> fields) {
		//does path contain plate name?
		String p = getPath();
		if(p==null) return null;
		
		if(name!=null && p.contains(name)) {
			int pos = p.substring(0,p.length()-2).lastIndexOf(File.separator);   
			p = p.substring(0,pos);  //one directory up : remove the old name of the plate from path
			p += File.separator;
		}

		//plate name
		String n = getNameFromFields(fields);
				
		//change path accordingly
		if(p!=null && n!=null && !p.contains(n)) {
			p =  p+n+File.separator;
			return p;
		}
		return null;
	}

	public boolean isCompatible(PlateSettings s) {
		//Check that PlateSettings s is compatible with instance of PlateSettings
		//Compatibility implies that plates with either of the two settings could be written in the same result file.
		//More precisely :
		//  * CFU types must be identical
		//  * Fields must be the same
		if(s.getNFIELDS()!=NFIELDS) return false;
		if(s.getNCFUTYPES()!=NCFUTYPES) return false;
		for(int i=0;i<NFIELDS;i++) if(!s.getFieldsName().get(i).equals(FieldsName.get(i))) return(false);
		for(int i=0;i<NCFUTYPES;i++) if(!s.getCFUType().get(i).equals(CFUType.get(i))) return(false);
		return true;
	}
	
	public void copyCFUColor(PlateSettings s) {
		if(s.getNCFUTYPES()!=NCFUTYPES) return;
		for(int i=0;i<NCFUTYPES;i++) {
			CFUColor.set(i,s.getCFUColor(i));
		}
	}
	
	public void copyCFUKey(PlateSettings s) {
		if(s.getNCFUTYPES()!=NCFUTYPES) return;
		for(int i=0;i<NCFUTYPES;i++) {
			CFUKey.set(i,s.getCFUKey(i));
		}
	}
	
	public static Color convertStringToColor(String str) {
		Color color;
		try {
			//try to parse string as an integer
			color = new Color(Integer.parseInt(str,10));
			return color;
		} catch(Exception e) {}	
		try {
			//try to parse string as a color name
			color = (Color) Color.class.getField(str).get(null);
			return color;
		} catch(Exception e) {}
		try {
			//try to parse string as an hexadecimal code
			color = new Color(Integer.parseInt(str.substring(1,str.length()),16));
			return color;
		} catch(Exception e) {
			System.out.println("The string "+str+" cannot be converted to Color!");
			color = Color.gray; // the color is not defined: default to gray!
		}
		
		return color;
	}

	public static String  convertColorToHex(Color color) {
		//code from https://stackoverflow.com/questions/17183587/convert-integer-color-value-to-rgb
		String hexColor = String.format("#%06X", (0xFFFFFF & color.getRGB()));
		return(hexColor);
	}

	public boolean cfuTypeExists(String type) {
		if(type==null) return false;
		if(type.equals("NA")) return true;
		//System.out.println(type);
		for(int i=0;i<CFUType.size();i++) {
			if(type.equalsIgnoreCase(CFUType.get(i))) return true;
		}
		return false;	
	}
	
	public void read() throws Exception, IOException {
		if(path==null || fileName==null) throw new Exception("No path or file name provided to read settings!");
		problems = false;
		System.out.print("Reading "+fileName+"... ");
		String tag = "";
		int line = 0;
		try {
			FileInputStream fis = new FileInputStream(path+fileName);
			Scanner scanner = new Scanner(fis);
			ArrayList<String> content = new ArrayList<String>();

			//reading file line by line using Scanner in Java
			while(scanner.hasNextLine()){
				content.add(scanner.nextLine());
			}    
			scanner.close();

			//tags
			ArrayList<String> tags = new ArrayList<String>();
			tags.add("NAME");  //plate name
			tags.add("IMAGE"); //image name
			tags.add("NROWS");
			tags.add("NCOLS");
			tags.add("NFIELDS");
			tags.add("NCFUTYPES");
			tags.add("VOLUME");
			tags.add("VOLUME PER WELL");
			tags.add("DILUTION SCHEME");
			tags.add("COORDINATES");
			
			//stop tags -- The tag IMAGE is used when reading experiment settings, where config file starts with
			//plate settings and ends with a list of the images that are part of the project.
			ArrayList<String> stopTags = new ArrayList<String>();
			stopTags.add("IMAGES");
			stopTags.add("END");
			stopTags.add("SAMPLE ID");
			
			NFIELDS=-1;
			NCFUTYPES=-1;
			dilutionScheme=-1;

			//int pos = -1;
			for(line = 0;line<content.size();line++) {				
				//split line content using ; as a separator
				String[] cells = content.get(line).split(";");	
				cells[0] = cells[0].trim();
				//System.out.println("["+cells[0]+"]");
				if(cells[0].length()>0) {									
					//does first cell corresponds to a stop tag??
					if(stopTags.contains(cells[0].toUpperCase())) return;
					
					//does first cell corresponds to a tag??
					if(tags.contains(cells[0].toUpperCase())) tag = cells[0].toUpperCase();										 					
					//System.out.println(""+line+" : "+tag+" ("+content.get(line)+")");

					if(tag.equals("NAME"))  name  = cells[1];
					if(tag.equals("IMAGE")) image = cells[1];
					if(tag.equals("NROWS")) NROWS = Integer.parseInt(cells[1]);											
					if(tag.equals("NCOLS")) NCOLS   = Integer.parseInt(cells[1]);											
					if(tag.equals("NFIELDS")) {
						if(NFIELDS<0) {
							NFIELDS   = Integer.parseInt(cells[1]);
							//empty array so that old fields are not kept
							FieldsType.clear();
							FieldsName.clear();
							FieldsValue.clear();
							FieldsDescription.clear();

						} else {							
							//lines that follow the tag NFIELDS contain fields description
							if(cells[0].equalsIgnoreCase("INT")) {
								FieldsType.add("INTEGER"); //backward compatibility!! INT -> INTEGER
							} else {
								FieldsType.add(cells[0]);
							}
							FieldsName.add(cells[1]);
							
							if(cells.length>3) {
								FieldsDescription.add(cells[3]);							
							} else {
								FieldsDescription.add("?");
							}
							
							//convert value into specified type
							String  type  = cells[0].toUpperCase(); 
							String  value = cells[2];
							boolean testValue = false;
							
							if(!testValue && type.equals("INTEGER")) {
								try {
									//tries converting value into integer
									//int test = Integer.parseInt(value);
									FieldsValue.add(value);
									testValue = true;								
								} catch(Exception e) {
									FieldsValue.add("-1");																	
								}
							}
							if(!testValue && type.equals("FLOAT")) {
								try {
									//tries converting value into double
									//double test = Double.parseDouble(value);
									FieldsValue.add(value);
									testValue = true;								
								} catch(Exception e) {
									FieldsValue.add("-1.0");																	
								}
							}
							if(!testValue && type.equals("BOOLEAN")) {
								if(value.equalsIgnoreCase("TRUE") || value.equalsIgnoreCase("FALSE")) {
									testValue = true;
								}
								if(value.equalsIgnoreCase("TRUE") || value.equalsIgnoreCase("1")) {
									FieldsValue.add("true");
								} else {
									FieldsValue.add("false");
								}								
							}
							if(!testValue && type.equals("STRING")) {
								FieldsValue.add(value);
								testValue = true;
							}
							if(!testValue) {
								//conversion of value in specified type has failed!
								problems = true;
								JOptionPane.showMessageDialog(null,"Problem while reading settings in "+path+fileName+"\nField '"+cells[1]+"' supposed to be "+type+" but '"+value+"' has been found. You should carrefully inspect plate settings!", "Reading plate settings", JOptionPane.WARNING_MESSAGE);
								System.out.println("Error while reading "+fileName);								
							}
						}
					}
					if(tag.equals("NCFUTYPES")) {
						if(NCFUTYPES<0) {
							NCFUTYPES   = Integer.parseInt(cells[1]);
							//empty array so that old types are not kept
							CFUType.clear();
							CFUColor.clear();
							CFUKey.clear();
							CFUDescription.clear();
						} else {
							//lines that follow the tag NCFUTYPES contain CFU types description
							if(CFUType.size()>=NCFUTYPES && cells.length==2) {
								//assumes that settings are under old format (i.e.) without COORDINATES tag
								//this line is therefore the first of plate coordinates
								tag="COORDINATES";
								line--; //this is because of line++ below

							} else {
								CFUType.add(cells[0]);
								CFUColor.add(cells[1]);
								CFUKey.add(cells[2]);
								if(cells.length>3) {
									CFUDescription.add(cells[3]);
								} else {
									CFUDescription.add("?");
								}
							}
							
						}
					}
					if(tag.equals("VOLUME") || tag.equals("VOLUME PER WELL")) {
						volume = Double.parseDouble(cells[1]);
					}
					if(tag.equals("DILUTION SCHEME")) {
						if(dilutionScheme<0) {
							dilutionScheme = (int) Integer.parseInt(cells[1]);
						} else {
							dilution = new double[cells.length];
							for(int i=0;i<cells.length;i++) dilution[i] = Double.parseDouble(cells[i]);
						}
					}
					if(tag.equals("COORDINATES")) {						
						boxX = new int[4];
						boxY = new int[4];	
						//reads the four lines that follow the coordinate tag
						line++;
						for(int j=0;j<4;j++) {
							cells = content.get(line).split(";");
							//System.out.println(cells[0]+";"+cells[1]);
							boxX[j]=Integer.parseInt(cells[0]);
							boxY[j]=Integer.parseInt(cells[1]);
							line++;
						}
					}
				}
			}
			System.out.println("done.\n");
		} catch(Exception e) {
			JOptionPane.showMessageDialog(null,"Failed to read settings in "+path+fileName+"\nFile seems to be badly configured (line "+(line+1)+", tag "+tag+").", "Reading plate settings", JOptionPane.ERROR_MESSAGE);
			System.out.println("Error while reading "+fileName+"\n");
			throw new Exception("Error while reading "+fileName);			
		}
	}

	public boolean save() {
		if(fileName==null || path==null) return false;
		problems = false;
		
		PrintWriter writer;
		try{
			writer = new PrintWriter(path+fileName, "UTF-8");

			if(image!=null) writer.println("IMAGE;"+image); //image name: don't write if null
			if(name!=null) writer.println("NAME;"+name);    //plate name: idem

			//plate dimension
			writer.println("NROWS;"+NROWS);
			writer.println("NCOLS;"+NCOLS);

			//Fields
			writer.println("NFIELDS;"+NFIELDS);
			for(int i=0;i<NFIELDS;i++) {
				//check that value is valid
				
				if(FieldsType.get(i).equals("INTEGER")) {
					try {
						//tries converting value into integer
						Integer.parseInt(FieldsValue.get(i));
					} catch(Exception e) {
						problems = true;																	
					}
				}
				if(FieldsType.get(i).equals("FLOAT")) {
					try {
						//tries converting value into double
						Double.parseDouble(FieldsValue.get(i));
					} catch(Exception e) {
						problems = true;					
						}
				}
				if(FieldsType.get(i).equals("BOOLEAN")) {
					if(!FieldsValue.get(i).equalsIgnoreCase("TRUE") && !FieldsValue.get(i).equalsIgnoreCase("FALSE")) {
						problems = true;
					}
				}

				writer.println(FieldsType.get(i)+";"+FieldsName.get(i)+";"+FieldsValue.get(i)+";"+FieldsDescription.get(i));
			}

			//dilution et volume
			writer.println("VOLUME;"+volume);
			writer.println("DILUTION SCHEME;"+dilutionScheme);
			writer.print(dilution[0]);
			for(int i=1;i<dilution.length;i++) writer.print(";"+dilution[i]);
			writer.println("");

			//CFU types
			writer.println("NCFUTYPES;"+NCFUTYPES);
			for(int i=0;i<NCFUTYPES;i++) {
				writer.println(CFUType.get(i)+";"+CFUColor.get(i)+";"+CFUKey.get(i)+";"+CFUDescription.get(i));
			}

			//bounding box: don't write if null
			if(boxX!=null & boxY!=null) {
				writer.println("COORDINATES");
				for(int j=0;j<4;j++) {
					writer.println(boxX[j]+";"+boxY[j]);
				}
			}

			writer.close();
			return true;
			//JOptionPane jop = new JOptionPane();
			//jop.showMessageDialog(null,"Settings written in "+path+fileName, "Saving plate settings", JOptionPane.INFORMATION_MESSAGE);			
		}
		catch(IOException se){
			problems = true;
			JOptionPane.showMessageDialog(null,"Failed to save settings in "+path+fileName, "Saving plate settings", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	public void addField(String Name,String type,String value,String description) {
		FieldsName.add(name);
		FieldsType.add(type);
		FieldsValue.add(value);
		FieldsDescription.add(description);
		save();
	}
	
	public void removeField(String name) {
		for(int i=0;i<FieldsName.size();i++) {
			if(name.equals(FieldsName.get(i))) {
				FieldsName.remove(i);
			}
		}
		save();
	}
	
	public void writeCountsFileHeaders(PrintWriter writer) {
		//col names		
		for(int j=0;j<getNFIELDS();j++) writer.print(getFieldsName().get(j)+";");
		writer.print("WELL;Volume;Dilution");
		//counts for each CFU type
		for(int j=0;j<getNCFUTYPES();j++) writer.print(";"+getCFUType(j));
		//counts for cfu with no time
		writer.println(";NA");		
	}
	
	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int[] getBoxX() {
		return boxX;
	}

	public int getBoxX(int i) {
		if(i<0 || i>3) return -1;
		return boxX[i];
	}

	public void setBoxX(int i, int x) {
		if(i<0 || i>3) return;
		boxX[i] = x;
	}

	public int[] getBoxY() {
		return boxY;
	}

	public int getBoxY(int i) {
		if(i<0 || i>3) return -1;
		return boxY[i];
	}

	public void setBoxY(int i, int y) {
		if(i<0 || i>3) return;
		boxY[i] = y;
	}

	public void setBox(int x[], int y[]) {
		if(x.length<3 || y.length<3) return;
		
		//set the first three positions and computes the position of the forth point
		boxX = new int[4];
		boxY = new int[4];
		for(int j=0;j<3;j++) {
			boxX[j]=x[j];
			boxY[j]=y[j];
		}

		int centerX = (x[0]+x[2])/2;
		int centerY = (y[0]+y[2])/2;
		boxX[3] = centerX+(centerX-x[1]);
		boxY[3] = centerY+(centerY-y[1]);
	}

}
