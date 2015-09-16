package com.outdoorapps.hiketimecallite.files;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.location.Location;

import com.outdoorapps.hiketimecallite.model.track.TrackingPoint;
import com.outdoorapps.hiketimecallite.support.constants.Constants;
import com.outdoorapps.hiketimecallite.support.constants.Version;

public class TrackExporter {

	public static final String XMLNS = "http://www.topografix.com/GPX/1/1";
	public static final String XMLNS_VERSION = "1.1";
	public static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";
	public static final String XSI_SCH_LOCATION = "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd";
	public static final String KML_XMLNS = "http://www.opengis.net/kml/2.2";
	public static final String KML_XMLN_GX = "http://www.google.com/kml/ext/2.2";
	public static final String KML_XMLNS_KML = "http://www.opengis.net/kml/2.2";
	public static final String KML_XMLNS_ATOM = "http://www.w3.org/2005/Atom";

	public void exportToGPX(String trackName, String filePath, ArrayList<TrackingPoint> trackingPTList, boolean isMetric) 
			throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();

		// create header
		Element gpxElement = doc.createElement("gpx");
		Attr xmlns = doc.createAttribute("xmlns");
		Attr xmlnsVersion = doc.createAttribute("version");
		Attr creator = doc.createAttribute("creator");
		Attr version = doc.createAttribute("version");
		Attr xsi = doc.createAttribute("xmlns:xsi");
		Attr xsiSch = doc.createAttribute("xsi:schemaLocation");

		xmlns.setValue(XMLNS);
		xmlnsVersion.setValue(XMLNS_VERSION);
		creator.setValue(Version.CREATOR);
		version.setValue(Version.CREATOR_VERSION);
		xsi.setValue(XSI);
		xsiSch.setValue(XSI_SCH_LOCATION);

		gpxElement.setAttributeNode(xmlns);
		gpxElement.setAttributeNode(xmlnsVersion);
		gpxElement.setAttributeNode(creator);
		gpxElement.setAttributeNode(version);		
		gpxElement.setAttributeNode(xsi);
		gpxElement.setAttributeNode(xsiSch);
		doc.appendChild(gpxElement);

		// create metaData
		Element metaDataElement = doc.createElement("metadata");
		gpxElement.appendChild(metaDataElement);

		Element nameElement = doc.createElement("name");
		nameElement.appendChild(doc.createTextNode(trackName));
		metaDataElement.appendChild(nameElement);

		// create Track content
		Element trkElement = doc.createElement("trk");
		gpxElement.appendChild(trkElement);
		
		Element trksegElement = doc.createElement("trkseg");
		trkElement.appendChild(trksegElement);
		
		double elevationConversionFactor = 1;
		if(isMetric==false)
			elevationConversionFactor = Constants.METER_TO_FEET;
		for(int i=0;i<trackingPTList.size();i++) {
			TrackingPoint trackingPT = trackingPTList.get(i);

			Element trkptElement = doc.createElement("trkpt");
			Attr lat = doc.createAttribute("lat");
			Attr lon = doc.createAttribute("lon");
			Location location = trackingPT.getLocation();
			lat.setValue(location.getLatitude()+"");
			lon.setValue(location.getLongitude()+"");
			trkptElement.setAttributeNode(lat);
			trkptElement.setAttributeNode(lon);				
			trksegElement.appendChild(trkptElement);

			Element eleElement = doc.createElement("ele");
			eleElement.appendChild(doc.createTextNode((location.getAltitude()*elevationConversionFactor)+""));
			trkptElement.appendChild(eleElement);
			
			Element timeElement = doc.createElement("time");
			timeElement.appendChild(doc.createTextNode(trackingPT.getTime()));
			trkptElement.appendChild(timeElement);
			
			Element bearingElement = doc.createElement("bearing");
			bearingElement.appendChild(doc.createTextNode(location.getBearing()+""));
			trkptElement.appendChild(bearingElement);
			
			Element speedElement = doc.createElement("speed");
			speedElement.appendChild(doc.createTextNode(location.getSpeed()+""));
			trkptElement.appendChild(speedElement);
			
			Element accuracyElement = doc.createElement("accuracy");
			accuracyElement.appendChild(doc.createTextNode(location.getAccuracy()+""));
			trkptElement.appendChild(accuracyElement);
		}
		
		File f = new File(filePath);
		String fileName = f.getName();
		String path = f.getPath().replace(File.separator+fileName, "");
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		DOMSource source = new DOMSource(doc);				
		StreamResult result = new StreamResult(new File(path+File.separator+fileName));
		transformer.transform(source, result);
		checkFileName(fileName,path);
	}
