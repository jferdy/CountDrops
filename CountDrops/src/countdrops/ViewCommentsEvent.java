package countdrops;

import java.util.EventObject;


public class ViewCommentsEvent extends EventObject {
	private static final long serialVersionUID = 1L;	
	Comments comments = null;
	
	public ViewCommentsEvent(Comments c) {
    	super(c);
    	comments = c;
	}
	
	public int getNbComments() {
		if(comments==null) return 0;		
		return comments.getNbComments(); 
	}
	
	public boolean hasComments() {
		if(comments==null) return false;
		if(comments.getNbComments()>0) return true;
		return false;
	}
}
