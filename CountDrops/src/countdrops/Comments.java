package countdrops;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class Comments extends CountDrops {
	private String    path = null;
	private File  	  inputFile = null;
	private int       nbComments = 0;
	
	private Document  doc = null;
	private NodeList  commentList = null;
	
	public Comments(String p) {
		path = p;
		
		try {
			//opens comment file
			inputFile = new File(path+"/comments.txt");
			if(!inputFile.exists()) {
				inputFile=null;
				return;
			}
		} catch(Exception e) {			
		}
								
		try {				
			//parse comment file
			//global tag is <comments> </comments>
			//each comment is delimited by <comment author="x"> </comment>
	        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	        doc = dBuilder.parse(inputFile);
	        doc.getDocumentElement().normalize();
	        updateCommentList();	        
		} catch (Exception e) {
	         e.printStackTrace();
	    }		
	}

	public void updateCommentList() {
		//extract comments
		commentList = doc.getElementsByTagName("comment");
		nbComments = commentList.getLength();
	}

	public int getNbComments() {return nbComments;}
	
	public Element getComment(int i) {
		if(commentList!=null && i>=0 && i<nbComments) {
			Node nNode = commentList.item(i);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				return( (Element) nNode );
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
		
	public String getAuthor(Element e) {
		if(e!=null) return(e.getAttribute("author"));
		return null;
	}

	public String getAuthor(int i) {
		Element e = getComment(i);
		return getAuthor(e);
	}
	
	public String getTimeStamp(Element e) {
		if(e!=null) return(e.getAttribute("timestamp"));
		return null;
	}
	
	public String getTimeStamp(int i) {
		Element e = getComment(i);
		return getTimeStamp(e);
	}
	
	public String getText(Element e) {
		if(e!=null) return(e.getTextContent());
		return null;
	}
	
	public String getText(int i) {
		Element e = getComment(i);
		return(getText(e));
	}
	
	public void write() {
		// https://www.tutorialspoint.com/java_xml/java_dom_create_document.htm

		if(doc==null) return;
		if(getNbComments()<=0) {
			//no comments: comments.txt is deleted
			try {
				Path p = FileSystems.getDefault().getPath(path+"/comments.txt");
			    Files.deleteIfExists(p);
			} catch (Exception e) {
				e.printStackTrace();
			} 
		
		} else {
			//comments saved in comments.txt
				try{
					// write the content into xml file
			         TransformerFactory transformerFactory = TransformerFactory.newInstance();
			         Transformer transformer = transformerFactory.newTransformer();
			         DOMSource source = new DOMSource(doc);
			         StreamResult result = new StreamResult(new File(path+"/comments.txt"));
			         transformer.transform(source, result);
			         
			      } catch (Exception e) {
			          e.printStackTrace();
			      }
		}
	}
	
	public void editComment(int pos,String author,String text) {
		Element e = getComment(pos);
		if(e==null) return;
		e.setAttribute("author",author);
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		e.setAttribute("timestamp",timeStamp);
		e.setTextContent(text);		
		write();
	}

	public void addComment(String author,String text) {
		Element rootElement = null;
		try {
			if(doc==null) {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				doc = dBuilder.newDocument();
				
				//Create root element
		        rootElement = doc.createElement("comments");
		        doc.appendChild(rootElement);
			} else {
				rootElement = (Element) doc.getElementsByTagName("comments").item(0);
			}
		
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
			
			Element n = doc.createElement("comment");			
			n.setAttribute("author", author);
			n.setAttribute("timestamp", timeStamp);
			n.setTextContent(text);
			rootElement.appendChild(n);
			updateCommentList();			
			write();			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void removeComment(int pos) {
		Element e = getComment(pos);
		if(e==null) return;
		try {			
			e.getParentNode().removeChild(e);
			updateCommentList();
			write();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
}

