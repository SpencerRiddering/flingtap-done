// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flurry.android.FlurryAgent;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Generic BroadcastReceiver for removing a notification. 
 *
 * Set the EXTRA_NOTIFICATION_ID extra param to your notification id and then create an alarm to go off
 *     at some time in the future.
 */
public class CancelNotificationBroadcastReceiver extends BroadcastReceiver {
	public static final String TAG = "CancelNotificationBroadcastReceiver";

	public static final String ACTION_CANCEL_NOTIFICATION = "com.flingtap.done.action.CANCEL_NOTIFICATION";
	public static final String EXTRA_NOTIFICATION_ID = "com.flingtap.done.extra.NOTIFICATION_ID";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		try{
			//Log.v(TAG, "onReceive(..) called.");
			
			if( ACTION_CANCEL_NOTIFICATION.equals(intent.getAction()) ){
				//Log.v(TAG, "action is " + ACTION_CANCEL_NOTIFICATION);
				cancelNotification(context, intent);
			}         	
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00018", exp);
			ErrorUtil.handleException("ERR00018", exp, context);
		}
	}

	public static void cancelNotification(Context context, Intent intent) {
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		assert null != nm;
		
		int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
		assert -1 != notificationId;
		//Log.v(TAG, "notificationId=="+ notificationId);

		nm.cancel(notificationId); 
		
		// Notification record may have already been deleted elsewhere.
		context.getContentResolver().delete(ContentUris.withAppendedId(Task.Notification.CONTENT_URI, notificationId), null, null);
		 
	}
}
