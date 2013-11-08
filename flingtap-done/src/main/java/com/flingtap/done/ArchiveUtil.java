// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.HashMap;
import java.util.Map;

import com.flingtap.done.provider.Task;
import com.flingtap.done.util.LicenseUtil;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

/**
 * Collection of utility logic for the Archive.
 *
 * NOTE: MonitorPhoneStateService.setupNotifications(..) prevents archived and completed tasks from triggering Nearminders.
 */
public class ArchiveUtil {

	public static int archiveTask(Context context, long taskId){
		int count = context.getContentResolver().update(Uri.parse("content://"+Task.AUTHORITY+"/archive_tasks/"+taskId), null, null, null);
		return count;
	}
	
	public static int unarchiveTask(Context context, long taskId){
		int count = context.getContentResolver().update(Uri.parse("content://"+Task.AUTHORITY+"/unarchive_tasks/"+taskId), null, null, null);
		return count;
		
	}
	
	public static int archiveCompletedTasks(Context context){
		int count = context.getContentResolver().update(Uri.parse("content://"+Task.AUTHORITY+"/archive_tasks"), null, null, null);
		
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(Event.ARCHIVE_TASKS__NUMBER_OF_TASKS, String.valueOf(count));
		Event.onEvent(Event.ARCHIVE_TASKS, parameters); 
		
		return count;
	}
	
	public static boolean isTaskArchived(Context context, String taskId){
		 Cursor cursor = context.getContentResolver().query(Uri.parse("content://"+Task.AUTHORITY+"/archive_tasks/"+taskId), null, null, null, null);
		 boolean retValue = cursor.getCount() > 0;
		 cursor.close();
		 return retValue; 
	}
	
	public static final boolean isArchiveEnabled(Context context){
		SharedPreferences settings = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
		boolean archiveEnabled = settings.getBoolean(ApplicationPreference.ENABLE_ARCHIVE, ApplicationPreference.ENABLE_ARCHIVE_DEFAULT);
		return (archiveEnabled && LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_ARCHIVING));
	}

}
