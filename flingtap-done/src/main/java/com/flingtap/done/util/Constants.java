// Licensed under the Apache License, Version 2.0

package com.flingtap.done.util;

import java.util.Calendar;

public final class Constants {

	/**
	 * Activity Action: Resolves a GPS position.
	 * 
	 * Input: Nothing.
	 * 
	 * Output: A GEO URI if the contact method could be resolved to a GPS position or null otherwise. 
	 * 
	 */
	public static final String ACTION_RESOLVE_POSITION = "com.flingtap.done.intent.action.RESOLVE_POSITION";   

	public static final long DEFAULT_NON_ID = 0;
	public static final int  DEFAULT_NON_POSITION = -1;
	public static final int  DEFAULT_DIALOG_NON_ID = -1;
	public static final int  DEFAULT_NON_VIEW_ID = -1;
	public static final int  DEFAULT_NON_REQUEST_ID = 0; // TODO: !!! Verify that 0 is an acceptable non-value here.
	
	public static final String PICK_POSTAL_CONTACT_METHOD_ACTION                 = "com.flingtap.done.intent.action.PICK_CONTACT_METHOD_ACTION";   
	public static final String FETCH_LOCATION_INFO_ACTION                 = "com.flingtap.done.intent.action.FETCH_LOCATION_INFO_ACTION";   
	public static final String CHECK_URI_MIGHT_CONTAIN_COORDINATES_ACTION = "com.flingtap.done.intent.action.CHECK_URI_MIGHT_CONTAIN_COORDINATES_ACTION";   
	
	// TODO: Shouldn't these be part of the Activity? Not sure....	
	public static final String BUNDLE_LATITUDE     = "BUNDLE_LATITUDE";     
	public static final String BUNDLE_LONGITUDE    = "BUNDLE_LONGITUDE";    
	public static final String BUNDLE_ZOOM_LEVEL   = "BUNDLE_ZOOM_LEVEL";   
	public static final String BUNDLE_IS_SATELLITE = "BUNDLE_IS_SATELLITE"; 
	public static final String BUNDLE_IS_TRAFFIC   = "BUNDLE_IS_TRAFFIC";   
	public static final String BUNDLE_DESCRIPTION  = "BUNDLE_DESCRIPTION";  
	
	
	public static final String GEOCODE_CONTACT   = "GEOCODE_CONTACT";

	public static final int LATITUDE_INT_NOT_SET         = 360000000;     
	public static final int LONGITUDE_INT_NOT_SET        = 360000000;    
	public static final int LATITUDE_DOUBLE_NOT_SET      = 360;     
	public static final int LONGITUDE_DOUBLE_INT_NOT_SET = 360;    
	public static final int IS_TRAFFIC_NOT_SET = -1;    
	public static final int IS_SATELLITE_NOT_SET = -1;    
	public static final int ZOOM_LEVEL_NOT_SET = -1;    

	// TODO: !!!! Move all references to this to UpdateActivityBase in com.flingtap.common.
	public static final String EXTRA_UPDATE_DEADLINE = "com.flingtap.done.intent.extra.UPDATE_DEADLINE"; // long, 
}
