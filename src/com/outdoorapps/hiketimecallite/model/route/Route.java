package com.outdoorapps.hiketimecallite.model.route;

import java.util.ArrayList;

import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.model.route.Calculator.InvalidParametersException;
import com.outdoorapps.hiketimecallite.support.LatLngSupport;

public class Route {
	private ArrayList<RoutePoint> RPList;
	private PolylineOptions polylineOptions;
	private ArrayList<MarkerOptions> markerOptionsList;
	private String name;
	private double routeDistance, adjRouteDistance; /** Distances are in km */
	private double tripTime, returnTripTime, roundTripTime; /**Time are in minutes */
	private double routeElevationGain, routeElevationLoss; /** Elevation are in meters */	
	private double speed, ascendTimeIncrease, ascendHeightInput, descendTimeIncrease, descendHeightInput;
	private final String DELIMITER = ",";
	private boolean elevationRequestNeeded;

	/**
	 * Standard way of creating new route, if elevationRequestNeeded is true, modifyRouteData
	 * should be called later when elevation is requested
	 * @param name
	 * @param RPList
	 * @throws InvalidParametersException 
	 */
	public Route(String name,ArrayList<RoutePoint> RPList, double[] parametersList, boolean elevationRequestNeeded) throws InvalidParametersException {
		this.name = name;
		this.RPList = RPList;
		setParameters(parametersList);
		this.elevationRequestNeeded = elevationRequestNeeded;
		if(!elevationRequestNeeded) {
			createRouteInfo();			
			createPolylineOptions();
			createMarkerOptionsList();
		}
	}

	/**
	 * Create a header only route
	 * @param name
	 * @param data
	 * @param parameters
	 */
	public Route(String name, String data, double[] parameters) {
		this.name = name;
		setData(data);
		setParameters(parameters);
		elevationRequestNeeded = false;
	}

	/**
	 * Add a pre-calculated route
	 * @param RPList
	 * @param name
	 * @param data
	 * @param parameters
	 * @throws InvalidRoutePointListException 
	 */
	public Route(ArrayList<RoutePoint> RPList, String name, String data, double[] parameters) throws InvalidRoutePointListException {
		this.RPList = RPList;
		if(RPList==null || RPList.size()<2)
			throw new InvalidRoutePointListException();
		this.name = name;
		elevationRequestNeeded = false;
		setData(data);
		setParameters(parameters);	
		createPolylineOptions();
		createMarkerOptionsList();
	}

	private void setData(String data) {
		String[] dataList = data.split(DELIMITER);
		routeDistance = Double.parseDouble(dataList[0]);
		adjRouteDistance = Double.parseDouble(dataList[1]);
		tripTime = Double.parseDouble(dataList[2]);
		returnTripTime = Double.parseDouble(dataList[3]);
		roundTripTime = Double.parseDouble(dataList[4]);
		routeElevationGain = Double.parseDouble(dataList[5]);
		routeElevationLoss = Double.parseDouble(dataList[6]);
	}

	public ArrayList<RoutePoint> getRPList() {
		if(RPList==null) {
			// Recover RPList if needed
			RPList = MainActivity.main.getRouteData().recoverRPList(name);
			// set Route Info			
			try {
				createRouteInfo();
				createPolylineOptions();
				createMarkerOptionsList();
			} catch (InvalidParametersException e) {
				Toast.makeText(MainActivity.main, "Errors encountered during retrieving route, invalid parameters", Toast.LENGTH_SHORT).show();
			}									
		}
		return RPList;
	}

	public void changeRouteData(ArrayList<RoutePoint> RPList,double[] parameters) throws InvalidParametersException {
		this.RPList = RPList;
		setParameters(parameters);
		createRouteInfo();
		createPolylineOptions();
		createMarkerOptionsList();			
	}

	public void modifyElevationList(ArrayList<Double> elevationList) throws InvalidParametersException {
		for(int i=0;i<elevationList.size();i++) {
			RoutePoint rp = getRPList().get(i);
			rp.setElevation(elevationList.get(i));
		}
		createRouteInfo();
		createPolylineOptions();
		createMarkerOptionsList();
	}

	/**
	 * Calculate route distances and times with given parameters
	 * Should be called every time RPList or parameters changed
	 * @throws InvalidParametersException
	 */
	private void createRouteInfo() throws InvalidParametersException {
		
		Calculator.calculate(this.getParameters(), getRPList());

		double adjRouteDistance, routeElevationGain, routeElevationLoss;
		adjRouteDistance = 0;
		routeElevationGain = 0;
		routeElevationLoss = 0;

		RoutePoint startPoint = RPList.get(0);
		RoutePoint endPoint = RPList.get(RPList.size()-1);

		for(int i=0; i<RPList.size()-1; i++) {
			RoutePoint rp = RPList.get(i);
			RoutePoint rpNext = RPList.get(i+1);
			adjRouteDistance += rp.getAdjDistanceToNextRP();
			double elevationChange = rpNext.getElevation() - rp.getElevation();
			if(elevationChange>=0)
				routeElevationGain += elevationChange;
			else
				routeElevationLoss -= elevationChange;
		}

		this.routeDistance = endPoint.getCulminativeDistance();
		this.adjRouteDistance = adjRouteDistance;
		this.tripTime = endPoint.getCulminativeTime();
		this.returnTripTime = startPoint.getCulminativeRtnTime() - endPoint.getCulminativeTime();
		this.roundTripTime = startPoint.getCulminativeRtnTime();
		this.routeElevationGain = routeElevationGain;
		this.routeElevationLoss = routeElevationLoss;
	}

