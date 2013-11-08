// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.ApplicationContextBase;
import com.flingtap.common.HandledException;
import com.flurry.android.FlurryAgent;
import com.tomgibara.android.veecheck.Veecheck;
import com.tomgibara.android.veecheck.util.PrefSettings;

import com.flingtap.done.base.R;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.DebugUtils;
import android.util.Log;
import android.widget.Toast;

/**
 * @author spencer
 */
public class ApplicationContext extends ApplicationContextBase {
	private final static String TAG = "ApplicationContext";
	
	public ApplicationContext() {
		super();
	}

	@Override
	protected void onFirstRun(){
		// These alarms/services are normally started from on-startup and on-reinstall broadcasts.
		
		// *******************************************************
		// Delayed alarms and services.
		// *******************************************************			
		SetupBroadcastReceiver.setupDelayedAlarms(ApplicationContext.this, null);
	}
	
	@Override
	protected boolean onNewVersion(int actualVersion, int recordedVersion){
		SharedPreferences settings = getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE); 
		assert null != settings;		
		
		// Log.i(TAG, "(actualVersion > recordedVersion) is " + "("+actualVersion+" > "+recordedVersion+")");
		
		// *******************************************************
		// Update version in preferences.
		// *******************************************************					
		if(0 == recordedVersion || StaticConfig.HAVE_NEW_EULA){
			
			SharedPreferences.Editor ed = settings.edit();
			assert null != ed;
			
			// Log.i(TAG, "(0 == recordedVersion || StaticConfig.HAVE_NEW_EULA) is " + "(0 == "+recordedVersion+" || "+StaticConfig.HAVE_NEW_EULA+")");
			ed.putBoolean(ApplicationPreference.EULA_ACCEPTANCE_REQUIRED, true);

//			boolean commit = ed.commit();
            // TODO: ! Bug in current Android code (1.1r1) always returns false.
            //         See issue: http://code.google.com/p/android/issues/detail?id=940
//					 if( !commit ){
//					 	Log.e(TAG, "Failed to upgrade shared preferences.");
//					  throw new RuntimeException("Failed to upgrade shared preferences.");
//					 }
			boolean commit2 = ed.commit();
		}
        
		return true;
	}

	
	@Override
	protected boolean killOnDowngrade() {
		return true;
	}
	
	@Override
	protected void onCreation() {
//		Log.v(TAG, "onCreation() called.");
        ErrorUtil.NOTIFY_USER_TOAST_MESSAGE_STRING_ID = R.string.toast_sorryAnErrorOccurred;
        SessionUtil.initErrorHandling(this);
	}

	@Override
	protected void onDowngrade(int actualVersion, int recordedVersion) {
		Log.w(TAG, "Attempt to downgrade application from version " + recordedVersion + " to version " + actualVersion + " is not allowed.");
		SessionUtil.onSessionStart(this); // TabActivity is an ActivityGroup and each of the contained Activities will start/stop the session on their own.
		ErrorUtil.handle("ERR00009", "Attempt to downgrade application from version " + recordedVersion + " to version " + actualVersion + " is not allowed.",this);
		SessionUtil.onSessionStop(this); 
	}

	@Override
	protected void onError(Error err) {
		SessionUtil.onSessionStart(this); 
		ErrorUtil.handleError("ERR000IB", err, this);
		SessionUtil.onSessionStop(this); 
	}

	@Override
	protected void onException(Exception exp) {
		Log.e(TAG, "ERR000IC", exp);
		SessionUtil.onSessionStart(this);
		ErrorUtil.handleException("ERR000IC", exp, this);
		SessionUtil.onSessionStop(this); 
	}

}
