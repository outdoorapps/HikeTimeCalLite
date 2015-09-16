package com.outdoorapps.hiketimecallite.model.route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.SelectRouteActivity;
import com.outdoorapps.hiketimecallite.asynctasks.GetElevationResponseInterface;
import com.outdoorapps.hiketimecallite.asynctasks.GetElevationTask;
import com.outdoorapps.hiketimecallite.database.RouteDatabaseOpenHelper;
import com.outdoorapps.hiketimecallite.managers.MarkerInfo;
import com.outdoorapps.hiketimecallite.model.route.Calculator.InvalidParametersException;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;

public class RouteData implements GetElevationResponseInterface {

	private MainActivity main;
	private ArrayList<Route> routeList;
	private ArrayList<String> routesOnMapList, checkedRouteNames; /** Save checked route names */
	private HashMap<Marker,MarkerInfo> markerMap;
	private HashMap<GetElevationTask,Route> editTaskMap, newRouteMap; /** New Route Map stoures routes currently being added, requesting elevation*/
	private HashMap<GetElevationTask,ArrayList<LatLng>> editTaskLatLngMap;
	private HashMap<GetElevationTask,Boolean> updateElevationModeMap, showMap;
	private RouteDatabaseOpenHelper routeDBHelper;

	public RouteData(MainActivity main) {
		this.main = main;
		routeDBHelper = new RouteDatabaseOpenHelper(main);
		checkedRouteNames = new ArrayList<String>();
		recoverRouteHeaderList();
		recoverRouteOnMapList();
		markerMap = new HashMap<Marker,MarkerInfo>();
		editTaskMap = new HashMap<GetElevationTask,Route>();
		newRouteMap = new HashMap<GetElevationTask,Route>();
		editTaskLatLngMap = new HashMap<GetElevationTask,ArrayList<LatLng>>();
		updateElevationModeMap = new HashMap<GetElevationTask,Boolean>();
		showMap = new HashMap<GetElevationTask,Boolean>();
	}

	private void recoverRouteHeaderList() {
		routeList = new ArrayList<Route>();
		SQLiteDatabase db = routeDBHelper.getReadableDatabase();
		String sql = "SELECT * FROM " + RouteDatabaseOpenHelper.ROUTE_TABLE_NAME + ";";
		Cursor cursor = db.rawQuery(sql, null);

		if (cursor.moveToFirst()) {
			do {
				String routeName = cursor.getString(0);
				String data = cursor.getString(1);
				String[] parametersStrList = cursor.getString(2).split(RoutePoint.DELIMITER);
				double[] parametersList = new double[parametersStrList.length];
				for(int i=0;i<parametersStrList.length;i++) 
					parametersList[i] = Double.parseDouble(parametersStrList[i]);
				
				Route route = new Route(routeName,data,parametersList);
				routeList.add(route);
			} while (cursor.moveToNext());				
		}
		db.close();
	}
	
	public ArrayList<RoutePoint> recoverRPList(String routeName) {
		SQLiteDatabase db = routeDBHelper.getReadableDatabase();
		ArrayList<RoutePoint> RPList = new ArrayList<RoutePoint>();
		Cursor cursor = db.query(RouteDatabaseOpenHelper.ROUTEPOINT_TABLE_NAME, 
				null,"ROUTENAME=?", new String[]{routeName}, 
				null, null,	null);
		if (cursor.moveToFirst()) {
			do {
				LatLng latLng = new LatLng(cursor.getDouble(1),cursor.getDouble(2));
				double elevation = cursor.getDouble(3);
				String dataRP = cursor.getString(4);
				RoutePoint rp = new RoutePoint(latLng,elevation,dataRP);
				RPList.add(rp);
			} while (cursor.moveToNext());	
		}
		return RPList;
	}

