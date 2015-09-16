package com.outdoorapps.hiketimecallite.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class RouteDatabaseOpenHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "Route_Database";
	public static final int DATABASE_VERSION = 5;
	public static final String DRAWN_LATLNG_TABLE_NAME = "Drawn_LatLng";
	public static final String ROUTE_TABLE_NAME = "Route";
	public static final String ROUTEPOINT_TABLE_NAME = "RoutePoint";
	public static final String ROUTE_ON_MAP_TABLE_NAME = "RouteOnMap";
	public static final String PLANNER_TABLE_NAME = "Planner"; // redundant table included in v1.0.4
	
	private static final String DRAWN_LATLNG_TABLE_CREATE =
			"CREATE TABLE " + DRAWN_LATLNG_TABLE_NAME + 
			" (LATITUDE TEXT not null, " +
			"LONGITUDE TEXT not null);";
	private static final String ROUTE_TABLE_CREATE =
			"CREATE TABLE " + ROUTE_TABLE_NAME + 
			" (ROUTENAME TEXT not null primary key, " +
			"DATA TEXT not null, " +
			"PARAMETERS TEXT not null);";
	private static final String ROUTEPOINT_TABLE_CREATE =
			"CREATE TABLE " + ROUTEPOINT_TABLE_NAME + 
			" (ROUTENAME TEXT not null, " +
			"LATITUDE TEXT not null, " +
			"LONGITUDE TEXT not null, " +
			"ELEVATION TEXT not null, " +
			"DATA TEXT not null);";
	private static final String ROUTE_ON_MAP_TABLE_CREATE =
			"CREATE TABLE " + ROUTE_ON_MAP_TABLE_NAME + 
			" (ROUTENAME TEXT not null primary key);";

	public RouteDatabaseOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DRAWN_LATLNG_TABLE_CREATE);
		db.execSQL(ROUTE_TABLE_CREATE);
		db.execSQL(ROUTEPOINT_TABLE_CREATE);
		db.execSQL(ROUTE_ON_MAP_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		Log.w(RouteDatabaseOpenHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to " + newVersion);
		// Remove redundant tables
        database.execSQL("DROP TABLE IF EXISTS "+PLANNER_TABLE_NAME);
        
        // Add new tables
	}
	
	public void reCreateAll(SQLiteDatabase database) {
		database.execSQL("DROP TABLE IF EXISTS "+DRAWN_LATLNG_TABLE_NAME);
        database.execSQL("DROP TABLE IF EXISTS "+ROUTE_TABLE_NAME);
        database.execSQL("DROP TABLE IF EXISTS "+ROUTEPOINT_TABLE_NAME);
        database.execSQL("DROP TABLE IF EXISTS "+ROUTE_ON_MAP_TABLE_NAME);
        onCreate(database);
	}
}
