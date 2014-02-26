// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flingtap.done.util.Constants;
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
import android.widget.ListView;
import android.widget.TextView;

/**
 * 
 * @author spencer
 *
 */
public class PriorityFilterElementDelegate 
		extends FilterElementDelegatingListAdapterDelegate 
		implements  View.OnCreateContextMenuListener{

	public static final String TAG = "PriorityFilterElementDelegate";

	private final static int FIRST_CODE_ID = 1400;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}

	// Dialog IDs
	protected final static int SELECT_PRIORITY_DIALOG_ID = FIRST_CODE_ID + 50;
	
	protected final static String FILTER_ELEMENT_INDEX_PARAMETER = "INDEX"; // TODO: How would this applied?  
	
	public PriorityFilterElementDelegate(){
		uriDelegateMapping = new UriDelegateMapping[1];
		uriDelegateMapping[0] = new UriDelegateMapping();
		uriDelegateMapping[0].authority = Task.AUTHORITY;
		uriDelegateMapping[0].pathPattern = Constraint.Version1.PRIORITY_URI_PATTERN_STRING;
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

		
		// Hide dual line layout is gone.
		view.findViewById(R.id.label_list_item_single_line_text).setVisibility(View.GONE);
		// Hide toggle button.
		view.findViewById(R.id.label_list_item_toggle_button_layout).setVisibility(View.GONE);


		view.findViewById(R.id.label_list_item_dual_line_text).setVisibility(View.VISIBLE);
		// **************************************
		// List item's text
		// **************************************
		TextView primaryLineText = (TextView) view.findViewById(R.id.label_list_item_dual_line_text_1);
		TextView secondaryLineText = (TextView) view.findViewById(R.id.label_list_item_dual_line_text_2);
		
		mSelectedLabelUri = ((Intent) view.getTag()).getData();
		int selectedIndex = null==mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER)?0:Integer.parseInt(mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER));
		updateLabelText(context, primaryLineText, selectedIndex);

		String description = context.getString(R.string.tasksWithLowerPriorityWillNotBeDisplayed);
		assert null != description;
		secondaryLineText.setText(description);
		
		long filterElementId = ((Intent) view.getTag()).getLongExtra(FilterElementListAdapter.TAG_FILTER_ELEMENT_ID_INDEX, Constants.DEFAULT_NON_ID);
		assert Constants.DEFAULT_NON_ID != filterElementId;
		mFilterElementUri = ContentUris.withAppendedId(Task.FilterElement.CONTENT_URI, filterElementId);

		tag = view.getTag();
		
	}

	private static void updateLabelText(Context context, TextView primaryLineText, int selectedIndex) {
		primaryLineText.setText(TextUtils.expandTemplate(context.getString(R.string.priorityX), context.getResources().getStringArray(R.array.array_priority)[selectedIndex]) );
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo){
    	//Log.v(TAG, "onCreateContextMenu(..) called.");
		final String action = mIntent.getAction();
		if (Intent.ACTION_EDIT.equals(action) || Intent.ACTION_INSERT.equals(action)) {

		}
	}

	

	/**
	 * 
	 */
	public void onListItemClick(ListView listview, View view, int position, long id) {
		try{
		// Prepare event info.
		Event.onEvent(Event.OPEN_PRIORITY_FILTER_ELEMENT, null); // Map<String,String> parameters = new HashMap<String,String>();
				
		mActivity.showDialog(SELECT_PRIORITY_DIALOG_ID);
		
		// HandledException is handled elsewhere --> }catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000A8", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000A8", exp, mActivity);
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		Log.v(TAG, "onActivityResult(..) called");
		if( resultCode == SharedConstant.RESULT_ERROR ){
			ErrorUtil.notifyUser(mActivity);
			return;
		}
	}

	public boolean onContextItemSelected(MenuItem item) {

    	AdapterView.AdapterContextMenuInfo mMenuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
    	
        switch (item.getItemId()) {
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
				case SELECT_PRIORITY_DIALOG_ID:
					
					int selectedIndex = null==mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER)?0:Integer.parseInt(mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER));
					clickListener = createClickListener(mFilterElementUri, selectedIndex);
					
					AlertDialog dialog =  new AlertDialog.Builder(mActivity)
							.setTitle(R.string.dialog_lowestDisplayedPriority)
							.setSingleChoiceItems(
								R.array.array_priority, 
								selectedIndex,
								clickListener)
							.setPositiveButton(R.string.button_ok, clickListener)
							.setNegativeButton(R.string.button_cancel, clickListener)
							.setOnCancelListener(clickListener)
							.create();
					return dialog;
			}
			
		}catch(Exception exp){
			Log.e(TAG, "ERR000A6", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000A6", exp, mActivity);
		}
		return null;
	}

	// NOTE: This is called twice during a config change. First because of the restore, and then again as the dialog is created.
	// TODO: !! Is there a cleaner way to do this?
	private SelectPriorityOnClickListener createClickListener(Uri filterElementUri, int selectedIndex) {
		if( null == clickListener ){ // This is very important because it prevents the intialization by the createDialog(..) from overriding the restored state.
			clickListener = new SelectPriorityOnClickListener(mActivity, selectedIndex, filterElementUri, (Intent) tag);
		}
		return clickListener;
	}
	
	

	public void onPrepareDialog(int dialogId, Dialog dialog){
	}

	
	private class SelectPriorityOnClickListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener{
		private int mOrigSelectedIndex = -1;
		private int mNewSelectedIndex = -1;
		private Uri mFilterElementUri = null;
		private Context mContext = null;
		private Intent mTag = null;
		
		
//		public SelectPriorityOnClickListener(Context context, int selectedIndex, Uri filterElementUri, TextView primaryLineText, Intent tag){
		public SelectPriorityOnClickListener(Context context, int selectedIndex, Uri filterElementUri, Intent tag){
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

				if( DialogInterface.BUTTON_NEGATIVE == which ){
					handleCancel(dialog);
				}else if( DialogInterface.BUTTON_POSITIVE == which ){
					if( mNewSelectedIndex == mOrigSelectedIndex ){
						return;
					}
					
					// Prepare event info.
					Event.onEvent(Event.EDIT_PRIORITY_FILTER_ELEMENT, null); // Map<String,String> parameters = new HashMap<String,String>();
					
					ContentValues cv = new ContentValues(1);
					switch(mNewSelectedIndex){
						case 0: // None
							cv.putNull(Task.FilterElement._PARAMETERS);
							break;
						case 1: // Low
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=1&" + Constraint.Version1.PRIORITY_PARAM+"="+Constraint.Version1.PRIORITY_VALUE_LOW);
							break;
						case 2: // Medium
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=2&" + Constraint.Version1.PRIORITY_PARAM+"="+Constraint.Version1.PRIORITY_VALUE_MEDIUM);
							break;
						case 3: // High
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=3&" + Constraint.Version1.PRIORITY_PARAM+"="+Constraint.Version1.PRIORITY_VALUE_HIGH);
							break;
						default:
							Log.e(TAG, "ERR00051 Unknown priority " + mNewSelectedIndex);
							ErrorUtil.handleExceptionNotifyUser("ERR00051", (Exception)(new Exception(  String.valueOf(mNewSelectedIndex) )).fillInStackTrace(), mContext);
							return;
					}
					int count = mContext.getContentResolver().update(mFilterElementUri, cv, null, null);
					if( 1 != count ){
						Log.e(TAG, "ERR00052 Failed to update priority for URI");
						ErrorUtil.handleExceptionNotifyUser("ERR00052", (Exception)(new Exception( mFilterElementUri.toString() )).fillInStackTrace() , mContext);
						return;
					}
					FilterUtil.applyFilterBits(mContext);
					
					mOrigSelectedIndex = mNewSelectedIndex;

					// Update mSelectedLabelUri in tag.
					(mTag).setData(Uri.parse(Constraint.Version1.PRIORITY_CONTENT_URI_STRING + "?" + cv.getAsString(Task.FilterElement._PARAMETERS)));
					
				}else if( 0 <= which ){
					mNewSelectedIndex = which;
				}
			}catch(HandledException h){ // Ignore.
			}catch(Exception exp){
				Log.e(TAG, "ERR00050", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR00050", exp, mContext);
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
	
	SelectPriorityOnClickListener clickListener = null;

	
	@Override
	public boolean hasInstanceState() { 
		return true;
	}

	private static final String SAVE_SELECTED_LABEL_URI 	= "PriorityFilterElementDelegate.SAVE_SELECTED_LABEL_URI";
	private static final String SAVE_FILTER_ELEMENT_URI 	= "PriorityFilterElementDelegate.SAVE_FILTER_ELEMENT_URI";
	private static final String SAVE_TAG 					= "PriorityFilterElementDelegate.SAVE_TAG";
	private static final String SAVE_NEW_SELECTED_INDEX 	= "PriorityFilterElementDelegate.SAVE_NEW_SELECTED_INDEX";
	private static final String SAVE_ORIGINAL_SELECTED_INDEX= "PriorityFilterElementDelegate.SAVE_ORIGINAL_SELECTED_INDEX";
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
