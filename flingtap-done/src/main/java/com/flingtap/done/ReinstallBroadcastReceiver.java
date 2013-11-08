// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 *  Handles Re-install event.
 * 
 * 
 */
public class ReinstallBroadcastReceiver extends BroadcastReceiver {  
	private static final String TAG = "ReinstallBroadcastReceiver"; 
	
	@Override
	public void onReceive(Context context, Intent intent) {
		try{
			//Log.v(TAG, "onReceive(..) called.");
			String data = intent.getDataString();
//			Log.v(TAG, "data == " + data + ", " + "action == " + intent.getAction());
//			Log.v(TAG, "context.getPackageName() == " + context.getPackageName());
				        
			if(Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction()) && 
					("package:"+context.getPackageName()).equals(data)) { //data.equals("package:"+ReinstallBroadcastReceiver.class.getName()) "package:com.ericcrook.InstallReceiver"
				// Do re-initialization work here.
					
				// Can't setup application version here because the event doesn't strictly occur before the application is run and so the version may not be setup correctly when it is needed during application startup initialization.
				// SetupBroadcastReceiver.setupApplicationVersion(context); 		
				
				// Alarms and Proximity alerts are lost durring re-install, so they must be re-added here.
				SetupBroadcastReceiver.setupNotificationsAlarmsAndProximityAlerts(context, true); 		
			} 

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000DD", exp);
			ErrorUtil.handleException("ERR000DD", exp, context);
		}
	}
}
