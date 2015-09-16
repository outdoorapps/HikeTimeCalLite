package com.outdoorapps.hiketimecallite.model.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.SelectTrackActivity;
import com.outdoorapps.hiketimecallite.asynctasks.GetElevationResponseInterface;
import com.outdoorapps.hiketimecallite.asynctasks.GetElevationTask;
import com.outdoorapps.hiketimecallite.database.TrackDatabaseOpenHelper;
import com.outdoorapps.hiketimecallite.managers.MarkerInfo;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;

/**
 * Keep track of all tracks
 * Note: Use main.getMapManager().addTrackOnMap(track.getName()); to show on map
 * @author chbting
 *
 */
public class TrackData implements GetElevationResponseInterface {

	private MainActivity main;
	private ArrayList<Track> trackList;
	private ArrayList<String> tracksOnMapList, checkedTrackNames; 
	private HashMap<Marker,MarkerInfo> markerMap;
	private HashMap<GetElevationTask,Track> editTaskMap;
	private HashMap<GetElevationTask,Boolean> updateElevationModeMap;
	private HashMap<GetElevationTask,Boolean> showMap;
	private HashMap<GetElevationTask,double[]> updateElevationBoundaryMap;
	private TrackDatabaseOpenHelper trackDBHelper;

	public TrackData(MainActivity main) {
		this.main = main;
		trackDBHelper = new TrackDatabaseOpenHelper(main);
		recoverTrackHeaderList();
		recoverTrackOnMapList();
		checkedTrackNames = new ArrayList<String>();
		markerMap = new HashMap<Marker,MarkerInfo>();
		editTaskMap = new HashMap<GetElevationTask,Track>();
		updateElevationModeMap = new HashMap<GetElevationTask,Boolean>();
		showMap = new HashMap<GetElevationTask,Boolean>();
		updateElevationBoundaryMap = new HashMap<GetElevationTask,double[]>();
	}

	private void recoverTrackHeaderList() {				
		trackList = new ArrayList<Track>();
		SQLiteDatabase db = trackDBHelper.getReadableDatabase();
		String sql = "SELECT * FROM " + TrackDatabaseOpenHelper.TRACK_HEADER_TABLE_NAME + ";";
		Cursor cursor = db.rawQuery(sql, null);

		if (cursor.moveToFirst()) {
			do {
				// Get track header
				String trackName = cursor.getString(0);
				double distance = Double.parseDouble(cursor.getString(1));
				long duration = Long.parseLong(cursor.getString(2));
				double elevationGain = Double.parseDouble(cursor.getString(3));
				String date = cursor.getString(4);
				trackList.add(new Track(trackName,distance,duration,elevationGain,date));
			} while (cursor.moveToNext());
		}
		db.close();
	}

	public ArrayList<TrackPoint> recoverTPList(String trackName) {
		SQLiteDatabase db = trackDBHelper.getReadableDatabase();

		// Get TrackPoint list
		ArrayList<TrackPoint> TPList = new ArrayList<TrackPoint>();
		Cursor cursor = db.query(TrackDatabaseOpenHelper.TRACKPOINT_TABLE_NAME, 
				null,"NAME=?", new String[]{trackName}, 
				null, null,	null);

		if (cursor.moveToFirst()) {
			do {
				Location location = new Location("");
				double lat = Double.parseDouble(cursor.getString(1));
				double lon = Double.parseDouble(cursor.getString(2));
				double altitude = Double.parseDouble(cursor.getString(3));
				String time = cursor.getString(4);
				float bearing = Float.parseFloat(cursor.getString(5));
				float speed = Float.parseFloat(cursor.getString(6));
				float accuracy = Float.parseFloat(cursor.getString(7));

				location.setLatitude(lat);
				location.setLongitude(lon);
				location.setAltitude(altitude);
				location.setBearing(bearing);
				location.setSpeed(speed);
				location.setAccuracy(accuracy);

				long culminatedTime = Long.parseLong(cursor.getString(8));
				double culminatedDistance = Double.parseDouble(cursor.getString(9));
				TrackingPoint trackingPT = new TrackingPoint(location,time);
				TPList.add(new TrackPoint(trackingPT,culminatedTime,culminatedDistance));
			} while (cursor.moveToNext());

			db.close();
		}
		return TPList;
	}

