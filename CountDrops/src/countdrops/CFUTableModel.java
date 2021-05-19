package countdrops;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;


// TableModel for cfuTable
//************************
// This table gathers data on each CFU, obtained from the ImageWell component.
// The first column gives CFU index + 1: from that, the position of the CFU in the ImageWell CFU list can therefore be obtained. 
// The function initializeCFU() does the job of constructing the table and getting data.
// It is called each time rows are added or deleted, which might seem unnecessarily heavy when a single row
// is affected. It turned out, tough, that this way of doing makes no difference is reactivity. And most
// of all, this ensures that indices of the first column match those of the CFU list in ImageWell.
// The function update CFU assumes that CFU indices have not changed, although CFU data may have been modified.

// The table also have a listener which triggers changes in CFU selected inside the ImageWell each time row selection is changed.
// Note that the isDeaf boolean is crucial here : has ImageWell also sends notification when selection is changed, the two coupled
// listener could end up creating an infinite loop if they were not blocked when necessary.  


public class CFUTableModel extends AbstractTableModel {
	
	private static final long serialVersionUID = 1L;
	private Object[][] data;
	private String[] title;
	private ImageWell img = null;
		
	
	//TODO add checkBox that would allow to choose which column to display in table
	//Statistics to display may be: 
		//Area
	    //Circularity
		//Feret min 
		//Feret max 
		//Feret min/max
		//Brightnes (one column per slice)
		//Hue  (one column per slice)
		//Saturation  (one column per slice)
	
	//This could be stored in a boolean vector like that
	/*
	private Boolean[] columnDisplayed = new  Boolean[] {};	
	private Boolean isColumnDisplayed(int i) {if(i<0 || i>=columnDisplayed.length) return false; return columnDisplayed[i];}
	*/
	//The list of options must also be included in the ViewWellEvent class, so that these options can be passed to the ViewWell constructor when navigating among wells. 
	//This way, the user will not have to set them each time he goes to a new well. The options would be lost, though, we a new well is clicked directly on the plate.
	
	//Constructor
	public CFUTableModel(Object[][] data, String[] title){
		this.data = data;
		this.title = title;
	}

	public CFUTableModel(ImageWell ximg){
		img = ximg;
		
		//fill the table from data stored in ImageWell 

		//set number of columns
		//one measure of brightness per slice !
		int nbslice = img.getImagePlus().getNSlices();		

		//column headers
		this.title = new String[6+nbslice];
		this.title[0] = "Num";	    
		this.title[1] = "Name";
		this.title[2] = "Type";
		this.title[3] = "Area";
		this.title[4] = "Circ.";
		this.title[5] = "F1/F2";
		for(int i=0;i<nbslice;i++) this.title[6+i] = "Bright "+(i+1);

		initializeCFU();
	}	

	public void initializeCFU() {		
		//resize data and copy from ImageWell
		int nbCFU = img.getWell().getNbCFU();		
		int nbslice = img.getImagePlus().getNSlices();		
		this.data = new Object[nbCFU][title.length];
		
		for(int i=0;i<nbCFU;i++) {
			CFU cfu = img.getWell().getCFU(i);		
			this.data[i][0] = i+1;
			this.data[i][1] = cfu.getCFUName();
			this.data[i][2] = cfu.getCFUType();
			this.data[i][3] = cfu.getArea();
			this.data[i][4] = cfu.getCircularity();
			this.data[i][5] = cfu.getFeret(2)/cfu.getFeret(0); // min / max calliper
			double br[] = cfu.getBrightness(img.getImagePlus());
			for(int j=0;j<nbslice;j++) this.data[i][6+j] = br[j];
		}
		
		fireTableDataChanged();		
	}
	
	//Return column number
	public int getColumnCount() {
		return this.title.length;
	}

	//Return row  number
	public int getRowCount() {
		if(data==null) return 0;
		return this.data.length;
	}

	//Return value for specified row and column position
	public Object getValueAt(int row, int col) {
		if(row<0 || row>=data.length) return null;
		return this.data[row][col];
	}
	
	public String getColumnName(int col) {
		if(col<0 || col>=title.length) return null;
	    return this.title[col];
	}

