package com.outdoorapps.hiketimecallite.asynctasks;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.view.Display;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.managers.MapManager;
import com.outdoorapps.hiketimecallite.model.track.Track;
import com.outdoorapps.hiketimecallite.model.track.TrackData;
import com.outdoorapps.hiketimecallite.support.LatLngSupport;

public class ShowOnMapTask extends AsyncTask<ArrayList<String>, Integer, LatLngBounds> {

	private Activity caller;
	private ProgressDialog dialog;
	private LatLngBounds latLngBounds;
	
	@Override
	protected void onPreExecute(){
		showProgressDialog(caller);
	}
	
	@Override
	protected LatLngBounds doInBackground(ArrayList<String>... param) {
		MapManager mapManager = MainActivity.main.getMapManager();
		TrackData trackData = MainActivity.main.getTrackData();
		ArrayList<String> checkedTrackNames = param[0];
		ArrayList<LatLng> latLngBoundList = new ArrayList<LatLng>();
		for(int i=0; i<checkedTrackNames.size(); i++) {
			String trackName = checkedTrackNames.get(i);
			Track track = trackData.getTrack(trackName);
			mapManager.addTrackOnMap(trackName);
			LatLngBounds bound = LatLngSupport.getLatLngBounds(track.getLatLngList());
			latLngBoundList.add(bound.northeast);
			latLngBoundList.add(bound.southwest);
		}
		// Center camera to the route(s)
		latLngBounds = LatLngSupport.getLatLngBounds(latLngBoundList);
		return latLngBounds;
	}
	
	private void showProgressDialog(Activity caller) {
		dialog = new ProgressDialog(caller);
		dialog.setTitle("Showing Items on Map");
		dialog.setMessage("Please wait...");
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		dialog.show();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPostExecute(LatLngBounds latLngBounds) {
		if(dialog!=null)
			dialog.dismiss();
		Display display = MainActivity.main.getWindowManager().getDefaultDisplay();
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds,display.getWidth(),display.getHeight(),100);
		MainActivity.main.getGoogleMap().animateCamera(cameraUpdate);
	}
	
	public void setCaller(Activity caller) {
		this.caller = caller;
	}
	
	public void showProgressDialogIfNeeded(Activity caller) {
		if(getStatus()==AsyncTask.Status.RUNNING)
			showProgressDialog(caller);
	}
	
	public void dismissProgressDialogIfNeeded() {
		if(getStatus()==AsyncTask.Status.RUNNING && dialog!=null) {
			dialog.dismiss();
			dialog = null;
		}
	}

	public LatLngBounds getLatLngBounds() {
		return latLngBounds;
	}
}
