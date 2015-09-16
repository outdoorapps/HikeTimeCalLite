package com.outdoorapps.hiketimecallite.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ChecklistDatabaseOpenHelper extends SQLiteOpenHelper {
	public static final String DATABASE_NAME = "Checklist_Database";
	public static final int DATABASE_VERSION = 1;
	public static final String CHECKLIST_TITLE_TABLE_NAME = "ChecklistTitle";
	public static final String CHECKLIST_CATEGORY_TABLE_NAME = "ChecklistCategory";
	public static final String CHECKLIST_TABLE_NAME = "Checklist";
	
	private static final String CHECKLIST_TITLE_TABLE_CREATE =
			"CREATE TABLE " + CHECKLIST_TITLE_TABLE_NAME + 
			" (TITLE TEXT not null);";
	
	private static final String CHECKLIST_CATEGORY_TABLE_CREATE =
			"CREATE TABLE " + CHECKLIST_CATEGORY_TABLE_NAME + 
			" (CATEGORY TEXT not null);";
	
	private static final String CHECKLIST_TABLE_CREATE =
			"CREATE TABLE " + CHECKLIST_TABLE_NAME + 
			" (CATEGORY TEXT not null, " +
			"ITEM TEXT not null, " +
			"CHECKED INTEGER not null);";
	
	public ChecklistDatabaseOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CHECKLIST_TITLE_TABLE_CREATE);
		db.execSQL(CHECKLIST_CATEGORY_TABLE_CREATE);
		db.execSQL(CHECKLIST_TABLE_CREATE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		Log.w(ChecklistDatabaseOpenHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to " + newVersion);
		// Remove redundant tables
        
        // Add new tables after v1.1
	}
	
	public void reCreateAll(SQLiteDatabase database) {
		database.execSQL("DROP TABLE IF EXISTS "+CHECKLIST_TITLE_TABLE_NAME);
        database.execSQL("DROP TABLE IF EXISTS "+CHECKLIST_CATEGORY_TABLE_NAME);
        database.execSQL("DROP TABLE IF EXISTS "+CHECKLIST_TABLE_NAME);
        onCreate(database);
	}
}
