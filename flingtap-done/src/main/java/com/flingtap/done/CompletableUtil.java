// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.net.URISyntaxException;

import com.flingtap.done.provider.Task;
import com.flingtap.done.util.Constants;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.flingtap.done.base.R;

/**
 *
 * TODO: !!! This should be refactored into a single AttachmentUtil object.
 * TODO: !!! Add cleanup thread for orphand completables.
 */
public class CompletableUtil {
	private static final String TAG = "CompletableUtil";

	public static boolean delete(final Activity context, final Uri attachUri) {
		if( deleteNoToast(context, attachUri) ){
			Toast.makeText(context, R.string.toast_completableDeleted, Toast.LENGTH_SHORT).show();
			return true;
		}else{
			// Let the caller handle the error condition.
			return false;
		}

	}
	
	/**
	 * 
	 * @param context
	 * @param attachUri
	 * @return
	 */
	public static boolean deleteNoToast(Activity context, Uri attachUri) {
		
		// Event.
		Event.onEvent(Event.DELETE_COMPLETABLE);

// TODO: !!! Why doesn't this code work reliably? Sometimes the activity (refered to by the _delete_intent) is called, but sometimes not.
//           --> I added the fkd_tasks_completable_id trigger to do the delete for now. Nearminders get deleted the same way (fkd_tasks_proximity_id).  
//           --> This issues seems to affect TaskProvider.sendDeleteIntents(..) as well. 
//           --> I've commented the code for _adding_  _delete_intent for now in CompletableEditor and TaskAttachmentListTab.onClick(..). 
		
//		try{
//			// ***************************************************
//			// Delete the completable db record
//			// ***************************************************
//			Cursor deleteAttachIntentCursor = context.getContentResolver().query(attachUri, new String[]{Task.TaskAttachments._DELETE_INTENT}, null, null, null);
//			assert null != deleteAttachIntentCursor;
//			if( deleteAttachIntentCursor.moveToFirst() ){
//				if( !deleteAttachIntentCursor.isNull(0) ){
//					String deleteIntentString = deleteAttachIntentCursor.getString(0);
//					try {
////						Activity parent = context.getParent();
////						if( null != parent ){
//							Intent deleteIntent = Intent.getIntent(deleteIntentString);
////						context.startActivity(deleteIntent); // Just let it go. Who knows what this intent will result in.
//							context.startActivityForResult(deleteIntent, Constants.DEFAULT_NON_REQUEST_ID); 
////						context.sendBroadcast(deleteIntent); // Just let it go. Who knows what this intent will result in.
//							
////						}
//					} catch (URISyntaxException exp) {
//						Log.e(TAG, "ERR000FI", exp);
//						ErrorUtil.handleExceptionNotifyUser("ERR000FI", exp, context);
//					}
//					
//				}
//			}
//			deleteAttachIntentCursor.close();
//			
//		}finally{
			// ***************************************************
			// Delete the attachment db record
			// ***************************************************
			if( 1 != context.getContentResolver().delete(attachUri, null, null) ){  
				Log.e(TAG, "ERR000FE Failed to delete db record.");
				ErrorUtil.handle("ERR000FE", "Failed to delete db record. uri="+attachUri, null);
				return false;
			}
//		}
		
//		// ***************************************************
//		// Delete the completable db record
//		//
//		// NOTE: attachment record will automatically be deleted by db trigger.
//		// ***************************************************
//		Cursor deleteAttachIntentCursor = context.getContentResolver().query(attachUri, new String[]{Task.TaskAttachments._URI}, null, null, null);
//		assert null != deleteAttachIntentCursor;
//		if( deleteAttachIntentCursor.moveToFirst() ){
//			String completableUriString = deleteAttachIntentCursor.getString(0);
//			Uri completableUri = Uri.parse(completableUriString);
//			if( 1 != context.getContentResolver().delete(completableUri, null, null) ){  
//				Log.e(TAG, "ERR000FN Failed to delete db record.");
//				ErrorUtil.handle("ERR000FN", "Failed to delete db record. uri="+completableUri, null);
//				return false;
//			}
//		}
//		deleteAttachIntentCursor.close();		
		
		return true;
		
	}
	
	public static boolean updateCompletableText(Activity activity, String text, Uri uri) {
		// Event.
		Event.onEvent(Event.EDIT_COMPLETABLE);
		
		ContentValues cv = new ContentValues();
		cv.put(Task.Completable.TEXT_CONTENT, text);
		int count = activity.getContentResolver().update(uri, cv, null, null);
		if( 1 != count ){
			Log.e(TAG, "ERR000G3" );
			ErrorUtil.handleExceptionNotifyUser("ERR000G3", (Exception)(new Exception(  uri.toString() )).fillInStackTrace(), activity);
			return false;
		}
		return true;
	}
	
}
