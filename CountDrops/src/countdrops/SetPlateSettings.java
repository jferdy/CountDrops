package countdrops;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;


public class SetPlateSettings extends JDialog implements ActionListener {
	private static final long serialVersionUID = -1715863936875920604L;
	private PlateSettings settings;	
	private ArrayList<Component> fieldsArray = new ArrayList<Component>();
	
	private SetPlateDilutionSettings dilutionPanel;	

	private ImagePicture img;	
	
	private int status;

	static int CREATE = 1;
	static int EDIT = 2;
	static int CANCEL = 0;
	static int FAILED = -1;
	
	//constructor
	public SetPlateSettings(PlateSettings s,ImagePicture i, boolean createPlate) {
		super();	
		
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
				
		settings = s;
		img = i;
		if(createPlate && img==null) return; 
		
		if(img!=null) img.getImageWindow().setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
				
		dilutionPanel = new SetPlateDilutionSettings(settings);
		if(createPlate) {
			status = SetPlateSettings.CREATE;

			this.setTitle("Create a new plate for image "+img.getImagePlus().getTitle());			
			setLocationRelativeTo(img.getImageWindow());
			setLocation(img.getImageWindow().getWidth()+50,100);
												
			dilutionPanel.setEnableNrowsNcolumns(true);
			dilutionPanel.addListener(img.getPlateDilutionSettingsListener());
			img.setTriangle(new PlateTriangle(settings,img.getImagePlus()));

			//TODO not sure setAlwaysOnTop is really the good way of doing this, but toFront does not work... 
			// Sure now this is a very bad idea, because if the ImagePicture is large the dialog might end up totally
			// hidden behind it.
			//img.getImageWindow().setAlwaysOnTop(true);
						
		} else {
			status = SetPlateSettings.EDIT;
			
			this.setTitle("Edit settings for plate "+settings.getName());
			
			dilutionPanel.setEnableNrowsNcolumns(false);
		}
		
		Panel mainPanel = new Panel();		
		mainPanel.setLayout(new BoxLayout(mainPanel , BoxLayout.PAGE_AXIS));

		mainPanel.add(setFieldPanel());

		mainPanel.add(dilutionPanel);		

		Panel bottomPanel = new Panel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel , BoxLayout.LINE_AXIS));

		JButton b_save = new JButton("OK");
		b_save.addActionListener(this);
		b_save.setActionCommand("SAVE");		
		bottomPanel.add(b_save);
		bottomPanel.add(Box.createRigidArea(new Dimension(5,0)));

		JButton b_cancel = new JButton("Cancel");
		b_cancel.addActionListener(this);
		b_cancel.setActionCommand("CANCEL");
		bottomPanel.add(b_cancel);

		mainPanel.add(bottomPanel);
		add(mainPanel);

		pack();
		this.setLocationRelativeTo(null);
		setVisible(true);
	}

	private Panel setFieldPanel() {
		Panel p = new Panel();	

		p.setLayout(new GridBagLayout());		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(4,4,4,4);        
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx=0;
		gbc.gridy=0;

		NumberFormat formatDouble = NumberFormat.getInstance();
		formatDouble.setMaximumFractionDigits(5);

		NumberFormat formatInteger = NumberFormat.getInstance();
		formatInteger.setMaximumFractionDigits(0);
		formatInteger.setParseIntegerOnly(true);

		fieldsArray.clear();

		for(int i=0;i<settings.getNFIELDS();i++) {
			String type = settings.getFieldsType().get(i);

			if(type.toUpperCase().equals("STRING")) {
				JFormattedTextField f = null;
				f = new JFormattedTextField();
				f.setColumns(25);
				f.setValue(settings.getFieldsValue().get(i));
				fieldsArray.add(f);
			}
			if(type.toUpperCase().equals("FLOAT")) {
				JFormattedTextField f = null;
				f = new JFormattedTextField(formatDouble);
				f.setColumns(20);
				try {
					double x = Double.parseDouble(settings.getFieldsValue().get(i));
					f.setValue(x);
				} catch (Exception e) {
					f.setValue(-1.0);
				}
				fieldsArray.add(f);
			}
			if(type.toUpperCase().equals("INTEGER") || type.toUpperCase().equals("INT")) {
				JFormattedTextField f = null;
				f = new JFormattedTextField(formatInteger);
				f.setColumns(20);
				try {
					int x = Integer.parseInt(settings.getFieldsValue().get(i));
					f.setValue(x);
				} catch(Exception e) {
					f.setValue(-1);
				}
				fieldsArray.add(f);
			}
			if(type.toUpperCase().equals("BOOLEAN")) {
				JCheckBox f = new JCheckBox(); 
				if(settings.getFieldsValue().get(i).toUpperCase().equals("TRUE")) {
					f.setSelected(true);
				} else {
					f.setSelected(false);
				}
				fieldsArray.add(f);
			}


			gbc.gridx=0;			
			p.add(new JLabel(settings.getFieldsName().get(i)),gbc);
			gbc.gridx=1;
			p.add(fieldsArray.get(fieldsArray.size()-1),gbc);
			gbc.gridy++;
		}
		return p;		

	}

	public void close() {
		if(img!=null) img.getImageWindow().setModalExclusionType(Dialog.ModalExclusionType.NO_EXCLUDE);
	}

	public int getStatus() {
		return status;
	}
	
	public String getFieldValue(int i) {
		if(i<0 || i>=settings.getNFIELDS()) return null;
		
		String type = settings.getFieldsType().get(i).toUpperCase();
		if(type.equals("STRING") || type.equals("FLOAT") || type.equals("INTEGER") || type.equals("INT")) {
				JFormattedTextField f = (JFormattedTextField) fieldsArray.get(i);				
				return f.getValue().toString();
		}
		if(type.equals("BOOLEAN")) {
				JCheckBox f = (JCheckBox) fieldsArray.get(i);
				if(f.isSelected()) return "true";
				return "false";
		}		
		return null;
	}
	
	public void updateSettingsFromDisplay() {
		//copy values from the JFrame components to the settings instance
		//then save the whole thing in a file
		for(int i=0;i<settings.getNFIELDS();i++) {
			//TODO check that adequate value is provided!
			settings.setFieldsValue(i,getFieldValue(i));
		}
		settings.setNCOLS(dilutionPanel.getNCOLS());
		settings.setNROWS(dilutionPanel.getNROWS());
		settings.setDilutionScheme(dilutionPanel.getDilutionScheme());
		
		settings.setVolume(dilutionPanel.getVolume());
		settings.setDilution(dilutionPanel.getDilution());
		
	}

	public boolean doesPlateAlreadyExists() {	
		// Test for the existence of path **without changing field values in settings** !!
		// Plate path is constructed from actual path in PlateSettings with plate name replaced by the name deduced from field values 
		ArrayList<String> fields = new ArrayList<String>();
		for(int i=0;i<settings.getNFIELDS();i++) fields.add(getFieldValue(i));
		String path = settings.getPathFromFields(fields);
		
		File f = new File (path);
		if(f.exists()) return true;
		return false;		
	}
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		String action = evt.getActionCommand();


		if (action == "SAVE") {
			String oldName = settings.getName();
						
			if(!doesPlateAlreadyExists()) {
				//the path where settings are to be saved does not exist
				if(img!=null && img.getTriangle()!=null) {
					settings.setBox(img.getTriangle().getX(),img.getTriangle().getY());
					img.setTriangle(null);
				}				
				//update settings (saving will be done elsewhere)
				updateSettingsFromDisplay();
				settings.setPathAndNameFromFields();									
				settings.setFileName("config.cfg");

				setVisible(false);
				if(img!=null) img.getImageWindow().setAlwaysOnTop(false);
				return;
			} else {
				//the path where settings are to be saved does exist
				if(oldName!=null && oldName.equals(settings.getName()) && status == SetPlateSettings.EDIT) {
					//if settings are simply edited, saving can be done without changing path
					//update settings (saving will be done elsewhere)
					updateSettingsFromDisplay();
					settings.setPathAndNameFromFields();									
					settings.setFileName("config.cfg");
					
					setVisible(false);
					if(img!=null) img.getImageWindow().setAlwaysOnTop(false);
					return;
				} else {
					//if settings are created edited, they cannot be saved in a folder that already exists
					//TODO this prevent to have two plates with identical names for a single picture
					//But two distinct pictures can still have plates with the same name. This is probably to avoid...
					JOptionPane.showMessageDialog(this,"Plate cannot be created because a plate with the same name already exists for current picture.\n" +
							"Change field values and try again!", "Create a plate",JOptionPane.ERROR_MESSAGE);
				}
			}
		}

		if (action == "CANCEL") {
			settings = null;
			if(img!=null) img.setTriangle(null);
			
			status = SetPlateSettings.CANCEL;
			setVisible(false);
			//img.getImageWindow().setAlwaysOnTop(false);
			
			return;
		}
	}

}
