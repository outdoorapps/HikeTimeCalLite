package com.outdoorapps.hiketimecallite.support.constants;

import android.content.SharedPreferences;

public class Defaults {
	public static final String DEFAULT_DISPLAY_UNIT = "true";
	public static final String DEFAULT_ELEVATION_SOURCE = "google_elevation_api";
	public static final String DEFAULT_INFO_TEXT_COLOR = "white";
	public static final double DEFAULT_SPEED = 5; /** km/h */
	public static final double DEFAULT_ASCEND_TIME_INCREASE = 3; /** mins */
	public static final double DEFAULT_ASCEND_HEIGHT = 20; /** m */
	public static final double DEFAULT_DESCEND_TIME_INCREASE = 1.2; /** mins */
	public static final double DEFAULT_DESCEND_HEIGHT = 20; /** m */
	public static final int DEFAULT_TRACKING_SAVING_FREQUENCY = 10000;
	public static final int DEFAULT_DRAW_FREQUENCY = 500; /** in milliseconds */
	
	// lite
	public static final int EXPORT_LIMIT = 540;
	
	public static double[] getDefaultParametersArray(SharedPreferences prefs) {
		double[] parameters = new double[5];
		parameters[0] = Double.parseDouble(prefs.getString(PrefKeys.SPEED_KEY, Defaults.DEFAULT_SPEED+""));
		parameters[1] = Double.parseDouble(prefs.getString(PrefKeys.ASCEND_TIME_INCREASE_KEY, Defaults.DEFAULT_ASCEND_TIME_INCREASE+""));
		parameters[2] = Double.parseDouble(prefs.getString(PrefKeys.ASCEND_HEIGHT_KEY, Defaults.DEFAULT_ASCEND_HEIGHT+""));
		parameters[3] = Double.parseDouble(prefs.getString(PrefKeys.DESCEND_TIME_INCREASE_KEY, Defaults.DEFAULT_DESCEND_TIME_INCREASE+""));
		parameters[4] = Double.parseDouble(prefs.getString(PrefKeys.DESCEND_HEIGHT_KEY, Defaults.DEFAULT_DESCEND_HEIGHT+""));		
		return parameters;
	}
	
	public static double[] getFactoryParametersArray() {
		double[] parameters = new double[5];
		parameters[0] = DEFAULT_SPEED;
		parameters[1] = DEFAULT_ASCEND_TIME_INCREASE;
		parameters[2] = DEFAULT_ASCEND_HEIGHT;
		parameters[3] = DEFAULT_DESCEND_TIME_INCREASE;
		parameters[4] = DEFAULT_DESCEND_HEIGHT;
		return parameters;
	}
}
