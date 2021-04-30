package countdrops;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;


public class SetPlateDilutionSettings extends Panel implements ActionListener {		
	private static final long serialVersionUID = 1L;
	
	private JComboBox<Integer> nRowsCombo = null;
	private JComboBox<Integer> nColsCombo = null;
	private JComboBox<String> dilutionScheme = null;
	private JFormattedTextField volumeField = null;
	private NumberFormat volumeFormat;
	private JTable    dilutionTable = null;
	private DilutionTableModel dilutionTableModel = null;

	//event sent out to whoever is listening
	private PlateDilutionSettingsEvent plateDilutionSettingsEvent; 
	//listeners hooked up onto that panel (typically opened by SetPlateSettings to react to changes in plate dimensions)
	ArrayList<PlateDilutionSettingsListener> listPlateDilutionSettingsListener = new ArrayList<PlateDilutionSettingsListener>();

	public SetPlateDilutionSettings(PlateSettings settings) {
		super();				
						
		plateDilutionSettingsEvent = new PlateDilutionSettingsEvent(settings.getNROWS(),settings.getNCOLS());
		
		this.setLayout(new GridBagLayout());		
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(4,4,4,4);        
        gbc.anchor = GridBagConstraints.WEST;
		
		//combo boxes for plate dimension
		Integer[] nRows = new Integer[16];
		Integer[] nCols = new Integer[24];
		for(int i=0;i<nRows.length;i++) nRows[i]=(i+1);
		for(int i=0;i<nCols.length;i++) nCols[i]=(i+1);
		nRowsCombo = new JComboBox<Integer>(nRows);
		nRowsCombo.setSelectedIndex(settings.getNROWS()-1);
		nRowsCombo.addActionListener(this);
		nRowsCombo.setActionCommand("CHANGEDIMENSION");	
		nRowsCombo.setLightWeightPopupEnabled (false);
		gbc.gridx=0;
		gbc.gridy=0;
		add(new JLabel("Number of rows"),gbc);
		gbc.gridx=1;
		gbc.gridy=0;
		add(nRowsCombo,gbc);
		
		nColsCombo = new JComboBox<Integer>(nCols);
		nColsCombo.setSelectedIndex(settings.getNCOLS()-1);
		nColsCombo.addActionListener(this);
		nColsCombo.setActionCommand("CHANGEDIMENSION");
		nColsCombo.setLightWeightPopupEnabled (false);
		gbc.gridx=0;
		gbc.gridy=1;
		add(new JLabel("Number of columns"),gbc);
		gbc.gridx=1;
		gbc.gridy=1;
		add(nColsCombo,gbc);
		
		//formatted field for volume
		gbc.gridx=0;
		gbc.gridy=2;
		add(new JLabel("Volume in microliters"),gbc);
		gbc.gridx=1;
		gbc.gridy=2;		
		volumeField = new JFormattedTextField(volumeFormat);
		volumeField.setValue(settings.getVolume());
		volumeField.setColumns(5);
		volumeField.setActionCommand("CHANGEVOLUME");
		volumeField.addActionListener(this);
		add(volumeField,gbc);				
		
		//comboBox for dilution scheme					
		String[] schemes = {"Fixed","By row","By columns","Custom"};		
		dilutionScheme = new JComboBox<String>(schemes);
		dilutionScheme.setSelectedIndex(settings.getDilutionScheme()-1);
		dilutionScheme.addActionListener(this);
		dilutionScheme.setActionCommand("CHANGESCHEME");
		dilutionScheme.setLightWeightPopupEnabled (false);
		gbc.gridx=0;
		gbc.gridy=3;
		add(new JLabel("Dilution scheme"),gbc);
		gbc.gridx=1;
		gbc.gridy=3;
		add(dilutionScheme,gbc);
		gbc.gridy++;
		gbc.gridx--;
		
		//JTable for dilution
		gbc.gridx=0;
		gbc.gridy=4;
		add(new JLabel("Dilution factors"),gbc);
		
		dilutionTableModel = new DilutionTableModel(settings);
		dilutionTable = new JTable(dilutionTableModel);		
		dilutionTable.setDefaultRenderer(Object.class, new DilutionTableCellRenderer());		
		dilutionTable.setDefaultEditor(double.class, new DilutionTableCellEditor());
		dilutionTable.setCellSelectionEnabled(true);
		dilutionTable.getTableHeader().setReorderingAllowed(false);
		
		dilutionTable.setFillsViewportHeight(true);
		dilutionTable.setPreferredScrollableViewportSize(new Dimension(13*12*4,9*12*2)); //12 per character, 4 characters per column
	
		dilutionTable.getColumnModel().setColumnSelectionAllowed(false);
		JScrollPane dilutionTableScrollPanel = new JScrollPane(dilutionTable,				
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		gbc.gridx=0;
		gbc.gridy=5;
		gbc.gridwidth=3;
		gbc.fill = GridBagConstraints.HORIZONTAL;		
		add(dilutionTableScrollPanel,gbc);
		gbc.gridy=6;
		add(new JLabel("1 means that sample is not diluted, 10 that it is diluted 10 times, etc."),gbc);
		gbc.gridy=7;
		add(new JLabel("You may use scientific notation (1E3 instead of 1000) if you wish."),gbc);
	}

	public void addListener(PlateDilutionSettingsListener toAdd) {
		listPlateDilutionSettingsListener.add(toAdd);
    }

	public int getNROWS() {		
		return nRowsCombo.getSelectedIndex()+1;		
	}
	public void setNROWS(int i) {		
		nRowsCombo.setSelectedIndex(i-1);		
	}

	public int getNCOLS() {
		return nColsCombo.getSelectedIndex()+1;
	}
	public void setNCOLS(int i) {		
		nColsCombo.setSelectedIndex(i-1);		
	}

	public double getVolume() {
		//get volume from the formatted field
		return (double) volumeField.getValue();
	}
	public void setVolume(double v) {
		//get volume from the formatted field
		volumeField.setValue(v);
	}
	
	public int getDilutionScheme() {
		//retrieve dilution scheme from the comboBox		
		return dilutionScheme.getSelectedIndex()+1;		
	}
	public void setDilutionScheme(int i) {		
		dilutionScheme.setSelectedIndex(i-1);		
	}
	
	public double[] getDilution() {
		//retrieve dilution from JTable
		double[] dilution;
		switch(getDilutionScheme()) {
		case 1:
			//fixed
			dilution = new double[1];
			dilution[0] = dilutionTableModel.getDilutionFactor(0,0);
			return dilution;
		case 2:	
			//by row
			dilution = new double[dilutionTableModel.getRowCount()];			
			for(int i=0;i<dilutionTableModel.getRowCount();i++) dilution[i] = dilutionTableModel.getDilutionFactor(i,0);
			return dilution;
		case 3:	
			//by column
			dilution = new double[dilutionTableModel.getColumnCount()];			
			for(int i=0;i<dilutionTableModel.getColumnCount()-1;i++) dilution[i] = dilutionTableModel.getDilutionFactor(0,i);
			return dilution;
		default:
			//custom
			dilution = new double[dilutionTableModel.getRowCount()*dilutionTableModel.getColumnCount()];
			int k=0;
			for(int i=0;i<dilutionTableModel.getRowCount();i++) {
				for(int j=0;j<dilutionTableModel.getColumnCount()-1;j++) {
					dilution[k] = dilutionTableModel.getDilutionFactor(i,j);
					k++;
				}			
			}
			return dilution;			
		}
	}
	
	public void setDilution(double[] dil) {
		dilutionTableModel.setDilution(dil);
	}		

	public void updateFromSettings(PlateSettings settings) {
		setNROWS(settings.getNROWS());
		setNCOLS(settings.getNCOLS());
		setDilutionScheme(settings.getDilutionScheme());
		setVolume(settings.getVolume());
		setDilution(settings.getDilution());
	}
	
	public void actionPerformed (ActionEvent e) {
		//resize JTable if plate dimension is changed
		//change editable cells if dilution scheme is changed
		String action = e.getActionCommand();
		if (action == "CHANGEDIMENSION") {
			plateDilutionSettingsEvent.setNCOLS(getNCOLS());
			plateDilutionSettingsEvent.setNROWS(getNROWS());
			for(PlateDilutionSettingsListener l : listPlateDilutionSettingsListener) l.DimentionHasChanged(plateDilutionSettingsEvent);
			
			dilutionTableModel.resize(getNROWS(),getNCOLS());					
			dilutionTableModel.setDilutionScheme(getDilutionScheme(),null);					
			return;
		}
		
		if (action == "CHANGESCHEME") {
			dilutionTableModel.setDilutionScheme(getDilutionScheme(),null);			
			return;
		}
	}
	
	public void setEnableNrowsNcolumns(boolean b) {
		nRowsCombo.setEnabled(b);
		nColsCombo.setEnabled(b);
	}
}
