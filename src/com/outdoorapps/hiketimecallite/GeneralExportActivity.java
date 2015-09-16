package com.outdoorapps.hiketimecallite;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.outdoorapps.hiketimecallite.adaptors.FileRowAdapter;
import com.outdoorapps.hiketimecallite.support.ImportExport.ImportExportType;
import com.outdoorapps.hiketimecallite.support.Storage;
import com.outdoorapps.hiketimecallite.support.constants.ExtraTypes;
import com.outdoorapps.hiketimecallite.support.constants.Links;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;

public class GeneralExportActivity extends FragmentActivity implements 
OnEditorActionListener,
OnCreateContextMenuListener,
OnItemClickListener {

	private GeneralExportActivity thisActivity;
	private FileRowAdapter adapter;
	private ArrayList<String> item;
	private List<String> path;
	private static String previous, currentFolder, fileExtension;
	private TextView myPath, fileTypeView;
	private SharedPreferences pref;
	private EditText fileNameEdit;
	private static ImportExportType exportType;
	private static final String[] contextMenuItems = {"Delete"};
	private ListView list;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_general_export);
		thisActivity = this;
		Intent intent = getIntent();
		fileExtension = intent.getStringExtra(ExtraTypes.FILE_EXTENSION);
		fileTypeView = (TextView)findViewById(R.id.file_type);
		myPath = (TextView)findViewById(R.id.path);
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		
		// Find preferred folder
		// Get import type
		if(intent.hasExtra(ExtraTypes.EXPORT_TYPE))
			exportType = ImportExportType.valueOf(intent.getStringExtra(ExtraTypes.EXPORT_TYPE));
		else
			exportType = null;
		String key = PrefKeys.KEY_GENERAL_EXPORT_FOLDER;
		if(exportType!=null) {
			switch(exportType) {
			case checklist:
				key = PrefKeys.KEY_CHECKLIST_EXPORT_FOLDER;
				break;
			case route:
				key = PrefKeys.KEY_ROUTE_EXPORT_FOLDER;
				break;
			case track:
				key = PrefKeys.KEY_TRACK_EXPORT_FOLDER;
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

		fileTypeView.setText("File Type: " + fileExtension);
		fileNameEdit = (EditText) this.findViewById(R.id.file_name_field);

		if(intent.hasExtra(ExtraTypes.PREFERRED_FILE_NAME)) {
			String preferredName = intent.getStringExtra(ExtraTypes.PREFERRED_FILE_NAME);
			fileNameEdit.setText(preferredName);
		} else
			fileNameEdit.setText("");
		fileNameEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
		fileNameEdit.setOnEditorActionListener(this);

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
		String key = PrefKeys.KEY_GENERAL_EXPORT_FOLDER;
		if(exportType!=null) {
			switch(exportType) {
			case checklist:
				key = PrefKeys.KEY_CHECKLIST_EXPORT_FOLDER;
				break;
			case route:
				key = PrefKeys.KEY_ROUTE_EXPORT_FOLDER;
				break;
			case track:
				key = PrefKeys.KEY_TRACK_EXPORT_FOLDER;
				break;
			default:
				break;
			}
		}
		editor.putString(key, currentFolder);
		editor.commit();
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
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.general_export, menu);
		return true;
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
		case R.id.save:
			exportCheck();
			break;
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
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_DONE) {
			exportCheck();
			return true;
		}
		return false;
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
					// filter for .gpx and .kml files (depends of choice)
					if((file+"").toLowerCase(Locale.getDefault()).endsWith(fileExtension)) {
						path.add(file.getPath());
						item.add(file.getName());
					}
				}
			}
		}

		// List items
		adapter = new FileRowAdapter(this,item);
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

	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
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
		}		
	}

	public void exportCheck() {
		// check writable
		File current = new File(currentFolder);
		if(!current.canWrite())
			Toast.makeText(this, "This folder is not writable", Toast.LENGTH_SHORT).show();
		else {
			// check overwrite
			final String fileName = fileNameEdit.getText().toString();
			if(fileName.equals(""))
				Toast.makeText(this, "Please enter a file name", Toast.LENGTH_SHORT).show();
			else {
				final String outputFileName = fileName + fileExtension;

				boolean needOverwrite = false;
				String[] fileList = current.list();
				int i=0;
				while(needOverwrite==false && i<fileList.length) {
					String outputFileNameLowerCase = outputFileName.toLowerCase(Locale.getDefault());
					String fileNameLowerCase = fileList[i].toLowerCase(Locale.getDefault());
					needOverwrite = outputFileNameLowerCase.equals(fileNameLowerCase);
					i++;
				}

				if(needOverwrite==true) {
					new AlertDialog.Builder(this)
					.setTitle(R.string.export_conflict)
					.setMessage("["+outputFileName+"] " + getString(R.string.export_conflict_message))
					.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							returnExportPath(outputFileName);
						}
					})
					.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// do nothing
						}
					}).show();
				}
				else
					returnExportPath(outputFileName);
			}
		}
	}

	private void returnExportPath(String outputFileName) {
		String fullPath = currentFolder + File.separator + outputFileName;
		Intent intent = new Intent();
		intent.putExtra(ExtraTypes.FILE_PATH, fullPath);
		setResult(RESULT_OK, intent);
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(fileNameEdit.getWindowToken(), 0);
		finish();
	}
}
