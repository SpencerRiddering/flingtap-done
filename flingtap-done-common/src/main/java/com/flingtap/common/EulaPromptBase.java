// Licensed under the Apache License, Version 2.0

package com.flingtap.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.flurry.android.FlurryAgent;
import com.flingtap.common.HandledException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;


public abstract class EulaPromptBase {
	private final static String TAG = "EulaPromptPart";
	
	/**
	 * Checks to see if the user should be prompted to accept a EULA.
	 * 
	 * @param context
	 * @return
	 */
	public static final boolean eulaAcceptanceRequired(Context context){
		assert null != context;

        // Retrieve shared preferences object.
		SharedPreferences settings = context.getSharedPreferences(ApplicationPreferenceBase.NAME, Context.MODE_PRIVATE); 
		assert null != settings;
		
		boolean eulaAcceptanceRequired = settings.getBoolean(ApplicationPreferenceBase.EULA_ACCEPTANCE_REQUIRED, ApplicationPreferenceBase.EULA_ACCEPTANCE_REQUIRED_DEFAULT);
		return eulaAcceptanceRequired;
	}
	
	public abstract void promptWithEula(Activity activity);

	protected void onAccept(Activity activity){
	}
	
	public void promptWithEula(final Activity activity, 
			int rawEulaResId, 
			int stringEulaPromptTitle,
			int stringEulaPromptButtonAccept,
			int stringEulaPromptButtonReject
			){
		
		StringBuffer eulaText = new StringBuffer();
		
		Resources resources = activity.getResources();
		assert null != resources;
		InputStream inputStream = resources.openRawResource(rawEulaResId); 
		assert null != inputStream;
		try{
			InputStreamReader isr = null;
			try {
				isr = new InputStreamReader(inputStream, "UTF-8");
				assert null != isr;
				char[] charArray = new char[1024];
				int readCount = -1;
				try {
					while( -1 != (readCount = isr.read(charArray) ) ){
						eulaText.append(charArray, 0, readCount);
					}
				} catch (IOException e) {
					// Prevent user from continuing on at this point. Make application exit.
					activity.setResult(Activity.RESULT_CANCELED);
					activity.finish();

					Log.e(TAG, "ERR000IM Failed to read EULA text.", e);
					ErrorUtil.handleExceptionAndThrow("ERR000IM", e, activity);
				}
			} catch (UnsupportedEncodingException e) {
				// Prevent user from continuing on at this point. Make application exit.
				activity.setResult(Activity.RESULT_CANCELED);
				activity.finish();
				
				Log.e(TAG, "ERR000IN EULA text encoding not supported.", e);
				ErrorUtil.handleExceptionAndThrow("ERR000IN", e, activity);
			}finally{
				if( null != isr){
					try {
						isr.close();
					} catch (IOException e) {
						// Suppress the error.
						ErrorUtil.handleException("ERR000IO", e, activity);
					}
				}
			}
		}finally{
			try{
				inputStream.close();
			} catch (IOException e) {
				// Suppress the error.
				ErrorUtil.handleException("ERR0002V", e, activity);
			}

		}
		
		
		AlertDialog alertDialog = new AlertDialog.Builder(activity)
            .setTitle(stringEulaPromptTitle)
            .setMessage(eulaText)
            .setPositiveButton(stringEulaPromptButtonAccept, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
            	try{
            		// Add "Prompt User to Accept EULA" event.
            		Map<String, String> parameters = new HashMap<String, String>();
            		parameters.put(EventBase.PROMPT_EULA__DECISION, EventBase.PROMPT_EULA__DECISION__ACCEPT);
            		EventBase.onEvent(EventBase.PROMPT_EULA, parameters); 
            		
            		SharedPreferences settings = activity.getSharedPreferences(ApplicationPreferenceBase.NAME, Context.MODE_PRIVATE); 
            		assert null != settings;
            		
            		SharedPreferences.Editor ed = settings.edit();
            		assert null != ed;
            		
            		ed.putBoolean(ApplicationPreferenceBase.EULA_ACCEPTANCE_REQUIRED, false);
            		ed.commit();

            		onAccept(activity);

            	}catch(HandledException h){ // Ignore.
            	}catch(Exception exp){
            		activity.setResult(Activity.RESULT_CANCELED);
            		activity.finish();
            		
            		Log.e(TAG, "ERR000IP", exp);
            		ErrorUtil.handleExceptionNotifyUser("ERR000IP", exp, activity);
            	}

            }
        })
        .setNegativeButton(stringEulaPromptButtonReject, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	try{
            		// Add "Prompt User to Accept EULA" event.
            		Map<String, String> parameters = new HashMap<String, String>();
            		parameters.put(EventBase.PROMPT_EULA__DECISION, EventBase.PROMPT_EULA__DECISION__DECLINE);
            		EventBase.onEvent(EventBase.PROMPT_EULA, parameters); 

            		
            		activity.setResult(Activity.RESULT_CANCELED);
            		activity.finish();
            	}catch(HandledException h){ // Ignore.
            	}catch(Exception exp){
            		Log.e(TAG, "ERR000IQ", exp);
            		ErrorUtil.handleExceptionNotifyUser("ERR000IQ", exp, activity);
            	}
            	return;
            }
        })
        .setCancelable(false)
        .create();
		alertDialog.show(); // It is ok that this dialog is not managed. 
	}
	
	
}
