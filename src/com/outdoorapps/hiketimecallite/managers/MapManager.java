package com.outdoorapps.hiketimecallite.managers;

import java.util.ArrayList;
import java.util.HashMap;

import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.model.route.Route;
import com.outdoorapps.hiketimecallite.model.route.RouteData;
import com.outdoorapps.hiketimecallite.model.track.Track;
import com.outdoorapps.hiketimecallite.model.track.TrackData;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;
import com.outdoorapps.hiketimecallite.support.MapObjectHashMap;

/**
 * Manages Items on map
 * @author chbting
 *
 */
public class MapManager {

	private MainActivity main;
	private RouteData routeData;
	private TrackData trackData;
	private MapObjectHashMap<String,Polyline> polylineMap;
	private MapObjectHashMap<String,ArrayList<Marker>> markerMap;
	private GoogleMap mMap;
	private Polyline trackerPolyline;
	private Polyline drawingModePolyline;
	private ArrayList<Marker> drawingModeMarkerList;
	
	public static final float POLYLINE_WIDTH = 5;
	public static final String POLYLINE_COLOR = "#B300AAFF";
	public static final float TRACK_POLYLINE_WIDTH = 5;
	public static final String TRACK_POLYLINE_COLOR = "#B3FFFC33";
	public static int MAP_OBJECT_MARKER_LIMIT = 150;
	/**
	 * Called after routeData has been created
	 * @param main
	 * @param mMap
	 */
	public MapManager(MainActivity main) {
		this.main = main;
		routeData = main.getRouteData();
		trackData = main.getTrackData();
		polylineMap = new MapObjectHashMap<String,Polyline>();
		markerMap = new MapObjectHashMap<String,ArrayList<Marker>>();
	}
	
	public void setGoogleMap(GoogleMap mMap) {
		this.mMap = mMap;
	}

	/**
	 * Return a new polyline, the old one should be removed first
	 * @return
	 */
	public Polyline getTrackerPolyline() {
		PolylineOptions polylineOptions = new PolylineOptions();
		trackerPolyline = mMap.addPolyline(polylineOptions.color(Color.parseColor(TRACK_POLYLINE_COLOR)).width(TRACK_POLYLINE_WIDTH));
		return trackerPolyline;
	}

	public void removeTrackerPolyline() {
		if(trackerPolyline!=null)
			trackerPolyline.remove();
	}

	/**
	 * Remove the route of the given route name if it is currently on the map
	 * @param route
	 */
	public void hideRouteFromMap(String routeName) {
		MapObjectType type = MapObjectType.route;
		if(routeData.hideRouteFromMap(routeName)) { // avoid removing non-existing route
			polylineMap.get(routeName,type).remove();
			polylineMap.remove(routeName,type);
			ArrayList<Marker> markerList = markerMap.get(routeName,type);
			for(int i=0;i<markerList.size();i++)
				markerList.get(i).remove();
			markerMap.remove(routeName,type);
			if(main.getMarkerInfo().getName().equals(routeName)) {
				main.deselectMarker();
			}
		}
	}

	/**
	 * Add a route of the given name of map if it is currently not on the map
	 * @param route
	 */
	public void addRouteOnMap(String routeName) {
		if(routeData.addRouteOnMap(routeName)==true)
			showRouteOnMap(routeName);
	}

	/**
	 * Draw the route of the given name without changing routesOnMapList
	 * if a route has not been added, addRouteOnMap should be used instead
	 * @param route
	 */
	public void showRouteOnMap(String routeName) {
		MapObjectType type = MapObjectType.route;
		Route route = routeData.getRoute(routeName);
		HashMap<Marker,MarkerInfo> markerInfoMap = routeData.getMarkerMap();
		PolylineOptions polylineOptions = route.getPolylineOptions();
		ArrayList<MarkerOptions> markerOptionsList = route.getMarkerOptionsList();
		ArrayList<Marker> markerList = new ArrayList<Marker>();
		for(int i=0;i<markerOptionsList.size();i++) {
			MarkerOptions markerOptions = markerOptionsList.get(i);
			MarkerInfo info = new MarkerInfo(main,routeName,markerOptions.getTitle(),i,MapObjectType.route);
			Marker marker = mMap.addMarker(markerOptions);
			markerList.add(marker);
			markerInfoMap.put(marker, info);
		}
		Polyline polyline = mMap.addPolyline(polylineOptions.color(Color.parseColor(POLYLINE_COLOR)).width(POLYLINE_WIDTH));		
		polylineMap.put(routeName, type, polyline);
		markerMap.put(routeName, type, markerList);
	}

