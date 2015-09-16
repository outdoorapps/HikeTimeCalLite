package com.outdoorapps.hiketimecallite.model.track;

import java.text.ParseException;
import java.util.ArrayList;

import android.location.Location;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.model.route.Calculator;
import com.outdoorapps.hiketimecallite.support.Time;
import com.outdoorapps.hiketimecallite.support.constants.Constants;

public class Track {

	private String name, date;
	private ArrayList<TrackPoint> TPList;
	private double distance; /** In km */
	private long duration; /** In secs */
	private double maxAltitude, minAltitude, elevationGain, elevationLoss;
	private ArrayList<Float> speedList, accuracyList;
	private float avgSpeed;

	/** in meters*/
	private PolylineOptions polylineOptions;
	private ArrayList<MarkerOptions> markerOptionsList;
	public static final int ELE_SAMPLING_FREQ = 20; /** every 20 points*/

	public Track(String name, ArrayList<TrackPoint> TPList) {
		this.name = name;
		this.TPList = TPList;
		// calculate distance and duration
		TrackPoint lastTP = TPList.get(TPList.size()-1);
		distance = lastTP.getCulminatedDistance();
		duration = lastTP.getCulminatedTime();
		setTrackInfo();
	}

	/**
	 * Create a header only track
	 * @param name
	 * @param distance
	 * @param duration
	 */
	public Track(String name, double distance, long duration, double elevationGain, String date) {
		this.name = name;
		this.distance = distance;
		this.duration = duration;
		this.elevationGain = elevationGain;
		this.date = date;
	}

	public static ArrayList<TrackPoint> convertToTPList(ArrayList<TrackingPoint> trackingPTList) {
		ArrayList<TrackPoint> TPList = new ArrayList<TrackPoint>();
		long culminatedTime = 0;
		double culminatedDistance = 0;		

		TrackingPoint trackingPT1 = trackingPTList.get(0);		//TODO bound checking
		TrackPoint tp = new TrackPoint(trackingPT1,culminatedTime,culminatedDistance);
		TPList.add(tp);

		for(int i=1;i<trackingPTList.size();i++) {
			trackingPT1 = trackingPTList.get(i-1);
			TrackingPoint trackingPT2 = trackingPTList.get(i);
			// Get time Increase
			String time1 = trackingPT1.getTime();
			String time2 = trackingPT2.getTime();
			try {
				culminatedTime += Time.getTimeDifference(time1, time2);
			} catch (ParseException e) {}

			// Get distance increase
			Location location1 = trackingPT1.getLocation();
			Location location2 = trackingPT2.getLocation();
			double lat1 = location1.getLatitude();
			double lon1 = location1.getLongitude();
			double lat2 = location2.getLatitude();
			double lon2 = location2.getLongitude();
			culminatedDistance += Calculator.getDistance(lat1, lon1, lat2, lon2);
			tp = new TrackPoint(trackingPT2,culminatedTime,culminatedDistance);
			TPList.add(tp);
		}
		return TPList;
	}

	public static ArrayList<TrackingPoint> convertToTrackingPTList(ArrayList<TrackPoint> TPList) {
		ArrayList<TrackingPoint> trackingPTList = new ArrayList<TrackingPoint>();
		for(int i=0;i<TPList.size();i++) {
			TrackPoint tp = TPList.get(i);
			TrackingPoint trackingPT = new TrackingPoint(tp.getLocation(),tp.getTime());
			trackingPTList.add(trackingPT);
		}
		return trackingPTList;
	}

	public ArrayList<LatLng> getLatLngList() {
		getTPList();// check TPList
		ArrayList<LatLng> latLngList = new ArrayList<LatLng>();
		for(int i=0;i<TPList.size();i++) {
			Location location = TPList.get(i).getLocation();
			latLngList.add(new LatLng(location.getLatitude(),location.getLongitude()));
		}
		return latLngList;
	}

	private void createPolylineOptions() {
		polylineOptions = new PolylineOptions();
		polylineOptions.addAll(getLatLngList());
	}

	public PolylineOptions getPolylineOptions() {	
		if(polylineOptions==null)
			createPolylineOptions();
		return polylineOptions;
	}

