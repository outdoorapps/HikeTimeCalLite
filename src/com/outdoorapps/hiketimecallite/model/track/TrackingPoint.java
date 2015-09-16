package com.outdoorapps.hiketimecallite.model.track;

import android.location.Location;

public class TrackingPoint {

	protected Location location;
	protected String time;
	
	public TrackingPoint(Location location, String time) {
		this.location = location;
		this.time = time;
	}

	public Location getLocation() {
		return location;
	}

	public void setTime(String time) {
		this.time = time;
	}
	
	public String getTime() {
		return time;
	}
	
	public String toString() {
		return location + ",time=" + time;
	}
}
