// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.HashMap;
import java.util.Map;

import com.flingtap.common.HandledException;
import com.flingtap.done.util.LicenseUtil;
import com.flurry.android.FlurryAgent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * 
 * TODO: Notification should be automatically dismissed a fixed amount of time after the phone call end. 
 *
 * TODO: !!! Consider moving this code into the add-on package so that the permission and manifest registration doesn't exist in the core product.
 */
public class NewOutgoingCallBroadcastReceiver extends BroadcastReceiver {
	public static final String TAG = "NewOutgoingCallBroadcastReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		try{
			//Log.v(TAG, "onReceive(..) called.");			
			if( MonitorPhoneStateService.areCallmindersEnabled(context) ){
				
				if( LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_CALLMINDER) ){
					
					// Prepare event info.
					Event.onEvent(Event.OUTGOING_CALLMINDER, null); // Map<String,String> parameters = new HashMap<String,String>();
					
					String dialedNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
					//Log.v(TAG, "phoneNumber == " + dialedNumber);
					
					assert null != dialedNumber;
					
					MonitorPhoneStateService.setupNotifications(context, dialedNumber);

					// In case the phone state monitor is killed by user, start it up here.
					MonitorPhoneStateService.startService(context);
				}
				
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0004J", exp);
			ErrorUtil.handleException("ERR0004J", exp, context);
		}
	}
}
