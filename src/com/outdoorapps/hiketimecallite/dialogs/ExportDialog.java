package com.outdoorapps.hiketimecallite.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.outdoorapps.hiketimecallite.R;

public class ExportDialog extends DialogFragment {
	private CheckBox imperialCheckBox;
	private String extension;
	private boolean isMetric;
	private int selectedItem;
	public ExportDialogListener mListener;
	
	public interface ExportDialogListener {
        public void onDialogPositiveClick(ExportDialog dialog);
    }
    
    public void setListener(ExportDialogListener mListener) {
    	this.mListener = mListener;
    }
    
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.export_dialog, null);
		imperialCheckBox = (CheckBox) view.findViewById(R.id.imperial_elevation_checkbox);
		
		selectedItem = 0;
		isMetric = true;
		builder.setTitle(R.string.export_route_dialog_title)
		.setView(view)
		.setSingleChoiceItems(R.array.export_file_types, selectedItem, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int item) {
				selectedItem = item;				
			}
		})
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				isMetric = !(imperialCheckBox.isChecked());
				mListener.onDialogPositiveClick(ExportDialog.this);
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
			}
		});
		return builder.create();
	}
	
	public String getExtension() {
		if(selectedItem==0)
			extension = ".gpx";
		else
			extension = ".kml";
		return extension;
	}
	
	public boolean isMetric() {
		return isMetric;
	}
}