	private void createPolylineOptions() {
		polylineOptions = new PolylineOptions();
		polylineOptions.addAll(getLatLngList());
	}

	/**
	 * Create a list of marker with predefined icons
	 */
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
					marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));
					marker.title("Point " + (i+1));
				}
			}
			markerOptionsList.add(marker);
		}
	}

	private void setParameters(double[] parameters) {
		speed = parameters[0];
		ascendTimeIncrease = parameters[1];
		ascendHeightInput = parameters[2];
		descendTimeIncrease = parameters[3];
		descendHeightInput = parameters[4];
	}

	public double[] getParameters() {
		double[] parametersList = new double[5];
		parametersList[0] = speed;
		parametersList[1] = ascendTimeIncrease;
		parametersList[2] = ascendHeightInput;
		parametersList[3] = descendTimeIncrease;
		parametersList[4] = descendHeightInput;		
		return parametersList;
	}

	public ArrayList<LatLng> getLatLngList() {
		getRPList(); // make sure RPList is not null
		ArrayList<LatLng> latLngList = new ArrayList<LatLng>();
		for(int i=0;i<RPList.size();i++)
			latLngList.add(RPList.get(i).getLatLng());
		return latLngList;
	}

	public String getDataString() {
		return routeDistance + DELIMITER
				+ adjRouteDistance + DELIMITER
				+ tripTime + DELIMITER
				+ returnTripTime + DELIMITER
				+ roundTripTime + DELIMITER
				+ routeElevationGain + DELIMITER
				+ routeElevationLoss;
	}

	public String getParametersString() {
		return speed + DELIMITER
				+ ascendTimeIncrease + DELIMITER
				+ ascendHeightInput + DELIMITER
				+ descendTimeIncrease + DELIMITER
				+ descendHeightInput;
	}

	@Override
	public String toString() {
		return "Route Distance (km): " + routeDistance + "\n"
				+ "Route Distance (Adjusted with slope) (km): " + adjRouteDistance + "\n"
				+ "Route Time (mins): " + tripTime + "\n"
				+ "Return Route Time (mins): " + returnTripTime + "\n"
				+ "Total Time (mins): " + roundTripTime + "\n"
				+ "Route Elevation Gain (m): " + routeElevationGain + "\n"
				+ "Route Elevation Loss (m): " + routeElevationLoss;
	}

	public LatLngBounds getLatLngBounds() {
		return LatLngSupport.getLatLngBounds(getLatLngList());
	}

	public PolylineOptions getPolylineOptions() {
		if(polylineOptions==null)
			createPolylineOptions();
		return polylineOptions;
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

	public boolean isElevationRequestNeeded() {
		return elevationRequestNeeded;
	}

	public void setElevationRequestNeeded(boolean elevationRequestNeeded) {
		this.elevationRequestNeeded = elevationRequestNeeded;
	}

	public double getTripTime() {
		return tripTime;
	}

	public double getRouteDistance() {
		return routeDistance;
	}

	public double getRouteElevationGain() {
		return routeElevationGain;
	}

	public double getRouteElevationLoss() {
		return routeElevationLoss;
	}

	public double getRouteAdjDistance() {
		return adjRouteDistance;
	}

	public double getRoundTripTime() {
		return roundTripTime;
	}

	public double getReturnTripTime() {
		return returnTripTime;
	}

	public double getSpeedInput() {
		return speed;
	}

	public void setSpeedInput(double speedInput) {
		this.speed = speedInput;
	}

	public double getAscendTimeIncrease() {
		return ascendTimeIncrease;
	}

	public void setAscendTimeIncrease(double ascendTimeIncrease) {
		this.ascendTimeIncrease = ascendTimeIncrease;
	}

	public double getAscendHeightInput() {
		return ascendHeightInput;
	}

	public void setAscendHeightInput(double ascendHeightInput) {
		this.ascendHeightInput = ascendHeightInput;
	}

	public double getDescendTimeIncrease() {
		return descendTimeIncrease;
	}

	public void setDescendTimeIncrease(double descendTimeIncrease) {
		this.descendTimeIncrease = descendTimeIncrease;
	}

	public double getDescendHeightInput() {
		return descendHeightInput;
	}

	public void setDescendHeightInput(double descendHeightInput) {
		this.descendHeightInput = descendHeightInput;
	}

	@SuppressWarnings("serial")
	public static class InvalidRoutePointListException extends Exception {
		public InvalidRoutePointListException () {

		}
	}
}
