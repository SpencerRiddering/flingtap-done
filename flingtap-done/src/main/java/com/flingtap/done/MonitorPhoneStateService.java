// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.HashMap;
import java.util.Map;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flurry.android.FlurryAgent;
import com.flingtap.done.base.R;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Contacts;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * TODO: Notification dismissal should be pegged to the end of the call, and maybe some configurable amount of additional "linger" time. Needs a database table to track since services can lose their state.
 * TODO: ! It seems that onCallStateChanged(TelephonyManager.CALL_STATE_RINGING, ..) is only called for incomming calls. So, I must use the  
 * TODO: Shoudln't this be called "PhoneStateMonitorService" ?
 * 
 * 
 */
public class MonitorPhoneStateService extends Service {
	private static final String TAG = "MonitorPhoneStateService";
	private static final boolean mDebug = false; // TODO: Use PackageManager to get an ApplicationInfo object on your application, and check the flags field for FLAG_DEBUGGABLE.
	
	public static final String EXTRA_ENABLE_CALLMINDERS = "com.flingtap.done.intent.extra.ENABLE_CALLMINDERS";
	
	private PhoneStateListener mPhoneStateListener = null; // TODO: !!! Consider wrapping this in an atomic reference.
	
	
//	@Override
//	public void onCreate() {
//		try{
//			Log.v(TAG, "onCreate() called.");
//			super.onCreate();
//				
//
//	        
//		}catch(HandledException h){ // Ignore.
//		}catch(Exception exp){
//			Log.e(TAG, "ERR0003H", exp);
//			ErrorUtil.handleException("ERR0003H", exp, this);
//		}
//
//	}

	// This is the old onStart method that will be called on the pre-2.0
	// platform.  On 2.0 or later we override onStartCommand() so this
	// method will not be called*. 
	// *See note on  onStartCommand().
	public void onStart(Intent intent, int startId) {
		if( mDebug ){
			String action = null==intent?"null":intent.getAction();
			Log.i(TAG, "onStart("+action+", "+startId+")");
		}		
	    handleCommand(intent);
	    setForeground(true);
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		if( mDebug ){
			String action = null==intent?"null":intent.getAction();
			Log.i(TAG, "onStart("+action+", "+flags+", "+startId+")");
		}
	    handleCommand(intent);
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
	    return 1; // START_STICKY
	}

