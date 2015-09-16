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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.outdoorapps.hiketimecallite.dialogs.GoProDialog;
import com.outdoorapps.hiketimecallite.dialogs.InstantCheckDialog;
import com.outdoorapps.hiketimecallite.managers.ExportManager;
import com.outdoorapps.hiketimecallite.model.route.Calculator.InvalidParametersException;
import com.outdoorapps.hiketimecallite.model.route.Route;
import com.outdoorapps.hiketimecallite.model.route.RoutePoint;
import com.outdoorapps.hiketimecallite.support.Formatters;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;
import com.outdoorapps.hiketimecallite.support.Parameters;
import com.outdoorapps.hiketimecallite.support.constants.Constants;
import com.outdoorapps.hiketimecallite.support.constants.Defaults;
import com.outdoorapps.hiketimecallite.support.constants.ExtraTypes;
import com.outdoorapps.hiketimecallite.support.constants.Links;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;
import com.outdoorapps.hiketimecallite.support.constants.RequestCode;

public class RouteDetailsActivity extends FragmentActivity implements 
OnEditorActionListener,
OnCheckedChangeListener,
OnSharedPreferenceChangeListener,
InstantCheckDialog.InstantCheckDialogListener {

	private Route route;
	private Formatters formatters;
	private TextView routeNameView, distanceView, distanceUnitView,
	distanceRoundTripView, distanceRoundTripUnitView, tripTimeView,
	returnTripTimeView, totalTimeView, elevationGainView, elevationGainUnitView, 
	elevationLossView, elevationLossUnitView, speed_unit, height_unit1, height_unit2;
	private EditText speedEdit, ascendTimeIncreaseEdit, ascendHeightInputEdit, 
	descendTimeIncreaseEdit, descendHeightInputEdit;
	private CheckBox useDefaultParametersCheckBox;
	private LinearLayout buttonLayout;
	private Button editButton, saveButton, discardButton;
	private SharedPreferences prefs;
	private boolean isMetric;
	public static final String CHANGE_ROUTE_NAME_TAG = "chang_route_name";
	private static ExportManager exportManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_route_details);		
		Intent intent = getIntent();
		String routeName = intent.getStringExtra(ExtraTypes.ROUTE_TO_BE_VIEWED);
		route = MainActivity.main.getRouteData().getRoute(routeName);
		route.getRPList(); // Make sure RPList and the related data has been created
		formatters = new Formatters();
		setRouteInfo();			
		addElevationProfile();

		useDefaultParametersCheckBox = (CheckBox) findViewById(R.id.default_parameters_checkBox);		
		useDefaultParametersCheckBox.setChecked(false); // Use default parameters by default
		useDefaultParametersCheckBox.setOnCheckedChangeListener(this);
		useDefaultParametersCheckBox.setEnabled(false);

		buttonLayout = (LinearLayout) findViewById(R.id.button_layout);
		editButton = (Button) findViewById(R.id.edit_parameters_button);
		saveButton = (Button) findViewById(R.id.save_button);
		discardButton = (Button) findViewById(R.id.discard_button);
		saveButton.setVisibility(View.INVISIBLE);
		discardButton.setVisibility(View.INVISIBLE);
		if(exportManager==null)
			exportManager = new ExportManager(MapObjectType.route);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.route_details, menu);
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

	private void setRouteInfo() {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);		
		isMetric = Boolean.parseBoolean(prefs.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, Defaults.DEFAULT_DISPLAY_UNIT));

		routeNameView = (TextView)findViewById(R.id.this_route_name);
		distanceView = (TextView)findViewById(R.id.this_distance);
		distanceUnitView = (TextView)findViewById(R.id.this_distance_unit);
		distanceRoundTripView = (TextView)findViewById(R.id.this_distance_round_trip);
		distanceRoundTripUnitView = (TextView)findViewById(R.id.this_distance_round_trip_unit);		
		tripTimeView = (TextView)findViewById(R.id.this_trip_time);
		returnTripTimeView = (TextView)findViewById(R.id.this_return_trip_time);
		totalTimeView = (TextView)findViewById(R.id.this_total_time);
		elevationGainView = (TextView)findViewById(R.id.this_elevation_gain);
		elevationGainUnitView = (TextView)findViewById(R.id.this_elevation_gain_unit);
		elevationLossView = (TextView)findViewById(R.id.this_elevation_loss);
		elevationLossUnitView = (TextView)findViewById(R.id.this_elevation_loss_unit);
		speed_unit = (TextView)findViewById(R.id.speed_unit);

		// parameters details
		speedEdit = (EditText) this.findViewById(R.id.speed_input);
		ascendTimeIncreaseEdit = (EditText) this.findViewById(R.id.ascendTimeIncrease);
		ascendHeightInputEdit = (EditText) this.findViewById(R.id.ascendHeightInput);
		descendTimeIncreaseEdit = (EditText) this.findViewById(R.id.descendTimeIncrease);
		descendHeightInputEdit = (EditText) this.findViewById(R.id.descentHeightInput);
		descendHeightInputEdit.setOnEditorActionListener(this);
		height_unit1 = (TextView)findViewById(R.id.height_unit1);
		height_unit2 = (TextView)findViewById(R.id.height_unit2);

		speedEdit.setEnabled(false);
		ascendTimeIncreaseEdit.setEnabled(false);
		ascendHeightInputEdit.setEnabled(false);
		descendTimeIncreaseEdit.setEnabled(false);
		descendHeightInputEdit.setEnabled(false);

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
		initializeParameters();
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

		ArrayList<RoutePoint> RPList = route.getRPList();
		GraphViewData[] dataList = new GraphViewData[RPList.size()];
		for(int i=0;i<RPList.size();i++) {
			RoutePoint rp = RPList.get(i);
			dataList[i] = new GraphViewData(rp.getCulminativeDistance()*distanceConversionFactor, rp.getElevation()*heightConversionFactor);
		}
		GraphViewSeries dataSeries = new GraphViewSeries(dataList);

		LineGraphView oneWayGraph = new LineGraphView(this, "Elevation Profile");  
		oneWayGraph.addSeries(dataSeries); 
		oneWayGraph.setDrawBackground(true);
		oneWayGraph.setScrollable(true);
		oneWayGraph.setScalable(true);
		// Set ViewPort between start and end distance
		oneWayGraph.setViewPort(0, RPList.get(RPList.size()-1).getCulminativeDistance()*distanceConversionFactor);
		LinearLayout layout = (LinearLayout) findViewById(R.id.elevation_profile);
		layout.removeAllViews();
		layout.addView(oneWayGraph); 
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// Export route
		if(resultCode==RESULT_OK && requestCode==RequestCode.EXPORT_GENERAL){
			exportManager.export(intent.getStringExtra(ExtraTypes.FILE_PATH),this);	
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {		
		case R.id.show_on_map:
			intent = new Intent();
			ArrayList<String> routeToBeDrawn = new ArrayList<String>();
			routeToBeDrawn.add(route.getName());
			intent.putStringArrayListExtra(ExtraTypes.ROUTE_TO_BE_DRAWN, routeToBeDrawn);
			this.setResult(RESULT_OK,intent);
			this.finish();
			break;
		case R.id.export_route:
			GoProDialog goProDialog_export = new GoProDialog();
			goProDialog_export.setFunctionName("Export Route");
			goProDialog_export.show(getSupportFragmentManager(), "go_pro_export");
			break;
		case R.id.edit_route:
			GoProDialog goProDialog_edit = new GoProDialog();
			goProDialog_edit.setFunctionName("Edit Route");
			goProDialog_edit.show(getSupportFragmentManager(), "go_pro_edit");
			break;
		case R.id.change_route_name:			
			InstantCheckDialog instantCheckDialog = new InstantCheckDialog();
			instantCheckDialog.show(getSupportFragmentManager(), CHANGE_ROUTE_NAME_TAG);
			instantCheckDialog.setTitle(getString(R.string.change_route_name));
			instantCheckDialog.setInitialName(route.getName());
			instantCheckDialog.setCompareList(MainActivity.main.getRouteData().getAllRouteName(),false);
			break;
		case R.id.update_elevations:
			final boolean showAfter = MainActivity.main.getRouteData().getRoutesOnMapList().contains(route.getName());
			new AlertDialog.Builder(this)
			.setTitle(R.string.update_elevations)
			.setMessage(R.string.update_elevations_message)
			.setPositiveButton(R.string.all, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					MainActivity.main.getMapManager().hideRouteFromMap(route.getName());
					finish();
					MainActivity.main.getRouteData().updateElevations(route.getName(), true, showAfter);					
				}
			})
			.setNeutralButton(R.string.part,  new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					MainActivity.main.getMapManager().hideRouteFromMap(route.getName());
					finish();
					MainActivity.main.getRouteData().updateElevations(route.getName(), false, showAfter);					
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

	private void setData(double distanceConverionFactor, double heightConversionFactor) {
		double distance = route.getRouteDistance();
		double distanceRoundTrip = route.getRouteDistance()*2;
		double elevationGain = route.getRouteElevationGain();
		double elevationLoss = route.getRouteElevationLoss();

		if(isMetric) {
			distanceUnitView.setText(" km");
			distanceRoundTripUnitView.setText(" km");
			elevationGainUnitView.setText(" m");
			elevationLossUnitView.setText(" m");
		} else {
			distanceUnitView.setText(" mi");
			distanceRoundTripUnitView.setText(" mi");
			elevationGainUnitView.setText(" ft");
			elevationLossUnitView.setText(" ft");
		}

		distanceView.setText(formatters.formatDistance(distance*distanceConverionFactor));								
		distanceRoundTripView.setText(formatters.formatDistance(distanceRoundTrip*distanceConverionFactor));
		elevationGainView.setText(formatters.formatElevation(elevationGain*heightConversionFactor));		
		elevationLossView.setText(formatters.formatElevation(elevationLoss*heightConversionFactor));
	}

	/**
	 * Change display parameters by current input on editTexts
	 */
	private void setParameters() {
		double distanceConversionFactor, heightConversionFactor;		
		if(isMetric) {
			speed_unit.setText("km/h");
			height_unit1.setText("m");
			height_unit2.setText("m");
			distanceConversionFactor = Constants.MI_TO_KM;
			heightConversionFactor = Constants.FEET_TO_METER;
		} else {
			speed_unit.setText("mph");
			height_unit1.setText("ft");
			height_unit2.setText("ft");		
			distanceConversionFactor = Constants.KM_TO_MI;
			heightConversionFactor = Constants.METER_TO_FEET;
		}

		double speedNum = Double.parseDouble(speedEdit.getText().toString());
		double ascendTimeIncreaseNum = Double.parseDouble(ascendTimeIncreaseEdit.getText().toString());
		double ascendHeightInputNum = Double.parseDouble(ascendHeightInputEdit.getText().toString());
		double descendTimeIncreaseNum = Double.parseDouble(descendTimeIncreaseEdit.getText().toString());
		double descendHeightInputNum = Double.parseDouble(descendHeightInputEdit.getText().toString());

		String speed = formatters.formatSpeed(speedNum*distanceConversionFactor);
		String ascendTimeIncrease = formatters.formatOneDecimalPT(ascendTimeIncreaseNum);
		String ascendHeightInput = formatters.formatElevation(ascendHeightInputNum*heightConversionFactor);
		String descendTimeIncrease = formatters.formatOneDecimalPT(descendTimeIncreaseNum);
		String descendHeightInput = formatters.formatElevation(descendHeightInputNum*heightConversionFactor);

		speedEdit.setText(speed);
		ascendTimeIncreaseEdit.setText(ascendTimeIncrease);
		ascendHeightInputEdit.setText(ascendHeightInput);
		descendTimeIncreaseEdit.setText(descendTimeIncrease);
		descendHeightInputEdit.setText(descendHeightInput);
	}

	/**
	 * initialize parameters from the current route
	 */
	private void initializeParameters() {
		double distanceConversionFactor, heightConversionFactor;
		if(isMetric==false) {
			distanceConversionFactor = Constants.KM_TO_MI;
			heightConversionFactor = Constants.METER_TO_FEET;
		} else {
			distanceConversionFactor = 1;
			heightConversionFactor = 1;
		}

		String speed = formatters.formatSpeed(route.getSpeedInput()*distanceConversionFactor);
		String ascendTimeIncrease = formatters.formatOneDecimalPT(route.getAscendTimeIncrease());
		String ascendHeightInput = formatters.formatElevation(route.getAscendHeightInput()*heightConversionFactor);
		String descendTimeIncrease = formatters.formatOneDecimalPT(route.getDescendTimeIncrease());
		String descendHeightInput = formatters.formatElevation(route.getDescendHeightInput()*heightConversionFactor);

		speedEdit.setText(speed);
		ascendTimeIncreaseEdit.setText(ascendTimeIncrease);
		ascendHeightInputEdit.setText(ascendHeightInput);
		descendTimeIncreaseEdit.setText(descendTimeIncrease);
		descendHeightInputEdit.setText(descendHeightInput);
	}

	/**
	 * Set to default parameters in settings
	 */
	private void setDefaultParameters() {
		// set factors
		double distanceConversionFactor, heightConversionFactor;
		if(isMetric==false) {
			distanceConversionFactor = Constants.KM_TO_MI;
			heightConversionFactor = Constants.METER_TO_FEET;
		} else {
			distanceConversionFactor = 1;
			heightConversionFactor = 1;
		}

		double[] defaultParametersList = new double[5];
		defaultParametersList[0] = Double.parseDouble(prefs.getString(PrefKeys.SPEED_KEY, Defaults.DEFAULT_SPEED+""));
		defaultParametersList[1] = Double.parseDouble(prefs.getString(PrefKeys.ASCEND_TIME_INCREASE_KEY, Defaults.DEFAULT_ASCEND_TIME_INCREASE+""));
		defaultParametersList[2] = Double.parseDouble(prefs.getString(PrefKeys.ASCEND_HEIGHT_KEY, Defaults.DEFAULT_ASCEND_HEIGHT+""));
		defaultParametersList[3] = Double.parseDouble(prefs.getString(PrefKeys.DESCEND_TIME_INCREASE_KEY, Defaults.DEFAULT_DESCEND_TIME_INCREASE+""));
		defaultParametersList[4] = Double.parseDouble(prefs.getString(PrefKeys.DESCEND_HEIGHT_KEY, Defaults.DEFAULT_DESCEND_HEIGHT+""));

		String speed = formatters.formatSpeed(defaultParametersList[0]*distanceConversionFactor);
		String ascendTimeIncrease = formatters.formatOneDecimalPT(defaultParametersList[1]);
		String ascendHeightInput = formatters.formatElevation(defaultParametersList[2]*heightConversionFactor);
		String descendTimeIncrease = formatters.formatOneDecimalPT(defaultParametersList[3]);
		String descendHeightInput = formatters.formatElevation(defaultParametersList[4]*heightConversionFactor);

		speedEdit.setText(speed);
		ascendTimeIncreaseEdit.setText(ascendTimeIncrease);
		ascendHeightInputEdit.setText(ascendHeightInput);
		descendTimeIncreaseEdit.setText(descendTimeIncrease);
		descendHeightInputEdit.setText(descendHeightInput);
	}

	private void setUnitUnchangeableValues() {
		routeNameView.setText(route.getName());

		double tripTime = route.getTripTime();
		tripTimeView.setText(formatters.formatTime(tripTime));

		double returnTripTime = route.getReturnTripTime();
		returnTripTimeView.setText(formatters.formatTime(returnTripTime));

		double totalTime = route.getRoundTripTime();
		totalTimeView.setText(formatters.formatTime(totalTime));
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
			setParameters();
			addElevationProfile();
		}

		if((key.equals(PrefKeys.SPEED_KEY) || key.equals(PrefKeys.ASCEND_TIME_INCREASE_KEY) 
				|| key.equals(PrefKeys.ASCEND_HEIGHT_KEY) || key.equals(PrefKeys.DESCEND_TIME_INCREASE_KEY)
				|| key.equals(PrefKeys.DESCEND_HEIGHT_KEY))
				&& useDefaultParametersCheckBox.isChecked()) {
			setDefaultParameters();
		}

	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_DONE) {
			parametersCheck();
			return true;
		}
		return false;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if(isChecked) {
			speedEdit.setEnabled(false);
			ascendTimeIncreaseEdit.setEnabled(false);
			ascendHeightInputEdit.setEnabled(false);
			descendTimeIncreaseEdit.setEnabled(false);
			descendHeightInputEdit.setEnabled(false);
			setDefaultParameters();
		} else {
			speedEdit.setEnabled(true);
			ascendTimeIncreaseEdit.setEnabled(true);
			ascendHeightInputEdit.setEnabled(true);
			descendTimeIncreaseEdit.setEnabled(true);
			descendHeightInputEdit.setEnabled(true);
		}	
	}

	public void editParameters(View view) {
		enableParametersEdit();
	}

	public void saveParametersChanges(View view) {
		parametersCheck();
	}

	public void discardParametersChanges(View view) {				
		// restore original parameters		
		initializeParameters();
		disableParametersEdit();
	}

	private void enableParametersEdit() {
		editButton.setVisibility(View.INVISIBLE);
		saveButton.setVisibility(View.VISIBLE);
		discardButton.setVisibility(View.VISIBLE);
		buttonLayout.setVisibility(View.VISIBLE);
		useDefaultParametersCheckBox.setEnabled(true);
		useDefaultParametersCheckBox.setChecked(false);
		speedEdit.setEnabled(true);
		ascendTimeIncreaseEdit.setEnabled(true);
		ascendHeightInputEdit.setEnabled(true);
		descendTimeIncreaseEdit.setEnabled(true);
		descendHeightInputEdit.setEnabled(true);
	}

	private void disableParametersEdit() {
		editButton.setVisibility(View.VISIBLE);
		saveButton.setVisibility(View.INVISIBLE);
		discardButton.setVisibility(View.INVISIBLE);
		buttonLayout.setVisibility(View.INVISIBLE);
		useDefaultParametersCheckBox.setEnabled(false);
		useDefaultParametersCheckBox.setChecked(false);
		speedEdit.setEnabled(false);
		ascendTimeIncreaseEdit.setEnabled(false);
		ascendHeightInputEdit.setEnabled(false);
		descendTimeIncreaseEdit.setEnabled(false);
		descendHeightInputEdit.setEnabled(false);
	}

	private void parametersCheck() {
		double speedBackConversionFactor, heightBackConversionFactor;
		double[] parametersList = new double[5];
		// conversion
		if(isMetric==false) {
			speedBackConversionFactor = Constants.MI_TO_KM;
			heightBackConversionFactor = Constants.FEET_TO_METER;
		} else {
			speedBackConversionFactor = 1;
			heightBackConversionFactor = 1;
		}
		String speed = speedEdit.getText().toString();
		String ascendTimeIncrease = ascendTimeIncreaseEdit.getText().toString();
		String ascendHeightInput = ascendHeightInputEdit.getText().toString();
		String descendTimeIncrease = descendTimeIncreaseEdit.getText().toString();
		String descendHeightInput = descendHeightInputEdit.getText().toString();

		// Parameters checking
		if(Parameters.parametersCheck(speed)==false
				|| Parameters.parametersCheck(ascendTimeIncrease)==false
				|| Parameters.parametersCheck(ascendHeightInput)==false
				|| Parameters.parametersCheck(descendTimeIncrease)==false
				|| Parameters.parametersCheck(descendHeightInput)==false) {
			new AlertDialog.Builder(this)
			.setTitle(R.string.parameter_input_invalid)
			.setMessage(R.string.parameter_input_invalid_message)			
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					// do nothing
				}
			}).show();
		} else {
			parametersList[0] = Double.parseDouble(speed)*speedBackConversionFactor;
			parametersList[1] = Double.parseDouble(ascendTimeIncrease);
			parametersList[2] = Double.parseDouble(ascendHeightInput)*heightBackConversionFactor;
			parametersList[3] = Double.parseDouble(descendTimeIncrease);
			parametersList[4] = Double.parseDouble(descendHeightInput)*heightBackConversionFactor;

			if(parametersList[0]==0 || parametersList[2]==0 || parametersList[4]==0) {
				new AlertDialog.Builder(this)
				.setTitle(R.string.parameter_input_invalid)
				.setMessage(R.string.parameter_input_invalid_message)				
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						// do nothing
					}
				}).show();
			}
			else {
				// if no problem is found, modify parameters
				try {
					MainActivity.main.getRouteData().modifyRoute(route.getName(),route.getRPList(),parametersList);
					setUnitUnchangeableValues();					
					disableParametersEdit();					
				} catch (InvalidParametersException e) {
					Toast.makeText(this, "Invalid Parameters", Toast.LENGTH_SHORT).show();
				}	
			}
		}		
	}

	@Override
	public void onDialogPositiveClick(InstantCheckDialog dialog) {
		if(dialog.getTag().equals(CHANGE_ROUTE_NAME_TAG)) {
			String newRouteName = dialog.getName();
			MainActivity.main.getRouteData().changeRouteName(route.getName(), newRouteName);
			route.setName(newRouteName);
			TextView routeNameView = (TextView) this.findViewById(R.id.this_route_name);
			routeNameView.setText(newRouteName);
			Intent intent = getIntent(); // change route name in created intent
			intent.putExtra(ExtraTypes.ROUTE_TO_BE_VIEWED,newRouteName);
		}
	}

	@Override
	public void onDialogNegativeClick(InstantCheckDialog dialog) {
		// Do nothing
	}
}
