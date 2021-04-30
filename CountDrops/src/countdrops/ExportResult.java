package countdrops;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


public class ExportResult extends JDialog implements ActionListener, ListSelectionListener {
	private static final long serialVersionUID = 1L;
	
	PlateSettings settings = null;
	SampleID sampleID = null;
	Well exampleWell = null;
	JLabel txtExample = new JLabel("?");
	
	DefaultListModel<String> modelFieldsID;
	JList<String> fieldsID;
	DefaultListModel<String> modelFieldsNonID;
	JList<String> fieldsNonID;
	
	
	static int OK = 1;
	static int CANCEL = 0;
	static int FAILED = -1;

	private int status = FAILED;

	JButton b_add,b_remove;
	
	public ExportResult(Experiment exp) {
		super();
		setModal(true);
		
		settings = exp.getSettings();
		sampleID = exp.getSampleID();		
		if(settings==null) return;
		if(sampleID==null) {
			ArrayList<String> v = new ArrayList<String>();
			v.add("WELL");
			sampleID = new SampleID(v,settings);
			exp.setSampleID(sampleID);
		}
	
		for(int i=0;i<exp.getNbPictures() && exampleWell==null;i++) {
			Picture p = exp.getPicture(i); 
			for(int j=0;j<p.getNbPlates() && exampleWell==null;j++) {
				Plate pl = p.getPlate(j);
				exampleWell = pl.getWell(0);
			}
		}
		
		Panel p = new Panel();
		p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));

		Panel p_top = new Panel();
		p_top.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        
		//construct list of fields already used in sample ID
		modelFieldsID = new DefaultListModel<String>();
		for(int i=0;i<sampleID.getNbFields();i++) modelFieldsID.addElement(sampleID.getField(i));
		fieldsID = new JList<String>(modelFieldsID); //data has type Object[]
		fieldsID.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		fieldsID.setLayoutOrientation(JList.VERTICAL);
		fieldsID.setVisibleRowCount(-1);
		fieldsID.addListSelectionListener(this);
		JScrollPane listScrollerID = new JScrollPane(fieldsID);
		listScrollerID.setPreferredSize(new Dimension(250, 80));
		
		gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weighty=0.0;
		p_top.add(new JLabel("Fields incorporated in sample ID"),gbc);		

		gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridheight = 2;
        gbc.weighty=1.0;
		p_top.add(listScrollerID,gbc);		
		
		//construct list of fields to be used to built sample ID
		modelFieldsNonID = new DefaultListModel<String>();
		if(!sampleID.getFields().contains("WELL")) {
			modelFieldsNonID.addElement("WELL");
		}
		if(!sampleID.getFields().contains("ROW")) {
			modelFieldsNonID.addElement("ROW");
		}
		if(!sampleID.getFields().contains("COLUMN")) {
			modelFieldsNonID.addElement("COLUMN");
		}
		for(int i=0;i<settings.getNFIELDS();i++) { 
			if(!sampleID.getFields().contains(settings.getFieldsName().get(i))) {
				modelFieldsNonID.addElement(settings.getFieldsName().get(i));				
			}
		}		 		
		fieldsNonID = new JList<String>(modelFieldsNonID); //data has type Object[]
		fieldsNonID.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		fieldsNonID.setLayoutOrientation(JList.VERTICAL);
		fieldsNonID.setVisibleRowCount(-1);		
		fieldsNonID.addListSelectionListener(this);
		JScrollPane listScrollerNonID = new JScrollPane(fieldsNonID);
		listScrollerNonID.setPreferredSize(new Dimension(250, 80));

		gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weighty=0.0;
		p_top.add(new JLabel("Fields you may use to construct sample ID"),gbc);		

		gbc.gridx = 2;
        gbc.gridy = 1;        
        gbc.gridheight = 2;
        gbc.weighty=1.0;
        p_top.add(listScrollerNonID,gbc);

		
		//load icons for add/remove buttons 
		ImageIcon icon_left = CountDrops.getIcon("go-previous.png");	
		ImageIcon icon_right = CountDrops.getIcon("go-next.png");
		
		//creates add/remove buttons
		b_add = new JButton(icon_left);
		b_add.setActionCommand("ADD");
		b_add.addActionListener(this);
		b_add.setFocusable(false);
		if(true) b_add.setEnabled(false);
		gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridheight = 1;
        gbc.weighty=0.0;
		p_top.add(b_add,gbc);
		
		b_remove = new JButton(icon_right);
		b_remove.setActionCommand("REMOVE");
		b_remove.addActionListener(this);
		b_remove.setFocusable(false);
		if(true) b_remove.setEnabled(false);
		gbc.gridx = 1;
        gbc.gridy = 2;
		p_top.add(b_remove,gbc);	
		
		if(exampleWell!=null) {
			txtExample.setText("Example of sample ID: "+sampleID.getSampleID(exampleWell));
		}
		gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridheight = 1;
        gbc.gridwidth = 3;
        gbc.weightx=1.0;
        gbc.weighty=0.0;		
		p_top.add(txtExample,gbc);
		
		p.add(p_top);
		
		JButton b_OK = new JButton("OK");
		b_OK.setActionCommand("OK");
		b_OK.addActionListener(this);
		b_OK.setFocusable(false);		
		
		JButton b_cancel = new JButton("Cancel");
		b_cancel.setActionCommand("CANCEL");
		b_cancel.addActionListener(this);
		b_cancel.setFocusable(false);
		
		
		Panel p_bottom = new Panel();
		p_bottom.setLayout(new BoxLayout(p_bottom, BoxLayout.LINE_AXIS));
		
		p_bottom.add(b_OK);
		p_bottom.add(b_cancel);		
		p.add(p_bottom);
		
		add(p);

		pack();
		this.setLocationRelativeTo(null);
		setVisible(true);
	}

	public int getStatus() {return status;}
	
	public void updateFieldsLists() {
		modelFieldsID.removeAllElements();
		modelFieldsNonID.removeAllElements();
		for(int i=0;i<sampleID.getNbFields();i++) {
			modelFieldsID.addElement(sampleID.getField(i));
		}
		if(!sampleID.getFields().contains("WELL")) {
			modelFieldsNonID.addElement("WELL");

		}
		if(!sampleID.getFields().contains("ROW")) {
			modelFieldsNonID.addElement("ROW");
		}
		if(!sampleID.getFields().contains("COLUMN")) {
			modelFieldsNonID.addElement("COLUMN");
		}
		for(int i=0;i<settings.getNFIELDS();i++) { 
			if(!sampleID.getFields().contains(settings.getFieldsName().get(i))) {
				modelFieldsNonID.addElement(settings.getFieldsName().get(i));				
			}
		}		 		
	}
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		if(evt.getActionCommand().equals("ADD")) {
			if(fieldsNonID.getSelectedIndex()>=0) {
				//first retrieve fields that are not selected
				String value = fieldsNonID.getSelectedValue();
				ArrayList<String> f = (ArrayList<String>) sampleID.getFields().clone();
				f.add(value);
				
				//update sample ID
				sampleID.update(f);				
				//update JList
				updateFieldsLists();
				txtExample.setText("Example of sample ID: "+sampleID.getSampleID(exampleWell));
			}

			return;
		}
		if(evt.getActionCommand().equals("REMOVE")) {
			if(fieldsID.getSelectedIndex()>=0) {
				//first retrieve fields that are not selected
				int pos = fieldsID.getSelectedIndex();
				ArrayList<String> f = new ArrayList<String>(); 
				for(int i=0;i<sampleID.getNbFields();i++) {
					if(i!=pos) {
						f.add(sampleID.getField(i));
					}
				}
				//update sample ID
				sampleID.update(f);
				//update JList
				updateFieldsLists();
				txtExample.setText("Example of sample ID: "+sampleID.getSampleID(exampleWell));
			}
			return;
		}
		if(evt.getActionCommand().equals("OK")) {
			status = ExportResult.OK;
			setVisible(false);
			return;
		}
		if(evt.getActionCommand().equals("CANCEL")) {
			status = ExportResult.CANCEL;
			setVisible(false);
			return;
		}		
	}

	@Override
	public void valueChanged(ListSelectionEvent evt) {
		if(evt.getSource().equals(fieldsID )) {
			//selection has changed in fieldsID
			if(fieldsID.getSelectedIndex()>=0) {
				//something selected : 
				fieldsNonID.clearSelection();
				b_remove.setEnabled(true);
				b_add.setEnabled(false);
			}
			return;
		}
		if(evt.getSource().equals(fieldsNonID )) {
			//selection has changed in fieldsID
			if(fieldsNonID.getSelectedIndex()>=0) {
				//something selected :
				fieldsID.clearSelection();
				b_remove.setEnabled(false);
				b_add.setEnabled(true);
			}
			return;
		}		
	}

}
