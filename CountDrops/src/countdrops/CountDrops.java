package countdrops;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.TreeSelectionModel;

//TODO what happens if load button is clicked when current experiment is no empty? Should check that current 
//project has been saved and ask user what to do if it has not. 

//the main class
public class CountDrops implements ActionListener, ViewWellListener {

	/**
	 * @param args
	 */
	//private static final String classPath = CountDrops.class.getResource("").getPath();
	private static ExperimentTree   experimentTree;
	private static JFrame       gui;    
	private static CountDrops   instance;

	private static JButton      buttonLoad,buttonEdit,buttonAdd,buttonAddFromFolder,buttonRemove,buttonSave,buttonNew,buttonAddPlate,buttonDeletePlate,buttonEditPlate,buttonClose,buttonExportResult,buttonOptions;	
	private static JCheckBox    chkShowImagePath,chkUnfold;
	private static JTextField   plateNamePattern;
	private static Experiment   experiment = null;
	private static File         lastFile = null;
	private static ImagePicture viewedImage = null; 
		
	private static String pathToR = "R";
	
	private static JFileChooser fc;

	//tree listeners	
	private static TreeSelectionListener treel = new TreeSelectionListener() {
		public void valueChanged(TreeSelectionEvent e) {    		
			//drawSelectedPlate();
			instance.updateButtons();
		}

	};   
	
	private static MouseListener treeml = new MouseListener() {
		@Override
		public void mouseClicked(MouseEvent event) {
			if (event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1) {
				drawSelectedPlate();
				instance.updateButtons();
			}
		}
		@Override
		public void mouseEntered(MouseEvent arg0) {}
		@Override
		public void mouseExited(MouseEvent arg0) {}
		@Override
		public void mousePressed(MouseEvent arg0) {}
		@Override
		public void mouseReleased(MouseEvent arg0) {}
	};
	
	//window listener that react to ImagePicture closing window
	private static WindowListener windowListener = new WindowListener() {
		@Override
		public void windowClosed(WindowEvent e) { 
			viewedImage = null;
			//unselect current image/plate so that next click is taken has a change in selection
			experimentTree.clearSelection();
		}
		@Override
		public void windowOpened(WindowEvent e) {}
		@Override
		public void windowDeactivated(WindowEvent e) {}
		@Override
		public void windowIconified(WindowEvent e) {}
		@Override
		public void windowClosing(WindowEvent e) {}
		@Override
		public void windowActivated(WindowEvent e) {}
		@Override
		public void windowDeiconified(WindowEvent e) {}
	};
	

