// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flingtap.done.util.Constants;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.flingtap.done.base.R;

/**
 * Consolidates all methods for managing filters.
 *
 * @author spencer
 *
 */
public class FilterUtil {
	private static final String TAG = "FilterUtil";

	public static final int DUE_DATE_FILTER_ELEMENT_ORDER 			= 10;
	public static final int PRIORITY_FILTER_ELEMENT_ORDER 			= 20;
	public static final int STATUS_FILTER_ELEMENT_ORDER   			= 30;
	public static final int FOLDERS_HEADER_ROW_FILTER_ELEMENT_ORDER= 39;
	public static final int ARCHIVE_FILTER_ELEMENT_ORDER  			= 40;
		
	public static final int LABEL_FILTER_ELEMENT_ORDER = Integer.MAX_VALUE;
	public static final int UNLABELED_FILTER_ELEMENT_ORDER = 1000;
	
	/**
     * Apply the current filter for all tasks.
     *
	 * TODO: !!! Call this method in the Content Provider or maybe as a ContentObserver so that it is applied more effeiciently. It could occur automatically and then you can remove the calls that are distributed across the code base.
	 * TODO: !!! This kind of code should be moved into the provider. Wrap in a single transaction.
	 * 
	 * TODO: !! Reconsider this approach. Requires state which may not be completely compatible with REST style cloud db. Also, the any time (ex. due date) may cause the results to be out dated since there is no event to cause the flags to be updated.
	 * TODO: !!! Consider using a temp table to make the process more effecient.
	 * @param context
	 */
	public static void applyFilterBits(final Context context){
		applyFilterBits(context, null);
	}

    /**
     * Apply the current filter to a specific task.
     *
     * @param context
     * @param taskUri
     */
	public static void applyFilterBits(final Context context, Uri taskUri){
		
		new AsyncTask<Uri, Void, Boolean>(){

			@Override
			protected Boolean doInBackground(Uri... params) {
				try{
					return applyFilterBitsSync(context, params[0]);
					
				}catch(HandledException h){ // Ignore.
				}catch(Exception exp){
					Log.e(TAG, "ERR000CF", exp);
					ErrorUtil.handleException("ERR000CF", exp, context); // Notify user handled in onPostExecute(..)
				}  		
				return false;
			}
			
			@Override
			protected void onPostExecute(Boolean result) {
				super.onPostExecute(result);
				if( !result ){
					ErrorUtil.notifyUser(context);
				}
			}
		}.execute(taskUri);
	}
	
	public static Boolean applyFilterBitsSync(final Context context, Uri taskUri) {
		//Log.v(TAG, "applyFilterBits(..) called in ContentProvider.");
		Uri uri = Uri.parse("content://"+Task.AUTHORITY+"/filter_bits"); 
		if(null != taskUri ){
			uri = uri.buildUpon().appendPath(taskUri.getLastPathSegment()).build();
		}
		context.getContentResolver().update(uri, null, null, null);
		return true; 
	}

	// 
	public static void actuallyDeleteFilter(Context context, Uri filterUri) {
    	// Prepare event info. 
    	Event.onEvent(Event.DELETE_FILTER, null);
		
    	if( -1 == ContentUris.parseId(filterUri) ){
			Log.e(TAG, "ERR000H2");
			ErrorUtil.handleExceptionNotifyUser("ERR000H2", (Exception)(new Exception(  )).fillInStackTrace(), context);
			return;
    	}

		int count = context.getContentResolver().delete(filterUri, null, null);
		if( 1 != count ){
			Log.e(TAG, "ERR000FW");
			ErrorUtil.handleExceptionNotifyUser("ERR000FW", (Exception)(new Exception(  )).fillInStackTrace(), context);
		}
	}
	
