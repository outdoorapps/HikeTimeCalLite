package com.outdoorapps.hiketimecallite.managers;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;

import com.outdoorapps.hiketimecallite.GeneralExportActivity;
import com.outdoorapps.hiketimecallite.asynctasks.ExportTask;
import com.outdoorapps.hiketimecallite.dialogs.ExportDialog;
import com.outdoorapps.hiketimecallite.support.ImportExport.ImportExportType;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;
import com.outdoorapps.hiketimecallite.support.Storage;
import com.outdoorapps.hiketimecallite.support.constants.ExtraTypes;
import com.outdoorapps.hiketimecallite.support.constants.RequestCode;

/**
 * onActivityResult must be set up in caller for this to work properly
 * @author chbting
 *
 */
public class ExportManager implements 
ExportDialog.ExportDialogListener {

	private String exportPath, preferredName, exportFileExtension;
	private MapObjectType type;
	private FragmentActivity caller; /** For Progress dialog's display */
	private Object exportObj;
	private ExportTask exportTask;
	
	public static final String EXPORT_ROUTE_DIALOG = "export_route_dialog";
	public static final String EXPORT_ROUTE_TAG = "export_route";
	public static final String EXPORT_TRACK_TAG = "export_track";
	public static final String TRACK_EXPORT_FILE_TYPES = ".gpx";

	public ExportManager(MapObjectType type) {
		this.type = type;
	}

	public void showExportRouteDialog(FragmentActivity caller) {
		this.caller = caller;
		ExportDialog exportDialog = new ExportDialog();
		exportDialog.setListener(this);
		exportDialog.show(caller.getSupportFragmentManager(), EXPORT_ROUTE_DIALOG);
	}

	public void startExportOperation(String preferredName, Object exportObj, FragmentActivity caller) {		
		this.caller = caller;
		this.preferredName = preferredName;
		this.exportObj = exportObj;
		switch(type) {				
		case route: // Route
			showExportRouteDialog(caller);
			break;
		case track: // Track
			Intent intent = new Intent(caller,GeneralExportActivity.class);
			intent.putExtra(ExtraTypes.FILE_EXTENSION, TRACK_EXPORT_FILE_TYPES);
			intent.putExtra(ExtraTypes.EXPORT_TYPE, ImportExportType.track.toString());
			intent.putExtra(ExtraTypes.PREFERRED_FILE_NAME, preferredName);
			intent.putExtra(ExtraTypes.PREFERRED_START_FOLDER, Storage.TRACKS_FOLDER);
			caller.startActivityForResult(intent, RequestCode.EXPORT_GENERAL);
			break;
		default:
			break;
		}		
	}

	public void export(String filePath, FragmentActivity caller) {
		this.caller = caller;
		exportTask = new ExportTask();
		exportTask.setCaller(caller);
		exportTask.setType(type);
		exportTask.setMapObject(exportObj);		
		switch(type) {
		case route:
			exportTask.setExtension(exportFileExtension);
			break;
		case track:
			break;
		default:
			break;
		}
		exportTask.execute(filePath);
		exportObj = null;
	}
	
	public void dismissProgressDialogIfNeeded() {
		if(exportTask!=null)
			exportTask.dismissProgressDialogIfNeeded();
	}
	
	public void showProgressDialogIfNeeded(Activity caller) {
		if(exportTask!=null)
			exportTask.showProgressDialogIfNeeded(caller);
	}

	/**
	 * Called during onActivityResult of caller
	 * @param exportPath
	 */
	public void setExportPath(String exportPath) {
		this.exportPath = exportPath;
	}

	public String getExportPath() {
		return exportPath;
	}

	public void setType(MapObjectType type) {
		this.type = type;
	}
	
	public MapObjectType getType() {
		return type;
	}

	@Override
	public void onDialogPositiveClick(ExportDialog dialog) {
		if(dialog.getTag().equals(EXPORT_ROUTE_DIALOG)) {
			exportFileExtension = dialog.getExtension();			
			Intent intent = new Intent(caller,GeneralExportActivity.class);
			intent.putExtra(ExtraTypes.FILE_EXTENSION, exportFileExtension);
			intent.putExtra(ExtraTypes.EXPORT_TYPE, ImportExportType.route.toString());
			intent.putExtra(ExtraTypes.PREFERRED_FILE_NAME, preferredName);
			intent.putExtra(ExtraTypes.PREFERRED_START_FOLDER, Storage.ROUTE_FOLDER);
			caller.startActivityForResult(intent, RequestCode.EXPORT_GENERAL);
		}		
	}

}
