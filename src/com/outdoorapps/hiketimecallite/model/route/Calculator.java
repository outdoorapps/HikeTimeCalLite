package com.outdoorapps.hiketimecallite.model.route;
import java.util.ArrayList;

import com.outdoorapps.hiketimecallite.support.constants.Constants;


/**
 * All inner calculations are in metric
 * @author chbting
 *
 */
public class Calculator {

	/**
	 * Inner calculation in metric, speed in km/h, elevation in meter, time in mins
	 * @param parametersList
	 * @param RPList
	 * @throws InvalidParametersException
	 */
	
	public static void calculate(double[] parametersList, ArrayList<RoutePoint> RPList) throws InvalidParametersException {		
		double speed =  parametersList[0];
		double ascendTimeIncrease = parametersList[1];
		double ascendHeight = parametersList[2];
		double descendTimeIncrease = parametersList[3];
		double descendHeight = parametersList[4];

		if(speed<=0 || ascendTimeIncrease<=0 || ascendHeight<=0 || descendHeight<=0)
			throw new InvalidParametersException();

		double ascendConstant, descendConstant;
		ascendConstant = ascendTimeIncrease/ascendHeight;
		descendConstant = descendTimeIncrease/descendHeight;

		double culminativeDistance = 0;
		double culminativeTime = 0;
		ArrayList<Double> timeToPreviousRPArrayList = new ArrayList<Double>();

		RPList.get(0).setCulminativeTime(0d);
		RPList.get(0).setCulminativeDistance(0d);
		timeToPreviousRPArrayList.add(0d);

		for(int i=0; i<RPList.size()-1; i++) {
			RoutePoint rp1 = RPList.get(i);
			RoutePoint rp2 = RPList.get(i+1);
			double distance = getDistance(rp1.getLatLng().latitude,rp1.getLatLng().longitude,rp2.getLatLng().latitude,rp2.getLatLng().longitude);
			culminativeDistance += distance;
			rp2.setCulminativeDistance(culminativeDistance);

			double elevationDiff = rp2.getElevation()-rp1.getElevation();

			double adjDistance = Math.sqrt(Math.pow(distance, 2)+Math.pow(elevationDiff/Constants.KM_TO_M, 2));
			rp1.setAdjDistanceToNextRP(adjDistance);

			// Calculate timeToNextRP for rp1 and timeToPreviousRP for rp2
			double timeToNextRP = distance/speed*Constants.MINS_PER_HOUR;
			double timeToPreviousRP = timeToNextRP;

			if(elevationDiff>0) {
				timeToNextRP += elevationDiff*ascendConstant;
				timeToPreviousRP += Math.abs(elevationDiff)*descendConstant;
			} else if(elevationDiff<0) {				
				timeToNextRP += Math.abs(elevationDiff)*descendConstant;
				timeToPreviousRP += Math.abs(elevationDiff)*ascendConstant;
			}
			culminativeTime += timeToNextRP;
			rp2.setCulminativeTime(culminativeTime);
			timeToPreviousRPArrayList.add(timeToPreviousRP);

		}
		// set return trip values
		RoutePoint endPoint = RPList.get(RPList.size()-1);
		double culminativeRtnDistance = endPoint.getCulminativeDistance();
		double culminativeRtnTime = endPoint.getCulminativeTime();

		endPoint.setCulminativeRtnDistance(culminativeRtnDistance);
		endPoint.setCulminativeRtnTime(culminativeRtnTime);

		for(int i=RPList.size()-1; i>0; i--) {
			RoutePoint rp1 = RPList.get(i);
			RoutePoint rp2 = RPList.get(i-1);

			double timeToPreviousRP = timeToPreviousRPArrayList.get(i);
			double distanceToPreviousRP = rp1.getCulminativeDistance() - rp2.getCulminativeDistance();

			culminativeRtnTime += timeToPreviousRP;
			culminativeRtnDistance += distanceToPreviousRP;

			rp2.setCulminativeRtnTime(culminativeRtnTime);
			rp2.setCulminativeRtnDistance(culminativeRtnDistance);
		}
	}

	public static double[] calculate(double[] parametersList, double[] inputList) throws InvalidParametersException {
		double distance =  inputList[0];
		double elevation = inputList[1];

		double speed =  parametersList[0];
		double ascendTimeIncrease = parametersList[1];
		double ascendHeight = parametersList[2];
		double descendTimeIncrease = parametersList[3];
		double descendHeight = parametersList[4];

		double tripTime, returnTripTime;
		
		// Calculate
		if(elevation>=0) {
			tripTime = distance/speed*Constants.MINS_PER_HOUR + elevation*ascendTimeIncrease/ascendHeight;
			returnTripTime = distance/speed*Constants.MINS_PER_HOUR + elevation*descendTimeIncrease/descendHeight;
		} else {
			tripTime = distance/speed*Constants.MINS_PER_HOUR + Math.abs(elevation)*descendTimeIncrease/descendHeight;
			returnTripTime = distance/speed*Constants.MINS_PER_HOUR + Math.abs(elevation)*ascendTimeIncrease/ascendHeight;
		}
	
		double[] resultArray = new double[5];
		resultArray[0] = distance; // Trip distance
		resultArray[1] = distance*2; // Round Trip distance
		resultArray[2] = tripTime;
		resultArray[3] = returnTripTime;
		resultArray[4] = tripTime + returnTripTime; // Total time

		return resultArray;
	}

	/**
	 * longitude and latitude in WGS84
	 * @param lat1
	 * @param lon1
	 * @param lat2
	 * @param lon2
	 * @return distance in km
	 */
	public static double getDistance(double lat1,double lon1,double lat2,double lon2) {
		double dLat = degreeToRadian(lat2-lat1);
		double dLon = degreeToRadian(lon2-lon1); 
		double a = Math.sin(dLat/2)*Math.sin(dLat/2) +
				Math.cos(degreeToRadian(lat1)) * Math.cos(degreeToRadian(lat2)) * 
				Math.sin(dLon/2) * Math.sin(dLon/2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		double distance = Constants.RADIUS_OF_EARTH * c;		
		return distance;
	}

	private static double degreeToRadian(double degree) {
		return degree*(Math.PI/180);
	}

	@SuppressWarnings("serial")
	public static class InvalidParametersException extends Exception {

	}
}
