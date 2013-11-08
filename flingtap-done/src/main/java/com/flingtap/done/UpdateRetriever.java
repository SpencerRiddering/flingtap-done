// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.tomgibara.android.veecheck.VeecheckReceiver;
import com.tomgibara.android.veecheck.VeecheckSettings;
import com.tomgibara.android.veecheck.VeecheckState;
import com.tomgibara.android.veecheck.util.PrefSettings;
import com.tomgibara.android.veecheck.util.PrefState;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * VeeCheck related.
 *
 * See: http://www.tomgibara.com/android/veecheck/
 */
public class UpdateRetriever extends VeecheckReceiver {
	private static final String TAG = "UpdateRetriever";

	@Override
	public void onReceive(Context context, Intent intent) {
		try{
			super.onReceive(context, intent);
			// Log.v(TAG, "onReceive(..) called.");
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0009R", exp);
			ErrorUtil.handleException("ERR0009R", exp, context);
		}
	}
	
	@Override
	protected VeecheckSettings createSettings(Context context) {
		try{
//			Log.v(TAG, "createSettings(..) called.");

			SharedPreferences prefs = PrefSettings.getSharedPrefs(context);
			//assign some default settings if necessary
            // Log.v(TAG, "Performing settings initialization.");
				
            Editor editor = prefs.edit();
            editor.putBoolean(PrefSettings.KEY_ENABLED, true);
            editor.putLong(PrefSettings.KEY_PERIOD, StaticConfig.VEECHECK_PERIOD);
            editor.putLong(PrefSettings.KEY_CHECK_INTERVAL, StaticConfig.VEECHECK_CHECK_INTERVAL);
            editor.putString(PrefSettings.KEY_CHECK_URI, StaticConfig.VEECHECK_CHECK_URI);
            editor.commit();

			return new PrefSettings(context);
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0009K", exp);
			ErrorUtil.handleException("ERR0009K", exp, context);
		}
		return null;
	}
	
	@Override
	protected VeecheckState createState(Context context) {
		try{
			// Log.v(TAG, "createState(..) called.");
			return new PrefState(context);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0009S", exp);
			ErrorUtil.handleException("ERR0009S", exp, context);
		}
		return null;
	}
	
}
