package countdrops;

import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

// TableModel for the summaryTable
// *******************************
// This table gathers CFU counts per type and data are computed from the ImageWell component.
// Upon counts updating, checkbox is set to false and disabled if some CFU are counted or some types are non countable 
// Upon counts updating, the non-countable checkbox associated to a CFU type is set to false and disabled if CFU have counted for that type 
// The row selected in that table corresponds to the current CFU type in the ImageWell component. Changes in row selection 
// are monitored by a RowListener and trigger an update of current CFU type in the ImageWell component.

public class SummaryTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	private Object[][] data;
	private String[] title;
		
	private ImageWell img; //the ImageWell where data are to be found
	private JCheckBox chkEmpty; //the empty checkbox, which must be disabled if CFU have be counted
	private GraphCanvas statisticsGraphics = null;
	
	ArrayList<ViewWellListener> listViewWellListener;
	ViewWellEvent viewWellEvent;
	
	public SummaryTableModel(ImageWell ximg,JCheckBox xchk,GraphCanvas gr,ArrayList<ViewWellListener> xlistViewWellListener,ViewWellEvent xviewWellEvent) {
		super();
		img=ximg;
		chkEmpty=xchk;
		statisticsGraphics = gr;
		
		listViewWellListener = xlistViewWellListener;
		viewWellEvent  = xviewWellEvent;
		
		//column headers
		this.title = new String[4];
		this.title[0] = "Type";	    
		this.title[1] = "Key";
		this.title[2] = "Nb";
		this.title[3] = "Inf.";
		
		//this.title[4] = img.getWell().getName()+" in other plates";

		//data
		int nbRow = img.getWell().getNCFUTYPES()+1;
		this.data = new Object[nbRow][title.length];
		for(int i=0;i<nbRow;i++) data[i][3]=false;		
		initializeTable();
	}

	public void initializeTable() {
		Well w = img.getWell();
		int nbRow = w.getNCFUTYPES()+1;
		
		data[0][0]="NA";
		data[0][1]="SPACE";
		data[0][2]=0;
		data[0][3]=w.isNonCountable(0); //NA CFU!!
		
		for(int i=0;i<w.getNCFUTYPES();i++) {
			data[i+1][0] = w.getCFUType(i);
			data[i+1][1] = w.getCFUKey(i);
			data[i+1][2] = 0;
			data[i+1][3] = w.isNonCountable(i+1);
		}
		
		for(int i=0;i<w.getNbCFU();i++) {
			CFU cfu = w.getCFU(i);
			for(int j=0;j<nbRow;j++) {
				if(data[j][0].equals(cfu.getCFUType())) data[j][2]= 1+ (Integer) (data[j][2]);
			}
		}
				
		chkEmpty.setEnabled(true);
		for(int i=0;i<nbRow;i++) { 
			if((Integer) (data[i][2]) > 0) {
				//type cannot be uncountable because some CFU have been counted
				data[i][3]=false;				
				//well cannot be empty because some CFU have been counted for this type
				chkEmpty.setSelected(false);
				chkEmpty.setEnabled(false);
				//update data in Well
				w.setEmpty(false);
				w.setNonCountable(i,false);				
			}
			if((Boolean) data[i][3]) {
				//well cannot be empty because type is uncountable
				chkEmpty.setSelected(false);
				chkEmpty.setEnabled(false);
				//update data in Well
				w.setEmpty(false);
			}
		}
		
		//update statistics in graphics !
		int x[] = new int[data[0].length];
		for(int i=0;i<nbRow;i++) {
			if((boolean) (data[i][3])) {
				x[i]=-1;
			} else {
				x[i] = (int) (data[i][2]);
			}
		}
		statisticsGraphics.updateCounts(x);
		statisticsGraphics.repaint();
		
		for(ViewWellListener l : listViewWellListener) l.viewWellChange(viewWellEvent);
		w.write();		
		fireTableDataChanged();					
	}
		
	public int getRowCount() {
		return  img.getWell().getNCFUTYPES()+1;
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
	public void setValueAt(Object value, int row, int col) {
		if(col!=3) return; //only third column (NC field) can be changed
		if((int) data[row][2]>0 && (boolean) value) return; //NC can be set to true only if no CFU has been counted for the type
		
        data[row][col] = value;
        img.getWell().setNonCountable(row,(Boolean) value);
        if((Boolean) value) {
        	//type set to NC -> the well cannot be empty
        	chkEmpty.setSelected(false);
        	chkEmpty.setEnabled(false);
        } else {
        	//type is set to not NC -> the well may be empty, but all types must be checked
        	boolean test = false;
        	for(int i=0;i<data.length;i++) test = test || (boolean) data[i][col] || (int) data[i][2] > 0;
        	//chkEmpty is disabled if some CFU counted or if a type is NC
        	chkEmpty.setEnabled(!test);
        	if(test) {
        		//chkEmpty is disabled... Not sure it is usefull to set it to false!
        		chkEmpty.setSelected(false);
        	} else {
        		//chkEmpty is enabled: set to true if no CFU counted and no type is NC
        		test = true;
        		for(int i=0;i<data.length;i++) test = test && (int) data[i][2] == 0;
        		chkEmpty.setSelected(test);
        	}
        }   
        img.getWell().setEmpty(chkEmpty.isSelected());
        for(ViewWellListener l : listViewWellListener) l.viewWellChange(viewWellEvent);
        img.getWell().write();
        
        fireTableCellUpdated(row, col);        
    }
	
	public boolean isCellEditable(int row, int col) {
		//Only columns 3 (with contains the non-countable checkboxes) is editable.
		//If the number of CFU is non zero, that checkbox should be set to false and not editable
		return col == 3 && (Integer) data[row][2] == 0;		
		}		
	public Class getColumnClass(int column) {
		switch (column) {
		case 0:
			return String.class;
		case 1:
			return String.class;
		case 2:
			return Integer.class;
		case 3:
			return Boolean.class;
		case 4:
			return String.class;
		default:
			return Object.class;
		}
	}	
}

//row selection listener for summaryTable
class SummaryTableRowListener  implements ListSelectionListener{
	ImageWell img;	
	GraphCanvas statisticsGraphics = null;
	
	public SummaryTableRowListener (ImageWell ximg,GraphCanvas gr) {
		//super();		
		img =ximg;		
		statisticsGraphics = gr;
	}
	 
	public SummaryTableRowListener (ImageWell ximg) {
		//super();		
		img =ximg;		
		statisticsGraphics = null;
	}
	
	public void setStatisticsGraphics(GraphCanvas gr) {
		statisticsGraphics = gr;
	}
	
	public void valueChanged(ListSelectionEvent e){
		if (e.getValueIsAdjusting()) return;
					
		ListSelectionModel lsm = (ListSelectionModel)e.getSource();
		
		//get selected line
		int i = lsm.getLeadSelectionIndex();
		
		//sets default type in img
		if(i<0 || i>img.getWell().getNCFUTYPES()) return;
		img.setCurrentCFUType(i-1);
		
		//sets displayed type in graphics
		if(statisticsGraphics != null) {		
			statisticsGraphics.setCFUtype(i);										
			statisticsGraphics.repaint();
		}
	}
	
}
