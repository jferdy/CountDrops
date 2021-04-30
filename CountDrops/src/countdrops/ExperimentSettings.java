package countdrops;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;


public class ExperimentSettings {
	//location of config file
	private String path;  
	private String fileName;	
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
	
	public ExperimentSettings() {
		// TODO Auto-generated constructor stub
		NROWS = 8;
		NCOLS = 12;
		NFIELDS = 1;
		NCFUTYPES = 1;
		
		FieldsType.add("string");
		FieldsName.add("Name");
		FieldsValue.add("plate 1");
		FieldsDescription.add("Name of the plate");
		
		CFUType.add("Bacteria");
		CFUColor.add("-65536"); //this is red!
		//CFUColor.add("java.awt.Color[r=255,g=0,b=0]");
		CFUKey.add("b");
		CFUDescription.add("A regular bacteria");
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

	public ArrayList<String> getFieldsName() {
		return FieldsName;
	}

	public void setFieldsName(ArrayList<String> fieldsName) {
		FieldsName = fieldsName;
	}

	public ArrayList<String> getFieldsValue() {
		return FieldsValue;
	}

	public void setFieldsValue(ArrayList<String> fieldsValue) {
		FieldsValue = fieldsValue;
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

	public ArrayList<String> getCFUDescription() {
		return CFUDescription;
	}

	public void setCFUDescription(ArrayList<String> cFUDescription) {
		CFUDescription = cFUDescription;
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

	public void setCFUType(ArrayList<String> cFUType) {
		CFUType = cFUType;
	}

	public ArrayList<String> getCFUColor() {
		return CFUColor;
	}

	public void setCFUColor(ArrayList<String> cFUColor) {
		CFUColor = cFUColor;
	}

	public ArrayList<String> getCFUKey() {
		return CFUKey;
	}

	public void setCFUKey(ArrayList<String> cFUKey) {
		CFUKey = cFUKey;
	}

	public void read() throws Exception, IOException {
		System.out.println("Reading "+fileName);
		try {
			FileInputStream fis = new FileInputStream(path+fileName);
			Scanner scanner = new Scanner(fis);
			ArrayList<String> content = new ArrayList<String>();

			//reading file line by line using Scanner in Java
			while(scanner.hasNextLine()){
				content.add(scanner.nextLine());
			}    
			scanner.close();

			ArrayList<String> tags = new ArrayList<String>();
			tags.add("NROWS");
			tags.add("NCOLS");
			tags.add("NFIELDS");
			tags.add("NCFUTYPES");
			
			String tag = "";
			int pos = -1;
			for(int line=0;line<content.size();line++) {
				//split line content using ; as a separator
				String[] cells = content.get(line).split(";");				
				if(cells.length>0) {					
					cells[0].replaceAll("[ \t]","");
					//does first cell corresponds to a tag??
					if(tags.contains(cells[0])) tag = cells[0];										 					
					
					if(tag.equals("NROWS")) NROWS = Integer.parseInt(cells[1]);											
					if(tag.equals("NCOLS")) NCOLS   = Integer.parseInt(cells[1]);											
					if(tag.equals("NFIELDS")) {
						if(NFIELDS<0) {
							NFIELDS   = Integer.parseInt(cells[1]);									
						} else {
							//lines that follow the tag NFIELDS contain fields description
							if(cells.length<4) {
								Exception e = new IOException("Cannot read Fields settings (line "+FieldsType.size()+")");
								throw e;
							}
							FieldsType.add(cells[0]);
							FieldsName.add(cells[1]);
							FieldsValue.add(cells[2]);
							FieldsDescription.add(cells[3]);
						}
					}
					if(tag.equals("NCFUTYPES")) {
						if(NCFUTYPES<0) {
							NCFUTYPES   = Integer.parseInt(cells[1]);
						} else {
							//lines that follow the tag NCFUTYPES contain CFU types description
							if(cells.length<4) {
								Exception e = new IOException("Cannot read CFUType settings (line "+CFUType.size()+")");
								throw e;
							}
							
							CFUType.add(cells[0]);
							CFUColor.add(cells[1]);
							CFUKey.add(cells[2]);
							CFUDescription.add(cells[3]);
						}
					}
				}
			}
		} catch(FileNotFoundException e) {
			System.out.println("Error while reading "+fileName);
		}
	}
	
	public void write() {
		
	}
}
