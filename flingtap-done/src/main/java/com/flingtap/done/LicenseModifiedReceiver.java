// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;
import com.flingtap.done.util.AddonUtil;
import com.flingtap.done.util.LicenseUtil;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 *  Handles Re-install event.
 * 
 */
public class LicenseModifiedReceiver extends BroadcastReceiver {  
	private static final String TAG = "LicenseModifiedReceiver";

    public static final String ACTION_VIEW_TUTORIAL = "com.flingtap.done.intent.action.VIEW_TUTORIAL";	
	
	private static final String PACKGE_PREFIX = "package:";
	private static final String MINDERS_PACKAGE = PACKGE_PREFIX + SharedConstant.ADDON_MINDERS;
	private static final String ORGANIZERS_PACKAGE = PACKGE_PREFIX + SharedConstant.ADDON_ORGANIZERS;
	private static final String BACKUP_PACKAGE = PACKGE_PREFIX + SharedConstant.ADDON_BACKUP;

	private boolean isLicenseNew(int currentLicense, int newlicense){
		return (currentLicense & newlicense) != newlicense;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		try{
			//Log.v(TAG, "onReceive(..) called.");
			String data = intent.getDataString();
//			Log.v(TAG, "data == " + data + ", " + "action == " + intent.getAction());
//			Log.v(TAG, "context.getPackageName() == " + context.getPackageName());

			String thePackage = null;
			if( MINDERS_PACKAGE.equals(data)) {
				thePackage = SharedConstant.ADDON_MINDERS;
			}else if( ORGANIZERS_PACKAGE.equals(data)) {
				thePackage = SharedConstant.ADDON_ORGANIZERS;
			}else if( BACKUP_PACKAGE.equals(data)) {
				thePackage = SharedConstant.ADDON_BACKUP;
			} 
			
			if( null != thePackage ){
				boolean isMindersTutorialNotificationNeeded = false;
				boolean isLabelsTutorialNotificationNeeded = false;
				boolean isArchiveTutorialNotificationNeeded = false;
				
				SharedPreferences settings = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE); 

				// A property that stores the current feature license bits for comparing it to the new state. Only then can you auto-enable a feature.
				int currentLicense = settings.getInt(ApplicationPreference.CURRENT_FEATURE_LICENSES, ApplicationPreference.CURRENT_FEATURE_LICENSES_DEFAULT);
				
				if(Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction()) ){
					Intent enableIntent = null;
					
					// *******************
					// Nearminder
					// *******************
					
					// If we have license for Nearminder ...
					if( LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_NEARMINDER) ){
						// If it is a new feature.
						if( isLicenseNew(currentLicense, LicenseUtil.FEATURE_NEARMINDER) ){
							isMindersTutorialNotificationNeeded = true;
						}
					}
					
					// *******************
					// Callminder
					// *******************

					// If Callminders currently not enabled ....
					if(!settings.getBoolean(ApplicationPreference.CALLMINDER_ENABLED, ApplicationPreference.CALLMINDER_ENABLED_DEFAULT)){
						// If we have license for Callminders ...
						if( LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_CALLMINDER) ){
							// If it is a new feature.
							if( isLicenseNew(currentLicense, LicenseUtil.FEATURE_CALLMINDER) ){
								// Enable Callminders.
								Editor editor = settings.edit();
								editor.putBoolean(ApplicationPreference.CALLMINDER_ENABLED, true);
								editor.commit();								
								MonitorPhoneStateService.sendMessage(context, true);
								
								isMindersTutorialNotificationNeeded = true;
							}
						}
					}
					
