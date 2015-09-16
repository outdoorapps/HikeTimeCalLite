package com.outdoorapps.hiketimecallite.support;

import android.content.Context;
import android.content.pm.PackageManager;

public class Verifier {
	
	public static boolean verifyProVersion(Context context) {
		boolean installed = appInstalledOrNot("com.outdoorapps.hiketimecal", context);  
        if(installed)
        	return true;
        else
        	return false;
	}
	
	private static boolean appInstalledOrNot(String uri, Context context) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed = false;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }
}