	private void handleCommand(Intent intent){
		try{

			boolean callmindersEnabled;
			if( null == intent ){ // If restarted intent will be null.
				callmindersEnabled = areCallmindersEnabled(this);
			}else{
				// Log.v(TAG, "onStart() called."); 
				callmindersEnabled = intent.getBooleanExtra(EXTRA_ENABLE_CALLMINDERS, areCallmindersEnabled(this));
			}
			if( mDebug ){
				Log.v(TAG, "callmindersEnabled == " + callmindersEnabled);
			}
			
			if( callmindersEnabled ){
				// Callminders should be enabled.
				enableCallminders();	
			}else{
				disableCallminders();
				
				stopSelf();
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000GH", exp);
			ErrorUtil.handleException("ERR000GH", exp, this);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		try{
			if( mDebug ){
				Log.v(TAG, "onDestroy() called.");
			}
			disableCallminders();
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0009Q", exp);
			ErrorUtil.handleException("ERR0009Q", exp, this);
		}
	}
	
	
	public static final boolean areCallmindersEnabled(Context context){
		SharedPreferences preferences = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
		
		return preferences.getBoolean(ApplicationPreference.CALLMINDER_ENABLED, ApplicationPreference.CALLMINDER_ENABLED_DEFAULT);
	}

	private void enableCallminders() { // TODO: !!! Does this need to be synchronized?
		if( null == mPhoneStateListener ){ // Check if the service has already been initialized.
			assert null == mPhoneStateListener;
			
			TelephonyManager telMan = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			assert null != telMan;
			
			
			mPhoneStateListener = new MyPhoneStateListener(this);
			
			telMan.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		}else{
			if( mDebug ){
				Log.v(TAG, "Service already running. No need to intialize again.");
			}
		}
	}


	private void disableCallminders() {
		if( null != mPhoneStateListener ){
			TelephonyManager telMan = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			assert null != telMan;
			telMan.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
		}
		mPhoneStateListener = null;
	}

	private class MyPhoneStateListener extends PhoneStateListener {
		
		MonitorPhoneStateService mService = null;
		public MyPhoneStateListener(MonitorPhoneStateService service){
			mService = service;
		}
		
		public void onCallStateChanged(int state, String dialedNumber) {
			try{
				if( mDebug ){
					Log.v(TAG, "onCallStateChanged("+state+", "+dialedNumber+") called.");
				}
				super.onCallStateChanged(state, dialedNumber);
				
				switch(state){
					case TelephonyManager.CALL_STATE_RINGING: // Only CALL_STATE_RINGING includes a phone number.
						if( mDebug ){
							Log.v(TAG, "state == TelephonyManager.CALL_STATE_RINGING");
						}
						assert null != dialedNumber;
						assert 0 != dialedNumber.length();
						
						if( null == dialedNumber || dialedNumber.length() == 0 ){ // TODO: Remove this code when assertions work.
							// TODO: ! Is the statement below correct?
							// User has not dialed the number yet.
							ErrorUtil.handle("ERR0003J", "null == phoneNumber || phoneNumber.length() == 0", this); // This isn't really an error,, I just want to know if the condition occurs so I know if I can remove this code.
							if( mDebug ){
								Log.v(TAG, "ERR0003J null == phoneNumber || phoneNumber.length() == 0");
							}
							return;
						}
						
						setupNotifications(mService, dialedNumber);
						
						break;
						
						
						// TODO: ! Use the CALL_STATE_IDLE to identify the end of the call and use this event to update the CancelNotificationBroadcastReceiver alarm to so that there are StaticConfig.CALLMINDER_TRAILING_DURATION milliseconds remaining before the Callminder notification is dismissed. 	
//			    			case TelephonyManager.CALL_STATE_IDLE:
//			    		        Log.v(TAG, "state == TelephonyManager.CALL_STATE_IDLE ");
//			    		        
//			    		    	NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
//			    		    	int personId = intent.getIntExtra(EXTRA_PERSON_ID, -1); 
//			    		    	if( -1 != personId ){
//			    		    		nm.cancel(personId);
//			    		    	}	    		        
//			    				break;
						
//			    			case TelephonyManager.CALL_STATE_OFFHOOK: // Does not include a number.
//			    		        Log.v(TAG, "state == TelephonyManager.CALL_STATE_OFFHOOK ");
//			    				break;
						
				}
			}catch(HandledException h){ // Ignore.
			}catch(Exception exp){
				Log.e(TAG, "ERR0003I", exp);
				ErrorUtil.handleException("ERR0003I", exp, mService);
			}
		}
		
	}
	
	
		
	@Override
	public IBinder onBind(Intent arg0) {
		// Not needed.
		return null;
	}
	
	private static final int CONTACT_PROJ_LOOKUP_KEY   = 0;
	private static final int CONTACT_PROJ_PHONE_NUMBER = 1;
	private static final int CONTACT_PROJ_DISPLAY_NAME = 2;
	private static final int CONTACT_PROJ_RAW_CONTACT_ID = 3;

	/**
	 * TODO: !!! Reconsider how to notify user. If a single number maps to multiple people then the user could receive too-many notifications.
	 * 
	 * dialedNumber.substring(0, -3)
	 * @param context
	 * @param dialedNumber
	 */
	public static void setupNotifications(Context context, String dialedNumber) {
		if( mDebug ){
			Log.v(TAG, "setupNotifications(context, "+dialedNumber+") called.");
		}
		Cursor phoneCursor = null;
		final int sdkVersion = Integer.parseInt( Build.VERSION.SDK ); // Build.VERSION.SDK_INT was introduced after API level 3 and so is not compatible with 1.5 devices.

		
		if( 5 > sdkVersion ){ // Anrdoid 1.x series code.
			phoneCursor = context.getContentResolver().query(
					android.provider.Contacts.Phones.CONTENT_URI, 
					new String[]{ android.provider.Contacts.Phones.PERSON_ID, android.provider.Contacts.PhonesColumns.NUMBER, android.provider.Contacts.PeopleColumns.DISPLAY_NAME},  
					android.provider.Contacts.Phones.NUMBER_KEY + " LIKE '" + PhoneNumberUtils.toCallerIDMinMatch(dialedNumber) + "%'", 
					null, 
					null);
		}else{ // Android 2.x series code.

			phoneCursor = context.getContentResolver().query(
					Uri.withAppendedPath(Uri.parse("content://com.android.contacts/phone_lookup"), Uri.encode(dialedNumber)), // PhoneLookup.CONTENT_FILTER_URI
					new String[]{ 
						"lookup", 		// ContactsContract.ContactsColumns.LOOKUP_KEY
						"number", 		// ContactsContract.PhoneLookup.NUMBER 
						"display_name", // ContactsContract.ContactsColumns.DISPLAY_NAME
						},  
					null, 
					null, 
					null);
			
			
//			phoneCursor = context.getContentResolver().query(
////					Uri.parse("content://com.android.contacts/data"), // ContactsContract.Data.CONTENT_URI
//					Uri.parse("content://com.android.contacts/phones"), // ContactsContract.CommonDataKinds.Phone.CONTENT_URI
//					new String[]{ 
//						"lookup", // ContactsContract.ContactsColumns.LOOKUP_KEY
//						"data1", // ContactsContract.CommonDataKinds.Phone.NUMBER 
//						"display_name"}, // ContactsContract.ContactsColumns.DISPLAY_NAME  
//					"data4 LIKE '" + PhoneNumberUtils.toCallerIDMinMatch(dialedNumber) + "%'", // data4 is not documented, but I'm not sure where else to find this reversed phone value.
//					null, 
//					null);

//			 Cursor c = getContentResolver().query(,
//			          new String[] {Data._ID, Phone.NUMBER, Phone.TYPE, Phone.LABEL},
//			          Data.CONTACT_ID + "=?" + " AND "
//			                  + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'",
//			          new String[] {String.valueOf(contactId)}, null);			
			
		}
		

		
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		int count = 0;
		
		// Possible that multiple contacts have the same phone number.
		while(phoneCursor.moveToNext()){
			String contactPhoneClean = PhoneNumberUtils.stripSeparators(phoneCursor.getString(CONTACT_PROJ_PHONE_NUMBER));
			if( mDebug ){
				Log.v(TAG, dialedNumber + " " + contactPhoneClean);
			}
			if( PhoneNumberUtils.compare(dialedNumber, contactPhoneClean) ){ // TODO: !! Consider whether this is needed in Android 2.x series.
				if( mDebug ){
					Log.v(TAG, dialedNumber);
				}

				
				if( mDebug ){
					Log.v(TAG, "lookup key " + phoneCursor.getString(CONTACT_PROJ_LOOKUP_KEY));
				}
				
				// Only notify user if there are tasks which _actually_ refer to the remote person.
//				Cursor taskIdForPersonCursor = TaskList.createDefaultFilteredTasksForPersonCursor(context, phoneCursor.getInt(CONTACT_PROJ_LOOKUP_KEY), new String[]{});
				Cursor taskIdForPersonCursor = TaskList.createDefaultFilteredTasksForPersonCursor(context, phoneCursor.getString(CONTACT_PROJ_LOOKUP_KEY), new String[]{});
				assert null != taskIdForPersonCursor;
				if( taskIdForPersonCursor.getCount() > 0 ){
					if( mDebug ){
						Log.v(TAG, "taskIdForPersonCursor.getCount() > 0 is true");
					}
					count++;
					
					// ***************************************
					// Create an entry in the notifications table
					// ***************************************
			        Uri notifUri = null;
			        
					if( 5 > sdkVersion ){ // Anrdoid 1.x series code.
						notifUri = NotificationUtil.createNotification(context, ContentUris.withAppendedId(Contacts.People.CONTENT_URI, phoneCursor.getInt(CONTACT_PROJ_LOOKUP_KEY)).toString());
					}else{ // Android 2.x series code.
						notifUri = NotificationUtil.createNotification(context, Uri.withAppendedPath(Uri.parse("content://com.android.contacts/contacts/lookup"), phoneCursor.getString(CONTACT_PROJ_LOOKUP_KEY)).toString());
					}
			        assert null != notifUri;
					final int notificationId = Integer.parseInt(notifUri.getLastPathSegment());
					if( mDebug){
						Log.v(TAG, "notificationId=="+ notificationId + " phoneCursor.getString(CONTACT_PROJ_LOOKUP_KEY)=="+phoneCursor.getString(CONTACT_PROJ_LOOKUP_KEY));
					}
			        
					// ***************************************************
					// Add alarm to remove notification for this person.
					// ***************************************************
					Intent alarmIntent = new Intent(CancelNotificationBroadcastReceiver.ACTION_CANCEL_NOTIFICATION);
					alarmIntent.setData(notifUri);
					alarmIntent.putExtra(CancelNotificationBroadcastReceiver.EXTRA_NOTIFICATION_ID, notificationId);
					
					PendingIntent pIntent = PendingIntent.getBroadcast(context, -1, alarmIntent, 0); 
					assert pIntent != null;
					AlarmManager aManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
					long alarmTime = SystemClock.elapsedRealtime() + StaticConfig.CALLMINDER_MAX_DURATION; 
					aManager.set(AlarmManager.ELAPSED_REALTIME, alarmTime, pIntent);

					
					// ***************************************
					// Add notification for this person.
					//
					// TODO: !! Adding two notifications in quick (<5 seconds) succession seems to leave one as a zombie which cannot be removed.
					//           (Also the previous one doesn't seem to get canceled when the flag is FLAG_CANCEL_CURRENT.
					// ***************************************
					
					// Create an intent for the notification to launch.
					Intent notifIntent = new Intent();
					notifIntent.setAction(Intent.ACTION_SEARCH); // TODO: This could be confused with another intent and so should have a separate name,, right?
					if( 5 > sdkVersion ){ // Anrdoid 1.x series code.
						notifIntent.setData(Task.Tasks.CONTENT_URI.buildUpon().appendQueryParameter(TaskList.EXTRA_QUERY_PERSON_ID, String.valueOf(phoneCursor.getInt(CONTACT_PROJ_LOOKUP_KEY))).build());  
					}else{ // Android 2.x series code.
						notifIntent.setData(Task.Tasks.CONTENT_URI.buildUpon().appendQueryParameter(TaskList.EXTRA_QUERY_PERSON_ID, phoneCursor.getString(CONTACT_PROJ_LOOKUP_KEY)).build());  
					}
					notifIntent.putExtra(CancelNotificationBroadcastReceiver.EXTRA_NOTIFICATION_ID, notificationId);
					notifIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Needed because activity will be launched from a non-Activity context.
					// TODO: !!! After this task launches, hitting the back button causes the app to exit,, even if I was looking at the main task list before hand. I think this person filter should run on top and then hitting back button takes you back to what you were doing before even if that was just viewing the task list. Consider using Intent.FLAG_ACTIVITY_MULTIPLE_TASK, but I don't have time to think through the special requirements.			
					PendingIntent pendNotifIntent = PendingIntent.getActivity(context, -1, notifIntent, 0);        		
					
					// Create the notification.
					Notification n = new Notification(R.drawable.list_reminder, context.getText(R.string.notify_contextTasksExist), System.currentTimeMillis()); 
					n.setLatestEventInfo(context, phoneCursor.getString(CONTACT_PROJ_DISPLAY_NAME), context.getText(R.string.notify_tasksRelatedToThisContact), pendNotifIntent) ;
					
					
			    	SharedPreferences preferences = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
			    	if( preferences.getBoolean(ApplicationPreference.CALLMINDER_VIBRATE, ApplicationPreference.CALLMINDER_VIBRATE_DEFAULT)){
			    		n.defaults |= Notification.DEFAULT_VIBRATE;
			    	}
			    	if( preferences.getBoolean(ApplicationPreference.CALLMINDER_FLASH, ApplicationPreference.CALLMINDER_FLASH_DEFAULT)){
			    		n.defaults |= Notification.DEFAULT_LIGHTS;
//			            n.ledARGB = Color.MAGENTA;
//			            n.ledOnMS = 300;
//			            n.ledOffMS = 1000;
//			            n.flags |= Notification.FLAG_SHOW_LIGHTS; 	    		
			    	}
			    	
			    	n.sound = Uri.parse(  preferences.getString(ApplicationPreference.CALLMINDER_RINGTONE, ApplicationPreference.CALLMINDER_RINGTONE_DEFAULT.toString()) );
					
					nm.notify(notificationId, n);
					
				}
				taskIdForPersonCursor.close();				
			}
		}
		phoneCursor.close();
		
		SessionUtil.onSessionStart(context);

		// Prepare event info.
		Map<String,String> parameters = new HashMap<String,String>();
		parameters.put(Event.INCOMING_CALLMINDER__NUMBER_OF_CONTACTS, String.valueOf(count));
		Event.onEvent(Event.INCOMING_CALLMINDER, parameters); 

		SessionUtil.onSessionStop(context);
	}

	public static void sendMessage(Context context, boolean callmindersEnabled){
        ComponentName cn = new ComponentName(StaticConfig.PACKAGE_NAME, MonitorPhoneStateService.class.getName()); 
        Intent monitorPhoneStateServiceIntent = new Intent();
        monitorPhoneStateServiceIntent.setComponent(cn);
        
        monitorPhoneStateServiceIntent.putExtra(EXTRA_ENABLE_CALLMINDERS, callmindersEnabled);
        
        ComponentName actualCN = context.startService(monitorPhoneStateServiceIntent);
        assert null != actualCN;
	}
	
	public static void startService(Context context){
        ComponentName cn = new ComponentName(StaticConfig.PACKAGE_NAME, MonitorPhoneStateService.class.getName()); 
        Intent monitorPhoneStateServiceIntent = new Intent();
        monitorPhoneStateServiceIntent.setComponent(cn);
        ComponentName actualCN = context.startService(monitorPhoneStateServiceIntent);
        assert null != actualCN;
	}
	
}