					// *******************
					// Labels
					// *******************
					if(!settings.getBoolean(ApplicationPreference.ENABLE_LABELS, ApplicationPreference.ENABLE_LABELS_DEFAULT)){
						// If does _not_ have license for Labels ...
						if( LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_LABELS) ){
                            // Don't automatically enable feature because they may not want the feature enabled.
							// If it is a new feature.
							if( isLicenseNew(currentLicense, LicenseUtil.FEATURE_LABELS) ){
								// Enable labels.
								enableIntent = ApplicationPreferenceActivity.createToggleLabelsIntent(true);
								enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								context.startActivity(enableIntent);
								
								isLabelsTutorialNotificationNeeded = true;
							}
						}
					}
					
					
					// *******************
					// Archiving			
					// *******************
					
					if(!settings.getBoolean(ApplicationPreference.ENABLE_ARCHIVE, ApplicationPreference.ENABLE_ARCHIVE_DEFAULT)){
                        // Don't automatically enable feature because they may not want the feature enabled.
						// If does _not_ have license for Archive ...
						if( LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_ARCHIVING) ){
							// If it is a new feature.
							if( isLicenseNew(currentLicense, LicenseUtil.FEATURE_ARCHIVING) ){
								// Enable archiving.
								enableIntent = ApplicationPreferenceActivity.createToggleArchiveIntent(true);
								enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								context.startActivity(enableIntent);
								
								isArchiveTutorialNotificationNeeded = true;
							}
						}
					}

					// *******************
					// Backup (Not a licensed product, but still requires installation procedure)
					// *******************
					if( SharedConstant.ADDON_BACKUP == thePackage ){ // Is this the package being installed?
						if(!settings.getBoolean(ApplicationPreference.BACKUP_ENABLED, ApplicationPreference.BACKUP_ENABLED_DEFAULT)){	
							if( AddonUtil.doesPackageHaveSameSignature(context, SharedConstant.ADDON_BACKUP) ){
								// Enable archiving.
								enableIntent = ApplicationPreferenceActivity.createToggleBackupIntent(true);
								enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								context.startActivity(enableIntent);								
							}
						}
					}
					
				}else if(Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction()) ){
					Intent enableDisableIntent = null;
					
					// *******************
					// Nearminder
					// *******************
					// If we have license for Nearminder ...
					if( LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_NEARMINDER) ){
						// If it is a new feature.
						if( isLicenseNew(currentLicense, LicenseUtil.FEATURE_NEARMINDER) ){
							isMindersTutorialNotificationNeeded = true;
						}
					}
					
					// *******************
					// Callminder
					// *******************
					
					// If Callminders currently not enabled ....
					if(!settings.getBoolean(ApplicationPreference.CALLMINDER_ENABLED, ApplicationPreference.CALLMINDER_ENABLED_DEFAULT)){
						// If we have license for Callminders ...
						
						if( LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_CALLMINDER) ){
                            // Don't automatically enable feature because they may not want the feature enabled.
							// If it is a new feature.
							if( isLicenseNew(currentLicense, LicenseUtil.FEATURE_CALLMINDER) ){
								// Enable Callminders.
								MonitorPhoneStateService.sendMessage(context, true);
								Editor editor = settings.edit();
								editor.putBoolean(ApplicationPreference.CALLMINDER_ENABLED, true);
								editor.commit();
								
								isMindersTutorialNotificationNeeded = true;
								
							}else{
								// Do nothing
							}
						}
					}else{
						if( !LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_CALLMINDER) ){
							
							// Disable Callminders.
							MonitorPhoneStateService.sendMessage(context, false);
							Editor editor = settings.edit();
							editor.putBoolean(ApplicationPreference.CALLMINDER_ENABLED, false);
							editor.commit();
						}else{
							// Do nothing
						}
					}				
					
					// *******************
					// Labels
					// *******************
					if(!settings.getBoolean(ApplicationPreference.ENABLE_LABELS, ApplicationPreference.ENABLE_LABELS_DEFAULT)){
                        // Don't automatically enable feature because they may not want the feature enabled.
						// If has license for Labels ...
						if( LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_LABELS) ){
							// If it is a new feature.
							if( isLicenseNew(currentLicense, LicenseUtil.FEATURE_LABELS) ){
								// Enable labels.
								enableDisableIntent = ApplicationPreferenceActivity.createToggleLabelsIntent(true);
								enableDisableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								context.startActivity(enableDisableIntent);
								
								isLabelsTutorialNotificationNeeded = true;
							}
						}else{
							// Do nothing
						}
					}else{ // Is enabled.
						// If does _not_ have license for Labels ...
						if( !LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_LABELS) ){
							// Enable labels.
							enableDisableIntent = ApplicationPreferenceActivity.createToggleLabelsIntent(false);
							enableDisableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(enableDisableIntent);
						}else{
							// Do nothing
						}
					}
					
					
					// *******************
					// Archiving			
					// *******************
					
					if(!settings.getBoolean(ApplicationPreference.ENABLE_ARCHIVE, ApplicationPreference.ENABLE_ARCHIVE_DEFAULT)){
                        // Don't automatically enable feature because they may not want the feature enabled.
						// If has license for Archive ...
						if( LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_ARCHIVING) ){
							// If it is a new feature.
							if( isLicenseNew(currentLicense, LicenseUtil.FEATURE_ARCHIVING) ){
								// Enable archiving.
								enableDisableIntent = ApplicationPreferenceActivity.createToggleArchiveIntent(true);
								enableDisableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								context.startActivity(enableDisableIntent);
								
								isArchiveTutorialNotificationNeeded = true;
							}
						}else{
							// Do nothing
						}
					}else{ // Is enabled ..
						// If does _not_ have license for Archive ...
						if( !LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_ARCHIVING) ){ 
							// Disable archiving.
							enableDisableIntent = ApplicationPreferenceActivity.createToggleArchiveIntent(false);
							enableDisableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(enableDisableIntent);
						}else{
							// Do nothing
						}
					}
					
				}else if(Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction()) ){
					Intent disableIntent = null;
					
					// TODO: !!!! I don't think that LicenseUtil.hasLicense actually works here because the package has already been removed. 
					
					// *******************
					// Nearminder
					// *******************
					
					// Nothing needed.
					
					// *******************
					// Callminder
					// *******************
					
					// If Callminders currently enabled ....
					if(settings.getBoolean(ApplicationPreference.CALLMINDER_ENABLED, ApplicationPreference.CALLMINDER_ENABLED_DEFAULT)){
						// If does _not_ have license for Callminders ...
						if( !LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_CALLMINDER) ){
							// Disable Callminders.
							MonitorPhoneStateService.sendMessage(context, false);
							Editor editor = settings.edit();
							editor.putBoolean(ApplicationPreference.CALLMINDER_ENABLED, false);
							editor.commit();
						}
					}
					
					// *******************
					// Labels
					// *******************
					if(settings.getBoolean(ApplicationPreference.ENABLE_LABELS, ApplicationPreference.ENABLE_LABELS_DEFAULT)){
						// If does _not_ have license for Labels ...
						if( !LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_LABELS) ){
							disableIntent = ApplicationPreferenceActivity.createToggleLabelsIntent(false);
							disableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(disableIntent);
						}
					}
					
					
					// *******************
					// Archiving			
					// *******************
					
					if(settings.getBoolean(ApplicationPreference.ENABLE_ARCHIVE, ApplicationPreference.ENABLE_ARCHIVE_DEFAULT)){
						// If does _not_ have license for Archive ...
						if( !LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_ARCHIVING) ){
							disableIntent = ApplicationPreferenceActivity.createToggleArchiveIntent(false);
							disableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(disableIntent);
						}
					}
					
					// *******************
					// Backup (Not a licensed product, but still requires un-installation procedure)
					// *******************

					if( SharedConstant.ADDON_BACKUP == thePackage ){ // Is this the package being un-installed?
						if(settings.getBoolean(ApplicationPreference.BACKUP_ENABLED, ApplicationPreference.BACKUP_ENABLED_DEFAULT)){
							// If does Backup is enabled  ...

							disableIntent = ApplicationPreferenceActivity.createToggleBackupIntent(false);
							disableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(disableIntent);
						}
					}					
				} 
				
				LicenseUtil.updateCurrentFeatureLicenseSetting(context);
				
				if( isMindersTutorialNotificationNeeded ){
					// Notify user of video tutorials
					notifyOfTutorial(context, "http://bit.ly/10q6WU", R.string.notify_watchMindersTutorial, R.string.notify_flingTapDoneMenuTutorials);
				}
				if( isLabelsTutorialNotificationNeeded ){
					// Notify user of video tutorials
					notifyOfTutorial(context, "http://bit.ly/4ffJZ7", R.string.notify_watchLabelsTutorial, R.string.notify_flingTapDoneMenuTutorials);
				}
				if( isArchiveTutorialNotificationNeeded ){
					// Notify user of video tutorials
					notifyOfTutorial(context, "http://bit.ly/1GM1D4", R.string.notify_watchArchiveTutorial, R.string.notify_flingTapDoneMenuTutorials);
				}

			}else if(ACTION_VIEW_TUTORIAL.equals(intent.getAction()) ){
				
				NotificationUtil.removeNotification(context, intent.getData());
				
			    Intent viewIntent = new Intent();
			    viewIntent.setAction(Intent.ACTION_VIEW);
			    viewIntent.setData(intent.getData());
			    viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Needed because activity will be launched from a non-Activity context.
			    context.startActivity(viewIntent);
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000IY", exp);
			ErrorUtil.handleException("ERR000IY", exp, context);
		}
	}
	
	private void notifyOfTutorial(Context context, String url, int titleResId, int descriptionResId){
		assert null != url;
		assert url.toString().startsWith("http");
		
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		
	    Intent viewTutorialIntent = new Intent();
	    viewTutorialIntent.setAction(ACTION_VIEW_TUTORIAL);
	    viewTutorialIntent.setData(Uri.parse(url));
	    viewTutorialIntent.setComponent(new ComponentName(context, LicenseModifiedReceiver.class));
	    PendingIntent pIntent = PendingIntent.getBroadcast(context, -1, viewTutorialIntent, PendingIntent.FLAG_ONE_SHOT); 

	    Notification n = new Notification(R.drawable.tutorial_notif, context.getText(titleResId), System.currentTimeMillis());
	    n.setLatestEventInfo(context, context.getText(titleResId), context.getText(descriptionResId), pIntent) ;
	    n.defaults = Notification.DEFAULT_ALL;	
	    
        Uri notifUri = NotificationUtil.createNotification(context, url);
        assert null != notifUri;
        
        // NOTE: There should be no harm in re-sending the notification since it will simply replace an existing notificaiton instance if one is present.
	    nm.notify(Integer.parseInt(notifUri.getLastPathSegment()), n);
	                                                     
	}
	
}
