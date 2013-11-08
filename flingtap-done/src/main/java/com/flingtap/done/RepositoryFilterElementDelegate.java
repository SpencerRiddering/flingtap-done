// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flingtap.done.util.Constants;
import com.flurry.android.FlurryAgent;
import com.flingtap.done.base.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * 
 * @author spencer
 *
 */
public class RepositoryFilterElementDelegate 
		extends FilterElementDelegatingListAdapterDelegate 
		implements  View.OnCreateContextMenuListener{

	public static final String TAG = "RepositoryFilterElementDelegate";

	private final static int FIRST_CODE_ID = 1500;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}
	
//	// Item IDs
//	protected final static int LABEL_MENU_REMOVE_ITEM_ID = FIRST_CODE_ID + 10;
//	protected final static int LABEL_MENU_RENAME_ITEM_ID = FIRST_CODE_ID + 11;
//	protected final static int LABEL_MENU_VIEW_ITEM_ID   = FIRST_CODE_ID + 12;
//	
//	// Result Codes
//	protected final static int DELETE_LABEL_REQUEST      = FIRST_CODE_ID + 98;
//
	
	// Dialog IDs
	protected final static int SELECT_REPOSITORY_DIALOG_ID = FIRST_CODE_ID + 50;
	
	public final static String FILTER_ELEMENT_INDEX_PARAMETER = "INDEX";
	
	
	public RepositoryFilterElementDelegate(){
		uriDelegateMapping = new UriDelegateMapping[1];
		uriDelegateMapping[0] = new UriDelegateMapping();
		uriDelegateMapping[0].authority = Task.AUTHORITY;
		uriDelegateMapping[0].pathPattern = Constraint.Version1.REPOSITORY_URI_PATTERN_STRING;
		uriDelegateMapping[0].code = 0; // Uniquely identifies this mapping. Some Attachment handlers may handle multiple different mime-types so this allows us to distinguish between them. The value is passed into bindView(..)
		// TODO: Couldn't the array index be used instead of adding the .code member? 
	}
	
	Uri mSelectedLabelUri = null;
	Uri mFilterElementUri = null;
	Object tag = null;	
	
	@Override
	protected void bindView(final View view, Context context, Cursor cursor,
			int code, Uri data) {

		// Hide header title.
		view.findViewById(R.id.header_title).setVisibility(View.GONE);
		
		// Hide dual line layout.
		view.findViewById(R.id.label_list_item_dual_line_text).setVisibility(View.GONE);
		view.findViewById(R.id.label_list_item_toggle_button_layout).setVisibility(View.GONE);
		
		view.findViewById(R.id.label_list_item_single_line_text).setVisibility(View.VISIBLE);

		// **************************************
		// List item's text
		// **************************************
		TextView singleLineText = (TextView) view.findViewById(R.id.label_list_item_single_line_text);
		
		mSelectedLabelUri = ((Intent) view.getTag()).getData();
		int selectedIndex = null==mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER)?0:Integer.parseInt(mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER));
		updateLabelText(context, singleLineText, selectedIndex);

		// TODO: !!! Why can't the filter element id be set on this object by the Delegating List Adapter? 
		long filterElementId = ((Intent) view.getTag()).getLongExtra(FilterElementListAdapter.TAG_FILTER_ELEMENT_ID_INDEX, Constants.DEFAULT_NON_ID);
		assert Constants.DEFAULT_NON_ID != filterElementId;
		mFilterElementUri = ContentUris.withAppendedId(Task.FilterElement.CONTENT_URI, filterElementId);		
		
		tag = view.getTag();
 
	}

	private static void updateLabelText(Context context, TextView primaryLineText, int selectedIndex) {
		primaryLineText.setText(TextUtils.expandTemplate(context.getText(R.string.folderX), context.getResources().getStringArray(R.array.array_repository)[selectedIndex]) );
	}
	
	static String getDefaultRepositoryFilterElementParameters() {
		String parameters = FILTER_ELEMENT_INDEX_PARAMETER + "=0&" + Constraint.Version1.REPOSITORY_PARAM+"="+Constraint.Version1.REPOSITORY_PARAM_OPTION_DEFAULT;
		return parameters;
	}	
	static String getArchiveRepositoryFilterElementParameters() {
		String parameters = FILTER_ELEMENT_INDEX_PARAMETER + "=1&" + Constraint.Version1.REPOSITORY_PARAM+"="+Constraint.Version1.REPOSITORY_PARAM_OPTION_ARCHIVE;
		return parameters;
	}	
	
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo){
    	//Log.v(TAG, "onCreateContextMenu(..) called.");
		final String action = mIntent.getAction();
		if (Intent.ACTION_EDIT.equals(action) || Intent.ACTION_INSERT.equals(action)) {
            // Do nothing.
		}
	}

	/**
	 * 
	 */
	public void onListItemClick(ListView listview, View view, int position, long id) {
		try{
			// Don't allow user to edit if the filter is marked "permanent".
			// long filterId = ((Intent)view.getTag()).getLongExtra(FilterElementListAdapter.TAG_FILTER_ID_INDEX, Constants.DEFAULT_NON_ID);
			String filterId = ((Intent)tag).getStringExtra(FilterElementListAdapter.TAG_FILTER_ID_INDEX);
			if( null == filterId ){
				Log.e(TAG, "ERR000EX");
				ErrorUtil.handleExceptionNotifyUser("ERR000EX", (Exception)(new Exception( filterId )).fillInStackTrace(), mActivity);
				return;
			}
			Cursor filterCursor = mActivity.getContentResolver().query(
					Task.Filter.CONTENT_URI, 
					null, 
					Task.Filter._ID+"=? AND " +Task.Filter._PERMANENT+" IS NOT NULL", 
					new String[]{filterId}, 
					null);
			try{
				if( filterCursor.getCount() > 0 ){
					return;
				}
			}finally{
				filterCursor.close();
			}
			
			// Prepare event info.
			Event.onEvent(Event.OPEN_REPOSITORY_FILTER_ELEMENT, null); // Map<String,String> parameters = new HashMap<String,String>();
			
			mActivity.showDialog(SELECT_REPOSITORY_DIALOG_ID);		
			
			// HandledException is handled elsewhere --> }catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000AA", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000AA", exp, mActivity);
		}
	}

	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.v(TAG, "onActivityResult(..) called");
		if( resultCode == SharedConstant.RESULT_ERROR ){
			ErrorUtil.notifyUser(mActivity);
			return;
		}
	}

	public boolean onContextItemSelected(MenuItem item) {

    	AdapterView.AdapterContextMenuInfo mMenuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
    	
        switch (item.getItemId()) {
            // Do nothing
        }
        return false;
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}
	
	public Dialog onCreateDialog(int dialogId){
		try{
			switch(dialogId){
				case SELECT_REPOSITORY_DIALOG_ID:

					int selectedIndex = null==mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER)?Integer.parseInt(Constraint.Version1.REPOSITORY_PARAM_OPTION_DEFAULT):Integer.parseInt(mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER));
					SelectRepositoryOnClickListener clickListener = createClickListener(mFilterElementUri, selectedIndex);
					
					AlertDialog dialog =  new AlertDialog.Builder(mActivity)
							//.setTitle(R.string.dialog_lowestDisplayedPriority)
							//.setCustomTitle(includeNotDueDateItemsView)
							.setTitle(R.string.dialog_selectFolder)
							.setSingleChoiceItems(R.array.array_repository,selectedIndex,clickListener)
							.setPositiveButton(R.string.button_ok, clickListener)
							.setNegativeButton(R.string.button_cancel, clickListener)
							.setOnCancelListener(clickListener)
							.create();
					return dialog;
					
					
			}
			
		}catch(Exception exp){
			Log.e(TAG, "ERR000AB", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000AB", exp, mActivity);
		}
		return null;
	}

	// NOTE: This is called twice during a config change. First because of the restore, and then again as the dialog is created.
	// TODO: !! Is there a cleaner way to do this?
	private SelectRepositoryOnClickListener createClickListener(Uri filterElementUri, int selectedIndex) {
		if( null == clickListener ){ // This is very important because it prevents the intialization by the createDialog(..) from overriding the restored state.
			clickListener = new SelectRepositoryOnClickListener(mActivity, selectedIndex, filterElementUri, (Intent) tag);
		}
		return clickListener;
	}


	public void onPrepareDialog(int dialogId, Dialog dialog){
	}

	
	private class SelectRepositoryOnClickListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener{
		private int mOrigSelectedIndex = -1;
		private int mNewSelectedIndex = -1;
		private Uri mFilterElementUri = null;
		private Context mContext = null;
		private Intent mTag = null;
		
		public SelectRepositoryOnClickListener(Context context, int selectedIndex, Uri filterElementUri, Intent tag){
			assert 0 <= selectedIndex;
			mOrigSelectedIndex = selectedIndex;
			mNewSelectedIndex = mOrigSelectedIndex;
			
			assert null != filterElementUri;
			mFilterElementUri = filterElementUri;
			
			assert null != context;
			mContext = context;
						
			assert null != tag;
			mTag = tag;
			
		}
		
		public void onClick(DialogInterface dialog, int which) {
			try{
				if( DialogInterface.BUTTON2 == which ){
					handleCancel(dialog);
				}else if( DialogInterface.BUTTON1 == which ){
					if( mNewSelectedIndex == mOrigSelectedIndex ){
						return;
					}
					
					// Prepare event info.
					Event.onEvent(Event.EDIT_REPOSITORY_FILTER_ELEMENT, null); // Map<String,String> parameters = new HashMap<String,String>();
					
					ContentValues cv = new ContentValues(1);
					switch(mNewSelectedIndex){
						case 0: // Main
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=0&" + Constraint.Version1.REPOSITORY_PARAM+"="+Constraint.Version1.REPOSITORY_PARAM_OPTION_MAIN);
							break;
						case 1: // Archive
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=1&" + Constraint.Version1.REPOSITORY_PARAM+"="+Constraint.Version1.REPOSITORY_PARAM_OPTION_ARCHIVE);
							break;
						default:
							Log.e(TAG, "ERR00058 Unknown repository " + mNewSelectedIndex);
							ErrorUtil.handleExceptionNotifyUser("ERR00058", (Exception)(new Exception( String.valueOf(mNewSelectedIndex) )).fillInStackTrace(), mContext);
							return;
					}
					int count = mContext.getContentResolver().update(mFilterElementUri, cv, null, null);
					if( 1 != count ){
						Log.e(TAG, "ERR00059 Failed to update repository for URI");
						ErrorUtil.handleExceptionNotifyUser("ERR00059", (Exception)(new Exception( mFilterElementUri.toString() )).fillInStackTrace(), mContext);
						return;
					}
					FilterUtil.applyFilterBits(mContext);
					
					mOrigSelectedIndex = mNewSelectedIndex;
					
					// Update mSelectedLabelUri in tag.
					(mTag).setData(Uri.parse(Constraint.Version1.REPOSITORY_CONTENT_URI_STRING + "?" + cv.getAsString(Task.FilterElement._PARAMETERS)));

				}else if( 0 <= which ){
					mNewSelectedIndex = which;
				}
			}catch(HandledException h){ // Ignore.
			}catch(Exception exp){
				Log.e(TAG, "ERR00057", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR00057", exp, mContext);
			}
		}

		private void handleCancel(DialogInterface dialog) {
			mNewSelectedIndex = mOrigSelectedIndex;
			ListView listView = ((AlertDialog)dialog).getListView();
			
			// listView.setSelection(mNewSelectedIndex); // NOTE: setSelection(..) does not "select" a radio group item. It's a different type of "selection". 
			if( !listView.isItemChecked(mOrigSelectedIndex) ){
				listView.setItemChecked(mOrigSelectedIndex, true);
			}
		}

		public int getOrigSelectedIndex() {
			return mOrigSelectedIndex;
		}
		public void setOrigSelectedIndex(int origSelectedIndex) {
			mOrigSelectedIndex = origSelectedIndex;
		}
		
		public int getNewSelectedIndex() {
			return mNewSelectedIndex;
		}
		public void setNewSelectedIndex(int newSelectedIndex) {
			mNewSelectedIndex = newSelectedIndex;
		}
		public void onCancel(DialogInterface dialog) {
			handleCancel(dialog);
		}
	}
	
	SelectRepositoryOnClickListener clickListener = null;
	
	@Override
	public boolean hasInstanceState() { 
		return true;
	}

	private static final String SAVE_SELECTED_LABEL_URI 	= "RepositoryFilterElementDelegate.SAVE_SELECTED_LABEL_URI";
	private static final String SAVE_FILTER_ELEMENT_URI 	= "RepositoryFilterElementDelegate.SAVE_FILTER_ELEMENT_URI";
	private static final String SAVE_TAG 					= "RepositoryFilterElementDelegate.SAVE_TAG";
	private static final String SAVE_NEW_SELECTED_INDEX 	= "RepositoryFilterElementDelegate.SAVE_NEW_SELECTED_INDEX";
	private static final String SAVE_ORIGINAL_SELECTED_INDEX= "RepositoryFilterElementDelegate.SAVE_ORIGINAL_SELECTED_INDEX";
	public void  onSaveInstanceState  (Bundle outState){
		//Log.v(TAG, "onSaveInstanceState(..) called.");
		outState.putParcelable(SAVE_SELECTED_LABEL_URI, mSelectedLabelUri);
		outState.putParcelable(SAVE_FILTER_ELEMENT_URI, mFilterElementUri);
		outState.putParcelable(SAVE_TAG, (Intent)tag);
		if( null != clickListener ){
			outState.putInt(SAVE_NEW_SELECTED_INDEX, clickListener.getNewSelectedIndex());
			outState.putInt(SAVE_ORIGINAL_SELECTED_INDEX, clickListener.getOrigSelectedIndex());
		}
	}
	
	public void  onRestoreInstanceState  (Bundle savedInstanceState){
		//Log.v(TAG, "onRestoreInstanceState(..) called.");
		mSelectedLabelUri = savedInstanceState.getParcelable(SAVE_SELECTED_LABEL_URI);
		mFilterElementUri = savedInstanceState.getParcelable(SAVE_FILTER_ELEMENT_URI);
		tag = savedInstanceState.getParcelable(SAVE_TAG);
		
		int newSelectedIndex = savedInstanceState.getInt(SAVE_NEW_SELECTED_INDEX, -1);
		int selectedIndex = savedInstanceState.getInt(SAVE_ORIGINAL_SELECTED_INDEX, -1);
		assert null == clickListener;
		clickListener = createClickListener(mFilterElementUri, selectedIndex);
		clickListener.setNewSelectedIndex(newSelectedIndex);
	}
}
