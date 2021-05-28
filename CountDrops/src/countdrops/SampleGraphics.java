package countdrops;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.border.BevelBorder;

public class SampleGraphics extends JPanel {			      
		private static final long serialVersionUID = 1336326822937286575L;
		
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
        
        private final int defaulInitialDelay = ToolTipManager.sharedInstance().getInitialDelay();

        public SampleGraphics(SampleStatistics st) {
            super();                       
            
            statistics = new SampleStatistics(st);
            
            
            ptX = new int[statistics.getNBcounts()];
            ptY = new int[statistics.getNBcounts()];
            updateMinMaxX();
            updateMinMaxY();
                        
            setBackground(new java.awt.Color(255, 255, 255));            
            setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
                                                
            setToolTipText("");
            addMouseListener(new MouseAdapter() {
            	  public void mouseEntered(MouseEvent me) {
            	    ToolTipManager.sharedInstance().setInitialDelay(0);
            	  }
            	  public void mouseExited(MouseEvent me) {
            		  ToolTipManager.sharedInstance().setInitialDelay(defaulInitialDelay);
            	  }
            	});
        }
        
        public SampleStatistics getSampleStatistics() {
        	return(statistics);	
        }
        
        public void setLogScaleX(boolean b) {logScaleX=b;}
        public void setLogScaleY(boolean b) {logScaleY=b;}
        
        public String getID() {
        	return statistics.getID();	
        }

        public int  getCFUtype() {return CFUtype;}        
        public void setCFUtype(int i) {
        	if(i<0 || i>statistics.getNbCFUtype()+1) return;
        	CFUtype=i;
        	}
        
        public void updateCounts(int[] x) {
    		statistics.updateCounts(x);
    		repaint();
    	}
        
        public void updateCounts(Well w) {
    		statistics.updateCounts(w);	
    		repaint();
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
            maxY = 5;
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
            
            if(!logScaleY) {
            	minY = (int) (minY/5)*5.0;
            	maxY = (1+(int) (maxY/5))*5.0;
            } else {
            	minY = 0.0;
            	maxY = Math.round(Math.log10(maxY)+1);
            }
            scaleY = 1.0/(maxY-minY);
        }
         
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            
            //buttonList.clear();
            
            Graphics2D g = (Graphics2D) g0;
            
            //improves text rendering on graphic            
            Map<?, ?> desktopHints = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
            if (desktopHints != null) {
            	    g.setRenderingHints(desktopHints);
            }            
            //improves the rendering of ovals and lines
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            
            //box
            FontMetrics metrics = g.getFontMetrics();
            htext = metrics.getHeight();            
            grWidth = getWidth()-(pointRadius+4*htext);
            grHeight = getHeight()-(pointRadius+4*htext);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(4*htext,pointRadius, grWidth,grHeight);  
            
             //TODO rotating text does not work!!
            String str = "counts";
            if(logScaleY) {
            	str = str + " (log scale)";            
            }

            AffineTransform tr = g.getTransform();
            g.rotate(-Math.PI/2.0);
            g.setColor(Color.BLACK);
            g.drawString(str,-grHeight/2-metrics.stringWidth(str)/2,2*htext);
            g.setTransform(tr);
            
            //x axis label
            str = "dilution";
            if(logScaleX) {
            	str = str + " (log scale)";            
            }
            g.setColor(Color.BLACK);
            g.drawString(str, 4*htext+grWidth/2-metrics.stringWidth(str)/2, pointRadius+grHeight+2*htext);
            
            
            if(CFUtype<0) return;
            updateMinMaxY();

            //draws ticks and tick labels on x scale
            int posLabY = -100;
            for(int i = 0; i < statistics.getNbUniqueDilutionValues(); i++) {
            	double z = statistics.getUniqueDilutionValue(i);
            	if(logScaleX) {
            		z = Math.log10(statistics.getUniqueDilutionValue(i));
            	}            	
            	int x = 4*htext + (int) (grWidth*(z-minX)*scaleX + 0.5);
            	int y = pointRadius + grHeight;
            	            	
            	g.setColor(Color.BLACK);
            	g.drawLine(x,y,x,y+5);            	
            	
            	//string is drawn only if it does not overlap with previous one
            	//careful: this assumes that unique dilution values are sorted!
            	if(statistics.getUniqueDilutionValue(i) == (long) statistics.getUniqueDilutionValue(i))
                    str= String.format("%d",(long) statistics.getUniqueDilutionValue(i));
                else
                    str = String.format("%s",statistics.getUniqueDilutionValue(i));
            	int l = metrics.stringWidth(str)/2;
            	if((x-l)>posLabY) {
            		g.drawString(str,x-l,y+htext);
            		posLabY=x+l;
            	}
            }
            
