package countdrops;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;


public class ImageChooser extends JDialog implements ActionListener  {	
	private static final long serialVersionUID = 1L;
	
	ArrayList<String> listExt = new ArrayList<String>();
	ArrayList<Integer> listExtNbImg = new ArrayList<Integer>();
	ArrayList<File> listImg = null;
	ArrayList<File> selectedImg = null;
	JCheckBox chkAll;
	JCheckBox[] chkExt;
	JButton     b_OK,b_cancel;
	
	public ImageChooser(ArrayList<File> l) {
		super(CountDrops.getGui(),"Add images from a folder");	
		
		listImg = l;		
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
					
		Panel pane = new Panel();
		pane.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 10);

        gbc.gridx=0;
        gbc.gridy=0;
		//pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		
		pane.add(new JLabel("Which type(s) of image do you want to add to your experiment?"),gbc); gbc.gridy++;
		pane.add(new JLabel("Images that have already been added will be ignored."),gbc); gbc.gridy++;
		//list file extensions
		for(int i=0;i<listImg.size();i++) {
			String name = listImg.get(i).getName();
			String extension = getExtension(name);
			if(extension!=null) {  
				if(!listExt.contains(extension)) {
					listExt.add(extension);
					listExtNbImg.add(1);
				}
				else {
					int pos = listExt.indexOf(extension);
					listExtNbImg.set(pos, listExtNbImg.get(pos)+1); 
				}
			}

		}
		//checkboxes
		chkAll = new JCheckBox("all ("+listImg.size()+" images)");
		pane.add(chkAll,gbc); gbc.gridy++;
		
		chkExt = new JCheckBox[listExt.size()];
		for(int i=0;i<listExt.size();i++) {
			chkExt[i]= new JCheckBox(listExt.get(i)+" ("+listExtNbImg.get(i)+" images)");
			chkExt[i].addActionListener(this);
			chkExt[i].setActionCommand("CHECKTYPE");
			pane.add(chkExt[i],gbc); gbc.gridy++;
		}
		
		//buttons
		b_OK = new JButton("OK");
		b_OK.addActionListener(this);
		b_OK.setActionCommand("OK");
		b_OK.setEnabled(false);
		
		b_cancel = new JButton("Cancel");
		b_cancel.addActionListener(this);
		b_cancel.setActionCommand("CANCEL");
		
		Panel paneBottom = new Panel();
		paneBottom.setLayout(new BoxLayout(paneBottom, BoxLayout.LINE_AXIS));
		paneBottom.add(b_OK);
		paneBottom.add(Box.createRigidArea(new Dimension(5,0)));
		paneBottom.add(b_cancel);		
		pane.add(paneBottom,gbc); gbc.gridy++;
		
		add(pane);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		
	}

	private String getExtension(String name) {
		int j = name.lastIndexOf('.');
		if (j > 0) {
			String ext = name.substring(j+1);
			return(ext);
		}
		return(null);
	}
	    
	public ArrayList<File> getImages() {
		return selectedImg;
	}
	
	//action listener
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		
		if(action.equals("CHECKTYPE")) {
			boolean test = false;
			for(int i=0;i<chkExt.length;i++) {
				if(chkExt[i].isSelected()) {
					test = true;			
				}
			}
			b_OK.setEnabled(test);
		}
		
		if(action.equals("OK")) {
			if(chkAll.isSelected()) {
				selectedImg = listImg;
			} else {			
				ArrayList<String> selectedExt = new ArrayList<String>();
				for(int i=0;i<chkExt.length;i++) {
					if(chkExt[i].isSelected()) {
						selectedExt.add(listExt.get(i));
					}
				}
				selectedImg = new ArrayList<File>();
				for(int i=0;i<listImg.size();i++) {
					if(selectedExt.contains(getExtension(listImg.get(i).getName()))) {
						selectedImg.add(listImg.get(i));
					}
				}
			}
			setVisible(false);
		}
		
		if(action.equals("CANCEL")) {
			selectedImg = null;
			setVisible(false);
		}
	}
}
