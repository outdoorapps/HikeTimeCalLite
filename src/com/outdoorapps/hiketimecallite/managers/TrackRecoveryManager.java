package com.outdoorapps.hiketimecallite.managers;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.asynctasks.ExportTask;
import com.outdoorapps.hiketimecallite.asynctasks.ExportTaskResponseInterface;
import com.outdoorapps.hiketimecallite.model.track.Track;
import com.outdoorapps.hiketimecallite.model.track.Tracker;
import com.outdoorapps.hiketimecallite.model.track.TrackingPoint;
import com.outdoorapps.hiketimecallite.support.FileSupport;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;
import com.outdoorapps.hiketimecallite.support.Storage;

public class TrackRecoveryManager implements
ExportTaskResponseInterface {

	private ExportTask recoveryTask;
	private String fileName;

	public void recoverTrack() {
		ArrayList<TrackingPoint> trackingPTList = Tracker.loadFromDatabase();
		String trackName = trackingPTList.get(0).getTime();
		Track track = new Track(trackName,Track.convertToTPList(Tracker.loadFromDatabase()));
		fileName = checkTrackName(trackName + ".gpx");
		recoveryTask = new ExportTask();
		recoveryTask.setDelegate(this);
		recoveryTask.setCaller(MainActivity.main);
		recoveryTask.setType(MapObjectType.track);
		recoveryTask.setMapObject(track);
		recoveryTask.execute(Storage.TRACK_RECOVERY_FOLDER+File.separator+fileName);
	}

	public void dismissProgressDialogIfNeeded() {
		if(recoveryTask!=null)
			recoveryTask.dismissProgressDialogIfNeeded();
	}

	public void showProgressDialogIfNeeded(Activity caller) {
		if(recoveryTask!=null)
			recoveryTask.showProgressDialogIfNeeded(caller);
	}

	private String checkTrackName(String fileName) {
		// Check usage
		ArrayList<String> allFileNames = new ArrayList<String>();
		File recoveryFolder = new File(Storage.TRACK_RECOVERY_FOLDER);
		File[] fileList = recoveryFolder.listFiles();
		for(File file: fileList) {
			if(!(file.isDirectory()))
				allFileNames.add(file.getName());
		}

		String testName = fileName;
		String extension = FileSupport.getExtension(fileName);
		boolean nameUsed = true;
		int version = 1;
		while(nameUsed==true) {
			int index = 0;
			nameUsed = false;
			while(index<allFileNames.size() && nameUsed==false) {
				String checkName = allFileNames.get(index);
				if(checkName.equals(testName)) {
					nameUsed = true;
					testName = FileSupport.removeExtension(fileName) + "_" + version + extension; // concat "_x" if the name is used
					version++;
				}
				else
					index++;
			}
		}
		return testName;
	}

	@Override
	public void postExportResponse(boolean error, String errorMessage) {
		if(error==false) {
			// Clear database
			Tracker.clearDatabase();

			// Notify user
			new AlertDialog.Builder(MainActivity.main)
			.setTitle(R.string.track_recovered)
			.setMessage(MainActivity.main.getString(R.string.track_recovered_message) 
					+ ": [" +Storage.TRACK_RECOVERY_FOLDER + File.separator + fileName + "]")			
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					// Do nothing
				}
			}).show();
		} else {
			//TODO
			// Notify user
			new AlertDialog.Builder(MainActivity.main)
			.setTitle(R.string.error)
			.setMessage(MainActivity.main.getString(R.string.track_recovery_error_message)+": "+errorMessage)			
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					// Do nothing
				}
			}).show();
		}
	}
}
