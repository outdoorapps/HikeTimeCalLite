package com.outdoorapps.hiketimecallite.managers;

import java.text.ParseException;

import android.content.SharedPreferences;

import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.model.route.Route;
import com.outdoorapps.hiketimecallite.model.route.RoutePoint;
import com.outdoorapps.hiketimecallite.model.track.Track;
import com.outdoorapps.hiketimecallite.model.track.TrackPoint;
import com.outdoorapps.hiketimecallite.support.Formatters;
import com.outdoorapps.hiketimecallite.support.MapObject;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;
import com.outdoorapps.hiketimecallite.support.Time;
import com.outdoorapps.hiketimecallite.support.constants.Constants;
import com.outdoorapps.hiketimecallite.support.constants.Defaults;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;

/**
 * Additional Information for each marker on map
 * @author chbting
 *
 */
public class MarkerInfo {

	private MainActivity main;
	private String name, markerTitle;
	private int markerNumber;
	private MapObjectType type;
	private static final String KEY_MARKERINFO_MAP_OBJECT_NAME = "MarkerInfo_map_object_name";
	private static final String KEY_MARKERINFO_MARKER_TITLE = "MarkerInfo_marker_title";
	private static final String KEY_MARKERINFO_MARKER_NUMBER = "MarkerInfo_marker_number";
	private static final String KEY_MARKERINFO_MARKER_TYPE = "MarkerInfo_marker_type";
	private Formatters formatters;

	public MarkerInfo(MainActivity main) {
		this.main = main;
		markerTitle = "";
		name = "";
		markerNumber = -1;
		type = MapObjectType.invalid;
	}

	public MarkerInfo(MainActivity main, String name, String markerTitle, int markerNumber, MapObjectType type) {
		this.main = main;
		this.markerTitle = markerTitle;
		this.name = name;
		this.markerNumber = markerNumber;
		this.type = type;
		formatters = new Formatters();
	}

	public MarkerInfo(MainActivity main, SharedPreferences prefs) {		
		this.main = main;
		name = prefs.getString(KEY_MARKERINFO_MAP_OBJECT_NAME, "");
		markerTitle = prefs.getString(KEY_MARKERINFO_MARKER_TITLE, "");
		markerNumber = prefs.getInt(KEY_MARKERINFO_MARKER_NUMBER, -1);
		int typeInt = prefs.getInt(KEY_MARKERINFO_MARKER_TYPE, -1);
		type = MapObject.intToMapObjectType(typeInt);
		formatters = new Formatters();
	}

	public void saveToSharedPreferences(SharedPreferences.Editor editor) {
		editor.putString(KEY_MARKERINFO_MAP_OBJECT_NAME, name);
		editor.putString(KEY_MARKERINFO_MARKER_TITLE, markerTitle);
		editor.putInt(KEY_MARKERINFO_MARKER_NUMBER, markerNumber);
		editor.putInt(KEY_MARKERINFO_MARKER_TYPE, MapObject.mapObjectTypeToInt(type));
		editor.commit();
	}