	/**
	 * Checks to see if the Archive filter is active.
	 * 
	 * In the future when there are multiple filters there will be 
	 *   only one "archive" filter which cannot be combined with 
	 *   other filters. This method returns true if that filter is 
	 *   active. 
	 *   
	 * Currently, this method returns true if the repository is set
	 *   to "Archive".
	 * 
	 */
	public static boolean isArchiveFilterActive(Context context){
		//Uri uri = ContentUris.withAppendedId(Task.Filter.CONTENT_URI, Long.parseLong(TaskProvider.ID_FILTER_BASIC)).buildUpon().appendEncodedPath("filter_elements").build(); 
		Uri uri = Uri.parse("content://"+Task.AUTHORITY+"/selected_filter/filter_elements"); 
		Cursor cursor = context.getContentResolver().query(
				uri, 
				new String[]{TaskProvider.FILTER_ELEMENT_TABLE_NAME+"."+Task.FilterElementColumns._ACTIVE, Task.FilterElementColumns._PARAMETERS}, 
				Task.FilterElementColumns._CONSTRAINT_URI+"=?", 
				new String[]{Constraint.Version1.REPOSITORY_CONTENT_URI_STRING}, 
				TaskProvider.FILTER_ELEMENT_TABLE_NAME+"."+Task.FilterElementColumns._ID);
		assert null != cursor;
		// assert cursor.getCount() == 1; // This could be 0, since some filters don't have a repository element.
		if( !cursor.moveToFirst() ){
			// This can be true when the archive is not enabled.
			cursor.close();
			return false;
		}
		String active = cursor.getString(0);
		String parameters = cursor.getString(1);
		cursor.close();

		String repository = null;
		if( null != parameters ){
			Uri uriParams = Uri.parse("?"+parameters); // NOTE: The "?" causes the URI parser to recognize the String as parameters.
			repository = uriParams.getQueryParameter(Constraint.Version1.REPOSITORY_PARAM);
		}
		if( null == repository ){
			repository = Constraint.Version1.REPOSITORY_PARAM_OPTION_DEFAULT;
		}
		if( Task.FilterElementColumns.ACTIVE_TRUE.equals(active) && 
				Constraint.Version1.REPOSITORY_PARAM_OPTION_ARCHIVE.equals(repository)){
			return true;
		}
		return false;
	}

	
	public static Uri createFilterWithBasicElements(Context context, String filterName, int filterNameArrayIndex){
    	// Prepare event info. 
    	Event.onEvent(Event.CREATE_FILTER, null);
		
		ContentValues values = new ContentValues();
		if( null == filterName ){
			values.put(Task.Filter._DISPLAY_NAME_ARRAY_INDEX, filterNameArrayIndex);
		}else{
			values.put(Task.Filter.DISPLAY_NAME, filterName);
		}
		values.put(Task.Filter._ACTIVE, Task.Filter.ACTIVE_TRUE);

		Uri uri = null;
		uri = context.getContentResolver().insert(Task.Filter.CONTENT_URI, values);
		if( null == uri ){
			return null;
		}
		boolean completedCorrectly = false;
		Uri feUri = null;
		try{
			// Due date.
			feUri = FilterUtil.createFilterElement(context, uri.getLastPathSegment(), Uri.parse(Constraint.Version1.DUE_CONTENT_URI_STRING).buildUpon().appendQueryParameter("INDEX","0").appendQueryParameter(Constraint.Version1.ANYTIME,"true").appendQueryParameter(Constraint.Version1.INCLUDE_NO_DUE_DATE_ITEMS, "true").build(), true, Task.FilterElementColumns.PHASE_INCLUDE, DUE_DATE_FILTER_ELEMENT_ORDER, true); // TODO: !!! I Shoudn't need to specify the default param values in this URI.
			if( null == feUri ){
				return null;
			}
			
			// Priority
			feUri = FilterUtil.createFilterElement(context, uri.getLastPathSegment(), Uri.parse(Constraint.Version1.PRIORITY_CONTENT_URI_STRING), true, Task.FilterElementColumns.PHASE_INCLUDE, PRIORITY_FILTER_ELEMENT_ORDER, true);
			if( null == feUri ){
				return null;
			}
			
			// Status
			feUri = FilterUtil.createFilterElement(context, uri.getLastPathSegment(), Uri.parse(Constraint.Version1.STATUS_CONTENT_URI_STRING), true, Task.FilterElementColumns.PHASE_INCLUDE, STATUS_FILTER_ELEMENT_ORDER, true);
			if( null == feUri ){
				return null;
			}
			
			completedCorrectly = true;
		}finally{
			if( !completedCorrectly ){
				int count = context.getContentResolver().delete(uri, null, null);
				Log.e(TAG, "ERR000G6");
				ErrorUtil.handleExceptionNotifyUser("ERR000G6", (Exception)(new Exception( String.valueOf(count) )).fillInStackTrace(), context);
				return null;
			}
		}
		
		return uri;
	}
	
