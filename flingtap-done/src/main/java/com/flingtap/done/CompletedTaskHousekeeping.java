// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flurry.android.FlurryAgent;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.util.Log;

/**
 *
 * TODO: Refactor generic Housekeeping logic out of CompletedTaskHousekeeping and ArchivedTaskHousekeeping.
 * @author Spencer Riddering
 * 
 */
public class CompletedTaskHousekeeping  extends BroadcastReceiver {

	private static final String TAG = "CompletedTaskHousekeeping";
	
	public static final String RESCHEDULE = "com.flingtap.done.actions.completedtask.cleanup.RESCHEDULE";
	public static final String CONSIDER = "com.flingtap.done.actions.completedtask.cleanup.CONSIDER";

	public void onReceive(Context context, Intent intent) {
		try{
			//Log.v(TAG, "onReceive(..) called.");
			
			SharedPreferences settings = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
			boolean archiveCompleted = ApplicationPreference.AUTO_ARCHIVE_COMPLETED_DEFAULT;
			boolean deleteCompleted  = ApplicationPreference.AUTO_DELETE_COMPLETED_DEFAULT;
			if( ArchiveUtil.isArchiveEnabled(context) ){
				archiveCompleted = settings.getBoolean(ApplicationPreference.AUTO_ARCHIVE_COMPLETED, ApplicationPreference.AUTO_ARCHIVE_COMPLETED_DEFAULT);
			}else{
				deleteCompleted = settings.getBoolean(ApplicationPreference.AUTO_DELETE_COMPLETED, ApplicationPreference.AUTO_DELETE_COMPLETED_DEFAULT);
			}
			
		    String action = intent.getAction();
		    if( RESCHEDULE.equals(action) ){
				
				PendingIntent pending = PendingIntent.getBroadcast(context, 0, new Intent(CONSIDER), 0); // 0 is correct. Just get the same PendingIntent over and over without changing it (or canceling it).
				AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				
				if( !archiveCompleted && !deleteCompleted ){
					manager.cancel(pending); 
					return;
				}
				
				//Log.d(TAG, "Registering cleanup intent with alarm service");
				GregorianCalendar triggerCal = (GregorianCalendar)GregorianCalendar.getInstance();
				
				boolean runToday = triggerCal.get(Calendar.HOUR_OF_DAY)<3;
				
				triggerCal.set(Calendar.HOUR_OF_DAY, StaticConfig.ARCHIVED_TASK_HOUSEKEEPING_TIME_HOUR ); 
				triggerCal.set(Calendar.MINUTE,      StaticConfig.ARCHIVED_TASK_HOUSEKEEPING_TIME_MINUTE ); 
				triggerCal.set(Calendar.SECOND,      0 ); 
				if( !runToday ){
					// Make first run tomorrow at 3AM
					triggerCal.add(Calendar.HOUR_OF_DAY, 24 );
				}// Else will run today at 3AM.
				
				//Log.v(TAG, "Next archived task run will be at: " + DateFormat.getInstance().format(triggerCal.getTime()));
				manager.setInexactRepeating(AlarmManager.RTC, triggerCal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pending); 
				
		    } else if( CONSIDER.equals(action) ){
				//Log.v(TAG, "Considering performing temp file cleanup.");
				
				
	// TODO: Check battery status. Not possible yet.
//	       See: http://code.google.com/p/android/issues/detail?id=926
//				int status = BatteryManager.BATTERY_STATUS_CHARGING;
//				if (status != BatteryManager.BATTERY_STATUS_CHARGING) {
//					return; // Run at some other, better, time
//				}
//				//Log.d(TAG, "Battery is in a suitable state.");

				// Perform the work here.
				performWork(context, archiveCompleted, deleteCompleted);
				
				// If the work might take longer than 10 seconds, then spin it off 
				//   into a Service.
				// context.startService(new Intent(PERFORM));
		    } 
		    
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000DE", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000DE", exp, context);
		}catch(ThreadDeath td){
			ErrorUtil.handleError("ERR000DE", td, context);
		}catch(OutOfMemoryError oome){
			ErrorUtil.handleError("ERR000DE", oome, context);
		}  
	}
	
	protected void performWork(Context context, boolean archiveCompleted, boolean deleteCompleted){

		SessionUtil.onSessionStart(context);
		if( archiveCompleted ){
			Event.onEvent(Event.ARCHIVE_COMPLETED_TASK_HOUSEKEEPING, null); // So we know how the deletion was initiated.
			ArchiveUtil.archiveCompletedTasks(context);
		}else if( deleteCompleted ){
			Event.onEvent(Event.DELETE_COMPLETED_TASK_HOUSEKEEPING, null); // So we know how the deletion was initiated.
    		TaskUtil.deleteCompleted(context);
		}else{
			ErrorUtil.handle("ERR000DF", "Unexpected condition", context);
		}

		SessionUtil.onSessionStop(context); // TODO: !!! Ask flurry if sessions initiated from BroadcastReceivers (or Services) will affect the user's active session if it happens to occur at the same time.
	}
	
	
}
