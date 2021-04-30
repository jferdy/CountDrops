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
import java.util.Hashtable;
import java.util.Scanner;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.filechooser.FileNameExtensionFilter;

import ij.ImagePlus;
import ij.process.ImageProcessor;


public class AutoDetect extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	
	ImageWell img;
	ViewWellEvent viewWellEvent;
	ArrayList<ViewWellListener> listener;
	
	JCheckBox chkDelete,chkLightBackground,chkAutoSplit;	
	JFormattedTextField fieldSensitivitySupNbValues;
	JSlider sliderMinCirc,sliderMinSize,sliderSensitivityInf,sliderSensitivitySupFrom,sliderSensitivitySupTo;
	
	//default parameters for CFU detection
	private int      slice = -1;
	private boolean  autoSplit = false;
	private boolean  lightBackground = true;
	private int      minSize = 20;
	private double   minCirc = 0.2;
	private double   sensitivityInf = 0.05;
	private double   sensitivitySupFrom = 0.1;
	private double   sensitivitySupTo = 0.5;
	private int      sensitivitySupNbValues = 10;

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
		
		//left panel with slider
		Panel left_p = new Panel();
		left_p.setLayout(new GridBagLayout());
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.fill = GridBagConstraints.HORIZONTAL;
        gbcL.insets = new Insets(4, 4, 4, 10);

        gbcL.gridx=0;
        gbcL.gridy=0;
		

		Hashtable<Integer,JLabel> labelTable;
		
		sliderMinSize = new JSlider(JSlider.HORIZONTAL,0,100,minSize/5);
		sliderMinSize.setMajorTickSpacing(10);
		sliderMinSize.setMinorTickSpacing(5);
		sliderMinSize.setPaintTicks(true);
		labelTable = new Hashtable<Integer,JLabel>();
		for(int j=0;j<=100;j+=20) {
		    labelTable.put(Integer.valueOf(j), new JLabel(String.valueOf(j*5)) );
		}
		sliderMinSize.setLabelTable(labelTable);
		sliderMinSize.setPaintLabels(true);
		gbcL.gridx=0; gbcL.gridy=0; left_p.add(new JLabel("Minimum size"),gbcL);
		gbcL.gridx=0; gbcL.gridy=1; left_p.add(sliderMinSize,gbcL);
		
		
		sliderMinCirc = new JSlider(JSlider.HORIZONTAL,0,100,(int)(minCirc*100));
		sliderMinCirc.setMajorTickSpacing(10);
		sliderMinCirc.setMinorTickSpacing(5);
		sliderMinCirc.setPaintTicks(true);
		labelTable = new Hashtable<Integer,JLabel>();
		for(int j=0;j<=100;j+=20) {
		    labelTable.put(Integer.valueOf(j), new JLabel(String.valueOf(((double) j)/100.0)) );
		}
		sliderMinCirc.setLabelTable(labelTable);
		sliderMinCirc.setPaintLabels(true);
		gbcL.gridx=1; gbcL.gridy=0; left_p.add(new JLabel("Minimum circularity"),gbcL);
		gbcL.gridx=1; gbcL.gridy=1; left_p.add(sliderMinCirc,gbcL);
				

		chkLightBackground = new JCheckBox("Light background");
		chkLightBackground.setSelected(lightBackground);
		gbcL.gridx=1; gbcL.gridy=2; left_p.add(chkLightBackground,gbcL);

		chkAutoSplit = new JCheckBox("Guess CFUs to be split");
		chkAutoSplit .setSelected(autoSplit);
		gbcL.gridx=0; gbcL.gridy=2; left_p.add(chkAutoSplit ,gbcL);
		
		NumberFormat formatInteger = NumberFormat.getInstance();
		formatInteger.setMaximumFractionDigits(0);
		formatInteger.setParseIntegerOnly(true);

		fieldSensitivitySupNbValues = new JFormattedTextField(formatInteger);
		fieldSensitivitySupNbValues.setColumns(20);
		fieldSensitivitySupNbValues.setValue(sensitivitySupNbValues);
		gbcL.gridx=1; gbcL.gridy=3; left_p.add(new JLabel("Number of values of maximum sensitivity"),gbcL);
		gbcL.gridx=1; gbcL.gridy=4; left_p.add(fieldSensitivitySupNbValues,gbcL);
		
		sliderSensitivityInf = new JSlider(JSlider.HORIZONTAL,0,50,(int) (sensitivityInf*100));
		sliderSensitivityInf.setMajorTickSpacing(10);
		sliderSensitivityInf.setMinorTickSpacing(5);
		sliderSensitivityInf.setPaintTicks(true);
		labelTable = new Hashtable<Integer,JLabel>();
		for(int j=0;j<=100;j+=20) {
		    labelTable.put(Integer.valueOf(j), new JLabel(String.valueOf(((double) j)/100.0)) );
		}
		sliderSensitivityInf.setLabelTable(labelTable);
		sliderSensitivityInf.setPaintLabels(true);
		gbcL.gridx=0; gbcL.gridy=3; left_p.add(new JLabel("Minimum sensitivity"),gbcL);
		gbcL.gridx=0; gbcL.gridy=4; left_p.add(sliderSensitivityInf,gbcL);
		
		
		sliderSensitivitySupFrom = new JSlider(JSlider.HORIZONTAL,0,100,(int) (sensitivitySupFrom*100));
		sliderSensitivitySupFrom.setMajorTickSpacing(10);
		sliderSensitivitySupFrom.setMinorTickSpacing(5);
		sliderSensitivitySupFrom.setPaintTicks(true);
		labelTable = new Hashtable<Integer,JLabel>();
		for(int j=0;j<=100;j+=20) {
		    labelTable.put(Integer.valueOf(j), new JLabel(String.valueOf(((double) j)/100.0)) );
		}
		sliderSensitivitySupFrom.setLabelTable(labelTable);
		sliderSensitivitySupFrom.setPaintLabels(true);
		gbcL.gridx=0; gbcL.gridy=5; left_p.add(new JLabel("Max sensitivity ranges from"),gbcL);
		gbcL.gridx=0; gbcL.gridy=6; left_p.add(sliderSensitivitySupFrom,gbcL);

		sliderSensitivitySupTo = new JSlider(JSlider.HORIZONTAL,0,100,(int) (sensitivitySupTo*100));
		sliderSensitivitySupTo.setMajorTickSpacing(10);
		sliderSensitivitySupTo.setMinorTickSpacing(5);
		sliderSensitivitySupTo.setPaintTicks(true);
		labelTable = new Hashtable<Integer,JLabel>();
		for(int j=0;j<=100;j+=20) {
		    labelTable.put(Integer.valueOf(j), new JLabel(String.valueOf(((double) j)/100.0)) );
		}
		sliderSensitivitySupTo.setLabelTable(labelTable);
		sliderSensitivitySupTo.setPaintLabels(true);
		gbcL.gridx=1; gbcL.gridy=5; left_p.add(new JLabel("Max sensitivity ranges ranges to"),gbcL);
		gbcL.gridx=1; gbcL.gridy=6; left_p.add(sliderSensitivitySupTo,gbcL);


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
		
		chkDelete = new JCheckBox("Delete CFUs before applying");
		
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
		gbc.gridy=0; right_p.add(chkDelete,gbc);
		gbc.gridy++; right_p.add(bApply,gbc);
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
		
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	public void updateParametersFromDisplay() {
		lightBackground = chkLightBackground.isSelected();
		minCirc = sliderMinCirc.getValue()/100.0;
		minSize = sliderMinSize.getValue()*5;
		sensitivityInf = sliderSensitivityInf.getValue()/100.0;
		sensitivitySupFrom = sliderSensitivitySupFrom.getValue()/100.0;
		sensitivitySupTo = sliderSensitivitySupTo.getValue()/100.0;
		sensitivitySupNbValues = Integer.parseInt(fieldSensitivitySupNbValues.getValue().toString());
	}
	
	public void updateDisplayFromParameters() {
		chkLightBackground.setSelected(lightBackground);
		sliderMinCirc.setValue((int)(100.0*minCirc));
		sliderMinCirc.setValue((int)(100.0*minCirc)); 
		sliderMinSize.setValue((int)(minSize/5.0));
		sliderSensitivityInf.setValue((int)(100.0*sensitivityInf));
		sliderSensitivitySupFrom.setValue((int)(100.0*sensitivitySupFrom));
		sliderSensitivitySupTo.setValue((int)(100.0*sensitivitySupTo));
		fieldSensitivitySupNbValues.setValue(sensitivitySupNbValues);
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
		tags.add("LIGHT BACKGROUND");
		tags.add("MIN SIZE");
		tags.add("MIN CIRCULARITY");
		tags.add("MIN SENSITIVITY");
		tags.add("MAX SENSITIVITY FROM");
		tags.add("MAX SENSITIVITY TO");
		tags.add("MAX SENSITIVITY NB VALUES");
		
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
				if(cells[0].equals(tags.get(3))) sensitivityInf = Double.parseDouble(cells[1]);
				if(cells[0].equals(tags.get(4))) sensitivitySupFrom = Double.parseDouble(cells[1]);
				if(cells[0].equals(tags.get(5))) sensitivitySupTo = Double.parseDouble(cells[1]);
				if(cells[0].equals(tags.get(6))) sensitivitySupNbValues = Integer.parseInt(cells[1]);
			}
		}

	}
	
	public int apply(ImagePlus imp,Well w) {
		return apply(imp,w,slice);	
	}
	
	public int apply(ImagePlus imp,Well w,int sl) {
		//TODO apply to current slice, not default slice so that when autodetect is applied to plate, the same slice is always analyzed.
		
		if(chkDelete.isSelected()) {
			w.deleteAllCFU();
		}
		
		//convert image to gray scale (slice sl should be selected)
		ImagePlus impCpy = w.convertImageToGrayLevels(imp,sl,chkLightBackground.isSelected());
		
		int firstCFU = w.getNbCFU();
		double delta = (sensitivitySupTo-sensitivitySupFrom)/(double) sensitivitySupNbValues;
		for(double s=sensitivitySupTo;s>=sensitivitySupFrom;s-=delta) {
			//detection on gray scale image
			//splitting is done once, after all contours have been detected !
			w.detectCFU(impCpy, sensitivityInf,s, chkLightBackground.isSelected(),false,minSize, minCirc);
		}		
		//split
		if(chkAutoSplit.isSelected()) {
			if(chkLightBackground.isSelected()) {
				ImageProcessor ip = impCpy.getProcessor();
				ip.invert();
				impCpy.setProcessor(ip);				
			}
			// w.splitToMax(impCpy,chkLightBackground.isSelected(),minSize, minCirc,false); // not bubble
			w.splitToMax(impCpy,firstCFU,chkLightBackground.isSelected(),minSize, minCirc,true); // bubble
		}
		impCpy.flush();
		return firstCFU;
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
				if(chkDelete.isSelected()) hl.CFUremoved();
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
				writer.println("MIN SENSITIVITY;"+sensitivityInf);
				writer.println("MAX SENSITIVITY FROM;"+sensitivitySupFrom);
				writer.println("MAX SENSITIVITY TO;"+sensitivitySupTo);
				writer.println("MAX SENSITIVITY NB VALUES;"+sensitivitySupNbValues);
				writer.close();
			} catch(Exception ex) {

			}							
			return;
		}

	}

}
