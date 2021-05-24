package countdrops;

import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;

public class DilutionTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	private Object[][] data;
	private String[] colNames;
	//private String[] rowNames;
	private int dilutionScheme = -1;
	
	public DilutionTableModel(PlateSettings settings) {
		super();
		resize(settings.getNROWS(),settings.getNCOLS());	
		dilutionScheme = settings.getDilutionScheme();		
		setDilutionScheme(settings.getDilutionScheme(),settings.getDilution());		
	}

	@Override
	public int getColumnCount() {
		return data[0].length;
	}

	@Override
	public int getRowCount() {
		return data.length;
	}

	@Override
	public String getColumnName(int col) {
		if(col<0 || col>=colNames.length) return null;
		return colNames[col];
	}

	//@Override
	public String getRowName(int row) {
		if(row<0 || row>=getRowCount()) return null;
		return (String) data[row][0];
	}
	
	@Override
	public boolean isCellEditable(int row, int col) {
		if(col==0) return false;
		switch(dilutionScheme) {
		case 1:
			//1: fixed
			if(row==0 && col==1) return true;
			else return false;			
		case 2:
			//2: by row
			if(col==1) return true;
			return false; 			
		case 3:
			//3: by column
			if(row==0) return true;
			return false;
		case 4:
			//4: by well
			return true;
		default:
			return false;	
		}		
	}
	
	@Override
	public Class getColumnClass(int column) {
		if(column==0) return String.class;
		return Double.class;
	}

	@Override
	public Object getValueAt(int row, int col) {
		//first column is row header!
		if(col<0 || col>getColumnCount()) return null;
		if(row<0 || row>getRowCount()) return null;
		return data[row][col];
	}

	public double getDilutionFactor(int row,int col) {
		//first column is row header!
		String s = getValueAt(row,col+1).toString();
		if(s==null) return -1.0;
		return Double.parseDouble(s);
	}
	
	public void resize(int nrows,int ncols) {
		//rowNames = null;
		colNames = null;
		data = null;		
		
		//column headers
		colNames = new String[ncols+1];
		colNames[0]="";
		for(int i=0;i<ncols;i++) colNames[i+1] = ""+(i+1);

		//data (first column contains row names)
		data = new Object[nrows][ncols+1];

		String txt = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		for(int i=0;i<nrows;i++) {
			//assume here that plate cannot have more than 26 rows!
			if(i<txt.length()) {
				data[i][0] = txt.substring(i,i+1);
			} else {
				data[i][0] = ""+i;
			}
		}			
		fireTableStructureChanged();		
	}
	
	public void setDilutionScheme(int s,double[] dil) {
		if(s<1 || s>4) return;
		//set scheme
		dilutionScheme = s;
		
		//change cell content according to scheme
		double z = 0.0;
		for(int i=0;i<getRowCount();i++) {
			for(int j=1;j<getColumnCount();j++) {
				switch(dilutionScheme) {
				case 1:
					//1: fixed
					if(dil!=null && 0<dil.length) {
						data[i][j]=dil[0];
					} else {
						data[i][j]=1;
					}
					break;
				case 2:
					//2: by row					
					if(dil!=null && i<dil.length) {
						data[i][j]=dil[i];
					} else {
						z = Math.pow(10.0,(double) i);
						data[i][j]= Math.round(z);
					}
					break;
				case 3:
					//3: by column
					if(dil!=null && j-1< dil.length) {
						data[i][j]=dil[j-1];
					} else {
						z = Math.pow(10.0,(double) (j-1));
						data[i][j]= Math.round(z);
					}
					break;
				default:
					//4: by well
					if(dil!=null && i*(getColumnCount()-1)+j-1<dil.length) {
						data[i][j]=dil[i*(getColumnCount()-1)+j-1];
					} else {					
					//Does not change data so facilitate editing one of the other scheme!
					//For example if changing from dilution by row to custom will keep the default by row scheme 
					//and allow to change only a few values. Changing all dilution factors to one, as in the code
					//below, would force user to set again all dilution factors.
					//data[i][j]=1;
					}
					break;				
				}				
			}
		}
		fireTableDataChanged();
	}
	
	@Override	
	public void setValueAt(Object value, int row, int col) {
		if(col==0) return;
		double dil = -1;
		//convert value into long
		try {
			String str = ""+value;
			dil = Double.parseDouble(str);
			setValueAt(dil,row,col); 
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,"Cannot convert \""+(String) value+"\" into double!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
	}
	
	public void setValueAt(double dil, int row, int col) {	
		if(col==0) return;
		if(dil<1) {
			JOptionPane.showMessageDialog(null,"Dilution factor must be greater than one!", "Error", JOptionPane.ERROR_MESSAGE);
			return;			
		}
		//set value according to dilution scheme
		switch(dilutionScheme) {
		case 1:
			//1: fixed -> the value is copied on all cells
			for(int i=0;i<getRowCount();i++) {
				for(int j=1;j<getColumnCount();j++) {
					data[i][j]=dil;
				}
			}
			break;
		case 2:
			//2: by row -> the value is copied on all columns					
			for(int j=1;j<getColumnCount();j++) {
				data[row][j]= dil;
			}
			break;
		case 3:
			//3: by column -> the value is copied on all rows
			for(int i=0;i<getRowCount();i++) {
				data[i][col]= dil;
			}
			break;
		default:
			//4: by well
			data[row][col]=dil;
			break;				
		}				
		
		fireTableDataChanged();
	}
	
	public void setDilution(double[] dil) {
		//retrieve dilution from JTable
		switch(dilutionScheme) {
		case 1:
			//1: fixed -> the value is copied on all cells
			for(int i=0;i<getRowCount();i++) {
				for(int j=1;j<getColumnCount();j++) {
					data[i][j]=dil[0];
				}
			}
			break;
		case 2:	
			//2: by row -> the value is copied on all columns
			for(int i=0;i<dil.length;i++) {
				for(int j=1;j<getColumnCount();j++) {
					if(i<getRowCount()) {
						data[i][j]= dil[i];
					}
				}
			}
			break;
		case 3:	
			//3: by column -> the value is copied on all rows
			for(int i=0;i<dil.length;i++) {
				for(int j=0;i<getRowCount();i++) {
					if(i+1<getColumnCount()) {
						data[j][i]= dil[i];
					}
				}
			}
			break;
		default:
			//custom
			int k=0;
			for(int i=0;i<getRowCount();i++) {
				for(int j=0;j<getColumnCount()-1;j++) {
					if(i<getRowCount() && j+1<getColumnCount()) {
						data[i][j+1]=dil[k];
					}
					k++;					
				}
			}
			break;
		}
		fireTableDataChanged();
	}		


}