	public Class getColumnClass(int col) {		
		if(getRowCount()==0) return Object.class;
	    if (col < 0 || col >= title.length) return Object.class;
		return getValueAt(0, col).getClass();	    		
	}
	
	// index / position conversion
	//****************************
	
	//return the CFU index for a given row
	public int getCFUIndex(int posRow) {
		int index = (int) (data[posRow][0]);
		index-=1;
		return index;
	}

	public int getRowPosition(int index) {
		if(data == null) return -1;
		
		//Index is the position of a CFU in the list which is stored in ImageWell		
		//But the table may have been sorted so that row index does not match CFU
		//index anymore! We thus need to find the row where the cfu is to be found.
		
		//Note that convertRowIndexToModel could be an option here, but only if the table has
		//as many rows as there the well has CFUs.
		for(int posRow=0;posRow<data.length;posRow++) {
			if(index == getCFUIndex(posRow)) {
				//this is the right row!	
				return(posRow);
			}
		}
		return -1;
	}
	
	// update data, adding or removing rows
	//*************************************
	//update CFU table from CFUs
	//new row is added if necessary (i.e. if index is not registered in table)
	public void updateCFU(int index) {
		CFU cfu = img.getWell().getCFU(index);
		if(cfu==null) return;
		
		int posRow = getRowPosition(index);
		if(posRow<0) {
			//CFU has not been found...
			return;
		}

		//CFU is already registered in the table: its measurement are update
		this.data[posRow][1] = cfu.getCFUName();
		this.data[posRow][2] = cfu.getCFUType();
		this.data[posRow][3] = cfu.getArea();
		this.data[posRow][4] = cfu.getCircularity();
		this.data[posRow][5] = cfu.getFeret(2)/cfu.getFeret(0);
		double br[] = cfu.getBrightness(img.getImagePlus());
		for(int i=0;i<img.getImagePlus().getNSlices();i++) this.data[posRow][6+i] = br[i];
		fireTableDataChanged();
	}

	public void updateCFU() {
		for(int index=0;index < img.getWell().getNbCFU();index++) {
			//this may not be optimal as new rows corresponding to news CFU will be added one by one
			//but at least the table and the CFU list in img should always correspond
			updateCFU(index);
		}				
	}
	
	public void removeRow () {	  
		int nbRow = 0;
		if(data!=null) nbRow = data.length;
		int nbCFU = img.getWell().getNbCFU();
		int nbRemoved = nbRow-nbCFU;
		if(nbRemoved<1) return;
				
		data = null;		
		if(nbRemoved>nbRow) return;
		
		//reinitialize table
		initializeCFU();
	}
	
	//add one row and fills it with data from CFU index
	public void addRow () {
		int nbRow = 0;
		if(data!=null) nbRow = data.length;
		int nbCFU = img.getWell().getNbCFU();
		int nbAdded = nbCFU-nbRow;
		if(nbAdded<1) return;
				
		data = null;		
		//reinitialize table
		initializeCFU();
	}

}

//row selection listener for cfuTable
class CFUTableRowListener  implements ListSelectionListener{
	ImageWell img;
	JTable    tab;
	Boolean   isDeaf = false;
	
	public CFUTableRowListener (ImageWell ximg,JTable xtab) {
		//super();		
		img =ximg;
		tab=xtab;
	}
	 
	public void setIsDeaf(Boolean b) {isDeaf = b;}
	
	public void valueChanged(ListSelectionEvent e){
		if(isDeaf) return;
		if (e.getValueIsAdjusting()) return;
		
		/*
	    GenericDialog gd = new GenericDialog("Well inspection");
	    gd.addMessage("Row sends event to whoever listens");
	    gd.showDialog();
		 */
		
		ListSelectionModel lsm = (ListSelectionModel)e.getSource();

		img.setIsMute(true);
		img.deselectAllCFU();
		
		if (! lsm.isSelectionEmpty())  {					
			for(int i=0;i<img.getWell().getNbCFU();i++) {
				if(lsm.isSelectedIndex(i)) {
					//convert row position in table (which may have been sorted) to position in data
					int j =  tab.convertRowIndexToModel(i); 
					img.selectCFU(j);	
				}
			}		
		}
		img.drawCFU();
		img.setIsMute(false);
	}
	
}
