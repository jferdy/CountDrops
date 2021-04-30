package countdrops;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

public class GraphCanvas extends JPanel implements MouseMotionListener {			
        private SampleStatistics statistics = null;
        private int CFUtype = -1;
        
        private int pointRadius = 10;
        
        private int htext = -1;
        private int grWidth = -1;
        private int grHeight = -1;
        
        private double minX = 0;
        private double maxX = 0;
        private double minY = 0;
        private double maxY = 0;
        
        private double scaleX = 1.0;
        private double scaleY = 1.0;

        private boolean logScaleX = true;
        private boolean logScaleY = false;
        
        private int[] ptX = null;
        private int[] ptY = null;
        
        public GraphCanvas(SampleStatistics st) {
            super();
            statistics = st;
            
            ptX = new int[statistics.getNBcounts()];
            ptY = new int[statistics.getNBcounts()];
            
            setBackground(new java.awt.Color(255, 255, 255));
            setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            
            updateMinMaxX();
            updateMinMaxY();
            
            this.addMouseMotionListener(this);
        }
        
        public void setLogScaleX(boolean b) {logScaleX=b;}
        public void setLogScaleY(boolean b) {logScaleY=b;}
        
        public int  getCFUtype() {return CFUtype;}        
        public void setCFUtype(int i) {
        	if(i<0 || i>statistics.getNbCFUtype()+1) return;
        	CFUtype=i;
        	}
        
        public void updateCounts(int[] x) {
    		statistics.updateCounts(x);		
    	}
        
        public void updateMinMaxX() {
        	if(statistics==null) return;
        	//computes min and max X
            minX = statistics.getDilution(0);
            if(logScaleX) minX = Math.log10(minX);
            maxX = minX;
            for(int i = 1; i < statistics.getNBcounts(); i++) {
            	double z = statistics.getDilution(i);
            	if(logScaleX) z = Math.log10(z);
            	
            	if(z>maxX) maxX=z;
            	if(z<minX) minX=z;
            }
            scaleX = 1.0/(maxX-minX);
        }
        
        public void updateMinMaxY() {
        	if(statistics==null) return;
        	            
        	minY = -1;
            maxY = 0;
            for(int i = 0; i < statistics.getNBcounts(); i++) {
            	double z = statistics.getCount(i, CFUtype);
            	if(logScaleY) {
            		if(z>0) {
            			z = Math.log10(z);
            		} else {
            			z = -1.0;
            		}
            	}
            	if(z>=0) {
            		if(z>maxY) maxY=z;
            		if(minY<0 || z<minY) minY=z;
            	}
            }
            if(minY<0) minY = 0;
            scaleY = 1.0/(maxY-minY);
        }
                
