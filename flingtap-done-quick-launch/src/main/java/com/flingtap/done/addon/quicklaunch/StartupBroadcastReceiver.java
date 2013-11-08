// Licensed under the Apache License, Version 2.0

package com.flingtap.done.addon.quicklaunch;

import com.flingtap.common.HandledException;
import com.tomgibara.android.veecheck.Veecheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 */
public class StartupBroadcastReceiver extends BroadcastReceiver { 
	public static final String TAG = "StartupBroadcastReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		try{
			//Log.v(TAG, "onReceive(..) called.");
	        
			
			// *******************************************************
			// Start Veecheck Reschedule Alarm
			// *******************************************************
			// Log.v(TAG, "Sending Update Reschedule Broadcast.");
			Intent rescheduleUpdateIntent = new Intent(Veecheck.getRescheduleAction(context));
			context.sendBroadcast(rescheduleUpdateIntent); 			

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000IU", exp);
			ErrorUtil.handleException("ERR000IU", exp, context);
		}
	}
}