	private void recoverTrackOnMapList() {
		tracksOnMapList = new ArrayList<String>();
		SQLiteDatabase db = trackDBHelper.getWritableDatabase();
		String sql = "SELECT * FROM " + TrackDatabaseOpenHelper.TRACK_ON_MAP_TABLE_NAME + ";";
		Cursor cursor = db.rawQuery(sql, null);

		if (cursor.moveToFirst()) {
			do {
				String trackName = cursor.getString(0);
				tracksOnMapList.add(trackName);
			} while (cursor.moveToNext());
		}
		db.close();
	}

	public void addTrack(Track track, boolean showOnMap) {
		trackList.add(track);
		saveTrackToDatabase(track); // update database immediately
		if(showOnMap==true)
			main.getMapManager().addTrackOnMap(track.getName());

		// update select track list if it is open		
		if(SelectTrackActivity.thisActivity!=null)
			SelectTrackActivity.notifyDataSetChanged();
		Toast.makeText(main, "Track added", Toast.LENGTH_SHORT).show();
	}

	/**
	 * Add recorded/imported track to track list
	 * @param elevationList
	 * @param latLngList
	 */
	public void addTrack(String trackName, ArrayList<TrackingPoint> trackingPTList, boolean showOnMap) {
		ArrayList<TrackPoint> TPList = Track.convertToTPList(trackingPTList);
		Track track = new Track(trackName,TPList);
		trackList.add(track);
		saveTrackToDatabase(track); // update database immediately
		if(showOnMap==true)
			main.getMapManager().addTrackOnMap(trackName);

		// update select track list if it is open		
		if(SelectTrackActivity.thisActivity!=null)
			SelectTrackActivity.notifyDataSetChanged();
		Toast.makeText(main, "Track saved", Toast.LENGTH_SHORT).show();
	}