          //draws ticks and tick labels on y scale            
          double dy = (maxY-minY)/5.0;
          if(dy<1.0) {
        	  if(logScaleY) {
        		  dy = Math.log10(2.0);
        	  } else {
        		  dy = 1.0;
        	  }
          } else {  
        	  if(dy>5.0) {
        		  dy = (int) (dy/5);
        		  dy *= 5.0;
        	  } else {
        		  dy = 5.0;
        	  }
          }

          double z = 0.0;          
          while(z<maxY) {
        	  if(z>=minY) {     
        		  int x = 4*htext;
        	  	  int y = pointRadius + (int) (grHeight*(1-(z-minY)*scaleY));
        	  	  if(logScaleY) {
        	  		  str = ""+Math.round(Math.pow(10.0,z));
        	  	  } else {
        	  		  str = ""+Math.round(z);  
        	  	  }        	    
        	  	  g.setColor(Color.BLACK);
        	  	  g.drawLine(x-5,y,x,y);        	  
        	  	  g.drawString(str,x-6-metrics.stringWidth(str),y+htext/3);
        	  }
        	  z+=dy;        	  
          }

            //draw points ***************************************************
            Color bgColor = Color.WHITE;
            if(CFUtype>0) {
            	bgColor = statistics.getCFUColor(CFUtype-1);
            }

        	double avgX = 0.0;
        	double avgY = 0.0;
        	int    nbAvg=0;

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
            			            			
            			avgX+=x;
            			avgY+=y;
            			nbAvg++;
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
            			//the point corresponding to the currently viewed well is surrounded by a circle
            			g.drawOval(ptX[i]-pointRadius,ptY[i]-pointRadius,2*pointRadius,2*pointRadius);
            		}            		
            		if(statistics.getIgnored(i)) {
            			//ignores points are crossed
            			g.drawLine(ptX[i]-pointRadius, ptY[i]-pointRadius, ptX[i]+pointRadius, ptY[i]+pointRadius);
            			g.drawLine(ptX[i]-pointRadius, ptY[i]+pointRadius, ptX[i]+pointRadius, ptY[i]-pointRadius);
            		}
            	}
            }
            //************************************************************************
            
            if(nbAvg>0) {
            	avgX/=nbAvg;
            	avgY/=nbAvg;
            }
            
            //on log-log graphs, a line of slope -1 going through average values is drawn
            if(logScaleX && logScaleY && nbAvg>0 && avgY>0) {
            	int x1,x2,y1,y2;
            	if(avgY - (minX-avgX)>maxY) {
            		x1 = 4*htext + (int) (grWidth*((avgY-maxY+avgX)-minX)*scaleX + 0.5);
            		y1 = pointRadius + (int) (grHeight*(1-(maxY-minY)*scaleY));            		
            	} else {
            		x1 = 4*htext;
            		y1 = pointRadius + (int) (grHeight*(1-((avgY - (minX-avgX))-minY)*scaleY));	
            	}        		
            	if(avgY - (maxX-avgX)<minY) {
            		x2 = 4*htext + (int) (grWidth*((avgY-minY+avgX)-minX)*scaleX + 0.5);
            		y2 = pointRadius + (int) grHeight;
            	} else {
            		x2 = 4*htext + (int) (grWidth*(maxX-minX)*scaleX + 0.5);        		
            		y2 = pointRadius + (int) (grHeight*(1-((avgY - (maxX-avgX))-minY)*scaleY));
            	}
            	
            	g.drawLine(x1,y1,x2,y2);
            }
            
        }
        
		
		@Override
		public String getToolTipText(MouseEvent event) {
			Point pt = event.getPoint();
			
			String txt = "<html>";
			for(int i=0;i<statistics.getNBcounts();i++) {
				double d = Math.sqrt(Math.pow(pt.getX()-ptX[i],2.0)+Math.pow(pt.getY()-ptY[i],2.0));
				//System.out.println("(" +pt.getX()+ "," + "," +pt.getY()+ ") <-> ("+ptX[i]+","+ptY[i]+") d="+d);				
				if(d < pointRadius/2.0) {
					txt += "<h2>plate "+statistics.getPlateName(i)+", ";
					txt += "well "+statistics.getWellName(i)+"</h2>";
					txt += "<p>(dilution: "+statistics.getDilution(i)+", ";
					txt += "volume: "+statistics.getVolume(i)+")</p><h3>";
					if(statistics.getCount(i,CFUtype)==-1) {
						txt += "count set to infinite";
					} else {
						txt += "count = "+statistics.getCount(i,CFUtype);
					}
					txt+="</h3>";
				}
			}

		    if(txt=="<html>") return null;
		    txt+="</html>";
		    return txt;
		}

    }
