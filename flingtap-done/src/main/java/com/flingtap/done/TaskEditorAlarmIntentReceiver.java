// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;

// TODO: !!!!! When phone starts, if an alarm notification exists, don't wipe it out, instead re-query to verify it's still valid and then re-add it to the status bar.
/**
 * Handle alarms created by user in the Task Editor. 
 * 
 * Results in a notification for user.
 *
 */
public class TaskEditorAlarmIntentReceiver extends BroadcastReceiver {
	private final static String TAG = "TaskEditorAlarmIntentReceiver";
	
    public static final String ALARM_ACTION = "com.flingtap.done.TaskEditor.ALARM_ACTION";
	
	
	/**
	 * Standard projection for the interesting columns of a normal task.
	 */
	private static final String[] TASK_PROJECTION = new String[] {
			Task.Tasks._ID, // 0
			Task.Tasks.TASK_DESC, // 1
			Task.Tasks.TASK_TITLE, // 2
			Task.Tasks.ALARM_TIME, // 3
	};
	private static final int ID_INDEX = 0;
	private static final int TASK_INDEX = 1;
	private static final int TITLE_INDEX = 2;
	private static final int ALARM_TIME_INDEX = 3;
	private Cursor mCursor;
	
	public TaskEditorAlarmIntentReceiver() {
	}

	public void onReceive(Context context, Intent intent) {
		try{
			//Log.v(TAG, "onReceive(..) called.");
			
			SessionUtil.onSessionStart(context);
			Event.onEvent(Event.ALARM_OCCURRED, null); // Map<String,String> parameters = new HashMap<String,String>();

		    Uri uri = intent.getData();
		    assert uri != null;
		    assert uri.toString().startsWith(Task.Tasks.CONTENT_URI_STRING);
		    assert -1 != ContentUris.parseId(uri);
		    NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		    
		    // Check that the alarm is indeed active. 
		    Cursor alarmCursor = context.getContentResolver().query(uri, null, Task.Tasks.ALARM_ACTIVE+"=?", new String[]{Task.Tasks.ALARM_ACTIVE_TRUE}, null);
		    assert null != alarmCursor;
		    
		    if(alarmCursor.getCount() != 1){ // The alarm is active.
		    	// The alarm is not active.
		    	ErrorUtil.handle("ERR0007Y", "Alarm occurred for non-active alarm.", this);
		    	assert false;
		    }
		    	
	    	// Deactivate the alarm.
	    	ContentValues values = new ContentValues(1);
	    	values.put(Task.Tasks.ALARM_ACTIVE, Task.Tasks.ALARM_ACTIVE_FALSE);
	    	int count = context.getContentResolver().update(uri, values, null, null);
	    	assert 1 == count;

	    	mCursor = context.getContentResolver().query(uri, TASK_PROJECTION, null, null, null);
	    	mCursor.moveToFirst();

	    	Intent viewAlertIntent = new Intent();
	    	viewAlertIntent.setAction(TaskEditor.NOTIFY_ACTION);
	    	viewAlertIntent.setData(uri);
	    	viewAlertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Needed because activity will be launched from a non-Activity context.
	    	PendingIntent pIntent2 = PendingIntent.getActivity(context, -1, viewAlertIntent, PendingIntent.FLAG_CANCEL_CURRENT);
	    	
	    	Notification n = new Notification(R.drawable.bkcheck, mCursor.getString(TITLE_INDEX), mCursor.getLong(ALARM_TIME_INDEX));
	    	n.setLatestEventInfo(context, mCursor.getString(TITLE_INDEX), mCursor.getString(TASK_INDEX), pIntent2);

	    	SharedPreferences preferences = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
	    	if( preferences.getBoolean(ApplicationPreference.ALARM_VIBRATE, ApplicationPreference.ALARM_VIBRATE_DEFAULT)){
	    		n.defaults |= Notification.DEFAULT_VIBRATE;
	    	}
	    	if( preferences.getBoolean(ApplicationPreference.ALARM_FLASH, ApplicationPreference.ALARM_FLASH_DEFAULT)){
	    		n.defaults |= Notification.DEFAULT_LIGHTS;
	    	}
	    	
			n.sound = Uri.parse(  preferences.getString(ApplicationPreference.ALARM_RINGTONE, ApplicationPreference.ALARM_RINGTONE_DEFAULT.toString()) );
	    	
	    	Uri notifUri = NotificationUtil.createNotification(context, ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, mCursor.getInt(ID_INDEX)).toString());
	    	assert null != notifUri;
	    	nm.notify(Integer.parseInt(notifUri.getLastPathSegment()), n);
	    	
	    	mCursor.close();

	    	Intent alarmIntent = new Intent(ALARM_ACTION, uri);
	    	PendingIntent pIntent = PendingIntent.getBroadcast(context, -1, alarmIntent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_CANCEL_CURRENT); // TODO: Revist this. Should I avoid this step by storing the original PendingIntent somewhere? Is this the right flag for what I'm doing?
	    	if( null != pIntent ){
	    		AlarmManager aManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	    		aManager.cancel(pIntent);
	    	}
		    	
		    alarmCursor.close();

			SessionUtil.onSessionStop(context);
		    
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0007X", exp);
			ErrorUtil.handleException("ERR0007X", exp, context);
		}
	}
}
