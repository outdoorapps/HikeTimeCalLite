package com.outdoorapps.hiketimecallite.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TrackDatabaseOpenHelper extends SQLiteOpenHelper {
	public static final String DATABASE_NAME = "Tracker_Database";
	public static final int DATABASE_VERSION = 1;
	public static final String TRACKING_POINT_TABLE_NAME = "TrackingPoint";
	public static final String TRACK_HEADER_TABLE_NAME = "TrackHeader";
	public static final String TRACKPOINT_TABLE_NAME = "TrackPoint";
	public static final String TRACK_ON_MAP_TABLE_NAME = "TrackOnMap";
	
	private static final String TRACKING_POINT_TABLE_CREATE =
			"CREATE TABLE " + TRACKING_POINT_TABLE_NAME + 
			" (LAT TEXT not null, " +
			"LON TEXT not null, " +
			"ALTITUDE TEXT not null, " +
			"TIME TEXT not null, " +
			"BEARING TEXT not null, " +
			"SPEED TEXT not null, " +
			"ACCURACY TEXT not null);";
	
	private static final String TRACK_HEADER_TABLE_CREATE =
			"CREATE TABLE " + TRACK_HEADER_TABLE_NAME + 
			" (NAME TEXT not null, " +
			" DISTANCE TEXT not null, " +
			" DURATION TEXT not null, " +
			" ELEVATIONGAIN TEXT not null, " +
			" DATE TEXT not null);";
					
	private static final String TRACKPOINT_TABLE_CREATE =
			"CREATE TABLE " + TRACKPOINT_TABLE_NAME + 
			" (NAME TEXT not null, " +
			"LAT TEXT not null, " +
			"LON TEXT not null, " +
			"ALTITUDE TEXT not null, " +
			"TIME TEXT not null, " +
			"BEARING TEXT not null, " +
			"SPEED TEXT not null, " +
			"ACCURACY TEXT not null, " +
			"CULTIME TEXT not null, " +
			"CULDISTANCE TEXT not null);";
	
	private static final String TRACK_ON_MAP_TABLE_CREATE =
			"CREATE TABLE " + TRACK_ON_MAP_TABLE_NAME + 
			" (NAME TEXT not null);";
	
	public TrackDatabaseOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public boolean checkUnsavedData() {
		SQLiteDatabase db = this.getReadableDatabase();
		String sql = "SELECT * FROM " + TrackDatabaseOpenHelper.TRACKING_POINT_TABLE_NAME + ";";
		Cursor cursor = db.rawQuery(sql, null);
		boolean result = cursor.moveToFirst();
		db.close();
		return result;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TRACKING_POINT_TABLE_CREATE);
		db.execSQL(TRACK_HEADER_TABLE_CREATE);
		db.execSQL(TRACKPOINT_TABLE_CREATE);
		db.execSQL(TRACK_ON_MAP_TABLE_CREATE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		Log.w(TrackDatabaseOpenHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to " + newVersion);
		// Remove redundant tables
        
        // Add new tables after v1.1
	}
	
	public void reCreateAll(SQLiteDatabase database) {
		database.execSQL("DROP TABLE IF EXISTS "+TRACKING_POINT_TABLE_NAME);
		database.execSQL("DROP TABLE IF EXISTS "+TRACK_HEADER_TABLE_NAME);
		database.execSQL("DROP TABLE IF EXISTS "+TRACKPOINT_TABLE_NAME);
		database.execSQL("DROP TABLE IF EXISTS "+TRACK_ON_MAP_TABLE_NAME);
        onCreate(database);
	}
}
