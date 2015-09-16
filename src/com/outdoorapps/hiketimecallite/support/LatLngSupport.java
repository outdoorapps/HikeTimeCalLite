package com.outdoorapps.hiketimecallite.support;

import java.util.ArrayList;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class LatLngSupport {
	
	/**
	 * Get the latitude and longitude bound of the route (assuming the 
	 * @return
	 */
	public static LatLngBounds getLatLngBounds(ArrayList<LatLng> latLngList) {
		Double maxLat, minLat, maxLon, minLon;
		maxLat = latLngList.get(0).latitude;
		minLat = latLngList.get(0).latitude;
		maxLon = latLngList.get(0).longitude;
		minLon = latLngList.get(0).longitude;

		for(int i=1;i<latLngList.size();i++) {
			Double lat = latLngList.get(i).latitude;
			Double lon = latLngList.get(i).longitude;
			if(lat>maxLat)
				maxLat = lat;
			if(lat<minLat)
				minLat = lat;
			if(lon>maxLon)
				maxLon = lon;
			if(lon<minLon)
				minLon = lon;
		}
		LatLng ne, sw;
		if(maxLon-minLon>180){
			// if the path crosses the antimeridian
			ne = new LatLng(maxLat,minLon);
			sw = new LatLng(minLat,maxLon);
		}
		else {
			ne = new LatLng(maxLat,maxLon);
			sw = new LatLng(minLat,minLon);			
		}
		return new LatLngBounds(sw,ne);
	}
}
