package com.outdoorapps.hiketimecallite.support;

public class Elevation {
	public final static double MAX_ELEVATION = 8850; // Highest land elevation
	public final static double MIN_ELEVATION = -500; // Lowest land elevation
	
	public static boolean isElevationValid(Double elevation) {
		if(elevation!=null) {
			if(elevation>MAX_ELEVATION || elevation<MIN_ELEVATION)
				return false;
			else
				return true;
		} else
			return false;		
	}
	
	@SuppressWarnings("serial")
	public static class ElevationOutOfBoundException extends Exception {
		public ElevationOutOfBoundException () {

		}
	}
}