	public void saveTrackToDatabase(Track track) {
		SQLiteDatabase db = trackDBHelper.getWritableDatabase();
		String trackName = track.getName();

		db.delete(TrackDatabaseOpenHelper.TRACK_HEADER_TABLE_NAME, "NAME=?", new String[]{trackName});
		db.delete(TrackDatabaseOpenHelper.TRACKPOINT_TABLE_NAME, "NAME=?", new String[]{trackName});

		ContentValues values = new ContentValues(); 
		values.put("NAME", trackName);
		values.put("DISTANCE", track.getDistance()+"");
		values.put("DURATION", track.getDuration()+"");
		values.put("ELEVATIONGAIN", track.getElevationGain()+"");
		values.put("DATE", track.getDate());
		db.insert(TrackDatabaseOpenHelper.TRACK_HEADER_TABLE_NAME, null, values);

		db.beginTransaction();
		try {
			ArrayList<TrackPoint> TPList = track.getTPList();	
			for(int i=0;i<TPList.size();i++) {
				TrackPoint tp = TPList.get(i);
				Location location = tp.getLocation();
				ContentValues TPValues = new ContentValues(); 
				TPValues.put("NAME", trackName);
				TPValues.put("LAT", location.getLatitude());
				TPValues.put("LON", location.getLongitude());
				TPValues.put("ALTITUDE", location.getAltitude());
				TPValues.put("TIME", tp.getTime());
				TPValues.put("BEARING", location.getBearing());
				TPValues.put("SPEED", location.getSpeed());
				TPValues.put("ACCURACY", location.getAccuracy());
				TPValues.put("CULTIME", tp.getCulminatedTime());
				TPValues.put("CULDISTANCE", tp.getCulminatedDistance());
				db.insert(TrackDatabaseOpenHelper.TRACKPOINT_TABLE_NAME, null, TPValues);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();
	}

	/**
	 * Add a track of the given name on map
	 * Actual actions: check if the track of the given name is currently marked
	 * as on map, if so, marked it (put its name in tracksOnMapList), update
	 * tracksOnMapList in database and return true to notify further actions
	 * are needed. Otherwise do nothing and return false.
	 * @param trackName
	 */
	public boolean addTrackOnMap(String trackName) {
		Track track = getTrack(trackName);
		if(track!=null) {
			// Make sure the track name is only on the list once
			if(!tracksOnMapList.contains(trackName)) {
				tracksOnMapList.add(trackName);
				// update database
				SQLiteDatabase db = trackDBHelper.getWritableDatabase();
				ContentValues values = new ContentValues(); 
				values.put("NAME", trackName);
				db.insert(TrackDatabaseOpenHelper.TRACK_ON_MAP_TABLE_NAME, null, values);
				db.close();
				return true;
			}
			else
				return false;
		} else
			return false;
	}

	/**
	 * Remove the track of the given track name and hide it from map as needed,
	 * return true if the track is removed, false otherwise
	 * @param trackName
	 * @return
	 */
	public boolean removeTrack(String trackName) {
		if(checkedTrackNames.contains(trackName))
			checkedTrackNames.remove(trackName);
		Track track = getTrack(trackName);
		boolean result = trackList.remove(track);		
		if(result==true) {
			main.getMapManager().hideTrackFromMap(trackName);
			hideTrackFromMap(trackName);
			SQLiteDatabase db =  trackDBHelper.getWritableDatabase();
			db.delete(TrackDatabaseOpenHelper.TRACK_HEADER_TABLE_NAME, "NAME=?", new String[]{trackName});
			db.delete(TrackDatabaseOpenHelper.TRACKPOINT_TABLE_NAME, "NAME=?", new String[]{trackName});
			db.close();
		}
		return result;
	}

	/**
	 * Hide a track from map
	 * Actual actions: check if a track is currently marked as on map (contained
	 * in tracksOnMapList), if so, remove it and update database and return true
	 * to notify caller to update its map
	 * @param trackName
	 * @return
	 */
	public boolean hideTrackFromMap(String trackName) {
		boolean result = tracksOnMapList.remove(trackName);
		if(result==true) { // remove from database
			SQLiteDatabase db =  trackDBHelper.getWritableDatabase();
			db.delete(TrackDatabaseOpenHelper.TRACK_ON_MAP_TABLE_NAME, "NAME=?", new String[]{trackName});
			db.close();
		}
		return result;
	}

	/**
	 * Modify data (TPList) of the track of the given name,
	 * update trackList and database
	 * @param trackName
	 * @param TPList
	 */
	public void modifyTrack(String trackName, ArrayList<TrackPoint> TPList) {
		Track track = getTrack(trackName);
		track.setTPList(TPList);

		// Modify track data and parameters in database
		SQLiteDatabase db =  trackDBHelper.getWritableDatabase();

		ContentValues values = new ContentValues(); 
		values.put("NAME", trackName);
		values.put("DISTANCE", track.getDistance()+"");
		values.put("DURATION", track.getDuration()+"");
		values.put("ELEVATIONGAIN", track.getElevationGain()+"");
		values.put("DATE", track.getDate());
		db.update(TrackDatabaseOpenHelper.TRACK_HEADER_TABLE_NAME, values, "NAME=?", new String[]{trackName});

		// Re-create RPList in database
		db.delete(TrackDatabaseOpenHelper.TRACKPOINT_TABLE_NAME, "NAME=?", new String[]{trackName});

		db.beginTransaction();
		try {
			for(int i=0;i<TPList.size();i++) {
				TrackPoint tp = TPList.get(i);
				Location location = tp.getLocation();
				ContentValues TPValues = new ContentValues(); 
				TPValues.put("NAME", trackName);
				TPValues.put("LAT", location.getLatitude());
				TPValues.put("LON", location.getLongitude());
				TPValues.put("ALTITUDE", location.getAltitude());
				TPValues.put("TIME", tp.getTime());
				TPValues.put("BEARING", location.getBearing());
				TPValues.put("SPEED", location.getSpeed());
				TPValues.put("ACCURACY", location.getAccuracy());
				TPValues.put("CULTIME", tp.getCulminatedTime());
				TPValues.put("CULDISTANCE", tp.getCulminatedDistance());
				db.insert(TrackDatabaseOpenHelper.TRACKPOINT_TABLE_NAME, null, TPValues);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();
		main.updateMarkerInfo();
	}

	/**
	 * Change the track of the given name (oldRouteName) to newRouteName
	 * @param oldTrackName
	 * @param newTrackName
	 */
	public void changeTrackName(String oldTrackName, String newTrackName) {
		// Remove from checked track names list
		if(checkedTrackNames.contains(oldTrackName)) {
			checkedTrackNames.remove(oldTrackName);
			checkedTrackNames.add(newTrackName);
		}

		Track track = getTrack(oldTrackName);
		track.setName(newTrackName); // 1. change trackList
		ArrayList<TrackPoint> TPList = track.getTPList();

		// 2. change trackList in database
		SQLiteDatabase db =  trackDBHelper.getWritableDatabase();

		ContentValues values = new ContentValues(); 
		values.put("NAME", newTrackName);
		values.put("DISTANCE", track.getDistance()+"");
		values.put("DURATION", track.getDuration()+"");
		values.put("ELEVATIONGAIN", track.getElevationGain()+"");
		values.put("DATE", track.getDate());
		db.update(TrackDatabaseOpenHelper.TRACK_HEADER_TABLE_NAME, values, "NAME=?", new String[]{oldTrackName});

		// Re-create RPList in database
		db.delete(TrackDatabaseOpenHelper.TRACKPOINT_TABLE_NAME, "NAME=?", new String[]{oldTrackName});

		db.beginTransaction();
		try {
			for(int i=0;i<TPList.size();i++) {
				TrackPoint tp = TPList.get(i);
				Location location = tp.getLocation();
				ContentValues TPValues = new ContentValues(); 
				TPValues.put("NAME", newTrackName);
				TPValues.put("LAT", location.getLatitude());
				TPValues.put("LON", location.getLongitude());
				TPValues.put("ALTITUDE", location.getAltitude());
				TPValues.put("TIME", tp.getTime());
				TPValues.put("BEARING", location.getBearing());
				TPValues.put("SPEED", location.getSpeed());
				TPValues.put("ACCURACY", location.getAccuracy());
				TPValues.put("CULTIME", tp.getCulminatedTime());
				TPValues.put("CULDISTANCE", tp.getCulminatedDistance());
				db.insert(TrackDatabaseOpenHelper.TRACKPOINT_TABLE_NAME, null, TPValues);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();

		//3. change tracksOnMapList
		if(tracksOnMapList.contains(oldTrackName)) {
			tracksOnMapList.remove(oldTrackName);
			tracksOnMapList.add(newTrackName);

			// update markerMap
			Iterator<Entry<Marker, MarkerInfo>> it = markerMap.entrySet().iterator();
			while(it.hasNext()) {
				Entry<Marker, MarkerInfo> entry = it.next();
				MarkerInfo markerInfo = entry.getValue();
				if(markerInfo.getName().equals(oldTrackName))
					markerInfo.setName(newTrackName);
			}

			// update markerInfo
			main.updateMarkerInfo();

			//4. change RouteOnMapList in database
			db = trackDBHelper.getWritableDatabase();

			ContentValues values2 = new ContentValues(); 
			values2.put("NAME", newTrackName);	
			db.update(TrackDatabaseOpenHelper.TRACK_ON_MAP_TABLE_NAME, values2, "NAME=?", new String[]{oldTrackName});

			db.close();
		}
	}

	/**
	 * Get a track object from trackList using a trackName
	 * @param requestedTrackName
	 * @return
	 */
	public Track getTrack(String requestedTrackName) {
		int i = 0;
		Track result = null;
		while(i<trackList.size() && result==null) {
			Track track = trackList.get(i);
			String trackName = track.getName();
			if(trackName.equals(requestedTrackName))
				result = track;
			else
				i++;
		}		
		return result;
	}

	public ArrayList<String> getCheckedTrackNames() {
		return checkedTrackNames;
	}

	public ArrayList<Track> getTrackList() {
		return trackList;
	}

	public ArrayList<String> getTracksOnMapList() {
		return tracksOnMapList;
	}

	public HashMap<Marker,MarkerInfo> getMarkerMap() {
		return markerMap;
	}

	/**
	 * Track should be hidden before update
	 * @param trackName
	 * @param updateAll
	 */
	@SuppressWarnings("unchecked")
	public void updateElevations(String trackName, boolean updateAll, double maxAltitude, double minAltitude, boolean showAfter) {
		Track track = getTrack(trackName);
		ArrayList<TrackPoint> TPList = track.getTPList();
		ArrayList<LatLng> requestList;
		double[] maxMin = new double[2];
		maxMin[0] = maxAltitude;
		maxMin[1] = minAltitude;

		if(updateAll==true) {
			ArrayList<LatLng> latLngList = new ArrayList<LatLng>();
			for(int i=0;i<TPList.size();i++) {
				TrackPoint tp = TPList.get(i);
				Location location = tp.getLocation();
				latLngList.add(new LatLng(location.getLatitude(),location.getLongitude()));
			}			
			requestList = latLngList;
		} else {			
			requestList = new ArrayList<LatLng>();			
			// 1. Find zero elevation in latLngList
			for(int i=0;i<TPList.size();i++) {
				TrackPoint tp = TPList.get(i);
				Location location = tp.getLocation();
				double altitude = location.getAltitude();

				if(altitude>maxMin[0] || altitude<maxMin[1])
					requestList.add(new LatLng(location.getLatitude(),location.getLongitude()));
			}
		}
		if(requestList.size()>0) {
			// Create task
			GetElevationTask task = new GetElevationTask();
			task.setType(MapObjectType.track);
			task.setTag(track.getName());
			editTaskMap.put(task, track);
			updateElevationModeMap.put(task, updateAll);
			updateElevationBoundaryMap.put(task, maxMin);
			showMap.put(task, showAfter);
			task.setDelegate(this);
			task.execute(requestList);
		} else {
			Toast.makeText(MainActivity.main, "No update needed", Toast.LENGTH_SHORT).show();
			if(showAfter==true)
				main.getMapManager().addTrackOnMap(trackName);
		}
	}

	/**
	 * The edited track should always be hidden before tasks begin
	 * @param task
	 */
	@Override
	public void processGetElevationFinish(GetElevationTask task) {
		// finish updating
		if(updateElevationModeMap.get(task)!=null)
			finishUpdateElevation(task);
	}

	private void finishUpdateElevation(GetElevationTask task) {
		Track track = editTaskMap.get(task);		
		boolean updateAll = updateElevationModeMap.get(task);
		double[] maxMin = updateElevationBoundaryMap.get(task);
		boolean showOnMap = showMap.get(task);
		editTaskMap.remove(task);
		updateElevationModeMap.remove(task);
		updateElevationBoundaryMap.remove(task);
		showMap.remove(task);

		if(task.isErrorFatal()) { // do nothing and add track back to map
			main.getMapManager().addTrackOnMap(track.getName());
		} else {
			ArrayList<Double> elevationList = task.getElevationList();
			ArrayList<TrackPoint> TPList = track.getTPList();
			if(updateAll==true) {
				for(int i=0;i<TPList.size();i++) {
					TrackPoint tp = TPList.get(i);
					tp.getLocation().setAltitude(elevationList.get(i));
				}
			} else {
				int index = 0;
				for(int i=0;i<TPList.size();i++) {
					TrackPoint tp = TPList.get(i);
					Location location = tp.getLocation();
					double altitude = location.getAltitude();
					if(altitude>maxMin[0] || altitude<maxMin[1]) {
						location.setAltitude(elevationList.get(index));
						index++;
					}
				}
			}
			String trackName = track.getName();
			modifyTrack(trackName, track.getTPList());

			// notify changes
			Toast.makeText(MainActivity.main, "Elevation update finished", Toast.LENGTH_SHORT).show();
			if(SelectTrackActivity.thisActivity!=null)
				SelectTrackActivity.notifyDataSetChanged();
			if(showOnMap==true)
				main.getMapManager().addTrackOnMap(track.getName());
		}
	}

	public ArrayList<String> getAllTrackName() {
		ArrayList<String> trackNameList = new ArrayList<String>();
		for(int i=0;i<trackList.size();i++)
			trackNameList.add(trackList.get(i).getName());
		return trackNameList;
	}

	public HashMap<GetElevationTask, Track> getEditTaskMap() {
		return editTaskMap;
	}

}
