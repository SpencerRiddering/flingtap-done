// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.flingtap.common.HandledException;

/**
 */
public class StartupBroadcastReceiver extends BroadcastReceiver { 
	public static final String TAG = "StartupBroadcastReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		try{
			//Log.v(TAG, "onReceive(..) called.");
	        
			SetupBroadcastReceiver.setupNotificationsAlarmsAndProximityAlerts(context, true); 		
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00069", exp);
			ErrorUtil.handleException("ERR00069", exp, context);
		}
		
		try{
			FilterBitsRefreshReceiver.setupNotifications(context);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000L0", exp);
			ErrorUtil.handleException("ERR000L0", exp, context);
		}

	}


}
