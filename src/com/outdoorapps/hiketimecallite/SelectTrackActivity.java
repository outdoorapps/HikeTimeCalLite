package com.outdoorapps.hiketimecallite;

import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.outdoorapps.hiketimecallite.adaptors.SelectTrackRowAdapter;
import com.outdoorapps.hiketimecallite.asynctasks.GetElevationTask;
import com.outdoorapps.hiketimecallite.dialogs.GoProDialog;
import com.outdoorapps.hiketimecallite.managers.ExportManager;
import com.outdoorapps.hiketimecallite.managers.ImportManager;
import com.outdoorapps.hiketimecallite.managers.MarkerInfo;
import com.outdoorapps.hiketimecallite.model.track.Track;
import com.outdoorapps.hiketimecallite.model.track.TrackData;
import com.outdoorapps.hiketimecallite.support.Formatters;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;
import com.outdoorapps.hiketimecallite.support.SplitActionBarStyle;
import com.outdoorapps.hiketimecallite.support.Verifier;
import com.outdoorapps.hiketimecallite.support.constants.Constants;
import com.outdoorapps.hiketimecallite.support.constants.Defaults;
import com.outdoorapps.hiketimecallite.support.constants.ExtraTypes;
import com.outdoorapps.hiketimecallite.support.constants.Links;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;
import com.outdoorapps.hiketimecallite.support.constants.RequestCode;

