package countdrops;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

public class ViewWell extends JFrame implements ActionListener, ImageWellListener, KeyListener, ChangeListener, ViewCommentsListener {
	private static final long serialVersionUID = 1L;

	private ImageWell img;
	
	//event sent out to whoever is listening
	private ViewWellEvent viewWellEvent;
	
	//listeners hooked up onto that window (typically opened by ImagePicture to monitor what well is under inspection)
	ArrayList<ViewWellListener> listViewWellListener = new ArrayList<ViewWellListener>();
	
	// CFU table
	private CFUTableModel cfuTableModel;
	private TableRowSorter<CFUTableModel> cfuTableSorter;
	private JTable cfuTable;
	private CFUTableRowListener cfuTableRowListener;

	//comments
	private Comments comments;
	private JButton buttonComment;
	
	// count table
	private SummaryTableModel summaryTableModel;
	private JTable summaryTable;
	JCheckBox chkEmpty,chkIgnore, chkDoWand,chkShowWellCountour,chkCloseWhenMoving;
	JButton   bCpy_row,bCpy_col, bCpy_plate;
	
	private GraphCanvas graphicStatistics = null;
	
	// key pressed
	// private KeyStrokeAction keyStrokeAction;
	private Boolean CTRLpressed = false;
	private Boolean SHIFTpressed = false;
	private Boolean ALTpressed = false;

