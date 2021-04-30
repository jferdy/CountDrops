package countdrops;

public interface ViewWellListener {
	public void newViewWellAsked(ViewWellEvent evt);  //ask a new ViewWell instance
	public void viewWellChange(ViewWellEvent evt);    //signal that data have changed
	public void viewWellCopyState(ViewWellEvent evt); //ask to copy state from well to others of the same plate
	public void viewWellHasClosed(ViewWellEvent evt); //ViewWell instance has been closed
	public void autoDetectRow(ViewWellEvent evt);    //ViewWell asks autoDetect on row
	public void autoDetectColumn(ViewWellEvent evt); //ViewWell asks autoDetect on column
	public void autoDetectPlate(ViewWellEvent evt);  //ViewWell asks autoDetect on plate
}