/*
	public void exportToKML(String routeName, String fileName, String path, boolean isMetric) 
			throws RouteNotFoundException, ParserConfigurationException, TransformerException  {
		route = MainActivity.routeData.getRoute(routeName);
		if(route==null)
			throw new RouteNotFoundException();
		else {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();

			// create header
			Element kmlElement = doc.createElement("kml");
			Attr xmlns = doc.createAttribute("xmlns:gx");
			Attr xmlns_gx = doc.createAttribute("xmlns:gx");
			Attr xmlns_kml = doc.createAttribute("xmlns:kml");
			Attr xmlns_atom = doc.createAttribute("xmlns:atom");

			xmlns.setValue(KML_XMLNS);
			xmlns_gx.setValue(KML_XMLN_GX);
			xmlns_kml.setValue(KML_XMLNS_KML);
			xmlns_atom.setValue(KML_XMLNS_ATOM);

			kmlElement.setAttributeNode(xmlns);
			kmlElement.setAttributeNode(xmlns_gx);
			kmlElement.setAttributeNode(xmlns_kml);
			kmlElement.setAttributeNode(xmlns_atom);
			doc.appendChild(kmlElement);

			// create metaData
			Element metaDataElement = doc.createElement("metadata");
			kmlElement.appendChild(metaDataElement);

			Element routeInfo = doc.createElement("routeInfo");
			metaDataElement.appendChild(routeInfo);

			Element eTripDistance = doc.createElement("routeDistance");
			eTripDistance.appendChild(doc.createTextNode(route.getRouteDistance()+""));
			routeInfo.appendChild(eTripDistance);

			Element eAdjTripDistance = doc.createElement("routeAdjDistance");
			eAdjTripDistance.appendChild(doc.createTextNode(route.getRouteAdjDistance()+""));
			routeInfo.appendChild(eAdjTripDistance);

			Element eTripTime = doc.createElement("tripTime");
			eTripTime.appendChild(doc.createTextNode(route.getTripTime()+""));
			routeInfo.appendChild(eTripTime);

			Element eReturnTripTime = doc.createElement("returnTripTime");
			eReturnTripTime.appendChild(doc.createTextNode(route.getReturnTripTime()+""));
			routeInfo.appendChild(eReturnTripTime);

			Element eRoundTripTime = doc.createElement("roundTripTime");
			eRoundTripTime.appendChild(doc.createTextNode(route.getRoundTripTime()+""));
			routeInfo.appendChild(eRoundTripTime);

			Element eTripElevationGain = doc.createElement("tripElevationGain");
			eTripElevationGain.appendChild(doc.createTextNode(route.getRouteElevationGain()+""));
			routeInfo.appendChild(eTripElevationGain);

			Element eTripElevationLoss = doc.createElement("tripElevationLoss");
			eTripElevationLoss.appendChild(doc.createTextNode(route.getRouteElevationLoss()+""));
			routeInfo.appendChild(eTripElevationLoss);

			Element eParameters = doc.createElement("parameters");
			metaDataElement.appendChild(eParameters);

			Element eParametersMetric = doc.createElement("parametersMetric");
			eParametersMetric.appendChild(doc.createTextNode(true+""));
			eParameters.appendChild(eParametersMetric);

			Element eElevationMetric = doc.createElement("elevationMetric");
			eElevationMetric.appendChild(doc.createTextNode(true+""));
			eParameters.appendChild(eElevationMetric);

			Element eSpeedInput = doc.createElement("speed");
			eSpeedInput.appendChild(doc.createTextNode(route.getSpeedInput()+""));
			eParameters.appendChild(eSpeedInput);

			Element eAscendTimeIncrease = doc.createElement("ascendTimeIncrease");
			eAscendTimeIncrease.appendChild(doc.createTextNode(route.getAscendTimeIncrease()+""));
			eParameters.appendChild(eAscendTimeIncrease);

			Element eAscendHeightInput = doc.createElement("ascendHeightInput");
			eAscendHeightInput.appendChild(doc.createTextNode(route.getAscendHeightInput()+""));
			eParameters.appendChild(eAscendHeightInput);

			Element eDescendTimeIncrease = doc.createElement("descendTimeIncrease");
			eDescendTimeIncrease.appendChild(doc.createTextNode(route.getDescendTimeIncrease()+""));
			eParameters.appendChild(eDescendTimeIncrease);

			Element eDescendHeightInput = doc.createElement("descendHeightInput");
			eDescendHeightInput.appendChild(doc.createTextNode(route.getDescendHeightInput()+""));
			eParameters.appendChild(eDescendHeightInput);
			// add steep descend adjustment in future versions

			// create content
			Element docElement = doc.createElement("Document");
			kmlElement.appendChild(docElement);

			Element nameElement = doc.createElement("name");
			nameElement.appendChild(doc.createTextNode(fileName));
			docElement.appendChild(nameElement);

			Element placemarkElement = doc.createElement("Placemark");
			docElement.appendChild(placemarkElement);

			Element pmNameElement = doc.createElement("name");
			pmNameElement.appendChild(doc.createTextNode(route.getName()));
			placemarkElement.appendChild(pmNameElement);

			Element lineStringElement = doc.createElement("LineString");
			placemarkElement.appendChild(lineStringElement);

			Element tessellateElement = doc.createElement("tessellate");
			tessellateElement.appendChild(doc.createTextNode("1")); //?
			lineStringElement.appendChild(tessellateElement);

			Element altitudeModeElement = doc.createElement("altitudeMode");
			altitudeModeElement.appendChild(doc.createTextNode("absolute"));
			lineStringElement.appendChild(altitudeModeElement);

			Element coordinatesElement = doc.createElement("coordinates");

			// Route Info
			String info = "";
			ArrayList<RoutePoint> RPList = route.getRPList();
			for(int i=0;i<RPList.size();i++) {
				RoutePoint rp = RPList.get(i);
				String lon = rp.getLatLng().longitude+"";
				String lat = rp.getLatLng().latitude+"";
				String ele = rp.getElevation()+""; // use metric elevation only					
				info = info + lon+","+lat+","+ele+" ";
			}

			coordinatesElement.appendChild(doc.createTextNode(info));
			lineStringElement.appendChild(coordinatesElement);				

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			DOMSource source = new DOMSource(doc);
			String outFileName = fileName+".kml";
			StreamResult result = new StreamResult(new File(path+File.separator+outFileName));
			transformer.transform(source, result);
			checkFileName(outFileName,path);
		}
	}
*/
	private void checkFileName(String correctFileName, String path) {
		String transformedName = correctFileName.replaceAll(" ", "%20");
		File transformedFile = new File(path+File.separator+transformedName);
		if(transformedFile.isFile())
			transformedFile.renameTo(new File(path+File.separator+correctFileName));
	}

}
