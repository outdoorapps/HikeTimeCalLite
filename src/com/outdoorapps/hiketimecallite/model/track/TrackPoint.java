package com.outdoorapps.hiketimecallite.model.track;


/**
 * A more detailed version of tracking point, contains more information
 * @author chbting
 *
 */
public class TrackPoint extends TrackingPoint {	

	private long culminatedTime;
	private double culminatedDistance;
	
	public TrackPoint(TrackingPoint trackingPT, long culminatedTime, double culminatedDistance) {
		super(trackingPT.getLocation(), trackingPT.getTime());
		this.culminatedTime = culminatedTime;
		this.culminatedDistance = culminatedDistance;
	}
	
	public String toString() {
		return location + ",time=" + time;
	}

	public double getCulminatedDistance() {
		return culminatedDistance;
	}

	public void setCulminatedDistance(double culminatedDistance) {
		this.culminatedDistance = culminatedDistance;
	}

	public long getCulminatedTime() {
		return culminatedTime;
	}

	public void setCulminatedTime(int culminatedTime) {
		this.culminatedTime = culminatedTime;
	}
}
