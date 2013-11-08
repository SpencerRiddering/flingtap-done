// Licensed under the Apache License, Version 2.0

package com.flingtap.done;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.widget.Toast;
import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;
import com.flingtap.done.util.Constants;
import com.flingtap.done.util.LicenseUtil;

/**
 * Consolidates all methods for managing user applied labels.
 *
 * @author spencer
 *
 */
public class LabelUtil {
	
	private static final String TAG = "LabelUtil";
	
	private static final String COLUMN_ALIAS_THE_LABEL_COLUMN = "theLabelColumn";
	private static final String COLUMN_ALIAS_THE_IS_CHECKED_COLUMN = "theIsCheckedColumn";

	protected static final String SAVE_DISPLAY_TEXT_KEY 	= "LabelUtil.SAVE_DISPLAY_TEXT_KEY";
	protected static final String SAVE_ID_KEY 				= "LabelUtil.SAVE_ID_KEY";
	protected static final String SAVE_APPLIED_ID_KEY 		= "LabelUtil.SAVE_APPLIED_ID_KEY";
	protected static final String SAVE_IS_LABEL_CHECKED_KEY = "LabelUtil.SAVE_IS_LABEL_CHECKED_KEY";
	protected static final String SAVE_BOOL_ARRAY_KEYS_KEY 		= "LabelUtil.SAVE_BOOL_ARRAY_KEYS_KEY";
	protected static final String SAVE_BOOL_ARRAY_VALUES_KEY 		= "LabelUtil.SAVE_BOOL_ARRAY_VALUES_KEY";
	
	
	public static ApplyLabelsOnClickListener createApplyLabelsOnClickListener(Activity activity, long taskId){
		return new ApplyLabelsOnClickListener(activity, taskId);
	}

	public static void onSaveInstanceStateApplyLabel(Bundle outState, ApplyLabelsOnClickListener clickListener) {
		//Log.v(TAG, "onSaveInstanceState(..) called.");	
		outState.putLongArray(		SAVE_ID_KEY, 				clickListener.labelsDataId);
		outState.putLongArray(		SAVE_APPLIED_ID_KEY, 		clickListener.labelsDataAppliedId);
		outState.putStringArray(	SAVE_DISPLAY_TEXT_KEY,		clickListener.labelsDataDisplayText);
		outState.putBooleanArray(	SAVE_IS_LABEL_CHECKED_KEY, 	clickListener.labelsDataIsChecked);
		int[] keys = new int[clickListener.boolArray.size()];
		boolean[] values = new boolean[keys.length];
		for(int i=0; i < keys.length; i++ ){
			keys[i] = clickListener.boolArray.keyAt(i);
			values[i] = clickListener.boolArray.valueAt(i);
		}
		outState.putIntArray(		SAVE_BOOL_ARRAY_KEYS_KEY, 	keys);
		outState.putBooleanArray(	SAVE_BOOL_ARRAY_VALUES_KEY, values);
	}

    public static void onRestoreInstanceStateApplyLabel(Bundle savedInstanceState, ApplyLabelsOnClickListener clickListener) {
		//Log.v(TAG, "onRestoreInstanceStateApplyLabel(..) called.");	
    	clickListener.labelsDataId 			= savedInstanceState.getLongArray(		SAVE_ID_KEY);
    	clickListener.labelsDataAppliedId 	= savedInstanceState.getLongArray(		SAVE_APPLIED_ID_KEY);	
    	clickListener.labelsDataDisplayText = savedInstanceState.getStringArray(	SAVE_DISPLAY_TEXT_KEY);
    	clickListener.labelsDataIsChecked 	= savedInstanceState.getBooleanArray(	SAVE_IS_LABEL_CHECKED_KEY);
    	
		int[] keys 			= savedInstanceState.getIntArray(		SAVE_BOOL_ARRAY_KEYS_KEY);
		boolean[] values 	= savedInstanceState.getBooleanArray(	SAVE_BOOL_ARRAY_VALUES_KEY);
		for(int i=0; i < keys.length; i++ ){
			clickListener.boolArray.put(keys[i], values[i]);
		}
    }
	
