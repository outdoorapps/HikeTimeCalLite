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
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.outdoorapps.hiketimecallite.adaptors.SelectRouteRowAdapter;
import com.outdoorapps.hiketimecallite.asynctasks.GetElevationTask;
import com.outdoorapps.hiketimecallite.dialogs.GoProDialog;
import com.outdoorapps.hiketimecallite.managers.ExportManager;
import com.outdoorapps.hiketimecallite.managers.ImportManager;
import com.outdoorapps.hiketimecallite.managers.MarkerInfo;
import com.outdoorapps.hiketimecallite.model.route.Route;
import com.outdoorapps.hiketimecallite.model.route.RouteData;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;
import com.outdoorapps.hiketimecallite.support.SplitActionBarStyle;
import com.outdoorapps.hiketimecallite.support.Verifier;
import com.outdoorapps.hiketimecallite.support.constants.ExtraTypes;
import com.outdoorapps.hiketimecallite.support.constants.Links;
import com.outdoorapps.hiketimecallite.support.constants.RequestCode;

public class SelectRouteActivity extends FragmentActivity implements 
ActionBar.OnNavigationListener,
OnItemClickListener { 

	public static Activity thisActivity;
	private ArrayList<String> checkedRouteNames;
	private static SelectRouteRowAdapter adaptor; // static to allow static notifyDataSetChanged	
	private static ImportManager importManager;
	private static ExportManager exportManager;
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_route);

		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
				// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(getActionBarThemedContextCompat(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, MainActivity.dropdownMenuItems), this);
		actionBar.setSelectedNavigationItem(1);		
		setupActionBar();

		thisActivity = this;
		checkedRouteNames = MainActivity.main.getRouteData().getCheckedRouteNames();
		// List Items
		adaptor = new SelectRouteRowAdapter(this,MainActivity.main.getRouteData().getRouteList(),checkedRouteNames);
		ListView list =(ListView)findViewById(android.R.id.list);
		list.setAdapter(adaptor);
		list.setOnItemClickListener(this);

		if(importManager==null)
			importManager = new ImportManager(MapObjectType.route);
		if(exportManager==null)
			exportManager = new ExportManager(MapObjectType.route);
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
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.select_route, menu);
		return true;
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		Intent intent;
		switch(position) {		
		case 0: // Map
			finish();
			break;
		case 1: // Select Route 
			break;
		case 2: // Select Track
			intent = new Intent(this,SelectTrackActivity.class);
			finish();
			startActivityForResult(intent, RequestCode.SELECT_TRACK);
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
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {		
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);		
			return true;
		case R.id.show_details:
			if(isEditingSelectedRoute()==false) {
				Intent viewDetailsIntent = new Intent(this,RouteDetailsActivity.class);
				viewDetailsIntent.putExtra(ExtraTypes.ROUTE_TO_BE_VIEWED,checkedRouteNames.get(0));
				startActivityForResult(viewDetailsIntent, RequestCode.ROUTE_DETAILS);
			}
			break;
		case R.id.show_on_map:
			if(isEditingSelectedRoute()==false) {
				finish();
				MainActivity.main.showRoutes(checkedRouteNames);
			}
			break;
		case R.id.hide_from_map:
			if(isEditingSelectedRoute()==false) {
				for(int i=0;i<checkedRouteNames.size();i++)
					MainActivity.main.getMapManager().hideRouteFromMap(checkedRouteNames.get(i));
				deselectAll();
				Toast.makeText(this, "Selected routes are hidden", Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.import_route:
			importManager.startImportOperation(this);
			break;
		case R.id.export_route:
			if(Verifier.verifyProVersion(this)==true) {
				Toast.makeText(this, "Pro version installation verified", Toast.LENGTH_SHORT).show();
				Route route = MainActivity.main.getRouteData().getRoute(checkedRouteNames.get(0));
				exportManager.startExportOperation(checkedRouteNames.get(0),route,this);
			} else {
				GoProDialog goProDialog_export = new GoProDialog();
				goProDialog_export.setFunctionName("Export Route");
				goProDialog_export.show(getSupportFragmentManager(), "go_pro_export");
			}
			break;
		case R.id.edit_route:
			GoProDialog goProDialog_edit = new GoProDialog();
			goProDialog_edit.setFunctionName("Edit Route");
			goProDialog_edit.show(getSupportFragmentManager(), "go_pro_edit");
			break;
		case R.id.remove_route:
			if(isEditingSelectedRoute()==false) {
				new AlertDialog.Builder(this)
				.setMessage(R.string.remove_route_message)
				.setTitle(R.string.remove_route)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						ArrayList<String> checkedRouteNamesTemp = new ArrayList<String>();
						checkedRouteNamesTemp.addAll(checkedRouteNames);
						for(int i=0; i<checkedRouteNamesTemp.size(); i++) {
							String routeName = checkedRouteNamesTemp.get(i);
							if(MainActivity.main.getRouteData().removeRoute(routeName)) {	
								// Deselect route if necessary
								MarkerInfo markerInfo = MainActivity.main.getMarkerInfo();
								if(markerInfo!=null)
									if(markerInfo.getName().equals(routeName))
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
		// Import route
		if(resultCode==RESULT_OK && requestCode==RequestCode.IMPORT_GENERAL) {
			importManager.setImportPath(intent.getStringExtra(ExtraTypes.FILE_PATH));
		}
		// Export route
		if(resultCode==RESULT_OK && requestCode==RequestCode.EXPORT_GENERAL){
			exportManager.export(intent.getStringExtra(ExtraTypes.FILE_PATH),this);	
		}

		if(resultCode==RESULT_OK && requestCode==RequestCode.ROUTE_DETAILS) {
			if(intent.hasExtra(ExtraTypes.ROUTE_TO_BE_DRAWN)) {
				finish();
				MainActivity.main.showRoutes(checkedRouteNames);
			}
			if(intent.hasExtra(ExtraTypes.ROUTE_TO_BE_EDITED)) {
				finish();
				MainActivity.main.editRoute(checkedRouteNames.get(0));
			}
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem showDetailsButton = menu.findItem(R.id.show_details);
		MenuItem showOnMapButton = menu.findItem(R.id.show_on_map);
		MenuItem hideFromMapButton = menu.findItem(R.id.hide_from_map);
		MenuItem editRouteButton = menu.findItem(R.id.edit_route);
		MenuItem importRouteButton = menu.findItem(R.id.import_route);
		MenuItem exportRouteButton = menu.findItem(R.id.export_route);		
		MenuItem removeRouteButton = menu.findItem(R.id.remove_route);

		Drawable showDetailsIcon = getResources().getDrawable(R.drawable.ic_details);
		Drawable showOnMapIcon = getResources().getDrawable(R.drawable.ic_show_on_map);
		Drawable hideFromMapIcon = getResources().getDrawable(R.drawable.ic_hide);
		Drawable editRouteIcon = getResources().getDrawable(R.drawable.ic_edit);
		Drawable importRouteIcon = getResources().getDrawable(R.drawable.ic_import);
		Drawable exportRouteIcon = getResources().getDrawable(R.drawable.ic_export);		
		Drawable removeRouteIcon = getResources().getDrawable(R.drawable.ic_remove);

		if(checkedRouteNames.size()==0) {
			showDetailsButton.setEnabled(false);
			showOnMapButton.setEnabled(false);
			hideFromMapButton.setEnabled(false);
			editRouteButton.setEnabled(false);
			importRouteButton.setEnabled(true);
			exportRouteButton.setEnabled(false);		
			removeRouteButton.setEnabled(false);		
		} else {
			if(checkedRouteNames.size()==1) {
				showDetailsButton.setEnabled(true);
				showOnMapButton.setEnabled(true);
				hideFromMapButton.setEnabled(true);
				editRouteButton.setEnabled(true);
				importRouteButton.setEnabled(false);
				exportRouteButton.setEnabled(true);
				removeRouteButton.setEnabled(true);
			} else {
				// if multiple routes are selected
				showDetailsButton.setEnabled(false);
				showOnMapButton.setEnabled(true);
				hideFromMapButton.setEnabled(true);	
				editRouteButton.setEnabled(false);
				importRouteButton.setEnabled(false);
				exportRouteButton.setEnabled(false);
				removeRouteButton.setEnabled(true);
			}
		}

		// Gray out disabled buttons
		if(showDetailsButton.isEnabled()==false)
			showDetailsIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
		if(showOnMapButton.isEnabled()==false)
			showOnMapIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
		if(hideFromMapButton.isEnabled()==false)
			hideFromMapIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
		if(editRouteButton.isEnabled()==false)
			editRouteIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
		if(importRouteButton.isEnabled()==false)
			importRouteIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
		if(exportRouteButton.isEnabled()==false)
			exportRouteIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);		
		if(removeRouteButton.isEnabled()==false)
			removeRouteIcon.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);		

		showDetailsButton.setIcon(showDetailsIcon);
		showOnMapButton.setIcon(showOnMapIcon);
		hideFromMapButton.setIcon(hideFromMapIcon);
		editRouteButton.setIcon(editRouteIcon);
		importRouteButton.setIcon(importRouteIcon);
		exportRouteButton.setIcon(exportRouteIcon);		
		removeRouteButton.setIcon(removeRouteIcon);
		return true;		
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
		CheckBox checkBox = (CheckBox) v.findViewById(R.id.checkBox);
		checkBox.toggle();
		if(checkBox.isChecked())
			checkedRouteNames.add(MainActivity.main.getRouteData().getRouteList().get(position).getName());
		else {
			if(checkBox.isChecked()==false)
				checkedRouteNames.remove(MainActivity.main.getRouteData().getRouteList().get(position).getName());
		}
		// Enable menu items based on the number of selected routes
		invalidateOptionsMenu();
	}

	public void selectAll() {
		ListView list =(ListView)findViewById(android.R.id.list);
		for(int i=0; i < list.getChildCount(); i++){
			ViewGroup item = (ViewGroup)list.getChildAt(i);
			CheckBox checkbox = (CheckBox)item.findViewById(R.id.checkBox);
			checkbox.setChecked(true);
			checkedRouteNames.add(MainActivity.main.getRouteData().getRouteList().get(i).getName());
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
		checkedRouteNames.clear();
		invalidateOptionsMenu();		
	}

	public static void notifyDataSetChanged() {
		adaptor.notifyDataSetChanged();
	}

	/**
	 * Check if a route is being edited and not available for operations
	 * @param routeName
	 * @return
	 */
	private boolean isEditingSelectedRoute() {		
		HashMap<GetElevationTask, Route> editTaskMap = MainActivity.main.getRouteData().getEditTaskMap();
		RouteData routeData = MainActivity.main.getRouteData();
		boolean isEditing = false;
		int i = 0;
		while(i<checkedRouteNames.size() && isEditing==false) {
			Route route = routeData.getRoute(checkedRouteNames.get(i));
			isEditing = editTaskMap.containsValue(route);
			i++;
		}

		if(isEditing==true) {
			new AlertDialog.Builder(this)
			.setTitle(R.string.edit_in_progress)
			.setMessage(R.string.route_editing_message)			
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {}					
			}).show();
			return true;
		} else
			return false;
	}

}
