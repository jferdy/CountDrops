package countdrops;

public interface ImageWellListener {
	public void SelectionHasChanged(); //selected CFU have changed by the ImageWell
	public void CFUedited();           //CFU data (e.g. CFU type) have been changed
	public void CFUadded();            //CFUs have been added
	public void CFUremoved();          //CFUs have been deleted
	
}
