package countdrops;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
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
	private JScrollPane p_image;
	
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
	private JButton buttonComment,buttonHelp;
	
	// count table
	private SummaryTableModel summaryTableModel;
	private JTable summaryTable;
	JCheckBox chkEmpty,chkIgnore, chkShowWellCountour,chkCloseWhenMoving;
	JButton   bCpy_row,bCpy_col, bCpy_plate;
	JSpinner  spinRadius,spinDoWand;
	JRadioButton chkDoWandYes,chkDoWandNo;
	private GraphCanvas graphicStatistics = null;
	
	// key pressed and other flags
	// private KeyStrokeAction keyStrokeAction;
	private Boolean CTRLpressed = false;
	private Boolean SHIFTpressed = false;
	private Boolean ALTpressed = false;
	private boolean Xreversed = false;
	private boolean Yreversed = false;

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
			Xreversed = evt.isXreversed();
			Yreversed = evt.isYreversed();			
		}
								
		//If no CFU and no NC, well is set to empty
		//This is to speed up things when scanning a bunch of empty wells: you don't have to click on 
		//anything, well will be set to empty automatically.
		if(img.getWell().guessIfEmpty()) {
			img.getWell().setEmpty();
		}		
		viewWellEvent = new ViewWellEvent(img.getWell(),this.getLocation());
		comments = img.getWell().getComments();	
		
		setResizable(true);		
		this.setUndecorated(false); //for some reason the cross button to close dialog does not show up...		
		setTitle(img.getImagePlus().getTitle());
				
		//image panel
		p_image = new JScrollPane(img.getImageCanvas(),JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				
		// creates cfuTable
		cfuTableModel = new CFUTableModel(img);
		cfuTable = new JTable(cfuTableModel);		
		
		// set cfuTable selection model : reacts to any change in selection by
		// selecting the right CFUs in img
		cfuTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		cfuTable.setRowSelectionAllowed(true);
		cfuTable.setColumnSelectionAllowed(false); 
		cfuTableRowListener = new CFUTableRowListener(img, cfuTable);
		cfuTable.getSelectionModel().addListSelectionListener(cfuTableRowListener);		
		
		// this allows to sort the table by clicking on column headers
		cfuTableSorter = new TableRowSorter<CFUTableModel>(cfuTableModel);
		cfuTable.setRowSorter(cfuTableSorter);
		cfuTable.addKeyListener(this);

		// scroll panel for cfuTable
		cfuTable.setFillsViewportHeight(true);
		JScrollPane cfuTableScrollPanel = new JScrollPane(cfuTable,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
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
		chkIgnore = new JCheckBox("Ignore data from this well in result file");
		chkIgnore.setAlignmentX((float) 0.0); // left alignement
		chkIgnore.setFocusable(false);
		chkIgnore.setSelected(img.getWell().isIgnored());
		chkIgnore.setActionCommand("IGNORE");
		chkIgnore.addActionListener(this);
		p_left_bottom_left.add(chkIgnore);
		
		graphicStatistics = new GraphCanvas(stat); 		//will be added in right panel

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
		JTableHeader summaryHeader = summaryTable.getTableHeader(); //header must be added to panel explicitly because summaryTable is not inside a JScroll...
		summaryHeader.setAlignmentX(summaryTable.getAlignmentX());
		summaryTable.setToolTipText("Select line to change the current type of CFU");
		
		p_left_bottom_left.add(summaryHeader);
		p_left_bottom_left.add(summaryTable);
		
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
		buttonComment.setToolTipText("View/add comments for this well.");

		//view help
		buttonHelp = new JButton();			
		buttonHelp.setIcon(CountDrops.getIcon("help-faq.png"));
		buttonHelp.setAlignmentX((float) 1.0);
		buttonHelp.setActionCommand("VIEWHELP");
		buttonHelp.addActionListener(this);
		buttonHelp.setFocusable(false);
		buttonHelp.setOpaque(false);
		buttonHelp.setContentAreaFilled(false);
		buttonHelp.setBorderPainted(false);					
		buttonHelp.setToolTipText("View help on shorcuts.");
		
		chkShowWellCountour = new JCheckBox("Show well contour");
		if(evt!=null) {
			chkShowWellCountour.setSelected(evt.isShowWellContour());
		}
		chkShowWellCountour.setFocusable(false);
		chkShowWellCountour.setActionCommand("SHOWWELLCONTOUR"); 
		chkShowWellCountour.addActionListener(this);				
		chkShowWellCountour.setToolTipText("Display well contour on image.");
		
		JLabel labDoWand = new JLabel("Create CFU using Magic Wand");
		ButtonGroup grpDoWand =	new ButtonGroup();
		chkDoWandYes = new JRadioButton("Yes",true);		
		chkDoWandNo = new JRadioButton("No",false);		
		grpDoWand.add(chkDoWandYes);
		grpDoWand.add(chkDoWandNo);				
		chkDoWandYes.setActionCommand("DOWAND");
		chkDoWandYes.addActionListener(this);
		chkDoWandYes.setFocusable(false);
		chkDoWandNo.setActionCommand("DOWAND");
		chkDoWandNo.addActionListener(this);
		chkDoWandNo.setFocusable(false);
		chkDoWandYes.setToolTipText("New CFUs will be created using \"Magic Wand\".");
		chkDoWandNo.setToolTipText("New CFUs will be created as simple circles.");
		
		SpinnerModel spinRadiusModel = new SpinnerNumberModel(img.getCFURadius(), //initial value
                1, //min
                img.getImagePlus().getWidth()/2, //max
                1); 
		spinRadius = new JSpinner(spinRadiusModel);
		spinRadius.setName("Circle radius");
		spinRadius.setFocusable(false);		
		spinRadius.setAlignmentX((float) 1.0); // right alignement	
		spinRadius.addChangeListener(this);
		//spinRadius.addKeyListener(this);
		spinRadius.getEditor().getComponent(0).addKeyListener(this); //not great
		spinRadius.setToolTipText("Radius of circle.");
		
		JLabel labSpinRadius = new JLabel("Circle radius");
		labSpinRadius.setAlignmentX( Component.RIGHT_ALIGNMENT );//0.0
			
		SpinnerModel spinDoWandModel = new SpinnerNumberModel(img.getCFURadius(), //initial value
		1, //min
		100, //max
		1); 
		spinDoWand = new JSpinner(spinDoWandModel);
		spinDoWand.setName("Magic Wand tolerance");
		spinDoWand.setFocusable(false);		
		spinDoWand.setAlignmentX((float) 1.0);
		spinDoWand.addChangeListener(this);		
		//spinDoWand.addKeyListener(this);
		spinDoWand.getEditor().getComponent(0).addKeyListener(this);
		spinDoWand.setToolTipText("Tolerance of Magic Wand.");
		
		JLabel labSpinDoWand = new JLabel("Magic Wand tolerance");
		labSpinDoWand.setAlignmentX( Component.RIGHT_ALIGNMENT );//0.0
		
		if(evt!=null) {
			chkDoWandYes.setSelected(evt.isDoWand());
			chkDoWandNo.setSelected(!evt.isDoWand());
			spinRadius.setValue(evt.getCircleRadius());
			spinDoWand.setValue(evt.getDoWandTolerance());
		}
		img.setDoWand(chkDoWandYes.isSelected());
		if(chkDoWandYes.isSelected()) {
			spinDoWand.setEnabled(true);
			spinRadius.setEnabled(false);
		} else {
			spinDoWand.setEnabled(false);
			spinRadius.setEnabled(true);
		}

		img.setShowWellContour(chkShowWellCountour.isSelected());		
		
		JButton b1 = new JButton("Delete all");		
		b1.setActionCommand("DELETE");
		b1.addActionListener(this);
		b1.setFocusable(false);		
		b1.setToolTipText("Delete all CFUs.");
		
		JButton b2 = new JButton("Autodetect");
		b2.setActionCommand("AUTODETECT");
		b2.addActionListener(this);
		b2.setFocusable(false);
		b2.setToolTipText("Auto-detect CFUs in current well.");
		
        //title *****************************************************************	        		
		JLabel title = new JLabel("Plate "+img.getWell().getPlate()+" -- Well "+img.getWell().getName());
		title.setFont(title.getFont().deriveFont((float) 24.0));				

		
		//the copy buttons panel ********************************************************
		String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		JLabel cpylab= new JLabel("Copy \"empty\", \"ignore\" or \"infinite\" state to other wells");
		
		bCpy_row = new JButton("Copy to row");
		bCpy_row.setActionCommand("COPYTOROW");
		bCpy_row.addActionListener(this);
		bCpy_row.setAlignmentX((float) 0.0);
		bCpy_row.setFocusable(false);			
		bCpy_row.setToolTipText("Copy empty, infinite or ignore to all wells of row "+String.valueOf(letters.charAt(img.getWell().getRowInPlate())));
		
		bCpy_col = new JButton("Copy to column");
		bCpy_col.setActionCommand("COPYTOCOL");
		bCpy_col.addActionListener(this);
		bCpy_col.setAlignmentX((float) 0.0);
		bCpy_col.setFocusable(false);		
		bCpy_col.setToolTipText("Copy empty, infinite or ignore to all wells of column "+(img.getWell().getColInPlate()+1));
		
		bCpy_plate = new JButton("Copy to plate");
		bCpy_plate.setActionCommand("COPYTOPLATE");
		bCpy_plate.addActionListener(this);
		bCpy_plate.setAlignmentX((float) 0.0);
		bCpy_plate.setFocusable(false);		
		bCpy_plate.setToolTipText("Copy empty, infinite or ignore to all wells of plate "+img.getWell().getPlate());
		
		//the navigation panel **************************************************			
		//load icons for navigation buttons 
		ImageIcon icon_left = CountDrops.getIcon("go-previous.png");		
		ImageIcon icon_right = CountDrops.getIcon("go-next.png");
		ImageIcon icon_up = CountDrops.getIcon("go-up.png");
		ImageIcon icon_down = CountDrops.getIcon("go-down.png");
		
		//creates navigation buttons
		int nc = img.getWell().getColInPlate();
		int nr = img.getWell().getRowInPlate();
		JButton b_left = new JButton(icon_left);
		if(Xreversed) {
			b_left.setActionCommand("MOVERIGHT");
			if(img.getWell().getColInPlate()==img.getPlate().getNCOLS()-1) b_left.setEnabled(false);
			nc++;
		} else {
			b_left.setActionCommand("MOVELEFT");
			if(img.getWell().getColInPlate()==0) b_left.setEnabled(false);
			nc--;			
		}
		b_left.addActionListener(this);
		b_left.setFocusable(false);		
		if(nr>=0 && nc>=0 && nr<letters.length()) b_left.setToolTipText("Move to "+letters.charAt(nr)+(nc+1));
		
		nc = img.getWell().getColInPlate();
		nr = img.getWell().getRowInPlate();
		JButton b_right = new JButton(icon_right);
		if(Xreversed) {
			b_right.setActionCommand("MOVELEFT");
			if(img.getWell().getColInPlate()==0) b_right.setEnabled(false);
			nc--;
		} else {
			b_right.setActionCommand("MOVERIGHT");
			if(img.getWell().getColInPlate()==img.getPlate().getNCOLS()-1) b_right.setEnabled(false);
			nc++;
		}
		b_right.addActionListener(this);
		b_right.setFocusable(false);
		if(nr>=0 && nc>=0 && nr<letters.length()) b_right.setToolTipText("Move to "+letters.charAt(nr)+(nc+1));
		
		nc = img.getWell().getColInPlate();
		nr = img.getWell().getRowInPlate();
		JButton b_up = new JButton(icon_up);
		if(Yreversed) {
			b_up.setActionCommand("MOVEDOWN");
			if(img.getWell().getRowInPlate()==img.getPlate().getNROWS()-1) b_up.setEnabled(false);
			nr++;
		} else {
			b_up.setActionCommand("MOVEUP");
			if(img.getWell().getRowInPlate()==0) b_up.setEnabled(false);
			nr--;
		}
		b_up.addActionListener(this);
		b_up.setFocusable(false);			
		if(nr>=0 && nc>=0 && nr<letters.length()) b_up.setToolTipText("Move to "+letters.charAt(nr)+(nc+1));
		
		nc = img.getWell().getColInPlate();
		nr = img.getWell().getRowInPlate();
		JButton b_down = new JButton(icon_down);
		if(Yreversed) {
			b_down.setActionCommand("MOVEUP");
			if(img.getWell().getRowInPlate()==0) b_down.setEnabled(false);
			nr--;
		} else {
			b_down.setActionCommand("MOVEDOWN");
			if(img.getWell().getRowInPlate()==img.getPlate().getNROWS()-1) b_down.setEnabled(false);
			nr++;
		}
		b_down.setActionCommand("MOVEDOWN");
		b_down.addActionListener(this);
		b_down.setFocusable(false);
		if(nr>=0 && nc>=0 && nr<letters.length()) b_down.setToolTipText("Move to "+letters.charAt(nr)+(nc+1));
		
		chkCloseWhenMoving = new JCheckBox("Close window after moving");		
		chkCloseWhenMoving.setSelected(true);
		chkCloseWhenMoving.setFocusable(false);					
			
		JPanel p_navigation = new JPanel();
		p_navigation.setLayout(new GridLayout(0,3));
		//first line
		p_navigation.add(Box.createRigidArea(new Dimension(5, 0)));
		p_navigation.add(b_up);
		p_navigation.add(Box.createRigidArea(new Dimension(5, 0)));
		//second line
		p_navigation.add(b_left);
		p_navigation.add(Box.createRigidArea(new Dimension(5, 0)));
		p_navigation.add(b_right);
		//third line
		p_navigation.add(Box.createRigidArea(new Dimension(5, 0)));
		p_navigation.add(b_down);
		p_navigation.add(Box.createRigidArea(new Dimension(5, 0)));
																								
		//the summary graphic************************
		JLabel sampleTitle = new JLabel("Sample "+stat.getID());
		sampleTitle.setFont(title.getFont().deriveFont((float) 24.0));
		
		JCheckBox chkLogScaleX = new JCheckBox("Use log scale for dilutions");
		chkLogScaleX.setSelected(true);
		chkLogScaleX.setFocusable(false);
		chkLogScaleX.setActionCommand("LOGSCALEX"); 
		chkLogScaleX.addActionListener(this);
				
		JCheckBox chkLogScaleY = new JCheckBox("Use log scale for counts");
		chkLogScaleY.setSelected(false);
		chkLogScaleY.setFocusable(false);
		chkLogScaleY.setActionCommand("LOGSCALEY"); 
		chkLogScaleY.addActionListener(this);

						
		//Create and populate panels
		//**************************
		
		// image/screen dimensions		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int widthLeft = 2*ximg.getImagePlus().getWidth();
		int widthCenter = ((int) (screenSize.getWidth())-widthLeft)/2; 
		int widthRight = (int) (screenSize.getWidth()-widthLeft-widthCenter );
				
		// left panel contains title, image, comment and help buttons
		JPanel left_panel = new JPanel();
		left_panel.setLayout(new BoxLayout(left_panel, BoxLayout.PAGE_AXIS));		
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		left_panel.add(title);
		p_image.setPreferredSize(new Dimension(widthLeft,widthLeft));
		left_panel.add(p_image);
				
		JPanel bottom_left_panel = new JPanel();
		bottom_left_panel.setLayout(new BoxLayout(bottom_left_panel, BoxLayout.LINE_AXIS));
		bottom_left_panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		bottom_left_panel.add(Box.createHorizontalGlue());
		bottom_left_panel.add(buttonComment);		
		bottom_left_panel.add(buttonHelp);
		left_panel.add(bottom_left_panel);
		
		//center panel contains CFU table, summary of counts and most buttons
		JPanel center_panel = new JPanel();
		center_panel.setLayout(new BoxLayout(center_panel, BoxLayout.PAGE_AXIS));		
		cfuTableScrollPanel.setPreferredSize(new Dimension(widthCenter,widthCenter*5/4));
		center_panel.add(cfuTableScrollPanel);		
		
		JPanel bottom_center_panel = new JPanel();		
		bottom_center_panel.setLayout(new GridLayout(0,2));
		summaryTable.setPreferredSize(new Dimension(widthCenter/2,widthCenter/4));
		
		JPanel bottom_left_center_panel = new JPanel();
		bottom_left_center_panel.setLayout(new BoxLayout(bottom_left_center_panel, BoxLayout.PAGE_AXIS));
		bottom_left_center_panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		bottom_left_center_panel.add(new JLabel("Counts per CFU type"));
		bottom_left_center_panel.add(summaryHeader);
		bottom_left_center_panel.add(summaryTable);		
		bottom_left_center_panel.add(chkEmpty);
		bottom_left_center_panel.add(chkIgnore);
		bottom_left_center_panel.add(chkShowWellCountour);
		
		JPanel bottom_right_center_panel = new JPanel();
		bottom_right_center_panel.setLayout(new BoxLayout(bottom_right_center_panel, BoxLayout.PAGE_AXIS));
		b1.setAlignmentX(Component.RIGHT_ALIGNMENT);
		b2.setAlignmentX(Component.RIGHT_ALIGNMENT);
		bottom_right_center_panel.add(Box.createRigidArea(new Dimension(0,5)));
		bottom_right_center_panel.add(b1);
		bottom_right_center_panel.add(Box.createRigidArea(new Dimension(0,5)));
		bottom_right_center_panel.add(b2);
		bottom_right_center_panel.add(Box.createRigidArea(new Dimension(0,10)));
		
		JPanel doWand_panel = new JPanel();		
		doWand_panel.setLayout(new BoxLayout(doWand_panel, BoxLayout.PAGE_AXIS));		
		doWand_panel.setMaximumSize(new Dimension(300,100));
		JPanel doWand_yes_panel = new JPanel();		
		doWand_yes_panel.setLayout(new FlowLayout(FlowLayout.TRAILING));
		JPanel doWand_no_panel = new JPanel();
		doWand_no_panel.setLayout(new FlowLayout(FlowLayout.TRAILING));
		doWand_yes_panel.add(chkDoWandYes);
		doWand_yes_panel.add(spinDoWand);
		doWand_no_panel.add(chkDoWandNo);
		doWand_no_panel.add(spinRadius);
		doWand_yes_panel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		doWand_no_panel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		labDoWand.setAlignmentX(Component.RIGHT_ALIGNMENT);
		doWand_panel.add(labDoWand);
		doWand_panel.add(doWand_yes_panel);
		doWand_panel.add(doWand_no_panel);		
		doWand_panel.setAlignmentX(Component.RIGHT_ALIGNMENT);				
		bottom_right_center_panel.add(doWand_panel);
		bottom_right_center_panel.add(Box.createVerticalGlue());

		bottom_center_panel.add(bottom_left_center_panel);
		bottom_center_panel.add(bottom_right_center_panel);
				
		JPanel cpy_panel = new JPanel();		
		cpy_panel.setLayout(new FlowLayout(FlowLayout.CENTER));
		cpy_panel.add(bCpy_col);
		cpy_panel.add(bCpy_row);
		cpy_panel.add(bCpy_plate);
		
		center_panel.add(Box.createVerticalGlue());				
		center_panel.add(bottom_center_panel);
		cpylab.setAlignmentX(Component.CENTER_ALIGNMENT);
		center_panel.add(cpylab);
		center_panel.add(cpy_panel);
		
		//right panel
		JPanel right_panel = new JPanel();		
		right_panel.setLayout(new BoxLayout(right_panel, BoxLayout.PAGE_AXIS));		
		sampleTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
		right_panel.add(sampleTitle);
		graphicStatistics.setPreferredSize(new Dimension(widthRight,widthRight));
		graphicStatistics.setAlignmentX(Component.CENTER_ALIGNMENT);
		chkLogScaleX.setAlignmentX(Component.CENTER_ALIGNMENT);
		chkLogScaleY.setAlignmentX(Component.CENTER_ALIGNMENT);
		right_panel.add(graphicStatistics);
		right_panel.add(chkLogScaleX);
		right_panel.add(chkLogScaleY);
		right_panel.add(Box.createVerticalGlue());
		
		p_navigation.setMaximumSize(new Dimension(3*50,3*50));
		p_navigation.setAlignmentX(Component.CENTER_ALIGNMENT);					
		chkCloseWhenMoving.setAlignmentX(Component.CENTER_ALIGNMENT);
		right_panel.add(p_navigation);		
		right_panel.add(chkCloseWhenMoving);
		
		left_panel.setBorder(BorderFactory.createEmptyBorder(0,10,0,5));
		center_panel.setBorder(BorderFactory.createEmptyBorder(28,5,0,5));
		right_panel.setBorder(BorderFactory.createEmptyBorder(0,5,0,10));
		
		JPanel main_panel = new JPanel();				
		main_panel.setLayout(new GridLayout(0,3)); //3 columns of equal width
		main_panel.setPreferredSize(screenSize);

		main_panel.add(left_panel);
		main_panel.add(center_panel);
		main_panel.add(right_panel);
		
		this.setContentPane(main_panel);
		pack();
		
		
		//position and size
		if(evt!=null) {				
			if(evt.getWindowSize()!=null) setSize(evt.getWindowSize());
			setLocation(evt.getLocation());												
		} else {
			setLocationRelativeTo(null); //center JFrame on screen
		}
		
		img.drawCFU();
		centerImageView(); 		
								
		img.getWell().lock(); // writes a lock file in the well folder
		
		setVisible(true);		
	}
	
	
	//listen to changes in spinner
	@Override
	public void stateChanged(ChangeEvent ev) {		
		JSpinner sp = (JSpinner) ev.getSource();
		int value = (int) sp.getValue();
		String name = sp.getName(); 
		if(name.equals(this.spinDoWand.getName())) {	
			viewWellEvent.setDoWandTolerance(value);
			img.setDoWandTolerance(value);
		}
		if(name.equals(this.spinRadius.getName())) {
			viewWellEvent.setCircleRadius(value);
			img.setCFURadius(value);	
		}				
	}	

	private void centerImageView() {
		//center image on JScrollPane
		Rectangle bounds = p_image.getViewport().getViewRect();
		Dimension size = p_image.getViewport().getViewSize();
		int x = (size.width - bounds.width) / 2;
		int y = (size.height - bounds.height) / 2;
		p_image.getViewport().setViewPosition(new Point(x, y));
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
		viewWellEvent.setDoWand(chkDoWandYes.isSelected());
		viewWellEvent.setDoWandTolerance((Integer) spinDoWand.getValue());
		viewWellEvent.setCircleRadius((Integer) spinRadius.getValue());
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

		if (action == "VIEWHELP") {
			HelpViewWell h = new HelpViewWell();
			h.setLocationRelativeTo(this);
			h.setVisible(true);
		}
		
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
			img.setDoWand(chkDoWandYes.isSelected());
			viewWellEvent.setDoWand(chkDoWandYes.isSelected());
			if(chkDoWandYes.isSelected()) {
				spinDoWand.setEnabled(true);
				spinRadius.setEnabled(false);
			} else {
				spinDoWand.setEnabled(false);
				spinRadius.setEnabled(true);
			}
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
			AutoDetect a = new AutoDetect(img,viewWellEvent,listViewWellListener);
			a.setLocationRelativeTo(this);
			a.setVisible(true);
			cfuTable.requestFocus();			
		}

		if(action == "VIEWCOMMENTS") {
			//System.out.println("view comments!");
			ViewComments vc = new ViewComments(comments,this);
			vc.setLocationRelativeTo(this);
			vc.setVisible(true);
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
		Object o = evt.getSource();
		
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
					if(Xreversed) moveToNeighborWell("MOVELEFT");
					else moveToNeighborWell("MOVERIGHT");					
				} catch (InterruptedException ex) {}
			}
			if (!CTRLpressed && ALTpressed){
				img.nextSlice();
			}
			//prevent JTable to react to that key!!			
			if(o.equals(cfuTable)) evt.consume();
			break;
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_KP_LEFT:
			if (CTRLpressed && !ALTpressed) {
				try {
					if(Xreversed) moveToNeighborWell("MOVERIGHT");
					else moveToNeighborWell("MOVELEFT");
				} catch (InterruptedException ex) {}
			}
			if (!CTRLpressed && ALTpressed){
				img.prevSlice();
			}
			//prevent JTable to react to that key!!				
			if(o.equals(cfuTable)) evt.consume();
			break;

		case KeyEvent.VK_UP:
		case KeyEvent.VK_KP_UP:
			if (ALTpressed && !CTRLpressed) {
				img.zoomIn();
				centerImageView();
				pack();
				evt.consume();
			}
			if(CTRLpressed) {
				try {
				  if(Yreversed) moveToNeighborWell("MOVEDOWN");
				  else moveToNeighborWell("MOVEUP");
				  evt.consume();
				} catch (InterruptedException ex) {}
			}
			break;
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_KP_DOWN:
			if (ALTpressed && !CTRLpressed) {
				img.zoomOut();				
				centerImageView();
				pack();
				evt.consume();
			}
			if(CTRLpressed) {
				try {
				  if(Yreversed) moveToNeighborWell("MOVEUP");
				  else moveToNeighborWell("MOVEDOWN");
				  evt.consume();
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
