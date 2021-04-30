package countdrops;

import java.awt.Color;
import java.util.ArrayList;


public class SampleStatistics {
	private SampleID sampleID = null;
	private PlateSettings settings = null;
	
	private int posOfCurrentWell = -1;
	private String id = null;
	
	int nbCFUtype = 0;
	private String[] CFUtype   = null;
	
	private ArrayList<String>  plateName = null;
	private ArrayList<String>  wellName = null;
	private ArrayList<Double>  dilution  = null;
	private ArrayList<Double>  volume    = null;
	private ArrayList<int[]>   counts    = null; //count is -1 if CFU type is not countable !
	private ArrayList<Boolean> ignore    = null;
	
	private ArrayList<Double>  uniqueDilutionValues  = null;

	public SampleStatistics(Well w,SampleID xid,PlateSettings s) {
		sampleID = xid;
		settings = s;
		
		id =xid.getSampleID(w);
		nbCFUtype = s.getNCFUTYPES();
		CFUtype = new String[nbCFUtype];
		for(int i=0;i<nbCFUtype;i++) CFUtype[i] = s.getCFUType(i);		
	
		
		plateName = new ArrayList<String>();
		wellName = new ArrayList<String>();
		dilution = new ArrayList<Double>();
		volume = new ArrayList<Double>();
		counts = new ArrayList<int[]>();
		ignore = new ArrayList<Boolean>();
		uniqueDilutionValues = new ArrayList<Double>();
	}

    public void setPosOfCurrentWell(int i) {posOfCurrentWell=i;}
    public int getPosOfCurrentWell() {return posOfCurrentWell;}

	public int getNbCFUtype() {
		return nbCFUtype;
	}
	
	public Color getCFUColor(int type) {
		if(type<0 || type>settings.getNCFUTYPES()+1) return null;
		String str = settings.getCFUColor(type);
		return settings.convertStringToColor(str);
	}
	
	public String getID() {return id;}
	
	public int getNBcounts() {
		if(plateName==null) return 0;
		return plateName.size();
	}
		
	public boolean addCounts(Well w) {
		if(!id.equals(sampleID.getSampleID(w))) return false;
		
		plateName.add(w.getPlate());
		wellName.add(w.getName());
		dilution.add(w.getDilution());
		volume.add(w.getVolume());
		ignore.add(w.isIgnored());
		
		int[] c = new int[nbCFUtype+1];
		for(int i=0;i<nbCFUtype;i++) c[i] = w.getNbCFU(i);		
		counts.add(c);
				
		return true;
	}
	
	public void updateCounts(int pos,int[] x) {
		if(pos<0 || pos>getNBcounts()) return;
		if(x.length != nbCFUtype + 1) return;
		counts.set(pos,x);
	}
	
	public void updateCounts(int[] x) {
		updateCounts(posOfCurrentWell,x);		
	}
	
	public void updateUniqueDilutionValues() {
		for(int i=0;i<dilution.size();i++) {
			double x = dilution.get(i);
			boolean test = true;
			for(int j=0;j<uniqueDilutionValues.size() & test;j++) {
				if(Math.abs(uniqueDilutionValues.get(j)-x)<0.001) test=false;
			}
			if(test) { //add new value
				uniqueDilutionValues.add(x);
			}
		}
	}
	
	public String getPlateName(int i) {
		if(plateName==null) return "?";
		if(i<0 || i>getNBcounts()) return "?";
		return(plateName.get(i));
	}
	
	public String getWellName(int i) {
		if(wellName==null) return "?";
		if(i<0 || i>getNBcounts()) return "?";
		return(wellName.get(i));
	}
	
	public double getDilution(int i) {
		if(dilution==null) return -2.0;
		if(i<0 || i>getNBcounts()) return -1.0;
		return(dilution.get(i));
	}

	public int getNbUniqueDilutionValues() {
		if(uniqueDilutionValues==null) return 0;
		return uniqueDilutionValues.size();
	}
	
	public double getUniqueDilutionValue(int i) {
		if(uniqueDilutionValues==null) return -2.0;
		if(i<0 || i>getNbUniqueDilutionValues()) return -1.0;
		return(uniqueDilutionValues.get(i));
	}
	public double getVolume(int i) {
		if(volume==null) return -2.0;
		if(i<0 || i>getNBcounts()) return -1.0;
		return(volume.get(i));
	}
	
	public boolean getIgnored(int i) {
		if(ignore==null) return false;
		if(i<0 || i>getNBcounts()) return false;
		return(ignore.get(i));
	}
	public int getCount(int i,int cfutype) {
		if(counts==null) return -3;
		if(i<0 || i>getNBcounts()) return -3;
		if(cfutype<0 || cfutype>nbCFUtype) return -3;
		return(counts.get(i)[cfutype]);
	}
}
