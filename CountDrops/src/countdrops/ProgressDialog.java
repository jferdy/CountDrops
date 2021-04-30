package countdrops;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;


public class ProgressDialog extends JDialog {	
	private static final long serialVersionUID = 1L;
	private JLabel label = null; 
	private JProgressBar progressBar = null;  
	private int total = -1;
	private int n = -1;
	
	public ProgressDialog(Dialog owner, String title,int nb) {
		super(owner, title);
		total = nb;
		n = 0;		
		init(owner);
	}

	public ProgressDialog(Frame owner, String title,int nb) {
		super(owner, title);
		total = nb;
		n = 0;		
        init(owner);
	}
	
	public ProgressDialog(Dialog owner, String title) {
		super(owner, title);
	}

	public ProgressDialog(Frame owner, String title) {
		super(owner, title);     
	}

	public void init(Component c) {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		label = new JLabel("Starting...");
		label.setPreferredSize(new Dimension(500,50));
		progressBar = new JProgressBar(n, total);
		progressBar.setIndeterminate(false);
		progressBar.setStringPainted(true);
		progressBar.setPreferredSize(new Dimension(500,25));
		
		pane.add(label);                       
		pane.add(progressBar);		
		this.add(pane);
		
		setPreferredSize(new Dimension(500,100));        
		pack();
		
		Point pt = c.getLocation();
		pt.x+=c.getWidth()/2;
		pt.y+=c.getHeight()/2;
		pt.x-=this.getWidth()/2;
		pt.y-=this.getHeight()/2;
		setLocation(pt);
	}
	
	public void setProgress(String txt) {
		//System.out.println("["+Thread.currentThread().getName()+" "+Thread.currentThread().getId()+"] setProgress");
		n++;
		label.setText(txt);
		progressBar.setValue(n);		
	}
	
	public void setText(String txt) {
		label.setText(txt);
	}
	
	public void setProgress() {		
		n++;
		progressBar.setValue(n);		
	}
	
	public void setProgress(int n) {		
		progressBar.setValue(n);		
	}
}
