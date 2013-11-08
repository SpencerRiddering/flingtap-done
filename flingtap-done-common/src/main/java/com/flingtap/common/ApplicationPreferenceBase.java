// Licensed under the Apache License, Version 2.0

package com.flingtap.common;

public class ApplicationPreferenceBase {

	public static final String NAME = "com.flingtap.prefs.name";

	public static final String APPLICATION_VERSION = "application_version";
	public static final String APPLICATION_NEW_VERSION = "application_new_version";
	
	public static final String UPDATE_REQUIRED_DEADLINE = "update_required_deadline"; // Not configurable by user (set by downloading the versions.xml from server). 
	public static final String UPDATE_REQUIRED_URI = "update_required_uri"; // Not configurable by user (set by downloading the versions.xml from server). Used as a key for requesting that a new version be checked for from the preferenes screen.
	
	public static final String EULA_ACCEPTANCE_REQUIRED = "eula_acceptance_required";
	public static final boolean EULA_ACCEPTANCE_REQUIRED_DEFAULT = true;
	
}
