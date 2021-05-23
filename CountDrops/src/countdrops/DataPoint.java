package countdrops;

import java.awt.geom.Ellipse2D;

public class DataPoint extends Ellipse2D.Double {	
	private static final long serialVersionUID = 3677925661408005438L;
	
	private String msg;
	
	public DataPoint(int xx,int yy,int ssize,String mmsg) {
		super(xx - ssize/2.0, yy - ssize/2.0, ssize, ssize);
	}

	@Override
    public String toString() {
        return msg;
    }
}
