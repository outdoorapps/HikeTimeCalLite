package com.outdoorapps.hiketimecallite.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.support.constants.Constants;
import com.outdoorapps.hiketimecallite.support.constants.Defaults;
import com.outdoorapps.hiketimecallite.support.constants.PrefKeys;

public class ElevationRangeDialog extends DialogFragment {
	public ElevationRangeDialogListener mListener;
	private EditText maxElevationEdit, minElevationEdit;
	private TextView maxElevationView, minElevationView;
	private double maxElevation, minElevation;
	private InputMethodManager imm;
	private boolean isMetric;
	private Activity caller;

	public interface ElevationRangeDialogListener {
		public void onDialogPositiveClick(ElevationRangeDialog dialog);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mListener = (ElevationRangeDialogListener) activity;
		caller = activity;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.elevation_range_dialog, null);
		maxElevationEdit = (EditText) view.findViewById(R.id.max_elevation_input);
		minElevationEdit = (EditText) view.findViewById(R.id.min_elevation_input);
		maxElevationView = (TextView) view.findViewById(R.id.max_elevation);
		minElevationView = (TextView) view.findViewById(R.id.min_elevation);

		isMetric = Boolean.parseBoolean(MainActivity.main.getPref()
				.getString(PrefKeys.KEY_PREF_DISPLAY_UNIT, Defaults.DEFAULT_DISPLAY_UNIT));				
		if(isMetric) {
			maxElevationView.setText(getString(R.string.max_elevation) + " (m)");
			minElevationView.setText(getString(R.string.min_elevation) + " (m)");
		} else {
			maxElevationView.setText(getString(R.string.max_elevation) + " (ft)");
			minElevationView.setText(getString(R.string.min_elevation) + " (ft)");
		}

		imm = (InputMethodManager)caller.getSystemService(Context.INPUT_METHOD_SERVICE);
		builder.setTitle(getString(R.string.define_elevation_range))
		.setView(view)
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				maxElevation = Double.parseDouble(maxElevationEdit.getText()+"");
				minElevation = Double.parseDouble(minElevationEdit.getText()+"");
				imm.hideSoftInputFromWindow(maxElevationEdit.getWindowToken(), 0);
				mListener.onDialogPositiveClick(ElevationRangeDialog.this);
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				imm.hideSoftInputFromWindow(maxElevationEdit.getWindowToken(), 0);
			}
		});
		
		AlertDialog dialog = builder.create();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		dialog.show();
		return dialog;
	}

	public double getMaxElevation() {
		if(isMetric)
			return maxElevation;
		else
			return maxElevation*Constants.FEET_TO_METER;
	}

	public double getMinElevation() {
		if(isMetric)
			return minElevation;
		else
			return minElevation*Constants.FEET_TO_METER;
	}
	
}
