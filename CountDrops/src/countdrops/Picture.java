package countdrops;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import ij.ImagePlus;


public class Picture {
	private ArrayList<Plate> plates;
	private String path;
	private String fileName;

	public Picture(String xpath,String xfileName) throws Exception,IOException {
		path = xpath;
		fileName = xfileName;
		
		// test if image exists!
		File f = new File(path+fileName);
		if(!f.exists()) {
			throw new IOException("Cannot find image "+path+fileName+"!");
		}
		
		plates = new ArrayList<Plate>();
		//Exception ex = null;
		try {
			loadPlates();
		} catch(Exception e) {
			// If n exception is thrown from here construction is interrupted and the picture is eligible for garbage collection...
			// This is not what we want! But throwing an exception would be a way to signal loading issues to the calling class.
			System.out.println(e);			
		}
		
	}

	public String getFileName() {return(fileName);}
	public String getPath() {return(path);}
	public int getNbPlates() {if(plates==null) return 0; return plates.size();}

	public ImagePlus view() {
		try {
			ImagePlus img = new ImagePlus(path+"/"+fileName);
			img.setTitle(fileName);			
			img.show();
			
			return img;
		} catch(Exception e) {

		}
		return null;
	}

	public void loadPlates() throws Exception {
		if(path==null || fileName==null) {
			return;
		}

		String errMsg = "";
		int sep = fileName.lastIndexOf(".");
		System.out.println(fileName.substring(0,sep));
		//File folder = new File(path+"/"+fileName.substring(0,sep));
		File folder = new File(path+fileName.substring(0,sep));
		if(folder.exists()) {
			//list plate directories
			File[] listOfFiles = folder.listFiles();
			if(plates!=null) plates.clear();
			for(int i=0;i<listOfFiles.length;i++) {
				if(listOfFiles[i].isDirectory()) {
					System.out.println(listOfFiles[i].getPath());
					String p = listOfFiles[i].getPath()+File.separator+"config.cfg";
					try{
						Plate pl = new Plate(p);
						plates.add(pl);
					} catch(Exception ex) {
						if(errMsg.length()==0) errMsg+="Failed to load the following plates:";
						errMsg+="\n"+p;						
					}
				}
			}
			if(errMsg.length()>0) throw new Exception(errMsg);
		}
	}
	
	public Plate getPlate(int i) {
		if(i<0 || i>=plates.size()) return null;
		return plates.get(i);
	}
	
	public int getPlateIndex(Plate pl) {
		if(pl==null) return -1;
		for(int i=0;i<plates.size();i++) if(plates.get(i)==pl) return i;
		return -1;
	}
	
    private void deletePlateFiles(File element) {
	if (element.isDirectory()) {
	    for (File sub : element.listFiles()) {
	    	deletePlateFiles(sub);
	    }
	}
	element.delete();
    }

	public void deletePlate(int i) {
		if(i<0 || i>=plates.size()) return;
		Plate pl = plates.get(i);
		// issues a warning as destruction cannot be undone!
        int confirm = JOptionPane.showConfirmDialog (null, "Are you sure you want to delete plate "+pl.getName()+" ?","Warning",JOptionPane.YES_NO_OPTION);
        if(confirm == JOptionPane.YES_OPTION){         
    		// destroy the directories associated to the plate
        	File plateDir = new File(pl.getPath());
        	deletePlateFiles(plateDir);	
        	// remove plate from the array
        	plates.remove(i);		
        }
	}
	
	public void deletePlate(Plate pl) {
		deletePlate(getPlateIndex(pl));			
	}
	
	public void addPlate(Plate pl) {
		plates.add(pl);
	}
}
