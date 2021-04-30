package countdrops;

public class PlateDilutionSettingsEvent {
	int NROWS;
	int NCOLS;
	
	public PlateDilutionSettingsEvent(int r, int c) {
    	super();
    	NROWS = r;
    	NCOLS = c;
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
	

}

