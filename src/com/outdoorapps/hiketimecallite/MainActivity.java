package com.outdoorapps.hiketimecallite;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.outdoorapps.hiketimecallite.asynctasks.ShowOnMapTask;
import com.outdoorapps.hiketimecallite.database.TrackDatabaseOpenHelper;
import com.outdoorapps.hiketimecallite.dialogs.EULADialog;
import com.outdoorapps.hiketimecallite.dialogs.EULADialog.EULADialogListener;
import com.outdoorapps.hiketimecallite.dialogs.GoProDialog;
import com.outdoorapps.hiketimecallite.dialogs.InstantCheckDialog;
import com.outdoorapps.hiketimecallite.dialogs.MapLayerDialog;
import com.outdoorapps.hiketimecallite.files.RouteParser;
import com.outdoorapps.hiketimecallite.managers.ButtonsManager;
import com.outdoorapps.hiketimecallite.managers.DrawingMode;
import com.outdoorapps.hiketimecallite.managers.ExportManager;
import com.outdoorapps.hiketimecallite.managers.ImportManager;
import com.outdoorapps.hiketimecallite.managers.MapManager;
import com.outdoorapps.hiketimecallite.managers.MarkerInfo;
import com.outdoorapps.hiketimecallite.managers.TrackRecoveryManager;
import com.outdoorapps.hiketimecallite.model.route.Calculator.InvalidParametersException;
import com.outdoorapps.hiketimecallite.model.route.Route;
import com.outdoorapps.hiketimecallite.model.route.RouteData;
import com.outdoorapps.hiketimecallite.model.route.RoutePoint;
import com.outdoorapps.hiketimecallite.model.track.Track;
import com.outdoorapps.hiketimecallite.model.track.TrackData;
import com.outdoorapps.hiketimecallite.model.track.Tracker;
import com.outdoorapps.hiketimecallite.model.track.TrackingInfo;
import com.outdoorapps.hiketimecallite.model.track.TrackingPoint;
import com.outdoorapps.hiketimecallite.support.LatLngSupport;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;
import com.outdoorapps.hiketimecallite.support.SplitActionBarStyle;
import com.outdoorapps.hiketimecallite.support.Storage;
import com.outdoorapps.hiketimecallite.support.Time;
import com.outdoorapps.hiketimecallite.support.Verifier;
import com.outdoorapps.hiketimecallite.support.constants.Defaults;
import com.outdoorapps.hiketimecallite.support.constants.ExtraTypes;
import com.outdoorapps.hiketimecallite.support.constants.Links;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;
import com.outdoorapps.hiketimecallite.support.constants.RequestCode;

/**
 * Provide functions to all map and main interface related activities
 * @author chbting
 *
 */