	private void recoverRouteOnMapList() {
		routesOnMapList = new ArrayList<String>();
		SQLiteDatabase db = routeDBHelper.getWritableDatabase();
		String sql = "SELECT * FROM " + RouteDatabaseOpenHelper.ROUTE_ON_MAP_TABLE_NAME + ";";
		Cursor cursor = db.rawQuery(sql, null);

		if (cursor.moveToFirst()) {
			do {
				String routeName = cursor.getString(0);
				routesOnMapList.add(routeName);
			} while (cursor.moveToNext());
		}
		db.close();
	}

	/**
	 * Add route to database if not elevation request is needed
	 * Request elevation if needed
	 * @param route
	 * @param showOnMap
	 */
	@SuppressWarnings("unchecked")
	public void addNewRoute(Route route, boolean showOnMap) {
		boolean elevationRequestNeeded = route.isElevationRequestNeeded();
		if(elevationRequestNeeded==false)
			addRoute(route,showOnMap);
		else {
			GetElevationTask task = new GetElevationTask();
			task.setType(MapObjectType.route);
			task.setTag(route.getName());
			newRouteMap.put(task, route);
			showMap.put(task, showOnMap);
			task.setDelegate(this);
			task.execute(route.getLatLngList());
		}
	}

	/**
	 * Add a route with elevation requested, shows on map is necessary
	 * @param route
	 * @param showOnMap
	 */
	private void addRoute(Route route, boolean showOnMap) {
		routeList.add(route);
		saveRouteToDatabase(route); // update database immediately
		if(showOnMap==true)
			main.getMapManager().addRouteOnMap(route.getName());

		// update select route list if it is open			
		if(SelectRouteActivity.thisActivity!=null)
			SelectRouteActivity.notifyDataSetChanged();

		Toast.makeText(main, "Route added", Toast.LENGTH_SHORT).show();
	}