	public String[] getInfoString(SharedPreferences prefs) {
		String[] markerInfoArray = new String[6];
		if(type==MapObjectType.route) {
			if(checkInfoValid() && main.getRouteData().getRoute(name)!=null) {
				Route route = main.getRouteData().getRoute(name);
				RoutePoint rp = route.getRPList().get(markerNumber);
				double distance = rp.getCulminativeDistance();
				double rtnDistance = rp.getCulminativeRtnDistance();
				double elevation = rp.getElevation();
				double time = rp.getCulminativeTime();
				double rTime = rp.getCulminativeRtnTime();

				String estTime = formatters.formatTime(time);
				String estRtnTime = formatters.formatTime(rTime);

				boolean isMetric = Boolean.parseBoolean(prefs.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, Defaults.DEFAULT_DISPLAY_UNIT));
				double distanceConversionFactor, heightConversionFactor;
				String hUnit, dUnit;
				if(isMetric) {
					distanceConversionFactor = 1;
					heightConversionFactor = 1;
					dUnit = " km";
					hUnit = " m";
				} else {
					distanceConversionFactor = Constants.KM_TO_MI;
					heightConversionFactor = Constants.METER_TO_FEET;
					dUnit = " mi";
					hUnit = " ft";
				}

				String displayDistance = formatters.formatDistance(distance*distanceConversionFactor);
				String displayRtnDistance = formatters.formatDistance(rtnDistance*distanceConversionFactor);
				String displayElevation = formatters.formatElevation(elevation*heightConversionFactor);

				markerInfoArray[0] = name	+" ("+ markerTitle + ")";
				markerInfoArray[1] = "Distance: "+displayDistance+dUnit;
				markerInfoArray[2] = "Rtn Distance: "+displayRtnDistance+dUnit;
				markerInfoArray[3] = "Elevation: "+displayElevation+hUnit;
				markerInfoArray[4] = "Trip Time: "+estTime;
				markerInfoArray[5] = "Rtn Time: "+estRtnTime;

				return markerInfoArray;
			}
			else {
				for(int i=0;i<markerInfoArray.length;i++)
					markerInfoArray[i] = "";
				return markerInfoArray;
			}
		} else if(type==MapObjectType.track) {
			Track track = main.getTrackData().getTrack(name);
			TrackPoint tp = track.getTPList().get(markerNumber);

			String timeString = tp.getTime();
			String time = timeString.split("T")[1].replace("Z", "");
			String date = timeString.split("T")[0];
			try {
				date = Time.getFormattedDate(track.getDate());
			} catch (ParseException e) {
				date = track.getDate();
			}
			double distance = tp.getCulminatedDistance();
			double altitude = tp.getLocation().getAltitude();
			long culTime = tp.getCulminatedTime();
			float speed = tp.getLocation().getSpeed();

			boolean isMetric = Boolean.parseBoolean(prefs.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, Defaults.DEFAULT_DISPLAY_UNIT));
			double distanceConversionFactor, heightConversionFactor;
			String hUnit, dUnit, sUnit;
			if(isMetric) {
				distanceConversionFactor = 1;
				heightConversionFactor = 1;
				dUnit = " km";
				hUnit = " m";
				sUnit = " km/h";
			} else {
				distanceConversionFactor = Constants.KM_TO_MI;
				heightConversionFactor = Constants.METER_TO_FEET;
				dUnit = " mi";
				hUnit = " ft";
				sUnit = " mph";
			}

			String displayDistance = formatters.formatDistance(distance*distanceConversionFactor);
			String displayCulTime = formatters.formatTime(culTime/60);
			String displayAltitude = formatters.formatElevation(altitude*heightConversionFactor);
			String displayTime = date + " " + time;
			String displaySpeed = formatters.formatSpeed(speed*distanceConversionFactor);

			markerInfoArray[0] = name	+" ("+ markerTitle + ")";
			markerInfoArray[1] = "Time: " + displayTime;
			markerInfoArray[2] = "Distance: "+displayDistance+dUnit;
			markerInfoArray[3] = "Elevation: "+displayAltitude+hUnit;
			markerInfoArray[4] = "Duration: "+displayCulTime;
			markerInfoArray[5] = "Speed: " +displaySpeed+sUnit;;
			return markerInfoArray;
		} else {
			for(int i=0;i<markerInfoArray.length;i++)
				markerInfoArray[i] = "";
			return markerInfoArray;
		}
	}

	public boolean checkInfoValid() {
		if(name.equals("") || markerTitle.equals("") || markerNumber<=-1)
			return false;
		else
			return true;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMarkerNumber() {
		return markerNumber;
	}

	public String getMarkerTitle() {
		return markerTitle;
	}

	public void setMarkerTitle(String markerTitle) {
		this.markerTitle = markerTitle;
	}

	public MapObjectType getType() {
		return type;
	}
}
