package com.outdoorapps.hiketimecallite.asynctasks;

import java.io.File;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.files.RouteParser;
import com.outdoorapps.hiketimecallite.files.RouteParser.VersionNotSupportException;
import com.outdoorapps.hiketimecallite.files.TrackImporter;
import com.outdoorapps.hiketimecallite.model.route.Route;
import com.outdoorapps.hiketimecallite.model.track.Track;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;

public class ImportTask extends AsyncTask<String, Integer, Object> {

	private Object obj;
	private ProgressDialog dialog;
	private Activity caller;
	private MapObjectType type;
	private String resultObjectName;
	private boolean metricElevation;
	private boolean error, versionNotSupport;
	private String errorMessage;
	private ImportTaskResponseInterface delegate;

	@Override
	protected void onPreExecute(){
		showProgressDialog(caller);
		error = false;
		versionNotSupport = false;
	}

	@Override
	protected Object doInBackground(String... params) {
		switch(type) {
		case route:
			Route route = null;
			try {
				if(resultObjectName==null)
					throw new Exception("No Route Name");
				File routeFile = new File(params[0]);
				RouteParser parser = new RouteParser();
				route = parser.Parse(resultObjectName,routeFile,metricElevation);				
			} catch(VersionNotSupportException e) {
				error = true;
				versionNotSupport = true;
			} catch (Exception e) {
				error = true;
				errorMessage = e.getMessage();
			}
			obj = (Object) route;
			break;
		case track:
			Track track = null;
			try {
				if(resultObjectName==null)
					throw new Exception("No Track Name");
				TrackImporter importer = new TrackImporter();
				track = importer.Parse(new File(params[0]), resultObjectName, metricElevation);
			} catch (Exception e) {
				error = true;
				errorMessage = e.getMessage();
			}
			obj = (Object) track;
			break;
		default:
			obj = null;
			break;
		}		 			
		return obj;
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
	}

	@Override
	protected void onPostExecute(Object obj) {
		if(error==false) {
			switch(type) {
			case route:
				Route route = (Route) obj;
				MainActivity.main.getRouteData().addNewRoute(route, false);
				break;
			case track:
				Track track = (Track) obj;
				MainActivity.main.getTrackData().addTrack(track, false);
				break;
			default:
				break;
			}
		} else {
			if(versionNotSupport==false)
				Toast.makeText(MainActivity.main, "Error during import: " + errorMessage, Toast.LENGTH_SHORT).show();
		}
		if(dialog!=null)
			dialog.dismiss();
		delegate.versionSupportResponse(versionNotSupport);
	}

	private void showProgressDialog(Activity caller) {
		dialog = new ProgressDialog(caller);
		dialog.setTitle("Importing");
		dialog.setMessage("Please wait...");
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		dialog.show();
	}

	public void dismissProgressDialogIfNeeded() {
		if(getStatus()==AsyncTask.Status.RUNNING && dialog!=null) {
			dialog.dismiss();
			dialog = null;
		}
	}

	public void showProgressDialogIfNeeded(Activity caller) {
		if(getStatus()==AsyncTask.Status.RUNNING)
			showProgressDialog(caller);
	}

	public void setType(MapObjectType type) {
		this.type = type;
	}

	public void setCaller(Activity caller) {
		this.caller = caller;
	}

	public void setResultObjectName(String resultObjectName) {
		this.resultObjectName = resultObjectName;
	}

	public void setMetricElevation(boolean metricElevation) {
		this.metricElevation = metricElevation;
	}

	public boolean isVersionNotSupport() {
		return versionNotSupport;
	}

	public void setDelegate(ImportTaskResponseInterface delegate) {
		this.delegate = delegate;
	}

}
