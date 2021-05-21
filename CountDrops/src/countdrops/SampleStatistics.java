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
	private ArrayList<Integer>   counts    = null; //count is -1 if CFU type is not countable !
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
		counts = new ArrayList<Integer>();
		ignore = new ArrayList<Boolean>();
		uniqueDilutionValues = new ArrayList<Double>();
	}

	public SampleStatistics(SampleStatistics s) {
		sampleID = s.getSampleID();
		settings = s.getSettings();
		
		id =s.getID();
		nbCFUtype = settings.getNCFUTYPES();
		CFUtype = new String[nbCFUtype];
		for(int i=0;i<nbCFUtype;i++) CFUtype[i] = s.getCFUType(i);		
	
		
		plateName = new ArrayList<String>();
		wellName = new ArrayList<String>();
		dilution = new ArrayList<Double>();
		volume = new ArrayList<Double>();
		counts = new ArrayList<Integer>();
		ignore = new ArrayList<Boolean>();
		uniqueDilutionValues = new ArrayList<Double>();
		
		for(int i=0;i<s.getNBcounts();i++) {
			plateName.add(s.getPlateName(i));
			wellName.add(s.getWellName(i));
			dilution.add(s.getDilution(i));
			volume.add(s.getVolume(i));
			
			ignore.add(s.getIgnored(i));
						
			for(int j=0;j<nbCFUtype+1;j++) counts.add(s.getCount(i,j));			
		}
		for(int i=0;i<s.getNbUniqueDilutionValues();i++) {
			uniqueDilutionValues.add(s.getUniqueDilutionValue(i));
		}
		posOfCurrentWell = s.getPosOfCurrentWell();
	}


    public void setPosOfCurrentWell(int i) {posOfCurrentWell=i;}
    public int getPosOfCurrentWell() {return posOfCurrentWell;}
    
	public int getNbCFUtype() {
		return nbCFUtype;
	}
	
	public Color getCFUColor(int type) {
		if(type<0 || type>settings.getNCFUTYPES()+1) return null;
		String str = settings.getCFUColor(type);
		return PlateSettings.convertStringToColor(str);
	}
	
	public String getCFUType(int i) {
		if(i<0 || i>=nbCFUtype) return(null);
		return(CFUtype[i]);
	}
	
	public SampleID getSampleID() {
		return(sampleID);
	}
	
	public PlateSettings getSettings() {
		return(settings);
	}
	
	public String getID() {return id;}
	
	public int getNBcounts() {
		if(plateName==null) return(0);
		if(plateName.size()<=0) return(0);
		return plateName.size();
	}
		
	public boolean addCounts(Well w) {
		if(!id.equals(sampleID.getSampleID(w))) return false;
		
		plateName.add(w.getPlate());
		wellName.add(w.getName());
		dilution.add(w.getDilution());
		volume.add(w.getVolume());
		ignore.add(w.isIgnored());
				
		for(int i=0;i<=nbCFUtype;i++) counts.add(w.getNbCFU(i));								
		return true;
	}
	
	public void updateCounts(Well w) {
		//look for well
		String pln = w.getPlate();
		String wln = w.getName();
		
		int pos = -1;		
		for(int i=0;i<getNBcounts() && pos<0;i++) {
			if(pln.equals(plateName.get(i)) && wln.equals(wellName.get(i))) {
				pos = i;
			}			
		}				
		if(pos<0 || pos>=getNBcounts()) return;		
		for(int i=0;i<nbCFUtype+1;i++) {
			if(w.isNonCountable(i)) {
				counts.set(pos*(nbCFUtype+1)+i,-1);
			} else {
				counts.set(pos*(nbCFUtype+1)+i,w.getNbCFU(i));
			}
		}
	}
	
	public void updateCounts(int pos,int[] x) {
		if(pos<0 || pos>getNBcounts()) return;
		if(x.length != nbCFUtype + 1) return;
		for(int i=0;i<x.length;i++) counts.set(pos*(nbCFUtype+1)+i,x[i]); 		
	}
	
	public void updateCounts(int[] x) {
		updateCounts(posOfCurrentWell,x);		
	}
	
	public void updateUniqueDilutionValues() {
		uniqueDilutionValues.clear();
		for(int i=0;i<dilution.size();i++) {
			double x = dilution.get(i);
			boolean test = true;
			for(int j=0;j<uniqueDilutionValues.size() & test;j++) {
				if(Math.abs(uniqueDilutionValues.get(j)-x)<1e-4) test=false;
			}
			if(test) { //add new value
				uniqueDilutionValues.add(x);
			}
		}
	}
	
	public String getPlateName(int i) {
		if(plateName==null) return "?";
		if(i<0 || i>=plateName.size()) return "?";
		if(i<0 || i>getNBcounts()) return "?";
		return(plateName.get(i));
	}
	
	public String getWellName(int i) {
		if(wellName==null) return "?";
		if(i<0 || i>=wellName.size()) return "?";
		if(i<0 || i>getNBcounts()) return "?";
		return(wellName.get(i));
	}
	
	public double getDilution(int i) {
		if(dilution==null) return -2.0;
		if(i<0 || i>=dilution.size()) return -2.0;
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
		if(i<0 || i>=volume.size()) return -2.0;
		if(i<0 || i>getNBcounts()) return -1.0;
		return(volume.get(i));
	}
	
	public boolean getIgnored(int i) {
		if(ignore==null) return false;
		if(i<0 || i>getNBcounts()) return false;
		return(ignore.get(i));
	}

	public boolean setIgnored(int i,boolean b) {
		if(i<0 || i>getNBcounts()) return false;		
		return(ignore.set(i,b));
	}

	public boolean setIgnored(boolean b) {
		return(ignore.set(posOfCurrentWell,b));
	}

	public int getCount(int i,int cfutype) {
		if(counts==null) return -3;
		if(i<0 || i>getNBcounts()) return -3;
		if(cfutype<0 || cfutype>nbCFUtype) return -3;
		
		int pos = i*(1+nbCFUtype)+cfutype;
		if(pos<0 || pos>=counts.size()) return -4;
		
		return(counts.get(pos));
	}
}
