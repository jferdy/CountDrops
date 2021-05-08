package countdrops;

import java.awt.Dimension;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextArea;



public class ViewComments extends JDialog implements ActionListener, KeyListener {
	private static final long serialVersionUID = 1L;
	private Comments comments = null;
	private JTextArea tp_who,tp_what;
	private String who,what;			
	private Panel paneComments = null; 
	
	//event sent out to whoever is listening
	private ViewCommentsEvent viewCommentsEvent;
	
	//listeners hooked up onto that window (typically opened by ViewWell to monitor changes in comments)
	ArrayList<ViewCommentsListener> listViewCommentsListener = new ArrayList<ViewCommentsListener>();
		
	public ViewComments(Comments c,ViewWell parent) {
		comments = c;
		viewCommentsEvent = new ViewCommentsEvent(c);
		
		tp_who  = new JTextArea("who?");
		tp_what = new JTextArea("what?");
		tp_who.setEditable(true);
		tp_who.setEnabled(true);
		tp_what.setEditable(false);
		tp_what.setEnabled(false);
		
		tp_who.addKeyListener(this);
		tp_what.addKeyListener(this);
		tp_who.setToolTipText("Hit Ctrl-Enter to validate");
		tp_what.setToolTipText("Hit Ctrl-Enter to validate");
		
		Panel pane = new Panel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));			
		pane.add(tp_who);
		pane.add(Box.createRigidArea(new Dimension(0,2)));
		pane.add(tp_what);
		this.setContentPane(pane);
		
		paneComments = new Panel();
		paneComments.setLayout(new BoxLayout(paneComments, BoxLayout.PAGE_AXIS));
		pane.add(paneComments);
		pane.add(Box.createRigidArea(new Dimension(0,5)));
		
		update();
		
		this.setMinimumSize(new Dimension(500,0));		
		addListener(parent);
		
		this.setModal(true);
		//setVisible(true);
	}
	
	public void update() {
		
		paneComments.removeAll();		
		if(comments.getNbComments()>0) {
		for(int i=0;i<comments.getNbComments();i++) {			
			paneComments.add(new BubblePanel(i,comments.getComment(i),this));			
		}
		} else {
			JLabel  lab = new JLabel("No comments.");			
			lab.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
			paneComments.add(lab);
			
		}		
		pack();
	}
	
	public void addListener(ViewCommentsListener toAdd) {
		listViewCommentsListener.add(toAdd);
    }
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		
		if(evt.getActionCommand().equals("SUPPRCOMMENT")) {
			JButton b = (JButton) evt.getSource();
			int i = Integer.parseInt(b.getName());
			comments.removeComment(i);
			for(ViewCommentsListener l : listViewCommentsListener) l.commentsHaveChanged(viewCommentsEvent);
			update();
		}
	}

	@Override
	public void keyPressed(KeyEvent evt) {			
		switch (evt.getExtendedKeyCode()) {
		case KeyEvent.VK_ESCAPE: {
			dispose();
			break;
		}
		case KeyEvent.VK_ENTER: {
			if(! evt.isControlDown() ) return;
			JTextArea tp = (JTextArea) evt.getSource();
			if(tp==tp_who) {
				who = tp_who.getText();
				tp_who.setEditable(false);
				tp_who.setEnabled(false);
				tp_what.setEditable(true);
				tp_what.setEnabled(true);
				tp_what.setText("");				
				tp_what.setCaretPosition(0);	
				tp_what.requestFocus();
			}
			if(tp==tp_what) {
				what = tp_what.getText();
				comments.addComment(who,what);
				
				tp_who.setEditable(true);
				tp_who.setEnabled(true);
				tp_what.setEditable(false);
				tp_what.setEnabled(false);
				tp_who.setText(who);
				tp_what.setText("what?");
				tp_who.setCaretPosition(0);
				tp_who.requestFocus();
				
				what = null;
			
				for(ViewCommentsListener l : listViewCommentsListener) l.commentsHaveChanged(viewCommentsEvent);
				update();
			}
			break;
		}
		default: {			
			break;
		}
		}		
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyTyped(KeyEvent evt) {	
		// TODO Auto-generated method stub
	}
}
