package com.outdoorapps.hiketimecallite.support;

public class FileSupport {
	
	public static String removeExtension(String fileName) {
	    int lastPeriodPos = fileName.lastIndexOf('.');
	    if (lastPeriodPos <= 0)
	        return fileName;
	    else
	        return fileName.substring(0, lastPeriodPos);
	}
	
	public static String getExtension(String fileName) {
	    int lastPeriodPos = fileName.lastIndexOf('.');
	    if (lastPeriodPos <= 0) // no extension
	        return "";
	    else
	        return fileName.substring(lastPeriodPos, fileName.length());
	}
}
