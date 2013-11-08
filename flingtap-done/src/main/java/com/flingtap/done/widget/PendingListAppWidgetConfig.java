// Licensed under the Apache License, Version 2.0

package com.flingtap.done.widget;

import com.flingtap.common.HandledException;
import com.flingtap.done.ErrorUtil;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.RemoteViews;
import android.widget.RemoteViews.RemoteView;

public class PendingListAppWidgetConfig extends Activity {
    static final String TAG = "PendingListAppWidgetConfig";

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public PendingListAppWidgetConfig() {
        super();
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
       	try{
       		// Set the result to CANCELED.  This will cause the widget host to cancel
       		// out of the widget placement if they press the back button.
       		setResult(RESULT_CANCELED);
       		
       		// Set the view layout resource to use.
       		setContentView(R.layout.pending_appwidget_configure);
       		
       		// Do configuration stuff here.
       		Intent theIntent = getIntent();
       		Log.v(TAG, theIntent.getAction());
       		
       		// Bind the action for the save button.
       		findViewById(R.id.positive_button).setOnClickListener(mOnClickListener);
       		findViewById(R.id.negative_button).setOnClickListener(mOnClickListener);
       		
       		// Set pre-sets.
       		((RadioGroup)findViewById(R.id.radio_theme)).check(R.id.theme_light);
       		((RadioGroup)findViewById(R.id.radio_text_size)).check(R.id.text_small);
       		
       		// Find the widget id from the intent. 
       		Intent intent = getIntent();
       		Bundle extras = intent.getExtras();
       		if (extras != null) {
       			mAppWidgetId = extras.getInt(
       					AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
       		}
       		
       		// If they gave us an intent without the widget id, just bail.
       		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
       			finish();
       			return;
       		}
       		
       		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
       		AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(mAppWidgetId);
       		if( R.layout.pending_appwidget_provider_2x2_light == info.initialLayout ){
       			findViewById(R.id.text_jumbo).setVisibility(View.GONE);
       		}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000JW", exp);
			ErrorUtil.handleExceptionNotifyUserFinish("ERR000JW", exp, this);
		}

    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
        	try{
        		final Context context = PendingListAppWidgetConfig.this;
        		
        		
        		if( v.getId() == R.id.positive_button ){
        			ContentValues values = new ContentValues(1);
        			
        			values.put(Task.AppWidgets._TYPE, Task.AppWidgets.TYPE_PENDING_LIST);
        			
        			// Determine theme.
        			RadioGroup themeRadio = (RadioGroup)findViewById(R.id.radio_theme);
        			int selectedThemeRadioId = themeRadio.getCheckedRadioButtonId();
        			switch(selectedThemeRadioId){
        				case R.id.theme_dark:
        					values.put(Task.AppWidgets._THEME, Task.AppWidgets.THEME_PENDING_DARK);
        					break;
        				default: // fallthrough
        				case R.id.theme_light:
        					values.put(Task.AppWidgets._THEME, Task.AppWidgets.THEME_PENDING_LIGHT);
        					break;
        			}
        			
        			// Determine text size.
        			RadioGroup textSizeRadio = (RadioGroup)findViewById(R.id.radio_text_size);
        			int selectedTextSizeRadioId = textSizeRadio.getCheckedRadioButtonId();
        			switch(selectedTextSizeRadioId){
        				default: // fallthrough
        				case R.id.text_small:
        					values.put(Task.AppWidgets._TEXT_SIZE, Task.AppWidgets.TEXT_SIZE_SMALL);
        					break;
        				case R.id.text_medium:
        					values.put(Task.AppWidgets._TEXT_SIZE, Task.AppWidgets.TEXT_SIZE_MEDIUM);
        					break;
        				case R.id.text_large:
        					values.put(Task.AppWidgets._TEXT_SIZE, Task.AppWidgets.TEXT_SIZE_LARGE);
        					break;
        				case R.id.text_jumbo:
        					values.put(Task.AppWidgets._TEXT_SIZE, Task.AppWidgets.TEXT_SIZE_JUMBO);
        					break;
        			}

        			// Insert appwidget ID into app widget table.
        			//Log.v(TAG, "Adding widget with Id " + mAppWidgetId);
        			// Not bound when this method called, so this is first setup.
        			values.put(Task.AppWidgets._APP_WIDGET_ID, mAppWidgetId);
        			// TODO: Add widget configuration here.
        			Uri uri = context.getContentResolver().insert(Task.AppWidgets.CONTENT_URI, values);
        			if( null == uri ){
        				// TODO: Handle this error.
        			}
        			
        			// Push widget update to surface with newly set prefix
        			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        			PendingListAppWidgetProvider.updateAppWidget(context, appWidgetManager,
        					new int[]{mAppWidgetId});
        			
        			// Make sure we pass back the original appWidgetId
        			Intent resultValue = new Intent();
        			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        			setResult(RESULT_OK, resultValue);
        			finish();
        			
        		}else { // v.getId() == R.id.negative_button
        			finish();
        		}
    		}catch(HandledException h){ // Ignore.
    		}catch(Exception exp){
    			Log.e(TAG, "ERR000JZ", exp);
    			ErrorUtil.handleExceptionNotifyUserFinish("ERR000JZ", exp, PendingListAppWidgetConfig.this);
    		}
        }
    };
}
