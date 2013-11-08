// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.List;

import com.flingtap.done.provider.Task;
import com.google.android.maps.GeoPoint;
import com.flingtap.done.base.R;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Contacts;
import android.util.Log;
import android.widget.Toast;

/**
 * TODO: !!! Since there is no way to manage (ie view/delete) nearminders, there should be a background thread (or install time action) to cleanup any orphaned nearminders (ie nearminders that don't have an associated attachment. 
 * TODO: Maybe add low priority (once a week) background thread to look for orphaned proximity alerts.  
 * @author spencer
 *
 * 
 */
public class Nearminder {
	private static final String TAG = "Nearminder";
	
	public static boolean delete(final Context context, final Uri proxUri) {
		if( deleteNoToast(context, proxUri) ){
			Toast.makeText(context, R.string.nearminderDeleted, Toast.LENGTH_SHORT).show();
			return true;
		}else{
			// Let the caller handle the error condition.
			return false;
		}
	}
	public static boolean deleteNoToast(Context context, Uri proxUri) {

// This now performed automatically by TaskProvider when deleting proximity URI.
//		// ***************************************************
//		// Remove the proximity alert from LocationManager
//		// ***************************************************
//		Nearminder.removeProximityAlert(context, proxUri);
		
		// ***************************************************
		// Delete the attachment and proximity alert db records
		// ***************************************************
		if( 1 == context.getContentResolver().delete(proxUri, null, null) ){  
			return true;
		}else{
			Log.e(TAG, "ERR0003K Failed to delete nearminder record.");
			ErrorUtil.handle("ERR0003K", "Failed to delete nearminder record. uri="+proxUri, null);
			return false;
		}
		
		// ***************************************************
		// Attachment record is deleted by DB trigger.
		// ***************************************************
	}
	
	public static int update(Context context, Uri uri, ParcelableGeoPoint geoPoint, int radius, int zoomLevel, Uri selectedUri) {
		ContentValues cv = buildContentValues(geoPoint, radius, zoomLevel, selectedUri); 
		
		int updateCount = context.getContentResolver().update(uri, cv, null, null);

// This now performed automatically by TaskProvider when deleting proximity URI.
//		if( 1 == updateCount ){
//			Nearminder.addOrUpdateProximityAlert(context, geoPoint, radius, uri);
//		}
		
		return updateCount;
	}

	public static Uri insert(Context context, ParcelableGeoPoint geoPoint, int radius, int zoomLevel, Uri selectedUri) {
		ContentValues cv = buildContentValues(geoPoint, radius, zoomLevel, selectedUri); 
		
		Uri proximityAlertUri = context.getContentResolver().insert(Task.ProximityAlerts.CONTENT_URI, cv);
		
// This now performed automatically by TaskProvider when deleting proximity URI.
//		if( null != proximityAlertUri ){
//			Nearminder.addOrUpdateProximityAlert(context, geoPoint, radius, proximityAlertUri);
//		}
		
		return proximityAlertUri;
	}
	
