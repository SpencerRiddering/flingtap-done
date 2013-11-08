// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;
import com.flingtap.common.HandledException;
import com.flingtap.done.android.TabHost;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;

//import android.widget.TabHost;

/**
 * 
 * 
 * 
 * @author spencer
 *
 */
public class TaskEditorTabActivity extends CoordinatedTabActivity {
	private static final String TAG = "TaskEditorTabActivity";

	// The different distinct states the activity can be run in.
	private static final int STATE_VIEW = 0;
	private static final int STATE_EDIT = 1;
	private static final int STATE_INSERT = 2;  

	// Identifiers for our menu items.
	protected static final int DONE_ID                        = Menu.FIRST; 
	protected static final int DISCARD_ID                      = Menu.FIRST + 1;
	protected static final int EDIT_ID                         = Menu.FIRST + 3;
	protected static final int DISMISS_ID                         = Menu.FIRST + 8;	
	protected static final int SEARCH_ID                         = Menu.FIRST + 11;	
	
	
	protected static final int DEFAULT_REQUEST_CODE = 0;	
	
	private int mState;		  // Requested Action 
	private Uri mURI; 		  // Requested URI
	protected TabHost tabHost; 
	
	public final static String CURRENT_TAB_LABEL =  "current_label";
	public final static String TAB_LABEL_DETAILS =  "details";
	public final static String TAB_LABEL_ATTACHMENTS =  "attachments";
	
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            //Log.v(TAG, "onCreate(..) called");

            // Allow user to use menu shortcuts without opening menu.
            setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

            // Remove Title Bar.
            requestWindowFeature(Window.FEATURE_NO_TITLE); 
            
            final Intent intent = getIntent();

    		final String action = intent.getAction();
    		if (action.equals(Intent.ACTION_EDIT)) {
    			mState = STATE_EDIT;
    			mURI = intent.getData();
    			
    			// Prepare event info.
    			Event.onEvent(Event.EDIT_TASK, null); 
    		} else if (action.equals(Intent.ACTION_INSERT)) { // TODO: !!! Shouldn't this be handled by TaskEditor?

    			mState = STATE_INSERT;

    			mURI = intent.getData(); 
    			if( null == mURI || Task.Tasks.CONTENT_URI_STRING.equals(mURI.toString())){ // ie there is no task id at the end of the URI.
        			mURI = getContentResolver().insert(Task.Tasks.CONTENT_URI, null);

        			// If we were unable to create a new task, then just finish
        			// this activity. A RESULT_CANCELED will be sent back to the
        			// original activity if they requested a result.
        			if (mURI == null) {
        				Log.e(TAG, "ERR00085 Failed to insert new task.");
        				ErrorUtil.handleExceptionFinish("ERR00085", (Exception)(new Exception(  )).fillInStackTrace(), this);
        				return;
        			}
        			intent.setData(mURI); 

					// Update the filter bit.
					FilterUtil.applyFilterBits(this, mURI);
    			}else{
    				if( mURI.toString().startsWith(Task.Tasks.CONTENT_URI_STRING) ){
    					// Task was created previously
    					// Do nothing here.
    				}else{
    					Log.e(TAG, "ERR000DH Unknown URI type " + mURI);
    					ErrorUtil.handleExceptionFinish("ERR000DH", (Exception)(new Exception( mURI.toString()  )).fillInStackTrace(), this);
    					return;
    				}
    			}

    			setResult(RESULT_OK, (new Intent()).setData(mURI));

    		} else if (action.equals(Intent.ACTION_VIEW)) {
    			mState = STATE_VIEW;
    			mURI = intent.getData();
    			setResult(RESULT_OK);
    		} else if (action.equals(com.flingtap.done.TaskEditor.NOTIFY_ACTION)) {
    			mState = STATE_VIEW;
    			mURI = intent.getData();
    			setResult(RESULT_OK);
    			
    			intent.setAction(Intent.ACTION_VIEW);

				NotificationUtil.removeNotification(this, mURI); // This is used!

    		} else { // TODO: !!! Check that all activities check their calling action and report an error if it is not recognized.
    			// Whoops, unknown action! Bail.
    			Log.e(TAG, "ERR00081 Unknown action, exiting");
    			ErrorUtil.handleExceptionFinish("ERR00081", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
    			return;
    		}
    		
            tabHost = getTabHost();

            // This is so that the tabs are in the same position after a config change.
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ){
            	addAttachmentsTab(intent);
            	addDetailsTab(intent);
            }else{
            	addDetailsTab(intent);
            	addAttachmentsTab(intent);
            }
            
