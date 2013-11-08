// Licensed under the Apache License, Version 2.0

package com.flingtap.done.addon.quicklaunch;

/**
 * Contains configuration which is constant for a single version/release.
 *
 */
public final class StaticConfig {

	/**
	 * Set this to true when the EULA is updated.
	 */
	public static final boolean HAVE_NEW_EULA = false;
	
	/**
	 * Veecheck update period (in milliseconds).
	 */
	public static final long VEECHECK_PERIOD = 48 * 60 * 60 * 1000L; // 48 hours (2 days). 
//	public static final long VEECHECK_PERIOD = 1 * 60 * 1000L; // 1 minutes 
	
	/**
	 * Veecheck update check interval (in milliseconds).
	 */
	public static final long VEECHECK_CHECK_INTERVAL = 168 * 60 * 60 * 1000L; // 168 hours (7 days). 
//	public static final long VEECHECK_CHECK_INTERVAL = 1 * 60 * 1000L; // 1 minutes.
	
	/**
	 * Veecheck update check URI.
	 */
//	public static final String VEECHECK_CHECK_URI = "http://www.flingtap.com/done/management/client/android/version.xml"; // TODO: !!! Switch to use this url.   
	public static final String VEECHECK_CHECK_URI = "http://www.leankeen.com/management/client/android/version.xml";  
	
	/**
	 * Flurry application ID.
	 */
	public static final String FLURRY_PROJECT_KEY = "YJ9KCLU5UW7JZHVAKJ6M"; 
	
	
}