public class MainActivity extends FragmentActivity implements
OnNavigationListener,
ConnectionCallbacks,
OnConnectionFailedListener,
LocationListener,
OnMyLocationButtonClickListener,
OnSharedPreferenceChangeListener,
EULADialogListener,
InstantCheckDialog.InstantCheckDialogListener {

	/** 
	 * Static variables are used to save themselves through every thing except device restarts
	 * Non static variables should be recreated every time onResume or onCreate is called
	 */
	public static MainActivity main;
	private static int actionBarHeight;
	public static final String[] dropdownMenuItems = {"Map","Routes","Log","Calculator","Checklist"};
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;
	private ButtonsManager buttonManager;

	private static RouteData routeData;
	private static String mapObjectToBeUnhidden;
	private static TrackData trackData;

	private GoogleMap mMap;
	private LocationClient mLocationClient;
	private static CameraPosition cp; // static to reduce SharedPreferences read
	private static LatLngBounds latLngBounds;
	private MenuItem trackButton, stopTrackingButton;
	private static MapManager mapManager;	
	private static Boolean tracking;
	private static Tracker tracker;
	private static ImportManager importManager;
	private static ExportManager exportManager;
	private static ShowOnMapTask showOnMapTask;

	private MarkerInfo markerInfo;
	private static TrackingInfo trackingInfo;
	private TextView routeNameInfoView, distanceInfoView, returnDistanceInfoView,
	elevationInfoView, timeInfoView, returnTimeInfoView, editInfoView, drawingInfoView;;
	private String editingRouteName;	
	private static DrawingMode drawingMode;
	private boolean isDrawing, isEditing;
	private static TrackRecoveryManager trackRecoveryManager;

	private boolean doubleBackToExitPressedOnce;
	public static final String TRACKING_NAME_TAG = "Tracking_name";
	public static final String ADD_DRAWN_ROUTE_TAG = "add_drawn_route";

	// These settings are the same as the settings for the map. They will in fact give you updates
	// at the maximal rates currently possible.
	private static final LocationRequest REQUEST = LocationRequest.create()
			.setInterval(5000)         // 5 seconds
			.setFastestInterval(16)    // 16ms = 60fps
			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 1. Set up app environment
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		editor = prefs.edit();
		main = this;
		Storage.createAppFoldersIfNeeded();

		// Restore routeList and routeOnMapList	
		if(routeData==null)
			routeData = new RouteData(this);
		if(trackData==null)
			trackData = new TrackData(this);
		if(mapManager==null)
			mapManager = new MapManager(this);

		if(importManager==null)
			importManager = new ImportManager(null);
		if(exportManager==null)
			exportManager = new ExportManager(null);

		if(trackRecoveryManager==null)
			trackRecoveryManager = new TrackRecoveryManager();

		// 2. Set up interface
		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);		
		SplitActionBarStyle.setSplitActionBarOverlayColor(actionBar);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
				// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(getActionBarThemedContextCompat(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, dropdownMenuItems), this);

		// restore camera position
		if(cp==null) {
			Double cp_lat = Double.parseDouble(prefs.getString(PrefKeys.KEY_CP_LAT, "0"));
			Double cp_lon = Double.parseDouble(prefs.getString(PrefKeys.KEY_CP_LON, "0"));
			Float cp_zoom = prefs.getFloat(PrefKeys.KEY_CP_ZOOM, 0);
			Float cp_tilt = prefs.getFloat(PrefKeys.KEY_CP_TILT, 0);
			Float cp_bearing = prefs.getFloat(PrefKeys.KEY_CP_BEARING, 0);
			cp = new CameraPosition(new LatLng(cp_lat,cp_lon), cp_zoom, cp_tilt, cp_bearing);
		}

		TypedValue tv = new TypedValue();
		if(getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
			actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
		setMyLocationButtonPosition();

		// Set up info views on top right
		editInfoView = (TextView) this.findViewById(R.id.edit_info);
		drawingInfoView = (TextView) this.findViewById(R.id.drawing_info);
		routeNameInfoView = (TextView) this.findViewById(R.id.route_name_info);
		distanceInfoView = (TextView) this.findViewById(R.id.distance_info);
		returnDistanceInfoView = (TextView) this.findViewById(R.id.return_distance_info);
		elevationInfoView = (TextView) this.findViewById(R.id.elevation_info);
		timeInfoView = (TextView) this.findViewById(R.id.time_info);
		returnTimeInfoView = (TextView) this.findViewById(R.id.return_time_info);

		String colorCode = prefs.getString(PrefKeys.KEY_PREF_INFO_TEXT_COLOR, Defaults.DEFAULT_INFO_TEXT_COLOR);
		routeNameInfoView.setTextColor(Color.parseColor(colorCode));
		distanceInfoView.setTextColor(Color.parseColor(colorCode));
		returnDistanceInfoView.setTextColor(Color.parseColor(colorCode));
		elevationInfoView.setTextColor(Color.parseColor(colorCode));
		timeInfoView.setTextColor(Color.parseColor(colorCode));
		returnTimeInfoView.setTextColor(Color.parseColor(colorCode));
		editInfoView.setTextColor(Color.parseColor(colorCode));
		drawingInfoView.setTextColor(Color.parseColor(colorCode));

		// Set up tracking items
		if(tracking==null)
			tracking = false; // tracking is false by default
		if(trackingInfo==null)
			trackingInfo = new TrackingInfo(this);			

		markerInfo = new MarkerInfo(this,prefs);
		setMarkerInfo(markerInfo);		

		mapObjectToBeUnhidden = "";
		// Set up double back button press for exit
		doubleBackToExitPressedOnce = false;

		// If starting the first time
		boolean eulaArgeed = prefs.getBoolean(PrefKeys.KEY_EULA_AGREED, false);
		if(eulaArgeed==false) {
			// pop EULA dialog
			DialogFragment eulaDialog = new EULADialog();
			eulaDialog.setCancelable(false);
			eulaDialog.show(getSupportFragmentManager(), "eula_dialog");
		}

		// Copy sample files to destination		
		boolean sampleCopied = prefs.getBoolean(PrefKeys.KEY_SAMPLES_COPIED, false);
		if(sampleCopied==false) {
			editor.putBoolean(PrefKeys.KEY_SAMPLES_COPIED, true);
			editor.commit();			
			try {
				Storage.copySampleFiles();
			} catch(Exception e) {
				Toast.makeText(this, "Error: Sample files copy unsuccessful," +
						" samples files can be obtained on developer's website", Toast.LENGTH_LONG).show();
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		isDrawing = prefs.getBoolean(PrefKeys.KEY_IS_DRAWING, false);
		isEditing = prefs.getBoolean(PrefKeys.KEY_IS_EDITING, false);
		editingRouteName = prefs.getString(PrefKeys.KEY_EDITING_ROUTE_NAME, "");
		if(mMap==null) {
			setUpMap();
			if(isDrawing)// TODO: move mMap code to setUpMap
				recoverDrawingSession();
			else {
				editInfoView.setText("");
				drawingInfoView.setText("");
				mMap.setOnMarkerClickListener(new OnMarkerClickListener());
				mMap.setOnMapClickListener(new OnMapNotDrawingClickListener());
			}

			// draw every route added on map 
			mapManager.drawAllRouteOnMap();//should be the only time drawAllRouteOnMap() is called
			mapManager.drawAllTrackOnMap();
		}
		// restore camera position if moveCameraToRoute was not called
		if(showOnMapTask!=null)
			showOnMapTask.showProgressDialogIfNeeded(this);
		if(latLngBounds==null)
			mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cp));
		else{
			Display display = getWindowManager().getDefaultDisplay();
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds,display.getWidth(),display.getHeight(),100);
			mMap.animateCamera(cameraUpdate);
			latLngBounds = null;
		}

		if(tracking==true) { // TODO: Avoid java to clear statics if tracking in background
			tracker.setPauseUpdate(false);
			trackingInfo.updateTextViews(this);
			TrackingPoint trackingPT = tracker.getLastTrackingPoint();
			if(trackingPT!=null)
				trackingInfo.updateTrackingInfo(trackingPT.getLocation());
			trackingInfo.resumeClock();
		} else {
			boolean saveTrackDialogOpen = prefs.getBoolean(PrefKeys.KEY_SAVE_TRACK_DIALOG_OPEN, false);
			if(saveTrackDialogOpen)
				openSaveTrackDialog();
			else{
				boolean discardTrackDialogOpen = prefs.getBoolean(PrefKeys.KEY_DISCARD_TRACK_DIALOG_OPEN, false);
				if(discardTrackDialogOpen)
					openDiscardTrackDialog();
				else {
					// Check if track recovery necessary
					TrackDatabaseOpenHelper trackerDBOpenHelper = new TrackDatabaseOpenHelper(this);
					if(trackerDBOpenHelper.checkUnsavedData()==true) {
						trackRecoveryManager.recoverTrack();
					}
				}
			}			
		}

		// Open instant name check dialog if importing
		if(importManager.hasJob())
			importManager.showNameDialog(this);

		setUpLocationClientIfNeeded();
		if(mLocationClient.isConnected()==false)
			mLocationClient.connect();
		checkSampleRouteAdded();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mLocationClient != null && tracking==false) {
			mLocationClient.disconnect();
		}
		if(tracking==true)
			tracker.setPauseUpdate(true);
		// save edit mode info
		editor.putString(PrefKeys.KEY_EDITING_ROUTE_NAME, editingRouteName);
		editor.putBoolean(PrefKeys.KEY_IS_EDITING, isEditing);
		editor.commit();

		saveDrawingSession();
		saveCameraPosition();
		editor.putInt(PrefKeys.KEY_MAP_TYPE, mMap.getMapType());

		// save MarkerInfo
		markerInfo.saveToSharedPreferences(editor);

		if(showOnMapTask!=null)
			showOnMapTask.dismissProgressDialogIfNeeded();
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

	private void saveCameraPosition() {
		cp = mMap.getCameraPosition();
		editor.putString(PrefKeys.KEY_CP_LAT, cp.target.latitude+"");
		editor.putString(PrefKeys.KEY_CP_LON, cp.target.longitude+"");
		editor.putFloat(PrefKeys.KEY_CP_ZOOM, cp.zoom);
		editor.putFloat(PrefKeys.KEY_CP_TILT, cp.tilt);
		editor.putFloat(PrefKeys.KEY_CP_BEARING, cp.bearing);
		editor.commit();
	}

	private void setUpMap() {
		// Try to obtain the map from the SupportMapFragment.
		mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
				.getMap();
		// Check if we were successful in obtaining the map.
		if (mMap != null) { // TODO : sometimes map is not successfully obtained
			mapManager.setGoogleMap(mMap);
			mMap.setMyLocationEnabled(true);
			mMap.setOnMyLocationButtonClickListener(this);
			mMap.setMapType(prefs.getInt(PrefKeys.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_TERRAIN));
			mMap.getUiSettings().setRotateGesturesEnabled(true);
			mMap.getUiSettings().setCompassEnabled(true);
			mMap.setPadding(0,actionBarHeight,0,actionBarHeight);
		}
	}

	private void enableDrawing() {
		if(isDrawing==false) { // avoid re-enabling
			deselectMarker();
			mMap.setOnMarkerClickListener(null);
			mMap.setOnMapClickListener(new OnMapClickListener());
			mMap.setOnMapLongClickListener(new OnMapLongClickListener());			
			drawingMode = new DrawingMode(this);
			isDrawing = true;
			if(isEditing==false)
				drawingInfoView.setText("Drawing new route");
			buttonManager.setDefaultButtonSetVisible(false);
			buttonManager.setMapObjectSelectButtonSetVisible(false);
			buttonManager.setDrawingButtonSetVisible(true);
		}
	}

	private void disableDrawing() {
		if(isDrawing==true) {  // avoid re-disabling		
			mapManager.removeDrawingModePolyline();
			mMap.setOnMapClickListener(null);
			mMap.setOnMapLongClickListener(null);
			mMap.setOnMarkerClickListener(new OnMarkerClickListener());
			mMap.setOnMapClickListener(new OnMapNotDrawingClickListener());
			drawingMode = null;
			isDrawing = false;
			drawingInfoView.setText("");
			buttonManager.setDefaultButtonSetVisible(true);
			buttonManager.setMapObjectSelectButtonSetVisible(false);
			buttonManager.setDrawingButtonSetVisible(false);
		}
	}

	private void saveDrawingSession() {
		editor.putBoolean(PrefKeys.KEY_IS_DRAWING, isDrawing);		
		editor.commit();
		if(drawingMode!=null) // make sure drawingMode is not null
			drawingMode.saveDrawingSession();
	}

	/**
	 * Recover previous drawing session
	 */
	private void recoverDrawingSession() {
		// = edited version of enable drawing
		mMap.setOnMarkerClickListener(null);
		mMap.setOnMapClickListener(new OnMapClickListener());
		mMap.setOnMapLongClickListener(new OnMapLongClickListener());			
		drawingMode = new DrawingMode(this);
		drawingMode.recoverDrawingSession();
		if(isEditing==true)
			editInfoView.setText("Editing " + editingRouteName);
		else
			drawingInfoView.setText("Drawing new route");
		mapManager.drawDrawingModePolyline();
	}

	private void enableEditMode(String routeName) {
		if(isEditing==false) {			
			isEditing = true;
			editingRouteName = routeName;					
			editInfoView.setText("Editing " + routeName);
			drawingInfoView.setText("");
			mapManager.hideRouteFromMap(routeName);
			enableDrawing();
			Route route = routeData.getRoute(routeName);
			drawingMode.setPointList(route.getLatLngList());
			drawingMode.setPointCount(route.getLatLngList().size());
			mapManager.drawDrawingModePolyline();

			// move camera to see the whole route
			latLngBounds = LatLngSupport.getLatLngBounds(route.getLatLngList());
		}
	}

	private void disableEditMode() {
		if(isEditing==true) {
			isEditing = false;
			editingRouteName = "";
			editInfoView.setText("");
			disableDrawing();	
		}
	}

	public void editRoute(String routeName) {
		enableEditMode(routeName);
		// prepare info to be continued on onResume 
		editor.putString(PrefKeys.KEY_EDITING_ROUTE_NAME, routeName);
		editor.putBoolean(PrefKeys.KEY_IS_EDITING, true);			
		editor.commit();
		saveDrawingSession();
		saveCameraPosition();
		buttonManager.setDefaultButtonSetVisible(false);
		buttonManager.setMapObjectSelectButtonSetVisible(false);
		buttonManager.setDrawingButtonSetVisible(true); // ok because option menu has been created
	}

	public void showRoutes(ArrayList<String> checkedRouteNames) {
		ArrayList<LatLng> latLngBoundList = new ArrayList<LatLng>();
		for(int i=0; i<checkedRouteNames.size(); i++) {
			String routeName = checkedRouteNames.get(i);
			Route route = routeData.getRoute(routeName);
			mapManager.addRouteOnMap(routeName);
			LatLngBounds bound = route.getLatLngBounds();
			latLngBoundList.add(bound.northeast);
			latLngBoundList.add(bound.southwest);
		}
		// Center camera to the route(s)
		latLngBounds = LatLngSupport.getLatLngBounds(latLngBoundList);
	}

	public void showTracks(ArrayList<String> checkedTrackNames) {
		ArrayList<LatLng> latLngBoundList = new ArrayList<LatLng>();
		for(int i=0; i<checkedTrackNames.size(); i++) {
			String trackName = checkedTrackNames.get(i);
			Track track = trackData.getTrack(trackName);
			mapManager.addTrackOnMap(trackName);
			LatLngBounds bound = LatLngSupport.getLatLngBounds(track.getLatLngList());
			latLngBoundList.add(bound.northeast);
			latLngBoundList.add(bound.southwest);
		}
		// Center camera to the route(s)
		latLngBounds = LatLngSupport.getLatLngBounds(latLngBoundList);
		// animate cameraUpdate onResume
	}

	/**
	 * Note: onResume is called after onActivityResult
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// Reset dropdown menu to "Map" when coming back from activities
		if(requestCode==RequestCode.SELECT_ROUTE 
				||requestCode==RequestCode.SELECT_TRACK
				|| requestCode==RequestCode.SIMPLE_CALCULATOR
				|| requestCode==RequestCode.CHECKLIST)
			getActionBar().setSelectedNavigationItem(0);

		// Import Route / track
		if(resultCode==RESULT_OK && requestCode==RequestCode.IMPORT_GENERAL) {
			importManager.setImportPath(intent.getStringExtra(ExtraTypes.FILE_PATH));
			// Open dialog in onResume
		}

		// Export route / track
		if(resultCode==RESULT_OK && requestCode==RequestCode.EXPORT_GENERAL){
			exportManager.export(intent.getStringExtra(ExtraTypes.FILE_PATH),this);	
		}
	}

	@Override
	public boolean onNavigationItemSelected(final int position, long id) {
		if(isDrawing && position==1) {
			if(drawingMode.getPointList().size()>0) { // Ask on exiting drawing session if a point has been added
				new AlertDialog.Builder(this)
				.setTitle(R.string.exit_drawing)
				.setMessage(R.string.exit_drawing_message)				
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						if(isEditing) {
							String editedRouteName = editingRouteName;
							disableEditMode();
							mapManager.addRouteOnMap(editedRouteName); // add route back to map
						}
						else
							disableDrawing();
						menuItemEvents(position);
					}
				})
				.setNegativeButton(R.string.go_back, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						main.getActionBar().setSelectedNavigationItem(0);
					}
				}).show();
				return true;

			} else {
				if(isEditing) {
					String editedRouteName = editingRouteName;
					disableEditMode();
					mapManager.addRouteOnMap(editedRouteName); // add route back to map
				} else
					disableDrawing();
				menuItemEvents(position);
				return true;
			}
		} else
			return menuItemEvents(position);
	}

	private boolean menuItemEvents(int position) {
		Intent intent;
		switch(position) {		
		case 0: // Map
			break;
		case 1: // Select Route 
			intent = new Intent(this,SelectRouteActivity.class);
			startActivityForResult(intent, RequestCode.SELECT_ROUTE);
			break;
		case 2: // Select Track
			intent = new Intent(this,SelectTrackActivity.class);
			startActivityForResult(intent, RequestCode.SELECT_TRACK);
			break;
		case 3: // simple calculator
			intent = new Intent(this,SimpleCalculatorActivity.class);
			startActivityForResult(intent, RequestCode.SIMPLE_CALCULATOR);
			break;
		case 4: // checklist
			intent = new Intent(this,ChecklistActivity.class);
			startActivityForResult(intent, RequestCode.CHECKLIST);
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		Intent intent;
		switch (item.getItemId()) {
		case R.id.track:
			startTracking();			
			break;
		case R.id.stop_tracking:
			new AlertDialog.Builder(this)
			.setTitle(R.string.stop_tracking)
			.setMessage(R.string.stop_tracking_message)			
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					stopTracking();
				}
			})
			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					// do nothing
				}
			}).show();					
			break;
		case R.id.unhide:
			if(!mapObjectToBeUnhidden.equals("")) {
				Route route = routeData.getRoute(mapObjectToBeUnhidden);
				if(route!=null)
					mapManager.addRouteOnMap(mapObjectToBeUnhidden);
				else {
					Track track = trackData.getTrack(mapObjectToBeUnhidden);
					if(track!=null)
						mapManager.addTrackOnMap(mapObjectToBeUnhidden);
				}
			}
			break;
		case R.id.draw:
			enableDrawing();
			break;
		case R.id.import_route:
			importManager.openDualOptionsDialog(this);
			break;
		case R.id.undo:
			drawingMode.undo();
			mapManager.removeDrawingModePolyline();
			mapManager.drawDrawingModePolyline();
			break;
		case R.id.redo:
			drawingMode.redo();
			mapManager.removeDrawingModePolyline();
			mapManager.drawDrawingModePolyline();
			break;
		case R.id.reverse:
			drawingMode.reverse();
			mapManager.removeDrawingModePolyline();
			mapManager.drawDrawingModePolyline();
			break;
		case R.id.discard:
			askIfExitingDrawingMode();
			break;
		case R.id.done:
			int pointCount = drawingMode.getPointCount();
			if(pointCount<2)
				Toast.makeText(this, "Error: The route has less than 2 points", Toast.LENGTH_SHORT).show();
			else {
				// finalize pointList
				ArrayList<LatLng> latLngList = drawingMode.finalizePointList();
				if(isEditing) {
					String routeName = editingRouteName;
					disableEditMode();
					routeData.modifyRouteRPList(routeName, latLngList);
				}
				else {
					// diable Drawing after the dialog
					InstantCheckDialog instantCheckDialog = new InstantCheckDialog();
					instantCheckDialog.setCancelable(false);
					instantCheckDialog.show(getSupportFragmentManager(), ADD_DRAWN_ROUTE_TAG);
					instantCheckDialog.setTitle(getString(R.string.choose_route_name));
					instantCheckDialog.setInitialName("New Drawn Route");
					instantCheckDialog.setCompareList(routeData.getAllRouteName(),false);
				}
			}
			break;
			// Route selected on map
		case R.id.show_details:
			if(markerInfo.getType()==MapObjectType.route) {
				Intent viewDetailsIntent = new Intent(this,RouteDetailsActivity.class);
				viewDetailsIntent.putExtra(ExtraTypes.ROUTE_TO_BE_VIEWED,markerInfo.getName());
				startActivityForResult(viewDetailsIntent, RequestCode.ROUTE_DETAILS);
			} else if(markerInfo.getType()==MapObjectType.track) {
				GoProDialog goProDialog_export = new GoProDialog();
				goProDialog_export.setFunctionName("View track details and update/correct elevations");
				goProDialog_export.show(getSupportFragmentManager(), "go_pro_track_details");
			}
			break;
		case R.id.hide_from_map:
			if(markerInfo.getType()==MapObjectType.route) {
				mapObjectToBeUnhidden = markerInfo.getName();
				mapManager.hideRouteFromMap(markerInfo.getName());
				deselectMarker();
			} else if(markerInfo.getType()==MapObjectType.track) {
				mapObjectToBeUnhidden = markerInfo.getName();
				mapManager.hideTrackFromMap(markerInfo.getName());
				deselectMarker();
			} 
			break;
		case R.id.export_route:
			if(markerInfo.getType()==MapObjectType.route) {
				// TODO: Route exporter
				if(Verifier.verifyProVersion(this)==true) {
					Toast.makeText(this, "Pro version installation verified", Toast.LENGTH_SHORT).show();
					Route route = routeData.getRoute(markerInfo.getName());
					exportManager.setType(MapObjectType.route);
					exportManager.startExportOperation(markerInfo.getName(),route,this);
				}
				else {
					GoProDialog goProDialog_export = new GoProDialog();
					goProDialog_export.setFunctionName("Export Route");
					goProDialog_export.show(getSupportFragmentManager(), "go_pro_export");
				}
			} else if(markerInfo.getType()==MapObjectType.track) {
				Track track = trackData.getTrack(markerInfo.getName());
				exportManager.setType(MapObjectType.track);
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
		case R.id.edit_route:
			GoProDialog goProDialog_edit = new GoProDialog();
			goProDialog_edit.setFunctionName("Edit Route");
			goProDialog_edit.show(getSupportFragmentManager(), "go_pro_edit");
			break;
		case R.id.remove_route:
			if(markerInfo.getType()==MapObjectType.route)
				removeRouteDialog();
			else
				if(markerInfo.getType()==MapObjectType.track)
					removeTrackDialog();
			break;
			// hidden items
		case R.id.select_map_layer:
			DialogFragment mapLayerDialog = new MapLayerDialog();
			mapLayerDialog.show(getSupportFragmentManager(), "map_layer");
			break;
		case R.id.help:
			Uri uri = Uri.parse(Links.HELP_LINK); 
			startActivity(new Intent(Intent.ACTION_VIEW, uri));
			break;
		case R.id.action_settings:
			intent = new Intent(this,SettingsActivity.class);
			startActivity(intent);
			break;
		default:
			buttonManager.checkAllButtonSets();
			return super.onOptionsItemSelected(item);
		}
		buttonManager.checkAllButtonSets();
		return true;
	}

	private void startTracking() {
		trackButton.setVisible(false);
		stopTrackingButton.setVisible(true);
		tracking = true;
		tracker = new Tracker();
		tracker.start(mapManager);
		trackingInfo.initializeTrackingInfo(this);
	}

	private void stopTracking() {
		trackButton.setVisible(true);
		stopTrackingButton.setVisible(false);
		tracking = false;
		tracker.stop(mapManager);
		trackingInfo.hideTrackingInfo();

		ArrayList<TrackingPoint> trackingPTList = Tracker.loadFromDatabase();
		if(trackingPTList.size()>0) { // Size checking
			openSaveTrackDialog();			
		}
	}

	private void openSaveTrackDialog() {
		InstantCheckDialog instantCheckDialog = new InstantCheckDialog();
		instantCheckDialog.setCancelable(false);
		instantCheckDialog.setPositiveButtonName(getString(R.string.save));
		instantCheckDialog.setNegativeButtonName(getString(R.string.discard));
		instantCheckDialog.show(getSupportFragmentManager(), TRACKING_NAME_TAG);
		instantCheckDialog.setTitle(getString(R.string.choose_track_name));
		instantCheckDialog.setCompareList(trackData.getAllTrackName(),false);
		// For screen rotation
		editor.putBoolean(PrefKeys.KEY_SAVE_TRACK_DIALOG_OPEN, true);
		editor.commit();	
	}

	private void openDiscardTrackDialog() {
		// For screen rotation
		editor.putBoolean(PrefKeys.KEY_DISCARD_TRACK_DIALOG_OPEN, true);
		editor.commit();
		new AlertDialog.Builder(this)
		.setTitle(R.string.discard_track)
		.setMessage(R.string.discard_track_message)
		.setCancelable(false)
		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				editor.putBoolean(PrefKeys.KEY_DISCARD_TRACK_DIALOG_OPEN, false);
				editor.commit();
				Tracker.clearDatabase();
			}
		})
		.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				editor.putBoolean(PrefKeys.KEY_DISCARD_TRACK_DIALOG_OPEN, false);
				editor.commit();
				openSaveTrackDialog();
			}
		}).show();
	}

	@Override
	public void onDialogPositiveClick(InstantCheckDialog dialog) {
		if(dialog.getTag().equals(TRACKING_NAME_TAG)) {
			editor.putBoolean(PrefKeys.KEY_SAVE_TRACK_DIALOG_OPEN, false);
			editor.commit();
			Track track = new Track(dialog.getName(),Track.convertToTPList(Tracker.loadFromDatabase()));			
			trackData.addTrack(track, true);
			Tracker.clearDatabase();
		}
		if(dialog.getTag().equals(ADD_DRAWN_ROUTE_TAG)) {
			ArrayList<RoutePoint> RPList = new ArrayList<RoutePoint>();
			ArrayList<LatLng> latLngList = drawingMode.getPointList();
			for(int i=0;i<latLngList.size();i++)
				RPList.add(new RoutePoint(latLngList.get(i),0));			
			disableDrawing();
			try {
				Route route = new Route(dialog.getName(),RPList,Defaults.getDefaultParametersArray(prefs),true);
				routeData.addNewRoute(route, true);
			} catch (InvalidParametersException e) {
				Toast.makeText(this, "Invalid parameters for drawn route", Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onDialogNegativeClick(InstantCheckDialog dialog) {
		if(dialog.getTag().equals(ADD_DRAWN_ROUTE_TAG)) {
			disableDrawing();
		}
		if(dialog.getTag().equals(TRACKING_NAME_TAG)) {
			editor.putBoolean(PrefKeys.KEY_SAVE_TRACK_DIALOG_OPEN, false);
			editor.commit();
			openDiscardTrackDialog();
		}
	}

	private void removeRouteDialog() {
		new AlertDialog.Builder(this)		
		.setTitle(R.string.remove_route)
		.setMessage(R.string.remove_route_message)
		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				routeData.removeRoute(markerInfo.getName());
				deselectMarker();
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				// do nothing
			}
		}).show();
	}

	private void removeTrackDialog() {
		new AlertDialog.Builder(this)
		.setTitle(R.string.remove_track)
		.setMessage(R.string.remove_track_message)		
		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				trackData.removeTrack(markerInfo.getName());
				deselectMarker();
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				// do nothing
			}
		}).show();
	}

	@Override
	public void onBackPressed() {		
		if (doubleBackToExitPressedOnce) {
			super.onBackPressed();
			return;
		}
		this.doubleBackToExitPressedOnce = true;
		Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				doubleBackToExitPressedOnce=false; 
			}
		}, 2000);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		trackButton = menu.findItem(R.id.track);
		stopTrackingButton = menu.findItem(R.id.stop_tracking);

		HashMap<String,MenuItem> buttonsMap = new HashMap<String,MenuItem>();

		buttonsMap.put(ButtonsManager.UNDO_BUTTON, menu.findItem(R.id.undo));
		buttonsMap.put(ButtonsManager.REDO_BUTTON, menu.findItem(R.id.redo));
		buttonsMap.put(ButtonsManager.DONE_BUTTON, menu.findItem(R.id.done));
		buttonsMap.put(ButtonsManager.REVERSE_BUTTON, menu.findItem(R.id.reverse));
		buttonsMap.put(ButtonsManager.DISCARD_BUTTON, menu.findItem(R.id.discard));

		buttonsMap.put(ButtonsManager.TRACK_BUTTON, menu.findItem(R.id.track));
		buttonsMap.put(ButtonsManager.STOP_TRACKING_BUTTON, menu.findItem(R.id.stop_tracking));
		buttonsMap.put(ButtonsManager.UNHIDE_BUTTON, menu.findItem(R.id.unhide));
		buttonsMap.put(ButtonsManager.DRAW_BUTTON, menu.findItem(R.id.draw));
		buttonsMap.put(ButtonsManager.IMPORT_BUTTON, menu.findItem(R.id.import_route));

		buttonsMap.put(ButtonsManager.SHOW_DETAILS_BUTTON, menu.findItem(R.id.show_details));
		buttonsMap.put(ButtonsManager.HIDE_FROM_MAP_BUTTON, menu.findItem(R.id.hide_from_map));
		buttonsMap.put(ButtonsManager.EDIT_ROUTE_BUTTON, menu.findItem(R.id.edit_route));
		buttonsMap.put(ButtonsManager.EXPORT_ROUTE_BUTTON, menu.findItem(R.id.export_route));
		buttonsMap.put(ButtonsManager.REMOVE_ROUTE_BUTTON, menu.findItem(R.id.remove_route));

		buttonsMap.put(ButtonsManager.MAP_LAYER_BUTTON, menu.findItem(R.id.select_map_layer));

		buttonManager = new ButtonsManager(this,buttonsMap);
		buttonManager.checkAllButtonSets();
		return super.onCreateOptionsMenu(menu);
	}

	private class OnMapClickListener implements GoogleMap.OnMapClickListener {
		@Override		
		public void onMapClick(LatLng point) {
			drawingMode.clickEventResponse(point);
			mapManager.removeDrawingModePolyline();
			mapManager.drawDrawingModePolyline(); // all must be redraw to avoid new and old polylines overlay			
			buttonManager.changeDrawingButtonSetState();
		}
	}

	private class OnMapLongClickListener implements GoogleMap.OnMapLongClickListener {
		@Override		
		public void onMapLongClick(LatLng point) {
			drawingMode.clickEventResponse(point);
			mapManager.removeDrawingModePolyline();
			mapManager.drawDrawingModePolyline(); // all must be redraw to avoid new and old polylines overlay
			buttonManager.changeDrawingButtonSetState();
		}
	}

	private class OnMarkerClickListener implements GoogleMap.OnMarkerClickListener {
		@Override
		public boolean onMarkerClick(Marker marker) {
			markerInfo = routeData.getMarkerMap().get(marker);
			if(markerInfo==null)
				markerInfo = trackData.getMarkerMap().get(marker);
			setMarkerInfo(markerInfo);
			buttonManager.setDefaultButtonSetVisible(false);
			buttonManager.setMapObjectSelectButtonSetVisible(true);
			return false;
		}	
	}

	private class OnMapNotDrawingClickListener implements GoogleMap.OnMapClickListener {
		@Override		
		public void onMapClick(LatLng point) {
			deselectMarker();
		}
	}

	public void deselectMarker() {
		markerInfo = new MarkerInfo(this);
		setMarkerInfo(markerInfo);
		markerInfo.saveToSharedPreferences(editor);
		buttonManager.checkAllButtonSets();
		mMap.setOnMarkerClickListener(new OnMarkerClickListener());
		mMap.setOnMapClickListener(new OnMapNotDrawingClickListener());
	}

	public void updateMarkerInfo() {
		setMarkerInfo(markerInfo);
	}

	private void setMarkerInfo(MarkerInfo markerInfo) {
		if(markerInfo!=null) {			
			String[] markerInfoArray = markerInfo.getInfoString(prefs);
			routeNameInfoView.setText(markerInfoArray[0]);
			distanceInfoView.setText(markerInfoArray[1]);
			returnDistanceInfoView.setText(markerInfoArray[2]);
			elevationInfoView.setText(markerInfoArray[3]);
			timeInfoView.setText(markerInfoArray[4]);
			returnTimeInfoView.setText(markerInfoArray[5]);
		}
		else {
			routeNameInfoView.setText("");
			distanceInfoView.setText("");
			returnDistanceInfoView.setText("");
			elevationInfoView.setText("");
			timeInfoView.setText("");
			returnTimeInfoView.setText("");
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equals(PrefKeys.KEY_PREF_DISPLAY_UNIT)) {
			setMarkerInfo(markerInfo);
			boolean isMetric = Boolean.parseBoolean(sharedPreferences.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, Defaults.DEFAULT_DISPLAY_UNIT));
			trackingInfo.changeUnit(isMetric);
		}
		if(key.equals(PrefKeys.KEY_PREF_INFO_TEXT_COLOR)) {
			// Get color code from colorName
			String colorCode = prefs.getString(PrefKeys.KEY_PREF_INFO_TEXT_COLOR, Defaults.DEFAULT_INFO_TEXT_COLOR);
			routeNameInfoView.setTextColor(Color.parseColor(colorCode));
			distanceInfoView.setTextColor(Color.parseColor(colorCode));
			returnDistanceInfoView.setTextColor(Color.parseColor(colorCode));
			elevationInfoView.setTextColor(Color.parseColor(colorCode));
			timeInfoView.setTextColor(Color.parseColor(colorCode));
			returnTimeInfoView.setTextColor(Color.parseColor(colorCode));
			editInfoView.setTextColor(Color.parseColor(colorCode));
			drawingInfoView.setTextColor(Color.parseColor(colorCode));
			trackingInfo.changeTextColor(colorCode);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());		
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	/**
	 * Backward-compatible version of {@link ActionBar#getThemedContext()} that
	 * simply returns the {@link android.app.Activity} if
	 * <code>getThemedContext</code> is unavailable.
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private Context getActionBarThemedContextCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return getActionBar().getThemedContext();
		} else {
			return this;
		}
	}

	private void setUpLocationClientIfNeeded() {
		if (mLocationClient == null) {
			mLocationClient = new LocationClient(
					getApplicationContext(),
					this,  // ConnectionCallbacks
					this); // OnConnectionFailedListener
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		if(tracking) {
			tracker.setLastTrackingPoint(new TrackingPoint(location,Time.getCurrentTime()));		
			if(tracker.getPauseUpdate()==false)
				trackingInfo.updateTrackingInfo(location);
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// Do nothing
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		mLocationClient.requestLocationUpdates(REQUEST,this);  // LocationListener
	}

	@Override
	public void onDisconnected() {
		// Do nothing
	}

	@Override
	public boolean onMyLocationButtonClick() {
		Toast.makeText(this, "Go to current location", Toast.LENGTH_SHORT).show();
		// Return false so that we don't consume the event and the default behavior still occurs
		// (the camera animates to the user's current position).
		return false;
	}

	public SharedPreferences getPref() {
		return prefs;
	}

	public SharedPreferences.Editor getEditor() {
		return editor;
	}

	public RouteData getRouteData() {
		return routeData;
	}

	public TrackData getTrackData() {
		return trackData;
	}

	public Tracker getTracker() {
		return tracker;
	}

	public DrawingMode getDrawingMode() {
		return drawingMode;
	}

	public boolean isDrawing() {
		return isDrawing;
	}

	public GoogleMap getGoogleMap() {
		return mMap;
	}

	public MapManager getMapManager() {
		return mapManager;
	}

	public MarkerInfo getMarkerInfo() {
		return markerInfo;
	}

	public Boolean getTracking() {
		return tracking;
	}

	/**
	 * Move the MyLocation button above zoom buttons
	 */
	private void setMyLocationButtonPosition() {
		//  below the action bar
		View myLocationButton = findViewById(R.id.main).findViewById(2); 
		View zoomButton = findViewById(R.id.main).findViewById(1); 

		int actionBarHeight = 0;
		TypedValue tv = new TypedValue();
		if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)){
			actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
		}

		if (myLocationButton != null){			
			ViewGroup.MarginLayoutParams marginParams1 = new ViewGroup.MarginLayoutParams(zoomButton.getLayoutParams());
			marginParams1.setMargins(10, 0, 0, actionBarHeight);
			RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(marginParams1);
			layoutParams1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			layoutParams1.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			zoomButton.setLayoutParams(layoutParams1);

			ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(myLocationButton.getLayoutParams());
			marginParams.setMargins(0, 0, 0, 0);
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(marginParams);
			layoutParams.addRule(RelativeLayout.ABOVE, zoomButton.getId());
			layoutParams.addRule(RelativeLayout.ALIGN_LEFT, zoomButton.getId());
			myLocationButton.setLayoutParams(layoutParams);
		}
	}

	private void askIfExitingDrawingMode() {
		if(drawingMode.getPointList().size()>0) { // Ask on exiting drawing session if a point has been added
			new AlertDialog.Builder(this)
			.setMessage(R.string.exit_drawing_message)
			.setTitle(R.string.exit_drawing)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					if(isEditing) {
						String editedRouteName = editingRouteName;
						disableEditMode();
						mapManager.addRouteOnMap(editedRouteName); // add route back to map
					}
					else
						disableDrawing();
				}
			})
			.setNegativeButton(R.string.go_back, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
				}
			}).show();
		} else {
			if(isEditing) {
				String editedRouteName = editingRouteName;
				disableEditMode();
				mapManager.addRouteOnMap(editedRouteName); // add route back to map
			} else
				disableDrawing();
		}
	}

	@Override
	public void onDialogPositiveClick(EULADialog dialog) {
		editor.putBoolean(PrefKeys.KEY_EULA_AGREED, true);
		editor.commit();
	}

	@Override
	public void onDialogNegativeClick(EULADialog dialog) {
		this.finish();
		Process.killProcess(Process.myPid());
	}

	private void checkSampleRouteAdded() {
		boolean sampleLoaded = prefs.getBoolean(PrefKeys.KEY_SAMPLE_ROUTE_ADDED, false);
		if(sampleLoaded==false) {
			editor.putBoolean(PrefKeys.KEY_SAMPLE_ROUTE_ADDED, true);
			editor.commit();
			try {
				RouteParser parser = new RouteParser();
				Route route = parser.Parse("Big Baldy Hike (Sample)",new File(Storage.SAMPLE_ROUTE_PATH), true);
				routeData.addNewRoute(route, false); // Not to show sample route on map by default
			} catch(Exception e) {
				Toast.makeText(this, "Error: Sample route adding unsuccessful", Toast.LENGTH_LONG).show();
			}
		}
	}
}