            if( null != intent.getStringExtra(CURRENT_TAB_LABEL) ){
            	tabHost.setCurrentTabByTag(intent.getStringExtra(CURRENT_TAB_LABEL));
            }else{
            	SharedPreferences preferences = getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
            	tabHost.setCurrentTabByTag(preferences.getString(ApplicationPreference.DEFAULT_EDITOR_TAB, ApplicationPreference.DEFAULT_EDITOR_TAB_DEFAULT));
            }
            
        }catch(HandledException h){ // Ignore.
        }catch(Exception exp){
        	Log.e(TAG, "ERR00082", exp);
        	ErrorUtil.handleExceptionFinish("ERR00082", exp, this);
        }        
    }

	private void addAttachmentsTab(final Intent intent) {
		Intent attachIntent = new Intent(intent);
		ComponentName attachCompName = new ComponentName(StaticConfig.PACKAGE_NAME,"com.flingtap.done.TaskAttachmentListTab");
		attachIntent.setComponent(attachCompName);
		tabHost.addTab(tabHost.newTabSpec(TAB_LABEL_ATTACHMENTS)
		        .setIndicator(getText(R.string.attachments), this.getResources().getDrawable(R.drawable.tab_attach))
		        .setContent(attachIntent));
	}

	private void addDetailsTab(final Intent intent) {
		Intent editorIntent = new Intent(intent);
		ComponentName editorCompName = new ComponentName(StaticConfig.PACKAGE_NAME,"com.flingtap.done.TaskEditor");
		editorIntent.setComponent(editorCompName);
		tabHost.addTab(tabHost.newTabSpec(TAB_LABEL_DETAILS)
		        .setIndicator(getText(R.string.details), this.getResources().getDrawable(R.drawable.tab_details)) 
		        .setContent(editorIntent));
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		try{
			// If we are working on a real honest-to-ghod task, then append to the
			// menu items for any other activities that can do stuff with it
			// as well. This does a query on the system for any activities that
			// implement the ALTERNATIVE_ACTION for our data, adding a menu item
			// for each one that is found.
			Intent intent = new Intent(null, getIntent().getData());
			intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
			int i = menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 30, new ComponentName(this,
					TaskEditor.class), null, intent, 0, null);
			if( i > 0 ){
				return true;
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00083", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00083", exp, this);
		}
		return true;
	}    
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		try{
            // Do nothing.
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0009Y", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0009Y", exp, this);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		try{
			// Handle all of the possible menu actions.
			switch (item.getItemId()) {
				case EDIT_ID:
					if (mState == STATE_VIEW){
						Event.onEvent(Event.TASK_VIEW_EDIT_OPTIONS_MENU_ITEM_CLICKED, null); 
						editTask();
					}
					break;
				case DISMISS_ID:
					if (mState == STATE_VIEW){
						// Do nothing.
						finish();
					}
					break;
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00084", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00084", exp, this);
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Take care of editing a task. 
	 */
	private final void editTask() {
		//Log.v(TAG, "editTask(..) called.");
		Intent intent = new Intent();
		intent.setData(mURI);
		intent.setAction(Intent.ACTION_EDIT);
		intent.putExtra(CURRENT_TAB_LABEL, tabHost.getCurrentTabTag());
		startActivityForResult(intent, DEFAULT_REQUEST_CODE); 
	}
    	
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode, resultCode, data);
		//Log.v(TAG, "onActivityResult(..) called with resultCode " + resultCode);
		if (SharedConstant.RESULT_ERROR == resultCode) {
			ErrorUtil.notifyUser(this);
			setResult(resultCode);
			finish();
			return;
		}
		if( Activity.RESULT_CANCELED == resultCode ){
			Toast.makeText(this, R.string.toast_canceled, Toast.LENGTH_SHORT).show();
			setResult(resultCode);
			finish();
			return;
		}
	}
}
