package countdrops;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
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
	private SampleID sampleID;

	private JTable fieldsTable,cfuTypeTable;
	private FieldsTableModel fieldsTableModel; 
	private CfuTypeTableModel cfuTypeTableModel;
	private SetPlateDilutionSettings dilutionPanel;
	private JComboBox<String> sampleIDfields;	
	private JList<String> sampleIDList;
	
	private int status;
	
	static int CREATE = 1;
	static int EDIT = 2;
	static int CANCEL = 0;
	static int FAILED = -1;

	//constructor
	public SetExperimentSettings(PlateSettings s,SampleID sid,int st) {
		super();		
				
		status = st;
		if(status!=CREATE && status!=EDIT && status!=FAILED && status!=CANCEL) return;
		
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		
		settings = s;	
		sampleID = sid;
		
		setPanel();		
		pack();
	}

	public SetExperimentSettings(int st) {		
		super();
				
		status = st;
		if(status!=CREATE && status!=EDIT && status!=FAILED && status!=CANCEL) return;
				
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		
		settings = new PlateSettings();
				
		// setting default values
		settings.setDefault();
		sampleID = new SampleID(settings);
		
		setPanel();		
		pack();		
	}

	public PlateSettings getSettings() {
		updateSettingsFromDisplay();
		return settings;	
	}
	
	public SampleID getSampleID() {
		updateSampleIDFromDisplay();
		return sampleID;	
	}
	
	public int getStatus() {
		return status;
	}
	
	public void setPanel() {
		//this.setResizable(false);
		
		fieldsTableModel = new FieldsTableModel(settings,status);
		fieldsTable = new JTable(fieldsTableModel);				
		fieldsTable.setCellSelectionEnabled(true);

				

		String[] fieldTypes = new String[] {"String","Integer","Boolean","Float"};
		JComboBox<String> comboBox = new JComboBox<String>(fieldTypes);
		//comboBox.setSelectedIndex(1);	    
		comboBox.setLightWeightPopupEnabled (false); //This line must be added otherwise combobox don't work properly.
													 //The issue comes from swing and awt libraries being mixed in the project.

		TableColumn fieldsTypeColumn = fieldsTable.getColumnModel().getColumn(1);
		DefaultCellEditor editor = new DefaultCellEditor(comboBox);
		fieldsTypeColumn.setCellEditor(editor);	    
		DefaultTableCellRenderer renderer =
				new DefaultTableCellRenderer();
		renderer.setToolTipText("Choose the type of field");
		fieldsTypeColumn.setCellRenderer(renderer);
		fieldsTable.getColumnModel().getColumn(0).setPreferredWidth(12*10);
		fieldsTable.getColumnModel().getColumn(1).setPreferredWidth(12*3);
		fieldsTable.getColumnModel().getColumn(2).setPreferredWidth(12*6);
		fieldsTable.getColumnModel().getColumn(3).setPreferredWidth(12*23);				
		fieldsTable.setFillsViewportHeight(true);
		JScrollPane fieldsTableScrollPanel = new JScrollPane(fieldsTable,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);						
		
		TableColumn fieldsNameColumn = fieldsTable.getColumnModel().getColumn(0);
		DefaultCellEditor editorName = new DefaultCellEditor(new JTextField());
		editorName.addCellEditorListener(new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent e) {
            	System.out.print("pof");
            	if(fieldsTable.getSelectedColumn()==0) updateSampleIDFieldsFromTable();
            }

            @Override
            public void editingCanceled(ChangeEvent e) {
            }
			
        });	    
		fieldsNameColumn.setCellEditor(editorName);
		
		ImageIcon icon_add = CountDrops.getIcon("list-add.png");	
		ImageIcon icon_rm = CountDrops.getIcon("list-remove.png");
		
		JButton b_AddField = null;
		JButton b_RmField  = null;		
		if(status == SetExperimentSettings.CREATE) {
			b_AddField = new JButton(icon_add);
			b_AddField.addActionListener(this);
			b_AddField.setActionCommand("ADDFIELD");
			b_AddField.setToolTipText("Add a new plate description field");			
			
			b_RmField = new JButton(icon_rm);
			b_RmField.addActionListener(this);
			b_RmField.setActionCommand("RMFIELD");
			b_RmField.setToolTipText("Remove the selected plate description field");						
		}					

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
		cfuTypeTable.setFillsViewportHeight(true);
		JScrollPane cfuTypeTableScrollPanel = new JScrollPane(cfuTypeTable,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);				

		JButton b_RmCfuType  = null;
		JButton b_AddCfuType = null; 
		if(status == SetExperimentSettings.CREATE) {
			b_AddCfuType = new JButton(icon_add);
			b_AddCfuType.addActionListener(this);
			b_AddCfuType.setActionCommand("ADDCFUTYPE");
			b_AddCfuType.setToolTipText("Add a new CFU type");

			b_RmCfuType = new JButton(icon_rm);
			b_RmCfuType.addActionListener(this);
			b_RmCfuType.setActionCommand("RMCFUTYPE");
			b_RmCfuType.setToolTipText("Remove the selected CFU type");
		}
						
		FieldsTableModel mf = (FieldsTableModel) fieldsTable.getModel();
		String[] fieldsname = new String[mf.getRowCount()+3];
		fieldsname[0] = "WELL";
		fieldsname[1] = "ROW";
		fieldsname[2] = "COL";
		for(int i=0;i<mf.getRowCount();i++) fieldsname[i+3]  = mf.getName(i);
				
		sampleIDfields = new JComboBox<String>(fieldsname);		
		
		DefaultListModel<String> sampleIDListModel = new DefaultListModel<String>();
		for(int i=0;i<sampleID.getNbFields();i++) sampleIDListModel.addElement(sampleID.getField(i));
		sampleIDList = new JList<String>(sampleIDListModel); 
		sampleIDList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sampleIDList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		sampleIDList.setVisibleRowCount(-1);
		
		JButton b_AddSampleID = new JButton(icon_add);
		b_AddSampleID.addActionListener(this);
		b_AddSampleID.setActionCommand("ADDIDFIELD");
		b_AddSampleID.setToolTipText("Add a new field to define sample ID");			
		
		JButton b_RmSampleID = new JButton(icon_rm);
		b_RmSampleID.addActionListener(this);
		b_RmSampleID.setActionCommand("RMIDFIELD");
		b_RmSampleID.setToolTipText("Remove the selected field from the definition of sample ID");						

		//panel with plate dimension, volume and dilution settings
		dilutionPanel = new SetPlateDilutionSettings(settings);

		JButton b_load = null;
		if(status == CREATE) {
			b_load = new JButton("Load settings");
			b_load.addActionListener(this);
			b_load.setActionCommand("LOAD");
			b_load.setToolTipText("Load settings from another experiment or from a plate setting file");
		}

		JButton b_save = new JButton("OK");
		b_save.addActionListener(this);
		b_save.setActionCommand("SAVE");
		b_save.setToolTipText("Save settings.");
		
		JButton b_cancel = new JButton("Cancel");
		b_cancel.addActionListener(this);
		b_cancel.setActionCommand("CANCEL");
		

		//populate panels *******************************************
		//left panel contains fieldsTable
		
		JPanel fieldPanel = new JPanel();		
		fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.LINE_AXIS));		
		JPanel fieldPanel_left = new JPanel();
		fieldPanel_left.setLayout(new BoxLayout(fieldPanel_left, BoxLayout.PAGE_AXIS));			
		fieldPanel_left.add(new JLabel("Fields which should be combined to uniquely describe plate images"));
		fieldPanel_left.add(fieldsTableScrollPanel);
		JPanel fieldPanel_right = new JPanel();		
		fieldPanel_right.setLayout(new BoxLayout(fieldPanel_right, BoxLayout.PAGE_AXIS));
		fieldPanel_right.add(Box.createVerticalGlue());
		if(status == SetExperimentSettings.CREATE) {
			b_AddField.setMaximumSize(new Dimension(30,30));
			b_RmField.setMaximumSize(new Dimension(30,30));
			fieldPanel_right.add(b_AddField);
			fieldPanel_right.add(Box.createRigidArea(new Dimension(0,5)));
			fieldPanel_right.add(b_RmField);
			fieldPanel_right.add(Box.createVerticalGlue());
		}		
		fieldPanel.add(fieldPanel_left);
		fieldPanel.add(fieldPanel_right);
		
		JPanel cfuPanel = new JPanel();
		cfuPanel.setLayout(new BoxLayout(cfuPanel, BoxLayout.LINE_AXIS));		
		JPanel cfuPanel_left = new JPanel();
		cfuPanel_left.setLayout(new BoxLayout(cfuPanel_left, BoxLayout.PAGE_AXIS));
		cfuPanel_left.add(new JLabel("Types of CFU"));		
		cfuPanel_left.add(cfuTypeTableScrollPanel);		
		JPanel cfuPanel_right = new JPanel();		
		cfuPanel_right.setLayout(new BoxLayout(cfuPanel_right, BoxLayout.PAGE_AXIS));
		cfuPanel_right.add(Box.createVerticalGlue());
		if(status == SetExperimentSettings.CREATE) {			
			b_AddCfuType.setMaximumSize(new Dimension(30,30));
			b_RmCfuType.setMaximumSize(new Dimension(30,30));
			cfuPanel_right.add(b_AddCfuType);
			cfuPanel_right.add(Box.createRigidArea(new Dimension(0,5)));
			cfuPanel_right.add(b_RmCfuType);
			cfuPanel_right.add(Box.createVerticalGlue());
		}				
		cfuPanel.add(cfuPanel_left);
		cfuPanel.add(cfuPanel_right);
				
		JPanel samplePanel = new JPanel();
		samplePanel.setLayout(new BoxLayout(samplePanel, BoxLayout.PAGE_AXIS));		
		
		JPanel samplePanel_top = new JPanel();
		samplePanel_top.setLayout(new BoxLayout(samplePanel_top, BoxLayout.PAGE_AXIS));
		samplePanel_top.add(Box.createVerticalGlue());
		JLabel sampleIDLab = new JLabel("Select which fields should be combined to uniquely describe biological samples:");
		sampleIDLab.setAlignmentX(CENTER_ALIGNMENT);
		samplePanel_top.add(sampleIDLab);
		sampleIDList.setAlignmentX(CENTER_ALIGNMENT);
		samplePanel_top.add(sampleIDList);					
		
		b_AddSampleID.setMaximumSize(new Dimension(15,15));
		b_RmSampleID.setMaximumSize(new Dimension(30,15));
		sampleIDfields.setMaximumSize(new Dimension(30,15));
		JPanel samplePanel_bottom = new JPanel();		
		samplePanel_bottom.setLayout(new FlowLayout());
		samplePanel_bottom.add(Box.createHorizontalGlue());
		samplePanel_bottom.add(sampleIDfields);
		samplePanel_bottom.add(Box.createRigidArea(new Dimension(5,0)));
		samplePanel_bottom.add(b_AddSampleID);
		samplePanel_bottom.add(Box.createRigidArea(new Dimension(5,0)));
		samplePanel_bottom.add(b_RmSampleID);
		samplePanel_bottom.add(Box.createHorizontalGlue());		
		
		samplePanel.add(samplePanel_top);
		samplePanel.add(samplePanel_bottom);
		samplePanel.add(Box.createVerticalGlue());
		
