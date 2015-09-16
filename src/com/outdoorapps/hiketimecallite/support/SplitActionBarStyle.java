package com.outdoorapps.hiketimecallite.support;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

public class SplitActionBarStyle {

	public static final String SPLIT_ACTION_BAR_COLOR = "#aa000000";
	
	@SuppressLint("NewApi")
	public static void setSplitActionBarOverlayColor(ActionBar actionBar) {
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			actionBar.setSplitBackgroundDrawable(new ColorDrawable(Color.parseColor(SPLIT_ACTION_BAR_COLOR)));
		}
	}

}
