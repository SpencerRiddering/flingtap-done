// Licensed under the Apache License, Version 2.0

package com.flingtap.common;

import java.util.HashMap;

import com.flingtap.common.HandledException;
import com.flingtap.common.SessionUtilBase;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public abstract class StaticDisplayActivityBase extends Activity {
	private static final String TAG = "StaticDisplayActivity";

	public static final String EXTRA_LAYOUT_RES_ID = "com.flingtap.common.intent.extra.StaticDisplayActivity.LAYOUT_RES_ID";
	public static final String EXTRA_TITLE_RES_ID = "com.flingtap.common.intent.extra.StaticDisplayActivity.TITLE_RES_ID";
	
	protected abstract String getSessionKey();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try{
			SessionUtilBase.onSessionStart(this, getSessionKey());

			Intent intent = getIntent();
			assert null != intent;
			
			int layoutResId = intent.getIntExtra(EXTRA_LAYOUT_RES_ID, 0);
			if( 0 == layoutResId ){
				Log.e(TAG, "ERR000IR Must specify a body layout resource id.");
				ErrorUtil.handleExceptionFinish("ERR000IR", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
				return;
			}
			assert layoutResId > 0;
			
			int titleResId = intent.getIntExtra(EXTRA_TITLE_RES_ID, 0);
			if( 0 == titleResId ){
				Log.e(TAG, "ERR000IS Must specify a title string resource id.");
				ErrorUtil.handleExceptionFinish("ERR000IS", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
				return;
			}
			assert titleResId > 0;
			
			
			setContentView(layoutResId);
			setTitle(titleResId);
			
			// Track which static displays are requested. 
			// Uses the title as a more userfriendly label (rather than the resource id).
			HashMap<String,String> params = new HashMap<String,String>();
			params.put(EventBase.STATIC_DISPLAY__PARAM_TITLE, getResources().getResourceEntryName(titleResId));
			params.put(EventBase.STATIC_DISPLAY__PARAM_LAYOUT, getResources().getResourceEntryName(layoutResId));
			EventBase.onEvent(EventBase.STATIC_DISPLAY, params); 

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000IT", exp);
			ErrorUtil.handleExceptionFinish("ERR000IT", exp, this);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		SessionUtilBase.onSessionStart(this, getSessionKey());
	}

	@Override
	protected void onPause() {
		super.onPause();
		SessionUtilBase.onSessionStop(this);
	}

//	// TODO: !!! Consider wrapping all calls that launch the intent in a try-catcch so we get any error messagse.
//	public static final Intent createIntent(Context context, int layout, int title){
//		Intent helpIntent = new Intent();
//		ComponentName cn = new ComponentName(context.getPackageName(), StaticDisplayActivityBase.class.getName());
//		helpIntent.setComponent(cn);
//		helpIntent.putExtra(StaticDisplayActivityBase.EXTRA_TITLE_RES_ID, title);
//		helpIntent.putExtra(StaticDisplayActivityBase.EXTRA_LAYOUT_RES_ID, layout);
//		return helpIntent;
//	}
//
}
