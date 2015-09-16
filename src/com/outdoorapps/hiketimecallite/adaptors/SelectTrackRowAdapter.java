package com.outdoorapps.hiketimecallite.adaptors;

import java.text.ParseException;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.SelectTrackActivity;
import com.outdoorapps.hiketimecallite.model.track.Track;
import com.outdoorapps.hiketimecallite.support.Formatters;
import com.outdoorapps.hiketimecallite.support.Time;
import com.outdoorapps.hiketimecallite.support.constants.Constants;
import com.outdoorapps.hiketimecallite.support.constants.Defaults;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;

public class SelectTrackRowAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private ArrayList<Track> tList;
	private ArrayList<String> checkedTrackNames;
	private SharedPreferences prefs;
	private Formatters formatters;

	public SelectTrackRowAdapter(Context context, ArrayList<Track> trackList, ArrayList<String> checkedTrackNames){
		// Caches the LayoutInflater for quicker use
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.tList = trackList;
		this.checkedTrackNames = checkedTrackNames;
		formatters = new Formatters();
		prefs = PreferenceManager.getDefaultSharedPreferences(SelectTrackActivity.thisActivity);	
	}

	@Override
	public int getCount() {
		return tList.size();
	}

	@Override
	public Track getItem(int index) {
		return tList.get(index);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {		
		Track track = getItem(position);
		View vi = convertView;
		if (vi == null)
			vi = inflater.inflate(R.layout.row_track, null);

		TextView header = (TextView) vi.findViewById(R.id.header);
		header.setText(track.getName());

		TextView text = (TextView) vi.findViewById(R.id.info);
		boolean isMetric = Boolean.parseBoolean(prefs.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, Defaults.DEFAULT_DISPLAY_UNIT));

		double distanceConversionFactor, heightConversionFactor;
		String distanceUnit, elevationUnit;
		if(isMetric) {
			distanceConversionFactor = 1;
			heightConversionFactor = 1;
			distanceUnit = " km";
			elevationUnit = " m";
		} else {
			distanceConversionFactor = Constants.KM_TO_MI;
			heightConversionFactor = Constants.METER_TO_FEET;
			distanceUnit = " mi";
			elevationUnit = " ft";
		}
		String date;
		try {
			date = Time.getFormattedDate(track.getDate());
		} catch (ParseException e) {
			date = track.getDate();
		}
		double distance = track.getDistance()*distanceConversionFactor;
		String dis = formatters.formatDistance(distance);
		double time = track.getDuration()/60;
		double elevationGain = track.getElevationGain();
		
		text.setText(date + "   Duration: " + formatters.formatTime(time) + "\nDistance: " + dis + distanceUnit
		+ "   Elevation Gain: " + formatters.formatElevation(elevationGain*heightConversionFactor) + elevationUnit);

		// Check checkbox if selected
		CheckBox checkbox = (CheckBox)vi.findViewById(R.id.checkBox);
		if(checkedTrackNames.contains(track.getName()))
			checkbox.setChecked(true);
		else
			checkbox.setChecked(false);

		return vi;
	}
	
	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		if(SelectTrackActivity.thisActivity!=null)
			SelectTrackActivity.setLogHeading();
	}

}
