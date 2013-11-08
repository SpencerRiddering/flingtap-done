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
 * @author Spencer Riddering
 */
public class ArchivedTaskHousekeeping  extends BroadcastReceiver {

	private static final String TAG = "ArchivedTaskHousekeeping";
	
	public static final String RESCHEDULE = "com.flingtap.done.actions.archivedtask.cleanup.RESCHEDULE";
	public static final String CONSIDER = "com.flingtap.done.actions.archivedtask.cleanup.CONSIDER";

	public void onReceive(Context context, Intent intent) {
		try{
			//Log.v(TAG, "onReceive(..) called.");
		    String action = intent.getAction();
		    if( RESCHEDULE.equals(action) ){
		    	
				AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				PendingIntent pending = PendingIntent.getBroadcast(context, 0, new Intent(CONSIDER), 0);;
				
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
				performWork(context);
				
				// If the work might take longer than 10 seconds, then spin it off 
				//   into a Service.
				// context.startService(new Intent(PERFORM));
		    } 
		    
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0000O", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0000O", exp, context);
		}catch(ThreadDeath td){
			ErrorUtil.handleError("ERR0000O", td, context);
		}catch(OutOfMemoryError oome){
			ErrorUtil.handleError("ERR0000O", oome, context);
		}  
	}
	
	protected void performWork(Context context){

		SessionUtil.onSessionStart(context);
		Event.onEvent(Event.ARCHIVE_TASK_HOUSEKEEPING, null); // So we know how the deletion was initiated.
		
		SharedPreferences settings = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
		int retainDays = settings.getInt(ApplicationPreference.ARCHIVE_RETAIN_DAYS, ApplicationPreference.ARCHIVE_RETAIN_DAYS_DEFAULT);
		Uri.Builder uriBuilder = Uri.parse("content://"+Task.AUTHORITY+"/archive_tasks").buildUpon();
		
		GregorianCalendar cal = (GregorianCalendar)GregorianCalendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY,  0);
		cal.set(Calendar.MINUTE,       0);
		cal.set(Calendar.SECOND,       0);
		cal.add(Calendar.DATE, (-1)*retainDays);

		uriBuilder.appendQueryParameter("older_than", String.valueOf(cal.getTimeInMillis()));
		
		int count = context.getContentResolver().delete(
				uriBuilder.build(),
				null, 
				null);
		
		// Add Delete task event.
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(Event.DELETE_TASKS__NUMBER_OF_TASKS, String.valueOf(count));
		Event.onEvent(Event.DELETE_TASKS, parameters);
		
// TODO: !!! Add a nightly check to archive tasks which are completed (when archiving in enabled.) Isn't this already implemented for deleting completed tasks? 
// TODO: !!! The following event will record the nightly archive of completed tasks mentioned in the TODO just above.
//		Event.onEvent(Event.COMPLETED_TASK_HOUSEKEEPING, null); // So we know how the archival was initiated.
//		Map<String, String> parameters = new HashMap<String, String>();
//		parameters.put(Event.ARCHIVE_TASKS__METHOD, Event.ARCHIVE_TASKS__METHOD__HOUSE_KEEPING);
//		parameters.put(Event.ARCHIVE_TASKS__NUMBER_OF_TASKS, String.valueOf(count));
//		Event.onEvent(Event.ARCHIVE_TASKS, parameters); 

		SessionUtil.onSessionStop(context); // TODO: !!! Ask flurry if sessions initiated from BroadcastReceivers (or Services) will affect the user's active session if it happens to occur at the same time.
		
		
		//Log.i(TAG, "Removed "+count+" archived task(s) older than the configured limit (keep for "+retainDays+" days, which means archived tasks older than " + DateFormat.getInstance().format(cal.getTime()) +")");
	}
	
	
}
