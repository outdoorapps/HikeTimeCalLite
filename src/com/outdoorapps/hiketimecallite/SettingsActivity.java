package com.outdoorapps.hiketimecallite;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.google.analytics.tracking.android.EasyTracker;
import com.outdoorapps.hiketimecallite.support.constants.Defaults;
import com.outdoorapps.hiketimecallite.support.constants.ExtraTypes;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;
import com.outdoorapps.hiketimecallite.support.constants.RequestCode;

public class SettingsActivity extends PreferenceActivity implements 
OnSharedPreferenceChangeListener{// change: hierarchy button not working

	public static Activity settingsActitvity;
	private SharedPreferences sharedPreferences;

	// remove assignments
	public static final String GOOGLE_ELEVATION_API = "google_elevation_api";
	public static final String USGS = "usgs";	

	private static String[] displayUnitValues, displayUnitEntries, 
	mapSourceValues, mapSourceEntries, infoTextColorValues, infoTextColorEntries;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settingsActitvity = this;
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);			
		sharedPreferences = getPreferenceScreen().getSharedPreferences();
		displayUnitValues = getResources().getStringArray(R.array.pref_unit_values);
		displayUnitEntries = getResources().getStringArray(R.array.pref_unit_entries);
		mapSourceValues = getResources().getStringArray(R.array.pref_elevation_source_values);
		mapSourceEntries = getResources().getStringArray(R.array.pref_elevation_source_entries);
		infoTextColorValues = getResources().getStringArray(R.array.pref_info_text_color_values);
		infoTextColorEntries = getResources().getStringArray(R.array.pref_info_text_color_entries);
		
		// Set preferences summary values
		Preference displayUnitPref = findPreference(PrefKeys.KEY_PREF_DISPLAY_UNIT);
		String isMetric = sharedPreferences.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, Defaults.DEFAULT_DISPLAY_UNIT);
		displayUnitPref.setSummary(getEntryValue(displayUnitEntries,displayUnitValues,isMetric));

		Preference parametersPref = findPreference(PrefKeys.KEY_PREF_DEFAULT_PARAMETERS);
		parametersPref.setOnPreferenceClickListener(new DefaultParameterSettingsListener());

		Preference elevationSourcePref = findPreference(PrefKeys.KEY_PREF_ELEVATION_SOURCE);
		String elevationSource = sharedPreferences.getString(PrefKeys.KEY_PREF_ELEVATION_SOURCE, Defaults.DEFAULT_ELEVATION_SOURCE);		
		elevationSourcePref.setSummary(getEntryValue(mapSourceEntries,mapSourceValues,elevationSource));

		Preference infoTextColorPref = findPreference(PrefKeys.KEY_PREF_INFO_TEXT_COLOR);
		String infoTextColor = sharedPreferences.getString(PrefKeys.KEY_PREF_INFO_TEXT_COLOR, Defaults.DEFAULT_INFO_TEXT_COLOR);		
		infoTextColorPref.setSummary(getEntryValue(infoTextColorEntries,infoTextColorValues,infoTextColor));
		
		Preference about = findPreference(PrefKeys.KEY_PREF_ABOUT);	
		about.setOnPreferenceClickListener(new AboutListener());
	}

	/**
	 * Find corresponding entry value to a value array given a value in value array
	 * @param entriesArray
	 * @param valuesArray
	 * @param value
	 * @return
	 */
	private String getEntryValue(String[] entriesArray, String[] valuesArray, String value) {			
		boolean found = false;
		int index = 0;
		while(index<entriesArray.length && found==false) {
			found = value.equals(valuesArray[index]);
			index++;
		}
		if(found)
			return entriesArray[index-1];
		else
			return "";
	}

	/**
	 * Update the summary for each setting
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equals(PrefKeys.KEY_PREF_DISPLAY_UNIT)) {
			Preference displayUnitPref = findPreference(key);
			String isMetric = sharedPreferences.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, Defaults.DEFAULT_DISPLAY_UNIT);
			displayUnitPref.setSummary(getEntryValue(displayUnitEntries,displayUnitValues,isMetric));
		}
		if(key.equals(PrefKeys.KEY_PREF_ELEVATION_SOURCE)) {
			Preference pref = findPreference(key);
			String elevationSource = sharedPreferences.getString(PrefKeys.KEY_PREF_ELEVATION_SOURCE, "google_map_api");
			pref.setSummary(getEntryValue(mapSourceEntries,mapSourceValues,elevationSource));
		}

		if(key.equals(PrefKeys.KEY_PREF_INFO_TEXT_COLOR)) {
			Preference infoTextColorPref = findPreference(key);
			String infoTextColor = sharedPreferences.getString(PrefKeys.KEY_PREF_INFO_TEXT_COLOR, Defaults.DEFAULT_INFO_TEXT_COLOR);		
			infoTextColorPref.setSummary(getEntryValue(infoTextColorEntries,infoTextColorValues,infoTextColor));
		}

	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
		.registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
		.unregisterOnSharedPreferenceChangeListener(this);
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
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(resultCode==RESULT_OK && requestCode==RequestCode.DEFAULT_PARAMETERS_SETTINGS){
			// Update default parameters
			double[] parametersList = intent.getDoubleArrayExtra(ExtraTypes.PARAMETERS);
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putString(PrefKeys.SPEED_KEY, parametersList[0]+"");
			editor.putString(PrefKeys.ASCEND_TIME_INCREASE_KEY, parametersList[1]+"");
			editor.putString(PrefKeys.ASCEND_HEIGHT_KEY, parametersList[2]+"");
			editor.putString(PrefKeys.DESCEND_TIME_INCREASE_KEY, parametersList[3]+"");
			editor.putString(PrefKeys.DESCEND_HEIGHT_KEY, parametersList[4]+"");
			editor.commit();
		}
	}

	private class DefaultParameterSettingsListener implements OnPreferenceClickListener {
		@Override
		public boolean onPreferenceClick(Preference pref) {
			Intent intent = new Intent(settingsActitvity,DefaultParametersSettingsActivity.class);
			double[] parametersList = new double[5];
			parametersList[0] = Double.parseDouble(sharedPreferences.getString(PrefKeys.SPEED_KEY, Defaults.DEFAULT_SPEED+""));
			parametersList[1] = Double.parseDouble(sharedPreferences.getString(PrefKeys.ASCEND_TIME_INCREASE_KEY, Defaults.DEFAULT_ASCEND_TIME_INCREASE+""));
			parametersList[2] = Double.parseDouble(sharedPreferences.getString(PrefKeys.ASCEND_HEIGHT_KEY, Defaults.DEFAULT_ASCEND_HEIGHT+""));
			parametersList[3] = Double.parseDouble(sharedPreferences.getString(PrefKeys.DESCEND_TIME_INCREASE_KEY, Defaults.DEFAULT_DESCEND_TIME_INCREASE+""));
			parametersList[4] = Double.parseDouble(sharedPreferences.getString(PrefKeys.DESCEND_HEIGHT_KEY, Defaults.DEFAULT_DESCEND_HEIGHT+""));

			intent.putExtra(ExtraTypes.PARAMETERS, parametersList);
			settingsActitvity.startActivityForResult(intent, RequestCode.DEFAULT_PARAMETERS_SETTINGS);
			return false;
		}
	}
	
	private class AboutListener implements OnPreferenceClickListener {
		@Override
		public boolean onPreferenceClick(Preference pref) {
			Intent intent = new Intent(settingsActitvity,AboutActivity.class);
			settingsActitvity.startActivity(intent);
			return false;
		}
	}
}
