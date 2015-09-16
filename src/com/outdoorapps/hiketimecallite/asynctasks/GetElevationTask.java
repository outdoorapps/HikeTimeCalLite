package com.outdoorapps.hiketimecallite.asynctasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.SettingsActivity;
import com.outdoorapps.hiketimecallite.support.Elevation;
import com.outdoorapps.hiketimecallite.support.Formatters;
import com.outdoorapps.hiketimecallite.support.MapObject.MapObjectType;
import com.outdoorapps.hiketimecallite.support.constants.Defaults;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;
import com.outdoorapps.hiketimecallite.support.constants.Version;

@SuppressLint("ShowToast")
public class GetElevationTask extends AsyncTask<ArrayList<LatLng>, Integer, ArrayList<Double>> {

	private int requestsNeeded;
	private String tag;
	private MapObjectType type;
	private ArrayList<LatLng> latLngList;
	private ArrayList<Double> elevationList;
	private boolean errorFatal, dailyLimitReached, requestError;	
	private GetElevationResponseInterface delegate;
	private NotificationCompat.Builder mBuilder;
	private NotificationManager mNotificationManager;
	private SharedPreferences prefs;
	private Formatters formatters;
	private final int REQUEST_BATCH_SIZE = 10;
	private final int REQUEST_PAUSE_TIME = 2500; // 2.5 seconds
	public final int GET_ELEVATION_NOTIFICATION_ID = 1;

