// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.io.File;
import java.util.Date;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;


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
 * Delete temp files which have exceeded their expiration date.
 *
 * @author Spencer Riddering
 */
public class TempFileHousekeeping  extends BroadcastReceiver {

	private static final String TAG = "TempFileHousekeeping";
	
	public static final String RESCHEDULE = "com.flingtap.done.actions.tempfile.cleanup.RESCHEDULE";
	public static final String CONSIDER = "com.flingtap.done.actions.tempfile.cleanup.CONSIDER";

	public void onReceive(Context context, Intent intent) {
		try{
			//Log.i(TAG, "Starting temp file cleanup process.");
			//Log.v(TAG, "onReceive(..) called.");
		    Uri uri = intent.getData();
		    String action = intent.getAction();
		    if( RESCHEDULE.equals(action) ){
		    	
				AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				PendingIntent pending = PendingIntent.getBroadcast(context, 0, new Intent(CONSIDER), 0);;
				
				//Log.d(TAG, "Registering cleanup intent with alarm service");
				long now = System.currentTimeMillis();
				long period = StaticConfig.TEMP_FILE_HOUSEKEEPING_PERIOD; 
				long offset = (long) (period * Math.random());
				//Log.v(TAG, "Period is " + period +"ms starting from " + new Date(now+offset));
				manager.setInexactRepeating(AlarmManager.RTC, now+offset, period, pending);
				
		    } else if( CONSIDER.equals(action) ){// Repeat every 24 hours.
				//Log.d(TAG, "Considering performing temp file cleanup.");
				
				SharedPreferences settings = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
				assert null != settings;
				
				// Find the last time the housekeeper was run.
				long lastRun = settings.getLong(ApplicationPreference.TEMP_FILE_HOUSEKEEPER_LAST_RUN, -1);
				
				long now = System.currentTimeMillis();
				if (lastRun >= 0L && lastRun + StaticConfig.TEMP_FILE_HOUSEKEEPING_INTERVAL > now) {
					return; //it has run too recently
				}
				//Log.d(TAG, "Last check was not too recent.");
				
	// TODO: Check battery status. Not possible yet.
//	       See: http://code.google.com/p/android/issues/detail?id=926
//				int status = BatteryManager.BATTERY_STATUS_CHARGING;
//				if (status != BatteryManager.BATTERY_STATUS_CHARGING) {
//					return; // Run at some other, better, time
//				}
//				//Log.d(TAG, "Battery is in a suitable state.");

				// Perform the work here.
				performWork(context);
				
				// Update the last run time.
				SharedPreferences.Editor editor = settings.edit();
				assert null != editor;
				editor.putLong(ApplicationPreference.TEMP_FILE_HOUSEKEEPER_LAST_RUN, now);
				editor.commit(); // TODO: Check the response when the code is fixed.
		    }
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0009C", exp);
			ErrorUtil.handleException("ERR0009C", exp, context);
		}
	}
	
	protected void performWork(Context context){
		Cursor cursor = context.getContentResolver().query(
				Task.TempFile.CONTENT_URI, 
				new String[]{Task.TempFile._ID}, 
				Task.TempFile._PRESERVE_UNTIL+"<?", 
				new String[]{String.valueOf(System.currentTimeMillis())}, 
				Task.TempFile._ID);
		assert null != cursor;
		int count;
		while(cursor.moveToNext()){ // This code could be moved into the TaskProvider's handling of multiple TempFile record deletes.
			count = context.getContentResolver().delete(
					ContentUris.withAppendedId(Task.TempFile.CONTENT_URI, cursor.getLong(0)), 
					null, 
					null);
			assert 0 != count;
		}
		cursor.close();
	}
	
	
}
