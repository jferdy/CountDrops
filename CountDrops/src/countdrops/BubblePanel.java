package countdrops;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;

import org.w3c.dom.Element;


public class BubblePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private Element comment = null;
	private int num = -1;
    private JTextField textField01 = null;
    private JTextField textField02 = null;
    
	public BubblePanel(int k,Element c,ViewComments vc)        {		
		super(new BorderLayout());
		comment = c;
		num = k; //position of the comment in list of comments
		
		final JPanel p = new JPanel();
		final JPanel pg = new JPanel();
		p.setLayout(new BoxLayout(p,BoxLayout.LINE_AXIS));
		pg.setLayout(new BoxLayout(pg,BoxLayout.PAGE_AXIS)); //new GridLayout(5,1,5,5));
		textField01 = new JTextField(20) {
			private static final long serialVersionUID = 1L;
			//Unleash Your Creativity with Swing and the Java 2D API!
			//http://java.sun.com/products/jfc/tsc/articles/swing2d/index.html
			@Override protected void paintComponent(Graphics g) {
				if(!isOpaque()) {
					int w = getWidth();
					int h = getHeight();
					Graphics2D g2 = (Graphics2D)g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(UIManager.getColor("TextField.background"));
					g2.fillRoundRect(0, 0, w-1/2, h-1/2, h, h);
					g2.fillRect(0,h/2,w,h/2);
					//g2.setColor(Color.BLUE);
					g2.dispose();
				}
				super.paintComponent(g);
			}
		};
		textField01.setOpaque(false);
		textField01.setBackground(new Color(0,0,0,0));
		textField01.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		textField01.setText(comment.getAttribute("author")+" ("+comment.getAttribute("timestamp")+")");
		textField01.setEditable(false);
		
		textField02 = new JTextField(20) {
			private static final long serialVersionUID = 1L;
			//Unleash Your Creativity with Swing and the Java 2D API!
			//http://java.sun.com/products/jfc/tsc/articles/swing2d/index.html
			@Override protected void paintComponent(Graphics g) {
				if(!isOpaque()) {
					int w = getWidth();
					int h = getHeight();
					Graphics2D g2 = (Graphics2D)g.create();
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(UIManager.getColor("TextField.background"));
					g2.fillRoundRect(0, 0, w-1/2, h-1/2, h, h);					
					g2.fillRect(0,0,w,h/2);					
					g2.dispose();
				}
				super.paintComponent(g);
			}
		};
		textField02.setOpaque(false);
		textField02.setBackground(new Color(0,0,0,0));
		//textField02.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		textField02.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		textField02.setText(comment.getTextContent());	
		textField02.setEditable(false);
		
		pg.add(textField01);
		pg.add(textField02);
		p.add(pg);
		
		ImageIcon icon_rm = CountDrops.getIcon("list-remove-small.png");		
		JButton b_suppr = new JButton(icon_rm);
		b_suppr.setOpaque(false);
		b_suppr.setContentAreaFilled(false);
		b_suppr.setBorderPainted(false);
		b_suppr.addActionListener(vc);
		b_suppr.setName(Integer.toString(num));
		b_suppr.setActionCommand("SUPPRCOMMENT");	
		p.add(Box.createRigidArea(new Dimension(2,0)));
		p.add(b_suppr);
		
		add(p);
		setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	}

}


class RoundedCornerBorder extends AbstractBorder {
	private static final long serialVersionUID = 1L;
	@Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int r = height-1;
        RoundRectangle2D round = new RoundRectangle2D.Float(x, y, width-1, height-1, r, r);
        Container parent = c.getParent();
        if(parent!=null) {
            g2.setColor(parent.getBackground());
            Area corner = new Area(new Rectangle2D.Float(x, y, width, height));
            corner.subtract(new Area(round));
            g2.fill(corner);
        }
        g2.setColor(Color.GRAY);
        g2.draw(round);
        g2.dispose();
    }
    @Override public Insets getBorderInsets(Component c) {
        return new Insets(4, 8, 4, 8);
    }
    @Override public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.right = 8;
        insets.top = insets.bottom = 4;
        return insets;
    }
}
