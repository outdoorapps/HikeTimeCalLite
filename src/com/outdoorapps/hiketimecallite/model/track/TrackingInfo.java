package com.outdoorapps.hiketimecallite.model.track;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.widget.TextView;

import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.model.route.Calculator;
import com.outdoorapps.hiketimecallite.support.Formatters;
import com.outdoorapps.hiketimecallite.support.Time;
import com.outdoorapps.hiketimecallite.support.constants.Constants;
import com.outdoorapps.hiketimecallite.support.constants.Defaults;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;

public class TrackingInfo {
	
	private TextView trackingView, durationView, odometerView, altitudeView, speedView;
	private double heightConversionFactor, distanceConversionFactor, distance;
	private String altitudeUnit, speedUnit, distanceUnit;
	private Formatters formatters;
	private Double lat0, lon0;
	private ClockRunnable clock;
	
	public TrackingInfo(MainActivity main) {
		trackingView = (TextView) main.findViewById(R.id.tracking);
		durationView = (TextView) main.findViewById(R.id.duration);
		odometerView = (TextView) main.findViewById(R.id.odometer);
		altitudeView = (TextView) main.findViewById(R.id.altitude_info);
		speedView = (TextView) main.findViewById(R.id.speed_info);
	}
	
	/**
	 * Update text views during onCreate of main
	 */
	public void updateTextViews(MainActivity main) {
		trackingView = (TextView) main.findViewById(R.id.tracking);
		durationView = (TextView) main.findViewById(R.id.duration);
		odometerView = (TextView) main.findViewById(R.id.odometer);
		altitudeView = (TextView) main.findViewById(R.id.altitude_info);
		speedView = (TextView) main.findViewById(R.id.speed_info);
		
		String colorCode = MainActivity.main.getPref()
				.getString(PrefKeys.KEY_PREF_INFO_TEXT_COLOR, Defaults.DEFAULT_INFO_TEXT_COLOR);
		changeTextColor(colorCode);
		if(MainActivity.main.getTracking()==true)
			setHeading(clock.getStartTime());
	}
	
	@SuppressLint("SimpleDateFormat")
	public void initializeTrackingInfo(MainActivity main) {
		// Initialize objects
		formatters = new Formatters();
		trackingView = (TextView) main.findViewById(R.id.tracking);
		durationView = (TextView) main.findViewById(R.id.duration);
		odometerView = (TextView) main.findViewById(R.id.odometer);
		altitudeView = (TextView) main.findViewById(R.id.altitude_info);
		speedView = (TextView) main.findViewById(R.id.speed_info);
		
		String colorCode = main.getPref().getString(PrefKeys.KEY_PREF_INFO_TEXT_COLOR, Defaults.DEFAULT_INFO_TEXT_COLOR);		
		trackingView.setTextColor(Color.parseColor(colorCode));
		durationView.setTextColor(Color.parseColor(colorCode));
		odometerView.setTextColor(Color.parseColor(colorCode));
		altitudeView.setTextColor(Color.parseColor(colorCode));
		speedView.setTextColor(Color.parseColor(colorCode));
		
		boolean isMetric = Boolean.parseBoolean(main.getPref().getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, Defaults.DEFAULT_DISPLAY_UNIT));
		setUnits(isMetric);
		lat0 = null;
		lon0 = null;	
		distance = 0;
		
		// Set TextViews
		Calendar c = Calendar.getInstance();
		long startTime = c.getTimeInMillis();		
		setHeading(startTime);
		odometerView.setText("Locating...");
		altitudeView.setText("Locating...");
		speedView.setText("Locating...");
		
		clock = new ClockRunnable(startTime);
		clock.run();
	}
	
	@SuppressLint("SimpleDateFormat")
	private void setHeading(long startTime) {		
		SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss a");
		trackingView.setText("Tracking since " + format.format(startTime));
	}
	
	public void resumeClock() {
		clock.resumeClock();
	}
	
	public void updateTrackingInfo(Location location) {
		// Set odometer
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		
		// Initialize first location if needed
		if(lat0==null)
			lat0 = lat;
		if(lon0==null)
			lon0 = lon;
		
		distance += Calculator.getDistance(lat0, lon0, lat, lon);
		lat0 = lat;
		lon0 = lon;		
		double displayDistance = distance*distanceConversionFactor;
		odometerView.setText("Distance: " + formatters.formatTwoDecimalPT(displayDistance) + distanceUnit);
		
		// Set Altitude
		if(location.hasAltitude()) {
			double altitude = location.getAltitude()*heightConversionFactor;
			altitudeView.setText("Altitude: "+ formatters.formatOneDecimalPT(altitude) + altitudeUnit);
		} else
			altitudeView.setText("No altitude info");
		
		// Set Speed
		if(location.hasAltitude()) {
			double speed = location.getSpeed()*distanceConversionFactor;
			speedView.setText("Speed: "+ formatters.formatOneDecimalPT(speed) + speedUnit);
		} else
			speedView.setText("No speed info");
	}
	
	public void hideTrackingInfo() {
		trackingView.setText("");
		durationView.setText("");
		odometerView.setText("");
		altitudeView.setText("");
		speedView.setText("");
	}

	private class ClockRunnable implements Runnable {
		private Handler mHandler;
		private long startTime; /** In Date class format, in milliseconds */
		private long duration; /** in seconds */
		
		@SuppressLint("SimpleDateFormat")
		public ClockRunnable(long startTime) {
			this.startTime = startTime;
			mHandler = new Handler();
			duration = 0;
		}
		
		@Override
		public void run() {
			// Pause update textview onPause to conserve power
			if(MainActivity.main.getTracker().getPauseUpdate()==false)
				durationView.setText(Time.getFormattedTime(duration));			
			duration++;
			if(MainActivity.main.getTracking()==true) {
				mHandler.postDelayed(this, 1000);
			} else {
				durationView.setText("");
			}
		}
		
		// Set the clock to correct time without waiting for an update
		public void resumeClock() {
			durationView.setText(Time.getFormattedTime(duration));
		}
		
		public long getStartTime() {
			return startTime;
		}
	}

	private void setUnits(boolean isMetric) {
		if(isMetric) {
			heightConversionFactor = 1;
			distanceConversionFactor = 1;
			altitudeUnit = " m";
			speedUnit = " km/h";
			distanceUnit = " km";
		}
		else {
			heightConversionFactor = Constants.METER_TO_FEET;
			distanceConversionFactor = Constants.KM_TO_MI;
			altitudeUnit = " ft";
			speedUnit = " mph";
			distanceUnit = " mi";
		}
	}
	
	public void changeTextColor(String colorCode) {
		trackingView.setTextColor(Color.parseColor(colorCode));
		durationView.setTextColor(Color.parseColor(colorCode));
		odometerView.setTextColor(Color.parseColor(colorCode));
		altitudeView.setTextColor(Color.parseColor(colorCode));
		speedView.setTextColor(Color.parseColor(colorCode));
	}
	
	public void changeUnit(boolean isMetric) {
		setUnits(isMetric);
	}
	
}