	public ViewWell(ImageWell ximg,ViewWellEvent evt,SampleStatistics stat) { 
		super();
		
		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);			
		this.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent e) {
		        close();
		    }
		});
		
		img = ximg;				
		img.addListener(this);
		img.getImageCanvas().addKeyListener(this);
		if(evt!=null && evt.getCanvasMagnification()>1) {
			//copy magnification from ViewWellEvent
			img.zoom(evt.getCanvasMagnification());		
		}
		if(evt!=null) {
			img.setSlice(evt.getSlice());
		}
						
		//If no CFU and no NC, well is set to empty
		//This is to speed up things when scanning a bunch of empty wells: you don't have to click on 
		//anything, well will be set to empty automatically.
		if(img.getWell().guessIfEmpty()) {
			img.getWell().setEmpty();
		}		
		viewWellEvent = new ViewWellEvent(img.getWell(),this.getLocation());

		comments = img.getWell().getComments();
		
		// window dimensions and position
		int imgWidth = img.getImagePlus().getWidth();
		this.setPreferredSize(new Dimension(imgWidth*2+1200, 700));
		setResizable(true);
		
		this.setUndecorated(false); //for some reason the cross button to cose dialog does not show up...		
		setTitle(img.getImagePlus().getTitle());
				
		
		Panel main_panel = new Panel();
		main_panel.setLayout(new BoxLayout(main_panel, BoxLayout.LINE_AXIS));

		Panel p_image = new Panel();
		p_image.setLayout(new BoxLayout(p_image, BoxLayout.PAGE_AXIS));
		
		p_image.add(Box.createRigidArea(new Dimension(0,16)));
		p_image.add(img.getImageCanvas());
		
		// left panel ***********************************************************************
		// contains the cfuTable and the summaryTable, plus associated checkboxes and buttons
		// **********************************************************************************
		Panel p_left = new Panel();
		p_left.setLayout(new BoxLayout(p_left, BoxLayout.PAGE_AXIS));

		// creates cfuTable
		cfuTableModel = new CFUTableModel(img);
		cfuTable = new JTable(cfuTableModel);		
		
		// set cfuTable selection model : reacts to any change in selection by
		// selecting the right CFUs in img
		cfuTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		cfuTable.setRowSelectionAllowed(true);
		cfuTable.setColumnSelectionAllowed(false); 
		cfuTableRowListener = new CFUTableRowListener(img, cfuTable);
		cfuTable.getSelectionModel().addListSelectionListener(
				cfuTableRowListener);		
		
		// this allows to sort the table by clicking on column headers
		cfuTableSorter = new TableRowSorter<CFUTableModel>(cfuTableModel);
		cfuTable.setRowSorter(cfuTableSorter);
		cfuTable.addKeyListener(this);

		// scroll panel for cfuTable
		cfuTable.setPreferredScrollableViewportSize(new Dimension(200,100));
		cfuTable.setFillsViewportHeight(true);
		JScrollPane cfuTableScrollPanel = new JScrollPane(cfuTable,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		p_left.add(new JLabel("Table of CFUs"));
		p_left.add(cfuTableScrollPanel);

		// bottom part of left panel
		Panel p_left_bottom = new Panel();
		p_left_bottom.setLayout(new BoxLayout(p_left_bottom,
				BoxLayout.LINE_AXIS));
		// creates summaryTable
		Panel p_left_bottom_left = new Panel();
		p_left_bottom_left.setLayout(new BoxLayout(p_left_bottom_left,
				BoxLayout.PAGE_AXIS));

		chkEmpty = new JCheckBox("Well is empty");
		chkEmpty.setAlignmentX((float) 0.0); // left alignement
		chkEmpty.setFocusable(false);
		chkEmpty.setSelected(img.getWell().isEmpty());
		chkEmpty.setActionCommand("EMPTY");
		chkEmpty.addActionListener(this);
		p_left_bottom_left.add(chkEmpty);

		//Counts from wells that have the ignore tag are not exported in the result file.
		chkIgnore = new JCheckBox("Counts from this well should not be written in result file");
		chkIgnore.setAlignmentX((float) 0.0); // left alignement
		chkIgnore.setFocusable(false);
		chkIgnore.setSelected(img.getWell().isIgnored());
		chkIgnore.setActionCommand("IGNORE");
		chkIgnore.addActionListener(this);
		p_left_bottom_left.add(chkIgnore);
		
		graphicStatistics = new GraphCanvas(stat); 		//will be added in right panel
		graphicStatistics.setPreferredSize(new Dimension(250,250));

		p_left_bottom_left.add(new JLabel("Number of CFU per type"));
		summaryTableModel = new SummaryTableModel(img, chkEmpty, graphicStatistics, listViewWellListener,viewWellEvent);
		summaryTable = new JTable(summaryTableModel);
		summaryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		summaryTable.getSelectionModel().addListSelectionListener(
				new SummaryTableRowListener(img,graphicStatistics));			
		// initialize selection from arguments passed to the constructor by ViewWellEvent
		if(evt!=null) {
			int type = evt.getSelectedType();
			if(type>-1 && type<summaryTable.getRowCount()) summaryTable.setRowSelectionInterval(type,type);
		}
		
		summaryTable.setFocusable(false);
		summaryTable.setAlignmentX((float) 0.0);
		//TODO center counts and statistics!!
		
		//header must be added to panel explicitly because summaryTable is not inside a JScroll...
		JTableHeader summaryHeader = summaryTable.getTableHeader();
		summaryHeader.setAlignmentX(summaryTable.getAlignmentX());
		p_left_bottom_left.add(summaryHeader);
		p_left_bottom_left.add(summaryTable);

		//right part of bottom panel: options
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		Panel p_left_bottom_right = new Panel();
		//p_left_bottom_right.setBackground(Color.cyan);
		p_left_bottom_right.setMaximumSize(new Dimension(400,200));
		p_left_bottom_right.setMinimumSize(new Dimension(400,200));
		
		p_left_bottom_right.setLayout(new GridBagLayout());
        GridBagConstraints gbc_br = new GridBagConstraints();        
        gbc_br.weightx = 1;
        gbc_br.fill = GridBagConstraints.NONE;
        gbc_br.insets = new Insets(4, 4, 4, 4);
        gbc_br.anchor = GridBagConstraints.EAST;

		//view comments		
		buttonComment = new JButton();
		updateCommentButton();			
		buttonComment.setAlignmentX((float) 1.0);
		buttonComment.setActionCommand("VIEWCOMMENTS");
		buttonComment.addActionListener(this);
		buttonComment.setFocusable(false);
		buttonComment.setOpaque(false);
		buttonComment.setContentAreaFilled(false);
		buttonComment.setBorderPainted(false);					
		gbc_br.gridx = 0;
        gbc_br.gridy = 0;
        gbc_br.gridwidth = 3;
		p_left_bottom_right.add(buttonComment,gbc_br);		
		gbc_br.gridy++;
		
		chkDoWand = new JCheckBox("Create CFU with magic wand");
		if(evt!=null) {
			chkDoWand.setSelected(evt.isDoWand());
		}
		chkDoWand.setAlignmentX((float) 1.0); // right alignement
		chkDoWand.setFocusable(false);
		chkDoWand.setActionCommand("DOWAND");
		chkDoWand.addActionListener(this);
		gbc_br.gridx = 0;
		gbc_br.gridwidth = 3;
		p_left_bottom_right.add(chkDoWand,gbc_br);
		gbc_br.gridy++;
		
		img.setDoWand(chkDoWand.isSelected());
		
		chkShowWellCountour = new JCheckBox("Show well contour");
		if(evt!=null) {
			chkShowWellCountour.setSelected(evt.isShowWellContour());
		}
		chkShowWellCountour.setAlignmentX((float) 1.0); // right alignement
		chkShowWellCountour.setFocusable(false);
		chkShowWellCountour.setActionCommand("SHOWWELLCONTOUR"); 
		chkShowWellCountour.addActionListener(this);
		gbc_br.gridx = 0;
		gbc_br.gridwidth = 3;
		p_left_bottom_right.add(chkShowWellCountour,gbc_br);
		gbc_br.gridy++;
		
		img.setShowWellContour(chkShowWellCountour.isSelected());
		
		JButton b1 = new JButton("Delete all");
		b1.setAlignmentX((float) 1.0);
		b1.setActionCommand("DELETE");
		b1.addActionListener(this);
		b1.setFocusable(false);		
		gbc_br.gridx = 0;
		gbc_br.gridwidth = 3;
		p_left_bottom_right.add(b1,gbc_br);
		gbc_br.gridy++;
		
		JButton b2 = new JButton("Autodetect");
		b2.setAlignmentX((float) 1.0); // right alignement
		b2.setActionCommand("AUTODETECT");
		b2.addActionListener(this);
		b2.setFocusable(false);
		gbc_br.gridx = 0;
		gbc_br.gridwidth = 3;
		p_left_bottom_right.add(b2,gbc_br);
		gbc_br.gridy++;
		
		SpinnerModel spinRadiusModel = new SpinnerNumberModel(img.getCFURadius(), //initial value
		                               1, //min
		                               img.getImagePlus().getWidth()/2, //max
		                               1); 
		JSpinner spinRadius = new JSpinner(spinRadiusModel);
		spinRadius.setFocusable(false);		
		spinRadius.setMaximumSize(new Dimension(72,24));
		spinRadius.addChangeListener(this);	
		JLabel labSpinRadius = new JLabel("CFU radius");
		labSpinRadius.setAlignmentX( Component.RIGHT_ALIGNMENT );//0.0
						
		gbc_br.gridx = 0;	
		gbc_br.gridwidth = 1;
		p_left_bottom_right.add(Box.createRigidArea(new Dimension(100,0)),gbc_br);
		gbc_br.gridx = 1;	
		gbc_br.gridwidth = 1;
		p_left_bottom_right.add(labSpinRadius,gbc_br);
		gbc_br.gridx = 2;	
		gbc_br.gridwidth = 1;
		p_left_bottom_right.add(spinRadius,gbc_br);
		gbc_br.gridy++;
		
				
		p_left_bottom.add(p_left_bottom_left);
		p_left_bottom.add(Box.createRigidArea(new Dimension(5, 0)));
		p_left_bottom.add(p_left_bottom_right);
		p_left.add(p_left_bottom);
		p_left.add(Box.createRigidArea(new Dimension(0,5)));
		
		// right panel **********************************************************
		// contains navigation buttons, copy buttons and description of shortcuts
		//***********************************************************************
		Panel p_right = new Panel();
		p_right.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

		gbc.anchor = GridBagConstraints.CENTER; //navigation panel is centered in the right panel
		JLabel title = new JLabel(img.getWell().getPlate()+" -- "+img.getWell().getName());
		title.setFont(title.getFont().deriveFont((float) 18.0));
		gbc.gridx=0; p_right.add(title);
		gbc.gridy++;

		//the navigation panel **************************************************
		Panel p_right_top = new Panel();
		p_right_top.setLayout(new GridLayout(0,3));
		
		//load icons for navigation buttons 
		ImageIcon icon_left = CountDrops.getIcon("go-previous.png");		
		ImageIcon icon_right = CountDrops.getIcon("go-next.png");
		ImageIcon icon_up = CountDrops.getIcon("go-up.png");
		ImageIcon icon_down = CountDrops.getIcon("go-down.png");
		
		//creates navigation buttons
		JButton b_left = new JButton(icon_left);
		b_left.setActionCommand("MOVELEFT");
		b_left.addActionListener(this);
		b_left.setFocusable(false);
		if(img.getWell().getColInPlate()==0) b_left.setEnabled(false);
		
		JButton b_right = new JButton(icon_right);
		b_right.setActionCommand("MOVERIGHT");
		b_right.addActionListener(this);
		b_right.setFocusable(false);
		if(img.getWell().getColInPlate()==img.getPlate().getNCOLS()-1) b_right.setEnabled(false);
		
		JButton b_up = new JButton(icon_up);
		b_up.setActionCommand("MOVEUP");
		b_up.addActionListener(this);
		b_up.setFocusable(false);
		if(img.getWell().getRowInPlate()==0) b_up.setEnabled(false);
		//b_up.setPreferredSize(b_left.getPreferredSize());
		
		JButton b_down = new JButton(icon_down);
		b_down.setActionCommand("MOVEDOWN");
		b_down.addActionListener(this);
		b_down.setFocusable(false);
		if(img.getWell().getRowInPlate()==img.getPlate().getNROWS()-1) b_down.setEnabled(false);
		//b_down.setPreferredSize(b_left.getPreferredSize());
				
		//first line
		p_right_top.add(Box.createRigidArea(new Dimension(5, 0)));
		p_right_top.add(b_up);
		p_right_top.add(Box.createRigidArea(new Dimension(5, 0)));
		//second line
		p_right_top.add(b_left);
		p_right_top.add(Box.createRigidArea(new Dimension(5, 0)));
		p_right_top.add(b_right);
		//third line
		p_right_top.add(Box.createRigidArea(new Dimension(5, 0)));
		p_right_top.add(b_down);
		p_right_top.add(Box.createRigidArea(new Dimension(5, 0)));
		        
		gbc.anchor = GridBagConstraints.CENTER; //navigation panel is centered in the right panel
		gbc.gridx=0; p_right.add(p_right_top,gbc);
		gbc.gridy++;
		
		chkCloseWhenMoving = new JCheckBox("Close window after moving");
		chkCloseWhenMoving.setSize((int)(p_right_top.getWidth()*0.5), 50);
		chkCloseWhenMoving.setAlignmentX((float) 0.5);
		chkCloseWhenMoving.setSelected(true);
		chkCloseWhenMoving.setFocusable(false);
		
		p_right.add(chkCloseWhenMoving,gbc);
		gbc.gridy++;
		
		gbc.anchor = GridBagConstraints.WEST; //other components will be  left aligned 			
		p_right.add(Box.createRigidArea(new Dimension(0,25)),gbc);
		gbc.gridy++;
		
		// the copy panel ******************************************************************
		JTextPane lab= new JTextPane();
		lab.setContentType("text/html");
		lab.setBackground(this.getBackground());		
		lab.setText("<html><p style=\"font-family:Dialog\">Copy state of current well (empty, ignored or non-countable) to other wells.</p></html>");				
		lab.setFocusable(false);
		lab.setPreferredSize(new Dimension(150,50));
		
		gbc.fill=GridBagConstraints.HORIZONTAL;
		p_right.add(lab,gbc);
		gbc.gridy++;
						
		Panel p_right_bottom = new Panel();
		p_right_bottom.setLayout(new BoxLayout(p_right_bottom,BoxLayout.PAGE_AXIS));		
		//p_right_bottom.setBackground(Color.YELLOW);
						
		bCpy_row = new JButton("Copy to row");
		bCpy_row.setActionCommand("COPYTOROW");
		bCpy_row.addActionListener(this);
		bCpy_row.setAlignmentX((float) 0.0);
		bCpy_row.setFocusable(false);
		p_right_bottom.add(bCpy_row);
		
		bCpy_col = new JButton("Copy to column");
		bCpy_col.setActionCommand("COPYTOCOL");
		bCpy_col.addActionListener(this);
		bCpy_col.setAlignmentX((float) 0.0);
		bCpy_col.setFocusable(false);
		p_right_bottom.add(bCpy_col);
		
		bCpy_plate = new JButton("Copy to plate");
		bCpy_plate.setActionCommand("COPYTOPLATE");
		bCpy_plate.addActionListener(this);
		bCpy_plate.setAlignmentX((float) 0.0);
		bCpy_plate.setFocusable(false);
		p_right_bottom.add(bCpy_plate);
		
		gbc.fill=GridBagConstraints.NONE;
		p_right.add(p_right_bottom,gbc);		
		gbc.gridy++; 
		
		gbc.fill = GridBagConstraints.VERTICAL;
		p_right.add(Box.createVerticalGlue(),gbc);
		gbc.gridy++;
		
		// the shortcuts *********************************************************************
		gbc.fill = GridBagConstraints.HORIZONTAL;
		//using JLabel here would be possible but
		//pack does not compute multi-line JLabel dimensions correctly...
		JTextPane textShortCuts = new JTextPane();
		textShortCuts.setContentType("text/html");
		textShortCuts.setBackground(this.getBackground());
		//System.out.println(this.getFont());
		textShortCuts.setText("<html>" +
					"<h2 style=\"font-family:Dialog\">Shortcuts</h2>" +
				    "<p style=\"font-family:Dialog\">" + //;width:300px
						"<b>[SPACE]</b> : display/hide all CFUs<br>"+
						"<b>[CTRL]+a</b> : select all CFUs<br>" +
						"<b>[ESC]</b> : unselect all CFUs<br>" +
						"<b>[SUPPR]</b> : delete selected CFUs<br>" +
						"<b>[CRTL][SHIFT]+Key</b> : change type of selected CFUs<br><br>" +				
						"<b>[CTRL]+[SHIFT]+click</b> : change CFU to current type<br>"+
						"<b>[CTRL]+[ALT]+click</b> : split CFU<br><br>"+
						"<b>[ALT]+UP/DOWN</b> : zoom in/out<br>"+				
						"<b>[ALT]+RIGHT/LEFT</b> : change image slice<br><br>"+
						"<b>[CRTL]+RIGHT/LEFT/UP/DOWN</b> : move to a neighbor well<br>" +
						"<b>[CTRL]+w</b> : close window"+
					"</p></html>");
		textShortCuts.setEditable(false);
		textShortCuts.setFocusable(false);
		
		p_right.add(textShortCuts,gbc);
		gbc.gridy++;
						
		p_right.add(new JLabel("Summary of counts for sample "+stat.getID()),gbc);
		gbc.gridy++;
					
		gbc.gridwidth = 2;
		gbc.gridx=0;
		p_right.add(graphicStatistics,gbc);
		gbc.gridy++;
		
		JCheckBox chkLogScaleX = new JCheckBox("Use log scale for dilutions");
		chkLogScaleX.setSelected(true);
		chkLogScaleX.setFocusable(false);
		chkLogScaleX.setActionCommand("LOGSCALEX"); 
		chkLogScaleX.addActionListener(this);
		
		gbc.gridwidth = 1;
		gbc.gridx=0;
		p_right.add(chkLogScaleX,gbc);
		
		
		JCheckBox chkLogScaleY = new JCheckBox("Use log scale for counts");
		chkLogScaleY.setSelected(false);
		chkLogScaleY.setFocusable(false);
		chkLogScaleY.setActionCommand("LOGSCALEY"); 
		chkLogScaleY.addActionListener(this);
		
		gbc.gridwidth = 1;
		gbc.gridx=1;
		p_right.add(chkLogScaleY,gbc);
		gbc.gridy++;
		
		gbc.gridwidth = 2;
		gbc.gridx=0;
		
		main_panel.add(p_image);
		main_panel.add(Box.createRigidArea(new Dimension(5, 0)));
		main_panel.add(p_left);
		main_panel.add(Box.createRigidArea(new Dimension(5, 0)));
		main_panel.add(p_right);
				
		this.setContentPane(main_panel);
		pack();
		
		//position and size
		if(evt!=null) {				
			setSize(evt.getWindowSize());
			setLocation(evt.getLocation());												
		} else {
			setLocationRelativeTo(null); //center JFrame on screen
		}
		 		
		setVisible(true);		
		
		img.drawCFU();
		img.getWell().lock(); // writes a lock file in the well folder

	}
	
	
	//listen to changes in spinner
	@Override
	public void stateChanged(ChangeEvent ev) {
		JSpinner sp = (JSpinner) ev.getSource();
		img.setCFURadius((int) sp.getValue());		
	}	
	
	//methods from the window listener********************************************
	public void windowDeactivated(WindowEvent e) {
		//System.out.println("deactivated "+this.getTitle());
	}
	public void windowActivated(WindowEvent e) {
		//System.out.println("activated "+this.getTitle());
		img.getImageCanvas().requestFocusInWindow();
	}	
	public boolean close() {
		//System.out.println("close");
		if(img==null) return false;
		
		//save data if necessary
		img.getWell().save();
		
    	//unlock well and sends message to ImagePicture before closing window
    	img.getWell().unlock();	     	
		for(ViewWellListener l : listViewWellListener) l.viewWellHasClosed(viewWellEvent);
		img = null;		
		//call dispose?
		setVisible(false);
		//System.out.println("close done");
		return true;
	}
	//***************************************************************************
	
	public void addListener(ViewWellListener toAdd) {
		listViewWellListener.add(toAdd);
    }

	public void updateSummaryTable() {
		// update data
		summaryTableModel.initializeTable();
		
		// set selected row accordingly to currantCFUType in img
		int currentType = img.getCurrentCFUType() + 1;
		summaryTable.setRowSelectionInterval(currentType, currentType);
	}

	public void updateSelectionFromImageWell() {
		cfuTable.clearSelection();
		for (int i = 0; i < cfuTable.getRowCount(); i++) {
			// convert row position in table (which may have been sorted) to
			// position in data
			int j = cfuTable.convertRowIndexToModel(i);
			// convert position in data in CFU index
			int index = -1 + (int) cfuTable.getModel().getValueAt(j, 0);
			// is index in the list of selected CFU?
			if (img.isSelected(index)) {
				cfuTable.addRowSelectionInterval(i, i);
			}
		}
	}

	public void updateCommentButton() {
		ImageIcon icon_comment;				
		if(comments.getNbComments()>0) {
			icon_comment = CountDrops.getIcon("comment.png");
			buttonComment.setIcon(icon_comment);
			buttonComment.setToolTipText("Well "+img.getWell().getName()+" has "+comments.getNbComments()+" comments");
		} else {
			icon_comment = CountDrops.getIcon("comment_0.png");
			buttonComment.setIcon(icon_comment);
			buttonComment.setToolTipText("Well "+img.getWell().getName()+" has no comment");
		}
	}
	
	public void moveToNeighborWell(String where) throws InterruptedException {
		if(where.equals("MOVERIGHT") && img.getWell().getColInPlate()>=img.getPlate().getNCOLS()-1) return;
		if(where.equals("MOVELEFT")  && img.getWell().getColInPlate()<=0) return;
		if(where.equals("MOVEDOWN")  && img.getWell().getRowInPlate()>=img.getPlate().getNROWS()-1) return;
		if(where.equals("MOVEUP")    && img.getWell().getRowInPlate()<=0) return;
		
		
		
		
		//update ViewWellEvent settings that will be passed to whoever is listening
		GraphicsDevice screen = this.getGraphicsConfiguration().getDevice();		
		viewWellEvent.setScreen(screen);
		viewWellEvent.setAction(where);
		viewWellEvent.setLocation(this.getLocation());                             //update location so that the next ViewWell is opened where the current ViewWell is located
		viewWellEvent.setCloseWhenMoving(chkCloseWhenMoving.isSelected());
		viewWellEvent.setDoWand(chkDoWand.isSelected());
		viewWellEvent.setShowWellContour(chkShowWellCountour.isSelected());
		viewWellEvent.setSelectedType(summaryTable.getSelectedRow());
		viewWellEvent.setSlice(img.getImagePlus().getSlice());
		viewWellEvent.setCanvasMagnification(img.getImageCanvas().getMagnification());
		viewWellEvent.setWindowSize(this.getSize());
		
		for(ViewWellListener l : listViewWellListener) l.newViewWellAsked(viewWellEvent);
		
		
		if(chkCloseWhenMoving.isSelected()) {
			//Wait a bit so that ImagePicture can process ViewWellEvent before it is destroyed
			//not sure this is useful...
			Thread.sleep(1);			
			this.close();				
		}
		
	}
	// **********************
	// action listener
	// **********************
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();

		if (action == "DELETE") {
			img.deleteAllCFU();
			return;
		}

		if (action == "EMPTY") {
			img.getWell().setEmpty(chkEmpty.isSelected());
			for(ViewWellListener l : listViewWellListener) l.viewWellChange(viewWellEvent);
		}
		
		if (action == "IGNORE") {
			img.getWell().setIgnored(chkIgnore.isSelected());
			for(ViewWellListener l : listViewWellListener) l.viewWellChange(viewWellEvent);
		}
		
		if (action == "DOWAND") {
			img.setDoWand(chkDoWand.isSelected());
		}
		
		if (action == "SHOWWELLCONTOUR") {
			img.setShowWellContour(chkShowWellCountour.isSelected());
			img.drawSelectedCFU();
		}
		
		if (action == "MOVELEFT" || action == "MOVERIGHT" || action == "MOVEUP" || action == "MOVEDOWN") {
			try {
				moveToNeighborWell(action);
			} catch(InterruptedException ex) {
				
			}
		}

		if (action == "COPYTOROW" || action == "COPYTOCOL" || action == "COPYTOPLATE") {
			viewWellEvent.setAction(action);
			for(ViewWellListener l : listViewWellListener) l.viewWellCopyState(viewWellEvent);
		}
		
		if(action == "AUTODETECT") {
			new AutoDetect(img,viewWellEvent,listViewWellListener);
			cfuTable.requestFocus();			
		}

		if(action == "VIEWCOMMENTS") {
			//System.out.println("view comments!");
			ViewComments vc = new ViewComments(comments,this);			
		}
		
		if(action == "LOGSCALEX") {
			JCheckBox chk = (JCheckBox) e.getSource();
			graphicStatistics.setLogScaleX(chk.isSelected());
			graphicStatistics.updateMinMaxX();
			graphicStatistics.repaint();
		}
		
		if(action == "LOGSCALEY") {
			JCheckBox chk = (JCheckBox) e.getSource();
			graphicStatistics.setLogScaleY(chk.isSelected());
			graphicStatistics.updateMinMaxY();
			graphicStatistics.repaint();
		}
	}

	// ************************************************************************
	// key listener (KeyBinding should probably be used instead of KeyListener)
	// ************************************************************************
	public void keyPressed(KeyEvent evt) {
		//this seems to be necessary because if CTRL has been pressed **before** window is opened no keyEvent
		//is issued and CTRLpressed will be wrongly set to false. Ideally this should be done upon window opening.
		//Think about whether to add the same sort of command for alt and shift keys...
		if(evt.isControlDown()) {
			CTRLpressed = true;
			img.setCTRLpressed(true);
		} 
		if(evt.isAltDown()) {
			ALTpressed = true;
			img.setALTpressed(true);			
		}
		if(evt.isShiftDown()) {
			SHIFTpressed = true;
			img.setSHIFTpressed(true);
		}
		
		switch (evt.getKeyCode()) {
		case KeyEvent.VK_META: //fall through
		case KeyEvent.VK_CONTROL:
			CTRLpressed = true;
			img.setCTRLpressed(true);
			//img.setAddToSelection(true);
			break;
		case KeyEvent.VK_SHIFT:
			SHIFTpressed = true;
			img.setSHIFTpressed(true);
			break;
		case KeyEvent.VK_ALT:
			ALTpressed = true;
			img.setALTpressed(true);
			break;

		// DELETE / SUPPR
		case KeyEvent.VK_DELETE: // fall through!! VK_DELETE and VK_BACK_SPACE
									// both trigger delete
		case KeyEvent.VK_BACK_SPACE:
			img.deleteSelectedCFU();
			break;

		case KeyEvent.VK_SPACE:
			if (CTRLpressed && SHIFTpressed) {
				//suppress type for selected CFU
				img.unsetTypeForSelectedCFU();
				//select NA in summary table
				summaryTable.setRowSelectionInterval(0,0);
			} else {
				if (!CTRLpressed && !SHIFTpressed) {
					// display / hide all CFUs
					img.setShowAllCFU(!img.isShowAllCFU());
					img.drawSelectedCFU();
					//prevent JTable to react to that key!
					evt.consume();
				}
			}
			break;

		// ARROWS
		// with CTRL pressed, move to a neighboring well
		// with ALT pressed up or down arrows zoom in or out
		// with CTRL nor pressed left and right arrows change slice
		case KeyEvent.VK_RIGHT:
		case KeyEvent.VK_KP_RIGHT:
			if (CTRLpressed && !ALTpressed) {
				try {
					moveToNeighborWell("MOVERIGHT");
				} catch (InterruptedException ex) {}
			}
			if (!CTRLpressed && ALTpressed){
				img.nextSlice();
			}
			//prevent JTable to react to that key!!
			evt.consume();
			break;
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_KP_LEFT:
			if (CTRLpressed && !ALTpressed) {
				try {
				  moveToNeighborWell("MOVELEFT");
				} catch (InterruptedException ex) {}
			}
			if (!CTRLpressed && ALTpressed){
				img.prevSlice();
			}
			//prevent JTable to react to that key!!
			evt.consume();
			break;

		case KeyEvent.VK_UP:
		case KeyEvent.VK_KP_UP:
			if (ALTpressed && !CTRLpressed) {
				img.zoomIn();
				//TODO on mac and windows full screen is lost if imaged in zoomed
				this.setPreferredSize(this.getSize());
				pack();
			}
			if(CTRLpressed) {
				try {
				  moveToNeighborWell("MOVEUP");
				} catch (InterruptedException ex) {}
			}
			break;
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_KP_DOWN:
			if (ALTpressed && !CTRLpressed) {
				img.zoomOut();
				//TODO on mac and windows full screen is lost if imaged in zoomed 
				this.setPreferredSize(this.getSize());
				pack();
			}
			if(CTRLpressed) {
				try {
				  moveToNeighborWell("MOVEDOWN");
				} catch (InterruptedException ex) {}
			}
			break;

		case KeyEvent.VK_ESCAPE:
			img.deselectAllCFU();
			img.setMouseDragged(false);
			img.clearOverlay();
			break;

		default:
			String key = KeyEvent.getKeyText(evt.getKeyCode());
			if (CTRLpressed && !SHIFTpressed) {				
				if (key.equals("A")) {
					// select all CFU
					// useful if cfuTable has lost focus...
					img.selectAllCFU();
					img.drawSelectedCFU();
				}
			}
			if (CTRLpressed && SHIFTpressed) {
				// change CFU type for selected CFU
				img.changeTypeForSelectedCFU(key);
				for(int i=0;i<summaryTable.getRowCount();i++) {
					String s = (String) (summaryTableModel.getValueAt(i,1));
					if(s.equalsIgnoreCase(key)) summaryTable.setRowSelectionInterval(i,i);
				}
			}
			if(CTRLpressed && key.equals("W")) {
				close();
			}
			break;
		}
	}

	public void keyReleased(KeyEvent evt) {
		switch (evt.getKeyCode()) {
		case KeyEvent.VK_CONTROL:
			CTRLpressed = false;
			img.setCTRLpressed(false);			
			break;
		case KeyEvent.VK_SHIFT:
			SHIFTpressed = false;
			img.setSHIFTpressed(false);
			break;
		case KeyEvent.VK_ALT:
			ALTpressed = false;
			img.setALTpressed(false);
			break;
		}
	}

	public void keyTyped(KeyEvent evt) {
	}

	// ******************************************************
	// Functions that handle the events sent out by ImageWell
	// ******************************************************	
	public void SelectionHasChanged() {
		// selection has changed in ImageWell, and JTable will adjust to this
		// change.
		// But RowListener must not react by notifying selection changes in
		// JTable back to ImageWell!
		cfuTableRowListener.setIsDeaf(true);
		updateSelectionFromImageWell();
		cfuTableRowListener.setIsDeaf(false);
	}

	public void CFUedited() {
		cfuTableRowListener.setIsDeaf(true);
		CFUTableModel m = (CFUTableModel) cfuTable.getModel();
		m.updateCFU();
		updateSelectionFromImageWell();
		updateSummaryTable();
		cfuTableRowListener.setIsDeaf(false);
	} // CFU data (e.g. CFU type) have been changed

	public void CFUadded() {
		cfuTableRowListener.setIsDeaf(true);
		// CFUs have been added in ImageWell
		CFUTableModel m = (CFUTableModel) cfuTable.getModel();
		m.addRow();
		updateSummaryTable();
		cfuTableRowListener.setIsDeaf(false);
	}

	public void CFUremoved() {
		cfuTableRowListener.setIsDeaf(true);
		// CFUs have been deleted in ImageWell
		// the notification must be sent out by ImageWell before selectedCFI is
		// cleared
		CFUTableModel m = (CFUTableModel) cfuTable.getModel();
		m.removeRow();
		updateSummaryTable();
		cfuTableRowListener.setIsDeaf(false);
	}


	@Override
	public void commentsHaveChanged(ViewCommentsEvent evt) {
		updateCommentButton();
		for(ViewWellListener l : listViewWellListener) l.viewWellChange(viewWellEvent);
	}

		
}
