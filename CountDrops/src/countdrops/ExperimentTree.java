package countdrops;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;


public class ExperimentTree extends JTree {
	private static final long serialVersionUID = 1L;

	Experiment experiment;
	boolean showImagePath = false;	
	
	public ExperimentTree () {
		super(new DefaultMutableTreeNode("No experiment loaded"));
	}
	
		
	//update tree content after a change in experiment
	public void update() {		
		DefaultTreeModel model = (DefaultTreeModel) this.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();		
		root.removeAllChildren();
		
		if(experiment==null) {
			//no experiment loaded
			root.setUserObject("No experiment loaded");			
			updateUI();
			return;
		}
		
		//root is experiment	
		root.setUserObject(experiment);		

		//root's children are pictures
		for(int i=0;i<experiment.getNbPictures();i++) {			
			Picture p = experiment.getPicture(i);
			if(p!=null) {
				DefaultMutableTreeNode pNode =  new DefaultMutableTreeNode(p);

				//add each new picture to the tree root
				root.add(pNode);

				//pictures' children are plates
				for(int j=0;j<p.getNbPlates();j++) {
					//add each plate as a child of picture node
					Plate pl = p.getPlate(j);
					DefaultMutableTreeNode plNode =  new DefaultMutableTreeNode(pl);
					pNode.add(plNode);
				}
			}
		}										
			
		updateUI();
		for(int i=0;i<this.getRowCount();i++) {
			expandRow(i);			
		}	
		
	}
	
	public void update(Experiment ex) {		
		experiment = ex;
		update();		
	}
	
	public boolean isExperimentSelected() {
		TreePath s = this.getSelectionPath();
		if (s == null) return false; //no selection
		DefaultMutableTreeNode n = (DefaultMutableTreeNode) (s.getLastPathComponent()); // the selected node
		if(n.isRoot()) return true;
		return false;
	}
	
