package com.outdoorapps.hiketimecallite.adaptors;

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
import com.outdoorapps.hiketimecallite.SelectRouteActivity;
import com.outdoorapps.hiketimecallite.model.route.Route;
import com.outdoorapps.hiketimecallite.support.Formatters;
import com.outdoorapps.hiketimecallite.support.constants.Constants;
import com.outdoorapps.hiketimecallite.support.constants.Defaults;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;

public class SelectRouteRowAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private ArrayList<Route> rList;
	private ArrayList<String> checkedRouteNames;
	private SharedPreferences prefs;
	private Formatters formatters;

	public SelectRouteRowAdapter(Context context, ArrayList<Route> routeList, ArrayList<String> checkRouteNames){
		// Caches the LayoutInflater for quicker use
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.rList = routeList;
		this.checkedRouteNames = checkRouteNames;
		formatters = new Formatters();
		prefs = PreferenceManager.getDefaultSharedPreferences(SelectRouteActivity.thisActivity);

	}

	@Override
	public int getCount() {
		return rList.size();
	}

	@Override
	public Route getItem(int index) {
		return rList.get(index);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {		
		Route route = getItem(position);
		View vi = convertView;
		if (vi == null)
			vi = inflater.inflate(R.layout.row_route, null);

		// Create header
		TextView header = (TextView) vi.findViewById(R.id.header);
		header.setText(route.getName());

		// Create Route info
		TextView text = (TextView) vi.findViewById(R.id.info);
		boolean isMetric = Boolean.parseBoolean(prefs.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, Defaults.DEFAULT_DISPLAY_UNIT));

		double distanceConversionFactor;
		String distanceUnit;
		if(isMetric) {
			distanceConversionFactor = 1;
			distanceUnit = " km";
		} else {
			distanceConversionFactor = Constants.KM_TO_MI;
			distanceUnit = " mi";
		}
		double distance = route.getRouteDistance()*distanceConversionFactor;
		String dis = formatters.formatDistance(distance);
		double time = route.getTripTime();
		text.setText("Distance: " + dis + distanceUnit + "   Est. Time: " + formatters.formatTime(time));

		// Check checkbox if selected
		CheckBox checkbox = (CheckBox)vi.findViewById(R.id.checkBox);
		if(checkedRouteNames.contains(route.getName()))
			checkbox.setChecked(true);
		else
			checkbox.setChecked(false);

		return vi;
	}

}
