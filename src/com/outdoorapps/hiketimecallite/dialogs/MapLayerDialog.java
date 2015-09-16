package com.outdoorapps.hiketimecallite.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;

import com.google.android.gms.maps.GoogleMap;
import com.outdoorapps.hiketimecallite.MainActivity;
import com.outdoorapps.hiketimecallite.R;

public class MapLayerDialog extends DialogFragment {
	private final CharSequence[] items = {"Google Road","Google Satellite","Google Terrain","Google Hybrid"};

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final GoogleMap map = MainActivity.main.getGoogleMap();
		final int currentMapType = map.getMapType();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.select_map_layer)
		.setSingleChoiceItems(items, currentMapType-1, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int item) {
				switch(item) {
				case 0:
					map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
					break;
				case 1:
					map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
					break;
				case 2:
					map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);     
					break;
				case 3:
					map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
					break;
				default:
					map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
					break;
				}
			}
		})
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				map.setMapType(currentMapType);
			}
		}).setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey (DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK 
						&& event.getAction() == KeyEvent.ACTION_UP
						&& !event.isCanceled()) {
					dialog.cancel();
					map.setMapType(currentMapType);
					return true;
				}
				return false;
			}
		});
		return builder.create();
	}

}

