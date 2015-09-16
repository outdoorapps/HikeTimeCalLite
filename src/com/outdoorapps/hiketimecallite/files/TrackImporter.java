package com.outdoorapps.hiketimecallite.files;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.outdoorapps.hiketimecallite.files.RouteParser.UnsupportedFileTypeException;
import com.outdoorapps.hiketimecallite.model.track.Track;
import com.outdoorapps.hiketimecallite.model.track.TrackingPoint;

/**
 * Parse route points a into an ArrayList of LatLng for adding (request elevation if needed)
 * @author chbting
 *
 */
public class TrackImporter {

	public Track Parse(File file, String trackName, boolean metricElevation) throws Exception{
		Track track = null;
		if((file+"").toLowerCase(Locale.getDefault()).endsWith(".gpx"))
			track = parseGPX(file, trackName, metricElevation);
		else {
			throw new UnsupportedFileTypeException();
		}
		if(track.getLatLngList().size()<1)
			throw new InvalidTrackPointListException();
		return track;
	}

	private Track parseGPX(File file, String trackName, boolean metricElevation) throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		TrkptHandler trkptHandler = new TrkptHandler(metricElevation);
		saxParser.parse(file, trkptHandler);
		
		ArrayList<TrackingPoint> trackingPTList = trkptHandler.getTrackingPTList();
		return new Track(trackName,Track.convertToTPList(trackingPTList));		
	}

	@SuppressWarnings("serial")
	public static class InvalidTrackPointListException extends Exception {
		public InvalidTrackPointListException () {

		}
	}
}
