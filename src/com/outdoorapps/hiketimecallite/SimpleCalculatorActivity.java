package com.outdoorapps.hiketimecallite;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.outdoorapps.hiketimecallite.model.route.Calculator;
import com.outdoorapps.hiketimecallite.model.route.Calculator.InvalidParametersException;
import com.outdoorapps.hiketimecallite.support.Formatters;
import com.outdoorapps.hiketimecallite.support.Parameters;
import com.outdoorapps.hiketimecallite.support.SplitActionBarStyle;
import com.outdoorapps.hiketimecallite.support.constants.Constants;
import com.outdoorapps.hiketimecallite.support.constants.Defaults;
import com.outdoorapps.hiketimecallite.support.constants.Links;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;
import com.outdoorapps.hiketimecallite.support.constants.RequestCode;

public class SimpleCalculatorActivity extends Activity implements
ActionBar.OnNavigationListener,
OnEditorActionListener,
OnCheckedChangeListener,
OnSharedPreferenceChangeListener {

	private TextView distanceUnit, elevationUnit, resultDistance, resultDistanceUnit,
	resultDistanceRoundTrip, resultDistanceRoundTripUnit, tripTime,
	returnTripTime, totalTime, speedUnit, heightUnit1, heightUnit2;
	private EditText distanceEdit, elevationEdit,
	speedEdit, ascendTimeIncreaseEdit, ascendHeightInputEdit, 
	descendTimeIncreaseEdit, descendHeightInputEdit;
	private CheckBox useDefaultParametersCheckBox;
	private SharedPreferences prefs;
	private boolean isMetric;
	private Formatters formatters;
	private InputMethodManager imm;

	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_calculator);
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
				// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(getActionBarThemedContextCompat(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, MainActivity.dropdownMenuItems), this);
		actionBar.setSelectedNavigationItem(3);		
		// Show the Up button in the action bar.
		setupActionBar();

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);		
		formatters = new Formatters();

		// Route info
		distanceEdit = (EditText) this.findViewById(R.id.distance_field);
		elevationEdit = (EditText) this.findViewById(R.id.elevation_field);
		elevationEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
		elevationEdit.setOnEditorActionListener(this);

		distanceUnit = (TextView)findViewById(R.id.distance_unit);
		elevationUnit = (TextView)findViewById(R.id.elevation_unit);

		// Parameters
		speedEdit = (EditText) this.findViewById(R.id.speed_input);
		ascendTimeIncreaseEdit = (EditText) this.findViewById(R.id.ascendTimeIncrease);
		ascendHeightInputEdit = (EditText) this.findViewById(R.id.ascendHeightInput);
		descendTimeIncreaseEdit = (EditText) this.findViewById(R.id.descendTimeIncrease);
		descendHeightInputEdit = (EditText) this.findViewById(R.id.descentHeightInput);
		descendHeightInputEdit.setOnEditorActionListener(this);

		// Use default parameters by default
		speedEdit.setEnabled(false);
		ascendTimeIncreaseEdit.setEnabled(false);
		ascendHeightInputEdit.setEnabled(false);
		descendTimeIncreaseEdit.setEnabled(false);
		descendHeightInputEdit.setEnabled(false);

		heightUnit1 = (TextView)findViewById(R.id.height_unit1);
		heightUnit2 = (TextView)findViewById(R.id.height_unit2);
		speedUnit = (TextView)findViewById(R.id.speed_unit);		

		useDefaultParametersCheckBox = (CheckBox) findViewById(R.id.default_parameters_checkBox);		
		useDefaultParametersCheckBox.setChecked(true); 
		useDefaultParametersCheckBox.setOnCheckedChangeListener(this);

		// Result
		resultDistance = (TextView)findViewById(R.id.this_distance);
		resultDistanceUnit = (TextView)findViewById(R.id.this_distance_unit);
		resultDistanceRoundTrip = (TextView)findViewById(R.id.this_distance_round_trip);
		resultDistanceRoundTripUnit = (TextView)findViewById(R.id.this_distance_round_trip_unit);		
		tripTime = (TextView)findViewById(R.id.this_trip_time);
		returnTripTime = (TextView)findViewById(R.id.this_return_trip_time);
		totalTime = (TextView)findViewById(R.id.this_total_time);

		resultDistance.setText("0");
		resultDistanceRoundTrip.setText("0");
		tripTime.setText("0 hrs 0 mins");
		returnTripTime.setText("0 hrs 0 mins"); 
		totalTime.setText("0 hrs 0 mins");

		isMetric = Boolean.parseBoolean(prefs.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, "true"));
		setInputUnits();
		setDefaultParameters();
		setResultsUnits();

		imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
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

	private void calculate() {
		isMetric = Boolean.parseBoolean(prefs.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, "true"));
		double[] inputList = inputCheck();
		if(inputList!=null) {
			double[] parametersList = parametersCheck();
			if(parametersList!=null) {
				try {
					double[] resultList = Calculator.calculate(parametersList, inputList);					
					resultDistance.setText(formatters.formatDistance(resultList[0]));
					resultDistanceRoundTrip.setText(formatters.formatDistance(resultList[1]));
					tripTime.setText(formatters.formatTime(resultList[2]));
					returnTripTime.setText(formatters.formatTime(resultList[3])); 
					totalTime.setText(formatters.formatTime(resultList[4]));
					imm.hideSoftInputFromWindow(elevationEdit.getWindowToken(), 0);
					imm.hideSoftInputFromWindow(descendHeightInputEdit.getWindowToken(), 0);
				} catch (InvalidParametersException e) { // Should not happen
					Toast.makeText(this, "Invalid parameters", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	private void reset() {
		distanceEdit.setText("");
		elevationEdit.setText("");
		useDefaultParametersCheckBox.setChecked(true);
		setDefaultParameters();
	}

	private void setResultsUnits() {
		double distanceConversionFactor;
		if(isMetric) {;
		resultDistanceUnit.setText(" km");
		resultDistanceRoundTripUnit.setText(" km");
		distanceConversionFactor = Constants.MI_TO_KM;
		} else {
			resultDistanceUnit.setText(" mi");
			resultDistanceRoundTripUnit.setText(" mi");
			distanceConversionFactor = Constants.KM_TO_MI;
		}

		double resultDistanceNum = Double.parseDouble(resultDistance.getText().toString());
		double resultDistanceRoundTripNum = Double.parseDouble(resultDistanceRoundTrip.getText().toString());

		String convertedResultDistance = formatters.formatDistance(resultDistanceNum*distanceConversionFactor);
		String convertedResultDistanceRoundTrip = formatters.formatDistance(resultDistanceRoundTripNum);

		resultDistance.setText(convertedResultDistance);
		resultDistanceRoundTrip.setText(convertedResultDistanceRoundTrip);
	}

	private void setParameters() {
		double distanceConversionFactor, heightConversionFactor;
		if(isMetric) {
			speedUnit.setText("km/h");
			heightUnit1.setText("m");
			heightUnit2.setText("m");
			distanceConversionFactor = Constants.MI_TO_KM;
			heightConversionFactor = Constants.FEET_TO_METER;
		} else {
			speedUnit.setText("mph");
			heightUnit1.setText("ft");
			heightUnit2.setText("ft");		
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

	private void setInputUnits() {
		double distanceConversionFactor, heightConversionFactor;
		if(isMetric==true) {		
			distanceUnit.setText("km");
			elevationUnit.setText("m");
			speedUnit.setText("km/h");
			distanceConversionFactor = Constants.MI_TO_KM;
			heightConversionFactor = Constants.FEET_TO_METER;
		} else {
			distanceUnit.setText("mi");
			elevationUnit.setText("ft");
			speedUnit.setText("mph");
			distanceConversionFactor = Constants.KM_TO_MI;
			heightConversionFactor = Constants.METER_TO_FEET;
		}

		// Check if user has initialize inputs
		try {
			double distanceInput = Double.parseDouble(distanceEdit.getText().toString());
			String distance = formatters.formatSpeed(distanceInput*distanceConversionFactor);
			distanceEdit.setText(distance);
		} catch(NumberFormatException e) {
			distanceEdit.setText("");
		}

		try {
			double elevationInput = Double.parseDouble(elevationEdit.getText().toString());
			String elevation = formatters.formatElevation(elevationInput*heightConversionFactor);
			elevationEdit.setText(elevation);
		} catch(NumberFormatException e) {
			elevationEdit.setText("");
		}
	}

	/**
	 * Set to default parameters in settings
	 */
	private void setDefaultParameters() {
		// set factors
		double distanceConversionFactor, heightConversionFactor;
		if(isMetric==true) {
			distanceConversionFactor = 1;
			heightConversionFactor = 1;
		} else {			
			distanceConversionFactor = Constants.KM_TO_MI;
			heightConversionFactor = Constants.METER_TO_FEET;			
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

	private double[] inputCheck() {
		double distanceBackConversionFactor, elevationBackConversionFactor;
		double[] inputList = new double[2];
		// conversion
		if(isMetric==false) {
			distanceBackConversionFactor = Constants.MI_TO_KM;
			elevationBackConversionFactor = Constants.FEET_TO_METER;
		} else {
			distanceBackConversionFactor = 1;
			elevationBackConversionFactor = 1;
		}
		String distanceInput = distanceEdit.getText().toString();
		String elevationInput = elevationEdit.getText().toString();
		if(Parameters.parametersCheck(distanceInput)==false ||
				Parameters.parametersCheck(elevationInput)==false) {
			new AlertDialog.Builder(this)			
			.setTitle(R.string.invalid_input)
			.setMessage(R.string.invalid_input_message)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					// do nothing
				}
			}).show();
			return null;
		} else {
			inputList[0] = Double.parseDouble(distanceInput)*distanceBackConversionFactor;
			inputList[1] = Double.parseDouble(elevationInput)*elevationBackConversionFactor;			
			return inputList;
		}
	}


	private double[] parametersCheck() {
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
			return null;
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
				return null;
			}
			else {
				// if no problem is found, modify parameters
				return parametersList;
			}
		}		
	}

	private void setupActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		SplitActionBarStyle.setSplitActionBarOverlayColor(actionBar);
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
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.simple_calculator, menu);
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
			intent = new Intent(this,SelectRouteActivity.class);
			finish();
			startActivityForResult(intent, RequestCode.SELECT_ROUTE);
			break;
		case 2: // Select Track
			intent = new Intent(this,SelectTrackActivity.class);
			finish();
			startActivityForResult(intent, RequestCode.SELECT_TRACK);
			break;
		case 3: // calculator
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
		switch (item.getItemId()) {
		case R.id.reset:
			reset();
			break;
		case R.id.calculate:
			calculate();
			break;
		case R.id.help:
			Uri uri = Uri.parse(Links.HELP_LINK); 
			startActivity(new Intent(Intent.ACTION_VIEW, uri));
			break;
		case R.id.action_settings:
			Intent intent = new Intent(this,SettingsActivity.class);
			startActivity(intent);
			break;
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_DONE) {
			calculate();
			return true;
		}
		return false;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equals(PrefKeys.KEY_PREF_DISPLAY_UNIT)) {
			isMetric = Boolean.parseBoolean(prefs.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, "true"));
			setInputUnits();
			setParameters();
			setResultsUnits();
		}

		if((key.equals(PrefKeys.SPEED_KEY) || key.equals(PrefKeys.ASCEND_TIME_INCREASE_KEY) 
				|| key.equals(PrefKeys.ASCEND_HEIGHT_KEY) || key.equals(PrefKeys.DESCEND_TIME_INCREASE_KEY)
				|| key.equals(PrefKeys.DESCEND_HEIGHT_KEY))
				&& useDefaultParametersCheckBox.isChecked()) {
			setDefaultParameters();
		}
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

}
