// Licensed under the Apache License, Version 2.0

package com.flingtap.done.backup;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.flingtap.done.ApplicationPreferenceActivity;
import com.flingtap.done.NotificationUtil;
import com.flingtap.done.base.R;

public final class BackupNotifications {
	private static final String TAG = "BackupNotifications";

	private static final String EXTRA_NOTIFY_URI = "com.flingtap.done.backup.notify.uri";
	
	public static void notify(Context context, String url, int titleResId, int descriptionResId){
		BackupNotifications.notify(context, url, titleResId, descriptionResId, null);
	}
	public static void notify(Context context, String url, int titleResId, int descriptionResId, Intent intent){
		// Log.d(TAG, "url="+url);
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		if( null == intent ){
			intent = ApplicationPreferenceActivity.getLaunchIntent(context);
		}
		
		Uri notifUri = NotificationUtil.createNotification(context, url);
		assert null != notifUri;
		
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(EXTRA_NOTIFY_URI, url);
		
		PendingIntent pIntent = PendingIntent.getActivity(context, -1, intent, 0); 
	    
	    Notification n = new Notification(R.drawable.tutorial_notif, context.getText(titleResId), System.currentTimeMillis());
	    n.setLatestEventInfo(context, context.getText(titleResId), context.getText(descriptionResId), pIntent) ;
	    n.defaults = Notification.DEFAULT_ALL;	
	    
        // NOTE: There should be no harm in re-sending the notification since it will simply replace an existing notification.
	    nm.notify(Integer.parseInt(notifUri.getLastPathSegment()), n);
	                                                     
	}
	
	public static boolean needsClear(Intent intent){
		return intent.hasExtra(EXTRA_NOTIFY_URI);
	}
	public static void clear(Context context, Intent intent ){
		String notifyUri = intent.getStringExtra(EXTRA_NOTIFY_URI);
		intent.removeExtra(EXTRA_NOTIFY_URI);
		// Log.d(TAG, "url="+notifyUri.toString());
		NotificationUtil.removeNotification(context, Uri.parse(notifyUri));
	}

}
