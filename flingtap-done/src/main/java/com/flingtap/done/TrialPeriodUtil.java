// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.HashMap;
import java.util.Map;

import com.flingtap.common.HandledException;
import com.flingtap.done.util.Constants;
import com.tomgibara.android.veecheck.Veecheck;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.util.Log;

import com.flingtap.done.base.R;

/**
 * 
 * 
 * @author spencer
 *
 */
public abstract class TrialPeriodUtil {
	private static final String TAG = "TrialPeriodUtil";
	
	public static void checkAndNotify(Activity activity){
		SharedPreferences settings = activity.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE); 
		long deadline = settings.getLong(ApplicationPreference.UPDATE_REQUIRED_DEADLINE, Long.MAX_VALUE);
		if( System.currentTimeMillis() > deadline && 
				settings.contains(ApplicationPreference.UPDATE_REQUIRED_URI)){
			
			// Double check the deadline. (maybe I updated it).
			// Note: A change noticed here will not prevent the user from being prompted to update.
			Intent intent = new Intent(Veecheck.getCheckAction(activity), Uri.parse(StaticConfig.VEECHECK_CHECK_URI));
			activity.startService(intent);

			// Prompt user to update.
			ButtonOnClickListener buttonOnClickListener = new ButtonOnClickListener(activity, settings);
			
			new AlertDialog.Builder(activity)
					.setIcon(android.R.drawable.ic_dialog_alert)				
					.setTitle(R.string.dialog_pleaseUpdate)				
			        .setMessage(R.string.dialog_yourTrialVersionHasExpired_) // TODO: !!!! Not all required updates will are trial versions,, right? 
			        .setPositiveButton(R.string.button_updateNow, buttonOnClickListener)
			        .setNegativeButton(R.string.button_noThanks, buttonOnClickListener)
			        .setCancelable(false)
			        .create()
					.show(); // It is ok that this dialog is not managed. 
		}
	}
	
	private static final class ButtonOnClickListener implements DialogInterface.OnClickListener {
		private Activity mActivity = null;
		private SharedPreferences mSettings = null;
		
		public ButtonOnClickListener(Activity activity, SharedPreferences settings){
			mActivity = activity;
			assert null != mActivity;
			
			mSettings = settings;
			assert null != mSettings;
			
		}
		
		public void onClick(DialogInterface dialog, int which) {
        	try{
        		switch(which){
        			case DialogInterface.BUTTON_POSITIVE:
        				handlePositiveButton();
        				break;
        			case DialogInterface.BUTTON_NEGATIVE:
                		handleNegativeButton();
                		break;
        		}
        	}catch(HandledException h){ // Ignore.
        	}catch(Exception exp){
        		mActivity.setResult(Activity.RESULT_CANCELED);
        		mActivity.finish();
        		
        		Log.e(TAG, "ERR000D7", exp);
        		ErrorUtil.handleExceptionNotifyUser("ERR000D7", exp, mActivity);
        	}

		}

		private void handleNegativeButton() {
			mActivity.setResult(Activity.RESULT_CANCELED);
			mActivity.finish();
		}

		private void handlePositiveButton() {
			try{
				String uriString = mSettings.getString(ApplicationPreference.UPDATE_REQUIRED_URI, null);
				assert null != uriString;
				try{
					Intent intent = Intent.getIntent(uriString);
					mActivity.startActivity(intent);
				}catch(HandledException h){ // Ignore.
				}catch(Exception exp){
					Log.e(TAG, "ERR000D9", exp);
					ErrorUtil.handleExceptionNotifyUser("ERR000D9", exp, mActivity);
				}
				
			}finally{
	    		mActivity.setResult(Activity.RESULT_CANCELED);
	    		mActivity.finish();
			}
		}
	};
}
