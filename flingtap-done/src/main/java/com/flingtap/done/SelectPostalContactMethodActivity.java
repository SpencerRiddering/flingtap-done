// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.ContactMethodsColumns;
import android.provider.Contacts.People;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;
import com.flingtap.done.util.Constants;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Prompts user to select a postal address. 
 * 
 * For use with SDK versions 3 and 4.
 * 
 * TODO: !!! Return immediately if only one postal address exists.
 */
public class SelectPostalContactMethodActivity extends Activity {
	
	private final static String TAG = "SelectPostalContactMethodActivity";

	public static final String EXTRA_TASK_ID = "TASK_ID";

	public static final String DRAWABLE = "DRAWABLE";
	public static final String LABEL = "LABEL";
	public static final String ADDRESS_1 = "ADDRESS_1";
	public static final String ADDRESS_2 = "ADDRESS_2";
	public static final String RETURN_DATA = "RETURN_DATA";
	
	private static final int DIALOG_CHOOSE_ADDRESS_ID = 50;
	private ViewTypeCursorAdapter addressSelectionAdapter = null;
	private long mPersonId = Constants.DEFAULT_NON_ID;
	
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		try{
			//Log.v(TAG, "onCreate(..) called.");
			SessionUtil.onSessionStart(this);
			
			//Log.v(TAG, "onCreate(..) called.");
			// Prepare event info.
			Event.onEvent(Event.SELECT_POSTAL_CONTACT_METHOD, null); // Map<String,String> parameters = new HashMap<String,String>();
			
			final Intent intent = getIntent();
	        assert intent != null;
	        
	        final Uri data = intent.getData(); 
	        if (data == null) {
				// Whoops, unknown action! Bail.
				Log.e(TAG, "ERR0005X Intent data is null");
				ErrorUtil.handleExceptionFinish("ERR0005X", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
				return;
	        }
	        
	        // Verify that it's an attachment URI with ID.
	        assert data.toString().startsWith(People.CONTENT_URI.toString());
	        assert -1L != ContentUris.parseId(data);
	        
			// Make sure we are handling an acceptable action.
			final String action = intent.getAction();
			if (null == action || !action.equals(Constants.PICK_POSTAL_CONTACT_METHOD_ACTION)  ) {
				// Whoops, unknown action! Bail.
				Log.e(TAG, "ERR0005Y Unspecified or unrecognized action.");
				ErrorUtil.handleExceptionFinish("ERR0005Y", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
				return;			
			}

			long taskId = intent.getLongExtra(EXTRA_TASK_ID, Constants.DEFAULT_NON_ID);
			if(taskId == Constants.DEFAULT_NON_ID){
				// Whoops, unknown action! Bail.
				Log.e(TAG, "ERR0005Z");
				ErrorUtil.handleExceptionFinish("ERR0005Z", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
				return;			
			}
			//Log.d(TAG, "taskId == " + taskId);
			
			final Long personId = ContentUris.parseId(data);
			if( personId < 0 ){
				// Whoops, bad data! Bail.
				Log.e(TAG, "ERR00060");
				ErrorUtil.handleExceptionFinish("ERR00060", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
				return;						
			}	
			mPersonId = personId;
			//Log.d(TAG, "personId == " + personId);
			
			// ****************************************************************************
			// TODO: !! The efficiency of the following code would be better if you used a straight db connection and joined the tables.
			// TODO: Refactor this bit of code into a subclass so that you can have a more generic ACTION_PICK for person contact methods.
			// ****************************************************************************
			// Create a Set of all the proximity ID's which are attached to the task.
			Cursor proximityAlertIdCursor = getContentResolver().query(Task.TaskAttachments.CONTENT_URI, new String[]{Task.TaskAttachments._URI}, Task.TaskAttachments.TASK_ID + "=? AND " + Task.TaskAttachments._URI + " LIKE " + DatabaseUtils.sqlEscapeString(Task.ProximityAlerts.CONTENT_URI.toString() + "/%") , new String[]{String.valueOf(taskId)}, null);
			assert null != proximityAlertIdCursor;
			final HashSet<String> proximityAlertIdSet = new HashSet<String>();
			while(proximityAlertIdCursor.moveToNext()){
				proximityAlertIdSet.add(proximityAlertIdCursor.getString(0).substring(proximityAlertIdCursor.getString(0).lastIndexOf('/')+1));
			}
			proximityAlertIdCursor.close();

			Iterator<String> itr = proximityAlertIdSet.iterator();
			StringBuffer sb = new StringBuffer();
			while(itr.hasNext()){
				sb.append(itr.next());
				if( itr.hasNext()){
					sb.append(',');
				}
			}
			// Create a Set of all the intent data person's contact-methods that are selected by a proximity alert -- AND -- which are referred to (indirectly) by this task.
			Cursor mContactMethodWithNearminderCursor = getContentResolver().query(Task.ProximityAlerts.CONTENT_URI, new String[]{Task.ProximityAlerts._SELECTED_URI}, Task.ProximityAlerts._ID + " IN (" + sb + ") AND " + Task.ProximityAlerts._SELECTED_URI + " LIKE " + DatabaseUtils.sqlEscapeString(data.toString()+'/'+Contacts.People.ContactMethods.CONTENT_DIRECTORY+"/%"), null, null); 
			assert null != mContactMethodWithNearminderCursor;
			final HashSet<String> contactMethodWithNearminder = new HashSet<String>();
			while(mContactMethodWithNearminderCursor.moveToNext()){
				contactMethodWithNearminder.add(mContactMethodWithNearminderCursor.getString(0).substring(mContactMethodWithNearminderCursor.getString(0).lastIndexOf('/')+1));
			}
			mContactMethodWithNearminderCursor.close();

			// TODO: These filters assume that contact method attachments indicate that a proximity alert exists. Should instead join against the proximity alert table.
			String[] SELECTION_ARGS = new String[]{personId.toString(), String.valueOf(Contacts.KIND_POSTAL)};
			
			// Get a list of the postal addresses
			// TODO: Should use the isprimary field of contact_methods to help order the records.
			Cursor mContactMethodPostalCursor = managedQuery(ContactMethods.CONTENT_URI,
					ContactMethodProjectionGps.CONTACT_METHODS_PROJECTION, ContactMethods.PERSON_ID+"=? AND "+ContactMethodsColumns.KIND+"=?" , SELECTION_ARGS, null); 
			
			if( 0 == mContactMethodPostalCursor.getCount()){ 
				// ***********************
				// Zero postal addresses
				// ***********************
				
				// No postal addresses exist, so no GPS locations can exist, right? 
				mContactMethodPostalCursor.close();
				Log.e(TAG, "ERR00061 No postal addresses exist.");
				ErrorUtil.handle("ERR00061", "No postal addresses exist.", this); // TODO: Consider whether this should be an error.
				setResult(RESULT_CANCELED);
				finish();
				return;
			}

			// ******************************************************
			// Multiple postal addresses exists
			// ******************************************************
			
			// There is more than one postal address, so we need to resolve which address the user wants to use.
            
			addressSelectionAdapter = new ViewTypeCursorAdapter(this,
					R.layout.contact_geocoder_address_list_item,
					mContactMethodPostalCursor,
					new String[] { "" , ContactMethodsColumns.DATA, ContactMethodsColumns.DATA, ContactMethods._ID },
					new int[] { R.id.address_list_dialog_item_text1 , R.id.address_list_dialog_item_text2 , R.id.address_list_dialog_item_text3, R.id.address_list_dialog_item_proximity_icon },
					new ViewTypeCursorAdapter.FieldType[] { ViewTypeCursorAdapter.FieldType.CURSOR , ViewTypeCursorAdapter.FieldType.STRING , ViewTypeCursorAdapter.FieldType.STRING, ViewTypeCursorAdapter.FieldType.LONG  }) {

				public void initView(View view, Context context, Cursor cursor){
					super.initView(view, context, cursor);
					try{
						Long value = cursor.getLong(ContactMethodProjectionGps.CONTACT_M_ID_INDEX); 
						view.setTag(value);
						//Log.d(TAG, "rowId == " + cursor.getPosition() + ", "+view+".setTag("+value+")");						
					}catch(HandledException h){ // Ignore.
					}catch(Exception exp){
						Log.e(TAG, "ERR00062", exp);
						ErrorUtil.handleExceptionFinish("ERR00062", exp, SelectPostalContactMethodActivity.this);
					}
				}
		
			    public void setViewData(View view, long value, int rowId){
			    	try{
			    		if( view.getId() == R.id.address_list_dialog_item_proximity_icon){
			    			ImageView iv = (ImageView)view;
			    			if( contactMethodWithNearminder.contains(String.valueOf(value)) ){
			    				iv.setVisibility(View.VISIBLE);
			    			}
			    		}else{
			    			super.setViewData(view, value, rowId);
			    		}
			    	}catch(HandledException h){ // Ignore.
			    	}catch(Exception exp){
			    		Log.e(TAG, "ERR00064", exp);
			    		ErrorUtil.handleExceptionFinish("ERR00064", exp, SelectPostalContactMethodActivity.this);
			    	}
			    }	
			    		    
			    public void setViewData(View view, Cursor cursor, int rowId){
			    	try{
			    		if( view.getId() == R.id.address_list_dialog_item_text1){
			    			TextView tv = (TextView)view;
			    			CharSequence label = determineContactMethodLabel(SelectPostalContactMethodActivity.this, cursor.getInt(ContactMethodProjectionGps.CONTACT_M_KIND_INDEX), cursor.getInt(ContactMethodProjectionGps.CONTACT_M_TYPE_INDEX), cursor.getString(ContactMethodProjectionGps.CONTACT_M_LABEL_INDEX));
			    			tv.setText(label);
			    		}else{
			    			super.setViewData(view, cursor, rowId);
			    		}
			    	}catch(HandledException h){ // Ignore.
			    	}catch(Exception exp){
			    		Log.e(TAG, "ERR00065", exp);
			    		ErrorUtil.handleExceptionFinish("ERR00065", exp, SelectPostalContactMethodActivity.this);
			    	}
			    }
			    public void setViewData(View view, CharSequence text, int rowId){
			    	try{
			    		if( view.getId() == R.id.address_list_dialog_item_text2){
			    			String textString = text.toString(); // TODO: Can this be optimized? 
			    			int newline = textString.indexOf('\n');
			    			if( -1 != newline ){
			    				TextView v1 = (TextView)view;
			    				v1.setText(textString.substring(0,newline));
			    			}else{
			    				TextView v1 = (TextView)view;
			    				v1.setText(textString);
			    			}
			    		}else if( view.getId() == R.id.address_list_dialog_item_text3){
			    			String textString = text.toString(); // TODO: Can this be optimized? 
			    			int newline = textString.indexOf('\n');
			    			if( -1 != newline && textString.length() > (newline + 1) ){
			    				TextView v2 = (TextView)view;
			    				v2.setText(textString.substring(newline+1));
			    			}
			    			
			    		}else{
			    			super.setViewData(view, text, rowId);
			    		}
			    	}catch(HandledException h){ // Ignore.
			    	}catch(Exception exp){
			    		Log.e(TAG, "ERR00066", exp);
			    		ErrorUtil.handleExceptionFinish("ERR00066", exp, SelectPostalContactMethodActivity.this);
			    	}
			    }
			};		
			
            new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_chooseAddress)
                .setAdapter(addressSelectionAdapter, new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					try{
						
						long contactMethodId = addressSelectionAdapter.getItemId(which);
						//Log.d(TAG, "returnValue == " + contactMethodId);
						
						Uri returnData = constructReturnData(personId, contactMethodId);
						
						setResult(RESULT_OK, new Intent().setData(returnData));
						finish();
					}catch(HandledException h){ // Ignore.
					}catch(Exception exp){
						Log.e(TAG, "ERR00067", exp);
						ErrorUtil.handleExceptionFinish("ERR00067", exp, SelectPostalContactMethodActivity.this);
					}
										
				}
			})
            
            
			.setOnCancelListener(new DialogInterface.OnCancelListener(){
				public void onCancel(DialogInterface dialog) {
					setResult(RESULT_CANCELED);
					finish();
				}
			})
            .create()
            .show();						

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00068", exp);
			ErrorUtil.handleExceptionFinish("ERR00068", exp, this);
		}
	}

	protected Uri queryForContainedAttachUri(Uri attachmentUri){
		Cursor mAttachmentCursor = getContentResolver().query(attachmentUri, new String[]{Task.TaskAttachments._URI}, null, null, null);
		if( !mAttachmentCursor.moveToFirst() ){
			return null;
		}
		Uri locationUri = Uri.parse(mAttachmentCursor.getString(0));
		mAttachmentCursor.close();
		return locationUri;
	}		

	public static CharSequence determineContactMethodLabel(Context context, int kind, int type, String otherLabel) {
		CharSequence returnLabel = ContactMethods.getDisplayLabel(context, kind, type, otherLabel); 
		return returnLabel;
	}
	
	protected Uri constructReturnData(Long personId, Long contactMethodId){
		Builder builder = ContentUris.appendId(Contacts.People.CONTENT_URI.buildUpon(), personId);
		builder.appendPath(Contacts.People.ContactMethods.CONTENT_DIRECTORY);
		builder.appendPath(contactMethodId.toString());
		Uri returnData = builder.build();
		return returnData;
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