	public static CreateFilterOnTextSetListener createCreateFilterOnTextSetListener(final Activity activity){
		return new CreateFilterOnTextSetListener(activity);
	}
	
    public static Dialog onCreateDialogCreateFilter(final Activity activity, TextEntryDialog.OnTextSetListener textSetListener){
    	Dialog dialog = null;
    	try{
    		
    		dialog = TextEntryDialog.onCreateDialog(activity, textSetListener, activity.getText(R.string.dialog_setFilterName), null, null);     	
    		
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000E1", exp);
    		ErrorUtil.handleException("ERR000E1", exp, activity);
    	}

        return dialog;
    		
    }
    public static void onPrepareDialogCreateFilter(final Activity activity, Dialog dialog){
    	try{
    		TextEntryDialog.onPrepareDialog(activity, dialog, null);
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000E2", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR000E2", exp, activity);
    	}
    }	

    public static Uri createFilterWithStandardElements(Context context, CharSequence filterName, int filterNameArrayIndex, boolean labelsEnabled, boolean archiveEnabled) {
    	return createFilterWithStandardElements(context, filterName, filterNameArrayIndex, null, labelsEnabled, archiveEnabled);
    }
    /**
     * Includes createFilterWithBasicElements( ).
     */
    public static Uri createFilterWithStandardElements(Context context, CharSequence filterName, int filterNameArrayIndex, String[] labelList, boolean labelsEnabled, boolean archiveEnabled) {

    	Cursor checkForDuplicateNameCursor = context.getContentResolver().query(Task.Filter.CONTENT_URI, null, Task.Filter.DISPLAY_NAME+"=?", new String[]{filterName.toString()}, null);
    	if( checkForDuplicateNameCursor.getCount() > 0 ){
    		// A filter with this name already exists. 
    		Toast.makeText(context, R.string.toast_filtersMayNotHaveTheSameName, Toast.LENGTH_SHORT).show();
    		return null;
    	}
    	checkForDuplicateNameCursor.close();
    	Uri resultUri = null;
		try{
			resultUri = createFilterWithBasicElements(context, null==filterName?null:filterName.toString(), filterNameArrayIndex);
		}catch(SQLException sce){
			// Probably just a duplicate filter name.
			return null;
		}

    	if( null == resultUri ){
    		Log.e(TAG, "ERR000EI");
    		ErrorUtil.handleExceptionNotifyUser("ERR000EI", (Exception)(new Exception(  )).fillInStackTrace(), context);
    		return null;
    	}
    	boolean resultOk = false;
    	try{
    		// Add filter elements.
    		
    		if( null == LabelUtil.createArchiveFilterElement(context, resultUri.getLastPathSegment(), archiveEnabled, Constraint.Version1.REPOSITORY_PARAM_OPTION_MAIN) ){
        		Log.e(TAG, "ERR000EJ");
        		ErrorUtil.handleExceptionNotifyUser("ERR000EJ", (Exception)(new Exception(  )).fillInStackTrace(), context);
        		return null;
    		}

    		// Unlabeled.
    		boolean enableUnlabledFilterElement = (null == labelList || labelList.length == 0 );
    		if( null == LabelUtil.createUnlabeledFilterElement(context, resultUri.getLastPathSegment(), labelsEnabled, enableUnlabledFilterElement) ){
    			Log.e(TAG, "ERR000EL");
    			ErrorUtil.handleExceptionNotifyUser("ERR000EL", (Exception)(new Exception(  )).fillInStackTrace(), context);
        		return null;
    		}
    		
    		if( labelsEnabled){
    			if( null != labelList && labelList.length != 0 ){
    				for(String label: labelList){
    					if( null == LabelUtil.createUserAppliedLabel(context, label, resultUri.getLastPathSegment()) ){
    						Log.w(TAG, "ERR000EU Failed to add user applied label.");
    						ErrorUtil.handleExceptionNotifyUser("ERR000EU", (Exception)(new Exception( "Failed to add user applied label." )).fillInStackTrace(), context);
    			    		return null;
    					}
    				}
    			}else{
        			if( -1 == LabelUtil.createFilterElementForEachLabel(context, resultUri.getLastPathSegment()) ){
            			
                		Log.e(TAG, "ERR000EK");
                		ErrorUtil.handleExceptionNotifyUser("ERR000EK", (Exception)(new Exception( resultUri.getLastPathSegment() )).fillInStackTrace(), context);
                		return null;
        			}
    			}
    		}
    		
    		resultOk = true;
    	}finally{
    		if(!resultOk){
    			if( 1 != context.getContentResolver().delete(resultUri, null, null) ){
            		Log.e(TAG, "ERR000EM");
            		ErrorUtil.handleExceptionNotifyUser("ERR000EM", (Exception)(new Exception(  )).fillInStackTrace(), context);
    			}
    		}
    	}
    	return resultUri;    	
    }

	public static class CreateFilterOnTextSetListener implements TextEntryDialog.OnTextSetListener {

		private Activity mActivity = null;
		
		public CreateFilterOnTextSetListener(Activity activity){
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
				
				Uri theNewUri = FilterUtil.createFilterWithStandardElements(mActivity, newText, -1, LabelUtil.isLabelsEnabled(mActivity), ArchiveUtil.isArchiveEnabled(mActivity));
				if( null == theNewUri ){
					return;
				}
				// Start viewing the filter here.
				Intent filterElementListActivityIntent = FilterElementListActivity.createViewIntent(theNewUri);
				mActivity.startActivity(filterElementListActivityIntent); // TODO: !!! Consider how errors from this activity will propagate back to the caller.
				
			}catch(HandledException h){ // Ignore.	
			}catch(Exception exp){
				Log.e(TAG, "ERR000E0", exp);
				ErrorUtil.handleException("ERR000E0", exp, mActivity);
			}
		}

		public void onCancel() {
			// Do nothing.
		}		
	}
	
	public static void switchSelectedFilter(Context context, String filterId){
		context.getContentResolver().update(Uri.parse("content://"+Task.AUTHORITY+"/switch_filter/"+filterId) , null, null, null); // TODO: !!! Check the return value so we know it worked.
	}
	public static void switchSelectedToPermanantFilter(Context context, String permanentFilterId){
		context.getContentResolver().update(Uri.parse("content://"+Task.AUTHORITY+"/switch_to_permanent_filter/"+permanentFilterId) , null, null, null); // TODO: !!! Check the return value so we know it worked.
	}
	
	// **********************************************************
	// Rename filter dialog
	// **********************************************************
	public static void renameFilter(Context context, CharSequence newText, long id) {
		assert newText.length() > 0;

    	// Prepare event info. 
    	Event.onEvent(Event.RENAME_FILTER, null); 

		ContentValues cv = new ContentValues(1);
		cv.put(Task.Filter.DISPLAY_NAME, newText.toString());
		int count = context.getContentResolver().update(Task.Filter.CONTENT_URI, cv, Task.Filter._ID+"=?", new String[]{String.valueOf(id)});
		assert 1 == count;
	}
	public static class RenameFilterOnTextSetListener implements TextEntryDialog.OnTextSetListener {

		private long mId = Constants.DEFAULT_NON_ID;
		public void setId(long id) {
			mId = id;
		}

		private Activity mActivity = null;
		
		public RenameFilterOnTextSetListener(Activity activity, long id){
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
				renameFilter(mActivity, newText, mId);
			}catch(HandledException h){ // Ignore.	
			}catch(Exception exp){
				Log.e(TAG, "ERR000FX", exp);
				ErrorUtil.handleException("ERR000FX", exp, mActivity);
			}
		}
		
		public void onCancel() {
			// Do nothing.
		}		
	}
	
	public static RenameFilterOnTextSetListener createRenameFilterOnTextSetListener(final Activity activity, long id){
		return new RenameFilterOnTextSetListener(activity, id);
	}
	
    public static Dialog onCreateDialogRenameFilter(final Activity activity, final long id, TextEntryDialog.OnTextSetListener filterNameTextSetListener){
    	Dialog dialog = null;
    	try{
    		Cursor cursor = queryFilterDisplayName(activity, id);
    		
    		dialog = TextEntryDialog.onCreateDialog(activity, filterNameTextSetListener, activity.getText(R.string.dialog_editLabelName), null, cursor.getString(0));     	
    		
        	cursor.close();
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000FY", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR000FY", exp, activity);
    	}

        return dialog;
    		
    }
    public static void onPrepareDialogRenameFilter(final Activity activity, Dialog dialog, final long id){
    	try{
    		Cursor cursor = queryFilterDisplayName(activity, id);
    		
    		TextEntryDialog.onPrepareDialog(activity, dialog, cursor.getString(0));
    		
    		cursor.close();
    		
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000FZ", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR000FZ", exp, activity);
    	}
    }

	private static Cursor queryFilterDisplayName(final Activity activity, final long id) {
		Cursor cursor = activity.getContentResolver().query(ContentUris.withAppendedId(Task.Filter.CONTENT_URI, id), new String[]{Task.Filter.DISPLAY_NAME}, null, null, null);
    	assert null != cursor;
    	if( !cursor.moveToFirst() ){
    		cursor.close();
    		Exception exp = (Exception)(new Exception("Unable to rename filter.").fillInStackTrace());    		
    		Log.e(TAG, "ERR000G0 Failed to find existing filter when attempting to rename it.");
    		ErrorUtil.handleExceptionNotifyUserAndThrow("ERR000G0", exp, activity);    		
    	}
		return cursor;
	}	
	
	public static Uri createFilterElement(Context context, String filterId, Uri constraintUri, boolean applyWhenActive, String phase, int order, boolean active){
		
		assert null != phase;
		assert Task.FilterElementColumns.PHASE_EXPLODE.equals(phase) || Task.FilterElementColumns.PHASE_INCLUDE.equals(phase) || Task.FilterElementColumns.PHASE_EXCLUDE.equals(phase);
		assert null != constraintUri;
		assert null != filterId;
		
		ContentValues values = new ContentValues();
		if( active ){
			values.put(Task.FilterElementColumns._ACTIVE, Task.FilterElementColumns.ACTIVE_TRUE);
		}else{
			values.putNull(Task.FilterElementColumns._ACTIVE);
		}
		values.put(Task.FilterElementColumns._APPLY_WHEN_ACTIVE, applyWhenActive?Task.FilterElementColumns.APPLY_WHEN_ACTIVE:Task.FilterElementColumns.APPLY_WHEN_NOT_ACTIVE);
		int position = constraintUri.toString().indexOf('?');
		values.put(Task.FilterElementColumns._CONSTRAINT_URI, position==-1?constraintUri.toString():constraintUri.toString().substring(0, position));
		values.put(Task.FilterElementColumns._PARAMETERS, position==-1?null:constraintUri.toString().substring(constraintUri.toString().indexOf('?')+1)); // TODO: Check if this is adding an empty string, rather than a null.
		values.put(Task.FilterElementColumns._FILTER_ID, filterId);
		values.put(Task.FilterElementColumns._PHASE, phase);
		if( Integer.MAX_VALUE != order ){
			values.put(Task.FilterElementColumns._ORDER, order);
		}
		return context.getContentResolver().insert(Task.FilterElement.CONTENT_URI, values);
	}

}
