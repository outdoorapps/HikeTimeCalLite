package com.outdoorapps.hiketimecallite.managers;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.database.RouteDatabaseOpenHelper;

/**
 * Holds all data needed for drawing in Main, when main is being interacted with
 * Drawing Mode receive in put and return the necessary output for main the response
 * @author chbting
 *
 */
public class DrawingMode {
	private MainActivity main;
	private ArrayList<LatLng> pointList;
	private int pointCount;	
	private PolylineOptions polylineOptions;
	private boolean isDrawing;
	private RouteDatabaseOpenHelper routeDBHelper;
	
	public DrawingMode(MainActivity main) {
		this.main = main;
		routeDBHelper = new RouteDatabaseOpenHelper(main);
		pointList = new ArrayList<LatLng>();
		pointCount = 0;
		isDrawing = true;
	}

	public PolylineOptions getPolylineOptions() {
		polylineOptions = new PolylineOptions();
		for(int i=0;i<pointCount;i++) {
			LatLng position = pointList.get(i);
			polylineOptions.add(position);
		}
		polylineOptions.color(Color.parseColor(MapManager.POLYLINE_COLOR)).width(MapManager.POLYLINE_WIDTH);
		return polylineOptions;
	}
	
	public ArrayList<MarkerOptions> getMarkerOptionsList() {
		ArrayList<MarkerOptions> markerList = new ArrayList<MarkerOptions>();
		for(int i=0;i<pointCount;i++) {
			LatLng position = pointList.get(i);
			MarkerOptions marker = new MarkerOptions(); 
			marker.position(position);
			marker.anchor((float) 0.5, (float) 0.5);		
			if(i==0) {
				marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_start));
				marker.title("Start");				
			} else {
				if(i==pointCount-1) {
					marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_end));
					marker.title("End");
				}
				else {
					marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));
					marker.title("Point " + (i+1));
				}
			}
			markerList.add(marker);
		}
		return markerList;
	}

	public void clickEventResponse(LatLng point) {
		if(pointCount<pointList.size())// 0 count error
			pointList.set(pointCount, point);
		else
			pointList.add(pointCount, point);
		// remove extra points if the add comes after an undo
		for(int i=pointCount+1;i<pointList.size();i++)
			pointList.remove(i);
		pointCount++;
	}
	
	public ArrayList<LatLng> finalizePointList() {
		for(int i=pointList.size()-1;i>=pointCount;i--)
			pointList.remove(i);
		ArrayList<LatLng> latLngList = new ArrayList<LatLng>();
		// Use a copy of pointList for AsyncTask so drawing session can be disabled
		for(int i=0;i<pointList.size();i++)
			latLngList.add(pointList.get(i));
		return latLngList;
	}

	public void redo() {
		pointCount++;
	}
	
	public void undo() {
		pointCount--;
	}
	
	public void reverse() {
		ArrayList<LatLng> newPointList = new ArrayList<LatLng>();
		for(int i=pointCount-1;i>=0;i--)
			newPointList.add(pointList.get(i));			
		pointList = newPointList;
	}
	
	public boolean isDrawing() {
		return isDrawing;
	}
	
	public void saveDrawingSession() {
		SharedPreferences.Editor editor = main.getEditor();
		editor.putInt("pointCount", pointCount);
		editor.commit();
		if(isDrawing) {
			// save pointList to a database
			SQLiteDatabase db = routeDBHelper.getWritableDatabase();
			//remove old data first
			db.delete(RouteDatabaseOpenHelper.DRAWN_LATLNG_TABLE_NAME, null, null);
			for(int i=0;i<pointList.size();i++) {
				ContentValues values = new ContentValues(); 
				values.put("Latitude", pointList.get(i).latitude);
				values.put("Longitude", pointList.get(i).longitude);			
				db.insert(RouteDatabaseOpenHelper.DRAWN_LATLNG_TABLE_NAME, null, values);
			}
			db.close();
			routeDBHelper.close();
		}
	}

	public void recoverDrawingSession() {
		SharedPreferences pref = main.getPref();
		pointCount = pref.getInt("pointCount", 0);		

		SQLiteDatabase db = routeDBHelper.getReadableDatabase();
		String sql = "SELECT * FROM " + RouteDatabaseOpenHelper.DRAWN_LATLNG_TABLE_NAME + ";";
		Cursor cursor = db.rawQuery(sql, null);			
		pointList.clear();
		if (cursor.moveToFirst()) {
			do {
				Double lat = Double.parseDouble(cursor.getString(0));
				Double lon = Double.parseDouble(cursor.getString(1));
				LatLng latlng = new LatLng(lat,lon);
				pointList.add(latlng);
			} while (cursor.moveToNext());
		}
		db.close();
	}
	
	public ArrayList<LatLng> getPointList() {
		return pointList;
	}

	public void setPointList(ArrayList<LatLng> pointList) {
		this.pointList = pointList;
	}

	public int getPointCount() {
		return pointCount;
	}

	public void setPointCount(int pointCount) {
		this.pointCount = pointCount;
	}

}