	/**
	 * TODO: Expand the argument list to include all the parameters for the Insert call.
	 * @param geoPoint
	 * @param radius
	 * @return
	 */
	private static ContentValues buildContentValues(ParcelableGeoPoint geoPoint, int radius, int zoomLevel, Uri selectedUri) {
		ContentValues cv = new ContentValues();
		cv.put(Task.ProximityAlerts._IS_SATELLITE, 0); // TODO: Pull these from SelectAreaActivity
		cv.put(Task.ProximityAlerts._IS_TRAFFIC, 0);   // TODO: Pull these from SelectAreaActivity
		cv.put(Task.ProximityAlerts._ZOOM_LEVEL, zoomLevel);  // TODO: Pull these from SelectAreaActivity
		cv.put(Task.ProximityAlerts.RADIUS, radius);  
//		cv.put(Task.ProximityAlerts.RADIUS_UNIT, ); 
//		cv.put(Task.ProximityAlerts.TASK_ID, mTaskId); 
		cv.put(Task.ProximityAlerts._GEO_URI, Util.createGeoUri(geoPoint).toString());
		if( null != selectedUri ){
			cv.put(Task.ProximityAlerts._SELECTED_URI, selectedUri.toString());
		} 
		return cv;
	}	
	
//	public static void addOrUpdateProximityAlert(Context context, ParcelableGeoPoint geoPoint, int radius, Uri proximityAlertUri) {
	public static void addOrUpdateProximityAlert(Context context, GeoPoint geoPoint, int radius, Uri proximityAlertUri) {
		// ********************************************
		// Add/Update the actual proximity alert to Android.
		// ********************************************
		
		// Get the location manager.
		LocationManager locMan = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		assert null != locMan;
		
		float actualRadius = SelectAreaBorderPart.calculateDisplayRadius(radius);
		PendingIntent pendingIntent = Nearminder.createPendingIntent(context, proximityAlertUri, PendingIntent.FLAG_CANCEL_CURRENT);
		
		// Calculate the amount of time before the alert expires. 
		//           Milliseconds *  minute  *  Hour * Day * Month * Year
//		long expiration = 1000 	  *   60	 *	 60  *  24 *  31   *  365; // TODO: ! Revisit this time limit. Maybe allow the user to configure it either using a preference or when creating the nearminder. 
		locMan.addProximityAlert((double)(((double)geoPoint.getLatitudeE6())/(double)1E6), (double)(((double)geoPoint.getLongitudeE6())/(double)1E6), actualRadius, -1, pendingIntent); // TODO: !!! Google enhancement: Should allow to register without sending the Event,, because you will likely be within the proximity range when you add the alert.
	}	

	
	public static void removeProximityAlert(Context context, Uri proximityAlertUri) {
		assert proximityAlertUri.toString().startsWith(Task.ProximityAlerts.CONTENT_URI_STRING);

		NotificationUtil.removeNotification(context, proximityAlertUri);
		
		PendingIntent pendingIntent = Nearminder.createPendingIntent(context, proximityAlertUri, PendingIntent.FLAG_NO_CREATE);
		
		if( null != pendingIntent ){
			// Get the location manager.
			LocationManager locMan = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
			assert null != locMan;
			
			locMan.removeProximityAlert(pendingIntent);
			
			pendingIntent.cancel(); // TODO: !!! Check all the source code to make sure that we are canceling the pending intents when we are done with them.
		}else{
			// Not a problem.
		}
	}
	
	public static PendingIntent createPendingIntent(Context context, Uri proximityAlertUri, int flag) {
		Intent proximityAlertIntent = new Intent(Intent.ACTION_RUN, proximityAlertUri);	
		// Create a pending intent.
//		PendingIntent pendingIntent = PendingIntent.getActivity(NearminderActivity.this, -1, new Intent(Intent.ACTION_RUN, proximityAlertUri), PendingIntent.FLAG_CANCEL_CURRENT );
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, -1, proximityAlertIntent, flag );
		return pendingIntent;
	}

