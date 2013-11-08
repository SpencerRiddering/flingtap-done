// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews.ActionException;

import com.flingtap.done.provider.Task;
import com.flurry.android.FlurryAgent;
import com.google.android.maps.GeoPoint;
import com.tomgibara.android.veecheck.Veecheck;

/**
 * 
 * @author spencer
 *
 */
public class SetupBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG = "SetupBroadcastReceiver";
	
	public static final String SETUP_HEAVY_DELAY_ACTION = "com.flingtap.done.action.SETUP_HEAVY_DELAY";
	public static final String SETUP_SLIGHT_DELAY_ACTION = "com.flingtap.done.action.SETUP_SLIGHT_DELAY";
	
	public static void setupNotificationsAlarmsAndProximityAlerts(final Context context, boolean startup) {
		//Log.v(TAG, "setupNotificationsAlarmsAndProximityAlerts(..) called.");
		
		// *******************************************************
		// Clear out the notifications table
		// *******************************************************
		if( startup ){
			context.getContentResolver().delete(Task.Notification.CONTENT_URI, null, null);
			
			// *******************************************************
			// Reserve the Update Notification ID
			// *******************************************************
			ContentValues cv = new ContentValues(1);
			cv.put(Task.Notification._ID, UpdateService.NOTIFICATION_ID);
			cv.put(Task.Notification._URI, "-- Reserved for Update Notifications --");
			Uri notifUri = context.getContentResolver().insert(Task.Notification.CONTENT_URI, cv);
			assert null != notifUri;
		}
		
		AlarmManager aManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);        
		assert null != aManager;

		// *******************************************************
		// Register proximity alerts
		// *******************************************************
		Cursor proximityAlertCursor = context.getContentResolver().query(Task.ProximityAlerts.CONTENT_URI, new String[]{Task.ProximityAlerts._ID, Task.ProximityAlerts._GEO_URI, Task.ProximityAlerts.RADIUS}, Task.ProximityAlerts.ENABLED+"=?", new String[]{Task.ProximityAlerts.ENABLED_TRUE}, null);
		assert null != proximityAlertCursor;
		try{
			while(proximityAlertCursor.moveToNext()){
				GeoPoint geoPoint = Util.createPoint(proximityAlertCursor.getString(1));
				assert null != geoPoint;
				Nearminder.addOrUpdateProximityAlert(context, geoPoint, proximityAlertCursor.getInt(2), ContentUris.withAppendedId(Task.ProximityAlerts.CONTENT_URI, proximityAlertCursor.getLong(0)));
			}
		}finally{
			proximityAlertCursor.close();
			proximityAlertCursor = null;
		}
		
		// *******************************************************
		// Register Alarms.
		// *******************************************************

		// ************
		// Find alarms
		// ************
		Cursor alarmCursor = context.getContentResolver().query(Task.Tasks.CONTENT_URI, new String[]{Task.Tasks._ID, Task.Tasks.ALARM_TIME}, Task.Tasks.ALARM_ACTIVE + "=?", new String[]{Task.Tasks.ALARM_ACTIVE_TRUE}, null);
		assert null != alarmCursor;
		try{
			// ************
			// Add alarms.
			// ************
			while(alarmCursor.moveToNext()){
				assert !alarmCursor.isNull(1);
				// Add the alarm.
				//   Note: This does not work if you run the emulator from inside Eclipse. Run the emulator from the command line instead. 
				TaskProvider.registerAlarm(context, alarmCursor.getLong(1), ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, alarmCursor.getLong(0)), aManager);
			}
		}finally{
			alarmCursor.close();
		}

		// **************************************************
		// Setup Delayed Alarms
		// **************************************************
		if( startup ){
			setupDelayedAlarms(context, aManager);
		}
	}

	public static void clearNotificationsAlarmsAndProximityAlerts(final Context context) {
		//Log.v(TAG, "clearNotificationsAlarmsAndProximityAlerts(..) called.");

		// *******************************************************
		// UnRegister proximity alerts
		// *******************************************************
		Cursor proximityAlertCursor = context.getContentResolver().query(Task.ProximityAlerts.CONTENT_URI, new String[]{Task.ProximityAlerts._ID}, Task.ProximityAlerts.ENABLED+"=?", new String[]{Task.ProximityAlerts.ENABLED_TRUE}, null);
		assert null != proximityAlertCursor;
		try{
			while(proximityAlertCursor.moveToNext()){
				Nearminder.removeProximityAlert(context, ContentUris.withAppendedId(Task.ProximityAlerts.CONTENT_URI, proximityAlertCursor.getLong(0)));
			}
		}finally{
			proximityAlertCursor.close();
			proximityAlertCursor = null;
		}
	}
	
	public static void setupDelayedAlarms(Context context, AlarmManager aManager) {
		if( null == aManager ){
			aManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		}
    	assert null != aManager;
		
		// **************************************************
		// Common stuff
		// **************************************************
		Intent delayedAlarmsIntent = null;
		PendingIntent pIntent = null; 
    	
		// **************************************************
		// Setup SETUP_HEAVY_DELAY_ACTION
		// **************************************************
		delayedAlarmsIntent = new Intent(SETUP_HEAVY_DELAY_ACTION);
		delayedAlarmsIntent.setComponent(new ComponentName(context, SetupBroadcastReceiver.class));
		pIntent = PendingIntent.getBroadcast(context, -1, delayedAlarmsIntent, 0); 
		assert pIntent != null;
		aManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+ 10 * 60 * 1000, pIntent);// Run 10 minutes from now.
		
		// **************************************************
		// Setup SETUP_SLIGHT_DELAY_ACTION
		// **************************************************
		delayedAlarmsIntent = new Intent(SETUP_SLIGHT_DELAY_ACTION);
		delayedAlarmsIntent.setComponent(new ComponentName(context, SetupBroadcastReceiver.class));
		pIntent = PendingIntent.getBroadcast(context, -1, delayedAlarmsIntent, 0); 
		assert pIntent != null;
		aManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+  20 * 1000, pIntent);// Run 20 seconds from now.
		
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		
		if( SETUP_HEAVY_DELAY_ACTION.equals(action) ){
			// *******************************************************
			// Start Veecheck Reschedule Alarm
			// *******************************************************
			//Log.v(TAG, "Sending Update Reschedule Broadcast.");
			Intent rescheduleUpdateIntent = new Intent(Veecheck.getRescheduleAction(context));
			context.sendBroadcast(rescheduleUpdateIntent); 
			
		}else if( SETUP_SLIGHT_DELAY_ACTION.equals(action) ){
			
			// *******************************************************
			// Do immediately
			// *******************************************************
			
			// **************************************************
			// Callminder
			//   Start Phone State Monitor Service
			// **************************************************
			if( MonitorPhoneStateService.areCallmindersEnabled(context)){
				//Log.v(TAG, "Adding Phone State Monitor Service.");
				MonitorPhoneStateService.startService(context);
//			}else{
//				Log.v(TAG, "Phone State Monitor Service not needed because callminders are disabled.");
			}
			
			// *******************************************************
			// Delay
			// *******************************************************
			
			// **************************************************
			// Common stuff
			// **************************************************
			AlarmManager aManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			Intent delayedAlarmsIntent = null;
			PendingIntent pIntent = null; 

			// *******************************************************
			// Start Temp File Cleanup Alarm. 
			// *******************************************************
			//Log.v(TAG, "Creating Alarm for sending Temp File Reschedule Broadcast.");
			delayedAlarmsIntent = new Intent(TempFileHousekeeping.RESCHEDULE);
			pIntent = PendingIntent.getBroadcast(context, -1, delayedAlarmsIntent, 0); 
			assert pIntent != null;
			aManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+ 10 * 1000, pIntent);// Run 10 seconds from now.
			
			// *******************************************************
			// Start Archived Task Housekeeping Alarm. 
			// *******************************************************
			//Log.v(TAG, "Creating Alarm for sending Archived Task Reschedule Broadcast.");
			delayedAlarmsIntent = new Intent(ArchivedTaskHousekeeping.RESCHEDULE);
			pIntent = PendingIntent.getBroadcast(context, -1, delayedAlarmsIntent, 0); 
			assert pIntent != null;
			aManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+ 20 * 1000, pIntent);// Run 20 seconds from now.
			
			// *******************************************************
			// Start Completed Task Housekeeping Alarm. 
			// *******************************************************
			//Log.v(TAG, "Creating Alarm for sending Completed Task Housekeeping Broadcast.");
			delayedAlarmsIntent = new Intent(CompletedTaskHousekeeping.RESCHEDULE);
			pIntent = PendingIntent.getBroadcast(context, -1, delayedAlarmsIntent, 0); 
			assert pIntent != null;
			aManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+ 30 * 1000, pIntent);// Run 30 seconds from now.

		}
	}

}
