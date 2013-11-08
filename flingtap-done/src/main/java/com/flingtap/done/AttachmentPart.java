// Licensed under the Apache License, Version 2.0

package com.flingtap.done;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flingtap.done.provider.Task.TaskAttachments;
import com.flingtap.done.provider.Task.Tasks;
import com.flingtap.done.util.Constants;
import com.flurry.android.FlurryAgent;
import com.flingtap.done.base.R;
import com.google.android.maps.MyLocationOverlay;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.provider.Contacts;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Handles all add and remove attachment operations.
 * 
 * 
 * 
 * TODO: !! ContentUris.java (from Android) uses long values,, but currently, FlingTap Done only uses int values for _ID,, so maybe it would be more efficient to write my own ContentUris.
 * TODO: Why is this a participant? And specifically a "Context" participant.
 */
public class AttachmentPart extends AbstractContextActivityParticipant {

	private final static String TAG = "AttachmentPart";

	protected final static int FIRST_CODE_ID 	  = 2100;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}	
	
	private Context mContext = null;
	
	public AttachmentPart(Context context){
		assert null != context;
		mContext = context;
	}

	public Uri addAttachment(long taskId, Intent attachIntent, String name, Bitmap icon, Intent.ShortcutIconResource iconResource, Intent deleteIntent){
		
		assert taskId >= 0;
		assert null != attachIntent;
		
		ContentValues cv = new ContentValues();
	
		Long now = Long.valueOf(System.currentTimeMillis());
		
		Uri attachmentUri = attachIntent.getData();
		
		if( null != attachmentUri ){
			cv.put(com.flingtap.done.provider.Task.TaskAttachments._URI, attachmentUri.toString()); 
		}
		ComponentName componentName = attachIntent.getComponent();
		if( null != componentName ){
			cv.put(com.flingtap.done.provider.Task.TaskAttachments._PACKAGE, componentName.getPackageName());
			cv.put(com.flingtap.done.provider.Task.TaskAttachments._CLASS_NAME, componentName.getClassName());			
		}
		
		assert null != attachmentUri || null != componentName;
		if( null == attachmentUri && null == componentName ){
			Exception exp = (Exception)(new Exception("Failed to find attachment in DB.").fillInStackTrace());
			Log.e(TAG, "ERR0000Z Failed to add attachement. " + (null==attachmentUri?"attachment is null. ":"") + (null != componentName ? " componentName is null.": "") + " taskId==" + taskId, exp);
			ErrorUtil.handleExceptionNotifyUserAndThrow("ERR0000Z", exp, mContext);			
		}
		
		cv.put(com.flingtap.done.provider.Task.TaskAttachments.TASK_ID, taskId);
		cv.put(com.flingtap.done.provider.Task.TaskAttachments.NAME, name);
		
		// ************
		// Add intent
		// ************
		String intentString = flattenIntent(attachIntent);
		cv.put(com.flingtap.done.provider.Task.TaskAttachments._INTENT, intentString);

		if( icon != null ){
			// Add icon
			ByteArrayOutputStream iconBytes = new ByteArrayOutputStream();
			icon.compress(Bitmap.CompressFormat.PNG, 0, iconBytes);
			cv.put(com.flingtap.done.provider.Task.TaskAttachments._ICON, iconBytes.toByteArray());
			
		}else if (null != iconResource){
			// Add icon resource
			// TODO: ! Shouldn't I also store iconResource.packageName ? 
			cv.put(com.flingtap.done.provider.Task.TaskAttachments._ICON_RESOURCE, iconResource.resourceName);
		}
		if( null != deleteIntent ){
			String deleteIntentBytes = flattenIntent(deleteIntent);
			cv.put(com.flingtap.done.provider.Task.TaskAttachments._DELETE_INTENT, deleteIntentBytes);
		}

		
		Uri newContentUri = null;		
		try{
			newContentUri = mContext.getContentResolver().insert(Task.TaskAttachments.CONTENT_URI, cv);
		}catch(SQLException iae){ // TODO: !! Better solution is to just check for the record before you add it.
			Log.e(TAG, "Failed to add attachement. " + (null!=attachmentUri?"attachment == " + attachmentUri.toString():"") + " taskId==" + taskId + " Maybe duplicate?", iae);
			Toast.makeText(mContext, "Duplicate attachment.", Toast.LENGTH_SHORT).show(); // TODO: !! i18n this line.
			throw new HandledException();
		}
		assert null != newContentUri;

		// Prepare event info.
		Map<String,String> parameters = Event.prepareIntentParams(mContext, new HashMap<String,String>(), attachIntent);
		Event.onEvent(Event.ADD_ATTACHMENT, parameters); 
		
		return newContentUri;
	}

	private static Rect calcViewBounds(View view){
		int[] pos = new int[2];
		view.getLocationOnScreen(pos);
		return new Rect(pos[0], pos[1], pos[0] + view.getWidth(), pos[1] + view.getHeight());
	}
	
	public static void launchAttachDefaultAction(Context context, Uri attachUri, View view) {
		Cursor launchActivityCursor = context.getContentResolver().query(attachUri, new String[]{Task.TaskAttachments._INTENT}, null, null, null);
		assert null != launchActivityCursor;
		assert 1 == launchActivityCursor.getCount();
		if( !launchActivityCursor.moveToFirst() ){
			launchActivityCursor.close();
			Exception exp = (Exception)(new Exception("Failed to find attachment in DB.").fillInStackTrace());
			Log.e(TAG, "ERR0000U Failed to find attachment in DB.", exp);
			ErrorUtil.handleExceptionAndThrow("ERR0000U", exp, context);
		}
		String intentString = launchActivityCursor.getString(0);
		launchActivityCursor.close();
		
		Intent launchIntent = AttachmentPart.expandIntent(intentString, context);
		if( null == launchIntent ){ 
			return;// If null then an error occurred in AttachmentPart.deserializeIntent(..) and was handled there. No need to do anything here except get out.
		}

		final int sdkVersion = Integer.parseInt( Build.VERSION.SDK ); // Build.VERSION.SDK_INT was introduced after API level 3 and so is not compatible with 1.5 devices.
		if( 7 <= sdkVersion ){ // Android 2.1 and above.		
			Class<Intent> intentClass = Intent.class;
			try{
				Method setSourceBounds = intentClass.getMethod("setSourceBounds", Rect.class);
				setSourceBounds.invoke(launchIntent, AttachmentPart.calcViewBounds(view));
			}catch(Exception e){
				Log.e(TAG, "ERR000JU", e);
				ErrorUtil.handleException("ERR000JU", e, context);
			}
		}		
				
		// Prepare event info.
		Map<String,String> parameters = Event.prepareIntentParams(context, new HashMap<String,String>(), launchIntent);
		Event.onEvent(Event.LAUNCH_ATTACHMENT, parameters); 
		
		launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK ); // This makes it safe.
		context.startActivity(launchIntent);
	}    	

	/**
	 *  
	 */
	public static String flattenIntent(Intent attachIntent) {
        return attachIntent.toURI();
	}
	
	/**
	 *  
	 */
	public static Intent expandIntent(String attachIntentString, Context context) {
        try {
			return Intent.getIntent(attachIntentString);
		} catch (URISyntaxException exp) {
    		Log.e(TAG, "ERR000G1", exp);
    		ErrorUtil.handleException("ERR000G1", exp, context);
		}
		return null;
	}

	/**
	 * 
	 */
	public static void removeAttachmentImmediately(Context context, Uri attachUri) {
		Intent intent = fetchAttachmentIntent(context, attachUri);
		if( null != intent ){ 
			// Prepare event info.
			Map<String,String> parameters = Event.prepareIntentParams(context, new HashMap<String,String>(), intent);
			Event.onEvent(Event.REMOVE_ATTACHMENT, parameters); 
		}

		long rowId = context.getContentResolver().delete(attachUri, null, null);	
		if (rowId == 0) {
			// Failed to delete the attachment.
			Exception exp = (Exception)(new Exception("Failed to delete the attachment in DB.").fillInStackTrace());
			Log.e(TAG, "ERR00013 Failed to delete the attachment in DB.", exp);
			ErrorUtil.handleExceptionNotifyUserAndThrow("ERR00013", exp, context);
		}
	}


	private static Intent fetchAttachmentIntent(Context context, Uri attachUri) {
		Cursor intentCursor = context.getContentResolver().query(attachUri, new String[]{Task.TaskAttachments._INTENT}, null, null, null);
		assert null != intentCursor;
		assert 1 == intentCursor.getCount();
		if( !intentCursor.moveToFirst() ){
			intentCursor.close();
			Exception exp = (Exception)(new Exception("Failed to find intent.").fillInStackTrace());
			Log.e(TAG, "ERR00016 Failed to find intent.", exp);
			ErrorUtil.handleExceptionAndThrow("ERR00016", exp, context);
		}
		String intentString = intentCursor.getString(0);
		intentCursor.close();
		
		Intent intent = AttachmentPart.expandIntent(intentString, context);
		return intent;
	}
	
	public static void renameDisplayName(Context context, CharSequence newText, Uri attachmentUri) {  
		assert newText.length() > 0;
		
		try{
			// Get Attachment Intent.
			String intentString = queryIntentBytes(context, attachmentUri);
			
			Intent attachIntent = AttachmentPart.expandIntent(intentString, context);
			if( null == attachIntent ){ 
				Exception exp = (Exception)(new Exception("Failed to find attachment intent.").fillInStackTrace());
				Log.e(TAG, "ERR00015", exp);
				ErrorUtil.handleExceptionNotifyUserAndThrow("ERR00015", exp, context);
			}
			// Prepare event info.
			Map<String,String> parameters = Event.prepareIntentParams(context, new HashMap<String,String>(), attachIntent);
			Event.onEvent(Event.RENAME_ATTACHMENT, parameters); 
			
			ContentValues cv = new ContentValues(1);
			cv.put(Task.TaskAttachments.NAME, newText.toString());
			int updateCount = context.getContentResolver().update(
					attachmentUri,
					cv, 
					null, 
					null);				
			assert 1 == updateCount;
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000B1", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000B1", exp, context);
		}
		
	}

	private static String queryIntentBytes(final Context context, Uri attachmentUri) {
		Cursor intentByteArrayCursor = context.getContentResolver().query(
				attachmentUri, 
				new String[]{Task.TaskAttachments._INTENT}, 
				null, 
				null, 
				null);
		assert null != intentByteArrayCursor;
		assert 1 == intentByteArrayCursor.getCount();
		if( !intentByteArrayCursor.moveToFirst() ){
			intentByteArrayCursor.close();
			
			Exception exp = (Exception)(new Exception("Attachment not found in DB. attachmentUri == " + attachmentUri).fillInStackTrace());
			Log.e(TAG, "ERR000B4 Attachment not found. in DB.", exp);
			ErrorUtil.handleExceptionNotifyUserAndThrow("ERR000B4", exp, context);
		}
		final String intentString = intentByteArrayCursor.getString(0);
		intentByteArrayCursor.close();	
		
		return intentString;
	}
	
	private static String queryDisplayName(final Context context, Uri attachmentUri) {  
		Cursor renameCursor = context.getContentResolver().query(
				attachmentUri, 
				new String[]{Task.TaskAttachments.NAME}, 
				null, 
				null, 
				null);
		assert null != renameCursor;
		assert 1 == renameCursor.getCount();
		if( !renameCursor.moveToFirst() ){
			renameCursor.close();
			
			Exception exp = (Exception)(new Exception("Attachment not found in DB. attachmentUri == " + attachmentUri).fillInStackTrace());
			Log.e(TAG, "ERR000B0 Attachment not found. in DB.", exp);
			ErrorUtil.handleExceptionNotifyUserAndThrow("ERR000B0", exp, context);
		}
		String name = renameCursor.getString(0);
		renameCursor.close();
		return name;
	}

	public static class RenameOnTextSetListener implements TextEntryDialog.OnTextSetListener {

		private Uri mAttachmentUri = null;
		public void setAttachmentUri(Uri attachmentUri) {
			mAttachmentUri = attachmentUri;
			assert null != mAttachmentUri;
		}

		private Activity mActivity = null;
		
		public RenameOnTextSetListener(Activity activity, Uri attachmentUri){
			mActivity = activity;
			assert null != mActivity;
			
			mAttachmentUri = attachmentUri;
			assert null != mAttachmentUri;
		}
		
		public void onTextSet(CharSequence newText) {
			try{
				if( newText.length() == 0 ){ 
					Toast.makeText(mActivity, R.string.toast_pleaseEnterAName, Toast.LENGTH_SHORT).show(); 
					return;
				}
				
				renameDisplayName(mActivity, newText, mAttachmentUri);
				
			}catch(HandledException h){ // Ignore.	
			}catch(Exception exp){
				Log.e(TAG, "ERR000AZ", exp);
				ErrorUtil.handleException("ERR000AZ", exp, mActivity);
			}
		}
		
		public void onCancel() {
			// Do nothing.
			try{
				// How is this condition even possible? 
				Toast.makeText(mActivity, R.string.toast_operationCanceled , Toast.LENGTH_SHORT).show();
				Log.e(TAG, "User cancelled the content namming part.");
			}catch(HandledException h){ // Ignore.
			}catch(Exception exp){
				Log.e(TAG, "ERR000B2", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR000B2", exp, mActivity);
			}

			return;
		}

	}

	public static Dialog onCreateDialogRenameDialog(final Activity mContext, int dialogId, final Uri attachmentUri, RenameOnTextSetListener listener) {
    	Dialog dialog = null;
    	try{
    		String displayName = queryDisplayName(mContext, attachmentUri);
    		
    		dialog = TextEntryDialog.onCreateDialog(mContext, listener, mContext.getText(R.string.dialog_setAttachmentName), null, displayName);
    		
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000AY", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR000AY", exp, mContext);
    	}

        return dialog;
	}

    public static void onPrepareDialogRenameDialog(final Activity activity, Dialog dialog, Uri attachmentUri){
    	try{
    		String displayName = queryDisplayName(activity, attachmentUri);
    		
    		TextEntryDialog.onPrepareDialog(activity, dialog, displayName);
    		
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000B3", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR000B3", exp, activity);
    	}
    }

	
	public boolean hasInstanceState(){
		return false;
	}
	
	public void  onSaveInstanceState  (Bundle outState){
	}
	
	public void  onRestoreInstanceState  (Bundle savedInstanceState){
	}

	
}


