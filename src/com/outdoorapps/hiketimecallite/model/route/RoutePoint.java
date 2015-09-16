package com.outdoorapps.hiketimecallite.model.route;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class RoutePoint implements Comparable<RoutePoint>, Parcelable {
	private LatLng latLng; /** Longitude and latitude are in WGS84 format */
	private double elevation; /** Elevation is in meters */	
	private Double adjDistanceToNextRP; 
	private Double culminativeTime, culminativeRtnTime;  /** Times are in minutes */
	private Double culminativeDistance, culminativeRtnDistance; /** Distances are in km */
	public final static String DELIMITER = ",";
	
	public RoutePoint(LatLng latLng, double elevation) {
		this.latLng = latLng;
		this.elevation = elevation;
		culminativeTime = null;
		culminativeRtnTime = null;
		culminativeDistance = null;
		culminativeRtnDistance = null;
		adjDistanceToNextRP = null;
	}
	
	public RoutePoint(LatLng latLng, double elevation, String data) {
		this.latLng = latLng;
		this.elevation = elevation;
		String[] dataList = data.split(DELIMITER);
		culminativeTime = parseToDouble(dataList[0]);
		culminativeRtnTime = parseToDouble(dataList[1]);
		culminativeDistance = parseToDouble(dataList[2]);
		culminativeRtnDistance = parseToDouble(dataList[3]);
		adjDistanceToNextRP = parseToDouble(dataList[4]);
	}
	
	public Double parseToDouble(String s) {
		if(s.equals("null"))
			return null;
		else
			return Double.parseDouble(s);
	}
	
	public RoutePoint(Parcel in) {
		readFromParcel(in);		
	}
	
	public String getDataString() {
		return culminativeTime + DELIMITER
				+ culminativeRtnTime + DELIMITER
				+ culminativeDistance + DELIMITER
				+ culminativeRtnDistance + DELIMITER
				+ adjDistanceToNextRP;
	}

	@Override
	public int compareTo(RoutePoint rp2) {
		if(latLng.longitude==rp2.getLatLng().longitude && latLng.latitude==rp2.getLatLng().latitude && elevation==rp2.getElevation())
			return 0;
		else
			return -1;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(latLng, flags);
		dest.writeDouble(elevation);
		dest.writeValue(culminativeTime);
		dest.writeValue(culminativeRtnTime);
		dest.writeValue(culminativeDistance);
		dest.writeValue(culminativeRtnDistance);
		dest.writeValue(adjDistanceToNextRP);
	}

	private void readFromParcel(Parcel in) {
		latLng = (LatLng) in.readValue(null);
		elevation = in.readDouble();
		culminativeTime = (Double) in.readValue(null);
		culminativeRtnTime = (Double) in.readValue(null);
		culminativeDistance = (Double) in.readValue(null);
		culminativeRtnDistance = (Double) in.readValue(null);
		adjDistanceToNextRP = (Double) in.readValue(null);
	}

	public static final Parcelable.Creator<RoutePoint> CREATOR
	= new Parcelable.Creator<RoutePoint>() {
		@Override
		public RoutePoint createFromParcel(Parcel in) {
			return new RoutePoint(in);
		}

		@Override
		public RoutePoint[] newArray(int size) {
			return new RoutePoint[size];
		}

	};
	
	public double getElevation() {
		return elevation;
	}
	
	public void setElevation(double elevation) {
		this.elevation = elevation;
	}
	
	public Double getCulminativeTime() {
		return culminativeTime;
	}

	public void setCulminativeTime(Double culminativeTime) {
		this.culminativeTime = culminativeTime;
	}

	public Double getCulminativeRtnTime() {
		return culminativeRtnTime;
	}

	public void setCulminativeRtnTime(Double culminativeRtnTime) {
		this.culminativeRtnTime = culminativeRtnTime;
	}

	public Double getCulminativeDistance() {
		return culminativeDistance;
	}

	public void setCulminativeDistance(Double culminativeDistance) {
		this.culminativeDistance = culminativeDistance;
	}

	public Double getCulminativeRtnDistance() {
		return culminativeRtnDistance;
	}

	public void setCulminativeRtnDistance(Double culminativeRtnDistance) {
		this.culminativeRtnDistance = culminativeRtnDistance;
	}

	@Override
	public String toString() {
		return getLatLng() + DELIMITER
				+elevation + DELIMITER
				+getDataString();
	}

	public Double getAdjDistanceToNextRP() {
		return adjDistanceToNextRP;
	}

	public void setAdjDistanceToNextRP(Double adjDistanceToNextRP) {
		this.adjDistanceToNextRP = adjDistanceToNextRP;
	}

	public LatLng getLatLng() {
		return latLng;
	}

	public void setLatLng(LatLng latLng) {
		this.latLng = latLng;
	}
}
