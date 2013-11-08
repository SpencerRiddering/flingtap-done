// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.done.provider.Task;
import com.flingtap.done.util.Constants;
import com.flurry.android.FlurryAgent;
import com.flingtap.done.base.R;

import android.R.anim;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriMatcher;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * 
 * @author spencer
 *
 */
public class DueDateFilterElementDelegate 
		extends FilterElementDelegatingListAdapterDelegate 
		implements  View.OnCreateContextMenuListener{

	public static final String TAG = "DueDateFilterElementDelegate";

	private final static int FIRST_CODE_ID = 800;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}
	
	// Item IDs
	protected final static int LABEL_MENU_REMOVE_ITEM_ID = FIRST_CODE_ID + 10;
	protected final static int LABEL_MENU_RENAME_ITEM_ID = FIRST_CODE_ID + 11;
	protected final static int LABEL_MENU_VIEW_ITEM_ID   = FIRST_CODE_ID + 12;
	
	// Result Codes
	protected final static int DELETE_LABEL_REQUEST      = FIRST_CODE_ID + 98;

	// Dialog IDs
	protected final static int SELECT_DUE_DATE_DIALOG_ID = FIRST_CODE_ID + 50;
	
	
	protected final static String FILTER_ELEMENT_INDEX_PARAMETER = "INDEX";
	
	public DueDateFilterElementDelegate(){
		uriDelegateMapping = new UriDelegateMapping[1];
		uriDelegateMapping[0] = new UriDelegateMapping();
		uriDelegateMapping[0].authority = Task.AUTHORITY;
		uriDelegateMapping[0].pathPattern = "tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+Constraint.Version1.DUE;
		uriDelegateMapping[0].code = 0; // Uniquely identifies this mapping. Some Attachment handlers may handle multiple different mime-types so this allows us to distinguish between them. The value is passed into bindView(..)
		// TODO: Couldn't the array index be used instead of adding the .code member? 
	}
	
	/**
	 * TODO: Seems that the single and dual line layouts could be combined to reduce complexity and size.
	 */
	protected void bindView(final View view, Context context, Cursor cursor,
			int code, Uri data) {
		// Log.v(TAG, "bindView(..) called.");
		
		// Hide header title.
		view.findViewById(R.id.header_title).setVisibility(View.GONE);

		// Hide dual line layout is gone.
		view.findViewById(R.id.label_list_item_dual_line_text).setVisibility(View.GONE);
		// Hide toggle button.
		view.findViewById(R.id.label_list_item_toggle_button_layout).setVisibility(View.GONE);
		
		// **************************************
		// 
		// **************************************
		TextView singleLineText = (TextView) view.findViewById(R.id.label_list_item_single_line_text);
		singleLineText.setVisibility(View.VISIBLE);
		
		tag = view.getTag();
		
		mSelectedLabelUri = ((Intent) view.getTag()).getData();
		int selectedIndex = null==mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER)?0:Integer.parseInt(mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER));

		// TODO: !!! "Off or never" sounds strange.
		singleLineText.setText(
				TextUtils.expandTemplate(
						context.getString(R.string.dueDateX), 
						context.getResources().getStringArray(R.array.array_dueDateFilter)[selectedIndex]
				)
		);
		

		long filterElementId = ((Intent) view.getTag()).getLongExtra(FilterElementListAdapter.TAG_FILTER_ELEMENT_ID_INDEX, Constants.DEFAULT_NON_ID);
		assert Constants.DEFAULT_NON_ID != filterElementId;
		mFilterElementUri = ContentUris.withAppendedId(Task.FilterElement.CONTENT_URI, filterElementId);
		
	}
	Uri mSelectedLabelUri = null;
	Uri mFilterElementUri = null;
	Object tag = null;			
		
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo){
		try{
			// Log.v(TAG, "onCreateContextMenu(..) called.");
			final String action = mIntent.getAction();
			if (Intent.ACTION_EDIT.equals(action) || Intent.ACTION_INSERT.equals(action)) {
				// Do nothing.
			}		
		// HandledException is handled elsewhere --> }catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001S", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0001S", exp, mActivity);
		}
	}

	
	/**
	 * 
	 */
	public void onListItemClick(ListView listview, View view, int position, long id) {
		try{
			// Prepare event info.
			Event.onEvent(Event.OPEN_DUE_DATE_FILTER_ELEMENT, null); // Map<String,String> parameters = new HashMap<String,String>();
			
			mActivity.showDialog(SELECT_DUE_DATE_DIALOG_ID);
			
		// HandledException is handled elsewhere --> }catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0002I", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0002I", exp, mActivity);
		}

	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		try{
			if( resultCode == SharedConstant.RESULT_ERROR ){
				ErrorUtil.notifyUser(mActivity);
				return;
			}

//			Log.v(TAG, "onActivityResult(..) called");

		// HandledException is handled elsewhere --> }catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0002J", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0002J", exp, mActivity);
		}
	}

	public boolean onContextItemSelected(MenuItem item) {
		try{
			AdapterView.AdapterContextMenuInfo mMenuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			
			switch (item.getItemId()) {
                // Do nothing.
			}
			
		// HandledException is handled elsewhere --> }catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0002K", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0002K", exp, mActivity);
		}

        return false;
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}
	
	private static class SelectDueDateOnClickListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener{
		
		// State is persisted
		private int mOrigSelectedIndex = -1;
		private int mNewSelectedIndex = -1;
		private boolean mOrigToggle = false;
		
		private Uri mFilterElementUri = null;
		private Activity mContext = null;
		private CheckBox mDialogCheckBox = null;
		private Intent mTag = null;
		
		
		
		public SelectDueDateOnClickListener(Activity context, int selectedIndex, Uri filterElementUri, CheckBox dialogCheckBox, Intent tag){
			assert 0 <= selectedIndex;
			mOrigSelectedIndex = selectedIndex;
			mNewSelectedIndex = mOrigSelectedIndex;
			
			mFilterElementUri = filterElementUri;
			assert null != mFilterElementUri;
			
			assert null != context;
			mContext = context;
			
			mDialogCheckBox = dialogCheckBox;
			mOrigToggle = mDialogCheckBox.isChecked();
			
			assert null != tag;
			mTag = tag;
		}

		public void onClick(DialogInterface dialog, int which) {
			try{

				if( DialogInterface.BUTTON_NEGATIVE == which ){
					handleCancel(dialog);
				}else if( DialogInterface.BUTTON_POSITIVE == which ){
					if( mNewSelectedIndex == mOrigSelectedIndex && mOrigToggle == mDialogCheckBox.isChecked() ){
						return;
					}
					
					// Prepare event info.
					Event.onEvent(Event.EDIT_DUE_DATE_FILTER_ELEMENT, null); // Map<String,String> parameters = new HashMap<String,String>();
					
					ContentValues cv = new ContentValues(1);
					switch(mNewSelectedIndex){
						case 7: // Off.
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=7&" + Constraint.Version1.DISABLE+"=true" + (mDialogCheckBox.isChecked()?'&'+Constraint.Version1.INCLUDE_NO_DUE_DATE_ITEMS+"=true":""));
							break;
						case 6: // Overdue.
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=6&" + Constraint.Version1.PAST+"=true" + (mDialogCheckBox.isChecked()?'&'+Constraint.Version1.INCLUDE_NO_DUE_DATE_ITEMS+"=true":""));
							break;
						case 5: // Today.
							//								mSelectedLabelUri = Uri.parse("content://"+Task.AUTHORITY+"/tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+Constraint.Version1.DUE+"/"+Constraint.Version1.DAYS_FROM_TODAYS_END+"/0");
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=5&" + Constraint.Version1.DAYS_FROM_TODAYS_END+"=0" + (mDialogCheckBox.isChecked()?'&'+Constraint.Version1.INCLUDE_NO_DUE_DATE_ITEMS+"=true":""));
							//								cv.put(Task.FilterElement._INCLUDED, isChecked?Task.FilterElement.INCLUDED_TRUE:Task.FilterElement.INCLUDED_FALSE);
							break;
						case 4: // Tomorrow.
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=4&" + Constraint.Version1.DAYS_FROM_TODAYS_END+"=1" + (mDialogCheckBox.isChecked()?'&'+Constraint.Version1.INCLUDE_NO_DUE_DATE_ITEMS+"=true":""));
							break;
						case 3: // This week.
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=3&" + Constraint.Version1.WEEKS_FROM_THIS_WEEKS_END+"=0" + (mDialogCheckBox.isChecked()?'&'+Constraint.Version1.INCLUDE_NO_DUE_DATE_ITEMS+"=true":""));
							break;
						case 2: // Next week.
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=2&" + Constraint.Version1.WEEKS_FROM_THIS_WEEKS_END+"=1" + (mDialogCheckBox.isChecked()?'&'+Constraint.Version1.INCLUDE_NO_DUE_DATE_ITEMS+"=true":""));
							break;
						case 1: // Next 30 days.
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=1&" + Constraint.Version1.DAYS_FROM_TODAYS_END+"=30" + (mDialogCheckBox.isChecked()?'&'+Constraint.Version1.INCLUDE_NO_DUE_DATE_ITEMS+"=true":""));
							break;
						case 0: // Anytime.
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=0&" + Constraint.Version1.ANYTIME+"=true" + (mDialogCheckBox.isChecked()?'&'+Constraint.Version1.INCLUDE_NO_DUE_DATE_ITEMS+"=true":""));
							break;
						default:
							Log.e(TAG, "ERR000AD Unknown due date filter index " + mNewSelectedIndex);
							ErrorUtil.handleExceptionNotifyUser("ERR000AD", (Exception)(new Exception( String.valueOf(mNewSelectedIndex) )).fillInStackTrace(), mContext);
							return;
					}
					int count = mContext.getContentResolver().update(mFilterElementUri, cv, null, null);
					if( 1 != count ){
						Log.e(TAG, "ERR000A7 Failed to update due date for URI");
						ErrorUtil.handleExceptionNotifyUser("ERR000A7", (Exception)(new Exception( mFilterElementUri.toString() )).fillInStackTrace(), mContext);
						return;
					}
					FilterUtil.applyFilterBits(mContext);

					mOrigSelectedIndex = mNewSelectedIndex;
					mOrigToggle = mDialogCheckBox.isChecked(); 

					// Update mSelectedLabelUri in tag.
					(mTag).setData( Uri.parse(Constraint.Version1.DUE_CONTENT_URI_STRING + "?" + cv.getAsString(Task.FilterElement._PARAMETERS)));
					
				}else if( 0 <= which ){
					mNewSelectedIndex = which;
				}
			// HandledException is handled elsewhere --> }catch(HandledException h){ // Ignore.
			}catch(Exception exp){
				Log.e(TAG, "ERR0002L", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR0002L", exp, mContext);
			}
		}

		private void handleCancel(DialogInterface dialog) {
			mNewSelectedIndex = mOrigSelectedIndex;
			mDialogCheckBox.setChecked(mOrigToggle); 
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
		
		public boolean isOrigToggle() {
			return mOrigToggle;
		}
		public void setOrigToggle(boolean origToggle) {
			mOrigToggle = origToggle;
		}

		public void onCancel(DialogInterface dialog) {
			handleCancel(dialog);
		}		
	}
	SelectDueDateOnClickListener clickListener = null;
	View includeNotDueDateItemsView = null;
	
	public Dialog onCreateDialog(int dialogId){
		try{
			switch(dialogId){
				case SELECT_DUE_DATE_DIALOG_ID:
					// Can't use this becuse the selected item in the dialog may change and I don't know how to set the selected item in the dialog.
//					long filterElementId = (Long)((Object[]) tag)[FilterElementListAdapter.TAG_FILTER_ELEMENT_ID_INDEX];
//					Uri filterElementUri = ContentUris.withAppendedId(Task.FilterElement.CONTENT_URI, filterElementId);
//					Uri mSelectedLabelUri = (Uri)((Object[]) tag)[FilterElementListAdapter.TAG_URI_INDEX];
					
					// Prepare custom title
					if( includeNotDueDateItemsView == null ){
						includeNotDueDateItemsView = inflateIncludeNotDueItemsView();
					}
					int selectedIndex = 0;
					if( null != mSelectedLabelUri ){
						selectedIndex = null==mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER)?0:Integer.parseInt(mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER));
					}else{
						// TODO: Is this a bad condition? 
					}
					clickListener = createClickListener(includeNotDueDateItemsView, selectedIndex);
					
					AlertDialog dialog =  new AlertDialog.Builder(mActivity)
					.setCustomTitle(includeNotDueDateItemsView)
					.setSingleChoiceItems(
							R.array.array_dueDateFilter, 
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

	// TODO: !!! Consider making the entire "Include no due date tasks?" line clickable so the check box is easier to toggle. 
	// TODO: !!! Consider replacing "Include no due date tasks?" line with more clear statement.
	private View inflateIncludeNotDueItemsView() {
		LayoutInflater lf = (LayoutInflater)mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return lf.inflate(R.layout.due_date_label_config_dialog_title, null);
	}

	// NOTE: This is called twice during a config change. First because of the restore, and then again as the dialog is created.
	// TODO: !! Is there a cleaner way to do this?
	private SelectDueDateOnClickListener createClickListener(View includeNotDueDateItemsView, int selectedIndex) {
		if( null == clickListener ){ // This is very important because it prevents the intialization by the createDialog(..) from overriding the restored state.
			CheckBox dialogCheckBox = (CheckBox)includeNotDueDateItemsView.findViewById(R.id.due_date_label_config_dialog_checkbox);
			dialogCheckBox.setChecked(null!=mSelectedLabelUri.getQueryParameter(Constraint.Version1.INCLUDE_NO_DUE_DATE_ITEMS));
			
			clickListener = new SelectDueDateOnClickListener(mActivity, selectedIndex, mFilterElementUri, dialogCheckBox, (Intent) tag);
		}
		return clickListener;
	}

	public void onPrepareDialog(int dialogId, Dialog dialog){
	}
	
	@Override
	public boolean hasInstanceState() {
		return true;
	}

	private static final String SAVE_SELECTED_LABEL_URI 	= "DueDateFilterElementDelegate.SAVE_SELECTED_LABEL_URI";
	private static final String SAVE_FILTER_ELEMENT_URI 	= "DueDateFilterElementDelegate.SAVE_FILTER_ELEMENT_URI";
	private static final String SAVE_TAG 					= "DueDateFilterElementDelegate.SAVE_TAG";
	private static final String SAVE_NEW_SELECTED_INDEX 	= "DueDateFilterElementDelegate.SAVE_NEW_SELECTED_INDEX";
	private static final String SAVE_ORIGINAL_SELECTED_INDEX= "DueDateFilterElementDelegate.SAVE_ORIGINAL_SELECTED_INDEX";
	private static final String SAVE_ORIGINAL_TOGGLE 		= "DueDateFilterElementDelegate.SAVE_ORIGINAL_TOGGLE";
	public void  onSaveInstanceState  (Bundle outState){
		//Log.v(TAG, "onSaveInstanceState(..) called.");
		outState.putParcelable(SAVE_SELECTED_LABEL_URI, mSelectedLabelUri);
		outState.putParcelable(SAVE_FILTER_ELEMENT_URI, mFilterElementUri);
		outState.putParcelable(SAVE_TAG, (Intent)tag);
		if( null != clickListener ){
			outState.putInt(SAVE_NEW_SELECTED_INDEX, clickListener.getNewSelectedIndex());
			outState.putInt(SAVE_ORIGINAL_SELECTED_INDEX, clickListener.getOrigSelectedIndex());
			outState.putBoolean(SAVE_ORIGINAL_TOGGLE, clickListener.isOrigToggle());
		}
	}
	
	public void  onRestoreInstanceState  (Bundle savedInstanceState){
		//Log.v(TAG, "onRestoreInstanceState(..) called.");
		mSelectedLabelUri = savedInstanceState.getParcelable(SAVE_SELECTED_LABEL_URI);
		mFilterElementUri = savedInstanceState.getParcelable(SAVE_FILTER_ELEMENT_URI);
		tag = savedInstanceState.getParcelable(SAVE_TAG);
		
//		assert null != clickListener;
		int newSelectedIndex = savedInstanceState.getInt(SAVE_NEW_SELECTED_INDEX, -1);
		int selectedIndex = savedInstanceState.getInt(SAVE_ORIGINAL_SELECTED_INDEX, -1);
		boolean origToggle = savedInstanceState.getBoolean(SAVE_ORIGINAL_TOGGLE, false);
		if( null == includeNotDueDateItemsView ){
			includeNotDueDateItemsView = inflateIncludeNotDueItemsView();
		}
		if( savedInstanceState.containsKey(SAVE_NEW_SELECTED_INDEX) && null != mSelectedLabelUri ){
			assert null == clickListener;
			clickListener = createClickListener(includeNotDueDateItemsView, selectedIndex);
			clickListener.setNewSelectedIndex(newSelectedIndex);
			clickListener.setOrigToggle(origToggle);
		}
	}

}