public class SelectTrackActivity extends FragmentActivity implements
ActionBar.OnNavigationListener,
OnItemClickListener,
OnSharedPreferenceChangeListener {

	private ArrayList<String> checkedTrackNames; /** Save checked track name */
	private static SelectTrackRowAdapter adaptor;
	private static TrackData trackData;
	public static Activity thisActivity;
	private static Formatters formatters;
	private static TextView totalTimeInfoView, totalDistanceInfoView, totalDistanceUnitView,
	totalElevationGainInfoView, totalElevationGainUnitView, numberOfHikesInfoView;

	private static ImportManager importManager; /** For import */
	private static ExportManager exportManager;
	public static final String EXPORT_TRACK_TAG = "exportTrack";

	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_track);
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
				// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(getActionBarThemedContextCompat(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, MainActivity.dropdownMenuItems), this);
		actionBar.setSelectedNavigationItem(2);		
		setupActionBar();
		thisActivity = this;

		checkedTrackNames = MainActivity.main.getTrackData().getCheckedTrackNames();
		if(trackData==null)
			trackData = MainActivity.main.getTrackData();
		// List Items
		adaptor = new SelectTrackRowAdapter(this,MainActivity.main.getTrackData().getTrackList(),checkedTrackNames);
		ListView list =(ListView)findViewById(android.R.id.list);
		list.setAdapter(adaptor);
		list.setOnItemClickListener(this);

		if(importManager==null)
			importManager = new ImportManager(MapObjectType.track);
		if(exportManager==null)
			exportManager = new ExportManager(MapObjectType.track);

		if(formatters==null)
			formatters = new Formatters();
		setTextView();
		setLogHeading();
	}

	@Override
	public void onResume() {
		super.onResume();
		adaptor.notifyDataSetChanged();

		// Open instant name check dialog if importing
		if(importManager.hasJob())
			importManager.showNameDialog(this);
		importManager.showProgressDialogIfNeeded(this);
		exportManager.showProgressDialogIfNeeded(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		importManager.dismissProgressDialogIfNeeded();
		exportManager.dismissProgressDialogIfNeeded();
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

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	@SuppressLint("NewApi")
	private void setupActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			SplitActionBarStyle.setSplitActionBarOverlayColor(actionBar);

			// Calculate ActionBar height
			TypedValue tv = new TypedValue();
			if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)){
				int actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
				ListView list =(ListView)findViewById(android.R.id.list);
				list.setPadding(0, 0, 0, actionBarHeight);
			}			
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private Context getActionBarThemedContextCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return getActionBar().getThemedContext();
		} else {
			return this;
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		Intent intent;
		switch(position) {		
		case 0: // Map
			finish();
			break;
		case 1: // Select Route 
			intent = new Intent(this,SelectRouteActivity.class);
			finish();
			startActivityForResult(intent, RequestCode.SELECT_ROUTE);
			break;
		case 2: // Select Track
			break;
		case 3: // calculator
			intent = new Intent(this,SimpleCalculatorActivity.class);
			finish();
			startActivityForResult(intent, RequestCode.SIMPLE_CALCULATOR);
			break;
		case 4: // checklist
			intent = new Intent(this,ChecklistActivity.class);
			finish();
			startActivityForResult(intent, RequestCode.CHECKLIST);
			break;
		default:
			break;
		}
		return true;		
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.select_track, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		Track track;
		switch (item.getItemId()) {		
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);		
			return true;
		case R.id.show_details:
			// Check if the selected track is being edited
			GoProDialog goProDialog = new GoProDialog();
			goProDialog.setFunctionName("View track details and update/correct elevations");
			goProDialog.show(getSupportFragmentManager(), "go_pro_track_details");
			break;
		case R.id.show_on_map:
			if(isEditingSelectedTrack()==false) {
				finish();
				MainActivity.main.showTracks(checkedTrackNames);
			}
			break;
		case R.id.hide_from_map:
			if(isEditingSelectedTrack()==false) {
				for(int i=0;i<checkedTrackNames.size();i++)
					MainActivity.main.getMapManager().hideTrackFromMap(checkedTrackNames.get(i));
				deselectAll();
				Toast.makeText(this, "Selected tracks are hidden", Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.import_track:
			importManager.startImportOperation(this);
			break;
		case R.id.export_track:
			if(isEditingSelectedTrack()==false) {
				track = trackData.getTrack(checkedTrackNames.get(0));
				if(track.getTPList().size()<Defaults.EXPORT_LIMIT)
					exportManager.startExportOperation(track.getName(),track,this);
				else {
					if(Verifier.verifyProVersion(this)==true) {
						Toast.makeText(this, "Pro version installation verified", Toast.LENGTH_SHORT).show();
						exportManager.startExportOperation(track.getName(),track,this);
					}
					else {
						GoProDialog goProDialog_export = new GoProDialog();
						goProDialog_export.setFunctionName("Save Track with more than 540 points (~1.5+ hrs)");
						goProDialog_export.show(getSupportFragmentManager(), "go_pro_track_export_limit");
					}
				}
			}
			break;
		case R.id.remove_track:
			if(isEditingSelectedTrack()==false) {
				new AlertDialog.Builder(this)
				.setTitle(R.string.remove_track)
				.setMessage(R.string.remove_track_message)			
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						ArrayList<String> checkedTrackNamesTemp = new ArrayList<String>();
						checkedTrackNamesTemp.addAll(checkedTrackNames);
						for(int i=0; i<checkedTrackNamesTemp.size(); i++) {
							String trackName = checkedTrackNamesTemp.get(i);
							if(trackData.removeTrack(trackName)) {
								// Deselect track if necessary
								MarkerInfo markerInfo = MainActivity.main.getMarkerInfo();
								if(markerInfo!=null)
									if(markerInfo.getName().equals(trackName))
										MainActivity.main.deselectMarker();
							}
						}									
						adaptor.notifyDataSetChanged();
						deselectAll();
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						// do nothing
					}
				}).show();
			}
			break;
		case R.id.select_all:
			selectAll();
			break;
		case R.id.deselect_all:
			deselectAll();
			break;
		case R.id.help:
			Uri uri = Uri.parse(Links.HELP_LINK); 
			startActivity(new Intent(Intent.ACTION_VIEW, uri));
			break;
		case R.id.action_settings:
			intent = new Intent(this,SettingsActivity.class);
			startActivity(intent);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// Show Track
		if(resultCode==RESULT_OK && requestCode==RequestCode.TRACK_DETAILS) {
			if(intent.hasExtra(ExtraTypes.TRACK_TO_BE_DRAWN)) {
				finish();
				MainActivity.main.showTracks(checkedTrackNames);
			}
		}

		// Export track
		if(resultCode==RESULT_OK && requestCode==RequestCode.EXPORT_GENERAL){
			exportManager.export(intent.getStringExtra(ExtraTypes.FILE_PATH),this);	
		}

		// Import track
		if(resultCode==RESULT_OK && requestCode==RequestCode.IMPORT_GENERAL){
			importManager.setImportPath(intent.getStringExtra(ExtraTypes.FILE_PATH));
			// Open dialog in onResume
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem showDetailsButton = menu.findItem(R.id.show_details);
		MenuItem showOnMapButton = menu.findItem(R.id.show_on_map);
		MenuItem hideFromMapButton = menu.findItem(R.id.hide_from_map);
		MenuItem importTrackButton = menu.findItem(R.id.import_track);
		MenuItem exportTrackButton = menu.findItem(R.id.export_track);		
		MenuItem removeTrackButton = menu.findItem(R.id.remove_track);

		Drawable showDetailsIcon = getResources().getDrawable(R.drawable.ic_details);
		Drawable showOnMapIcon = getResources().getDrawable(R.drawable.ic_show_on_map);
		Drawable hideFromMapIcon = getResources().getDrawable(R.drawable.ic_hide);
		Drawable importTrackIcon = getResources().getDrawable(R.drawable.ic_import);
		Drawable exportTrackIcon = getResources().getDrawable(R.drawable.ic_export);		
		Drawable removeTrackIcon = getResources().getDrawable(R.drawable.ic_remove);

		if(checkedTrackNames.size()==0) {
			showDetailsButton.setEnabled(false);
			showOnMapButton.setEnabled(false);
			hideFromMapButton.setEnabled(false);
			importTrackButton.setEnabled(true);
			exportTrackButton.setEnabled(false);		
			removeTrackButton.setEnabled(false);		
		} else {
			if(checkedTrackNames.size()==1) {
				showDetailsButton.setEnabled(true);
				showOnMapButton.setEnabled(true);
				hideFromMapButton.setEnabled(true);
				importTrackButton.setEnabled(false);
				exportTrackButton.setEnabled(true);
				removeTrackButton.setEnabled(true);
			} else {
				// if multiple tracks are selected
				showDetailsButton.setEnabled(false);
				showOnMapButton.setEnabled(true);
				hideFromMapButton.setEnabled(true);	
				importTrackButton.setEnabled(false);
				exportTrackButton.setEnabled(false);
				removeTrackButton.setEnabled(true);
			}
		}

		// Gray out disabled buttons
		if(showDetailsButton.isEnabled()==false)
			showDetailsIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
		if(showOnMapButton.isEnabled()==false)
			showOnMapIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
		if(hideFromMapButton.isEnabled()==false)
			hideFromMapIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
		if(importTrackButton.isEnabled()==false)
			importTrackIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
		if(exportTrackButton.isEnabled()==false)
			exportTrackIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);		
		if(removeTrackButton.isEnabled()==false)
			removeTrackIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);		

		showDetailsButton.setIcon(showDetailsIcon);
		showOnMapButton.setIcon(showOnMapIcon);
		hideFromMapButton.setIcon(hideFromMapIcon);
		importTrackButton.setIcon(importTrackIcon);
		exportTrackButton.setIcon(exportTrackIcon);		
		removeTrackButton.setIcon(removeTrackIcon);
		return true;		
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
		CheckBox checkBox = (CheckBox) v.findViewById(R.id.checkBox);
		checkBox.toggle();
		if(checkBox.isChecked())
			checkedTrackNames.add(MainActivity.main.getTrackData().getTrackList().get(position).getName());
		else {
			if(checkBox.isChecked()==false)
				checkedTrackNames.remove(MainActivity.main.getTrackData().getTrackList().get(position).getName());
		}
		// Enable menu items based on the number of selected tracks
		invalidateOptionsMenu();
	}

	public void selectAll() {
		ListView list =(ListView)findViewById(android.R.id.list);
		for(int i=0; i < list.getChildCount(); i++){
			ViewGroup item = (ViewGroup)list.getChildAt(i);
			CheckBox checkbox = (CheckBox)item.findViewById(R.id.checkBox);
			checkbox.setChecked(true);
			checkedTrackNames.add(MainActivity.main.getTrackData().getTrackList().get(i).getName());
		}
		invalidateOptionsMenu();
	}

	public void deselectAll() {
		ListView list =(ListView)findViewById(android.R.id.list);
		for(int i=0; i < list.getChildCount(); i++){
			ViewGroup item = (ViewGroup)list.getChildAt(i);
			CheckBox checkbox = (CheckBox)item.findViewById(R.id.checkBox);
			checkbox.setChecked(false);
		}
		checkedTrackNames.clear();
		invalidateOptionsMenu();		
	}

	private void setTextView() {
		totalTimeInfoView = (TextView) this.findViewById(R.id.this_total_time);
		totalDistanceInfoView = (TextView) this.findViewById(R.id.this_total_distance);
		totalDistanceUnitView  = (TextView) this.findViewById(R.id.this_total_distance_unit);		
		totalElevationGainInfoView = (TextView) this.findViewById(R.id.this_total_elevation_gain);
		totalElevationGainUnitView = (TextView) this.findViewById(R.id.this_total_elevation_gain_unit);
		numberOfHikesInfoView = (TextView) this.findViewById(R.id.this_number_of_hikes);
	}

	public static void setLogHeading() {
		boolean isMetric = Boolean.parseBoolean(MainActivity.main.getPref()
				.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, Defaults.DEFAULT_DISPLAY_UNIT));
		double distanceConversionFactor, heightConversionFactor;
		if(isMetric) {
			distanceConversionFactor = 1;
			heightConversionFactor = 1;
			totalDistanceUnitView.setText(" km");
			totalElevationGainUnitView.setText(" m");
		} else {
			distanceConversionFactor = Constants.KM_TO_MI;
			heightConversionFactor = Constants.METER_TO_FEET;
			totalDistanceUnitView.setText(" mi");
			totalElevationGainUnitView.setText(" ft");
		}
		ArrayList<Track> trackList = MainActivity.main.getTrackData().getTrackList();
		long totalTime = 0;
		double totalDistance = 0;
		double totalElevationGain = 0;
		for(Track track: trackList) {
			totalTime += track.getDuration();
			totalDistance += track.getDistance();
			totalElevationGain += track.getElevationGain();			
		}
		totalTimeInfoView.setText(formatters.formatTime(totalTime/60));
		totalDistanceInfoView.setText(formatters.formatDistance(totalDistance*distanceConversionFactor));
		totalElevationGainInfoView.setText(formatters.formatElevation(totalElevationGain*heightConversionFactor));
		numberOfHikesInfoView.setText(" " + trackList.size());
	}

	public static void notifyDataSetChanged() {
		adaptor.notifyDataSetChanged();
	}

	/**
	 * Check if a track is being edited and not available for operations
	 * @param trackName
	 * @return
	 */
	private boolean isEditingSelectedTrack() {		
		HashMap<GetElevationTask, Track> editTaskMap = MainActivity.main.getTrackData().getEditTaskMap();
		TrackData trackData = MainActivity.main.getTrackData();
		boolean isEditing = false;
		int i = 0;
		while(i<checkedTrackNames.size() && isEditing==false) {
			Track track = trackData.getTrack(checkedTrackNames.get(i));
			isEditing = editTaskMap.containsValue(track);
			i++;
		}

		if(isEditing==true) {
			new AlertDialog.Builder(this)
			.setTitle(R.string.edit_in_progress)
			.setMessage(R.string.track_editing_message)			
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {}					
			}).show();
			return true;
		} else
			return false;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equals(PrefKeys.KEY_PREF_DISPLAY_UNIT)) {
			setLogHeading();
		}
	}
}