	/**
	 * Draw all the routes on routesOnMapList
	 */
	public void drawAllRouteOnMap() {
		ArrayList<String> routesOnMapList = routeData.getRoutesOnMapList();
		routeData.getMarkerMap().clear();
		for(int i=0;i<routesOnMapList.size();i++) {
			String routeName = routesOnMapList.get(i);
			showRouteOnMap(routeName);
		}
	}

	/**
	 * Add a track of the given name of map if it is currently not on the map
	 * @param track
	 */
	public void addTrackOnMap(String trackName) {
		if(trackData.addTrackOnMap(trackName)==true)
			showTrackOnMap(trackName);
	}

	/**
	 * Draw the track of the given name without changing tracksOnMapList
	 * if a track has not been added, addRouteOnMap should be used instead
	 * @param track
	 */
	public void showTrackOnMap(String trackName) {
		MapObjectType type = MapObjectType.track;
		Track track = trackData.getTrack(trackName);
		HashMap<Marker,MarkerInfo> markerInfoMap = trackData.getMarkerMap();
		PolylineOptions polylineOptions = track.getPolylineOptions();
		ArrayList<MarkerOptions> markerOptionsList = track.getMarkerOptionsList();
		ArrayList<Marker> markerList = new ArrayList<Marker>();
		
		int inc = 1;
		if(markerOptionsList.size()>MAP_OBJECT_MARKER_LIMIT) {
			inc = markerOptionsList.size()/MAP_OBJECT_MARKER_LIMIT;
		}
		
		int lastIndex = 0;
		for(int i=0;i<markerOptionsList.size();i+=inc) {
			MarkerOptions markerOptions = markerOptionsList.get(i);
			MarkerInfo info = new MarkerInfo(main,trackName,markerOptions.getTitle(),i,MapObjectType.track);
			Marker marker = mMap.addMarker(markerOptions);
			markerList.add(marker);
			markerInfoMap.put(marker, info);
			lastIndex = i;
		}
		// draw end marker
		if(lastIndex!=markerOptionsList.size()-1 && (markerOptionsList.size()-1)>0) {
			lastIndex = markerOptionsList.size()-1;
			MarkerOptions markerOptions = markerOptionsList.get(lastIndex);
			MarkerInfo info = new MarkerInfo(main,trackName,markerOptions.getTitle(),lastIndex,MapObjectType.track);
			Marker marker = mMap.addMarker(markerOptions);
			markerList.add(marker);
			markerInfoMap.put(marker, info);
		}			
		Polyline polyline = mMap.addPolyline(polylineOptions.color(Color.parseColor(TRACK_POLYLINE_COLOR)).width(TRACK_POLYLINE_WIDTH));		
		polylineMap.put(trackName, type, polyline);
		markerMap.put(trackName, type, markerList);
	}

	/**
	 * Draw all the tracks on tracksOnMapList
	 */
	public void drawAllTrackOnMap() {
		ArrayList<String> tracksOnMapList = trackData.getTracksOnMapList();
		trackData.getMarkerMap().clear();
		for(int i=0;i<tracksOnMapList.size();i++) {
			String trackName = tracksOnMapList.get(i);
			showTrackOnMap(trackName);
		}
	}
	
	/**
	 * Remove the track of the given track name if it is currently on the map
	 * @param trackName
	 */
	public void hideTrackFromMap(String trackName) {
		MapObjectType type = MapObjectType.track;
		if(trackData.hideTrackFromMap(trackName)) { // avoid removing non-existing track
			polylineMap.get(trackName,type).remove();
			polylineMap.remove(trackName,type);
			ArrayList<Marker> markerList = markerMap.get(trackName,type);
			for(int i=0;i<markerList.size();i++)
				markerList.get(i).remove();
			markerMap.remove(trackName,type);
			if(main.getMarkerInfo().getName().equals(trackName)) {
				main.deselectMarker();
			}
		}
	}
	
	public void drawDrawingModePolyline() {
		ArrayList<MarkerOptions> markerOptionsList = main.getDrawingMode().getMarkerOptionsList();
		drawingModeMarkerList = new ArrayList<Marker>();
		for(int i=0;i<markerOptionsList.size();i++)
			drawingModeMarkerList.add(mMap.addMarker(markerOptionsList.get(i)));		
		drawingModePolyline = mMap.addPolyline(main.getDrawingMode().getPolylineOptions());
	}
	
	public void removeDrawingModePolyline() {
		if(drawingModePolyline!=null)
			drawingModePolyline.remove();
		if(drawingModeMarkerList!=null) {
			for(int i=0;i<drawingModeMarkerList.size();i++)
				drawingModeMarkerList.get(i).remove();
		}
	}
}
