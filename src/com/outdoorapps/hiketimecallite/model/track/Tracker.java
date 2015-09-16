package com.outdoorapps.hiketimecallite.model.track;

import java.util.ArrayList;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.database.TrackDatabaseOpenHelper;
import com.outdoorapps.hiketimecallite.managers.MapManager;
import com.outdoorapps.hiketimecallite.support.constants.Defaults;
import com.outdoorapps.hiketimecallite.support.constants.Version;

public class Tracker {
	private TrackingPoint lastTrackingPT;
	private UpdatePolylineRunnable updatePolyline;
	private boolean pauseUpdate;
	private Handler savingHandler;
	public static final int TRACKING_NOTIFICATION_ID = 0;

	public Tracker() {
		pauseUpdate = false;		
	}

	public void start(MapManager mapManager) {
		clearDatabase();
		startSaving();
		updatePolyline = new UpdatePolylineRunnable(mapManager);
		updatePolyline.startDrawing();

		// Create Notification
		NotificationCompat.Builder mBuilder = 
				new NotificationCompat.Builder(MainActivity.main)
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle(MainActivity.main.getString(R.string.tracking))
		.setContentText(Version.CREATOR)
		.setOngoing(true);

		Intent resultIntent = new Intent(MainActivity.main,MainActivity.class);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(MainActivity.main,0,resultIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
				(NotificationManager) MainActivity.main.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(TRACKING_NOTIFICATION_ID, mBuilder.build());
	}

	public void stop(MapManager mapManager) {
		mapManager.removeTrackerPolyline();
		savingHandler.removeCallbacksAndMessages(null);
		updatePolyline.stopUpdating();

		// Cancel Notifications
		NotificationManager mNotificationManager =
				(NotificationManager) MainActivity.main.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(TRACKING_NOTIFICATION_ID);
	}

	private class UpdatePolylineRunnable implements Runnable {

		private Handler polylineHandler, latLngHandler; // latLngHandler keeps running until user stops tracking
		private Polyline polyline;
		private MapManager mapManager;
		private ArrayList<LatLng> latLngList;

		public UpdatePolylineRunnable(MapManager mapManager) {
			this.mapManager = mapManager;
			polylineHandler = new Handler();
			latLngHandler = new Handler();
			polyline = mapManager.getTrackerPolyline();
			latLngList = new ArrayList<LatLng>();
		}

		@Override
		public void run() {			
			polyline.setPoints(latLngList);
			polylineHandler.postDelayed(this, Defaults.DEFAULT_DRAW_FREQUENCY);
		}
		
		/**
		 * Define update rate of the latLngList used for drawing polyline
		 * reduce rate on pause to conserve power
		 * @param freq
		 */
		private void updateLatLngList(final int freq) {
			if(latLngHandler!=null)
				latLngHandler.removeCallbacksAndMessages(null);
			
			latLngHandler.postDelayed(new Runnable() {
				public void run() {
					if(lastTrackingPT!=null) {
						Location location = lastTrackingPT.getLocation();
						LatLng lastLatLng = new LatLng(location.getLatitude(),location.getLongitude());
						latLngList.add(lastLatLng);
					}
					latLngHandler.postDelayed(this, freq);
				}
			}, freq);
		}

		public void pauseDrawing() {
			polylineHandler.removeCallbacksAndMessages(null);
			updateLatLngList(Defaults.DEFAULT_TRACKING_SAVING_FREQUENCY); // Slow down
		}
		
		private void resumeDrawing() {
			polyline.remove(); // Get a new polyline
			polyline = mapManager.getTrackerPolyline();
			polyline.setPoints(latLngList);
			polylineHandler.postDelayed(this, Defaults.DEFAULT_DRAW_FREQUENCY);
			updateLatLngList(Defaults.DEFAULT_DRAW_FREQUENCY);
		}

		public void startDrawing() {
			polyline = mapManager.getTrackerPolyline();
			polylineHandler.postDelayed(updatePolyline, Defaults.DEFAULT_DRAW_FREQUENCY);
			updateLatLngList(Defaults.DEFAULT_DRAW_FREQUENCY);
		}

		public void stopUpdating() {
			polylineHandler.removeCallbacksAndMessages(null);
			latLngHandler.removeCallbacksAndMessages(null);
		}
	}

	private void startSaving() {
		savingHandler = new Handler(); // this handler keep running until the user stops tracking
		savingHandler.postDelayed(new Runnable() {
			public void run() {
				if(lastTrackingPT!=null) {
					saveTrackingPoint(lastTrackingPT);
					savingHandler.postDelayed(this, Defaults.DEFAULT_TRACKING_SAVING_FREQUENCY);
				}
			}
		}, Defaults.DEFAULT_TRACKING_SAVING_FREQUENCY);
	}

	/**
	 * Pause updating polyline onPause to conserve power
	 * @param pauseUpdate
	 */
	public void setPauseUpdate(boolean pauseUpdate) {
		this.pauseUpdate = pauseUpdate;
		if(pauseUpdate==false)
			updatePolyline.resumeDrawing();
		else
			updatePolyline.pauseDrawing();
	}

	private void saveTrackingPoint(TrackingPoint trackingPT) {
		TrackDatabaseOpenHelper trackerDBOpenHelper = new TrackDatabaseOpenHelper(MainActivity.main);
		SQLiteDatabase db = trackerDBOpenHelper.getWritableDatabase();
		Location location = trackingPT.getLocation();
		ContentValues values = new ContentValues();
		values.put("LAT", location.getLatitude());
		values.put("LON", location.getLongitude());
		values.put("ALTITUDE", location.getAltitude());
		values.put("TIME", trackingPT.getTime());
		values.put("BEARING", location.getBearing());
		values.put("SPEED", location.getSpeed());
		values.put("ACCURACY", location.getAccuracy());		
		db.insert(TrackDatabaseOpenHelper.TRACKING_POINT_TABLE_NAME, null, values);		
		db.close();
	}

	/**
	 *  Should be called only when track is saved or discarded
	 *  The first in main when stop tracking occurred
	 *  The second in TrackRecoveyManager after track has been successfully exported
	 *  The third when the user discards it
	 */
	public static void clearDatabase() {
		TrackDatabaseOpenHelper trackerDBOpenHelper = new TrackDatabaseOpenHelper(MainActivity.main);
		SQLiteDatabase db = trackerDBOpenHelper.getWritableDatabase();
		db.delete(TrackDatabaseOpenHelper.TRACKING_POINT_TABLE_NAME, null, null);
		db.close();
	}

	public static ArrayList<TrackingPoint> loadFromDatabase() {
		ArrayList<TrackingPoint> trackingPointList = new ArrayList<TrackingPoint>();
		TrackDatabaseOpenHelper trackerDBOpenHelper = new TrackDatabaseOpenHelper(MainActivity.main);
		SQLiteDatabase db = trackerDBOpenHelper.getReadableDatabase();
		String sql = "SELECT * FROM " + TrackDatabaseOpenHelper.TRACKING_POINT_TABLE_NAME + ";";
		Cursor cursor = db.rawQuery(sql, null);
	
		if(cursor.moveToFirst()) {
			do {
				Location location = new Location("");
				double lat = Double.parseDouble(cursor.getString(0));
				double lon = Double.parseDouble(cursor.getString(1));
				double altitude = Double.parseDouble(cursor.getString(2));
				String time = cursor.getString(3);
				float bearing = Float.parseFloat(cursor.getString(4));
				float speed = Float.parseFloat(cursor.getString(5));
				float accuracy = Float.parseFloat(cursor.getString(6));
	
				location.setLatitude(lat);
				location.setLongitude(lon);
				location.setAltitude(altitude);
				location.setBearing(bearing);
				location.setSpeed(speed);
				location.setAccuracy(accuracy);
	
				trackingPointList.add(new TrackingPoint(location,time));				
			} while(cursor.moveToNext());
		}
		db.close();
		return trackingPointList;
	}

	public boolean getPauseUpdate() {
		return pauseUpdate;
	}

	public TrackingPoint getLastTrackingPoint() {
		return lastTrackingPT;
	}

	public void setLastTrackingPoint(TrackingPoint lastTrackingPT) {
		this.lastTrackingPT = lastTrackingPT;
	}

}