    public static void loadApplyLabelDialogData(Activity activity, ApplyLabelsOnClickListener clickListener, long taskId){
		final Cursor cursor = queryAppliedLabels(activity, taskId);

		// ********** workaround for Android issue 2998 ****************
		clickListener.initState(cursor.getCount());

		for(int i=0; i < cursor.getCount(); i++){
			cursor.moveToNext();

			clickListener.labelsDataId[i] = cursor.getLong(0);
			clickListener.labelsDataAppliedId[i] = cursor.getLong(1);
			clickListener.labelsDataDisplayText[i] = cursor.getString(3);
			clickListener.labelsDataIsChecked[i] = 0!=cursor.getInt(2);    			
		}
		cursor.close();
		// ********** workaround ****************
    }
    public static Dialog onCreateDialogApplyLabel(final Activity activity, ApplyLabelsOnClickListener clickListener, long taskId, final int dialogId){
    	AlertDialog dialog = null;
    	try{
    		//Log.v(TAG, "onCreateDialogApplyLabel(..) called. " + taskId);

            dialog = new AlertDialog.Builder(activity)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.dialog_addRemoveLabels)
            .setMultiChoiceItems(clickListener.labelsDataDisplayText, clickListener.labelsDataIsChecked, clickListener) // TODO: !! Using cursor causes this method to not function. Hack workaround begins above. See: http://code.google.com/p/android/issues/detail?id=2998
            .setPositiveButton(R.string.button_ok, clickListener)
            .setNegativeButton(R.string.button_cancel, clickListener )
            .setOnCancelListener(clickListener)
            .create();
    		
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener(){
				public void onDismiss(DialogInterface dialog) {
					activity.removeDialog(dialogId);
				}
            });

    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000AN", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR000AN", exp, activity);
    	}

        return dialog;
    		
    }

    public static void onPrepareDialogApplyLabel(final Activity activity, Dialog dialog, long taskId){
    }
    
	public static class ApplyLabelsOnClickListener 
			implements DialogInterface.OnMultiChoiceClickListener, DialogInterface.OnClickListener, DialogInterface.OnCancelListener{
		
		private Activity mContext = null;
    	final SparseBooleanArray boolArray = new SparseBooleanArray();
    	private long mTaskId = Constants.DEFAULT_NON_ID;
		
    	String[] labelsDataDisplayText = null;
    	long[] labelsDataId            = null;
    	long[] labelsDataAppliedId     = null;
    	boolean[] labelsDataIsChecked  = null;
    	
    	public void setTaskId(long taskId) {
			mTaskId = taskId;
		}
    	public long getTaskId() {
			return mTaskId;
    	}

		private ApplyLabelsOnClickListener(Activity context, long taskId){
			mContext = context;
			assert null != mContext;
			
			mTaskId = taskId;
			assert Constants.DEFAULT_NON_ID != mTaskId;
		}

        public void onClick(DialogInterface dialog, int which) {
            try {
                if (DialogInterface.BUTTON_NEGATIVE == which) {
                    handleCancel(dialog);
                    // Do nothing.

                } else if (DialogInterface.BUTTON_POSITIVE == which) {
                    // Prepare event info.
                    Event.onEvent(Event.EDIT_TASK_LABELS, null);

                    ContentValues cv = new ContentValues();
                    Uri insertUri = null;
                    for (int i = 0; i < boolArray.size(); i++) {
                        Cursor recordExistsCursor = mContext.getContentResolver().query(
                                Task.LabeledContent.CONTENT_URI,
                                new String[]{},
                                Task.LabeledContentColumns._LABEL_ID + "=? AND " + Task.LabeledContentColumns._CONTENT_URI + "=?",
                                new String[]{String.valueOf(labelsDataId[boolArray.keyAt(i)]), ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, mTaskId).toString()},
                                null);

                        if (recordExistsCursor.getCount() > 0) { // Need to remove the the LabeledContent record.
                            if (boolArray.valueAt(i) == false) {
                                int deleteCount = mContext.getContentResolver().delete(Task.LabeledContent.CONTENT_URI, Task.LabeledContent._ID + "=?", new String[]{String.valueOf(labelsDataAppliedId[boolArray.keyAt(i)])});

                                if (deleteCount != 1) {// TODO: Handle this error.
                                    Toast.makeText(mContext, R.string.toast_failedToRemoveLabel, Toast.LENGTH_LONG).show();
                                }
                            }
                        } else { // Need to insert a new LabeledContent record.
                            if (boolArray.valueAt(i) == true) {
                                cv.clear();
                                cv.put(Task.LabeledContentColumns._LABEL_ID, labelsDataId[boolArray.keyAt(i)]);
                                cv.put(Task.LabeledContentColumns._CONTENT_URI, ContentUris.withAppendedId(Task.Tasks.CONTENT_URI, mTaskId).toString());
                                insertUri = mContext.getContentResolver().insert(Task.LabeledContent.CONTENT_URI, cv);
                                if (null == insertUri) { // TODO: !!!! Handle this error.
                                    Toast.makeText(mContext, R.string.toast_failedToAddNewLabel, Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                        recordExistsCursor.close();
                    }

                    // Update the filter bits.
                    FilterUtil.applyFilterBits(mContext);

                    clearState();
                }
                // HandledException is handled elsewhere --> }catch(HandledException h){ // Ignore.
            } catch (Exception exp) {
                Log.e(TAG, "ERR0002L", exp);
                ErrorUtil.handleExceptionNotifyUser("ERR0002L", exp, mContext);
            }
        }

        private void handleCancel(DialogInterface dialog) {
			clearState();
		}
		
		public void initState(int size){
			this.labelsDataDisplayText = new String[size];
			this.labelsDataId          = new long[size];
			this.labelsDataAppliedId   = new long[size];
			this.labelsDataIsChecked  	= new boolean[size];
			boolArray.clear();
		}

		private void clearState() {
			labelsDataDisplayText = null;
			labelsDataId 		  = null;
			labelsDataAppliedId   = null;
	    	labelsDataIsChecked   = null;
	    	mTaskId = Constants.DEFAULT_NON_ID;
		}

		
		public void onCancel(DialogInterface dialog) {
			handleCancel(dialog);
		}

		public void onClick(DialogInterface dialog, int listItemPosition, boolean isChecked) {
			boolArray.put(listItemPosition, isChecked);
		}

	}

	private static Cursor queryAppliedLabels(final Context context, final long taskId) {
		final Cursor cursor = context.getContentResolver().query(
    			Task.Labels.CONTENT_URI, 
    			new String[]{
    					Task.Labels._ID, 
    					"(SELECT "+TaskProvider.LABELED_CONTENT_TABLE_NAME+"."+ Task.LabeledContent._ID +" FROM "+TaskProvider.LABELED_CONTENT_TABLE_NAME+" WHERE "+TaskProvider.LABELED_CONTENT_TABLE_NAME+"."+Task.LabeledContentColumns._LABEL_ID+"="+TaskProvider.LABELS_TABLE_NAME+"."+Task.LabelsColumns._ID+" AND "+Task.LabeledContentColumns._CONTENT_URI+"='"+ContentUris.appendId(Task.Tasks.CONTENT_URI.buildUpon(), taskId)+"')",                                         // TODO: Is Task.Labels._ID needed?
    					"(SELECT COUNT(*)                                                                  FROM "+TaskProvider.LABELED_CONTENT_TABLE_NAME+" WHERE "+TaskProvider.LABELED_CONTENT_TABLE_NAME+"."+Task.LabeledContentColumns._LABEL_ID+"="+TaskProvider.LABELS_TABLE_NAME+"."+Task.LabelsColumns._ID+" AND "+Task.LabeledContentColumns._CONTENT_URI+"='"+ContentUris.appendId(Task.Tasks.CONTENT_URI.buildUpon(), taskId)+"') AS " + COLUMN_ALIAS_THE_IS_CHECKED_COLUMN, // TODO: Is Task.Labels._ID needed? 
    					Task.LabelsColumns.DISPLAY_NAME + " AS " + COLUMN_ALIAS_THE_LABEL_COLUMN}, 
    			Task.LabelsColumns._USER_APPLIED+"=?", 
    			new String[]{Task.LabelsColumns.USER_APPLIED_TRUE}, 
    			Task.LabelsColumns.DISPLAY_NAME);
		return cursor;
	}

	public static class RenameLabelOnTextSetListener implements TextEntryDialog.OnTextSetListener {

		private long mId = Constants.DEFAULT_NON_ID;
		public void setId(long id) {
			mId = id;
		}

		private Activity mActivity = null;
		
		public RenameLabelOnTextSetListener(Activity activity, long id){
			mActivity = activity;
			assert null != mActivity;
			
			mId = id;
			assert Constants.DEFAULT_NON_ID != mId;
		}
		
		public void onTextSet(CharSequence newText) {
			try{
				if( newText.length() == 0 ){ 
					Toast.makeText(mActivity, R.string.toast_pleaseEnterAName, Toast.LENGTH_SHORT).show();
					return;
				}
				renameUserAppliedLabel(mActivity, newText, mId);
			}catch(HandledException h){ // Ignore.	
			}catch(Exception exp){
				Log.e(TAG, "ERR0003D", exp);
				ErrorUtil.handleException("ERR0003D", exp, mActivity);
			}
		}
		
		public void onCancel() {
			// Do nothing.
		}		
	}
	
	public static RenameLabelOnTextSetListener createRenameLabelOnTextSetListener(final Activity activity, long id){
		return new RenameLabelOnTextSetListener(activity, id);
	}
	
    public static Dialog onCreateDialogRenameLabel(final Activity activity, final long id, TextEntryDialog.OnTextSetListener displayLabelTextSetListener){
    	Dialog dialog = null;
    	try{
    		Cursor cursor = queryLabelDisplayName(activity, id);
    		
    		dialog = TextEntryDialog.onCreateDialog(activity, displayLabelTextSetListener, activity.getText(R.string.dialog_editLabelName), null, cursor.getString(0));
    		
        	cursor.close();
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000AN", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR000AN", exp, activity);
    	}

        return dialog;
    		
    }
    public static void onPrepareDialogRenameLabel(final Activity activity, Dialog dialog, final long id){
    	try{
    		Cursor cursor = queryLabelDisplayName(activity, id);
    		
    		TextEntryDialog.onPrepareDialog(activity, dialog, cursor.getString(0));
    		
    		cursor.close();
    		
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000AO", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR000AO", exp, activity);
    	}
    }

	private static Cursor queryLabelDisplayName(final Activity activity, final long id) {
		Cursor cursor = activity.getContentResolver().query(ContentUris.withAppendedId(Task.Labels.CONTENT_URI, id), new String[]{Task.Labels.DISPLAY_NAME}, null, null, null);
    	assert null != cursor;
    	if( !cursor.moveToFirst() ){
    		cursor.close();
    		Exception exp = (Exception)(new Exception("Unable to rename label.").fillInStackTrace());    		
    		Log.e(TAG, "ERR0003C Failed to find existing label when attempting to rename it.");
    		ErrorUtil.handleExceptionNotifyUserAndThrow("ERR0003C", exp, activity);    		
    	}
		return cursor;
	}
	
	public static CreateLabelOnTextSetListener createCreateLabelOnTextSetListener(final Activity activity){
		return new CreateLabelOnTextSetListener(activity);
	}
	
    public static Dialog onCreateDialogCreateLabel(final Activity activity, TextEntryDialog.OnTextSetListener textSetListener){
    	Dialog dialog = null;
    	try{
    		
    		dialog = TextEntryDialog.onCreateDialog(activity, textSetListener, activity.getText(R.string.dialog_setLabelName), null, null);     	
    		
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000BT", exp);
    		ErrorUtil.handleException("ERR000BT", exp, activity);
    	}

        return dialog;
    		
    }
    public static void onPrepareDialogCreateLabel(final Activity activity, Dialog dialog){
    	try{
    		TextEntryDialog.onPrepareDialog(activity, dialog, null);
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000BU", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR000BU", exp, activity);
    	}
    }	
	
	public static class CreateLabelOnTextSetListener implements TextEntryDialog.OnTextSetListener {

		private Activity mActivity = null;
		
		public CreateLabelOnTextSetListener(Activity activity){
			mActivity = activity;
			assert null != mActivity;
		}
		
		public void onTextSet(CharSequence newText) {
			try{
				newText = newText.toString().trim();
				if( newText.length() == 0 ){ 
					Toast.makeText(mActivity, R.string.toast_pleaseEnterAName, Toast.LENGTH_SHORT).show();
					return;
				}
				createUserAppliedLabel(mActivity, newText);
			}catch(HandledException h){ // Ignore.	
			}catch(Exception exp){
				Log.e(TAG, "ERR000BV", exp);
				ErrorUtil.handleException("ERR000BV", exp, mActivity);
			}
		}
		
		public void onCancel() {
			// Do nothing.
		}		
	}

    
	public static void deleteUserAppliedLabel(Activity activity, long id){
    	// Prepare event info. 
    	Event.onEvent(Event.DELETE_LABEL, null); 
		if( 0 < activity.getContentResolver().delete(Uri.parse("content://"+Task.AUTHORITY+"/user_applied_label/" + id), null, null)){// TODO: !! Remove Hardcoded string
			Event.onEvent(Event.EDIT_TASK_LABELS, null); // Map<String,String> parameters = new HashMap<String,String>();
		}
	}
	

	public static Uri createUserAppliedLabel(Context context, CharSequence newText) {
		return createUserAppliedLabel(context, newText, null);
	}
	public static Uri createUserAppliedLabel(Context context, CharSequence newText, String filterId) {
		assert newText.length() > 0;
		
		// Prepare event info.
		Event.onEvent(Event.CREATE_LABEL, null); // Map<String,String> parameters = new HashMap<String,String>();
		Event.onEvent(Event.EDIT_TASK_LABELS, null); // Map<String,String> parameters = new HashMap<String,String>();
    	
		ContentValues cv = new ContentValues(3);
		cv.put(Task.LabelsColumns.DISPLAY_NAME, newText.toString());
		
		Uri userAppliedLabelUri = Uri.parse("content://"+Task.AUTHORITY+"/user_applied_label");
		
		if( null != filterId ){
			// cv.put("filter_id", String.valueOf(filterId));
			userAppliedLabelUri = userAppliedLabelUri.buildUpon().appendQueryParameter("filter_id", filterId).build();
		}
		
		Uri uri = context.getContentResolver().insert(userAppliedLabelUri, cv);
		if( null == uri ){
			Exception exp = (Exception)(new Exception("Failed to apply label to task.").fillInStackTrace());
			Log.e(TAG, "ERR0003E Failed to apply label to task.");
			ErrorUtil.handleExceptionNotifyUserAndThrow("ERR0003E", exp, context);
		}
		return uri;
	}
	
	public static void renameUserAppliedLabel(Context context, CharSequence newText, long id) {
		assert newText.length() > 0;

    	// Prepare event info. 
    	Event.onEvent(Event.RENAME_LABEL, null); 

		ContentValues cv = new ContentValues(3);
		cv.put(Task.LabelsColumns.DISPLAY_NAME, newText.toString());
		int count = context.getContentResolver().update(Uri.parse("content://"+Task.AUTHORITY+"/user_applied_label/"+id), cv, null, null);
		assert 1 == count;
	}


	public static Uri applyLabel(Context context, long labelId, Uri uri) {
		
    	// Prepare event info. 
    	Event.onEvent(Event.EDIT_TASK_LABELS, null); 

		ContentValues cv = new ContentValues();
		cv.put(Task.LabeledContentColumns._LABEL_ID, labelId);
		cv.put(Task.LabeledContentColumns._CONTENT_URI, uri.toString());
		Uri insertedUri = context.getContentResolver().insert(Task.LabeledContent.CONTENT_URI, cv);
		assert null != insertedUri;
		return insertedUri;
	}

	public static void applyLabels(Context context, long[] labelIds, Uri uri) {
		
    	// Prepare event info. 
    	Event.onEvent(Event.EDIT_TASK_LABELS, null); 

		ContentValues[] cvs = new ContentValues[labelIds.length];
		for(int i=0; i<labelIds.length; i++){
			cvs[i] = new ContentValues();
			cvs[i].put(Task.LabeledContentColumns._LABEL_ID, labelIds[i]);
			cvs[i].put(Task.LabeledContentColumns._CONTENT_URI, uri.toString());
		}
		int count = context.getContentResolver().bulkInsert(Task.LabeledContent.CONTENT_URI, cvs);
		assert labelIds.length == count;
	}
	
	public static void removeLabel(Context context, long labeledContentId) {
		
    	// Prepare event info. 
    	Event.onEvent(Event.EDIT_TASK_LABELS, null); 
		
		if (1 != context.getContentResolver().delete(
				ContentUris.withAppendedId(Task.LabeledContent.CONTENT_URI, labeledContentId), null, null)) {
			// Hey! Why did we attempt to delete an entry that doesn't exist?
			Log.w(TAG, "ERR0007S Attempted to remove entry that doesn't exist.");
			ErrorUtil.handle("ERR0007S", "Attempted to remove entry that doesn't exist.", context);
		}
	}
	
	public static Uri createUserAppliedLabelFilterElement(Context context, String labelId, String filterId){
		return FilterUtil.createFilterElement(context, filterId, Uri.parse(Constraint.Version1.LABEL_CONTENT_URI_STRING+"/"+labelId), true, Task.FilterElementColumns.PHASE_EXPLODE, FilterUtil.LABEL_FILTER_ELEMENT_ORDER, true);	
	}
	 
	public static Uri createUnlabeledFilterElement(Context context, String filterId, boolean active, boolean enabled){
		return FilterUtil.createFilterElement(context, filterId, Uri.parse(Constraint.Version1.UNLABELED_CONTENT_URI_STRING+"?"+LabelFilterElementDelegate.getFilterElementParameters(enabled)), true, Task.FilterElementColumns.PHASE_EXPLODE, FilterUtil.UNLABELED_FILTER_ELEMENT_ORDER, active);
	}
	public static Uri createArchiveFilterElement(Context context, String filterId, boolean active, String repository){
		Uri theUri = Uri.parse(Constraint.Version1.REPOSITORY_CONTENT_URI_STRING);
		theUri = theUri.buildUpon()
				.appendQueryParameter("INDEX", repository)
				.appendQueryParameter("repository", repository)
				.build();
		return FilterUtil.createFilterElement(context, filterId, theUri, true, Task.FilterElementColumns.PHASE_EXCLUDE, FilterUtil.ARCHIVE_FILTER_ELEMENT_ORDER, active);	
	}

	public static int createFilterElementForEachLabel(Context context, String filterId){
		Cursor labelCursor = context.getContentResolver().query(Task.Labels.CONTENT_URI, new String[]{Task.Labels._ID}, Task.Labels._USER_APPLIED+"=?", new String[]{Task.Labels.USER_APPLIED_TRUE}, Task.Labels._ID);
		assert null != labelCursor;
		
		while(labelCursor.moveToNext()){
			if( null == createUserAppliedLabelFilterElement(context, labelCursor.getString(0), filterId) ){
				Log.w(TAG, "ERR000EH Failed to add user applied label.");
				ErrorUtil.handleExceptionNotifyUser("ERR000EH", (Exception)(new Exception( filterId )).fillInStackTrace(), context);
				labelCursor.close();
				return -1;
			}
		}
		int count = labelCursor.getCount(); 
		labelCursor.close();
		return count;
	}
	
	public static final boolean isLabelsEnabled(Context context){
		SharedPreferences settings = context.getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
		boolean labelsEnabled = settings.getBoolean(ApplicationPreference.ENABLE_LABELS, ApplicationPreference.ENABLE_LABELS_DEFAULT);
		return ( labelsEnabled && LicenseUtil.hasLicense(context, LicenseUtil.FEATURE_LABELS) );
	}
	
}
