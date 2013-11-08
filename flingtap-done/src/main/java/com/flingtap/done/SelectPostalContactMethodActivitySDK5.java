// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flingtap.done.util.Constants;
import com.flurry.android.FlurryAgent;
import com.flingtap.done.base.R;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.ContactMethodsColumns;
import android.provider.Contacts.People;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Prompts user to select a postal address. 
 * 
 * For use with SDK version 5 and above.
 */
public class SelectPostalContactMethodActivitySDK5 extends Activity {
	
	private final static String TAG = "SelectPostalContactMethodActivity";

	public static final String EXTRA_TASK_ID = "TASK_ID";

	public static final String DRAWABLE = "DRAWABLE";
	public static final String LABEL = "LABEL";
	public static final String ADDRESS_1 = "ADDRESS_1";
	public static final String ADDRESS_2 = "ADDRESS_2";
	public static final String RETURN_DATA = "RETURN_DATA";
	
	private static final int DIALOG_CHOOSE_ADDRESS_ID = 50;
	private ViewTypeCursorAdapter addressSelectionAdapter = null;

	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		try{
			//Log.v(TAG, "onCreate(..) called.");
			SessionUtil.onSessionStart(this);
			
			// Prepare event info.
			Event.onEvent(Event.SELECT_POSTAL_CONTACT_METHOD, null); // Map<String,String> parameters = new HashMap<String,String>();
			
			final Intent intent = getIntent();
	        assert intent != null;
	        
	        final Uri contactUri = intent.getData(); 
	        if (contactUri == null) {
				// Whoops, unknown action! Bail.
				Log.e(TAG, "ERR000J5 Intent data is null");
				ErrorUtil.handleExceptionFinish("ERR000J5", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
				return;
	        }
	        
	        // Verify that it's an attachment URI with ID.
	        assert contactUri.toString().startsWith("content://com.android.contacts/contacts/"); // TODO: !!! Refactor this String into a constant object. 
	        assert -1L != ContentUris.parseId(contactUri);
	        
			// Make sure we are handling an acceptable action.
			final String action = intent.getAction();
			if (null == action || !action.equals(Constants.PICK_POSTAL_CONTACT_METHOD_ACTION)  ) {
				// Whoops, unknown action! Bail.
				Log.e(TAG, "ERR000J6 Unspecified or unrecognized action.");
				ErrorUtil.handleExceptionFinish("ERR000J6", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
				return;			
			}

			long taskId = intent.getLongExtra(EXTRA_TASK_ID, Constants.DEFAULT_NON_ID);
			if(taskId == Constants.DEFAULT_NON_ID){
				// Whoops, unknown action! Bail.
				Log.e(TAG, "ERR000J7");
				ErrorUtil.handleExceptionFinish("ERR000J7", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
				return;			
			}
			//Log.d(TAG, "taskId == " + taskId);
			
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
			Cursor mContactMethodWithNearminderCursor = getContentResolver().query(Task.ProximityAlerts.CONTENT_URI, new String[]{Task.ProximityAlerts._SELECTED_URI}, Task.ProximityAlerts._ID + " IN (" + sb + ") AND " + Task.ProximityAlerts._SELECTED_URI + " LIKE " + DatabaseUtils.sqlEscapeString("content://com.android.contacts/data/%"), null, null); 
			
			assert null != mContactMethodWithNearminderCursor;
			final HashSet<String> postalContactDataReferencedByNearminder = new HashSet<String>();
			while(mContactMethodWithNearminderCursor.moveToNext()){
				postalContactDataReferencedByNearminder.add(mContactMethodWithNearminderCursor.getString(0).substring(mContactMethodWithNearminderCursor.getString(0).lastIndexOf('/')+1));
			}
			mContactMethodWithNearminderCursor.close();
			
			// TODO: These filters assume that contact method attachments indicate that a proximity alert exists. The could should instead joing agains the proximity alert table.
			String[] SELECTION_ARGS = new String[]{contactUri.getLastPathSegment(), "vnd.android.cursor.item/postal-address_v2"}; // StructuredPostal.CONTENT_ITEM_TYPE
			
			
			Cursor mContactMethodPostalCursor = getContentResolver().query(Uri.parse("content://com.android.contacts/data"),
					ContactMethodProjectionGps.CONTACT_CONTRACT_DATA_PROJECTION,
			          "contact_id=?" + " AND " // ContactsContract.Data.CONTACT_ID
			                  + "mimetype=?", // ContactsContract.Data.MIMETYPE, 
			                  SELECTION_ARGS, null);		
			assert null != mContactMethodPostalCursor;
			
			
			if( 0 == mContactMethodPostalCursor.getCount()){ 
				// ***********************
				// Zero postal addresses
				// ***********************
				
				// No postal addresses exist, so no GPS locations can exist, right? 
				mContactMethodPostalCursor.close();
				Log.e(TAG, "ERR000JC No postal addresses exist.");
				ErrorUtil.handle("ERR000JC", "No postal addresses exist.", this); // TODO: Consider whether this should be an error.
				setResult(RESULT_CANCELED);
				finish();
				return;			
//			}else if(1 == mContactMethodPostalCursor.getCount()){
//				// ******************************************************
//				// A single postal address exists
//				// ******************************************************				
//				
//				if( !mContactMethodPostalCursor.moveToFirst()){
//					Log.e(TAG, "No postal address exists (despite the fact that cursor said so).");
//					setResult(RESULT_CANCELED);
//					finish();
//					return;		
//				}
//
//				Long contactMethodId = mContactMethodPostalCursor.getLong(ContactMethodProjectionGps.CONTACT_CONTRACT_DATA_ID_INDEX);		
//				
//				mContactMethodPostalCursor.close();
//				
//				Uri returnData = constructReturnData(personId, contactMethodId);
//				
//				
//		        setResult(RESULT_OK, new Intent().setData(returnData));
//		        finish();
//		        return;
			}

			// ******************************************************
			// Multiple postal addresses exists
			// ******************************************************
			
			// There is more than one postal address, so we need to resolve which address the user want's to use.
            
			addressSelectionAdapter = new ViewTypeCursorAdapter(this,
					R.layout.contact_geocoder_address_list_item,
					mContactMethodPostalCursor,
					new String[] { "" , "data1", "data1", ContactMethods._ID },
					new int[] { R.id.address_list_dialog_item_text1 , R.id.address_list_dialog_item_text2 , R.id.address_list_dialog_item_text3, R.id.address_list_dialog_item_proximity_icon },
					new ViewTypeCursorAdapter.FieldType[] { ViewTypeCursorAdapter.FieldType.CURSOR , ViewTypeCursorAdapter.FieldType.STRING , ViewTypeCursorAdapter.FieldType.STRING, ViewTypeCursorAdapter.FieldType.LONG  }) {

				public void initView(View view, Context context, Cursor cursor){
					super.initView(view, context, cursor);
					try{
						Long value = cursor.getLong(ContactMethodProjectionGps.CONTACT_CONTRACT_DATA_ID_INDEX); 
						view.setTag(value);
						//Log.d(TAG, "rowId == " + cursor.getPosition() + ", "+view+".setTag("+value+")");						
					}catch(HandledException h){ // Ignore.
					}catch(Exception exp){
						Log.e(TAG, "ERR000JD", exp);
						ErrorUtil.handleExceptionFinish("ERR000JD", exp, SelectPostalContactMethodActivitySDK5.this);
					}
				}
		
			    public void setViewData(View view, long value, int rowId){
			    	try{
			    		if( view.getId() == R.id.address_list_dialog_item_proximity_icon){
			    			ImageView iv = (ImageView)view;
			    			if( postalContactDataReferencedByNearminder.contains(String.valueOf(value)) ){
			    				iv.setVisibility(View.VISIBLE);
			    			}
			    		}else{
			    			super.setViewData(view, value, rowId);
			    		}
			    	}catch(HandledException h){ // Ignore.
			    	}catch(Exception exp){
			    		Log.e(TAG, "ERR000JE", exp);
			    		ErrorUtil.handleExceptionFinish("ERR000JE", exp, SelectPostalContactMethodActivitySDK5.this);
			    	}
			    }	
			    		    
			    public void setViewData(View view, Cursor cursor, int rowId){
			    	try{
			    		if( view.getId() == R.id.address_list_dialog_item_text1){
			    			TextView tv = (TextView)view;
			    			CharSequence label = determineContactMethodLabel(SelectPostalContactMethodActivitySDK5.this, cursor.getInt(ContactMethodProjectionGps.CONTACT_CONTRACT_DATA_TYPE_INDEX), cursor.getString(ContactMethodProjectionGps.CONTACT_CONTRACT_DATA_LABEL_INDEX));
			    						    			
			    			tv.setText(label);
			    		}else{
			    			super.setViewData(view, cursor, rowId);
			    		}
			    	}catch(HandledException h){ // Ignore.
			    	}catch(Exception exp){
			    		Log.e(TAG, "ERR000JF", exp);
			    		ErrorUtil.handleExceptionFinish("ERR000JF", exp, SelectPostalContactMethodActivitySDK5.this);
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
			    		Log.e(TAG, "ERR000JG", exp);
			    		ErrorUtil.handleExceptionFinish("ERR000JG", exp, SelectPostalContactMethodActivitySDK5.this);
			    	}
			    }
			};		
			
            new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_chooseAddress)
            .setAdapter(addressSelectionAdapter, new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					try{
						
						long postalContactDataId = addressSelectionAdapter.getItemId(which);
						//Log.d(TAG, "postalContactDataId == " + postalContactDataId);
						
						Uri returnData = constructReturnData(postalContactDataId);
						
						setResult(RESULT_OK, new Intent().setData(returnData));
						finish();
					}catch(HandledException h){ // Ignore.
					}catch(Exception exp){
						Log.e(TAG, "ERR000JH", exp);
						ErrorUtil.handleExceptionFinish("ERR000JH", exp, SelectPostalContactMethodActivitySDK5.this);
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
			Log.e(TAG, "ERR000JI", exp);
			ErrorUtil.handleExceptionFinish("ERR000JI", exp, this);
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

	public static CharSequence determineContactMethodLabel(Context context, int type, String label) {
		try {
			Class struturedPostalClass = LocationUtil.findContactsContractsCommonDataKindsStruturedPostalClass();
			Method getTypeLabelMethod = struturedPostalClass.getMethod("getTypeLabel", Resources.class, int.class, CharSequence.class);
			if( null != getTypeLabelMethod ){
				return (CharSequence)getTypeLabelMethod.invoke(null, context.getResources(), type, label);
			}else{
				Log.e(TAG, "ERR000JQ");
				ErrorUtil.handle("ERR000JQ", "No name found", context);
				return "";
			}
		} catch (Exception e) {
			Log.e(TAG, "ERR000JP");
			ErrorUtil.handle("ERR000JP", "No name found", context);
			return "";
		}
	}
	
	protected Uri constructReturnData(Long contactMethodId){
		Builder builder = ContentUris.appendId(Uri.parse("content://com.android.contacts/data").buildUpon(), contactMethodId);
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

	public static Intent getIntent(Context context, Uri contactUri, long taskId){
		// Create Intent
		Intent selectPostalContactMethodIntent = new Intent(Constants.PICK_POSTAL_CONTACT_METHOD_ACTION, contactUri, context, SelectPostalContactMethodActivitySDK5.class); 
		selectPostalContactMethodIntent.putExtra(EXTRA_TASK_ID, taskId);
		return selectPostalContactMethodIntent;
	}
	
}
