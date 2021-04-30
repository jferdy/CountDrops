package countdrops;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;


public class SetExperimentSettings extends JDialog implements ActionListener {
	/**
	 A class to define or edit the experiment settings.  
	 **/

	private static final long serialVersionUID = 1L;
	private PlateSettings settings;

	private JTable fieldsTable,cfuTypeTable;
	private FieldsTableModel fieldsTableModel; 
	private CfuTypeTableModel cfuTypeTableModel;
	private SetPlateDilutionSettings dilutionPanel;

	private int status;
	
	static int CREATE = 1;
	static int EDIT = 2;
	static int CANCEL = 0;
	static int FAILED = -1;

	//constructor
	public SetExperimentSettings(PlateSettings s,int st) {
		super();		
				
		status = st;
		if(status!=CREATE && status!=EDIT && status!=FAILED && status!=CANCEL) return;
		
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		
		settings = s;	
		
		setPanel();		
		pack();
		this.setLocationRelativeTo(null);
		setVisible(true);
	}

	public SetExperimentSettings(int st) {		
		super();
				
		status = st;
		if(status!=CREATE && status!=EDIT && status!=FAILED && status!=CANCEL) return;
				
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		
		settings = new PlateSettings();		
		// setting default values
		settings.setDefault();
		
				
		setPanel();		
		pack();
		
		this.setLocationRelativeTo(null);
		setVisible(true);
	}

	public PlateSettings getSettings() {
		updateSettingsFromDisplay();
		return settings;	
	}
	
	public int getStatus() {
		return status;
	}
	
