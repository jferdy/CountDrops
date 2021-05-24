package countdrops;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


public class DilutionTableCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;

	@Override
	  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {

	    //Cells are by default rendered as a JLabel.
	    JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);	 	
	    if(col>0) {
		    //Get the status for the current row.
		    DilutionTableModel tableModel = (DilutionTableModel) table.getModel();
		    if (! tableModel.isCellEditable(row,col)) {		      
		    	l.setBackground(Color.LIGHT_GRAY);
		    	l.setOpaque(true);
		    } else {
		    	l.setOpaque(false);
		    }		    
	    } else {
	        l.setBackground(table.getTableHeader().getBackground());
	        l.setForeground(table.getTableHeader().getForeground());
	        l.setBorder(table.getTableHeader().getBorder());	        
	        l.setHorizontalAlignment(CENTER);
	    }
	  //Return the JLabel which renders the cell.
	  return l;
	}
}