	public void saveRouteToDatabase(Route route) {
		SQLiteDatabase db = routeDBHelper.getWritableDatabase();
		String routeName = route.getName();

		db.delete(RouteDatabaseOpenHelper.ROUTE_TABLE_NAME, "ROUTENAME=?", new String[]{routeName});
		db.delete(RouteDatabaseOpenHelper.ROUTEPOINT_TABLE_NAME, "ROUTENAME=?", new String[]{routeName});

		ContentValues values = new ContentValues(); 
		values.put("ROUTENAME", routeName);
		values.put("DATA", route.getDataString());
		values.put("PARAMETERS", route.getParametersString());
		db.insert(RouteDatabaseOpenHelper.ROUTE_TABLE_NAME, null, values);

		db.beginTransaction();
		try {
			ArrayList<RoutePoint> RPList = route.getRPList();
			for(int j=0;j<RPList.size();j++) {
				RoutePoint rp = RPList.get(j);
				ContentValues RPValues = new ContentValues(); 
				RPValues.put("ROUTENAME", routeName);
				RPValues.put("LATITUDE", rp.getLatLng().latitude);
				RPValues.put("LONGITUDE", rp.getLatLng().longitude);
				RPValues.put("ELEVATION", rp.getElevation());
				RPValues.put("DATA", rp.getDataString());
				db.insert(RouteDatabaseOpenHelper.ROUTEPOINT_TABLE_NAME, null, RPValues);
			}	
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();
	}

	/**
	 * Add a route of the given name on map
	 * Actual actions: check if the route of the given name is currently marked
	 * as on map, if so, marked it (put its name in routesOnMapList), update
	 * routesOnMapList in database and return true to notify further actions
	 * are needed. Otherwise do nothing and return false.
	 * @param routeName
	 */
	public boolean addRouteOnMap(String routeName) {
		Route route = getRoute(routeName);
		if(route!=null) {
			// Make sure the route name is only on the list once
			if(!routesOnMapList.contains(routeName)) {
				routesOnMapList.add(routeName);
				// update database
				SQLiteDatabase db = routeDBHelper.getWritableDatabase();
				ContentValues values = new ContentValues(); 
				values.put("ROUTENAME", routeName);
				db.insert(RouteDatabaseOpenHelper.ROUTE_ON_MAP_TABLE_NAME, null, values);
				db.close();
				return true;
			}
			else
				return false;
		} else
			return false;
	}

	/**
	 * Remove the route of the given route name and hide it from map as needed,
	 * return true if the route is removed, false otherwise
	 * @param routeName
	 * @return
	 */
	public boolean removeRoute(String routeName) {
		if(checkedRouteNames.contains(routeName))
			checkedRouteNames.remove(routeName);
		Route route = getRoute(routeName);
		boolean result = routeList.remove(route);
		if(result==true) {
			main.getMapManager().hideRouteFromMap(routeName);
			hideRouteFromMap(routeName);
			SQLiteDatabase db =  routeDBHelper.getWritableDatabase();
			db.delete(RouteDatabaseOpenHelper.ROUTE_TABLE_NAME, "ROUTENAME=?", new String[]{routeName});
			db.delete(RouteDatabaseOpenHelper.ROUTEPOINT_TABLE_NAME, "ROUTENAME=?", new String[]{routeName});
			db.close();
		}
		return result;
	}

	/**
	 * Hide a route from map
	 * Actual actions: check if a route is currently marked as on map (contained
	 * in routesOnMapList), if so, remove it and update database and return true
	 * to notify caller to update its map
	 * @param routeName
	 * @return
	 */
	public boolean hideRouteFromMap(String routeName) {
		boolean result = routesOnMapList.remove(routeName);
		if(result==true) { // remove from database
			SQLiteDatabase db =  routeDBHelper.getWritableDatabase();
			db.delete(RouteDatabaseOpenHelper.ROUTE_ON_MAP_TABLE_NAME, "ROUTENAME=?", new String[]{routeName});
			db.close();
		}
		return result;
	}

	/**
	 * Modify data (parameters or RPList) of the route of the given name,
	 * update routeList and database
	 * @param routeName
	 * @param RPList
	 * @param parametersList
	 * @throws InvalidParametersException
	 */
	public void modifyRoute(String routeName, ArrayList<RoutePoint> RPList, double[] parametersList) throws InvalidParametersException {
		Route route = getRoute(routeName);
		route.changeRouteData(RPList,parametersList);

		// Modify route data and parameters in database
		SQLiteDatabase db =  routeDBHelper.getWritableDatabase();

		ContentValues values = new ContentValues(); 
		values.put("ROUTENAME", routeName);
		values.put("DATA", route.getDataString());
		values.put("PARAMETERS", route.getParametersString());		
		db.update(RouteDatabaseOpenHelper.ROUTE_TABLE_NAME, values, "ROUTENAME=?", new String[]{routeName});

		// Re-create RPList in database
		db.delete(RouteDatabaseOpenHelper.ROUTEPOINT_TABLE_NAME, "ROUTENAME=?", new String[]{routeName});

		db.beginTransaction();
		try {
			ArrayList<RoutePoint> newRPList = route.getRPList();
			for(int j=0;j<newRPList.size();j++) {
				RoutePoint rp = newRPList.get(j);
				ContentValues RPValues = new ContentValues(); 
				RPValues.put("ROUTENAME", routeName);
				RPValues.put("LATITUDE", rp.getLatLng().latitude);
				RPValues.put("LONGITUDE", rp.getLatLng().longitude);
				RPValues.put("ELEVATION", rp.getElevation());
				RPValues.put("DATA", rp.getDataString());
				db.insert(RouteDatabaseOpenHelper.ROUTEPOINT_TABLE_NAME, null, RPValues);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();
		main.updateMarkerInfo();
	}

	/**
	 * Change the route of the given name (oldRouteName) to newRouteName
	 * @param oldRouteName
	 * @param newRouteName
	 */
	public void changeRouteName(String oldRouteName, String newRouteName) {
		// Remove from checked route names list
		if(checkedRouteNames.contains(oldRouteName)) {
			checkedRouteNames.remove(oldRouteName);
			checkedRouteNames.add(newRouteName);
		}
			
		Route route = getRoute(oldRouteName);
		route.setName(newRouteName); // 1. change routeList

		// 2. change routeList in database
		SQLiteDatabase db =  routeDBHelper.getWritableDatabase();

		ContentValues values = new ContentValues(); 
		values.put("ROUTENAME", newRouteName);
		values.put("DATA", route.getDataString());
		values.put("PARAMETERS", route.getParametersString());		
		db.update(RouteDatabaseOpenHelper.ROUTE_TABLE_NAME, values, "ROUTENAME=?", new String[]{oldRouteName});

		// Re-create RPList in database
		db.delete(RouteDatabaseOpenHelper.ROUTEPOINT_TABLE_NAME, "ROUTENAME=?", new String[]{oldRouteName});

		db.beginTransaction();
		try {
			ArrayList<RoutePoint> newRPList = route.getRPList();
			for(int j=0;j<newRPList.size();j++) {
				RoutePoint rp = newRPList.get(j);
				ContentValues RPValues = new ContentValues(); 
				RPValues.put("ROUTENAME", newRouteName);
				RPValues.put("LATITUDE", rp.getLatLng().latitude);
				RPValues.put("LONGITUDE", rp.getLatLng().longitude);
				RPValues.put("ELEVATION", rp.getElevation());
				RPValues.put("DATA", rp.getDataString());
				db.insert(RouteDatabaseOpenHelper.ROUTEPOINT_TABLE_NAME, null, RPValues);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();

		//3. change RouteOnMapList
		if(routesOnMapList.contains(oldRouteName)) {
			routesOnMapList.remove(oldRouteName);
			routesOnMapList.add(newRouteName);

			// update markerMap
			Iterator<Entry<Marker, MarkerInfo>> it = markerMap.entrySet().iterator();
			while(it.hasNext()) {
				Entry<Marker, MarkerInfo> entry = it.next();
				MarkerInfo markerInfo = entry.getValue();
				if(markerInfo.getName().equals(oldRouteName))
					markerInfo.setName(newRouteName);
			}

			// update markerInfo
			main.updateMarkerInfo();

			//4. change RouteOnMapList in database
			db =  routeDBHelper.getWritableDatabase();

			ContentValues values2 = new ContentValues(); 
			values2.put("ROUTENAME", newRouteName);	
			db.update(RouteDatabaseOpenHelper.ROUTE_ON_MAP_TABLE_NAME, values2, "ROUTENAME=?", new String[]{oldRouteName});

			db.close();
		}
	}

	/**
	 * Get a route object from routeList using a routeName
	 * @param requestedRouteName
	 * @return
	 */
	public Route getRoute(String requestedRouteName) {
		int i = 0;
		Route result = null;
		while(i<routeList.size() && result==null) {
			Route route = routeList.get(i);
			String routeName = route.getName();
			if(routeName.equals(requestedRouteName))
				result = route;
			else
				i++;
		}		
		return result;
	}

	public ArrayList<String> getAllRouteName() {
		ArrayList<String> routeNameList = new ArrayList<String>();
		for(int i=0;i<routeList.size();i++)
			routeNameList.add(routeList.get(i).getName());
		return routeNameList;
	}

	public ArrayList<Route> getRouteList() {
		return routeList;
	}

	public ArrayList<String> getRoutesOnMapList() {
		return routesOnMapList;
	}

	public ArrayList<String> getCheckedRouteNames() {
		return checkedRouteNames;
	}

	public HashMap<Marker,MarkerInfo> getMarkerMap() {
		return markerMap;
	}

	/**
	 * Request only the elevations of changed LatLng
	 * @param routeName
	 * @param latLngList
	 */
	@SuppressWarnings("unchecked")
	public void modifyRouteRPList(String routeName, ArrayList<LatLng> latLngList) {
		Route route = getRoute(routeName);
		ArrayList<RoutePoint> oriRPList = route.getRPList();
		ArrayList<Double> elevationList = new ArrayList<Double>();
		ArrayList<LatLng> requestList = new ArrayList<LatLng>();
		// Find matching LatLng in oriRPList
		for(int i=0;i<latLngList.size();i++) {
			LatLng latLng = latLngList.get(i);
			boolean contains = false;
			int j = 0;	
			RoutePoint oriRP;
			while(contains==false && j<oriRPList.size()) {
				oriRP = oriRPList.get(j);
				LatLng oriLatLng = oriRP.getLatLng();
				contains = latLng.equals(oriLatLng); // exit loop if equal
				j++;
			}
			if(contains==true) {
				elevationList.add(oriRPList.get(j-1).getElevation());
			} else {
				requestList.add(latLng); // add latLng with missing elevation to requestList
				elevationList.add(null);
			}

		}
		if(requestList.size()>0) {
			GetElevationTask task = new GetElevationTask();
			task.setType(MapObjectType.route);
			task.setTag(route.getName());
			editTaskMap.put(task, route);
			editTaskLatLngMap.put(task, latLngList);
			task.setDelegate(this);
			task.execute(requestList);			
		}
		else {
			ArrayList<RoutePoint> newRPList = new ArrayList<RoutePoint>();
			for(int i=0;i<latLngList.size();i++) {
				RoutePoint rp = new RoutePoint(latLngList.get(i),elevationList.get(i));
				newRPList.add(rp);
			}		
			try {
				modifyRoute(routeName, newRPList, route.getParameters());

				// notify changes
				main.getMapManager().addRouteOnMap(routeName);
				if(SelectRouteActivity.thisActivity!=null)
					SelectRouteActivity.notifyDataSetChanged();
			} catch (InvalidParametersException e) { // should not happen
				Toast.makeText(main, "Invalid parameters", Toast.LENGTH_SHORT).show();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void updateElevations(String routeName, boolean updateAll, boolean showAfter) {
		Route route = getRoute(routeName);
		ArrayList<RoutePoint> RPList = route.getRPList();
		ArrayList<LatLng> latLngList = route.getLatLngList();
		ArrayList<LatLng> requestList;

		if(updateAll==true) {
			requestList = latLngList;
		} else {
			requestList = new ArrayList<LatLng>();
			// Find zero elevation in latLngList
			for(int i=0;i<RPList.size();i++) {
				RoutePoint rp = RPList.get(i);
				if(rp.getElevation()==0)
					requestList.add(rp.getLatLng());		
			}			
		}
		if(requestList.size()>0) {
			// Create task
			GetElevationTask task = new GetElevationTask();
			task.setType(MapObjectType.route);
			task.setTag(route.getName());
			editTaskMap.put(task, route);
			showMap.put(task, showAfter);
			updateElevationModeMap.put(task, updateAll);
			task.setDelegate(this);
			task.execute(requestList);
		} else {
			Toast.makeText(MainActivity.main, "No update needed", Toast.LENGTH_SHORT).show();
			if(showAfter==true)
				main.getMapManager().addRouteOnMap(routeName);
		}
	}

	/**
	 * The edited route should always be hidden before tasks begin
	 * @param task
	 */
	@Override
	public void processGetElevationFinish(GetElevationTask task) {
		// add route		
		if(newRouteMap.get(task)!=null) {
			finishAddNewRoute(task);
		} else {
			// finish updating
			if(editTaskLatLngMap.get(task)!=null)
				finishEditRoute(task);
			else {
				if(updateElevationModeMap.get(task)!=null)
					finishUpdateElevation(task);
			}
		}
	}

	private void finishAddNewRoute(GetElevationTask task) {
		Route route = newRouteMap.get(task);
		newRouteMap.remove(task);
		boolean showOnMap = showMap.get(task);
		showMap.remove(task);
		try {
			route.modifyElevationList(task.getElevationList());
			route.setElevationRequestNeeded(false);
			addRoute(route, showOnMap);
		} catch (InvalidParametersException e) {
			Toast.makeText(main, "Error during import: Invalid Parameters", Toast.LENGTH_SHORT).show();
		}		
	}

	private void finishEditRoute(GetElevationTask task) {
		Route route = editTaskMap.get(task);
		ArrayList<LatLng> latLngList = editTaskLatLngMap.get(task);
		editTaskMap.remove(task);
		editTaskLatLngMap.remove(task);

		if(task.isErrorFatal()) { // do nothing and add route back to map
			main.getMapManager().addRouteOnMap(route.getName());
		} else {
			ArrayList<RoutePoint> oldRPList = route.getRPList();
			ArrayList<Double> elevationResultList = task.getElevationList();
			ArrayList<RoutePoint> newRPList = new ArrayList<RoutePoint>();

			// Use old route point or elevationResultList as elevation source
			int k = 0;
			for(int i=0;i<latLngList.size();i++) {
				LatLng latLng = latLngList.get(i);
				// 1. Try using old RPList as elevation source
				boolean contains = false;
				int j = 0;	
				RoutePoint oriRP;
				while(contains==false && j<oldRPList.size()) {
					oriRP = oldRPList.get(j);
					LatLng oriLatLng = oriRP.getLatLng();
					contains = latLng.equals(oriLatLng); // exit loop if equal
					j++;
				}
				double elevation;		
				if(contains==true) {
					elevation = oldRPList.get(j-1).getElevation();				
				} else { // 2. Use elevationResultList as elevation source if 1 failed
					elevation = elevationResultList.get(k);
					k++;
				}
				// 3. Create new Route Point
				RoutePoint rp = new RoutePoint(latLng,elevation);
				newRPList.add(rp);
			}
			try {
				String routeName = route.getName();
				modifyRoute(routeName, newRPList, route.getParameters());

				// notify changes
				main.getMapManager().addRouteOnMap(routeName);
				if(SelectRouteActivity.thisActivity!=null)
					SelectRouteActivity.notifyDataSetChanged();
				Toast.makeText(main, "Editing Finished", Toast.LENGTH_SHORT).show();
			} catch (InvalidParametersException e) { // should not happen
				Toast.makeText(main, "Invalid parameters", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void finishUpdateElevation(GetElevationTask task) {
		Route route = editTaskMap.get(task);		
		boolean updateAll = updateElevationModeMap.get(task);
		boolean showOnMap = showMap.get(task);
		editTaskMap.remove(task);
		updateElevationModeMap.remove(task);
		showMap.remove(task);

		if(task.isErrorFatal()) { // do nothing and add route back to map
			main.getMapManager().addRouteOnMap(route.getName());
		} else {
			ArrayList<Double> elevationList = task.getElevationList();
			ArrayList<RoutePoint> RPList = route.getRPList();

			if(updateAll==true) {
				for(int i=0;i<RPList.size();i++) {
					RoutePoint rp = RPList.get(i);
					rp.setElevation(elevationList.get(i));
				}
			} else {
				int index = 0;
				for(int i=0;i<RPList.size();i++) {
					RoutePoint rp = RPList.get(i);
					if(rp.getElevation()==0) {
						rp.setElevation(elevationList.get(index));
						index++;
					}
				}
			}

			try {
				String routeName = route.getName();
				modifyRoute(routeName, route.getRPList(), route.getParameters());

				// notify changes
				if(SelectRouteActivity.thisActivity!=null)
					SelectRouteActivity.notifyDataSetChanged();
				if(showOnMap==true)
					main.getMapManager().addRouteOnMap(route.getName());
				Toast.makeText(main, "Elevations Updated", Toast.LENGTH_SHORT).show();
			} catch (InvalidParametersException e) { // should not happen
				Toast.makeText(main, "Invalid parameters", Toast.LENGTH_SHORT).show();
			}
		}
	}

	public HashMap<GetElevationTask, Route> getEditTaskMap() {
		return editTaskMap;
	}

}
