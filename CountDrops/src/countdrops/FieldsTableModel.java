package countdrops;

import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;

// TableModel for the fieldsTable
// *******************************

public class FieldsTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	private Object[][] data;
	private String[] title;
	PlateSettings settings;

	int status = -1;
	
	public FieldsTableModel(PlateSettings s, int st) {
		super();
		if(s==null) {			
			settings = new PlateSettings();
		} else {
			settings = s;
		}
		status = st;
		
		//column headers
		this.title = new String[4];
		this.title[0] = "Name";	    
		this.title[1] = "Type";
		this.title[2] = "Default";
		this.title[3] = "Description";

		updateTableFromSettings();
	}
	
	public void updateTableFromSettings() {
		data = null;		
		//data
		data = new Object[settings.getNFIELDS()][4];
		for(int i=0;i<settings.getNFIELDS();i++) {
			data[i][0] = settings.getFieldsName().get(i);
			data[i][1] = settings.getFieldsType().get(i);
			data[i][2] = settings.getFieldsValue().get(i);
			data[i][3] = settings.getFieldsDescription().get(i);
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
	public String getName(int row) {
		if(row<0 || row>data.length-1) return null;
		return (String) data[row][0];
	}	
	public String getType(int row) {
		if(row<0 || row>data.length-1) return null;
		return (String) data[row][1];
	}
	public String getValue(int row) {
		if(row<0 || row>data.length-1) return null;
		return (String) data[row][2];
	}
	public String  getDescription(int row) {
		if(row<0 || row>data.length-1) return null;
		return (String) data[row][3];
	}

	public Object getValueAt(int i,int j) {
		if(i<0 || i>data.length-1) return null;
		if(j<0 || j>title.length-1) return null;
		return data[i][j];
	}
	public void setValueAt(Object value, int row, int col) {
		if(row<0 || row>data.length-1) return;
		if(col<0 || col>title.length-1) return;
		data[row][col] = value;
		fireTableCellUpdated(row, col);
	}
	public boolean isCellEditable(int row, int col) {
		if(status == SetExperimentSettings.CREATE) return true;
		if(col<2) return false; //when settings are edited, the name and type of fields cannot be changed
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

	//checks that all Fields are adequately formated
	//should be called before new settings are saved
	public boolean sanityCheck() {
		ArrayList<String> tmpArrayName = new ArrayList<String>();
				
		for(int i=0;i<getRowCount();i++) {			
			if(tmpArrayName.contains(data[i][0].toString())) {
				//name and key must be unique
				JOptionPane.showMessageDialog(null,"Field names must be unique! Please correct before saving.", "Save settings", JOptionPane.ERROR_MESSAGE);
				System.out.println("Failed to save CFU type settings!");						
				return false;
			}
												
			if(data[i][0].equals("") || data[i][2].equals("")) {
				// all CFU types must have a name and a key values
				JOptionPane.showMessageDialog(null,"All fields must have a name and a default value! Please correct before saving.", "Save settings", JOptionPane.ERROR_MESSAGE);
				System.out.println("Failed to save CFU type settings!");						
				return false;
			}
			
			tmpArrayName.add(data[i][0].toString());
		}
		return true;
	}
	
	public void updateSettingsFromTable() {
		//TODO check sanity of lines before adding them!! e.g. each field should have a name and a default value
		//issue a warning if this is not the case?
		settings.setNFIELDS(data.length);

		settings.getFieldsName().clear();
		settings.getFieldsType().clear();
		settings.getFieldsValue().clear();
		settings.getFieldsDescription().clear();

		for(int i=0;i<settings.getNFIELDS();i++) {
			settings.getFieldsName().add((String) data[i][0]);
			settings.getFieldsType().add((String) data[i][1]);
			settings.getFieldsValue().add((String)data[i][2]); 
			settings.getFieldsDescription().add((String) data[i][3]); 
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
		data2[nb][1] = "String";
		data2[nb][2] = "";
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
		data=data2;
		fireTableDataChanged();		
	}
}

