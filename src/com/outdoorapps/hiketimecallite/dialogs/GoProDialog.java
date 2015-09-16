package com.outdoorapps.hiketimecallite.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.outdoorapps.hiketimecallite.R;
import com.outdoorapps.hiketimecallite.support.constants.Links;

public class GoProDialog extends DialogFragment {

	private String functionName;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.gopro_title)
		.setMessage(getString(R.string.gopro_message) + ": " + functionName)
		.setPositiveButton(R.string.gopro, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// link to pro version
				Uri uri = Uri.parse(Links.PRO_LINK);
				startActivity(new Intent(Intent.ACTION_VIEW, uri));
			}
		})
		.setNegativeButton(R.string.go_back, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// Do nothing
			}
		});
		return builder.create();
	}
	
	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}
}
