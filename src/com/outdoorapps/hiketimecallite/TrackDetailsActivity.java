package com.outdoorapps.hiketimecallite;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.outdoorapps.hiketimecallite.dialogs.ElevationRangeDialog;
import com.outdoorapps.hiketimecallite.dialogs.GoProDialog;
import com.outdoorapps.hiketimecallite.dialogs.InstantCheckDialog;
import com.outdoorapps.hiketimecallite.managers.ExportManager;
import com.outdoorapps.hiketimecallite.model.track.Track;
import com.outdoorapps.hiketimecallite.model.track.TrackPoint;
import com.outdoorapps.hiketimecallite.support.Formatters;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;
import com.outdoorapps.hiketimecallite.support.constants.Constants;
import com.outdoorapps.hiketimecallite.support.constants.Defaults;
import com.outdoorapps.hiketimecallite.support.constants.ExtraTypes;
import com.outdoorapps.hiketimecallite.support.constants.Links;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;
import com.outdoorapps.hiketimecallite.support.constants.RequestCode;

public class TrackDetailsActivity extends FragmentActivity implements 
OnSharedPreferenceChangeListener,
InstantCheckDialog.InstantCheckDialogListener,
ElevationRangeDialog.ElevationRangeDialogListener {

	private Track track;
	private Formatters formatters;
	private TextView trackNameView, dateView, startTimeView, endTimeView,
	durationView, distanceView, distanceUnitView,
	avgSpeedView, avgSpeedUnitView, maxElevationView, maxElevationUnitView,
	minElevationView, minElevationUnitView,
	elevationGainView, elevationGainUnitView, 
	elevationLossView, elevationLossUnitView;
	private ExportManager exportManager;
	private SharedPreferences prefs;
	private boolean isMetric;
	private ElevationRangeDialog elevationRangeDialog;
	public static final String CHANGE_TRACK_NAME_TAG = "change_track_name";
	public static final String ELEVATION_RANGE_TAG = "elevation_range";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_track_details);		
		Intent intent = getIntent();
		String trackName = intent.getStringExtra(ExtraTypes.TRACK_TO_BE_VIEWED);
		track = MainActivity.main.getTrackData().getTrack(trackName);
		track.getTPList(); // Make sure TPList and the related data has been created
		formatters = new Formatters();
		setTrackInfo();			
		addElevationProfile();
		addSpeedProfile();
		addAccuracyProfile();

		if(exportManager==null)
			exportManager = new ExportManager(MapObjectType.track);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.track_details, menu);
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

	private void setTrackInfo() {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);		
		isMetric = Boolean.parseBoolean(prefs.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, Defaults.DEFAULT_DISPLAY_UNIT));

		trackNameView = (TextView)findViewById(R.id.this_track_name);
		startTimeView = (TextView)findViewById(R.id.this_start_time);
		endTimeView = (TextView)findViewById(R.id.this_end_time);
		distanceView = (TextView)findViewById(R.id.this_distance);
		distanceUnitView = (TextView)findViewById(R.id.this_distance_unit);
		dateView = (TextView)findViewById(R.id.this_date);
		distanceView = (TextView)findViewById(R.id.this_distance);		
		durationView = (TextView)findViewById(R.id.this_duration);
		avgSpeedView = (TextView)findViewById(R.id.this_avg_speed);
		avgSpeedUnitView = (TextView)findViewById(R.id.this_avg_speed_unit);
		maxElevationView = (TextView)findViewById(R.id.this_max_elevation);
		maxElevationUnitView = (TextView)findViewById(R.id.this_max_elevation_unit);
		minElevationView = (TextView)findViewById(R.id.this_min_elevation);
		minElevationUnitView = (TextView)findViewById(R.id.this_min_elevation_unit);
		elevationGainView = (TextView)findViewById(R.id.this_elevation_gain);
		elevationGainUnitView = (TextView)findViewById(R.id.this_elevation_gain_unit);
		elevationLossView = (TextView)findViewById(R.id.this_elevation_loss);
		elevationLossUnitView = (TextView)findViewById(R.id.this_elevation_loss_unit);

		// set values
		double distanceConverionFactor, heightConversionFactor;		
		if(isMetric) {
			distanceConverionFactor = 1;
			heightConversionFactor = 1;
		} else {
			distanceConverionFactor = Constants.KM_TO_MI;
			heightConversionFactor = Constants.METER_TO_FEET;
		}
		setData(distanceConverionFactor, heightConversionFactor);
		setUnitUnchangeableValues();
	}

	private void addElevationProfile() {
		double heightConversionFactor, distanceConversionFactor;
		if(isMetric) {
			heightConversionFactor = 1;
			distanceConversionFactor = 1;
		}
		else {
			heightConversionFactor = Constants.METER_TO_FEET;
			distanceConversionFactor = Constants.KM_TO_MI;
		}
		
		ArrayList<TrackPoint> TPList = track.getTPList();
		GraphViewData[] dataList = new GraphViewData[TPList.size()];
		for(int i=0;i<TPList.size();i++) {
			TrackPoint tp = TPList.get(i);
			dataList[i] = new GraphViewData(tp.getCulminatedDistance()*distanceConversionFactor, tp.getLocation().getAltitude()*heightConversionFactor);
		}
		
		GraphViewSeries dataSeries = new GraphViewSeries(dataList);
		LineGraphView elevationGraph = new LineGraphView(this, "Elevation Profile");  
		elevationGraph.addSeries(dataSeries); 
		elevationGraph.setDrawBackground(true);
		elevationGraph.setScrollable(true);
		elevationGraph.setScalable(true);
		// Set ViewPort between start and end distance
		elevationGraph.setViewPort(0, TPList.get(TPList.size()-1).getCulminatedDistance()*distanceConversionFactor);
		LinearLayout layout = (LinearLayout) findViewById(R.id.elevation_profile);  
		layout.removeAllViews();
		layout.addView(elevationGraph);
	}

	private void addSpeedProfile() {
		double distanceConversionFactor;
		if(isMetric)
			distanceConversionFactor = 1;
		else
			distanceConversionFactor = Constants.KM_TO_MI;
		
		ArrayList<TrackPoint> TPList = track.getTPList();
		GraphViewData[] dataList = new GraphViewData[TPList.size()];
		for(int i=0;i<TPList.size();i++) {
			TrackPoint tp = TPList.get(i);
			dataList[i] = new GraphViewData(tp.getCulminatedDistance()*distanceConversionFactor, tp.getLocation().getSpeed()*distanceConversionFactor);
		}
		GraphViewSeries dataSeries = new GraphViewSeries(dataList);
		LineGraphView distanceGraph = new LineGraphView(this, "Speed Profile");  
		distanceGraph.addSeries(dataSeries); 
		distanceGraph.setDrawBackground(true);
		distanceGraph.setScrollable(true);
		distanceGraph.setScalable(true);
		// Set ViewPort between start and end distance
		distanceGraph.setViewPort(0, TPList.get(TPList.size()-1).getCulminatedDistance()*distanceConversionFactor);
		LinearLayout layout = (LinearLayout) findViewById(R.id.speed_profile);  
		layout.removeAllViews();
		layout.addView(distanceGraph); 
	}

	private void addAccuracyProfile() {
		double distanceConversionFactor;
		if(isMetric)
			distanceConversionFactor = 1;
		else
			distanceConversionFactor = Constants.KM_TO_MI;
		ArrayList<TrackPoint> TPList = track.getTPList();
		GraphViewData[] dataList = new GraphViewData[TPList.size()];
		for(int i=0;i<TPList.size();i++) {
			TrackPoint tp = TPList.get(i);
			dataList[i] = new GraphViewData(tp.getCulminatedDistance()*distanceConversionFactor, tp.getLocation().getAccuracy());
		}
		GraphViewSeries dataSeries = new GraphViewSeries(dataList);

		LineGraphView accuracyGraph = new LineGraphView(this, "Accuracy Profile");  
		accuracyGraph.addSeries(dataSeries); 
		accuracyGraph.setDrawBackground(true);
		accuracyGraph.setScrollable(true);
		accuracyGraph.setScalable(true);
		// Set ViewPort between start and end distance
		accuracyGraph.setViewPort(0, TPList.get(TPList.size()-1).getCulminatedDistance()*distanceConversionFactor);
		LinearLayout layout = (LinearLayout) findViewById(R.id.accuracy_profile);  
		layout.removeAllViews();
		layout.addView(accuracyGraph); 
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {		
		case R.id.show_on_map:
			intent = new Intent();
			ArrayList<String> trackToBeDrawn = new ArrayList<String>();
			trackToBeDrawn.add(track.getName());
			intent.putStringArrayListExtra(ExtraTypes.TRACK_TO_BE_DRAWN, trackToBeDrawn);
			this.setResult(RESULT_OK,intent);
			this.finish();
			break;
		case R.id.export_track:
			if(track.getTPList().size()<Defaults.EXPORT_LIMIT)
				exportManager.startExportOperation(track.getName(),track,this);
			else {
				GoProDialog goProDialog_export = new GoProDialog();
				goProDialog_export.setFunctionName("Save Track with more than 540 points (~1.5+ hrs)");
				goProDialog_export.show(getSupportFragmentManager(), "go_pro_track_export_limit");
			}
			break;
		case R.id.change_track_name:
			InstantCheckDialog instantCheckDialog = new InstantCheckDialog();
			instantCheckDialog.show(getSupportFragmentManager(), CHANGE_TRACK_NAME_TAG);
			instantCheckDialog.setTitle(getString(R.string.change_track_name));
			instantCheckDialog.setInitialName(track.getName());
			instantCheckDialog.setCompareList(MainActivity.main.getTrackData().getAllTrackName(),false);
			break;
		case R.id.update_elevations:
			new AlertDialog.Builder(this)
			.setTitle(R.string.update_elevations)
			.setMessage(R.string.update_elevations_message_warning)
			.setPositiveButton(R.string.all, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					boolean showAfter = MainActivity.main.getTrackData().getTracksOnMapList().contains(track.getName());
					MainActivity.main.getMapManager().hideTrackFromMap(track.getName());
					finish();
					Toast.makeText(MainActivity.main, "Updating Elevations", Toast.LENGTH_SHORT).show();
					MainActivity.main.getTrackData().updateElevations(track.getName(),true,0,0,showAfter);
				}
			})
			.setNeutralButton(R.string.part_dotted, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					elevationRangeDialog = new ElevationRangeDialog();
					elevationRangeDialog.show(getSupportFragmentManager(), ELEVATION_RANGE_TAG);
				}
			})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					// do nothing
				}
			}).show();
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
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// Export track
		if(resultCode==RESULT_OK && requestCode==RequestCode.EXPORT_GENERAL){
			exportManager.export(intent.getStringExtra(ExtraTypes.FILE_PATH),this);	
		}
	}

	private void setData(double distanceConverionFactor, double heightConversionFactor) {
		double distance = track.getDistance();
		double elevationGain = track.getElevationGain();
		double elevationLoss = track.getElevationLoss();
		double maxElevation = track.getMaxAltitude();
		double minElevation = track.getMinAltitude();
		float avgSpeed = track.getAvgSpeed();

		if(isMetric) {
			distanceUnitView.setText(" km");
			distanceView.setText(" km");
			elevationGainUnitView.setText(" m");
			elevationLossUnitView.setText(" m");
			maxElevationUnitView.setText(" m");
			minElevationUnitView.setText(" m");
			avgSpeedUnitView.setText(" km/h");
		} else {
			distanceUnitView.setText(" mi");
			distanceView.setText(" mi");
			elevationGainUnitView.setText(" ft");
			elevationLossUnitView.setText(" ft");
			maxElevationUnitView.setText(" ft");
			minElevationUnitView.setText(" ft");
			avgSpeedUnitView.setText(" mph");
		}

		distanceView.setText(formatters.formatDistance(distance*distanceConverionFactor));
		elevationGainView.setText(formatters.formatElevation(elevationGain*heightConversionFactor));		
		elevationLossView.setText(formatters.formatElevation(elevationLoss*heightConversionFactor));
		maxElevationView.setText(formatters.formatElevation(maxElevation*heightConversionFactor));
		minElevationView.setText(formatters.formatElevation(minElevation*heightConversionFactor));
		avgSpeedView.setText(formatters.formatSpeed(avgSpeed*distanceConverionFactor));
	}

	private void setUnitUnchangeableValues() {
		trackNameView.setText(track.getName());

		ArrayList<TrackPoint> TPList = track.getTPList();
		String start = TPList.get(0).getTime();
		String end = TPList.get(TPList.size()-1).getTime();

		String startDate = start.split("T")[0];
		String endDate = end.split("T")[0];
		String startTime = start.split("T")[1].replace("Z", "");
		String endTime = end.split("T")[1].replace("Z", "");

		if(startDate.equals(endDate))
			dateView.setText(startDate);
		else
			dateView.setText(startDate+ " to " +endDate);
		double durationInSecs = track.getDuration();
		durationView.setText(formatters.formatTime(durationInSecs/60));
		startTimeView.setText(startTime);
		endTimeView.setText(endTime);
	}


	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equals(PrefKeys.KEY_PREF_DISPLAY_UNIT)) {
			isMetric = Boolean.parseBoolean(prefs.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, Defaults.DEFAULT_DISPLAY_UNIT));

			double distanceConversionFactor, heightConversionFactor;
			if(isMetric) {
				distanceConversionFactor = 1;
				heightConversionFactor = 1;
			} else {
				distanceConversionFactor = Constants.KM_TO_MI;
				heightConversionFactor = Constants.METER_TO_FEET;
			}
			setData(distanceConversionFactor, heightConversionFactor);
			addElevationProfile();
			addSpeedProfile();
			addAccuracyProfile();
		}
	}

	/**
	 * Change Track Name
	 */
	@Override
	public void onDialogPositiveClick(InstantCheckDialog dialog) {
		if(dialog.getTag().equals(CHANGE_TRACK_NAME_TAG)) {
			String newTrackName = dialog.getName();		
			MainActivity.main.getTrackData().changeTrackName(track.getName(), newTrackName);
			track.setName(newTrackName);
			TextView trackNameView = (TextView) this.findViewById(R.id.this_track_name);
			trackNameView.setText(newTrackName);
			Intent intent = getIntent();
			intent.putExtra(ExtraTypes.TRACK_TO_BE_VIEWED,newTrackName);
		}
	}

	@Override
	public void onDialogNegativeClick(InstantCheckDialog dialog) {
		// do nothing
	}

	@Override
	public void onDialogPositiveClick(ElevationRangeDialog dialog) {
		if(dialog.getTag().equals(ELEVATION_RANGE_TAG)) {
			boolean showAfter = MainActivity.main.getTrackData().getTracksOnMapList().contains(track.getName());
			MainActivity.main.getMapManager().hideTrackFromMap(track.getName());
			double maxElevation = dialog.getMaxElevation();
			double minElevation = dialog.getMinElevation();
			finish();
			Toast.makeText(MainActivity.main, "Updating Elevations", Toast.LENGTH_SHORT).show();
			MainActivity.main.getTrackData().updateElevations(track.getName(),false,maxElevation,minElevation,showAfter);
		}
	}
}
