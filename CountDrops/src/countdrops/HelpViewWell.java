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
					"<h2 style=\"font-family:Dialog\">Shortcuts</h2>" +
				    "<table style=\"font-family:Dialog\">"+
				    "<tr>"+
				    "<td>"+
					"<b>[SPACE]</b> : display/hide all CFUs<br>"+
					"<b>[CTRL]+a</b> : select all CFUs<br>" +
					"<b>[ESC]</b> : unselect all CFUs<br>" +
				    "</td>"+
					"<td>"+
					"<b>[SUPPR]</b> : delete selected CFUs<br>" +
					"<b>[CTRL]+[ALT]+click</b> : split CFU<br><br>"+
					"<b>[CRTL][SHIFT]+Key</b> : change type of selected CFUs<br>" +				
					"<b>[CTRL]+[SHIFT]+click</b> : change CFU to current type<br>"+						
				    "</td>"+
					"<td>"+
					"<b>[ALT]+UP/DOWN</b> : zoom in/out<br>"+				
					"<b>[ALT]+RIGHT/LEFT</b> : change image slice<br><br>"+
					"<b>[CRTL]+RIGHT/LEFT/UP/DOWN</b> : move to a neighbor well<br>" +
					"<b>[CTRL]+w</b> : close window"+
					"</td>"+
					"</tr>"+
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
