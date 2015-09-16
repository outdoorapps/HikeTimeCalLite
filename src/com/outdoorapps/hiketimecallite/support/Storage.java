package com.outdoorapps.hiketimecallite.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.Environment;

import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.support.constants.Version;

public class Storage {
	public static final String ROOT = Environment.getExternalStorageDirectory().getParent(); // "/storage"
	public static final String APP_FOLDER = Environment.getExternalStorageDirectory()+File.separator+Version.CREATOR.replace(" ", "");
	public static final String ROUTE_FOLDER = APP_FOLDER + File.separator + "Routes";
	public static final String TRACKS_FOLDER = APP_FOLDER + File.separator + "Tracks";
	public static final String TRACK_RECOVERY_FOLDER = TRACKS_FOLDER + File.separator + "Recovered_Track";
	public static final String CHECKLISTS_FOLDER = APP_FOLDER + File.separator + "Checklists";
	public static final String UP_FOLDER_TEXT = "..";
	public static final String SAMPLE_ROUTE_PATH = ROUTE_FOLDER+File.separator+"Big_Baldy_Hike.gpx";
	public static final String SAMPLE_CHECKLIST_PATH = CHECKLISTS_FOLDER+File.separator+"day_hike.csv";
	
	/**
	 * Check if APP_FOLDER and sub-folders need to be created, return true if it 
	 * exists already or has been created, false if it is not a folder or it 
	 * cannot be created
	 * @return
	 */
	public static boolean createAppFoldersIfNeeded() {
		File appFolder = new File(APP_FOLDER);
		if(appFolder.isDirectory())
			return createSubFoldersIfNeeded();
		else {
			try {
				appFolder.mkdir();
				return createSubFoldersIfNeeded();
			} catch (Exception e) {
				return false;
			}
		}
	}
	
	private static boolean createSubFoldersIfNeeded() {
		try {
			createFolderIfNeeded(ROUTE_FOLDER);	
			createFolderIfNeeded(TRACKS_FOLDER);
			createFolderIfNeeded(TRACK_RECOVERY_FOLDER);
			createFolderIfNeeded(CHECKLISTS_FOLDER);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	private static void createFolderIfNeeded(String folderName) throws Exception {
		File folder = new File(folderName);
		if(!folder.isDirectory())
			folder.mkdir();
	}
	
	public static void copySampleFiles() throws IOException {
		// Copy Sample Route File
		InputStream routeInputStream = MainActivity.main.getResources().openRawResource(R.raw.big_baldy_hike);
		OutputStream routeOutputStream = new FileOutputStream(SAMPLE_ROUTE_PATH);
	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = routeInputStream.read(buf)) > 0) {
	    	routeOutputStream.write(buf, 0, len);
	    }
	    routeInputStream.close();
	    routeOutputStream.close();
	    
	    // Copy Sample Checklist
		InputStream checklistInputStream = MainActivity.main.getResources().openRawResource(R.raw.day_hike);
		OutputStream checklistOutputStream = new FileOutputStream(SAMPLE_CHECKLIST_PATH);
	    while ((len = checklistInputStream.read(buf)) > 0) {
	    	checklistOutputStream.write(buf, 0, len);
	    }
	    routeInputStream.close();
	    checklistOutputStream.close();
	}
}
