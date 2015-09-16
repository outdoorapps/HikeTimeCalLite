package com.outdoorapps.hiketimecallite;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.outdoorapps.hiketimecallite.support.constants.Links;
import com.outdoorapps.hiketimecallite.support.constants.Version;

public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		TextView privacyPolicy = (TextView) findViewById(R.id.privacy_policy);
		TextView version = (TextView) findViewById(R.id.version);
		TextView googleMapsAPI = (TextView) findViewById(R.id.google_maps_android);
		TextView googleElevationAPI = (TextView) findViewById(R.id.google_elevation_api);
		TextView usgs = (TextView) findViewById(R.id.usgs);
		TextView graphViewLink = (TextView) findViewById(R.id.graphview);
		
		version.setText("Version " + Version.CREATOR_VERSION);

		privacyPolicy.setText(
				Html.fromHtml(
						"<a href=\"https://sites.google.com/site/hiketimecal/home/privacy-policy\">" + 
								getString(R.string.privacy_policy)
								+"</a> "));
		privacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());
		
		googleMapsAPI.setText(
				Html.fromHtml(
						"<a href=\"https://developers.google.com/maps/documentation/android/\">" + 
								getString(R.string.google_maps_android)
								+"</a> "));
		googleMapsAPI.setMovementMethod(LinkMovementMethod.getInstance());

		googleElevationAPI.setText(
				Html.fromHtml(
						"<a href=\"https://developers.google.com/maps/documentation/elevation/\">" +
								getString(R.string.google_elevation_api) + "</a> "));
		googleElevationAPI.setMovementMethod(LinkMovementMethod.getInstance());

		usgs.setText(
				Html.fromHtml(
						"<a href=\"http://gisdata.usgs.gov/xmlwebservices2/elevation_service.asmx\">" +
								getString(R.string.usgs) + "</a> "));
		usgs.setMovementMethod(LinkMovementMethod.getInstance());

		graphViewLink.setText(
				Html.fromHtml(
						"<a href=\"http://www.jjoe64.com/p/graphview-library.html\">" +
								getString(R.string.graphview) + "</a> "));
		graphViewLink.setMovementMethod(LinkMovementMethod.getInstance());
	}
	
	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.about, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.help:
			Uri uri = Uri.parse(Links.HELP_LINK); 
			startActivity(new Intent(Intent.ACTION_VIEW, uri));
			break;
		case R.id.action_settings:
			Intent intent = new Intent(this,SettingsActivity.class);
			startActivity(intent);
			break;
		default:
			break;
		}
		return true;
	}

}
