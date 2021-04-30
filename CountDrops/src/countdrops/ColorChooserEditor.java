package countdrops;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

public class ColorChooserEditor extends AbstractCellEditor implements TableCellEditor {

	/**
	 color picker to add to second column of the CFU table
	 */
	private static final long serialVersionUID = 1L;

	private JButton delegate = new JButton();

	Color savedColor = Color.gray;
	int row,column;
	TableModel model;
	
	public ColorChooserEditor() {
		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				Color color = JColorChooser.showDialog(delegate, "Color Chooser", savedColor);				
				ColorChooserEditor.this.changeColor(color);
				
				//added so that color is updated in CFUTypeTable when ColorChooser is closed
				if(color!=null) model.setValueAt(color,row,column);
			}
		};
		delegate.addActionListener(actionListener);
	}

	public Object getCellEditorValue() {
		//System.out.println(PlateSettings.convertColorToHex((Color) savedColor));		
		return savedColor;
	}

	private void changeColor(Color color) {
		if (color != null) {
			savedColor = color;
			delegate.setBackground(color);
		}
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
			int r, int c) {
		changeColor((Color) value);
		
		//allows to access the cell that has been clicked
		//there might be a more elegant way of doing this, but it works fine
		row =r;
		column = c;
		if(table!=null) model = table.getModel();
		
		return delegate;
	}
}