	//*********
	public static void main(String[] args) {
		instance = new CountDrops();		

		
		gui = new JFrame();
		gui.setTitle("CountDrops");
		gui.setSize(1000, 500);
		gui.setLocationRelativeTo(null);
		gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		gui.setModalExclusionType(Dialog.ModalExclusionType.NO_EXCLUDE);

		Panel p = new Panel();
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));

		Panel p_left = new Panel();
		p_left.setLayout(new BoxLayout(p_left, BoxLayout.PAGE_AXIS));
		
					   
		experimentTree = new ExperimentTree();		
		experimentTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);    
		experimentTree.addTreeSelectionListener(treel);		
		experimentTree.addMouseListener(treeml);
		experimentTree.setCellRenderer(new ExperimentTreeRenderer());				       
		 //Enable tool tips.
	    ToolTipManager.sharedInstance().registerComponent(experimentTree);
	    
		JScrollPane treeView = new JScrollPane(experimentTree , ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		treeView.setPreferredSize(new Dimension(600,600));
		p_left.add(treeView);

		Panel p_left_bottom = new Panel();
		p_left_bottom.setLayout(new BoxLayout(p_left_bottom, BoxLayout.LINE_AXIS));
		
		chkShowImagePath = new JCheckBox("Show full image path");
		chkShowImagePath.setSelected(false);
		chkShowImagePath.setFocusable(false);
		chkShowImagePath.setActionCommand("SHOWPATH");
		chkShowImagePath.addActionListener(instance);
				
		chkUnfold = new JCheckBox("Unfold");
		chkUnfold.setSelected(false);
		chkUnfold.setFocusable(false);
		chkUnfold.setActionCommand("UNFOLD");
		chkUnfold.addActionListener(instance);
		
		
		plateNamePattern = new JTextField(10);
		plateNamePattern.setActionCommand("SEARCHPATTERN");
		plateNamePattern.addActionListener(instance);
		
		p_left_bottom.add(new JLabel(CountDrops.getIcon("edit-find.png")));
		p_left_bottom.add(Box.createRigidArea(new Dimension(5,0)));
		p_left_bottom.add(plateNamePattern);
		p_left_bottom.add(chkUnfold);
		p_left_bottom.add(chkShowImagePath);
		
		p_left.add(p_left_bottom);
		
		//panel with buttons
		Panel p_right  = new Panel();
		p_right.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);
        
		buttonNew = new JButton("New");
		buttonNew.addActionListener(instance);
		buttonNew.setActionCommand("NEW");		
		
		buttonLoad = new JButton("Load");
		buttonLoad.addActionListener(instance);
		buttonLoad.setActionCommand("LOAD");
		
		buttonEdit = new JButton("Edit settings");
		buttonEdit.addActionListener(instance);
		buttonEdit.setActionCommand("EDIT");
		buttonEdit.setEnabled(false);
		
		buttonAdd = new JButton("Add image");
		buttonAdd.addActionListener(instance);
		buttonAdd.setActionCommand("ADD");
		buttonAdd.setEnabled(false);

		buttonAddFromFolder = new JButton("Add from folder");
		buttonAddFromFolder.addActionListener(instance);
		buttonAddFromFolder.setActionCommand("ADDFROMFOLDER");
		buttonAddFromFolder.setEnabled(false);

		buttonRemove = new JButton("Remove image");
		buttonRemove.addActionListener(instance);
		buttonRemove.setActionCommand("REMOVE");
		buttonRemove.setEnabled(false);
		
		buttonSave = new JButton("Save");
		buttonSave.addActionListener(instance);
		buttonSave.setActionCommand("SAVE");
		buttonSave.setEnabled(false);
		
		buttonClose = new JButton("Close");
		buttonClose.addActionListener(instance);
		buttonClose.setActionCommand("CLOSE");
		buttonClose.setEnabled(false);
		
		buttonAddPlate = new JButton("Add a plate");
		buttonAddPlate.addActionListener(instance);
		buttonAddPlate.setActionCommand("ADDPLATE");
		buttonAddPlate.setEnabled(false);
		
		buttonDeletePlate = new JButton("Delete plate");
		buttonDeletePlate.addActionListener(instance);
		buttonDeletePlate.setActionCommand("DELETEPLATE");
		buttonDeletePlate.setEnabled(false);
		
		buttonEditPlate = new JButton("Edit plate settings");
		buttonEditPlate.addActionListener(instance);
		buttonEditPlate.setActionCommand("EDITPLATE");
		buttonEditPlate.setEnabled(false);
		
		buttonExportResult = new JButton("Export results");
		buttonExportResult.addActionListener(instance);
		buttonExportResult.setActionCommand("EXPORTRESULT");
		buttonExportResult.setEnabled(false);

		//load icons for option button 
		ImageIcon icon_options = CountDrops.getIcon("applications-system.png");		
		buttonOptions = new JButton(icon_options);		
		buttonOptions.addActionListener(instance);
		buttonOptions.setActionCommand("SETOPTIONS");
		buttonOptions.setEnabled(true);
		buttonOptions.setOpaque(false);
		buttonOptions.setContentAreaFilled(false);
		buttonOptions.setBorderPainted(false);


		
		gbc.gridx=0;
		gbc.gridy=0; p_right.add(new JLabel("Experiment"),gbc);
		gbc.gridy++; p_right.add(buttonNew,gbc);
		gbc.gridy++; p_right.add(buttonLoad,gbc);
		gbc.gridy++; p_right.add(buttonClose,gbc);
		gbc.gridy++; p_right.add(buttonEdit,gbc);
		//p_right.add(buttonSave); //this button is probably useless as all changes in experiment settings are automatically saved
		gbc.gridy++; p_right.add(buttonExportResult,gbc);
		
		
		gbc.gridy++; p_right.add(Box.createRigidArea(new Dimension(0,5)),gbc);
		gbc.gridy++; p_right.add(new JLabel("Images"),gbc);
		gbc.gridy++; p_right.add(buttonAdd,gbc);
		gbc.gridy++; p_right.add(buttonAddFromFolder,gbc);
		gbc.gridy++; p_right.add(buttonRemove,gbc);
		
		gbc.gridy++; p_right.add(Box.createRigidArea(new Dimension(0,5)),gbc);
		gbc.gridy++; p_right.add(new JLabel("Plates"),gbc);	
		gbc.gridy++; p_right.add(buttonAddPlate,gbc);
		gbc.gridy++; p_right.add(buttonDeletePlate,gbc);
		gbc.gridy++; p_right.add(buttonEditPlate,gbc);
		
		gbc.gridy++; p_right.add(Box.createRigidArea(new Dimension(0,10)),gbc);		
		gbc.gridy++;
		
		Panel p_option  = new Panel();
		p_option.add(buttonOptions);
		p_right.add(p_option,gbc);		
		
		p.add(p_left);
		p.add(p_right);
		gui.add(p);
		
		gui.setResizable(false);
		
		gui.pack();
		gui.setVisible(true);

		//The following command starts IJ with no window displayed. 
		//It is necessary to do that otherwise an exception in thrown when ImageWindow is showed
		//ImageWindow construction is incomplete, and some ImageWindow features (such as KeyListeners!) don't work.
		
		//TODO this line may be removed in case updates in IJ correct the bug! 
		//new ImageJ( null, ImageJ.NO_SHOW );
		
		//reads last opened project		
		readIni();
	}
	static void readIni() {
		try {
			String  home = System.getProperty("user.home");	
			//System.out.println(home);
            // FileReader reads text files in the default encoding.
            FileReader fileReader = new FileReader(home+File.separator+".countdrops.ini");
            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            
            String line;
            while((line = bufferedReader.readLine()) != null) {
            	//System.out.println(line);
            	//read path to R ?
            	if(line.substring(0,1).equals("R")) {
            		pathToR = line.substring(2,line.length());     
            		System.out.println("Path to R executable: "+pathToR);       		
            	} else {
            		File f= new File(line);
            		if(f.exists()) lastFile= f;
            	}
            }   
            // Always close files.
            bufferedReader.close();         
		}
		catch(IOException ex) {
		}
	}

	static void writeIni() {
		try {
			//path to home directory
			String  home = System.getProperty("user.home");
			FileWriter fileWriter = new FileWriter(home+File.separator+".countdrops.ini");

			// Always wrap FileWriter in BufferedWriter.
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			
			bufferedWriter.write("R "+pathToR);
			bufferedWriter.newLine();
		
			if(experiment!=null) {
				PlateSettings s = experiment.getSettings();
				bufferedWriter.write(s.getPath()+s.getFileName());
				bufferedWriter.newLine();
			}

			// Always close files.
			bufferedWriter.close();         
		}
		catch(IOException ex) {
		}
	}

	public void updateButtons() {
		unblockButtons();
		
		boolean hasExp = experiment!=null;
		
		buttonNew.setEnabled(!hasExp);
		buttonLoad.setEnabled(!hasExp);		
		buttonSave.setEnabled(hasExp);
		buttonClose.setEnabled(hasExp);
		
		buttonEdit.setEnabled(hasExp);
		buttonExportResult.setEnabled(hasExp);

		buttonAdd.setEnabled(hasExp);
		buttonAddFromFolder.setEnabled(hasExp);
		buttonRemove.setEnabled(hasExp);
		buttonAddPlate.setEnabled(hasExp);
		buttonDeletePlate.setEnabled(hasExp);
		buttonEditPlate.setEnabled(hasExp);
		
		if(!hasExp) return;
		
		if(experimentTree.isPlateSelected()) return;
		
		if(experimentTree.isPictureSelected()) {
			buttonDeletePlate.setEnabled(false);
			buttonEditPlate.setEnabled(false);	
			return;
		}
				
		buttonRemove.setEnabled(false);
		buttonAddPlate.setEnabled(false);
		buttonDeletePlate.setEnabled(false);
		buttonEditPlate.setEnabled(false);		
		
	}
	
	public void blockButtons() {
		setEnableComponents(gui,false);
	}

	public void unblockButtons() {
		setEnableComponents(gui,true);
	}
	
	public static void setEnableComponents(final Container g,boolean b) {
		Component[] comp = g.getComponents();
		for (Component c : comp) {
			if(c instanceof Container) setEnableComponents((Container) c,b);
			if(c instanceof JButton || c instanceof JCheckBox || c instanceof JTextField ) {				
				c.setEnabled(b);
			}			
		}
	}
	
	public static JFrame getGui() {return gui;}
	
	public static void drawSelectedPlate() {
		Picture p = experimentTree.getSelectedPicture();    	
		if(p==null) return;


		//display selected picture
		if(viewedImage==null || viewedImage.getImageWindow().isClosed() || !(viewedImage.getImagePlus().getTitle().equals(p.getFileName()))) {
			setWaitCursor();		

			System.gc();
			if(viewedImage!=null && !viewedImage.getImageWindow().isClosed()) viewedImage.close(); 
			try{
				viewedImage = new ImagePicture(p,instance);
			} catch(Exception ex) {
				JOptionPane.showMessageDialog(gui,"Failed to open image! "+ex.getMessage(), "Opening image", JOptionPane.ERROR_MESSAGE);
				System.out.println("Failed to open image!");
				setDefaultCursor();
				return;
			}
			viewedImage.show();
			//must be called after show, otherwise ImageWindpw has not been instantiated yet
			viewedImage.getImageWindow().addWindowListener(windowListener);

			setDefaultCursor();		
		}

		int i = experimentTree.getSelectedPlateIndex();		
		if(i<0) return;

		viewedImage.selectPlate(i);

	}

	public int[][] getCountsFromOtherPlates(Well w) {
		if(experiment==null) return null;
		return experiment.getCountsFromOtherPlates(w);
	}
	
	public SampleStatistics getStatistics(Well w) {
		if(experiment == null) return null;
		return experiment.getStatistics(w);
	}
	
	public static void deleteSelectedPicture() {    	
		experiment.deletePicture(experimentTree.getSelectedPictureIndex());
	}

	public static void deleteSelection() {
		Picture p = experimentTree.getSelectedPicture();
		if(p==null) return;

		Plate pl = experimentTree.getSelectedPlate();
		if(pl!=null) {
			//the selected plate is **destroyed**
			p.deletePlate(pl);
			return;
		}

		//no plate selected : the selected picture is removed
		//(i.e. files are not destroyed)
		int i = experimentTree.getSelectedPictureIndex();
		experiment.deletePicture(i);
	}

	public static ImageIcon getIcon(String fileName) {
		ImageIcon ico = null;
		try{
			ClassLoader classLoader = CountDrops.class.getClassLoader();
			URL url = classLoader.getResource("images/"+fileName); 
			//System.out.println(url.toString());
			ico = new ImageIcon(url);			
		} catch(Exception ex) {
			System.out.println(ex);
		}
		return ico;
	}
	
    public static void setWaitCursor(Component x) {
        if (x != null) {        	
            x.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));            
        }
    }    

    public static void setDefaultCursor(Component x) {
        if (x != null) {
            x.setCursor(Cursor.getDefaultCursor());            
        }
    }

    public static void setWaitCursor() {
    	setWaitCursor(gui);
    }
    public static void setDefaultCursor() {
    	setDefaultCursor(gui);
    }

	public ArrayList<File> searchImageInFolder(String path) {
		if(path==null) return null;
		
		File dir = new File(path);
		if(!dir.exists() || !dir.isDirectory()) return null;
		
		// create new filename filter
        FilenameFilter fileNameFilter = new FilenameFilter() {
  
           @Override
           public boolean accept(File dir, String name) {
        	  File f = new File(dir.getAbsolutePath()+File.separator+name);
        	  if(f.isDirectory()) return true;
              if(name.lastIndexOf('.')>0) {              
                 // get last index for '.' char
                 int lastIndex = name.lastIndexOf('.');                 
                 // get extension
                 String str = name.substring(lastIndex);                 
                 // match path name extension
                 String [] ext = {".avi",".jpg",".jpeg",".tiff",".tif"};
                 for(int i=0;i<ext.length;i++) {
                	 if(str.equalsIgnoreCase(ext[i])) return true;
                 }         
              }              
              return false;
           }
        };
		
        ArrayList<String> listImgAlreadyAdded = new ArrayList<String>(); 
        for(int k=0;k<experiment.getNbPictures();k++) {
			Picture p = experiment.getPicture(k);
			if(p!=null) listImgAlreadyAdded.add(p.getPath()+p.getFileName());
        }
        
		File[] f = dir.listFiles(fileNameFilter);
		
		ArrayList<File> listImg = new ArrayList<File>(); 
		for(int i=0;i<f.length;i++) {
			if(f[i].isDirectory()) {
				ArrayList<File> listImg2 = searchImageInFolder(f[i].getAbsolutePath());
				for(int j=0;j<listImg2.size();j++) {
					//check that image has not already been added to the experiment?
					String imgName = listImg2.get(j).getAbsolutePath();
					if(!listImgAlreadyAdded.contains(imgName)) listImg.add(listImg2.get(j));
				}
			} else {
				listImg.add(f[i]);
			}
		}
		return listImg;
	}

	public static String getDirFromFileChooser(Component parent,File defaultFile) {		
		LookAndFeel old = UIManager.getLookAndFeel();
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} 
		catch (Throwable ex) {
			old = null;
		}
		
		fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);		
		if(defaultFile!=null) fc.setSelectedFile(defaultFile);

		String x = null;
		if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			File f = fc.getSelectedFile();
			x = f.getAbsolutePath();
		}

		try {
			if(old!=null) UIManager.setLookAndFeel(old);
		} catch(Throwable ex) {				
		}
	    
		return x;
	}

	public static String[] getFromFileChooser(String title,Component parent,File defaultFile,FileNameExtensionFilter extentionFilter) {		
		LookAndFeel old = UIManager.getLookAndFeel();
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} 
		catch (Throwable ex) {
			old = null;
		}
		
		fc = new JFileChooser();
		fc.setDialogTitle(title);
		if(extentionFilter!=null) fc.setFileFilter(extentionFilter);
		if(defaultFile!=null) fc.setSelectedFile(defaultFile);

		String[] x = null;
		if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			File f = fc.getSelectedFile();
			x = new String[2];
			x[0] = f.getName();  // file name first
			x[1] = f.getParent();//path second
			if (x[1].charAt(x[1].length()-1) != File.separatorChar) x[1]+=File.separatorChar; //add the separator char if necessary    		
		}

		try {
			if(old!=null) UIManager.setLookAndFeel(old);
		} catch(Throwable ex) {				
		}
	    
		return x;
	}

	public static String[] createFromFileChooser(Component parent,File defaultFile,FileNameExtensionFilter extentionFilter) {
		LookAndFeel old = UIManager.getLookAndFeel();
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} 
		catch (Throwable ex) {
			old = null;
		}

		fc = new JFileChooser();
		if(extentionFilter!=null) fc.setFileFilter(extentionFilter);
		if(defaultFile!=null) fc.setSelectedFile(defaultFile);

		String[] x = null;
		if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
			File f = fc.getSelectedFile();
			x = new String[2];
			x[0] = f.getName();  // file name first
			x[1] = f.getParent();//path second
			if (x[1].charAt(x[1].length()-1) != File.separatorChar) x[1]+=File.separatorChar; //add the separator char if necessary    		
		}

		try {
			if(old!=null) UIManager.setLookAndFeel(old);
		} catch(Throwable ex) {				
		}

		return x;
	}

	
	// action listener
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		
		if(action=="SHOWPATH") {
			experimentTree.setShowImagePath(chkShowImagePath.isSelected());
			experimentTree.updateUI();
		}
		
		if(action=="UNFOLD") {
			experimentTree.unfold(chkUnfold.isSelected());			
		}
		
		if(action=="SEARCHPATTERN") {
			int pos =experimentTree.search(plateNamePattern.getText());
			if(pos>-1) {
				experimentTree.scrollRowToVisible(pos);
			}
			
		}
		
		if(action=="NEW") {    		    
			PlateSettings s = new PlateSettings();
			s.setDefault();

			SetExperimentSettings ws = new SetExperimentSettings(s,SetExperimentSettings.CREATE);

			if(ws.getStatus()==SetExperimentSettings.CREATE) {
				experiment = new Experiment(s);				
				if(experiment.save()) {
					experimentTree.update(experiment);
				}				
				writeIni();
			}			
			ws = null;
			return;
		}

		if(action=="EDIT") { 
			if(experiment == null) return;
			
			SetExperimentSettings ws = new SetExperimentSettings(experiment.getSettings(),SetExperimentSettings.EDIT);
			if(ws.getStatus()==SetExperimentSettings.EDIT) {
				experiment.save();				
				//propagates changes to Pictures and Plates
				for(int i=0;i<experiment.getNbPictures();i++) {
					Picture p = experiment.getPicture(i);
					for(int j=0;j<p.getNbPlates();j++) {
						Plate pl = p.getPlate(j);
						//only color and short cuts can be updated because other fields might already have been set 
						//when plate has been created. Updating would then erase field value set by the user. 
						pl.getSettings().copyCFUColor(experiment.getSettings());
						pl.getSettings().copyCFUKey(experiment.getSettings());						
						pl.getSettings().save();
						//plate settings must be checked again against experiment settings
						//because save only checks fields value validity 
						if(! experiment.getSettings().isCompatible(pl.getSettings())) {
							pl.getSettings().setProblems(true);
						} 
					}
				}
			}
			ws = null;
			return;
		}

		if(action=="ADD") {
			if(experiment==null) return;
			String[] path = getFromFileChooser("Add image",gui,lastFile,new FileNameExtensionFilter("Image files (*.jpeg, *.jpg, *.tiff, *.avi)", "jpeg", "jpg", "tiff", "avi"));
			if (path!=null) {    			
				try {
					//check that the picture has not already been added
					for(int i=0;i<experiment.getNbPictures();i++) {
						Picture p = experiment.getPicture(i);
						if(p.getPath().equals(path[1]) && p.getFileName().equals(path[0])) {
							JOptionPane.showMessageDialog(gui,"Image "+path[0]+" has already been added to experiment!", "Add an image to the experiment", JOptionPane.ERROR_MESSAGE);
							System.out.println("Failed to add image!");
							return;
						}
					}
					Picture p = new Picture(path[1],path[0]);
					boolean settingsOK = true;
					for(int i=0;i<p.getNbPlates();i++) {
						if(!experiment.getSettings().isCompatible(p.getPlate(i).getSettings())) {
							//the plate has settings that are not compatible with the experiment settings
							settingsOK=false;
						}
					}
					if(settingsOK) {
						lastFile = new File(path[1]+path[0]); 
						experiment.addPicture(p);
						experiment.save();
						
						experimentTree.update();						
						experimentTree.selectPicture(p);
					} else {							
						JOptionPane.showMessageDialog(gui,"Some plates associated to the image have settings that are not compatible with experiment settings.\n The image will not be added to the experiment!", "Add an image to the experiment", JOptionPane.ERROR_MESSAGE);
						System.out.println("Failed to add image!");						
					}
				} catch (Exception ex) {
					//failed to open image !								
					JOptionPane.showMessageDialog(gui,ex.getMessage(), "Add an image to the experiment", JOptionPane.ERROR_MESSAGE);
					System.out.println("Failed to add image!");
				}
			}
			return;
		}

		if(action=="ADDFROMFOLDER") {
			if(experiment==null) return;
			String path = getDirFromFileChooser(gui,lastFile);
			if (path!=null) {
				ArrayList<File> listImg = searchImageInFolder(path);								 
				if(listImg!=null && listImg.size()>0) {
					ImageChooser ic = new ImageChooser(listImg);
					listImg = ic.getImages();
					if(listImg!=null && listImg.size()>0) {
												
						final int nbImg = listImg.size();
						final ArrayList<File> finalListImg = listImg;
						final ProgressDialog pgDlg = new  ProgressDialog(gui,"Add images from folder",nbImg);	
						blockButtons();
						pgDlg.setVisible(true);
						CountDrops.setWaitCursor();
												
						SwingWorker<Integer, String> sw = new SwingWorker<Integer, String>(){
							ArrayList<File> listImgFailed = new ArrayList<File>();
							ArrayList<String> listMsgFailed = new ArrayList<String>();
							
							@Override
							protected Integer doInBackground() throws Exception {								
								for(int i=0;i<nbImg;i++) {
									File f = finalListImg.get(i);
									publish(f.getName());									
									
									try {								
										Picture p = new Picture(f.getParent()+File.separator,f.getName());
										boolean settingsOK = true;
										for(int j=0;j<p.getNbPlates();j++) {											
											if(!experiment.getSettings().isCompatible(p.getPlate(j).getSettings())) {
												//the plate has settings that are not compatible with the experiment settings
												settingsOK=false;
												p.getPlate(j).setProblems(true);
											}																				
										}
										if(settingsOK) {
											lastFile = f; 
											experiment.addPicture(p);
										} else {											
											experiment.addPicture(p);
											listImgFailed.add(f);
											listMsgFailed.add("[wrong plate settings]");
											System.out.println("Plate and experiment settings are incompatible!");						
										}
									} catch (Exception ex) {
										//failed to open image !								
										listImgFailed.add(f);
										listMsgFailed.add("[file error]");
										System.out.println("Failed to add image!");						
									}
									
									setProgress(i);
									Thread.sleep(100);
								}
								return nbImg-listImgFailed.size();
							}
							
							@Override
							protected void process(List<String> txt) {
								try{							
									pgDlg.setText(txt.get(txt.size()-1));
								} catch(Exception ex) {
									
								}
							}
							
							@Override
							protected void done() {
								pgDlg.setVisible(false);
								CountDrops.setDefaultCursor();								
								updateButtons();
								
								experiment.save();												
								experimentTree.update();	
								experimentTree.unfold(true);
								if(listImgFailed.size()>0) {
									String msg = ""+listImgFailed.size()+" image(s) could not be added to the experiment, or have problematic plates !\n";
									for(int i=0;i<listImgFailed.size();i++) msg += listMsgFailed.get(i)+" "+listImgFailed.get(i).getName()+"\n";
									JOptionPane.showMessageDialog(gui,msg, "Add an image to the experiment", JOptionPane.ERROR_MESSAGE);
								}
							}
						};
						
						sw.addPropertyChangeListener(new PropertyChangeListener(){
							public void propertyChange(PropertyChangeEvent event) {
								if("progress".equals(event.getPropertyName())){
									if(SwingUtilities.isEventDispatchThread())								
										pgDlg.setProgress();
								}            
							}         
						});
						
						//starting swingWorker
						sw.execute();
					}
				} else {
					JOptionPane.showMessageDialog(gui,"No images have been detected", "Add images to the experiment", JOptionPane.WARNING_MESSAGE);
				}
				
			}
		}
		
		if(action=="REMOVE") {
			if(viewedImage!=null) viewedImage.close();
			viewedImage = null;
			deleteSelectedPicture();
			experimentTree.update();
			experiment.save();
		}    	

		if(action=="LOAD") {			
			final String[] ep = getFromFileChooser("Open experiment file",gui,lastFile,new FileNameExtensionFilter("Experiment files (*.txt, *.cfg)", "cfg", "txt"));						
								
			if(ep!=null) {						
				
				//loads experiment settings (but not pictures: this is fast!) 
				try {
					experiment = new Experiment(ep[1],ep[0],false);				
				} catch(Exception ex) {    		
					//failed
					JOptionPane.showMessageDialog(gui,"Failed to load experiment! "+ex.getMessage(), "Reading experiment file", JOptionPane.ERROR_MESSAGE);					
					System.out.println("Failed to load experiment settings!");							
					experiment = null;
				}

				if(experiment!=null) {
					final int nbImg = experiment.getNbImagesPath();
					
					final ProgressDialog pgDlg = new  ProgressDialog(gui,"Loading experiment",nbImg);	
					blockButtons();
					if(nbImg>0) {
							pgDlg.setVisible(true);
					}
					CountDrops.setWaitCursor();

					SwingWorker<Integer, String> sw = new SwingWorker<Integer, String>(){
						@Override
						protected Integer doInBackground() throws Exception {
							try {
								//tries to load pictures: this is long !
								ArrayList<String> errMsg = new ArrayList<String>();
								for(int i=0;i<nbImg;i++) {
									publish(experiment.getImagePath(i)[1]);
									try {									
										errMsg.addAll(experiment.loadPicture(i));
									} catch (Exception ignore) {}

									setProgress(i);
									Thread.sleep(100);
								}
								if(errMsg.size()>0) {				
									String txt = "CountDrops encountered problems while loading experiment!\n";
									for(int i=0;i<errMsg.size();i++) txt+=errMsg.get(i)+"\n";
									JOptionPane.showMessageDialog(CountDrops.getGui(),txt, "Reading experiment file", JOptionPane.WARNING_MESSAGE);

								}

								//update display							
								return experiment.getNbPictures();
							} catch (Exception ex) {
								return -1;
							}						
						}	

						@Override
						protected void process(List<String> txt) {
							try{							
								pgDlg.setText(txt.get(txt.size()-1));
							} catch(Exception ex) {

							}
						}

						public void done(){
							if(SwingUtilities.isEventDispatchThread()) {											
								updateButtons();	
								CountDrops.setDefaultCursor();
								if(nbImg>0) pgDlg.setVisible(false);

								lastFile = new File(experiment.getPath()+experiment.getSettings().getFileName());
								writeIni();

								chkUnfold.setSelected(true);
								experimentTree.update(experiment);

							}
						}   
					};								

					sw.addPropertyChangeListener(new PropertyChangeListener(){
						public void propertyChange(PropertyChangeEvent event) {
							if("progress".equals(event.getPropertyName())){
								if(SwingUtilities.isEventDispatchThread())								
									pgDlg.setProgress();
							}            
						}         
					});

					//starting swingWorker
					sw.execute();

				}
			}
			return;
		}


		if(action=="CLOSE") {
			writeIni();
			if(viewedImage!=null) viewedImage.close();
			
			viewedImage = null;
			experiment = null;			
			experimentTree.update(experiment);
			updateButtons();
		}
		
		if(action=="SAVE") {    		
			if(experiment.getSettings().getFileName()==null) {
				String[] p = getFromFileChooser("Open experiment file",gui,new File("experiment.txt"),new FileNameExtensionFilter("Experiment files (*.txt, *.cfg)", "cfg", "txt"));
				if (p!=null) {
					experiment.getSettings().setFileName(p[0]);    		
					experiment.getSettings().setPath(p[1]);        		
				} else return;
			}

			experiment.save();
			return;
		}						
		
		if(action=="ADDPLATE") {
			if(experimentTree.getSelectedPicture()==null) return;
			
			drawSelectedPlate();
			
			Plate pl = experimentTree.getSelectedPlate();
			PlateSettings s = null;
			
			//create a new PlateSettings which will be passed to SetPlateSettings interface
			if(pl==null) {
				s = new PlateSettings(experiment.getSettings());				
			} else {
				s = new PlateSettings(pl.getSettings());
			}
			
			//sets path to image folder (where plate folders will be created)
			Picture p = experimentTree.getSelectedPicture();
			String path = p.getPath()+p.getFileName();
			path = path.substring(0,path.lastIndexOf("."))+File.separator; //remove file extension and add path separator
			s.setPath(path);
			//sets image name
			s.setImage(p.getFileName());
			
			System.out.println("Enter plate settings...");			
			SetPlateSettings sps = new SetPlateSettings(s,viewedImage,true);
			
			//sets path and image according to the selected picture
			//path should have the same name image, with the extension omitted
			if(sps.getStatus()==SetPlateSettings.CREATE) { 
				Plate pl2 = null;
				try {
					pl2 = new Plate(s);
					p.addPlate(pl2);
					experimentTree.update();
					experimentTree.selectPlate(pl2);
					drawSelectedPlate();
				} catch(Exception ex) {
					System.out.println("Failed to create plate!");					
				}
			}
			sps = null;
			return;
		}
		
		if(action=="DELETEPLATE") {
			Picture p = experimentTree.getSelectedPicture();
			if(p==null) return;
			int index = experimentTree.getSelectedPlateIndex();
			if(index<0) return;
			
			p.deletePlate(index);	
			if(viewedImage!=null) {
				viewedImage.getOverlay().clear();
				viewedImage.getImagePlus().repaintWindow();
			}
			experimentTree.update();			 
			experimentTree.selectPicture(p);
			return;
		}
		
		if(action=="EDITPLATE") {			
			Plate pl = experimentTree.getSelectedPlate();
			if(pl==null) return;
			String oldPath = pl.getPath();
			SetPlateSettings sps = new SetPlateSettings(pl.getSettings(),viewedImage,false);
			if(sps.getStatus()==SetPlateSettings.EDIT) {
				if(!oldPath.equals(pl.getPath())) {
					//path has changed: the plate folder must be renamed
					File f = new File(oldPath);
					f.renameTo(new File(pl.getPath()));
				}
				pl.getSettings().save();	
				pl.updateWellDilutionAndVolume();
								
				experimentTree.update();
				experimentTree.selectPlate(pl);
			}
			
			sps = null;
			if(viewedImage!=null) drawSelectedPlate();
			
			return;
		}
		if(action=="EXPORTRESULT") {
			ExportResult er = new ExportResult(experiment);
			if(er.getStatus()==ExportResult.OK) {
				experiment.save();
				
				String path = experiment.getPath();			
				String countfile = experiment.exportCounts();
				String loadfile = countfile.replace("COUNTS","LOAD");

				//path to the CountDrop jar file
				File res = new File(CountDrops.class.getResource("").getPath());				
				String jar = res.getAbsolutePath()+File.separator+"CountDrops.jar";				 			
				
				try {
					//ensure that path is not written as an URL
					jar = URLDecoder.decode(jar,Charset.defaultCharset().toString());
					//new File(jar).toURI().toString();
					//Windoze file separator must be replaced by /
					path = path.replace("\\", "/");
					jar = jar.replace("\\","/");
					
					//write local script to run load estimation !!					
					PrintWriter writer = new PrintWriter(experiment.getPath()+"src_load_estimate.R", Charset.defaultCharset().toString());					
					writer.println("rm(list=ls())");					
					writer.println("setwd(\""+path+"\")"); 
					writer.println("unzip(\""+jar+"\",file=\"src_load_estimate_functions.R\",exdir=\".\")");
					writer.println("source(\"src_load_estimate_functions.R\")");
					writer.println("cfu <- read.table(\""+countfile+"\",header=T,sep=\";\")");										
					writer.println("load <- estimateLoad(cfu,max.cfu.per.drop=30)");
					writer.println("write.table(load,file=\""+loadfile+"\",sep=\";\")");
					writer.close();
					
					//run estimate										
					try {
						//TODO does not seem to work on Macs!!
						ProcessBuilder pb = new ProcessBuilder(pathToR, "CMD","BATCH","--vanilla","src_load_estimate.R");						
						pb.directory(new File(experiment.getPath()));					
						Process p = pb.start();
						//waits for the end of process. What if process got stuck? Display progress bar with a cancel button??
						p.waitFor();
					} catch(Exception ex) {
						JOptionPane.showMessageDialog(CountDrops.getGui(),"CountDrops encountered an error while starting R! You can try estimating load by running the script src_load_estimate.R", "Estimating load from counts", JOptionPane.WARNING_MESSAGE);
					}
					
					
					//write a new version of script file, with code to estimate load commented, and new code added to read load file and plot
					//the distribution of total load
					writer = new PrintWriter(experiment.getPath()+"src_load_estimate.R", Charset.defaultCharset().toString());					
					writer.println("rm(list=ls())");
					writer.println("setwd(\""+path+"\")");
					
					writer.println();
					writer.println("# Load estimation");
					writer.println("# cfu <- read.table(\""+countfile+"\",header=T,sep=\";\")");										
					writer.println("# unzip(\""+jar+"\",file=\"src_load_estimate_functions.R\",exdir=\".\")");
					writer.println("# source(\"src_load_estimate_functions.R\")");
					writer.println("# load <- estimateLoad(cfu,max.cfu.per.drop=30)");
					writer.println("# write.table(load,file=\""+loadfile+"\",sep=\";\")");
					
					writer.println();
					writer.println("load <- read.table(file=\""+loadfile+"\",header=T,sep=\";\")");
					writer.println("hist(load$TOTAL)");
					writer.close();

				} catch(Exception ex) {					
					System.out.println("Problem while trying to start R code to estimate load");
					System.out.println(ex);
				}
			}
			
		}
		
		if(action=="SETOPTIONS") {
			//let the user change options
			Object res = JOptionPane.showInputDialog(gui, "Path to R","Set options", JOptionPane.QUESTION_MESSAGE,null,null, pathToR);			
			if(res != null) {
				pathToR = res.toString();
			}
			//save new option values in CountDrop.ini
			writeIni();
		}
	}

	
	@Override
	public void viewWellChange(ViewWellEvent evt) {
		experimentTree.updateUI();		
	}
	@Override
	public void newViewWellAsked(ViewWellEvent evt) {}
	@Override
	public void viewWellCopyState(ViewWellEvent evt) {}
	@Override
	public void viewWellHasClosed(ViewWellEvent evt) {}
	@Override
	public void autoDetectRow(ViewWellEvent evt) {		
		experimentTree.updateUI();
	}
	@Override
	public void autoDetectColumn(ViewWellEvent evt) {
		experimentTree.updateUI();
	}
	@Override
	public void autoDetectPlate(ViewWellEvent evt) {
		experimentTree.updateUI();
	}	
	
}