	public void setPanel() {
		this.setResizable(false);
		
		//left panel contains fieldsTable 		
		Panel leftPanel = new Panel();
		leftPanel.setLayout(new BoxLayout(leftPanel , BoxLayout.PAGE_AXIS));		
		leftPanel.add(new JLabel("<html><p style=\"width:300px\">Enter here fields that will describe plates.</p></html>"));

		fieldsTableModel = new FieldsTableModel(settings,status);
		fieldsTable = new JTable(fieldsTableModel);				
		fieldsTable.setCellSelectionEnabled(true);

		TableColumn fieldsTypeColumn = fieldsTable.getColumnModel().getColumn(1);		

		String[] fieldTypes = new String[] {"String","Integer","Boolean","Float"};
		JComboBox<String> comboBox = new JComboBox<String>(fieldTypes);
		//comboBox.setSelectedIndex(1);	    
		comboBox.setLightWeightPopupEnabled (false); //This line must be added otherwise combobox don't work properly.
													 //The issue comes from swing and awt libraries being mixed in the project.

		DefaultCellEditor editor = new DefaultCellEditor(comboBox);
		fieldsTypeColumn.setCellEditor(editor);	    

		DefaultTableCellRenderer renderer =
				new DefaultTableCellRenderer();
		renderer.setToolTipText("Click for combo box");
		fieldsTypeColumn.setCellRenderer(renderer);

		fieldsTable.getColumnModel().getColumn(0).setPreferredWidth(12*10);
		fieldsTable.getColumnModel().getColumn(1).setPreferredWidth(12*6);
		fieldsTable.getColumnModel().getColumn(2).setPreferredWidth(12*6);
		fieldsTable.getColumnModel().getColumn(3).setPreferredWidth(12*20);

		fieldsTable.setPreferredScrollableViewportSize(new Dimension(200,
				200));
		fieldsTable.setFillsViewportHeight(true);
		JScrollPane fieldsTableScrollPanel = new JScrollPane(fieldsTable,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);				
		//fieldsTableScrollPanel.setAlignmentX(0);
		leftPanel.add(fieldsTableScrollPanel);

		Panel leftPanel_bottom = new Panel();
		leftPanel_bottom.setLayout(new BoxLayout(leftPanel_bottom, BoxLayout.LINE_AXIS));

		ImageIcon icon_add = CountDrops.getIcon("list-add.png");	
		ImageIcon icon_rm = CountDrops.getIcon("list-remove.png");

		if(status == SetExperimentSettings.CREATE) {
			leftPanel_bottom.add(Box.createHorizontalGlue());			
			JButton b_AddField = new JButton(icon_add);
			b_AddField.addActionListener(this);
			b_AddField.setActionCommand("ADDFIELD");
			b_AddField.setToolTipText("Add a new plate description field.");
			leftPanel_bottom.add(b_AddField);
			
			
			JButton b_RmField = new JButton(icon_rm);
			b_RmField.addActionListener(this);
			b_RmField.setActionCommand("RMFIELD");
			b_RmField.setToolTipText("Remove the selected plate description field.");						
			leftPanel_bottom.add(b_RmField);
		
			leftPanel_bottom.add(Box.createHorizontalGlue());
			leftPanel.add(Box.createRigidArea(new Dimension(0,5)));
			leftPanel.add(leftPanel_bottom);
		}
		leftPanel.add(Box.createRigidArea(new Dimension(0,5)));		


		// the right panel contains CFU type description		
		Panel rightPanel = new Panel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.PAGE_AXIS));
		rightPanel.add(new JLabel("<html><p style=\"width:300px\">Enter here fields that describe CFU types.</p></html>"));

		cfuTypeTableModel = new CfuTypeTableModel(settings,status);
		cfuTypeTable = new JTable(cfuTypeTableModel);		
		cfuTypeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		TableColumn cfuColorColumn = cfuTypeTable.getColumnModel().getColumn(2);
		TableCellEditor colorEditor = new ColorChooserEditor();
		ColorRenderer colorRenderer = new ColorRenderer(false);
		cfuColorColumn.setCellEditor(colorEditor);
		cfuColorColumn.setCellRenderer(colorRenderer);		
		
		cfuTypeTable.getColumnModel().getColumn(0).setPreferredWidth(12*12);
		cfuTypeTable.getColumnModel().getColumn(1).setPreferredWidth(12*4);
		cfuTypeTable.getColumnModel().getColumn(2).setPreferredWidth(12*6);
		cfuTypeTable.getColumnModel().getColumn(3).setPreferredWidth(12*20);
		
		cfuTypeTable.setPreferredScrollableViewportSize(new Dimension(200,
				200));
		cfuTypeTable.setFillsViewportHeight(true);
		JScrollPane cfuTypeTableScrollPanel = new JScrollPane(cfuTypeTable,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);				
		//cfuTypeTableScrollPanel.setAlignmentX(0);
		rightPanel.add(cfuTypeTableScrollPanel);

		if(status == SetExperimentSettings.CREATE) {
			Panel rightPanel_bottom = new Panel();
			rightPanel_bottom.setLayout(new BoxLayout(rightPanel_bottom, BoxLayout.LINE_AXIS));

			JButton b_AddCfuType = new JButton(icon_add);
			b_AddCfuType.addActionListener(this);
			b_AddCfuType.setActionCommand("ADDCFUTYPE");
			b_AddCfuType.setToolTipText("Add a new CFU type.");

			JButton b_RmCfuType = new JButton(icon_rm);
			b_RmCfuType.addActionListener(this);
			b_RmCfuType.setActionCommand("RMCFUTYPE");
			b_RmCfuType.setToolTipText("Remove the selected CFU type.");

			rightPanel_bottom.add(Box.createHorizontalGlue());
			rightPanel_bottom.add(b_AddCfuType);
			rightPanel_bottom.add(b_RmCfuType);
			rightPanel_bottom.add(Box.createHorizontalGlue());

			rightPanel.add(Box.createRigidArea(new Dimension(0,5)));
			rightPanel.add(rightPanel_bottom);
		}
		rightPanel.add(Box.createRigidArea(new Dimension(0,5)));

		Panel topPanel = new Panel();		
		topPanel.setLayout(new BoxLayout(topPanel , BoxLayout.LINE_AXIS));		
		topPanel .add(leftPanel);
		topPanel.add(Box.createRigidArea(new Dimension(5,0)));
		topPanel.add(rightPanel);

		//panel with plate dimension, volume and dilution settings
		dilutionPanel = new SetPlateDilutionSettings(settings);

		//bottom panel with load, save and cancel buttons
		Panel bottomPanel = new Panel();		
		bottomPanel.setLayout(new BoxLayout(bottomPanel , BoxLayout.LINE_AXIS));

		if(status == CREATE) {
			JButton b_load = new JButton("Load settings");
			b_load.addActionListener(this);
			b_load.setActionCommand("LOAD");
			b_load.setToolTipText("Load settings from another experiment or from a plate setting file.");
			bottomPanel.add(b_load);
			bottomPanel.add(Box.createRigidArea(new Dimension(5,0)));
		}

		JButton b_save = new JButton("OK");
		b_save.addActionListener(this);
		b_save.setActionCommand("SAVE");
		b_save.setToolTipText("Save settings.");
		bottomPanel.add(b_save);
		bottomPanel.add(Box.createRigidArea(new Dimension(5,0)));

		JButton b_cancel = new JButton("Cancel");
		b_cancel.addActionListener(this);
		b_cancel.setActionCommand("CANCEL");
		bottomPanel.add(b_cancel);

		Panel mainPanel = new Panel();		
		mainPanel.setLayout(new BoxLayout(mainPanel , BoxLayout.PAGE_AXIS));
		mainPanel.add(topPanel);

		mainPanel.add(dilutionPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0,5)));

		mainPanel.add(bottomPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0,5)));


		add(mainPanel);				
	}

	public void updateDisplayFromSettings() {		
		dilutionPanel.updateFromSettings(settings);
		fieldsTableModel.updateTableFromSettings();
		cfuTypeTableModel.updateTableFromSettings();
	}

	public void updateSettingsFromDisplay() {		
		//copy values from the JFrame components to the settings instance
		settings.setNFIELDS(fieldsTable.getRowCount());
		FieldsTableModel mf = (FieldsTableModel) fieldsTable.getModel();	
		mf.updateSettingsFromTable();
		
		
		settings.setNCFUTYPES(cfuTypeTable.getRowCount());				
		CfuTypeTableModel mcfu = (CfuTypeTableModel) cfuTypeTable.getModel();
		mcfu.updateSettingsFromTable();
		//TODO update is called each time a change is made in the table... This is probably not a very good idea, because settings will be
		//updated even if cancel is hit
		
		settings.setNROWS(dilutionPanel.getNROWS());
		settings.setNCOLS(dilutionPanel.getNCOLS());
		settings.setDilutionScheme(dilutionPanel.getDilutionScheme());
		settings.setVolume(dilutionPanel.getVolume());

		settings.setDilution(dilutionPanel.getDilution());		
	}
	
	public void actionPerformed(ActionEvent evt) {
		String action = evt.getActionCommand();

		if (action == "ADDFIELD") {
			FieldsTableModel mod = (FieldsTableModel) fieldsTable.getModel();
			mod.add();
			return;
		}
		
		if (action == "RMFIELD") {
			ArrayList<Integer> index = new ArrayList<Integer>();
			int[] pos = fieldsTable.getSelectedRows();
			for(int i=0;i<pos.length;i++) index.add(pos[i]);

			FieldsTableModel mod = (FieldsTableModel) fieldsTable.getModel();
			mod.delete(index);
			return;
		}
		
		if (action == "ADDCFUTYPE") {
			CfuTypeTableModel mod = (CfuTypeTableModel) cfuTypeTable.getModel();
			mod.add();
			return;
		}
		
		if (action == "RMCFUTYPE") {
			ArrayList<Integer> index = new ArrayList<Integer>();
			int[] pos = cfuTypeTable.getSelectedRows();
			for(int i=0;i<pos.length;i++) index.add(pos[i]);

			CfuTypeTableModel mod = (CfuTypeTableModel) cfuTypeTable.getModel();
			mod.delete(index);
			return;
		}

		if (action == "SAVE") {
			if(!((CfuTypeTableModel) cfuTypeTable.getModel()).sanityCheck()) return;
			if(!((FieldsTableModel) fieldsTable.getModel()).sanityCheck()) return;
			
			updateSettingsFromDisplay();
			
			if(status == SetExperimentSettings.CREATE) {
				//ask for file name only if creation of a new project
				File f = null;
				if(settings.getPath()!=null && settings.getFileName()!=null) f = new File(settings.getPath()+settings.getFileName());

				String[] p = CountDrops.createFromFileChooser(this, f ,
						new FileNameExtensionFilter("Configuration files (*.txt, *.cfg)", "cfg", "txt"));
				if(p != null) {
					settings.setPath(p[1]);
					settings.setFileName(p[0]);
				}
			}				
			//quit design setting interface  : saving will be performed by DountDrops			
			setVisible(false);
			return;
		}
		
		if(action=="LOAD") {
			//TODO give last file to file chooser instead of null
			String[] p = CountDrops.getFromFileChooser("Load experiment settings",this,null,new FileNameExtensionFilter("Configuration files (*.txt, *.cfg)", "cfg", "txt")); 			
			if(p!=null) {
				String filename = p[0];
				String path     = p[1];

				System.out.println(filename);				
				System.out.println(path);				
				settings.setPath(path);
				settings.setFileName(filename);

				try{
					settings.read();
					updateDisplayFromSettings();
					settings.setPath(null);
					settings.setFileName(null); //set path and file name back to null, so that user will be ask where to save settings
				} catch (Exception e) {					
				}
			}
			return;
		}

		if (action == "CANCEL") {
			status = SetExperimentSettings.CANCEL;
			setVisible(false);		
			return;
		}

	}
}
