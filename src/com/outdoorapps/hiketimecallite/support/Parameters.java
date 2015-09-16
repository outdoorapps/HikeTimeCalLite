package com.outdoorapps.hiketimecallite.support;

public class Parameters {
	public static boolean parametersCheck(String input) {
		if(input.equals(""))
			return false;
		else {
			try{ 
				Double.parseDouble(input);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}
	}
}