	public void selectPlate(Plate pl) {
		if(pl==null) return;
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.getModel().getRoot();

		for(int i=0;i<root.getChildCount();i++) {
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) root.getChildAt(i);
			for(int j=0;j<n.getChildCount();j++) {
				DefaultMutableTreeNode leaf = (DefaultMutableTreeNode) n.getChildAt(j);
				if(leaf.getUserObject() == pl) {
					this.setSelectionPath(new TreePath(leaf.getPath()));
					this.expandPath(new TreePath(n.getPath()));
					return;
				}
			}
		}

	}

	public void selectPicture(Picture p) {
		if(p==null) return;
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.getModel().getRoot();
		
		for(int i=0;i<root.getChildCount();i++) {
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) root.getChildAt(i);
			if(n.getUserObject() == p) {
				this.setSelectionPath(new TreePath(n.getPath()));
				this.expandPath(new TreePath(n.getPath()));
				return;
			}
		}
	}
	
	public int getSelectedPictureIndex() {
		TreePath s = this.getSelectionPath();
		if (s == null) return -1; //no selection
		
		DefaultMutableTreeNode n = (DefaultMutableTreeNode) (s.getLastPathComponent()); // the selected node
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.getModel().getRoot(); //the root node

		if(n==root) return -1; //nothing to display if root is selected
		if(n.getParent() == root) {    			
			//the selected node corresponds to a picture
			int i = root.getIndex(n);
			if(i<0 || i>=experiment.getNbPictures()) return -1;
			return i;		
		} else {
			//the selected node corresponds to a plate
			int i = root.getIndex(n.getParent()); //index of the picture node
			if(i<0 || i>=experiment.getNbPictures()) return -1;
			return i;		
		}		

	}

	public Picture getSelectedPicture() {
		TreePath s = this.getSelectionPath();
		if (s == null) return null; //no selection
		
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.getModel().getRoot(); //the root node
		DefaultMutableTreeNode n = (DefaultMutableTreeNode) (s.getLastPathComponent()); // the selected node
		
		if(n==root) return null; //nothing to display if root is selected
		
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) (n.getParent()); // the parent of the selected node		
		if(parent == root) {    			
			//the selected node corresponds to a picture
			return((Picture) n.getUserObject());			
		} else {
			//the selected node corresponds to a plate
			return((Picture) parent.getUserObject());
		}		
	}

	public boolean isPictureSelected() {
		TreePath s = this.getSelectionPath();
		if (s == null) return false; //no selection
		DefaultMutableTreeNode n = (DefaultMutableTreeNode) (s.getLastPathComponent()); // the selected node
		if(n.getUserObject() instanceof Picture) return true;
		return false;
	}

	public int getSelectedPlateIndex() {
		TreePath s = this.getSelectionPath();
		if (s == null) return -1; //no selection

		DefaultMutableTreeNode n = (DefaultMutableTreeNode) (s.getLastPathComponent()); // the selected node
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.getModel().getRoot(); //the root node

		if(n==root) return -1; //nothing to display if root is selected
		
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) (n.getParent()); // the parent of the selected node
		if(parent == root) return -1; 

		//the selected mode corresponds to a plate						
		Picture p = (Picture) parent.getUserObject();

		int j = parent.getIndex(n);    //index of the plate node
		if(j<0 || j>=p.getNbPlates()) return -1;
		return j;				

	}

	public Plate getSelectedPlate() {
		TreePath s = this.getSelectionPath();
		if (s == null) return null; //no selection
		
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.getModel().getRoot(); //the root node
		DefaultMutableTreeNode n = (DefaultMutableTreeNode) (s.getLastPathComponent()); // the selected node
		
		if(n==root) return null; //nothing to display if root is selected
		
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) (n.getParent()); // the parent of the selected node		
		if(parent == root) {    			
			//the selected node corresponds to a picture
			return(null);			
		} else {
			//the selected node corresponds to a plate
			return((Plate) n.getUserObject());
		}		
	}

	public boolean isPlateSelected() {
		TreePath s = this.getSelectionPath();
		if (s == null) return false; //no selection
		DefaultMutableTreeNode n = (DefaultMutableTreeNode) (s.getLastPathComponent()); // the selected node
		if(n.getUserObject() instanceof Plate) return true;
		return false;
	}
	
	public void setShowImagePath(boolean b) {
		this.showImagePath = b;
	}
	
	public boolean isShowImagePath() {
		return showImagePath;
	}
	
	public void unfold() {
		unfold(true);
	}
	
	public void fold() {
		unfold(false);
	}
	
	public void unfold(boolean b) {
		for(int i=this.getRowCount()-1;i>-1;i--) {
			TreePath p = this.getPathForRow(i);
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) p.getLastPathComponent();
			if(n.getUserObject() instanceof Picture && !b) {
				//only picture nodes must be collapsed if b is false
				this.collapseRow(i);
			} else {
				this.expandRow(i);
			}
		}
        
		this.updateUI();
	}

	public void unfold(String str) {
		for(int i=this.getRowCount()-1;i>-1;i--) {
			TreePath paf = this.getPathForRow(i);
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) paf.getLastPathComponent();
			if(n.getUserObject() instanceof Picture) {
				Picture p = (Picture) n.getUserObject();
				boolean expand = p.getFileName().contains(str);
				for(int j=0;j<p.getNbPlates() && !expand;j++) {
					if(p.getPlate(j).getName().contains(str)) expand = true;
				}
				//picture node is expanded if some of its plate have names that contain str
				if(expand) {
					this.expandRow(i);					
				} else {
					this.collapseRow(i);					
				}
			} else {
				this.expandRow(i);
			}
		}
        
		this.updateUI();
	}

	public int search(String str) {
		int pos = 0;
		if(getSelectionCount()>0) {
			pos = getSelectionRows()[0];
			pos++;
			if(pos>=getRowCount()) pos = 0;
		}		

		for(int i=pos;i<this.getRowCount();i++) {
			TreePath paf = this.getPathForRow(i);
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) paf.getLastPathComponent();
			if(n.getUserObject() instanceof Picture) {
				Picture p = (Picture) n.getUserObject();
				if(p.getFileName().contains(str)) {
					expandRow(i);
					setSelectionRow(i);
					this.updateUI();
					return i;					
				}				
				for(int j=0;j<p.getNbPlates();j++) {
					if(p.getPlate(j).getName().contains(str)) {
						expandRow(i);
						setSelectionRow(i);
						this.updateUI();
						return i;
					}
				}
			}
		}		
		return -1;
	}
}

