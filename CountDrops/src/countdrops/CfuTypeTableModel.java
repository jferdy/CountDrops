package countdrops;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;



	// TableModel for the cfuTypeTable
	// *******************************
public class CfuTypeTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	private Object[][] data;
	private String[] title;
	PlateSettings settings;
	
	int status = -1;
	
	public CfuTypeTableModel(PlateSettings s,int st) {
		super();
		if(s==null) {			
			settings = new PlateSettings();
		} else {
			settings = s;
		}
		status = st;
		updateTableFromSettings();
	}

	public void updateTableFromSettings() {
		//column headers
		this.title = new String[4];
		this.title[0] = "Name";	    
		this.title[1] = "Key";
		this.title[2] = "Color";
		this.title[3] = "Description";
		//data
		data = new Object[settings.getNCFUTYPES()][4];
		for(int i=0;i<settings.getNCFUTYPES();i++) {
			data[i][0] = settings.getCFUType().get(i);
			data[i][1] = settings.getCFUKey().get(i);
			data[i][2] = PlateSettings.convertStringToColor(settings.getCFUColor().get(i));
			data[i][3] = settings.getCFUDescription().get(i);
		}
		fireTableDataChanged();
	}
	public int getRowCount() {
		return  data.length;
	}
	public int getColumnCount() {
		return  title.length;
	}
	public String getColumnName(int col) {
		if(col<0 || col>=title.length) return null;
		return this.title[col];
	}
	public Object getValueAt(int i,int j) {
		return data[i][j];
	}
	
	public String getCfuName(int row) {
		if(row<0 || row>=data.length) return null;
		return (String) data[row][0];
	}

	public char getCfuKey(int row) {
		if(row<0 || row>=data.length) return '?';
		String txt = (String) data[row][1];
		return txt.toCharArray()[0]; 
	}

	public String getCfuColor(int row) {
		if(row<0 || row>=data.length) return null;
		String s = PlateSettings.convertColorToHex((Color) data[row][2]);
		return s;
	}
	
	public String getCfuDescription(int row) {
		if(row<0 || row>=data.length) return null;
		return (String) data[row][3];
	}
	
	public void setValueAt(Object value, int row, int col) {
		if(row<0 || row>=data.length) return;
		if(col<0 || col>=title.length) return;
		data[row][col] = value;		
		fireTableCellUpdated(row,col);
	}
	public boolean isCellEditable(int row, int col) {
		if(status == SetExperimentSettings.CREATE) return true;
		if(col<1) return false; //if settings are edited, the name of CFU cannot be changed because some CFU with this name may have already been created
								//TODO this should be relaxed, and editing should be enabled for CFU types that have counts to zero
								// A better solution would even be to always allow edition and propagate changes to plate, wells and CFU... 
		return true;		
	}		
	public Class getColumnClass(int column) {
		switch (column) {
		case 0:
			return String.class;
		case 1:
			return String.class;
		case 2:
			return String.class;
		case 3:
			return String.class;
		default:
			return Object.class;
		}
	}
	
	//checks that all CFUtypes are adequately formated
	//should be called before new settings are saved
	public boolean sanityCheck() {
		ArrayList<String> tmpArrayName = new ArrayList<String>();
		ArrayList<String> tmpArrayKey  = new ArrayList<String>();
		
		for(int i=0;i<getRowCount();i++) {			
			if(tmpArrayName.contains(data[i][0].toString()) || tmpArrayKey.contains(data[i][1].toString())) {
				//name and key must be unique
				//JOptionPane jop = new JOptionPane();			
				JOptionPane.showMessageDialog(null,"CFU types names and key must be unique! Please correct before saving.", "Save settings", JOptionPane.ERROR_MESSAGE);
				System.out.println("Failed to save CFU type settings!");						
				return false;
			}
			
			tmpArrayName.add(data[i][0].toString());
			tmpArrayKey.add(data[i][1].toString());
			
			if(data[i][0].equals("") || data[i][1].equals("")) {
				// all CFU types must Zhave a name and a key values
				//JOptionPane jop = new JOptionPane();			
				JOptionPane.showMessageDialog(null,"All CFU types must have a name and a key! Please correct before saving.", "Save settings", JOptionPane.ERROR_MESSAGE);
				System.out.println("Failed to save CFU type settings!");						
				return false;
			}
		}
		return true;
	}
	
	public void updateSettingsFromTable() {
		//TODO check sanity of lines before adding them!! e.g. each type should have a name a key and a color 
		//issue a warning if this is not the case?
		settings.setNCFUTYPES(data.length);
		
		settings.getCFUType().clear();
		settings.getCFUKey().clear();
		settings.getCFUColor().clear();
		settings.getCFUDescription().clear();
		
		for(int i=0;i<settings.getNCFUTYPES();i++) {
			settings.getCFUType().add((String) data[i][0]);			
			settings.getCFUKey().add((String)data[i][1]); 
			settings.getCFUColor().add(PlateSettings.convertColorToHex((Color)data[i][2]));	
			settings.getCFUDescription().add((String) data[i][3]); 
		}				
	}

	public void add() {
		//data
		int nb = data.length;
		Object[][] data2 = new Object[nb+1][4];
		for(int i=0;i<nb;i++) {
			for(int j=0;j<4;j++) {
			data2[i][j] = data[i][j];
			}
		}
		//last line
		data2[nb][0] = "";
		data2[nb][1] = "";
		data2[nb][2] = Color.gray;
		data2[nb][3] = "";
		data = data2;
		fireTableDataChanged();		
	}
	
	public void delete(ArrayList<Integer> index) {
		//data		
		int nb = data.length;
		Object[][] data2 = new Object[nb-index.size()][4];
		int pos = 0;
		for(int i=0;i<nb;i++) {
			if(!index.contains(i)) {
				for(int j=0;j<4;j++) {
					data2[pos][j] = data[i][j]; 				
				}
				pos++;
			}
		}
		data = data2;
		fireTableDataChanged();			
	}
}