	private void createMarkerOptionsList() {
		ArrayList<LatLng> latLngList = getLatLngList();
		markerOptionsList = new ArrayList<MarkerOptions>();
		for(int i=0;i<latLngList.size();i++) {
			LatLng position = latLngList.get(i);
			MarkerOptions marker = new MarkerOptions();
			marker.position(position);
			marker.anchor((float) 0.5, (float) 0.5);		
			if(i==0) {
				marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_start));
				marker.title("Start");				
			} else {
				if(i==latLngList.size()-1) {
					marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_end));
					marker.title("End");
				}
				else {
					marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker3));
					marker.title("Point " + (i+1));
				}
			}
			markerOptionsList.add(marker);
		}
	}

	public ArrayList<MarkerOptions> getMarkerOptionsList() {
		if(markerOptionsList==null)
			createMarkerOptionsList();
		return markerOptionsList;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<TrackPoint> getTPList() {
		if(TPList==null) {
			// Recover TPList if needed
			TPList = MainActivity.main.getTrackData().recoverTPList(name);
			setTrackInfo();
		}
		return TPList;
	}

	/**
	 * Set TPList, change related values as well
	 * @param TPList
	 */
	public void setTPList(ArrayList<TrackPoint> TPList) {
		if(TPList.size()>0) {
			this.TPList = TPList;
			TrackPoint lastTP = TPList.get(TPList.size()-1);
			distance = lastTP.getCulminatedDistance();
			duration = lastTP.getCulminatedTime();
			createPolylineOptions();
			createMarkerOptionsList();
			setTrackInfo();
		}
	}

	/**
	 * Set elevationGain, elevationLoss, maxAltitude, minAltitude and avgSpeed
	 */
	private void setTrackInfo() {
		date = TPList.get(0).getTime().split("T")[0];

		elevationGain = 0;
		elevationLoss = 0;
		Location location1, location2;
		double altitude1, altitude2;

		location1 = TPList.get(0).getLocation();
		maxAltitude = location1.getAltitude();
		minAltitude = location1.getAltitude();

		speedList = new ArrayList<Float>();
		accuracyList = new ArrayList<Float>();
		speedList.add(location1.getSpeed());
		accuracyList.add(location1.getAccuracy());

		// Set elevation gain and loss
		int sampleFreq;
		if(TPList.size()<ELE_SAMPLING_FREQ*5)
			sampleFreq = 1;
		else
			sampleFreq = ELE_SAMPLING_FREQ;			
		for(int i=sampleFreq;i<TPList.size();i+=sampleFreq) {
			location1 = TPList.get(i-sampleFreq).getLocation();
			location2 = TPList.get(i).getLocation();
			altitude1 = location1.getAltitude();
			altitude2 = location2.getAltitude();
			double elevationDiff = altitude2 - altitude1;
			if(elevationDiff>0)
				elevationGain += elevationDiff;
			else
				elevationLoss += Math.abs(elevationDiff);
		}

		// Set other info
		for(int i=1;i<TPList.size();i++) {
			location1 = TPList.get(i-1).getLocation();
			altitude1 = location1.getAltitude();
			location2 = TPList.get(i).getLocation();
			altitude2 = location2.getAltitude();
			if(altitude1>maxAltitude)
				maxAltitude = altitude1;
			if(altitude1<minAltitude)
				minAltitude = altitude1;

			speedList.add(location1.getSpeed());
			accuracyList.add(location1.getAccuracy());
		}
		this.avgSpeed = (float) (distance/(double)(duration/Constants.SECS_PER_HOUR));
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public double getElevationGain() {
		return elevationGain;
	}

	public double getElevationLoss() {
		return elevationLoss;
	}

	public double getMaxAltitude() {
		return maxAltitude;
	}

	public double getMinAltitude() {
		return minAltitude;
	}

	public float getAvgSpeed() {
		return avgSpeed;
	}

	public ArrayList<Float> getSpeedList() {
		return speedList;
	}

	public ArrayList<Float> getAccuracyList() {
		return accuracyList;
	}

	public String getDate() {
		return date;
	}

}
