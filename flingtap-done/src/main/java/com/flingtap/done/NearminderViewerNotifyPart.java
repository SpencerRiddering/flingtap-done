// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flingtap.done.util.Constants;
import com.flingtap.done.base.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

/** 
 * Displays a Nearminder. 
 * 
 * TODO: !!!! Mark the actual location of the nearminder (with contact method bubble if available)
 * 
 * TODO: !!! What happens if the user just hits the back button? Should snooze,, right?
 * TODO: !!! Snooze needs some polish. Problem is that GPS is a little flakey, so the user appears to come and go from the region even if they stand still. Maybe deactivate the proximity alert for 30 minutes,, or something.
 * 
 * 
 */
public class NearminderViewerNotifyPart extends AbstractContextActivityParticipant {
	private static final String TAG = "NearminderViewerNotifyPart";
	
	protected final static int FIRST_CODE_ID 	  = 2200;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}	

	/**
	 * Standard projection for the interesting columns of a proximity alert.
	 */
	private static final String[] PROXIMITY_PROJECTION = new String[] {
			TaskProvider.PROXIMITY_ALERT_TABLE_NAME+"."+Task.ProximityAlerts._ID,        // 0
			Task.Tasks.TASK_TITLE,        // 1
			Task.TaskAttachments.NAME,        // 2
			TaskProvider.TASK_TABLE_NAME+"."+Task.Tasks._ID,        // 3
	};	
	private static final int ID_INDEX 				= 0;
	private static final int TASK_TITLE_INDEX 		= 1;
	private static final int ATTACHMENT_NAME_INDEX 	= 2;
	private static final int TASK_ID_INDEX			= 3;
	private Cursor mProximityCursor;	
	
	protected final static int DELETE_NEARMINDER_DIALOG_ID  = FIRST_CODE_ID + 50;	
	protected final static int CHOOSE_ACTION_DIALOG_ID      = FIRST_CODE_ID + 51;	
	
	final private Activity mActivity;
	private Uri mProxUri;
	private String mTaskId;
	private String mProximityAlertId;
	
	// final String taskId, 
	public NearminderViewerNotifyPart(Activity activity, final String proximityAlertId, Uri proxUri) {
		super();
		
		mActivity = activity;
		assert null != mActivity;
		
		mProxUri = proxUri;
		assert null != mProxUri;
		
		mProximityAlertId = proximityAlertId;
		assert null != mProximityAlertId;
		
	    NotificationManager nm = (NotificationManager)mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
	    
	    // Cancel notification.
        Cursor notifCursor = mActivity.getContentResolver().query(
        		Task.Notification.CONTENT_URI, 
        		new String[]{Task.Notification._ID}, 
        		Task.Notification._URI+"=?", 
        		new String[]{Task.ProximityAlerts.CONTENT_URI.buildUpon().appendPath(mProximityAlertId).build().toString()}, 
        		null);
        
        if( notifCursor.moveToFirst() ){
        	nm.cancel(notifCursor.getInt(0));
        	int count = mActivity.getContentResolver().delete(ContentUris.withAppendedId(Task.Notification.CONTENT_URI, notifCursor.getInt(0)), null, null);
        	assert 1 == count;
        }
        notifCursor.close();
		
	    View backgroundView = mActivity.findViewById(R.id.proximity_alert_view_dialog);
	    backgroundView.setVisibility(View.VISIBLE);
	    
		Uri proximityTaskAttachUri = Uri.withAppendedPath(proxUri, "tasks/attachments");
		mProximityCursor = mActivity.getContentResolver().query(proximityTaskAttachUri, PROXIMITY_PROJECTION, null, null, null);
		if( !mProximityCursor.moveToFirst()){
			// TODO: !!! Causes the Activity to finish without user ever seeing a message!
			Log.e(TAG, "ERR0004F Empty proximity attachment cursor.");
			ErrorUtil.handleExceptionNotifyUserFinish("ERR0004F", (Exception)(new Exception( proximityTaskAttachUri.toString() )).fillInStackTrace(), mActivity);
			return;
		}
		
		TextView dialogTitle = (TextView)mActivity.findViewById(R.id.proximity_alert_viewer_dialog_title);
		dialogTitle.setText(mProximityCursor.getString(ATTACHMENT_NAME_INDEX));
		
		TextView dialogSnippet = (TextView)mActivity.findViewById(R.id.proximity_alert_title);
		dialogSnippet.setText(mProximityCursor.getString(TASK_TITLE_INDEX));
		
		mTaskId = mProximityCursor.getString(TASK_ID_INDEX);
		mProximityCursor.close();
	    
	    
//	    // Get Task Id.
//		Cursor mAttachCursor = activity.getContentResolver().query(
//				Task.TaskAttachments.CONTENT_URI, 
//				new String[] {Task.TaskAttachments.TASK_ID}, // Assumes a 1:1 relationship between proximity alert and task.  
//				Task.TaskAttachments._URI + "=?", 
//				new String[]{Task.ProximityAlerts.CONTENT_URI.buildUpon().appendPath(proximityAlertId).toString()}, 
//				null);
//		
//		assert mAttachCursor.getCount() <= 1; // Assert a 1:1 relationship between proximity alert and task.  
//		
//		if(!mAttachCursor.moveToFirst()){
//			Log.e(TAG, "Failed to find referenced task attachment in database.");
//			Toast.makeText(activity, "Internal error.", Toast.LENGTH_SHORT).show();
//			activity.setResult(Activity.RESULT_CANCELED);
//			activity.finish();
//			return; // TODO: ! ContextActivityParticipant needs an init() method (separate from constructor) so that in it's return value it can let the main activity know that it needs to abort.
//		}
//		final String taskId = mAttachCursor.getString(0);
//		mAttachCursor.close(); 
//	    
//		// No need to manage this cursor because it's used only once to set the task name. 
//		Cursor mTaskCursor = activity.getContentResolver().query(
//				Task.Tasks.CONTENT_URI.buildUpon().appendPath(taskId).build(), 
//				new String[] {Task.Tasks.TASK_TITLE}, 
//				null, 
//				null, 
//				null);
//		if(!mTaskCursor.moveToFirst()){
//			Log.e(TAG, "Failed to find referenced task in database.");
//			Toast.makeText(activity, "Internal error.", Toast.LENGTH_SHORT).show();
//			activity.setResult(Activity.RESULT_CANCELED);
//			activity.finish();
//			return; // TODO: ! ContextActivityParticipant needs an init() method (separate from constructor) so that in it's return value it can let the main activity know that it needs to abort.
//		}
//		TextView taskName = (TextView)activity.findViewById(R.id.proximity_alert_title);
//		taskName.setText(mTaskCursor.getString(0));
//		mTaskCursor.close(); 
		
			
		// Listen for "Task" button clicks. 
		Button nearminderActionButton = (Button) mActivity.findViewById(R.id.proximity_alert_button_choose_action);
		nearminderActionButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				try{
					mActivity.showDialog(CHOOSE_ACTION_DIALOG_ID);
				}catch(HandledException h){ // Ignore.
				}catch(Exception exp){
					Log.e(TAG, "ERR0004G", exp);
					ErrorUtil.handleExceptionNotifyUser("ERR0004G", exp, mActivity);
				}
			}
		});	
		
		// Listen for "Snooze" button clicks.
		//   Snooze means wait until the next time the user enters the proximity.
		Button snoozeButton = (Button) mActivity.findViewById(R.id.proximity_alert_button_snooze);
		snoozeButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				mActivity.setResult(Activity.RESULT_OK);
				mActivity.finish();
			}
		});
		
	}

	private WeakReference<LabelUtil.RenameLabelOnTextSetListener> renameLabelOnTextSetListener = null;
	public Dialog onCreateDialog(int dialogId){
		try{
			switch(dialogId){
				case DELETE_NEARMINDER_DIALOG_ID:	
					return new AlertDialog.Builder(mActivity)
			        .setTitle(R.string.dialog_confirmDelete)
			        .setIcon(android.R.drawable.ic_dialog_alert)
			        .setMessage(R.string.dialog_areYouSure)
			        .setPositiveButton(R.string.button_yes, new android.content.DialogInterface.OnClickListener(){
			    		public void onClick(DialogInterface dialog, int whichButton){
			    			try{
								Event.onEvent(Event.DELETE_NEARMINDER, null);

								//Log.v(TAG, "onClick(..) called.");
								if( Nearminder.delete(mActivity, mProxUri) ){
									mActivity.setResult(Activity.RESULT_OK);
								}else{
									mActivity.setResult(SharedConstant.RESULT_ERROR);
								}
								mActivity.finish();
								
			    			}catch(HandledException h){ // Ignore.
			    			}catch(Exception exp){
			    				Log.e(TAG, "ERR000CH", exp);
			    				ErrorUtil.handleExceptionNotifyUser("ERR000CH", exp, mActivity);
			    			}
			    		}
			    	})
			        .setNegativeButton(R.string.button_no, null)
			    	.create();		
				case CHOOSE_ACTION_DIALOG_ID:
					return new AlertDialog.Builder(mActivity)
					.setItems(R.array.options_nearminderNotification, new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int which) {
							try{
								switch(which){
									case 0: //  <item>Open task</item>
										assert null != mTaskId;
										
										Intent intent = new Intent();
										intent.setAction(Intent.ACTION_VIEW); // TODO: I think that VIEW_ACTION would be better. (currently do not have a pure task view action though.)
										intent.setData(Task.Tasks.CONTENT_URI.buildUpon().appendPath(mTaskId).build()); // TODO: Can't use MIME type because tasks don't have a clear MIME type,, right?
										mActivity.startActivity(intent);
										mActivity.finish();
										break;
									case 1: //  <item>Get Directions</item>
										Uri proximityAlertUri = ContentUris.withAppendedId(Task.ProximityAlerts.CONTENT_URI,Long.parseLong(mProximityAlertId));
										Nearminder.launchGetDirections(mActivity, proximityAlertUri);
										
										break;
									case 2: //  <item>Delete Nearminder</item>
										
										// TODO: Add a button-clicked type event here.
										// Prepare event info.
										//Event.onEvent(Event.NEARMINDER_ATTACHMENT_sdfs_HANDLER_DELETE_CONTEXT_MENU_ITEM_CLICKED, null); 

										mActivity.showDialog(DELETE_NEARMINDER_DIALOG_ID);
										
										break;
								}
							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR0004H", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR0004H", exp, mActivity);
							}
						}
					})
					.create();
					
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000CI", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000CI", exp, mActivity);
		}
		return null;
	}

}
