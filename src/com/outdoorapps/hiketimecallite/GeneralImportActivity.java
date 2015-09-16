package com.outdoorapps.hiketimecallite;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.outdoorapps.hiketimecallite.adaptors.FileRowAdapter;
import com.outdoorapps.hiketimecallite.support.ImportExport.ImportExportType;
import com.outdoorapps.hiketimecallite.support.Storage;
import com.outdoorapps.hiketimecallite.support.constants.ExtraTypes;
import com.outdoorapps.hiketimecallite.support.constants.Links;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;

public class GeneralImportActivity extends FragmentActivity implements 
OnItemClickListener,
OnCreateContextMenuListener {

	private GeneralImportActivity thisActivity;
	private FileRowAdapter adapter;
	private ArrayList<String> item;
	private List<String> path;
	private String extensionFilterTypes;
	private String previous, currentFolder;
	private static String[] extensionFilterArray;
	private TextView filterView, myPath;
	private SharedPreferences pref;
	private static final String[] contextMenuItems = {"Delete"};
	private ListView list;

	private static ImportExportType importType;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_general_import);
		thisActivity = this;
		Intent intent = getIntent();
		extensionFilterTypes = intent.getStringExtra(ExtraTypes.FILE_EXTENSION);		
		extensionFilterArray = extensionFilterTypes.split(",");
		filterView = (TextView)findViewById(R.id.filter);
		myPath = (TextView)findViewById(R.id.path);
		pref = PreferenceManager.getDefaultSharedPreferences(this);

		// Find preferred folder
		// Get import type
		if(intent.hasExtra(ExtraTypes.IMPORT_TYPE))
			importType = ImportExportType.valueOf(intent.getStringExtra(ExtraTypes.IMPORT_TYPE));
		else
			importType = null;
		String key = PrefKeys.KEY_GENERAL_IMPORT_FOLDER;
		if(importType!=null) {
			switch(importType) {
			case checklist:
				key = PrefKeys.KEY_CHECKLIST_IMPORT_FOLDER;
				break;
			case route:
				key = PrefKeys.KEY_ROUTE_IMPORT_FOLDER;
				break;
			case track:
				key = PrefKeys.KEY_TRACK_IMPORT_FOLDER;
				break;
			default:
				break;
			}
		}
		String preferredStartFolder = intent.getStringExtra(ExtraTypes.PREFERRED_START_FOLDER);
		currentFolder = pref.getString(key, preferredStartFolder);
		Storage.createAppFoldersIfNeeded(); // make sure the default folders exists			
		File start = new File(currentFolder);
		previous = start.getParent();

		if(extensionFilterTypes.equals(""))
			extensionFilterTypes = "All Types";
		else
			extensionFilterTypes = extensionFilterTypes.replace(".", " ");
		filterView.setText("File Type: " + extensionFilterTypes);
		
		list =(ListView)findViewById(android.R.id.list);
		list.setOnCreateContextMenuListener(this);
		registerForContextMenu(list);
	}

	@Override
	public void onResume() {
		super.onResume();
		getDir(currentFolder);
	}

	@Override
	public void onPause() {
		super.onPause();
		SharedPreferences.Editor editor = pref.edit();
		String key = PrefKeys.KEY_GENERAL_IMPORT_FOLDER;
		if(importType!=null) {
			switch(importType) {
			case checklist:
				key = PrefKeys.KEY_CHECKLIST_IMPORT_FOLDER;
				break;
			case route:
				key = PrefKeys.KEY_ROUTE_IMPORT_FOLDER;
				break;
			case track:
				key = PrefKeys.KEY_TRACK_IMPORT_FOLDER;
				break;
			default:
				break;
			}
		}
		editor.putString(key, currentFolder);
		editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.general_import, menu);
		return true;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId()==android.R.id.list) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
			// avoid actions outside storage folder and non files
			if(!(currentFolder.equals(Storage.ROOT)) && info.position>0 && !(item.get(info.position).endsWith("/"))) {
				menu.setHeaderTitle(item.get(info.position));
				for (int i = 0; i<contextMenuItems.length; i++) {
					menu.add(Menu.NONE, i, i, contextMenuItems[i]);
				}
			}						
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		int menuItemIndex = item.getItemId();
		switch(menuItemIndex) {
		case 0: // Delete
			final File f = new File(currentFolder + File.separator + this.item.get(info.position));
			new AlertDialog.Builder(this)		
			.setTitle(R.string.delete_item)
			.setMessage(R.string.delete_item_message)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					try {
						f.delete();
						path.remove(info.position);
						thisActivity.item.remove(info.position);
						adapter.notifyDataSetChanged();
					} catch (Exception e) {
						Toast.makeText(thisActivity, "Errors during deleting", Toast.LENGTH_SHORT).show();
					}
				}
			})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					// do nothing
				}
			}).show();
			break;
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.help:
			Uri uri = Uri.parse(Links.HELP_LINK); 
			startActivity(new Intent(Intent.ACTION_VIEW, uri));
			break;
		case R.id.action_settings:
			Intent intent = new Intent(this,SettingsActivity.class);
			startActivity(intent);
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		File file = new File(path.get(position));
		if (file.isDirectory()){
			if(file.canRead())
				getDir(path.get(position));
			else {
				new AlertDialog.Builder(this)
				.setTitle("[" + file.getName() + "] folder can't be read!")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
			}
		} else {
			final File f = file;
			String message = getString(R.string.general_import_message) + " [" + f.getName() + "]?";

			new AlertDialog.Builder(this)
			.setTitle(R.string.general_import_title)
			.setMessage(message)
			.setPositiveButton(R.string.yes,new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which){
					Intent intent = new Intent();
					intent.putExtra(ExtraTypes.FILE_PATH, f.getAbsolutePath());
					setResult(RESULT_OK, intent);
					finish();
				}
			})
			.setNeutralButton(R.string.no, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which){
					// Do nothing					
				}
			}).show();
		}	
	}

	private void getDir(String dirPath){
		currentFolder = dirPath;
		myPath.setText("Location: " + dirPath);
		item = new ArrayList<String>();
		path = new ArrayList<String>();
		File f = new File(dirPath);
		File[] files;
		if(f.isDirectory())	// avoid crashing if foler is change outside the app
			files = f.listFiles();
		else {
			f = new File(Storage.ROOT);
			files = f.listFiles();
			myPath.setText("Location: " + f.getAbsolutePath());
		}

		// Sorting
		Arrays.sort(files, new fileComparator());

		// Create path and items
		if(!dirPath.equals(Storage.ROOT)) {
			previous = f.getParent();
			item.add(Storage.UP_FOLDER_TEXT);
			path.add(previous);			
		}

		for(int i=0; i<files.length; i++){
			File file = files[i];
			if(file.canRead()) { // ignore unreadable files
				if(file.isDirectory()) {
					if(file.getName().startsWith(".")==false) { // ignore special folders
						path.add(file.getPath());
						item.add(file.getName() + "/");	
					}
				}
				else {
					if(checkExtension(file)) {
						path.add(file.getPath());
						item.add(file.getName());
					}
				}
			}
		}

		// List items
		adapter = new FileRowAdapter(this,item);
		list =(ListView)findViewById(android.R.id.list);
		list.setAdapter(adapter);
		list.setOnItemClickListener(this);
	}

	private class fileComparator implements Comparator<File> {
		@Override
		public int compare(File f1, File f2) {
			if(f1.isDirectory() && f2.isDirectory()==false)			
				return -1;
			else {
				if(f1.isDirectory()==false && f2.isDirectory())
					return 1;
				else{
					String name1 = f1.toString();
					String name2 = f2.toString();
					return name1.compareToIgnoreCase(name2);
				}
			}
		}

	}

	private boolean checkExtension(File file) {
		boolean result = false;
		String filePath = (file+"").toLowerCase(Locale.getDefault());
		int i = 0;
		while(i<extensionFilterArray.length && result==false) {
			result = filePath.endsWith(extensionFilterArray[i]);
			i++;
		}
		return result;
	}
}