class ExperimentTreeRenderer extends DefaultTreeCellRenderer {
	private static final long serialVersionUID = 1L;
	DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
	JPanel    pane = new JPanel();
	JTextPane txt  = new JTextPane();
	JLabel    lab1  = new JLabel();
	JLabel    lab2  = new JLabel();
	
    public ExperimentTreeRenderer() {
        backgroundSelectionColor    = defaultRenderer.getBackgroundSelectionColor();
        backgroundNonSelectionColor = defaultRenderer.getBackgroundNonSelectionColor();
        
        txt.setContentType("text/html");
        txt.setOpaque(false);
        
        pane.setPreferredSize(new Dimension(400, 24));
        pane.setOpaque(false);
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        pane.add(txt);
        pane.add(lab1);
        pane.add(Box.createRigidArea(new Dimension(5,0)));
        pane.add(lab2);
    }

    public Component getTreeCellRendererComponent(
                        JTree tree,
                        Object value,
                        boolean sel,
                        boolean expanded,
                        boolean leaf,
                        int row,
                        boolean hasFocus) {

    	if (value != null || (value instanceof DefaultMutableTreeNode))  {    	        
    		Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
    		    		    		    	    	
    		Icon ic = null;
    		Icon ic_comments = null;
    		String str = "<html><span style=\"font-family:Dialog;font-size:12pt\">";
    		if(userObject instanceof Experiment) {
    			//this is root!
    			Experiment exp = (Experiment) userObject;
    			try {
    				String s = exp.getPath()+exp.getSettings().getFileName();
    				if(s.length()>52) s = "..."+s.substring(s.length()-51,s.length());
    				str+="<b>"+s+"</b>";
    			} catch(Exception ex) {}
    		}

    		if (userObject instanceof Plate) {
    			//this is a plate
    			Plate pl = (Plate) userObject;
    			
    			try {
    				if(pl.getNbCFUs()<=0) str+="<b>";
    				str+=pl.getName()+" ["+pl.getNbCFUs()+" CFUs";
    				if(pl.hasNACFU()) str += " <i>NA</i>";
    				if(pl.hasNonCountable()) str += " <i>NC</i>";
    				str+="]";
    				if(pl.getNbCFUs()<=0) str+="</b>";
    				if(!pl.hasWellToInspect() && !pl.hasProblems()) {
    					ic = CountDrops.getIcon("ok.png");
    				}
    				if(pl.hasProblems()) {
    					ic = CountDrops.getIcon("warning.png");    			
    				}
    				if(pl.hasComments()) {
    					ImageIcon icon = CountDrops.getIcon("comment.png");
    					Image img = icon.getImage() ;  
    					Image newimg = img.getScaledInstance( (int) (icon.getIconWidth()*0.75), (int) (icon.getIconHeight()*0.75),  java.awt.Image.SCALE_SMOOTH ) ;  
    					ImageIcon icon2 = new ImageIcon( newimg );
    					   
    					ic_comments = (Icon) icon2;
    					
    				}
    			} catch(Exception ex) {    				
    			}
    		} 

    		if (userObject instanceof Picture) {
    			//this is a picture    			
    			Picture p = (Picture) userObject;
    			ExperimentTree tr = (ExperimentTree) tree;
    			try {
    				if(tr.isShowImagePath()) {
    					str += p.getPath()+p.getFileName();
    				} else {
    					str +=p.getFileName();
    				} 
    			} catch(Exception ex) {}   			
    		} 
    		
    		
    		if(userObject instanceof Experiment || userObject instanceof Plate || userObject instanceof Picture) {
    			if(str!=null) str+="</span></html>";
    			
    			txt.setText(str);    			
    			lab1.setIcon(ic);
    			lab2.setIcon(ic_comments);
    			    			    		        			
    			//pane.setVisible(true);
    			return pane;
    		}

    	}
        return defaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded,
                leaf, row, hasFocus);
    }

}