public static void addNotification(Context context, Uri proxAlertUri) {
		assert null != proxAlertUri;
		assert proxAlertUri.toString().startsWith(Task.ProximityAlerts.CONTENT_URI_STRING);
		
		/**
		 * Standard projection for the interesting columns of a proximity alert.
		 */
		final String[] PROXIMITY_PROJECTION = new String[] {
				TaskProvider.PROXIMITY_ALERT_TABLE_NAME+"."+Task.ProximityAlerts._ID,        // 0
				Task.Tasks.TASK_TITLE,        // 1
				Task.TaskAttachments.NAME,        // 2
		};	
		final int ID_INDEX 						= 0;
		final int TASK_TITLE_INDEX 			    = 1;
		final int ATTACHMENT_NAME_INDEX 		= 2;		
		
		
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
//        try {
//			db = context.openDatabase(TaskProvider.DATABASE_NAME, null);
//		} catch (FileNotFoundException e) {
//			Log.e(TAG, "Failed to open database " + TaskProvider.DATABASE_NAME, e);
//			nm.notifyWithText(-1, "Failed to open database " + TaskProvider.DATABASE_NAME, NotificationManager.LENGTH_SHORT, null);
//			// TODO: Record and retrieve.
//			return;
//		} 

	    // TODO: !!! I want to add "helper" methods to the provider, but how do I acquire a reference to the provider if this method is removed from 1.0 ?
	    //       --> One way would be to encapsulate the logic in a static method within the provider. 
	    //       --> Or use: context.getPackageManager().resolveContentProvider(name, flags)
	    //       --> Or use: context.getPackageManager().queryContentProviders(processName, uid, flags);
//		IContentProvider taskProvider = context.getContentResolver().acquireProvider(uri);
////		TaskProvider taskProvider = (TaskProvider)provider;
		
		Uri proximityTaskAttachUri = Uri.withAppendedPath(proxAlertUri, "tasks/attachments");

		Cursor mProximityCursor;
	    
//		try {
//			mProximityCursor = taskProvider.query(uri, PROXIMITY_PROJECTION, null, null, null);
//			mProximityCursor = context.getContentResolver().query(uri, PROXIMITY_PROJECTION, null, null, null);
			mProximityCursor = context.getContentResolver().query(proximityTaskAttachUri, PROXIMITY_PROJECTION, null, null, null);
//		} catch (DeadObjectException e) {
//			Log.e(TAG, "Failed to query content provider for uri " + uri, e);
//			Toast toast = Toast.makeText(context, R.string.error_failed_to_open_database + uri.toString(), Toast.LENGTH_SHORT); <string name="error_failed_to_open_database">Failed to open database </string>
//			toast.show();			
//			return;
//		} catch (RemoteException e) {
//			Log.e(TAG, "Failed to query content provider for uri " + uri, e);
//			Toast toast = Toast.makeText(context, R.string.error_failed_to_open_database + uri.toString(), Toast.LENGTH_SHORT);
//			toast.show();			
//			return;
//		}
		
		if( !mProximityCursor.moveToFirst()){
			// Normally this shouldn't occure, but when deploying development builds the database is wiped but the notifications are not so this error may occur.
			Log.e(TAG, "ERR0003L Proximity alert fired for non-existent Nearminder. Will remove proximity alert.");
			ErrorUtil.handle("ERR0003L", "Failed to delete nearminder record. " + proximityTaskAttachUri, null);
			Nearminder.deleteNoToast(context, proxAlertUri);
			return;
		}
		
	    Intent viewAlertIntent = new Intent();
//	    viewAlertIntent.setAction(Intent.ACTION_VIEW);
	    viewAlertIntent.setAction(NearminderViewer.ACTION_PROXIMITY_ALERT_NOTIFY);
	    viewAlertIntent.setData(proxAlertUri);
	    viewAlertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Needed because activity will be launched from a non-Activity context.
//	    viewAlertIntent.putExtra(ProximityAlertViewer.EXTRA_TASK_ID, mProximityCursor.getString(TASK_ID_INDEX)); // TODO: Remove, no longer used.
//	    viewAlertIntent.putExtra(ProximityAlertViewer.EXTRA_ATTACHEMENT_ID, mProximityCursor.getString(TASK_ATTACHMENT_ID_INDEX)); // TODO: Remove, no longer used.
//	    PendingIntent pIntent = PendingIntent.getActivity(context, -1, viewAlertIntent, PendingIntent.FLAG_NO_CREATE); 
	    PendingIntent pIntent = PendingIntent.getActivity(context, -1, viewAlertIntent, PendingIntent.FLAG_CANCEL_CURRENT); 

//	    Notification n = new Notification(R.drawable.bluesmallproimity, mProximityCursor.getString(TASK_TITLE_INDEX), System.currentTimeMillis());
	    Notification n = new Notification(R.drawable.blackproimity, mProximityCursor.getString(TASK_TITLE_INDEX), System.currentTimeMillis());

//	    n.setLatestEventInfo(context, mProximityCursor.getString(TASK_TITLE_INDEX), mProximityCursor.getString(ATTACHMENT_NAME_INDEX), pIntent) ;
	    n.setLatestEventInfo(context, mProximityCursor.getString(ATTACHMENT_NAME_INDEX), mProximityCursor.getString(TASK_TITLE_INDEX), pIntent) ;
	                         	    
        // after a 100ms delay, vibrate for 250ms, pause for 100 ms and
        // then vibrate for 500ms.
	    // TODO: Add some sort of configuration for this, or tap into the some common preferences system. 
//        n.vibrate = new long[] { 100, 250, 100, 500};
//        n.defaults = Notification.DEFAULT_ALL;
//	    nm.notify(mProximityCursor.getInt(ID_INDEX), n);

    	SharedPreferences preferences = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
    	if( preferences.getBoolean(ApplicationPreference.NEARMINDER_VIBRATE, ApplicationPreference.NEARMINDER_VIBRATE_DEFAULT)){
    		n.defaults |= Notification.DEFAULT_VIBRATE;
    	}
    	if( preferences.getBoolean(ApplicationPreference.NEARMINDER_FLASH, ApplicationPreference.NEARMINDER_FLASH_DEFAULT)){
    		n.defaults |= Notification.DEFAULT_LIGHTS;
//            n.ledARGB = Color.MAGENTA;
//            n.ledOnMS = 300;
//            n.ledOffMS = 1000;
//            n.flags |= Notification.FLAG_SHOW_LIGHTS; 	    		
    	}
    	
    	n.sound = Uri.parse(  preferences.getString(ApplicationPreference.NEARMINDER_RINGTONE, ApplicationPreference.NEARMINDER_RINGTONE_DEFAULT.toString()) );
	    
	    
        Uri notifUri = NotificationUtil.createNotification(context, proxAlertUri.toString());
        assert null != notifUri;
        
        // NOTE: There should be no harm in re-sending the notification since it will simply replace an existing notificaiton instance if one is present.
	    nm.notify(Integer.parseInt(notifUri.getLastPathSegment()), n);
	                                                     
	    mProximityCursor.close();
	}
	public static void launchGetDirections(Activity activity, Uri proximityAlertUri) {
		// If the proximity alert has an attached contact method URI, then use that URI to launch the map with an info bubble. 

		Cursor proximityAlertCursor = activity.getContentResolver().query(proximityAlertUri, new String[]{Task.ProximityAlerts._GEO_URI, Task.ProximityAlerts._SELECTED_URI}, null, null, null);
		assert null != proximityAlertCursor;
		if( !proximityAlertCursor.moveToFirst() ){
			Log.e(TAG, "ERR0004I Empty proximity cursor. " + proximityAlertUri);
			ErrorUtil.handleExceptionNotifyUser("ERR0004I", (Exception)(new Exception( proximityAlertUri.toString() )).fillInStackTrace(), activity);
			return;
		}
		Intent mapIntent = null;
		
		if( proximityAlertCursor.isNull(1) || // Is there a _SELECTED_URI ? No.
				(activity.getContentResolver().query(Uri.parse(proximityAlertCursor.getString(1)), null, null, null, null)).getCount() > 0 ){ // _SELECTED_URI no longer points to a valid record.
			
			LocationManager lm = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
			if( null != lm){
				String locProviderName = lm.getBestProvider(new Criteria(), true);
				if( null != locProviderName){
					Location loc = lm.getLastKnownLocation(locProviderName);
					if( null != loc ){
						
						// ****************************************************
						// * http://mapki.com/wiki/Google_Map_Parameters 
						// ****************************************************
						
						// Uri.parse(proximityAlertCursor.getString(0))
						GeoPoint geoPoint = Util.createPoint(proximityAlertCursor.getString(0));
					    //                                                    http://maps.google.com/maps?f=d&saddr=$lat,$lng  &daddr=$iPhoneEndAddress"
						mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?f=d&saddr="+loc.getLatitude() +","+ loc.getLongitude()+"&daddr="+(((float)geoPoint.getLatitudeE6())/1E6)+","+(((float)geoPoint.getLongitudeE6())/1E6)) );// &hl=en

						// ******************************************************
						// Select Google Maps if it is installed.
						// ******************************************************
						List<ResolveInfo>  resolveInfoList = activity.getPackageManager().queryIntentActivities(
								mapIntent, 
								PackageManager.MATCH_DEFAULT_ONLY);
						if( resolveInfoList.size() > 1 ){
							for(ResolveInfo resolveInto: resolveInfoList ){
								//Log.v(TAG, "resolveInto.activityInfo.packageName == " + resolveInto.activityInfo.packageName );
								if("com.google.android.apps.maps".equals(resolveInto.activityInfo.packageName) ){
									mapIntent.setComponent(new ComponentName(resolveInto.activityInfo.packageName, resolveInto.activityInfo.name));
									break;
								}
							}
						}
						
					}else{
						Toast.makeText(activity, R.string.toast_unableToDetermineYourLocation, Toast.LENGTH_LONG).show(); 
						proximityAlertCursor.close();
						return;
					}
				}else{
					Toast.makeText(activity, R.string.toast_unableToDetermineYourLocation, Toast.LENGTH_LONG).show(); 
					proximityAlertCursor.close();
					return;
				}
			}else{
				Toast.makeText(activity, R.string.toast_unableToDetermineYourLocation, Toast.LENGTH_LONG).show(); 
				proximityAlertCursor.close();
				return;
			}											
			
//											mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(proximityAlertCursor.getString(0)));
////											Geocoder geocoder = new Geocoder(mActivity); // TODO: Add locale info.
////											GeoPoint geopoint = Util.createPoint(proximityAlertCursor.getString(0)); 
////											List<Address> addresses = geocoder.getFromLocation((double)(((double)geopoint.getLatitudeE6())/1E6), (double)(((double)geopoint.getLongitudeE6())/1E6), 1);
////											if( null != addresses && addresses.size() > 0 ){
////												// TODO: Insert this new address into the Address book so that the contact method URI can be used below.											
////												mapIntent.setData(addresses.get(0).);
////											}
			mapIntent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

		}else{ // Is there a _SELECTED_URI? Yes. 
			// Intent { action=android.intent.action.VIEW data=content://contacts/people/2/contact_methods/1 comp={com.google.android.apps.maps/com.google.android.maps.MapsActivity} }								
			mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(proximityAlertCursor.getString(1)));
//											mapIntent.setComponent(new ComponentName("com.google.android.apps.maps","com.google.android.maps.MapsActivity")); // Not necessary.
			mapIntent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
		}
		activity.startActivity(mapIntent);
		activity.finish();
		proximityAlertCursor.close();
	}
	
	
}
