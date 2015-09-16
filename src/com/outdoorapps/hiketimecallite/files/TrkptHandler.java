package com.outdoorapps.hiketimecallite.files;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.location.Location;

import com.outdoorapps.hiketimecallite.model.track.TrackingPoint;
import com.outdoorapps.hiketimecallite.support.constants.Constants;

public class TrkptHandler extends DefaultHandler {

	private boolean inMetadata, inTrkpt, inName;
	private double conversionFactor;
	private TrackingPoint currentTrackingPT;
	private ArrayList<TrackingPoint> trackingPTList;
	private String trackName;
	private HashMap<String,TagType> tagMap;
	private TagType type;
	public enum TagType {TRKPT,ELE,TIME,SPEED,BEARING,ACCURACY,METADATA,NAME}

	public TrkptHandler(boolean metricElevation) {		
		inTrkpt = false;
		inMetadata = false;
		inName = false;
		trackingPTList = new ArrayList<TrackingPoint>();
		trackName = "";
		if(metricElevation==false)
			conversionFactor = Constants.FEET_TO_METER;
		else
			conversionFactor = 1;
		
		tagMap = new HashMap<String,TagType>();
		tagMap.put("TRKPT".toLowerCase(Locale.ENGLISH), TagType.TRKPT);
		tagMap.put("ELE".toLowerCase(Locale.ENGLISH), TagType.ELE);
		tagMap.put("TIME".toLowerCase(Locale.ENGLISH), TagType.TIME);
		tagMap.put("SPEED".toLowerCase(Locale.ENGLISH), TagType.SPEED);
		tagMap.put("BEARING".toLowerCase(Locale.ENGLISH), TagType.BEARING);
		tagMap.put("ACCURACY".toLowerCase(Locale.ENGLISH), TagType.ACCURACY);
		tagMap.put("METADATA".toLowerCase(Locale.ENGLISH), TagType.METADATA);
		tagMap.put("NAME".toLowerCase(Locale.ENGLISH), TagType.NAME);
	}

	public void startElement(String uri, String localName,String qName, Attributes attributes) throws SAXException {
		type = tagMap.get(qName.toLowerCase(Locale.ENGLISH));
		if(type!=null) {
			switch(type) {
			case TRKPT:
				double lat = Double.parseDouble(attributes.getValue("lat"));
				double lon = Double.parseDouble(attributes.getValue("lon"));
				Location location = new Location("");
				location.setLatitude(lat);
				location.setLongitude(lon);
				currentTrackingPT = new TrackingPoint(location,"");
				inTrkpt = true;	
				break;
			case METADATA:
				inMetadata = true;
				break;
			case NAME:
				inName = true;
			default:
				break;
			}	
		}
	}
	public void endElement(String uri, String localName, String qName) throws SAXException {
		// Close a trkpt element
		if (qName.equalsIgnoreCase("TRKPT")) {
			inTrkpt = false;
			trackingPTList.add(currentTrackingPT);
		} else
			if (qName.equalsIgnoreCase("METADATA")) {
				inMetadata = false;
			}
	}

	public void characters(char ch[], int start, int length) throws SAXException {
		if(type!=null) {
			String item = new String(ch, start, length);
			if(inTrkpt==true) {				
				switch(type) {
				case TIME:
					currentTrackingPT.setTime(item);
					break;
				case ELE:
					double altitude = Double.parseDouble(item)*conversionFactor;
					currentTrackingPT.getLocation().setAltitude(altitude);
					break;
				case SPEED:
					float speed = Float.parseFloat(item);
					currentTrackingPT.getLocation().setSpeed(speed);
					break;
				case BEARING:
					float bearing = Float.parseFloat(item);
					currentTrackingPT.getLocation().setBearing(bearing);
					break;
				case ACCURACY:
					float accuracy = Float.parseFloat(item);
					currentTrackingPT.getLocation().setAccuracy(accuracy);
					break;
				default:					
					break;
				}
			} else {
				if(inMetadata && inName) {
					trackName = item;
					inName = false;					
				}
			}
			type = null; // Reset to null
		}
	}

	public ArrayList<TrackingPoint> getTrackingPTList() {
		return trackingPTList;
	}

	public String getTrackName() {
		return trackName;
	}

}
