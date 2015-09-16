package com.outdoorapps.hiketimecallite.support;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.annotation.SuppressLint;

public class Time {
	
	public static final String[] MONTHNAMES = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
	/**
	 * Calculate the time diffence and return the answer in seconds
	 * @param time1
	 * @param time2
	 * @return
	 * @throws ParseException
	 */
	@SuppressLint("SimpleDateFormat")
	public static long getTimeDifference(String time1, String time2) throws ParseException {
		long date1, date2;
		try {
			date1 = Long.parseLong(time1.subSequence(8, 10)+"");
			date2 = Long.parseLong(time2.subSequence(8, 10)+"");

			long timeDiff;

			// Avoid using SimpleDateFormat if it is on the same date, save cpu time
			if(date1==date2) {
				long sec1 = Long.parseLong(time1.subSequence(17, 19)+"");
				long min1 = Long.parseLong(time1.subSequence(14, 16)+"");
				long hour1 = Long.parseLong(time1.subSequence(11, 13)+"");

				long sec2 = Long.parseLong(time2.subSequence(17, 19)+"");
				long min2 = Long.parseLong(time2.subSequence(14, 16)+"");
				long hour2 = Long.parseLong(time2.subSequence(11, 13)+"");

				timeDiff = (sec2 + min2*60 + hour2*3600) - (sec1 + min1*60 + hour1*3600);
			} else {
				String t1 = (time1.replace("T", "/")).replace("Z", "");
				String t2 = (time2.replace("T", "/")).replace("Z", "");

				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss");
				Date d1 = format.parse(t1);
				Date d2 = format.parse(t2);

				timeDiff = (d2.getTime()-d1.getTime())/1000;
			}
			return timeDiff;
		} catch(IndexOutOfBoundsException e) {
			// If the time if invalid
			return 0;
		}	
	}

	public static String getCurrentTime() {
		DecimalFormat twoDigits = new DecimalFormat("00");
		String time;
		Calendar c = Calendar.getInstance();

		String year = c.get(Calendar.YEAR)+"";
		String month = twoDigits.format(c.get(Calendar.MONTH)+1);
		String date = twoDigits.format(c.get(Calendar.DATE));		
		String hour = twoDigits.format(c.get(Calendar.HOUR_OF_DAY));
		String min = twoDigits.format(c.get(Calendar.MINUTE));
		String sec = twoDigits.format(c.get(Calendar.SECOND));

		time = year+"-"+month+"-"+date+"T"+hour+":"+min+":"+sec+"Z";
		return time;
	}

	public static String getFormattedTime(long timeInSecs) {
		DecimalFormat twoDigitFormat = new DecimalFormat("00");
		long sec = timeInSecs % 60;
		long mins = (timeInSecs / 60) % 60; // Round down
		long hours = timeInSecs / 3600; // Round down

		// avoid rounding error
		if(sec==60) {
			sec = 0;
			mins++;
		}
		if(mins==60) {
			mins = 0;
			hours++;		
		} 
		return twoDigitFormat.format(hours) + ":" 
				+ twoDigitFormat.format(mins) + ":" + twoDigitFormat.format(sec);
	}
	
	/**
	 * 
	 * @param date (in yyyy-MM-dd format)
	 * @return date (in MONTH_NAME dd, yyyy format)
	 * @throws ParseException 
	 */
	@SuppressLint("SimpleDateFormat")
	public static String getFormattedDate(String date) throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date d = format.parse(date);
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		String month = getMonthName(c.get(Calendar.MONTH));
		return month + " " + c.get(Calendar.DATE)+ ", " +c.get(Calendar.YEAR);
	}
	
	/**
	 * 0 returns Jan
	 * @param month
	 * @return
	 */
	public static String getMonthName(int month) {
		return MONTHNAMES[month];
	}
}
