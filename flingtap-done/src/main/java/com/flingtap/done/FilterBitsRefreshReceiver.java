// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.Calendar;
import java.util.TimeZone;

import com.flingtap.common.HandledException;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class FilterBitsRefreshReceiver extends BroadcastReceiver {
	private static final String TAG = "FilterBitsRefreshReceiver";
	
	private static final String ACTION_REFRESH_FILTER_BITS = "com.flingtap.done.intent.action.REFRESH_FILTER_BITS";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		try{
			//Log.i(TAG, "onReceive() called.");
			FilterUtil.applyFilterBits(context);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000L2", exp);
			ErrorUtil.handleException("ERR000L2", exp, context);
		}
	}

	public static void setupNotifications(Context context){
		//Log.i(TAG, "setupNotifications() called.");
		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND,0);
		Intent intent = new Intent(ACTION_REFRESH_FILTER_BITS);
		PendingIntent pendIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		am.setRepeating(AlarmManager.RTC, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendIntent);
	}

	public static void registerReceiver(Context context, FilterBitsRefreshReceiver receiver){
		IntentFilter filter = new IntentFilter(ACTION_REFRESH_FILTER_BITS);
		context.registerReceiver(receiver, filter);
	}
	
	public static void unregisterReceiver(Context context, FilterBitsRefreshReceiver receiver){
		try{
			context.unregisterReceiver(receiver);
		}catch(IllegalArgumentException e){
			// Ignore if the receiver is not registered.
		}
	}
	
}
