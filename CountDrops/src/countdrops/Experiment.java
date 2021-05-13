package countdrops;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JOptionPane;

/*
 * One experiment should contain all the pictures that are necessary to compute bacterial loads
 * for a given set of samples. 
 * CFU types and Plate descriptive fields should probably be defined here, as they should be the same for all the plates of a single experiment
 * Plate description (number of rows, columns and dilution scheme) might differ among plates of a single experiment...
 */

public class Experiment {	
	//private String path,fileName;
	ArrayList<String[]> 		imagesPath = null;
	private ArrayList<Picture> 	pictures= null;
	private PlateSettings 		settings = null;
	private SampleID 			sampleID = null;
						
	public Experiment() {
		//empty experiment
		pictures = new ArrayList<Picture>();
		settings = new PlateSettings();
	}

	public Experiment(PlateSettings s,SampleID sid) {
		//empty experiment with settings
		pictures = new ArrayList<Picture>();
		settings = s;
		sampleID = sid;
	}

	public Experiment(String path,String fileName,boolean loadPictures) throws Exception {
		try {
			settings = new PlateSettings(path,fileName);
		} catch(Exception e) {
			throw e;
		}


		// create pictures list from file
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ";";
		ArrayList<String> tags = new ArrayList<String>();
		tags.add("IMAGES");
		tags.add("SAMPLE ID");

		boolean hasImage = false;
		boolean hasSampleID = false;
		ArrayList<String> fieldsForID = null;
		ArrayList<String> imagesFullName = null;		
		ArrayList<String> errMsg = new ArrayList<String>();
		
		try {
			br = new BufferedReader(new FileReader(path+fileName));
			
			while ((line = br.readLine()) != null) {
				String[] data = line.split(cvsSplitBy);
				if(data.length>0) {
					if(tags.contains(data[0])) {
						//line contains a tag
						if(data[0].equals("IMAGES")) {
							hasImage = true;						
							imagesFullName = new ArrayList<String>();						
							imagesPath = new ArrayList<String[]>();
						}
						if(data[0].equals("SAMPLE ID")) {
							hasSampleID = true;
							fieldsForID = new ArrayList<String>();
						}
					} else {
						//line contains no tag
						if(hasSampleID) {
							//sampleID is being read
							for(int i=0;i<data.length;i++) {
								//add to SampleID only if present in FieldsName (or equals to well) 
								if(settings.getFieldsName().contains(data[i]) || data[i].equals("WELL") || data[i].equals("ROW") || data[i].equals("COLUMN")) {
									fieldsForID.add(data[i]);
								}
							}
						}
						if(hasImage && data.length==2) {
							//Image is being read
							//change relative path to absolute path							
							String image_path = this.getPath()+data[0];
							if(imagesFullName.contains(image_path+data[1])) {
								//imagesFullName is used to check that the same picture is not added twice
								errMsg.add("Image "+data[0]+data[1]+" will not be added twice!");
								System.out.println("Image "+image_path+data[1]+" will not be added twice!");
							} else {
								//names are stored here for subsequent loading
								imagesFullName.add(data[0]+data[1]);
								imagesPath.add(new String[] {image_path,data[1]});
							}

						}	    			

						if(hasSampleID && fieldsForID.size()>0) {
							sampleID = new SampleID(fieldsForID,settings);
						} else {
							//set default sample ID to WELL
							sampleID = new SampleID(settings);
						}
					}
				}
			}
									

		} catch (Exception e) {			
			throw new IOException("Could not find "+fileName);
		} finally {
			if (br != null) {
				try {
					br.close();					
				} catch (IOException e) {
					e.printStackTrace();
					throw new IOException("Could not close "+fileName);
				}
			}			
		}
		
		if(imagesPath!=null && imagesPath.size()>0) {
			//sort path according to image name
			Collections.sort(imagesPath, new NameComparator());

			//config file has been fully parsed; images can now be loaded 
			//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			if(loadPictures) {
				//reads pictures if loading is asked for
				pictures = new ArrayList<Picture>();
				for(int i=0;i<imagesPath.size();i++) {        	
					String[] ip = imagesPath.get(i);
					try {
						errMsg.addAll(loadPicture(ip[0],ip[1]));
					} catch (Exception ignore) {}			          
				}	
			}
		}
		
