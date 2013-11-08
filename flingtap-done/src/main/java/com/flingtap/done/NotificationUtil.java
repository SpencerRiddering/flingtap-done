// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.util.Log;

import com.flingtap.done.provider.Task;

/**
 * 
 * @author spencer
 * 
 * TODO: !!!! Add a "remove when launched" flag so we know which notifications can be cleared out when a FlingTap Done Activity is launched.
 */
public class NotificationUtil {
	private final static String TAG = "NotificationUtil";
	
	// Not thread safe.
	public static Uri createNotification(Context context, String uri) {
		assert null != uri;
		
		Uri notifUri = null;
		Cursor notifCursor = context.getContentResolver().query(Task.Notification.CONTENT_URI, new String[]{Task.Notification._ID}, Task.Notification._URI+"=?", new String[]{uri}, null);
		assert null != notifCursor;
		
		if( notifCursor.moveToFirst() ){
			// The notification already exists.
			notifUri = ContentUris.withAppendedId(Task.Notification.CONTENT_URI, notifCursor.getLong(0));
		}else{
			ContentValues cv = new ContentValues(1);
			cv.put(Task.Notification._URI, uri); 
			notifUri = context.getContentResolver().insert(Task.Notification.CONTENT_URI, cv);
		}
		notifCursor.close();
		assert null != notifUri;
		assert 0 < Integer.parseInt(notifUri.getLastPathSegment());
		return notifUri;
	}
		
	// **********************************************
	// Notifications
	// **********************************************
	public static boolean removeNotification(Context context, Uri uri) {
		assert null != uri;
		
        Cursor notifCursor = context.getContentResolver().query(Task.Notification.CONTENT_URI, new String[]{Task.Notification._ID}, Task.Notification._URI+"=?", new String[]{uri.toString()}, null);
        assert null != notifCursor;
        if( !notifCursor.moveToFirst() ){
        	// Not a problem. There just isn't a notification at this time. Very common case.
            notifCursor.close();
        	return false;
        }
        int id = notifCursor.getInt(0);
        notifCursor.close();
        
    	NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    	nm.cancel(id);
    	int count = context.getContentResolver().delete(ContentUris.withAppendedId(Task.Notification.CONTENT_URI, id), null, null);
    	assert 1 == count;
    	
    	return 0 < count;
	}

}
