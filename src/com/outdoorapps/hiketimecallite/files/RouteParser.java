package com.outdoorapps.hiketimecallite.files;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.android.gms.maps.model.LatLng;
import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.model.route.Calculator.InvalidParametersException;
import com.outdoorapps.hiketimecallite.model.route.Route;
import com.outdoorapps.hiketimecallite.model.route.Route.InvalidRoutePointListException;
import com.outdoorapps.hiketimecallite.model.route.RoutePoint;
import com.outdoorapps.hiketimecallite.support.Elevation;
import com.outdoorapps.hiketimecallite.support.Elevation.ElevationOutOfBoundException;
import com.outdoorapps.hiketimecallite.support.constants.Constants;
import com.outdoorapps.hiketimecallite.support.constants.Defaults;

/**
 * Parse route points a into an ArrayList of LatLng for adding (request elevation if needed)
 * @author chbting
 *
 */
public class RouteParser {

	private boolean metricElevation;
	private boolean elevationRequestNeeded;
	private final int RP_LIMIT = 30;

	public Route Parse(String routeName, File file, boolean metricElevation) 
			throws InvalidRoutePointListException, ParserConfigurationException, SAXException, 
			IOException, UnsupportedFileTypeException, CorruptedDataException, 
			ElevationOutOfBoundException, InvalidParametersException, VersionNotSupportException{
		elevationRequestNeeded = false;
		this.metricElevation = metricElevation;
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();		
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(file);
		ArrayList<RoutePoint> RPList;
		if((file+"").toLowerCase(Locale.getDefault()).endsWith(".gpx"))
			RPList = prepareLatLngListGPX(doc);
		else {
			if((file+"").toLowerCase(Locale.getDefault()).endsWith(".kml"))
				RPList = prepareLatLngListKML(doc);
			else
				throw new UnsupportedFileTypeException();
		}
		if(RPList.size()<2)
			throw new InvalidRoutePointListException();

		return new Route(routeName,RPList,
				Defaults.getDefaultParametersArray(MainActivity.main.getPref()),elevationRequestNeeded);
	}

	private ArrayList<RoutePoint> prepareLatLngListGPX(Document doc) throws VersionNotSupportException {
		ArrayList<RoutePoint> RPList = new ArrayList<RoutePoint>();
		NodeList rteptList = doc.getElementsByTagName("rtept");
		if(rteptList.getLength()>RP_LIMIT)
			throw new VersionNotSupportException();
		for(int i=0; i<rteptList.getLength();i++) {
			Node nNode = rteptList.item(i);
			Element eElement = (Element) nNode;
			Double lon = Double.parseDouble(eElement.getAttribute("lon"));
			Double lat = Double.parseDouble(eElement.getAttribute("lat"));
			LatLng latLng = new LatLng(lat,lon);

			double elevation = 0;
			try{
				elevation = Double.parseDouble(eElement.getElementsByTagName("ele").item(0).getTextContent());
				if(metricElevation==false) // if the elevations on file are in feet
					elevation *= Constants.FEET_TO_METER;
				if(!(Elevation.isElevationValid(elevation))) { // assumes recoverable
					elevation = 0;
					elevationRequestNeeded = true;
				}
			} catch(NullPointerException e){ // request elevation for all point if one elevation is missing
				elevation = 0;
				elevationRequestNeeded = true;
			}
			RPList.add(new RoutePoint(latLng,elevation));
		}
		return RPList;
	}

	private ArrayList<RoutePoint> prepareLatLngListKML(Document doc) throws CorruptedDataException, 
	IOException, ElevationOutOfBoundException {
		ArrayList<RoutePoint> RPList = new ArrayList<RoutePoint>();
		elevationRequestNeeded = true; // need elevation request by default
		NodeList lineStringList = doc.getElementsByTagName("LineString");
		Node nNode = lineStringList.item(0);
		Element eElement = (Element) nNode;

		String coordinatesStr = eElement.getElementsByTagName("coordinates").item(0).getTextContent();
		coordinatesStr = coordinatesStr.replace("\n", "");
		coordinatesStr = coordinatesStr.replace("\t", "");
		coordinatesStr = coordinatesStr.replace("\r", "");

		InputStream is = new ByteArrayInputStream(coordinatesStr.getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = br.readLine()) != null) {

			String[] itemList = line.split(" ");
			for(int i=0;i<itemList.length;i++) {
				String item = itemList[i];
				String values[] = item.split(",");
				double lon, lat;
				try {					
					lon = Double.parseDouble(values[0]);
					lat = Double.parseDouble(values[1]);
				} catch (NumberFormatException e) {
					throw new CorruptedDataException(); // unrecoverable, return here
				}
				LatLng latLng = new LatLng(lat,lon);
				
				double elevation = 0;
				try {					
					elevation = Double.parseDouble(values[2]);
				} catch (NumberFormatException e) {} // assumes elevation corruption recoverable through elevation requests
				if(elevation!=0)
					elevationRequestNeeded = false; // assumes there is elevation data if there is any non-zero elevation
				if(metricElevation==false) // if the elevations on file are in feet
					elevation *= Constants.FEET_TO_METER;
				if(!(Elevation.isElevationValid(elevation)))
					elevation = 0;
				RPList.add(new RoutePoint(latLng,elevation));
			}
		}			
		br.close();
		return RPList;
	}

	public boolean isElevationRequestNeeded() {
		return elevationRequestNeeded;
	}

	@SuppressWarnings("serial")
	public static class UnsupportedFileTypeException extends Exception {
		public UnsupportedFileTypeException () {

		}
	}

	@SuppressWarnings("serial")
	public static class CorruptedDataException extends Exception {
		public CorruptedDataException () {

		}
	}
	
	@SuppressWarnings("serial")
	public static class VersionNotSupportException extends Exception {
		public VersionNotSupportException () {

		}
	}
}