//		fieldPanel.setMaximumSize(new Dimension(100,100));
//		cfuPanel.setMaximumSize(new Dimension(100,100));
//		samplePanel.setMaximumSize(new Dimension(100,100));
		
		JPanel blablaPanel = new JPanel();
		blablaPanel.setLayout(new BoxLayout(blablaPanel, BoxLayout.LINE_AXIS));
		JPanel blablaPanel_left = new JPanel();
		blablaPanel_left.setLayout(new BoxLayout(blablaPanel_left, BoxLayout.PAGE_AXIS));	
		JLabel txt = new JLabel("<html>"
				+ "<h1>Some hints on how to describe your experiment</h1>"				
				+ "<p>You need to define enough fields so that unique combinations of them describe each plate image. <i>Two different plate images must not have the same combination of fields!</i></p><br/>"				
				+ "<p>Samples ID are defined by the same fields than the plate images, to which you can add well, row or column positions.</p><br/>"
				+ "<p>A typical case is when biological samples are each arranged in a well inside 96 wells plates, and plated at various dilutions with several plating replicates for each dilution. "
				+ "Fields should then be \"Plate Number\", which is the ID of the plate that contains the biological samples, \"Dilution\" and \"Replicate\", which is the plating replicate "
				+ "number. Sample ID should be \"Plate Number\" combined to \"WELL\".</p>"
				+ "<p><i>Carefull:</i> Adding or removing fields means that you change the name of plate images, and therefore the folders in which data are stored. "
				+ "You will therefore not be able to change easily fields (and CFU types) after having started to count CFUs! The definition of sample ID can conversely be changed at any time.</p>"				
				+ "</html>");
		blablaPanel_left.add(txt);
		blablaPanel.add(blablaPanel_left);
		blablaPanel.add(Box.createRigidArea(new Dimension(70,0)));
		
		blablaPanel_left.setBackground(Color.decode("#DDDDEE"));
		
		JPanel topPanel = new JPanel();		
		topPanel.setLayout(new GridLayout(0,2));				
		topPanel .add(fieldPanel);		
		topPanel .add(cfuPanel);		
		topPanel .add(blablaPanel);
		topPanel .add(samplePanel);
		
		//bottom panel with load, save and cancel buttons
		JPanel bottomPanel = new JPanel();		
		bottomPanel.setLayout(new BoxLayout(bottomPanel , BoxLayout.LINE_AXIS));

		if(status == CREATE) {
			bottomPanel.add(b_load);
			bottomPanel.add(Box.createHorizontalGlue());
		}
		bottomPanel.add(b_save);
		bottomPanel.add(Box.createRigidArea(new Dimension(5,0)));
		bottomPanel.add(b_cancel);

		JPanel mainPanel = new JPanel();		
		mainPanel.setLayout(new BoxLayout(mainPanel , BoxLayout.PAGE_AXIS));
		mainPanel.add(topPanel);
		
		mainPanel.add(dilutionPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0,5)));

		mainPanel.add(bottomPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0,5)));

		mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		add(mainPanel);				
	}

	private void updateDisplayFromSettings() {		
		dilutionPanel.updateFromSettings(settings);
		fieldsTableModel.updateTableFromSettings();
		cfuTypeTableModel.updateTableFromSettings();
	}

	private void updateSettingsFromDisplay() {		
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
	
	private void updateSampleIDFromDisplay() {
		DefaultListModel<String> lm = (DefaultListModel<String>) sampleIDList.getModel();			
		ArrayList<String> f = new ArrayList<String>(); 
		for(int i=0;i<lm.getSize();i++) f.add(lm.get(i));	
		sampleID.update(f);			
	}
	
	private void updateSampleIDFieldsFromTable() {
		//retrieve fields from table
		FieldsTableModel mod = (FieldsTableModel) fieldsTable.getModel();
		ArrayList<String> fields = new ArrayList<String>();
		
		fields.add("WELL");
		fields.add("COL");
		fields.add("ROW");
		for(int i=0;i<mod.getRowCount();i++) {
			fields.add(mod.getName(i));
		}
		
		//check that all ID fields are still in table
		//remove them if not
		DefaultListModel<String> lm = (DefaultListModel<String>) sampleIDList.getModel();
		for(int i=lm.getSize()-1;i>=0;i--) {
			String f = lm.get(i);	
			if(! fields.contains(f)) lm.remove(i);
		}
		
		//changes combo box values
		String ffields[] = new String[fields.size()];
		for(int i=0;i<fields.size();i++) ffields[i] = fields.get(i);
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>( ffields );		
		sampleIDfields.setModel( model );

	}
	
//	
//	private void updateDisplayFromSampleID() {
//		
//	}
	
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
			
			updateSampleIDFieldsFromTable();
			return;
		}
		
		if (action == "ADDIDFIELD") {
			String field = (String) sampleIDfields.getSelectedItem();
			DefaultListModel<String> lm = (DefaultListModel<String>) sampleIDList.getModel();			
			ArrayList<String> f = new ArrayList<String>(); 
			for(int i=0;i<lm.getSize();i++) f.add(lm.get(i));
			
			if(!f.contains(field)) {
				lm.addElement(field);
				f.add(field);
			}
			sampleID.update(f);			
			return;
		}
		
		if (action == "RMIDFIELD") {
			int pos = sampleIDList.getSelectedIndex();
			DefaultListModel<String> lm = (DefaultListModel<String>) sampleIDList.getModel();
			lm.remove(pos);
			ArrayList<String> f = new ArrayList<String>(); 
			for(int i=0;i<lm.getSize();i++) f.add(lm.get(i));
			sampleID.update(f);
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
