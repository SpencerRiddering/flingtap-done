// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;

import java.util.Calendar;

/**
 * 
 * 
 * 
 * @author spencer
 */
public class AboutActivity extends Activity {
	private static final String TAG = "AboutActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try{
			//SessionUtil.onSessionStart(this);

			setContentView(R.layout.about);
			// TODO: ! Add title here.
//			setTitle(R.string.help_task_List);
			
			// Add Current Version Number.
			String versionName = getString(R.string.unknown);
			try {
				PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
				assert null != pi;
				versionName = pi.versionName;
				assert null != versionName;
			} catch (PackageManager.NameNotFoundException exp) {
				Log.e(TAG, "ERR00004", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR00004", exp, this);
			}
			TextView version = (TextView)findViewById(R.id.about_version);
			assert null != version;
			assert null != getString(R.string.versionX);
			version.setText(TextUtils.expandTemplate(getText(R.string.versionX), versionName));

			TextView trialDays = (TextView)findViewById(R.id.trialDaysRemainingX);
			SharedPreferences settings = getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE); 
			long deadline = settings.getLong(ApplicationPreference.UPDATE_REQUIRED_DEADLINE, Long.MAX_VALUE);
			if( Long.MAX_VALUE != deadline ){
				Calendar begin = Calendar.getInstance();
				Calendar end = Calendar.getInstance();
				end.setTimeInMillis(deadline);
				
				// Calculate day difference accounting for day-light savings time.
				long endL = end.getTimeInMillis() + end.getTimeZone().getOffset( end.getTimeInMillis() );
				long startL = begin.getTimeInMillis() + begin.getTimeZone().getOffset( begin.getTimeInMillis() );
				int daysRemaining = Math.round((endL - startL) / (24 * 60 * 60 * 1000)); // Divide by milliseconds in a day.
				
				version.setText(TextUtils.expandTemplate(getText(R.string.trialDaysRemainingX), String.valueOf(daysRemaining)));
				trialDays.setVisibility(View.VISIBLE);
			}

			// Disabled because GMail doesn't allow attachments from my content provider 
			//   and instead insists on a file URI (file:///sdcard/blah/blah) or something,, I don't know.
			//   all I know is that this code worked until just recently. 
			//   I don't have time to fix it now so I am disabling it. 
//			Button feedbackButton = (Button)findViewById(R.id.about_feedback_button);
//			assert null != feedbackButton;
//			feedbackButton.setOnClickListener(new View.OnClickListener(){
//				public void onClick(View arg0) {
//					try{
//						Event.onEvent(Event.ABOUT_FEEDBACK_BUTTON_CLICKED, null); 
//						
//						FeedbackPart.launchFeedbackDialog(AboutActivity.this, null); 
//					}catch(HandledException h){ // Ignore.
//					}catch(Exception exp){
//						Log.e(TAG, "ERR00002", exp);
//						ErrorUtil.handleExceptionNotifyUser("ERR00002", exp, AboutActivity.this);
//					}
//				}
//			});
			
			

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00001", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00001", exp, this);
		}catch(ThreadDeath td){
			ErrorUtil.handleError("ERR00001", td, this);
		}catch(OutOfMemoryError oome){
			ErrorUtil.handleError("ERR00001", oome, this);
		} 		
	}

	@Override
	protected void onResume() {
		super.onResume();
		SessionUtil.onSessionStart(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		SessionUtil.onSessionStop(this);
	}
	
}
