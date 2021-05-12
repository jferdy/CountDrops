package countdrops;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;


public class SetPlateDilutionSettings extends JPanel implements ActionListener {		
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
			
		nColsCombo = new JComboBox<Integer>(nCols);
		nColsCombo.setSelectedIndex(settings.getNCOLS()-1);
		nColsCombo.addActionListener(this);
		nColsCombo.setActionCommand("CHANGEDIMENSION");
		nColsCombo.setLightWeightPopupEnabled (false);
		
		//formatted field for volume
		volumeField = new JFormattedTextField(volumeFormat);
		volumeField.setValue(settings.getVolume());
		volumeField.setColumns(5);
		volumeField.setActionCommand("CHANGEVOLUME");
		volumeField.addActionListener(this);
		
		//comboBox for dilution scheme					
		String[] schemes = {"Fixed","By row","By columns","Custom"};		
		dilutionScheme = new JComboBox<String>(schemes);
		dilutionScheme.setSelectedIndex(settings.getDilutionScheme()-1);
		dilutionScheme.addActionListener(this);
		dilutionScheme.setActionCommand("CHANGESCHEME");
		dilutionScheme.setLightWeightPopupEnabled (false);


		//JTable for dilution				
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

		this.setLayout(new BoxLayout(this,BoxLayout.PAGE_AXIS));
		
		JPanel nrowsPanel = new JPanel();
		nrowsPanel.setLayout(new FlowLayout());		
		nrowsPanel.add(new JLabel("Number of Rows"));		
		nrowsPanel.add(nRowsCombo);
		nrowsPanel.add(Box.createRigidArea(new Dimension(5,0)));
		nrowsPanel.add(new JLabel("Number of columns"));
		nrowsPanel.add(nColsCombo);
		nrowsPanel.setAlignmentX(CENTER_ALIGNMENT);
		add(nrowsPanel);
						
		JPanel volPanel = new JPanel();
		volPanel.setLayout(new FlowLayout());
		volPanel.add(new JLabel("Volume in microliters"));
		volPanel.add(volumeField);
		volPanel.add(Box.createRigidArea(new Dimension(5,0)));
		volPanel.add(new JLabel("Dilution scheme"));
		volPanel.add(dilutionScheme);
		volPanel.setAlignmentX(CENTER_ALIGNMENT);
		add(volPanel);
			
		JLabel dilLab = new JLabel("Dilution factors");
		dilLab.setAlignmentX(CENTER_ALIGNMENT);
		add(dilLab);
		dilutionTableScrollPanel.setAlignmentX(CENTER_ALIGNMENT);
		add(dilutionTableScrollPanel);
		JLabel dilLab2 = new JLabel("1 means that sample is not diluted, 10 that it is diluted 10 times, etc.");
		JLabel dilLab3 = new JLabel("You may use scientific notation (1E3 instead of 1000) if you wish.");
		dilLab2.setAlignmentX(CENTER_ALIGNMENT);
		dilLab3.setAlignmentX(CENTER_ALIGNMENT);
		add(dilLab2);
		add(dilLab3);
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