		if(errMsg.size()>0) {				
			String txt = "CountDrops encountered problems while loading experiment!\n";
			for(int i=0;i<errMsg.size();i++) txt+=errMsg.get(i)+"\n";
			JOptionPane.showMessageDialog(CountDrops.getGui(),txt, "Reading experiment file", JOptionPane.WARNING_MESSAGE);

		}	
	}

	public ArrayList<String>  loadPicture(int i) {
		if(i<0 || i>=imagesPath.size()) return null;
		String[] ip = imagesPath.get(i);		
		return loadPicture(ip[0],ip[1]);
	}
	
	public ArrayList<String>  loadPicture(String path,String name) {
		ArrayList<String> errMsg = new ArrayList<String>();
		Picture p = null;
		try{			
			p = new Picture(path,name);
		} 
		catch(Exception ex) {
			//image not found							
			errMsg.add(ex.toString());
			System.out.println(ex);			
		}
		
		if(p!=null) {
			if(pictures==null) pictures = new ArrayList<Picture>();
			pictures.add(p);
			//check that Plates associated to current Picture have settings that are compatible to that of the experiment
			ArrayList<String> msg = new ArrayList<String>();
			for(int j=0;j<p.getNbPlates();j++) {
				Plate pl =p.getPlate(j);
				if(!settings.isCompatible(pl.getSettings())) {
					pl.setProblems(true);
					msg.add(pl.getName());					
				}
			}
			if(msg.size()>0) {
				//the Picture has some plates with incompatible settings...	    								
				errMsg.add("Some plates associated to the image "+p.getFileName()+" have settings that are not compatible with experiment settings:");
				for(int j=0;j<msg.size();j++) errMsg.add(msg.get(j));
				if(msg.size()>1) {
					errMsg.add("The plates have been added but you might encounter difficulties when exporting results.");
				} else {
					errMsg.add("The plate has been added but you might encounter difficulties when exporting results.");
				}
				System.out.println("Incompatible settings when importing image!");
			}
		}
		return errMsg;
	}
	
	public String getPath() {
		return settings.getPath();	
	}
	
	public int getNbImagesPath() {
		if(imagesPath==null) return(0);
		return (imagesPath.size());		
	}
	
	public int getNbPictures() {
		if(pictures==null && imagesPath==null) return(0);
		if(pictures==null) return (imagesPath.size());
		return(pictures.size());
	}
	
	public String[] getImagePath(int i) {
		if(imagesPath==null) return null;
		if(i<0 && i>= imagesPath.size()) return null;
		return imagesPath.get(i);
	}
	
	public Picture getPicture(int i) {
		if(pictures==null) return(null);
		if(i<0 || i>=pictures.size()) return(null);
		return(pictures.get(i));
	}
	public ArrayList<Picture> getPictures() {
		return pictures;
	}

	public void setPictures(ArrayList<Picture> pictures) {
		this.pictures = pictures;
	}

	public PlateSettings getSettings() {
		return settings;
	}

	public void setSettings(PlateSettings settings) {
		this.settings = settings;
	}

	public void setSampleID(SampleID s) {
		sampleID = s;
	}
	
	public SampleID getSampleID() {
		return sampleID;
	}
	public int getNbPlates() {
		if(pictures==null) return(0);
		int nb = 0;
		for(int i=0;i<this.getNbPictures();i++) nb+= this.getPicture(i).getNbPlates();
		return(nb);
	}
	
	public int getNbCFUs() {
		if(pictures==null) return(0);
		int nb = 0;
		for(int i=0;i<this.getNbPictures();i++) {
			Picture p = this.getPicture(i);
			for(int j=0;j<p.getNbPlates();j++) {
				Plate pl = p.getPlate(j);
				nb+= pl.getNbCFUs();
			}
		}
		return(nb);
	}
	
	public int[][] getCountsFromOtherPlates(Well w) {
		if(getNbPlates()<=0) return null;
		
		int ntype = this.getSettings().getNCFUTYPES()+1; //add one because NA is a type
		if(ntype<=0) return null;
				
		int[][] stat = null;
		int nbw = 0;
		
		for(int i=0;i<getNbPictures();i++) {
			Picture p = getPicture(i);
			for(int j=0;j<p.getNbPlates();j++){
				Plate pl = p.getPlate(j);
				
				
				Well w2 = pl.getWell(w.getRowInPlate(), w.getColInPlate());
				if(w2!=null &  ! w.getPath().equals(w2.getPath())) {
					if(w2.isEmpty() || w2.hasNonCountable() || w2.hasCFU()) {
						if(nbw<=0) {
							stat = new int[2][ntype];
						}
						nbw++;
						for(int k=0;k<ntype;k++) {			
							int n = w2.getNbCFU(k);
							if(n>-2) { // getNbCFU returns -2 if well is not empty but no CFU exist
								if(nbw==1) {
									stat[0][k]=n;
									stat[1][k]=n;
								} else {
									if((n!=-1 || stat[0][k]==-1) && stat[0][k]>n) stat[0][k]=n;
									if(n == -1) {
										stat[1][k]=-1;
									} else {
										if(stat[1][k]<n && stat[1][k]!=-1) stat[1][k]=n;
									}
								}
							}
						}
					}
				}
			}
		}	
		/*
		if(stat!=null) {
			for(int k=0;k<ntype;k++) {
				System.out.println(stat[0][k]+" "+stat[1][k]);
			}
		}
		*/
		
		return stat;		
	}
	
	public SampleStatistics getStatistics(Well w) {
		if(getNbPlates()<=0) return null;
		
		int ntype = this.getSettings().getNCFUTYPES()+1; //add one because NA is a type
		if(ntype<=0) return null;
		SampleStatistics stat = new SampleStatistics(w,getSampleID(),getSettings());
		
		String id = sampleID.getSampleID(w);
		int nbw = 0;		
		for(int i=0;i<getNbPictures();i++) {
			Picture p = getPicture(i);
			for(int j=0;j<p.getNbPlates();j++){
				Plate pl = p.getPlate(j);
				for(int l=0;l<pl.getNbWell();l++){
					Well w2 = pl.getWell(l);
					if(w2!=null & id.equals(sampleID.getSampleID(w2))) {
						stat.addCounts(w2);
						if(w.getPath().equals(w2.getPath())) stat.setPosOfCurrentWell(nbw);
						nbw++;
					}
				}
			}
		}	
		stat.updateUniqueDilutionValues();
		return stat;		
	}
	
	void addPicture(Picture p) {		
		if(p!=null) {
			if(pictures==null) pictures = new ArrayList<Picture>();
			pictures.add(p);
		}
	}
	
	public void deletePicture(int i) {
		if(pictures==null) return;
		if(i<0 || i>=pictures.size()) return;
		pictures.remove(i);
	}
	
	public boolean save() {
		if(settings==null) return false;
		System.out.print("Saving "+settings.getFileName());
		try {
			if(!settings.save()) return false;
		    PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(settings.getPath()+settings.getFileName(),true)));//"UTF-8" ??
		    
		    if(sampleID!=null) {
		    	writer.println("SAMPLE ID");
		    	sampleID.write(writer);
		    }
		    		    
		    if(pictures!=null) {
		    	writer.println("IMAGES");	
		    	//path of the experiment config file
		    	String base = this.getPath();
		    	for(int i=0;i<pictures.size();i++) {
		    		Picture p = pictures.get(i);	
		    		//make the image path relative to that of experiment config file
		    		String relative_path = new File(p.getPath()).toURI().relativize(new File(base).toURI()).getPath();
		    		writer.println(relative_path+";"+p.getFileName());    				
		    	}    		   
		    }
		    writer.close();
		    return true;
		}
		catch(IOException exc) {			
			JOptionPane.showMessageDialog(null,"Cannot write in "+settings.getFileName()+"!", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

	}
	
	public String exportCounts() {
		if(settings.getPath()==null || settings.getFileName()==null) return null;
		if(this.getNbPlates()<=0) return null;
		
		String out = settings.getFileName();
		out = out.replaceAll("\\.[a-z]*$","")+".csv";
		out = "COUNTS_"+out;
		
		Plate pl = null;
		String plateName = "";
		try {
			PrintWriter writer = new PrintWriter(settings.getPath()+out, "UTF-8");
			writer.print("sample_ID;");
			settings.writeCountsFileHeaders(writer);
			for(int i=0;i<this.getNbPictures();i++) {
				Picture p = this.getPicture(i);
				for(int j=0;j<p.getNbPlates();j++) {
					pl = p.getPlate(j);
					plateName = pl.getName();
					for(int k=0;k<pl.getNbWell();k++) {
						Well w = pl.getWell(k);
						writer.print(sampleID.getSampleID(w)+";");
						w.exportCounts(writer);	
					}
				}			
			}
			writer.close();
			return (out);
		}
		catch(IOException e) {
			JOptionPane.showMessageDialog(null,"Failed to write CFU counts in "+out+"!", "Write CFU counts for plate "+plateName, JOptionPane.ERROR_MESSAGE);
			System.out.println("Failed to write CFU counts in "+out);
		}
		return null;
	}

}

class NameComparator implements Comparator<String[]> {
    @Override
    public int compare(String[] a, String[] b) {
    	if(a.length<2 || b.length<2) return -1;
        return a[1].compareToIgnoreCase(b[1]);
    }
}
