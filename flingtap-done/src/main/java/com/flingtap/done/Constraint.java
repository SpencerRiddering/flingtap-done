// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.net.Uri;

import com.flingtap.done.provider.Task;

/**
 *
 * When querying/updating/deleting tasks, constraints are used to specify which tasks are affected.
 *
 * These textual pieces are strung together when constructing URIs used with the Tasks content provider.
 * Most are URI params.
 *
 * @author spencer
 *
 */
public final class Constraint {
	private Constraint(){}
	
	public static final String CONSTRAINT = "mask"; // Twig
	
	// TODO: ! Add a default value to each constraint to synchronize the value used by TaskProvider and the un-initialized value use by the filter element delegate. Example: STATUS_PARAM_OPTION_DEFAULT_VALUE
	public static class Version1{
		private Version1(){}
		
		public static final String VERSION = "1"; // TODO: !!! Consider removing this.

    	public static final String PARAM_INCLUSIVE = "inclusive";

		public static final String DUE = "due"; // Twig
		public static final String DUE_CONTENT_URI_STRING = "content://"+Task.AUTHORITY+"/tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+DUE; 
		public static final String INCLUDE_NO_DUE_DATE_ITEMS = "include_not_due"; // Parameter.
		public static final String DAYS_FROM_TODAYS_END = "dfte"; // Days from today's end. 
		public static final String WEEKS_FROM_THIS_WEEKS_END = "wftwe"; // Weeks from this week's end. 
		public static final String ANYTIME = "anytime"; //  
		public static final String PAST = "past"; //  
		public static final String DISABLE = "disable"; // Don't display any tasks with due dates (does not address tasks without due dates).

		public static final String ARCHIVE = "archive"; // Twig
		public static final String ARCHIVE_URI_PATTERN_STRING = "tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+ARCHIVE; 
		public static final String ARCHIVE_CONTENT_URI_STRING = "content://"+Task.AUTHORITY  + "/" + ARCHIVE_URI_PATTERN_STRING ;
		
		public static final String REPOSITORY = "repository"; // Twig
		public static final String REPOSITORY_URI_PATTERN_STRING = "tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+REPOSITORY; 
		public static final String REPOSITORY_CONTENT_URI_STRING = "content://"+Task.AUTHORITY  + "/" + REPOSITORY_URI_PATTERN_STRING ;
		public static final String REPOSITORY_PARAM = "repository"; //  
		public static final String REPOSITORY_PARAM_OPTION_MAIN = "0";     
		public static final String REPOSITORY_PARAM_OPTION_ARCHIVE = "1";   
		public static final String REPOSITORY_PARAM_OPTION_DEFAULT = REPOSITORY_PARAM_OPTION_MAIN;

		public static final String STATUS = "completed"; // Twig
		public static final String STATUS_URI_PATTERN_STRING = "tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+STATUS; 
		public static final String STATUS_CONTENT_URI_STRING = "content://"+Task.AUTHORITY  + "/" + STATUS_URI_PATTERN_STRING ;
		public static final String STATUS_PARAM_OPTION = "option"; //  
		public static final String STATUS_PARAM_OPTION_BOTH          = "0"; // Both 
		public static final String STATUS_PARAM_OPTION_COMPLETED     = "1"; // Completed  
		public static final String STATUS_PARAM_OPTION_NOT_COMPLETED = "2"; // Not completed  
		public static final String STATUS_PARAM_OPTION_DATE_RANGE    = "3"; // Date range  
		public static final String STATUS_PARAM_OPTION_DEFAULT_VALUE = STATUS_PARAM_OPTION_BOTH; //  
		public static final String STATUS_PARAM_FROM_DATE = "fromDate"; 
		public static final String STATUS_PARAM_TO_DATE   = "toDate"; 

		public static final String PRIORITY = "priority"; // Twig
		public static final String PRIORITY_URI_PATTERN_STRING = "tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+PRIORITY; 
		public static final String PRIORITY_CONTENT_URI_STRING = "content://"+Task.AUTHORITY + "/" + PRIORITY_URI_PATTERN_STRING; 
		public static final String PRIORITY_PARAM = "priority"; //  
		public static final String PRIORITY_VALUE_NONE   = "0"; // No priority  
		public static final String PRIORITY_VALUE_LOW    = "1"; // Low priority  
		public static final String PRIORITY_VALUE_MEDIUM = "2"; // Medium priority  
		public static final String PRIORITY_VALUE_HIGH   = "3"; // High priority  
		
		public static final String LABEL = "label"; // Twig
		public static final String LABEL_CONTENT_URI_STRING = "content://"+Task.AUTHORITY+"/tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+LABEL; 
		public static final String LABEL_PARAM_ENABLED = "enabled"; //  
		public static final String LABEL_PARAM_ENABLED_VALUE_FALSE = "0"; // Not enabled.  
		public static final String LABEL_PARAM_ENABLED_VALUE_TRUE  = "1"; // Enabled.
		
		public static final String UNLABELED = "unlabeled"; // Twig
		public static final String UNLABELED_CONTENT_URI_STRING = "content://"+Task.AUTHORITY+"/tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+UNLABELED; 
		public static final String UNLABELED_PARAM_ENABLED = "enabled"; //  
		public static final String UNLABELED_PARAM_ENABLED_VALUE_FALSE = "0"; // Not enabled.  
		public static final String UNLABELED_PARAM_ENABLED_VALUE_TRUE  = "1"; // Enabled.
		
		public static final String ALL = "all"; // Twig
		public static final String ALL_CONTENT_URI_STRING = "content://"+Task.AUTHORITY+"/tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+ALL; 
		public static final Uri    ALL_CONTENT_URI = Uri.parse(ALL_CONTENT_URI_STRING); 
		
		public static final String NONE = "none"; // Twig
		public static final String NONE_CONTENT_URI_STRING = "content://"+Task.AUTHORITY+"/tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+NONE;
	}
	
	
}
