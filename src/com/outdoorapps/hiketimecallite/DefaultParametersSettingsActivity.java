package com.outdoorapps.hiketimecallite;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.google.analytics.tracking.android.EasyTracker;
import com.outdoorapps.hiketimecallite.support.Formatters;
import com.outdoorapps.hiketimecallite.support.Parameters;
import com.outdoorapps.hiketimecallite.support.SplitActionBarStyle;
import com.outdoorapps.hiketimecallite.support.constants.Constants;
import com.outdoorapps.hiketimecallite.support.constants.ExtraTypes;
import com.outdoorapps.hiketimecallite.support.constants.Links;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;


public class DefaultParametersSettingsActivity extends Activity implements
OnEditorActionListener {

	private double[] originalParametersList;
	private double speedConversionFactor, heightConversionFactor;
	private double speedBackConversionFactor, heightBackConversionFactor;
	private boolean isMetric;
	private TextView speed_unit, height_unit1, height_unit2;
	private EditText speedEdit, ascendTimeIncreaseEdit, ascendHeightInputEdit, 
					descendTimeIncreaseEdit, descendHeightInputEdit;
	private Formatters formatters;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_default_parameters_settings);
		setupActionBar();
		formatters = new Formatters();

		// set default parameters in EditTexts
		speedEdit = (EditText) findViewById(R.id.speed_input);
		ascendTimeIncreaseEdit = (EditText) findViewById(R.id.ascendTimeIncrease);
		ascendHeightInputEdit = (EditText) findViewById(R.id.ascendHeightInput);
		descendTimeIncreaseEdit = (EditText) findViewById(R.id.descendTimeIncrease);
		descendHeightInputEdit = (EditText) findViewById(R.id.descentHeightInput);
		descendHeightInputEdit.setOnEditorActionListener(this);

		// Conversion
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		isMetric = Boolean.parseBoolean(prefs.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, ""));
		if(isMetric==false) {
			speedConversionFactor = Constants.KM_TO_MI;
			heightConversionFactor = Constants.METER_TO_FEET;
			speed_unit = (TextView)findViewById(R.id.speed_unit);			
			height_unit1 = (TextView)findViewById(R.id.height_unit1);
			height_unit2 = (TextView)findViewById(R.id.height_unit2);

			speed_unit.setText("mph");
			height_unit1.setText("ft");
			height_unit2.setText("ft");
		} else {
			speedConversionFactor = 1;
			heightConversionFactor = 1;
		}

		Intent intent = getIntent();
		originalParametersList = intent.getDoubleArrayExtra(ExtraTypes.PARAMETERS);
		
		String speed = formatters.formatSpeed(originalParametersList[0]*speedConversionFactor);
		String ascendTimeIncrease = formatters.formatOneDecimalPT(originalParametersList[1]);
		String ascendHeightInput = formatters.formatElevation(originalParametersList[2]*heightConversionFactor);
		String descendTimeIncrease = formatters.formatOneDecimalPT(originalParametersList[3]);
		String descendHeightInput = formatters.formatElevation(originalParametersList[4]*heightConversionFactor);
		
		speedEdit.setText(speed);
		ascendTimeIncreaseEdit.setText(ascendTimeIncrease);
		ascendHeightInputEdit.setText(ascendHeightInput);
		descendTimeIncreaseEdit.setText(descendTimeIncrease);
		descendHeightInputEdit.setText(descendHeightInput);	
	}

	private void setupActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		SplitActionBarStyle.setSplitActionBarOverlayColor(actionBar);
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
		getMenuInflater().inflate(R.menu.default_parameters_settings, menu);
		return true;
	}

	private void save() {
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
				Intent intent = new Intent();
				intent.putExtra(ExtraTypes.PARAMETERS, parametersList);
				setResult(RESULT_OK, intent);
				finish();
			}
		}
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_DONE) {
			save();
			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.save:
			save();
			break;
		case R.id.cancel:
			finish();
			break;
		case R.id.reset:
			String speed = formatters.formatSpeed(originalParametersList[0]*speedConversionFactor);
			String ascendTimeIncrease = formatters.formatOneDecimalPT(originalParametersList[1]);
			String ascendHeightInput = formatters.formatElevation(originalParametersList[2]*heightConversionFactor);
			String descendTimeIncrease = formatters.formatOneDecimalPT(originalParametersList[3]);
			String descendHeightInput = formatters.formatElevation(originalParametersList[4]*heightConversionFactor);
			
			speedEdit.setText(speed);
			ascendTimeIncreaseEdit.setText(ascendTimeIncrease);
			ascendHeightInputEdit.setText(ascendHeightInput);
			descendTimeIncreaseEdit.setText(descendTimeIncrease);
			descendHeightInputEdit.setText(descendHeightInput);	
			break;
		case R.id.help:
			Uri uri = Uri.parse(Links.HELP_LINK); 
			startActivity(new Intent(Intent.ACTION_VIEW, uri));
			break;
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		default:
			break;
		}
		return true;
	}

}
