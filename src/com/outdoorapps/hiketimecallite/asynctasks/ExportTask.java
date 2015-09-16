package com.outdoorapps.hiketimecallite.asynctasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.files.RouteExporter;
import com.outdoorapps.hiketimecallite.files.TrackExporter;
import com.outdoorapps.hiketimecallite.model.route.Route;
import com.outdoorapps.hiketimecallite.model.track.Track;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;

public class ExportTask extends AsyncTask<String, Integer, Object> {

	private Object obj;
	private ProgressDialog dialog;
	private Activity caller;
	private MapObjectType type;
	private String extension, errorMessage;
	private boolean isMetric, error;
	private ExportTaskResponseInterface delegate;

	@Override
	protected void onPreExecute(){
		showProgressDialog(caller);
	}

	@Override
	protected Object doInBackground(String... path) {
		switch(type) {
		case route:
			Route route = (Route) obj;
			RouteExporter routeExporter = new RouteExporter(MainActivity.main.getRouteData());
			try {
				if(extension.equals(".gpx"))
					routeExporter.exportToGPX(route.getName(), path[0], isMetric);
				if(extension.equals(".kml")) 
					routeExporter.exportToKML(route.getName(), path[0], isMetric);
			} catch (Exception e) {
				error = true;
				errorMessage = e.getMessage();
			}
			break;
		case track:
			Track track = (Track) obj;
			try {
				TrackExporter exporter = new TrackExporter();
				String name = track.getName();
				exporter.exportToGPX(name, path[0], Track.convertToTrackingPTList(track.getTPList()), true);
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
			String outputString;
			switch(type) {
			case route:
				outputString = "Route Exported";
				break;
			case track:
				outputString = "Track Exported";
				break;
			default:
				outputString = "Invalid Export Operation";
				break;
			}
			Toast.makeText(MainActivity.main, outputString, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(MainActivity.main, "Error during export: " + errorMessage, Toast.LENGTH_SHORT).show();
		}
		if(dialog!=null)
			dialog.dismiss();
		if(delegate!=null)
			delegate.postExportResponse(error, errorMessage);
	}
	
	private void showProgressDialog(Activity caller) {
		dialog = new ProgressDialog(caller);
		dialog.setTitle("Exporting");
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

	public void setMapObject(Object obj) {
		this.obj = obj;
	}

	public void setType(MapObjectType type) {
		this.type = type;
	}

	public void setCaller(Activity caller) {
		this.caller = caller;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public void setMetric(boolean isMetric) {
		this.isMetric = isMetric;
	}

	public void setDelegate(ExportTaskResponseInterface delegate) {
		this.delegate = delegate;
	}
}
