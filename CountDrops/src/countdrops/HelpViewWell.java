package countdrops;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextPane;

public class HelpViewWell extends JDialog {
	private static final long serialVersionUID = 6248163580382439212L;
	
	public HelpViewWell() {
		// the shortcuts *********************************************************************		
		//using JLabel here would be possible but
		//pack does not compute multi-line JLabel dimensions correctly...
		JTextPane textShortCuts = new JTextPane();
		textShortCuts.setContentType("text/html");
		textShortCuts.setBackground(this.getBackground());
		//System.out.println(this.getFont());
		textShortCuts.setText("<html>" +
					"<h1 style=\"font-family:Dialog\">Shortcuts</h1>" +
				    "<table style=\"font-family:Dialog\">"+
				    "<tr><td><b>[SPACE]</b></td><td>display/hide all CFUs</td></tr>"+
					"<tr><td><b>[CTRL]+a</b></td><td> select all CFUs</td></tr>" +
					"<tr><td><b>[ESC]</b></td><td> unselect all CFUs</td></tr>" +
					"<tr><td><b>[SUPPR]</b></td><td>delete selected CFUs</td></tr>" +
					"<tr><td><b>[CTRL]+[ALT]+click</b> or <b>right click</b></td><td>split CFU</td></tr>"+
					"<tr><td><b>[CRTL][SHIFT]+Key</b></td><td> change type of selected CFUs</td></tr>" +				
					"<tr><td><b>[CTRL]+[SHIFT]+click</b></td><td>change CFU to current type</td></tr>"+						
					"<tr><td><b>[ALT]+UP/DOWN</b></td><td>zoom in/out</td></tr>"+				
					"<tr><td><b>[ALT]+RIGHT/LEFT</b></td><td>change image slice</td></tr>"+
					"<tr><td><b>[CRTL]+RIGHT/LEFT/UP/DOWN</b></td><td>move to a neighbor well</td></tr>" +
					"<tr><td><b>[CTRL]+w</b></td><td>close window</td></tr>"+
				    "</table></html>");
		textShortCuts.setEditable(false);
		textShortCuts.setFocusable(false);
		
		JPanel main_panel = new JPanel();				
		main_panel.setLayout(new BoxLayout(main_panel,BoxLayout.PAGE_AXIS)); //3 columns of equal width		
		main_panel.add(textShortCuts);		
		this.setContentPane(main_panel);
		pack();		
	}

}
