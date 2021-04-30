package countdrops;

import ij.gui.GenericDialog;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;


public class KeyStrokeAction extends AbstractAction {

	private ImageWell img;
	
	public KeyStrokeAction (ImageWell ximg) {
		super();
		img = ximg;
	}
	
    public void actionPerformed( ActionEvent tf )
        {
            // provides feedback to the console to show that the enter key has
            // been pressed
    		GenericDialog gd = new GenericDialog( "Action :" + tf.getActionCommand() );
    		gd.addMessage("Paf!");
    		gd.showDialog();

            System.out.println();
            
        } // end method actionPerformed()
   
}
