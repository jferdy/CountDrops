package countdrops;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.filechooser.FileNameExtensionFilter;

import ij.ImagePlus;


public class AutoDetect extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	
	ImageWell img;
	ViewWellEvent viewWellEvent;
	ArrayList<ViewWellListener> listener;
	
	JCheckBox chkLightBackground,chkAutoSplit;	
	JFormattedTextField fieldContrastEnhance,fieldGBlurSigma,fieldMinSize,fieldMinCirc;
	JComboBox<String> comboBoxCFUtype;
	
	//default parameters for CFU detection
	private int      slice = -1;	
	private boolean  lightBackground = true;
	private int      minSize = 20;
	private double   minCirc = 0.2;
	private double   enhanceContrast = 0.3;
	private double   gBlurSigma = 1.0;
	private String   defaultCFUtype = "NA";

	public AutoDetect(ImageWell i,ViewWellEvent e,ArrayList<ViewWellListener> l) {
		super();
		setModal(true);
				
		img = i;
		slice = img.getImagePlus().getSlice();
		viewWellEvent = e;
		viewWellEvent.setSlice(slice);
		listener = l;
		
		this.setTitle("Auto-detection of CFUs -- well "+e.getWell().getName()+" slice "+slice);
		
		viewWellEvent.setAutoDetect(this);
		
		//read parameters from autoDetect.cfg (if it exists)
		readParameters();
		
		//left panel with form
		Panel left_p = new Panel();
		left_p.setLayout(new GridBagLayout());
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.fill = GridBagConstraints.HORIZONTAL;
        gbcL.insets = new Insets(4, 4, 4, 10);

        gbcL.gridx=0;
        gbcL.gridy=0;
		

		//Hashtable<Integer,JLabel> labelTable;

		NumberFormat formatInteger = NumberFormat.getInstance();
		formatInteger.setMaximumFractionDigits(0);
		formatInteger.setParseIntegerOnly(true);

		NumberFormat formatDouble = NumberFormat.getInstance();
		formatDouble.setMaximumFractionDigits(3);
		formatDouble.setParseIntegerOnly(false);	
		
		String[] cfuTypes = new String[i.getPlate().getCFUType().size()+1];
		cfuTypes[0] = "NA";
		for(int j=0;j<i.getPlate().getCFUType().size();j++) cfuTypes[j+1] = i.getPlate().getCFUType(j);		
		comboBoxCFUtype = new JComboBox<String>(cfuTypes);					  
		comboBoxCFUtype.setLightWeightPopupEnabled (false); //This line must be added otherwise combobox don't work properly.
													 //The issue comes from swing and awt libraries being mixed in the project.
		comboBoxCFUtype.setSelectedItem(defaultCFUtype);
		gbcL.gridx=1; gbcL.gridy=0; left_p.add(comboBoxCFUtype,gbcL);
		
		chkLightBackground = new JCheckBox("Light background");
		chkLightBackground.setSelected(lightBackground);
		gbcL.gridx=1; gbcL.gridy++; left_p.add(chkLightBackground,gbcL);

		fieldContrastEnhance = new JFormattedTextField(formatDouble);
		fieldContrastEnhance.setColumns(20);
		fieldContrastEnhance.setValue(enhanceContrast);
		gbcL.gridx=1; gbcL.gridy++; left_p.add(new JLabel("Enhance contrast"),gbcL);
		gbcL.gridx=1; gbcL.gridy++; left_p.add(fieldContrastEnhance,gbcL);

		fieldGBlurSigma = new JFormattedTextField(formatDouble);
		fieldGBlurSigma.setColumns(20);
		fieldGBlurSigma.setValue(gBlurSigma);
		gbcL.gridx=1; gbcL.gridy++; left_p.add(new JLabel("Gaussian blur sigma"),gbcL);
		gbcL.gridx=1; gbcL.gridy++; left_p.add(fieldGBlurSigma,gbcL);

		fieldMinSize = new JFormattedTextField(formatInteger);
		fieldMinSize.setColumns(20);
		fieldMinSize.setValue(minSize);
		gbcL.gridx=1; gbcL.gridy++; left_p.add(new JLabel("Minimum CFU size"),gbcL);
		gbcL.gridx=1; gbcL.gridy++; left_p.add(fieldMinSize,gbcL);

		fieldMinCirc = new JFormattedTextField(formatDouble);
		fieldMinCirc.setColumns(20);
		fieldMinCirc.setValue(minCirc);
		gbcL.gridx=1; gbcL.gridy++; left_p.add(new JLabel("Minimum CFU circularity"),gbcL);
		gbcL.gridx=1; gbcL.gridy++; left_p.add(fieldMinCirc,gbcL);

		//right panel with buttons
		Panel right_p = new Panel();		
		right_p.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        
		JButton bLoad = new JButton("Load settings");
		bLoad.setActionCommand("LOAD");
		bLoad.addActionListener(this);
		
		JButton bSave = new JButton("Save settings");
		bSave.setActionCommand("SAVE");
		bSave.addActionListener(this);
		
		JButton bCancel = new JButton("Close");
		bCancel.setActionCommand("CANCEL");
		bCancel.addActionListener(this);			
		
		JButton bApply = new JButton("Apply");
		bApply.setActionCommand("APPLY");
		bApply.addActionListener(this);
		
		JButton bApplyRow = new JButton("Apply to row");
		bApplyRow.setActionCommand("APPLYROW");
		bApplyRow.addActionListener(this);
		
		JButton bApplyColumn = new JButton("Apply to column");
		bApplyColumn.setActionCommand("APPLYCOLUMN");
		bApplyColumn.addActionListener(this);
		
		JButton bApplyPlate = new JButton("Apply to plate");
		bApplyPlate.setActionCommand("APPLYPLATE");
		bApplyPlate.addActionListener(this);
		
		gbc.gridx=0;		
		gbc.gridy=0; right_p.add(bApply,gbc);
		gbc.gridy++; right_p.add(bApplyRow,gbc);
		gbc.gridy++; right_p.add(bApplyColumn,gbc);
		gbc.gridy++; right_p.add(bApplyPlate,gbc);
		
		gbc.gridy++; right_p.add(Box.createRigidArea(new Dimension(0,5)),gbc);
		gbc.gridy++; right_p.add(bLoad,gbc);
		gbc.gridy++; right_p.add(bSave,gbc);
		gbc.gridy++; right_p.add(bCancel,gbc);
		
		//main panel
		Panel p = new Panel();
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
		p.add(left_p);
		p.add(right_p);
		add(p);
		
		
//		if(e!=null) {			
//			setLocation(e.getLocation());
//			//TODO does not seem to work... dialog is not on the same screen than ViewWell when not on default screen
//		} else {
//			setLocationRelativeTo(null);
//		}
		
		pack();		
	}

	public void updateParametersFromDisplay() {		
		defaultCFUtype = (String) (comboBoxCFUtype.getSelectedItem());
		lightBackground = chkLightBackground.isSelected();
		gBlurSigma = Double.parseDouble(fieldGBlurSigma.getValue().toString());
		enhanceContrast = Double.parseDouble(fieldContrastEnhance.getValue().toString());
		minCirc    = Double.parseDouble(fieldMinCirc.getValue().toString()); 				
		minSize    = Integer.parseInt(fieldMinSize.getValue().toString());
	}
	
	public void updateDisplayFromParameters() {		 
		comboBoxCFUtype.setSelectedItem(defaultCFUtype);		
		chkLightBackground.setSelected(lightBackground);
		fieldGBlurSigma.setValue(gBlurSigma);
		fieldContrastEnhance.setValue(enhanceContrast);
		fieldMinCirc.setValue(minCirc);
		fieldMinSize.setValue(minSize);
	}
	
	public void readParameters() {
		PlateSettings settings = img.getPlate().getSettings(); 
		File f = null;
		if(settings.getPath()!=null && settings.getFileName()!=null) {
			String p = settings.getPath();
			f = new File(p+File.separator+"autoDetect.cfg");
		}
		readParameters(f);		
	}
	
	public void readParameters(File f) {	
		if(!f.exists()) return;
		//read parameters from file

		ArrayList<String> content = new ArrayList<String>();
		try{
			FileInputStream fis = new FileInputStream(f.getAbsolutePath());
			Scanner scanner = new Scanner(fis);

			//reading file line by line using Scanner in Java
			while(scanner.hasNextLine()){
				content.add(scanner.nextLine());
			}    
			scanner.close();
		} catch(Exception ex) {
			return;
		}
		
		ArrayList<String> tags = new ArrayList<String>();
		tags.add("LIGHT BACKGROUND"); 		//0
		tags.add("MIN SIZE");         		//1
		tags.add("MIN CIRCULARITY");  		//2
		tags.add("ENHANCE CONTRAST"); 		//3
		tags.add("GAUSSIAN BLUR SIGMA");    //4
		tags.add("DEFAULT CFU TYPE");		//5
		
		//int pos = -1;
		for(int line = 0;line<content.size();line++) {
			String[] cells = content.get(line).split(";");	
			cells[0] = cells[0].trim();
			System.out.println("["+cells[0]+"]");
			if(cells[0].length()>0) {					
				cells[0] = cells[0].toUpperCase();
				if(cells[0].equals(tags.get(0))) lightBackground = cells[1].equals("true");
				if(cells[0].equals(tags.get(1))) minSize = Integer.parseInt(cells[1]);
				if(cells[0].equals(tags.get(2))) minCirc = Double.parseDouble(cells[1]);
				if(cells[0].equals(tags.get(3))) enhanceContrast = Double.parseDouble(cells[1]);
				if(cells[0].equals(tags.get(4))) gBlurSigma = Double.parseDouble(cells[1]);
				if(cells[0].equals(tags.get(5))) defaultCFUtype = cells[1];				
			}
		}

	}
	
	public int apply(ImagePlus imp,Well w) {
		int slice = imp.getSlice(); //detection will be performed on currently selected slice
		return apply(imp,w,slice);	
	}
	
	public int apply(ImagePlus imp,Well w,int sl) {
		//TODO apply to current slice, not default slice so that when autodetect is applied to plate, the same slice is always analyzed.
		
		//pre-existing CFU should probably always been deleted before detection		
		w.deleteAllCFU();	
		w.detectCFU(imp.duplicate(),sl,gBlurSigma,enhanceContrast,lightBackground,minSize,minCirc,defaultCFUtype);
		
		return w.getNbCFU();
	}
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		String cmd = evt.getActionCommand();
		
		if(cmd.equals("APPLY")) {
			updateParametersFromDisplay();
			img.deselectAllCFU();
			int pos = apply(img.getImagePlus(),img.getWell());
			for(int i=pos;i<img.getWell().getNbCFU();i++) img.selectCFU(i);
			
			img.drawCFU();			
			for (ImageWellListener hl : img.getListeners()) {
				hl.CFUremoved();
				hl.CFUadded();
				hl.CFUedited();
				hl.SelectionHasChanged();	    			
			}
			return;
		}
		
		if(cmd.equals("APPLYROW") || cmd.equals("APPLYCOLUMN") || cmd.equals("APPLYPLATE")) {
			updateParametersFromDisplay();
			
			for (ViewWellListener l : listener) {
				if(cmd.equals("APPLYROW"))    l.autoDetectRow(viewWellEvent);
				if(cmd.equals("APPLYCOLUMN")) l.autoDetectColumn(viewWellEvent);
				if(cmd.equals("APPLYPLATE"))  l.autoDetectPlate(viewWellEvent);
			}
			//wait until autoDetect is done								
			img.drawCFU();			
			for (ImageWellListener hl : img.getListeners()) {				
				hl.CFUadded();				
				hl.CFUedited();
				hl.SelectionHasChanged();
			}			
			return;
		}
		
		if(cmd.equals("CANCEL")) {
			viewWellEvent.setAutoDetect(null);
			setVisible(false);
			return;
		}
		
		if(cmd.equals("LOAD")) {
			PlateSettings s = img.getPlate().getSettings();
			File f = new File(s.getPath()+File.separator+s.getFileName());
			String[] p = CountDrops.getFromFileChooser("Load autodetect settings",(Component) this,f,new FileNameExtensionFilter("Configuration files (*.txt, *.cfg)", "cfg", "txt"));
			if(p==null) return;
			f = new File(p[1]+File.separator+p[0]);
			if(!f.exists())	return;
			
			readParameters(f);
			updateDisplayFromParameters();
			return;			
		}
		
		if(cmd.equals("SAVE")) {
			PlateSettings settings = img.getPlate().getSettings(); 
			File f = null;
			if(settings.getPath()!=null && settings.getFileName()!=null) {
				String p = settings.getPath();
				f = new File(p+File.separator+"autoDetect.cfg");
			} else {
				return;
			}
			updateParametersFromDisplay();
			try{
				PrintWriter writer = new PrintWriter(f.getAbsolutePath(), "UTF-8");
				if(lightBackground) {
					writer.println("LIGHT BACKGROUND;true");
				} else {
					writer.println("LIGHT BACKGROUND;false");
				}
				writer.println("MIN SIZE;"+minSize);
				writer.println("MIN CIRCULARITY;"+minCirc);
				writer.println("ENHANCE CONTRAST;"+enhanceContrast);
				writer.println("GAUSSIAN BLUR SIGMA;"+gBlurSigma);
				writer.println("DEFAULT CFU TYPE;"+defaultCFUtype);				
				writer.close();
			} catch(Exception ex) {

			}							
			return;
		}

	}

}
