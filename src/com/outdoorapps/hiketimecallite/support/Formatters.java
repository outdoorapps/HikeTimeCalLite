package com.outdoorapps.hiketimecallite.support;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class Formatters {
	private DecimalFormat distanceFormatter, integerFormatter, hoursFormatter, minsFormatter;
	private DecimalFormat speedFormatter, oneDecimalPTFormatter, twoDecimalPTFormatter;

	public Formatters() {
		distanceFormatter = new DecimalFormat("###.##");
		integerFormatter = new DecimalFormat("###");	
		hoursFormatter = new DecimalFormat("###");
		minsFormatter = new DecimalFormat("00");
		speedFormatter = new DecimalFormat("###.#");
		oneDecimalPTFormatter = new DecimalFormat("###.#");	
		twoDecimalPTFormatter = new DecimalFormat("###.##");	

		integerFormatter.setRoundingMode(RoundingMode.HALF_UP);
		hoursFormatter.setRoundingMode(RoundingMode.DOWN);		
		minsFormatter.setRoundingMode(RoundingMode.HALF_UP);
		speedFormatter.setRoundingMode(RoundingMode.HALF_UP);			
		oneDecimalPTFormatter.setRoundingMode(RoundingMode.HALF_UP);
		twoDecimalPTFormatter.setRoundingMode(RoundingMode.HALF_UP);
	}
	
	public String formatInteger(double number) {
		return integerFormatter.format(number);
	}

	public String formatHours(double hours) {
		return hoursFormatter.format(hours);
	}

	public String formatOneDecimalPT(double number) {
		return oneDecimalPTFormatter.format(number);
	}
	
	public String formatTwoDecimalPT(double number) {
		return twoDecimalPTFormatter.format(number);
	}

	public String formatDistance(double distance) {
		return distanceFormatter.format(distance);
	}

	public String formatElevation(double elevation) {
		return integerFormatter.format(elevation);
	}

	public String formatSpeed(double speed) {
		return speedFormatter.format(speed);
	}
	
	public String formatTwoDigits(double value) {
		return minsFormatter.format(value);
	}

	public String formatTime(double timeInMins) {
		double hours = Double.parseDouble(hoursFormatter.format(timeInMins / 60));
		double mins = Double.parseDouble(integerFormatter.format(timeInMins % 60));
		if(mins==60) { // avoid rounding error
			mins = 0;
			hours++;		
		} return hoursFormatter.format(hours) + " hrs " 
		+ minsFormatter.format(mins) + " mins";
	}
	
}
