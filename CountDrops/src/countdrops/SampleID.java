package countdrops;

import java.io.PrintWriter;
import java.util.ArrayList;


public class SampleID {
	private PlateSettings settings;
	
	private ArrayList<String> Fields = null;  //names of the field to be taken	
	private int n = -1;                       //number of fields 
	private int[] posFields = null;           //position of the field (e.g. in FieldsValue)
	private int[] where = null;               //should field be taken in well (0), plate (1) or table (>=2)
	
	public SampleID(ArrayList<String> f,PlateSettings s) {
		settings = s;
		update(f);
	}
	public SampleID(PlateSettings s) {
		settings = s;
		//default initialization of field list to "WELL": samples are defined by the WELL
		ArrayList<String> fields = new ArrayList<String>();
		fields.add("WELL");
		update(fields);
	}
	public void update(ArrayList<String> f) {
		Fields =f;
		
		n = Fields.size();
		posFields = new int[n];
		where = new int[n];
		ArrayList<String> tags = (ArrayList<String>) settings.getFieldsName().clone();
		tags.add("WELL");
		tags.add("ROW");
		tags.add("COLUMN");
		
		for(int i=0;i<n;i++) {
			if(tags.contains(Fields.get(i))) {
				//field is WELL
				if(Fields.get(i).equals("WELL")) {				
					where[i] = 0;
					posFields[i] = 0;
				}
				if(Fields.get(i).equals("ROW")) {				
					where[i] = 0;
					posFields[i] = 1;
				}
				if(Fields.get(i).equals("COLUMN")) {
					where[i] = 0;
					posFields[i] = 2;					
				}
				//field is one of the plate fields
				if(settings.getFieldsName().contains(Fields.get(i))) {
					where[i] = 1;
					posFields[i] = settings.getFieldsName().indexOf(Fields.get(i));
				}
				//field is to be taken in a plate?
			} else {
				//field has not been found (will be ignored)
				where[i] = -1;
				posFields[i] = -1;
			}
		}

	}
	public int getNbFields() {
		return n;
	}
	public ArrayList<String> getFields() {
		return Fields;
	}
	public String getField(int index) {
		return Fields.get(index);
	}
	
	public String getSampleID(Well w) {
		String id = "";
		for(int i=0;i<n;i++) {
			if(id!="") id+="_";
			switch(where[i]) {
			case 0:
				switch(posFields[i]) {
				case 0: //WELL
					id += w.getName();
					break;
				case 1: //ROW
					id += PlateSettings.getRowLetterFromInt(w.getRowInPlate());
					break;
				case 2: //COLUMN
					id += (w.getColInPlate()+1);
					break;
				}
				break;
			case 1:
				id += w.getSettings().getFieldsValue().get(posFields[i]);
				break;
			}
		}
		return id;
	}
	public void write(PrintWriter writer) {
		writer.print(Fields.get(0));
		for(int i=1;i<Fields.size();i++) writer.print(";"+Fields.get(i));
		writer.println();
	}
}