	@Override
	protected void onPreExecute() {
		switch(type) {
		case route:
			tag += "_r";
			break;
		case track:
			tag += "_t";
			break;
		case invalid:
			tag += "_i";
			break;
		default:
			tag += "_d";
			break;
		}
		formatters = new Formatters();
		// Create Notification
		mBuilder = new NotificationCompat.Builder(MainActivity.main)
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle(MainActivity.main.getString(R.string.requesting_elevations))
		.setContentText(Version.CREATOR)
		.setOngoing(true)
		.setProgress(100, 0, false);
		Intent resultIntent = new Intent(MainActivity.main,MainActivity.class);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(MainActivity.main,0,resultIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
	
		mBuilder.setContentIntent(resultPendingIntent);	
		mNotificationManager =
				(NotificationManager) MainActivity.main.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(tag, GET_ELEVATION_NOTIFICATION_ID, mBuilder.build());//TODO
	}
	@Override
	protected void onProgressUpdate(Integer... values) {
		double percentage = (double)values[0]/(double)requestsNeeded *100;
		mBuilder.setContentText(values[0] +"/"+ requestsNeeded + " elevations received\t\t" + formatters.formatInteger(percentage) + " %");
		mBuilder.setProgress(requestsNeeded, values[0], false);		
		mNotificationManager.notify(tag, GET_ELEVATION_NOTIFICATION_ID, mBuilder.build());//TODO
	}
	@Override
	protected ArrayList<Double> doInBackground(ArrayList<LatLng>... params) {
		errorFatal = false;
		dailyLimitReached = false;
		requestError = false;
		prefs = MainActivity.main.getPref();
		String elevationSource = prefs.getString(PrefKeys.KEY_PREF_ELEVATION_SOURCE, 
				Defaults.DEFAULT_ELEVATION_SOURCE);	
		if(elevationSource.equals(SettingsActivity.GOOGLE_ELEVATION_API))
			elevationList = requestByGoogle(params);
		else
			elevationList = requestByUSGS(params);
		return elevationList;
	}
	@Override
	protected void onPostExecute(ArrayList<Double> elevationList) {
		mNotificationManager =
				(NotificationManager) MainActivity.main.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(tag,GET_ELEVATION_NOTIFICATION_ID);//TODO id
		
		try{
			if(errorFatal) {
				Toast.makeText(MainActivity.main, 
						"Elevation request encounters error, all requested elevaions are zeroed", Toast.LENGTH_LONG).show();
			} else {
				if(dailyLimitReached) {
					Toast.makeText(MainActivity.main, 
							"Google Elevation API's daily query limit reached, " +
									"further elevation requests may not be available " +
									"for the next 24 hours, please switch elevation source", Toast.LENGTH_LONG).show();
				}
				if(requestError)
					Toast.makeText(MainActivity.main, 
							"Errors occurred during elevation requests, data may not be accurate", Toast.LENGTH_LONG).show();
			}
		} catch (IllegalStateException e) { // if there has been a screen rotation
		} 
		delegate.processGetElevationFinish(this);
	}
	// limit request rate to avoid error
	private ArrayList<Double> requestByGoogle(ArrayList<LatLng>... params) {
		latLngList = params[0];
		requestsNeeded = latLngList.size();
		ArrayList<Double> newElevationList = new ArrayList<Double>();

		int index = 0;
		while(index<latLngList.size()) {
			int batchItemCount = 0;
			String latLngStr = "";
			while(batchItemCount<REQUEST_BATCH_SIZE && index<latLngList.size()) {
				latLngStr = latLngStr + latLngList.get(index).latitude + "," +latLngList.get(index).longitude + "|";				
				batchItemCount++;
				index++;
			}
			latLngStr = latLngStr.substring(0, latLngStr.length()-2); // remove the last "|"
			String query = "http://maps.googleapis.com/maps/api/elevation/xml?locations="
					+ latLngStr + "&sensor=false";

			try {
				URL url = new URL(query);
				String resultXML = executeRequest(url);
				String status = getStatus(resultXML);

				if(status.equals("OK")) {
					ArrayList<Double> elevationResultList = getElevationFromResult(resultXML);
					for(int i=0;i<elevationResultList.size();i++)		
						newElevationList.add(elevationResultList.get(i));						
				} else {
					if(status.equals("OVER_QUERY_LIMIT")) {
						// Re-request after 2.5 seconds
						ArrayList<Double> elevationResultList = secondRequest(url);
						if(elevationResultList!=null) { // request success
							for(int i=0;i<elevationResultList.size();i++)		
								newElevationList.add(elevationResultList.get(i));
						} else {
							for(int i=0;i<batchItemCount;i++) 
								newElevationList.add(0d);
						}				
					}
					else {
						for(int i=0;i<batchItemCount;i++) 
							newElevationList.add(0d);
					}
				}
				publishProgress(newElevationList.size());
			} catch (MalformedURLException e) {	// Cannot happen
				errorFatal = true;
				return getEmptyNewElevationList(requestsNeeded);
			} catch (NumberFormatException e) {
				errorFatal = true;
				return getEmptyNewElevationList(requestsNeeded);
			} catch (SAXException e) {
				errorFatal = true;
				return getEmptyNewElevationList(requestsNeeded);
			} catch (IOException e) {
				errorFatal = true;
				return getEmptyNewElevationList(requestsNeeded);
			} catch (ParserConfigurationException e) {
				errorFatal = true;
				return getEmptyNewElevationList(requestsNeeded);
			} catch (Exception e) {
				errorFatal = true;
				return getEmptyNewElevationList(requestsNeeded);
			}
		}
		return newElevationList;
	}

	private String getStatus(String resultXML) {
		String status = "";
		String tagOpen = "<status>";
		String tagClose = "</status>";

		if (resultXML.indexOf(tagOpen) != -1) {
			int start = resultXML.indexOf(tagOpen) + tagOpen.length();
			int end = resultXML.indexOf(tagClose);
			status = resultXML.substring(start, end);
		}				
		return status;
	}

	private ArrayList<Double> getElevationFromResult(String resultXML) 
			throws ParserConfigurationException, SAXException, IOException {
		ArrayList<Double> elevationResultList = new ArrayList<Double>();
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();		
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();					
		InputSource is = new InputSource(new StringReader(resultXML));
		Document doc = docBuilder.parse(is);
		NodeList resultList = doc.getElementsByTagName("result");
		for(int i=0; i<resultList.getLength();i++) {
			Node nNode = resultList.item(i);
			Element eElement = (Element) nNode;
			double elevation = Double.parseDouble(eElement.getElementsByTagName("elevation").item(0).getTextContent());
			// range check
			if(Elevation.isElevationValid(elevation))
				elevationResultList.add(elevation);
			else
				elevationResultList.add(0d);
		}		
		return elevationResultList;
	}

	private ArrayList<Double> secondRequest(URL url) throws IOException, ParserConfigurationException, SAXException {
		ArrayList<Double> elevationResultList = null;
		try {
			// wait for 2.5 seconds and request again
			Thread.sleep(REQUEST_PAUSE_TIME);
			String resultXML = executeRequest(url);
			String newStatus = getStatus(resultXML);
			if(newStatus.equals("OK")) {
				elevationResultList = getElevationFromResult(resultXML);
			}
			if(newStatus.equals("OVER_QUERY_LIMIT"))
				dailyLimitReached = true;						    
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		return elevationResultList;
	}

	private ArrayList<Double> requestByUSGS(ArrayList<LatLng>... params) {		
		latLngList = params[0];
		requestsNeeded = latLngList.size();
		ArrayList<Double> newElevationList = new ArrayList<Double>();

		try {
			for(int i=0;i<latLngList.size();i++) {
				LatLng latlng = latLngList.get(i);
				String query = "http://gisdata.usgs.gov/"
						+ "xmlwebservices2/elevation_service.asmx/"   
						+ "getElevation?X_Value=" + String.valueOf(latlng.longitude)
						+ "&Y_Value=" + String.valueOf(latlng.latitude)
						+ "&Elevation_Units=METERS&Source_Layer=-1&Elevation_Only=true"; 
				try {
					Double elevation = 0d;
					URL encodedURL = new URL(query);
					String resultStr = executeRequest(encodedURL);
					if(resultStr.equals(""))
						requestError = true;
					String tagOpen = "<double>";
					String tagClose = "</double>";
					if (resultStr.indexOf(tagOpen) != -1) {
						int start = resultStr.indexOf(tagOpen) + tagOpen.length();
						int end = resultStr.indexOf(tagClose);
						String value = resultStr.substring(start, end);
						elevation = Double.parseDouble(value);
						// Range Check
						if(Elevation.isElevationValid(elevation))
							newElevationList.add(elevation);
						else
							newElevationList.add(0d);
					} else
						newElevationList.add(0d); // if there is any error, set elevation to 0
				} catch (NumberFormatException e) {
					newElevationList.add(0d);
				} finally {
					publishProgress(newElevationList.size());
				}
			}
		} catch (MalformedURLException e) {
			errorFatal = true;
			return getEmptyNewElevationList(requestsNeeded);
		} catch (Exception e) {
			errorFatal = true;
			return getEmptyNewElevationList(requestsNeeded);
		}
		return newElevationList;
	}

	private String executeRequest(URL encodedURL) throws IOException {
		String result = "";
		HttpURLConnection connection = (HttpURLConnection)encodedURL.openConnection();			
		connection.setRequestMethod("GET");
		connection.connect();

		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			InputStream responseBody = connection.getInputStream();				
			BufferedReader br = new BufferedReader(new InputStreamReader(responseBody));
			String output;
			while((output=br.readLine())!=null) {
				result = result + output;
			}
			br.close();
			responseBody.close();
		}
		return result;
	}
	
	public void setType(MapObjectType type) {
		this.type = type;
	}
	
	public void setTag(String name) {
		
	}
	
	public GetElevationResponseInterface getDelegate() {
		return delegate;
	}

	public void setDelegate(GetElevationResponseInterface delegate) {
		this.delegate = delegate;
	}

	public ArrayList<Double> getElevationList() {
		return elevationList;
	}

	public boolean isErrorFatal() {
		return errorFatal;
	}

	private ArrayList<Double> getEmptyNewElevationList(int size) {
		ArrayList<Double> emptyNewElevationList = new ArrayList<Double>();
		for(int i=0;i<size;i++)
			emptyNewElevationList.add(0d);
		return emptyNewElevationList;
	}
}
