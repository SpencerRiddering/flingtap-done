// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

/**
 * Contains configuration which is constant for a single version/release.
 */
public final class StaticConfig {

	/**
	 * Set this to true when the EULA is updated.
	 */
	public static final boolean HAVE_NEW_EULA = false;
	
	/**
	 * Set this to the email address that should receive the user feedback.
	 */
//	public static final String USER_FEEDBACK_EMAIL_ADDRESS = "FlingTap Done Feedback<done.feedback@flingtap.com>"; 
	public static final String USER_FEEDBACK_EMAIL_ADDRESS = "ftdone.feedback@leankeen.com"; 
	
	/**
	 * Set this to time period (in milliseconds) between cleaning up old temp files.
	 */
	public static final long TEMP_FILE_HOUSEKEEPING_PERIOD   = 48*60*60*1000; // 48 hours
	public static final long TEMP_FILE_HOUSEKEEPING_INTERVAL = 96*60*60*1000; // 96 hours

	/**
	 * Set this to time (in hours and minutes) when the archived tasks will be cleaned.
	 * 
	 * 3AM
	 */
	public static final int ARCHIVED_TASK_HOUSEKEEPING_TIME_HOUR = 3; 
	public static final int ARCHIVED_TASK_HOUSEKEEPING_TIME_MINUTE = 0; 

	/**
	 * Set this to the amount of time (in milliseconds) to hold onto the feedback SystemDetails.txt attachment temp file before deleting it.
	 */
	public static final long FEEDBACK_SYSTEM_DETAILS_FILE_LIFE_TIME = 6*60*60*1000; // 6 Hours
	
	/**
	 * Callminder maximum notification duration (in milliseconds).
	 */
	public static final long CALLMINDER_MAX_DURATION = 5 * 60 * 1000;  // 5 minutes.
	
	/**
	 * Callminder trailing notification duration (in milliseconds).
	 * 
	 *  The amount of time beyond the end of the phone call that the notification should stick around.
	 *  
	 *  TODO: !! This value is currently not implemented. See MonitorPhoneStateService for details.
	 */
	public static final long CALLMINDER_TRAILING_DURATION = 12 * 1000; // 12 seconds
	
	/**
	 * Veecheck update period (in milliseconds).
	 */
	public static final long VEECHECK_PERIOD = 24 * 60 * 60 * 1000L; // 24 hours. 
//	public static final long VEECHECK_PERIOD = 5 * 60 * 1000L; // 5 minutes 
	
	/**
	 * Veecheck update check interval (in milliseconds).
	 */
	public static final long VEECHECK_CHECK_INTERVAL = 48 * 60 * 60 * 1000L; // 48 hours.
//	public static final long VEECHECK_CHECK_INTERVAL = 10 * 60 * 1000L; // 10 minutes.
	
	/**
	 * Veecheck update check URI.
	 */
	public static final String VEECHECK_CHECK_URI = "http://www.leankeen.com/management/client/android/version.xml";

	/**
	 * Flag to indicate that we are developing.
	 * FIXME: Double check this value.
	 */
	public static final boolean PRODUCTION_MODE = ${command.line.productionMode};
	
	/**
	 * Application package name.
	 */
	public static final String PACKAGE_NAME = "com.flingtap.done.base";  

	/**
	 * Application package name.
	 */
    public static final String FLURRY_PROJECT_KEY = "${command.line.flurryId}";
}
