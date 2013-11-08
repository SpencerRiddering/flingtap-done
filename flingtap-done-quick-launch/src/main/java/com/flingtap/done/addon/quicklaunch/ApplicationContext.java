// Licensed under the Apache License, Version 2.0

package com.flingtap.done.addon.quicklaunch;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.flingtap.common.ApplicationContextBase;

public class ApplicationContext extends ApplicationContextBase {
	private final static String TAG = "ApplicationContext";

	@Override
	protected void onFirstRun(){
	}
	
	@Override
	protected boolean onNewVersion(int actualVersion, int recordedVersion){
		SharedPreferences settings = getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE); 
		assert null != settings;		
		
		// *******************************************************
		// Update version in preferences.
		// *******************************************************					

		if(0 == recordedVersion || StaticConfig.HAVE_NEW_EULA){
			
			SharedPreferences.Editor ed = settings.edit();
			assert null != ed;
			
			// Log.i(TAG, "(0 == recordedVersion || StaticConfig.HAVE_NEW_EULA) is " + "(0 == "+recordedVersion+" || "+StaticConfig.HAVE_NEW_EULA+")");
			ed.putBoolean(ApplicationPreference.EULA_ACCEPTANCE_REQUIRED, true);
			ed.commit();
		}

		return true;
	}

	@Override
	protected boolean killOnDowngrade() {
		return true;
	}
	
	@Override
	protected void onCreation() {
		ErrorUtil.NOTIFY_USER_TOAST_MESSAGE_STRING_ID = R.string.error_util_notify_user_toast_message;
		SessionUtil.initErrorHandling(this);
	}

	@Override
	protected void onDowngrade(int actualVersion, int recordedVersion) {
		Log.e(TAG, "Attempt to downgrade application from version " + recordedVersion + " to version " + actualVersion + " is not allowed.");
		SessionUtil.onSessionStart(this); // TabActivity is an ActivityGroup and each of the contained Activities will start/stop the session on their own.
		ErrorUtil.handle("ERR000I5", "Attempt to downgrade application from version " + recordedVersion + " to version " + actualVersion + " is not allowed.",this);
		SessionUtil.onSessionStop(this); 
	}

	@Override
	protected void onError(Error err) {
		SessionUtil.onSessionStart(this);
		ErrorUtil.handleError("ERR000I8", err, this);
		SessionUtil.onSessionStop(this); 
	}

	@Override
	protected void onException(Exception exp) {
		Log.e(TAG, "ERR000I7", exp);
		SessionUtil.onSessionStart(this);
		ErrorUtil.handleException("ERR000I7", exp, this);
		SessionUtil.onSessionStop(this); 
	}
}