        public void paint(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            
            FontMetrics metrics = g.getFontMetrics();
            htext = metrics.getHeight();            
            grWidth = getWidth()-(pointRadius+4*htext);
            grHeight = getHeight()-(pointRadius+4*htext);
            g.drawRect(4*htext,pointRadius, grWidth,grHeight);  
            
            /*
             //TODO rotating text does not work!!
            String str = "counts";
            if(logScaleY) {
            	str = str + " (log scale)";            
            }
            int w  = metrics.stringWidth(str);                       
            AffineTransform tr = g.getTransform();
            g.rotate(-Math.PI/2);

            //g.drawString(str, htext, 2*htext+grHeight/2-w/2);
            g.drawString(str, getHeight()/2,getWidth()/2);
            g.setTransform(tr);
            */
            
            String str = "dilution";
            if(logScaleX) {
            	str = str + " (log scale)";            
            }
            g.drawString(str, 4*htext+grWidth/2-metrics.stringWidth(str)/2, pointRadius+grHeight+2*htext);
            
            
            if(CFUtype<0) return;
            updateMinMaxY();
            
            Color bgColor = Color.WHITE;
            if(CFUtype>0) {
            	bgColor = statistics.getCFUColor(CFUtype-1);
            }
            
            for(int i = 0; i < statistics.getNBcounts(); i++) {
            	double x = 1.0;
            	if(logScaleX) {
            		x = Math.log10(statistics.getDilution(i));
            	} else {
            		x = statistics.getDilution(i);
            	}
            	            	
            	double y = statistics.getCount(i,CFUtype);
            	boolean inftyY = false;            	            
            	if((int) y == -1) { // y is infinite positive
            		inftyY = true;
            		y = maxY;
            	}
            	if(logScaleY & !inftyY) {
            		if(y<=0) { // log(y) is infinite negative            			
            			inftyY = true;
            			y = minY;
            		} else {            			
            			y = Math.log10(y);            			
            		}
            	}
            	
            	if(y>=0) {            		            		
            		ptX[i] = 4*htext + (int) (grWidth*(x-minX)*scaleX + 0.5);
            		ptY[i] = pointRadius + (int) (grHeight*(1-(y-minY)*scaleY));
            		
            		if(!inftyY) {
            			g.setColor(bgColor);
            			g.fillOval(ptX[i]-pointRadius/2,(int) ptY[i]-pointRadius/2,pointRadius,pointRadius);
            			g.setColor(Color.BLACK);
            			g.drawOval(ptX[i]-pointRadius/2,(int) ptY[i]-pointRadius/2,pointRadius,pointRadius);
            		} else {
            			int[] xx = new int[3];
            			int[] yy = new int[3];
            			xx[0] = ptX[i]-pointRadius/2;
            			xx[1] = ptX[i]+pointRadius/2;
            			xx[2] = ptX[i];
            			yy[0] = ptY[i];
            			yy[1] = ptY[i];
            			if((int) y == (int) minY) {
            				// log(y) is -Infty : triangle is downward
            				yy[2] = ptY[i]+ pointRadius/2;
            			} else {
            				// y or log(y) is + Infty : triangle is upward
            				yy[2] = ptY[i]- pointRadius/2;
            			}
            			g.setColor(bgColor);
            			g.fillPolygon(xx, yy, 3);
            			g.setColor(Color.BLACK);
            			g.drawPolygon(xx,yy, 3);            				            			
            		}
            		if(i==statistics.getPosOfCurrentWell()) {
            			g.drawOval(ptX[i]-pointRadius,ptY[i]-pointRadius,2*pointRadius,2*pointRadius);
            		}            		
            		if(statistics.getIgnored(i)) {
            			g.drawLine(ptX[i]-pointRadius, ptY[i]-pointRadius, ptX[i]+pointRadius, ptY[i]+pointRadius);
            			g.drawLine(ptX[i]-pointRadius, ptY[i]+pointRadius, ptX[i]+pointRadius, ptY[i]-pointRadius);
            		}
            	}
            }
            
            //draws ticks and tick labels on x scale
            int y = pointRadius + grHeight;
            for(int i = 0; i < statistics.getNbUniqueDilutionValues(); i++) {
            	double z = -1.0;
            	if(logScaleX) {
            		z = Math.log10(statistics.getUniqueDilutionValue(i));
            	} else {
            		z = statistics.getUniqueDilutionValue(i);
            	}
            	int x = 4*htext + (int) (grWidth*(z-minX)*scaleX + 0.5);            	
            	g.drawLine(x,y,x,y+5);
            	str = ""+z;
            	g.drawString(str,x-metrics.stringWidth(str)/2,y+htext);
            }
          //draws ticks and tick labels on y scale            
          int x = 4*htext;  
          for(int i=0;i<5;i++) {
        	  double z = minY + (maxY-minY)*i/5.0;
        	  y = pointRadius + (int) (grHeight*(1-(z-minY)*scaleY));
        	  if(logScaleY) {
        		  str = ""+((int) (Math.pow(10.0,z)*100))/100.0;
        	  } else {
        		  str = ""+((int) z*100)/100.0;  
        	  }        	  
        	          	  
        	  g.drawLine(x-5,y,x,y);        	  
        	  g.drawString(str,x-6-metrics.stringWidth(str),y+htext/3);
          }
        }
        
		@Override
		public void mouseDragged(MouseEvent arg0) {			
		}
		
		@Override
		public void mouseMoved(MouseEvent evt) {
			int x = evt.getX();					
			int y = evt.getY();
						
			String txt = "";
			for(int i=0;i<statistics.getNBcounts();i++) {
				double d = Math.sqrt(Math.pow(x-ptX[i],2.0)+Math.pow(y-ptY[i],2.0));
				//System.out.println("(" +x+ "," + "," +y+ ") <-> ("+ptX[i]+","+ptY[i]+") d="+d);				
				if(d < pointRadius/2.0) {
					if(!txt.equals("")) txt += "\n";
					txt += "plate "+statistics.getPlateName(i)+", ";
					txt += "well "+statistics.getWellName(i);
					txt += " (dilution: "+statistics.getDilution(i)+", ";
					txt += "volume: "+statistics.getVolume(i)+"): ";
					if(statistics.getCount(i,CFUtype)==-1) {
						txt += "count is Inf.";
					} else {
						txt += "count = "+statistics.getCount(i,CFUtype);
					}					
				}
			}
			
			if(!txt.equals("")) {
				JOptionPane.showMessageDialog(this,txt,"Sample "+statistics.getID(),JOptionPane.INFORMATION_MESSAGE);
			}
		}
    }
