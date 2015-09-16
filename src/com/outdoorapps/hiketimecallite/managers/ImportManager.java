package com.outdoorapps.hiketimecallite.managers;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;

import com.outdoorapps.hiketimecallite.GeneralImportActivity;
import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.asynctasks.ImportTask;
import com.outdoorapps.hiketimecallite.asynctasks.ImportTaskResponseInterface;
import com.outdoorapps.hiketimecallite.dialogs.GoProDialog;
import com.outdoorapps.hiketimecallite.dialogs.InstantCheckDialog;
import com.outdoorapps.hiketimecallite.support.FileSupport;
import com.outdoorapps.hiketimecallite.support.ImportExport.ImportExportType;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;
import com.outdoorapps.hiketimecallite.support.Storage;
import com.outdoorapps.hiketimecallite.support.constants.ExtraTypes;
import com.outdoorapps.hiketimecallite.support.constants.RequestCode;

/**
 * onActivityResult and onResume must be set up in caller for this to work properly
 * @author chbting
 *
 */
public class ImportManager implements
InstantCheckDialog.InstantCheckDialogListener,
ImportTaskResponseInterface {

	private String importPath;
	private MapObjectType type;
	private boolean hasJob; /** hasJob is true when importPath is not null, false otherwise */
	private FragmentActivity caller; /** For displaying ProgressDialog */
	private ImportTask importTask;

	public static final String IMPORT_ROUTE_TAG = "import_route";
	public static final String IMPORT_TRACK_TAG = "import_track";
	public static final String ROUTE_IMPORT_FILE_TYPES = ".gpx,.kml";
	public static final String TRACK_IMPORT_FILE_TYPES = ".gpx,.kml";
	private final CharSequence[] items = {"Metric","Imperial"};
	private final CharSequence[] importTypes = {"Route","Track"};

	public ImportManager(MapObjectType type) {
		this.type = type;
		hasJob = false;	
	}

	/**
	 * Called during onResume of caller if hasJob
	 */
	public void showNameDialog(FragmentActivity caller) {
		this.caller = caller;
		InstantCheckDialog instantCheckDialog = new InstantCheckDialog();
		instantCheckDialog.setCancelable(false);
		instantCheckDialog.setListener(this);
		File f = new File(importPath);
		String initialName = FileSupport.removeExtension(f.getName()); 
		switch(type){
		case route:
			instantCheckDialog.show(caller.getSupportFragmentManager(), IMPORT_ROUTE_TAG);
			instantCheckDialog.setTitle(caller.getString(R.string.choose_route_name));			
			instantCheckDialog.setInitialName(initialName);
			instantCheckDialog.setCompareList(MainActivity.main.getRouteData().getAllRouteName(),false);
			break;
		case track:
			instantCheckDialog.show(caller.getSupportFragmentManager(), IMPORT_TRACK_TAG);
			instantCheckDialog.setTitle(caller.getString(R.string.choose_track_name));
			instantCheckDialog.setInitialName(initialName);
			instantCheckDialog.setCompareList(MainActivity.main.getTrackData().getAllTrackName(),false);
			break;
		default:
			break;
		}		
	}

	public void startImportOperation(FragmentActivity caller) {
		this.caller = caller;
		Intent intent;
		String preferred_start_folder, importFileTypes;
		ImportExportType importType;
		switch(type) {				
		case route: // Route
			preferred_start_folder = Storage.ROUTE_FOLDER;
			importFileTypes = ROUTE_IMPORT_FILE_TYPES;
			importType = ImportExportType.route;
			break;
		case track: // Track
			preferred_start_folder = Storage.TRACKS_FOLDER;
			importFileTypes = TRACK_IMPORT_FILE_TYPES;
			importType = ImportExportType.track;
			break;
		default:
			preferred_start_folder = "";
			importFileTypes = "";
			importType = ImportExportType.general;
			break;
		}
		intent = new Intent(caller,GeneralImportActivity.class);
		intent.putExtra(ExtraTypes.IMPORT_TYPE,importType.toString());
		intent.putExtra(ExtraTypes.FILE_EXTENSION, importFileTypes);
		intent.putExtra(ExtraTypes.PREFERRED_START_FOLDER, preferred_start_folder);
		caller.startActivityForResult(intent, RequestCode.IMPORT_GENERAL);
	}

	public void openDualOptionsDialog(final FragmentActivity caller) {
		this.caller = caller;
		new AlertDialog.Builder(caller)
		.setCancelable(false) // Make sure tempFilePath reset to null
		.setTitle(R.string.choose_import_type)
		.setSingleChoiceItems(importTypes, 0, null)
		.setPositiveButton(R.string.ok,new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which){
				int position = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
				Intent intent;
				String preferred_start_folder, importFileTypes;
				ImportExportType importType;
				switch(position) {				
				case 0: // Route
					type = MapObjectType.route;
					preferred_start_folder = Storage.ROUTE_FOLDER;
					importFileTypes = ROUTE_IMPORT_FILE_TYPES;
					importType = ImportExportType.route;
					break;
				case 1: // Track
					type = MapObjectType.track;
					preferred_start_folder = Storage.TRACKS_FOLDER;
					importFileTypes = TRACK_IMPORT_FILE_TYPES;
					importType = ImportExportType.track;
					break;
				default:
					preferred_start_folder = "";
					importFileTypes = "";
					importType = ImportExportType.general;
					break;
				}
				intent = new Intent(caller,GeneralImportActivity.class);
				intent.putExtra(ExtraTypes.IMPORT_TYPE,importType.toString());
				intent.putExtra(ExtraTypes.FILE_EXTENSION, importFileTypes);
				intent.putExtra(ExtraTypes.PREFERRED_START_FOLDER, preferred_start_folder);
				caller.startActivityForResult(intent, RequestCode.IMPORT_GENERAL);
			}
		})
		.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which){
			}
		}).show();
	}

	@Override
	public void onDialogPositiveClick(InstantCheckDialog dialog) {
		importTask = new ImportTask();
		importTask.setCaller(caller);
		importTask.setResultObjectName(dialog.getName());
		importTask.setDelegate(this);
		if(dialog.getTag().equals(IMPORT_ROUTE_TAG)) {			
			importTask.setType(MapObjectType.route);
		}
		if(dialog.getTag().equals(IMPORT_TRACK_TAG)) {
			importTask.setType(MapObjectType.track);
		}
		// Ask user to choose elevation unit
		new AlertDialog.Builder(caller)
		.setCancelable(false) // Make sure tempFilePath reset to null
		.setTitle(R.string.choose_elevation_unit)
		.setSingleChoiceItems(items, 0, null)
		.setPositiveButton(R.string.ok,new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which){
				int position = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
				switch(position) {
				case 0: // Metric
					importTask.setMetricElevation(true);
					importTask.execute(importPath);
					break;
				case 1: // Imperial
					importTask.setMetricElevation(false);
					importTask.execute(importPath);
					break;
				default:
					break;
				}
				importPath = null;
				hasJob = false;
			}
		})
		.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which){
				importPath = null;
				hasJob = false;
			}
		}).show();
	}

	@Override
	public void onDialogNegativeClick(InstantCheckDialog dialog) {
		if(dialog.getTag().equals(IMPORT_ROUTE_TAG) || 
				dialog.getTag().equals(IMPORT_TRACK_TAG)) {			
			importPath = null;
			hasJob = false;
		}
	}

	/**
	 * Called during onActivityResult of caller
	 * @param importPath
	 */
	public void setImportPath(String importPath) {
		this.importPath = importPath;
		hasJob = true;
	}

	public void dismissProgressDialogIfNeeded() {
		if(importTask!=null)
			importTask.dismissProgressDialogIfNeeded();
	}

	public void showProgressDialogIfNeeded(Activity caller) {
		if(importTask!=null)
			importTask.showProgressDialogIfNeeded(caller);
	}

	public String getImportPath() {
		return importPath;
	}

	public MapObjectType getType() {
		return type;
	}

	public boolean hasJob() {
		return hasJob;
	}
	
	@Override
	public void versionSupportResponse(boolean versionNotSupport) {
		if(versionNotSupport==true) {
			GoProDialog goProDialog_export = new GoProDialog();
			goProDialog_export.setFunctionName("Import routes with more than 30 points");
			goProDialog_export.show(caller.getSupportFragmentManager(), "go_pro_rpLimit");
		}
	}
}
