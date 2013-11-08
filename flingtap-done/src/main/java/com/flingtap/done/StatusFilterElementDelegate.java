// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.Calendar;

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
import android.location.Address;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * TODO: !!! Write some automated tests for the "Completed only" choice, I think there might be a bug.
 */
public class StatusFilterElementDelegate 
		extends FilterElementDelegatingListAdapterDelegate 
		implements  View.OnCreateContextMenuListener{

	public static final String TAG = "StatusFilterElementDelegate";

	private final static int FIRST_CODE_ID = 1600;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}

	// Dialog IDs
	protected final static int SELECT_STATUS_DIALOG_ID = FIRST_CODE_ID + 50;

	final Calendar mFromDate = Calendar.getInstance();
	final Calendar mToDate = Calendar.getInstance();
	
	protected final static String FILTER_ELEMENT_INDEX_PARAMETER = "INDEX";

	public StatusFilterElementDelegate(){
		uriDelegateMapping = new UriDelegateMapping[1];
		uriDelegateMapping[0] = new UriDelegateMapping();
		uriDelegateMapping[0].authority = Task.AUTHORITY;
		uriDelegateMapping[0].pathPattern = Constraint.Version1.STATUS_URI_PATTERN_STRING;
		uriDelegateMapping[0].code = 0; // Uniquely identifies this mapping. Some Attachment handlers may handle multiple different mime-types so this allows us to distinguish between them. The value is passed into bindView(..)
		// TODO: Couldn't the array index be used instead of adding the .code member? 
	}
	
	private Uri mSelectedLabelUri = null;
	private Uri mFilterElementUri = null;
	private Object tag = null;		

	@Override
	protected void bindView(final View view, Context context, Cursor cursor,
			int code, Uri data) {
		
		// Hide header title.
		view.findViewById(R.id.header_title).setVisibility(View.GONE);
		
		// **************************************
		// List item's text
		// **************************************
		// Hide dual line layout is gone.
		view.findViewById(R.id.label_list_item_single_line_text).setVisibility(View.GONE);
		// Hide toggle button.
		view.findViewById(R.id.label_list_item_toggle_button_layout).setVisibility(View.GONE);


		view.findViewById(R.id.label_list_item_dual_line_text).setVisibility(View.VISIBLE);
		TextView primaryLineText = (TextView) view.findViewById(R.id.label_list_item_dual_line_text_1);
		TextView secondaryLineText = (TextView) view.findViewById(R.id.label_list_item_dual_line_text_2);
		
		tag = view.getTag();
		
		mSelectedLabelUri = ((Intent) view.getTag()).getData();
		int mSelectedIndex = null==mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER)?Integer.parseInt(Constraint.Version1.STATUS_PARAM_OPTION_DEFAULT_VALUE):Integer.parseInt(mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER));
		updateLabelText(context, primaryLineText, mSelectedIndex);
		
		long filterElementId = ((Intent) view.getTag()).getLongExtra(FilterElementListAdapter.TAG_FILTER_ELEMENT_ID_INDEX, Constants.DEFAULT_NON_ID);
		assert Constants.DEFAULT_NON_ID != filterElementId;
		mFilterElementUri = ContentUris.withAppendedId(Task.FilterElement.CONTENT_URI, filterElementId);
		
		String description = context.getString(R.string.areTheTasksCompletedOrNot);
		assert null != description;
		secondaryLineText.setText(description);
	}

	private static void updateLabelText(Context context, TextView primaryLineText, int selectedIndex) {
		primaryLineText.setText( TextUtils.expandTemplate(context.getText(R.string.statusX), context.getResources().getStringArray(R.array.array_statusFilter)[selectedIndex]) );
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
		// Prepare event info.
		Event.onEvent(Event.OPEN_STATUS_FILTER_ELEMENT, null); // Map<String,String> parameters = new HashMap<String,String>();

		mActivity.showDialog(SELECT_STATUS_DIALOG_ID);
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
            // Do nothing.
        }
        return false;
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}
	
	// ********************************************************************
	// 
	// ********************************************************************
	final android.widget.DatePicker.OnDateChangedListener fromDatePickerOnDateChangedListener = new android.widget.DatePicker.OnDateChangedListener() {
		public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			try{
				//Log.v(TAG, "fromDatePickerOnDateChangedListener.onDateChanged(..) called");
				
				mFromDate.set(Calendar.YEAR, year);
				mFromDate.set(Calendar.MONTH, monthOfYear);
				mFromDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		    	mFromDate.set(Calendar.HOUR_OF_DAY, 0);
		    	mFromDate.set(Calendar.MINUTE, 0);
		    	mFromDate.set(Calendar.SECOND, 0);
		    	mFromDate.set(Calendar.MILLISECOND, 0);     				
				mFromDate.getTimeInMillis(); // I think this is needed to make insure that the Calendar calculates the value now. 
				//Log.v("mFromDate", String.valueOf(mFromDate.getTimeInMillis()) + " onDateChanged id="+view.getId());

			}catch(HandledException h){ // Ignore.
			}catch(Exception exp){
				Log.e(TAG, "ERR0006E", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR0006E", exp, mActivity);
			}
		}           
	};
	final android.widget.DatePicker.OnDateChangedListener toDatePickerOnDateChangedListener = new android.widget.DatePicker.OnDateChangedListener() {
		public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			try{
				// Log.v(TAG, "toDatePickerOnDateChangedListener.onDateChanged(..) called");
				
				mToDate.set(Calendar.YEAR, year);
				mToDate.set(Calendar.MONTH, monthOfYear);
				mToDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);		
		    	mToDate.set(Calendar.HOUR_OF_DAY, 23);
		    	mToDate.set(Calendar.MINUTE, 59);
		    	mToDate.set(Calendar.SECOND, 59);
		    	mToDate.set(Calendar.MILLISECOND, 999);		
		    	mToDate.getTimeInMillis();

			}catch(HandledException h){ // Ignore.
			}catch(Exception exp){
				Log.e(TAG, "ERR0006F", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR0006F", exp, mActivity);
			}
		}           
	};				
	
	private DatePicker mFromDatePicker = null;
	private DatePicker mToDatePicker = null;
	SelectStatusFilterOnClickListener clickListener = null;

	public Dialog onCreateDialog(int dialogId){
		try{
			switch(dialogId){
				case SELECT_STATUS_DIALOG_ID:
					
					int mSelectedIndex = null==mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER)?0:Integer.parseInt(mSelectedLabelUri.getQueryParameter(FILTER_ELEMENT_INDEX_PARAMETER));
					
					// One week ago in milliseconds (604800000 = 7*24*60*60*1000)

					clickListener = createClickListener(calculateFromDate(), calculateToDate(), mSelectedIndex);

					// TODO: !!! No idea why this happens. Occassionally, when selecting an item from the list, all of the from/to date picker _text_ will go blank. Selecting on the same or a different list item causes the text to re-appear.   
					BaseAdapter statusAdapter = new LeanAdapter<Object>(mActivity, R.layout.status_filter_element_options_dialog_list_item) {
						
						public int getCount() {
							return 4;
						}
						
						public Object getItem(int position) {
							return null;
						}
						public void bindView(View view, Context context, Object data, int position) {
							try{
								View relativeLayout = view.findViewById(R.id.status_filter_element_options_list_item_date_range_config);
								switch(position){
									case 0:
										((TextView) view.findViewById(R.id.status_filter_element_options_list_item_dual_line_text_1)).setText(context.getString(R.string.dialog_all));
										((TextView) view.findViewById(R.id.status_filter_element_options_list_item_dual_line_text_2)).setText(context.getString(R.string.dialog_bothCompletedAndIncompleteTasks));
										relativeLayout.setVisibility(View.GONE);
										break;
									case 1:
										((TextView) view.findViewById(R.id.status_filter_element_options_list_item_dual_line_text_1)).setText(context.getString(R.string.dialog_completed));
										((TextView) view.findViewById(R.id.status_filter_element_options_list_item_dual_line_text_2)).setText(context.getString(R.string.dialog_onlyCompletedTasks));
										relativeLayout.setVisibility(View.GONE);
										break;
									case 2:
										((TextView) view.findViewById(R.id.status_filter_element_options_list_item_dual_line_text_1)).setText(context.getString(R.string.dialog_incomplete));
										((TextView) view.findViewById(R.id.status_filter_element_options_list_item_dual_line_text_2)).setText(context.getString(R.string.dialog_onlyIncompleteTasks));
										relativeLayout.setVisibility(View.GONE);
										break;
									case 3: // TODO: !!! Consider hiding the date range unless this item is selected,, then use an animation to expand it. 
										((TextView) view.findViewById(R.id.status_filter_element_options_list_item_dual_line_text_1)).setText(context.getString(R.string.dialog_dateRange));
										((TextView) view.findViewById(R.id.status_filter_element_options_list_item_dual_line_text_2)).setText(context.getString(R.string.dialog_tasksCompletedWithinADateRange));
										
										// Set date and time here
										// TODO: !!! Consider a more efficient way to include the DatePicker. Maybe use a ViewStub or the like.  
										// TODO: !!! Consider removing the OnDateChangedListener and just asking for the value when the user commits.
										mFromDatePicker = ((DatePicker) view.findViewById(R.id.from_date_picker));
										mToDatePicker   = ((DatePicker) view.findViewById(R.id.to_date_picker));

										mFromDatePicker.init(mFromDate.get(Calendar.YEAR), mFromDate.get(Calendar.MONTH), mFromDate.get(Calendar.DAY_OF_MONTH), fromDatePickerOnDateChangedListener);
										mToDatePicker.init(mToDate.get(Calendar.YEAR), mToDate.get(Calendar.MONTH), mToDate.get(Calendar.DAY_OF_MONTH), toDatePickerOnDateChangedListener);

										//Log.v("mFromDate", String.valueOf(mFromDate.getTimeInMillis()) + " bindView id="+mFromDatePicker.getId());
										
										relativeLayout.setVisibility(View.VISIBLE);
										break;
									default:
								}
							}catch(HandledException h){ // Ignore.
							}catch(Exception exp){
								Log.e(TAG, "ERR0006D", exp);
								ErrorUtil.handleExceptionFinish("ERR0006D", exp, mActivity);
							}
							
						}

						@Override
						public long getItemId(int position) {
							return position;
						}
						
					};

					LayoutInflater lf = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					
					View title = lf.inflate(R.layout.dialog_builder_default_title, null);
					ImageView iv = (ImageView) title.findViewById(R.id.dialog_builder_title_icon);
					iv.setImageResource(android.R.drawable.ic_dialog_info);
					TextView dialogTitleText = (TextView) title.findViewById(R.id.dialog_builder_title_text); 
					dialogTitleText.setText(R.string.dialog_taskStatus);
					// dialogTitleText will be initialized later in onPrepareDialog(..) with  prepareDialogTitle();
					
					AlertDialog dialog = new AlertDialog.Builder(mActivity)
					.setCustomTitle(title)
					.setSingleChoiceItems(statusAdapter, mSelectedIndex, clickListener)
					.setPositiveButton(R.string.button_ok, clickListener)
					.setNegativeButton(R.string.button_cancel, clickListener)
					.setOnCancelListener(clickListener)
					.create();
					return dialog;
			}
			
		}catch(Exception exp){
			Log.e(TAG, "ERR000A9", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000A9", exp, mActivity);
		}
		return null;
	}

	private long calculateToDate() {
		return null==mSelectedLabelUri.getQueryParameter(Constraint.Version1.STATUS_PARAM_TO_DATE)  ?System.currentTimeMillis()          :Long.parseLong(mSelectedLabelUri.getQueryParameter(Constraint.Version1.STATUS_PARAM_TO_DATE));
	}

	/**
	 * 
	 * 604800000 is 7 days in milliseconds. It is used to make the default date range cover the past week.
	 */
	private long calculateFromDate() {
		return null==mSelectedLabelUri.getQueryParameter(Constraint.Version1.STATUS_PARAM_FROM_DATE)?System.currentTimeMillis()-604800000:Long.parseLong(mSelectedLabelUri.getQueryParameter(Constraint.Version1.STATUS_PARAM_FROM_DATE));
	}
	
	// NOTE: This is called twice during a config change. First because of the restore, and then again as the dialog is created.
	private SelectStatusFilterOnClickListener createClickListener(long fromDate, long toDate, int selectedIndex) {
		if( null == clickListener ){ // This is very important because it prevents the intialization by the createDialog(..) from overriding the restored state.
			
			mFromDate.setTimeInMillis(fromDate);
			mToDate.setTimeInMillis(toDate);
			//Log.v("mFromDate", String.valueOf(mFromDate.getTimeInMillis()) + " createClickListener");

			clickListener = new SelectStatusFilterOnClickListener(mActivity, selectedIndex, mFilterElementUri, (Intent) tag);
		}
		return clickListener;
	}

	public void onPrepareDialog(int dialogId, Dialog dialog){
		
		mFromDate.setTimeInMillis(calculateFromDate());
		mToDate.setTimeInMillis(calculateToDate());
		
		if( null != mFromDatePicker ){
			mFromDatePicker.updateDate(mFromDate.get(Calendar.YEAR), mFromDate.get(Calendar.MONTH), mFromDate.get(Calendar.DAY_OF_MONTH));
			// Log.v("mFromDate",  String.valueOf(mFromDate.getTimeInMillis()) + " onPrepareDialog");
		}
		if( null != mToDatePicker ){
			mToDatePicker.updateDate(mToDate.get(Calendar.YEAR), mToDate.get(Calendar.MONTH), mToDate.get(Calendar.DAY_OF_MONTH));
		}
		((AlertDialog)dialog).getListView().invalidateViews();
	}

	
	private class SelectStatusFilterOnClickListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener{
		private int mOrigSelectedIndex = -1;
		private int mNewSelectedIndex = -1;
		private Uri mFilterElementUri = null;
		private Context mContext = null;
		private Intent mTag = null;
		
		public SelectStatusFilterOnClickListener(Context context, int selectedIndex, Uri filterElementUri, Intent tag){
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
					if( mNewSelectedIndex == mOrigSelectedIndex && 3 != mNewSelectedIndex ){
						return;
					}
					
					// Prepare event info.
					Event.onEvent(Event.EDIT_STATUS_FILTER_ELEMENT, null); // Map<String,String> parameters = new HashMap<String,String>();

					String fromToString = 
						Constraint.Version1.STATUS_PARAM_FROM_DATE + "=" + mFromDate.getTimeInMillis() +"&" + 
						Constraint.Version1.STATUS_PARAM_TO_DATE   + "=" + mToDate.getTimeInMillis();						
					
					ContentValues cv = new ContentValues(1);
					switch(mNewSelectedIndex){
						case 0: // Completed
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=0&" + Constraint.Version1.STATUS_PARAM_OPTION+"=0&"+fromToString);
							break;
						case 1: // Completed
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=1&" + Constraint.Version1.STATUS_PARAM_OPTION+"=1&"+fromToString);
							break;
						case 2: // Not completed
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=2&" + Constraint.Version1.STATUS_PARAM_OPTION+"=2&"+fromToString);
							break;
						case 3: // Date range
							// Could be more effecient here by considering whether the from/to dates have changed and skiping if not needed.
							cv.put(Task.FilterElement._PARAMETERS, FILTER_ELEMENT_INDEX_PARAMETER + "=3&" + Constraint.Version1.STATUS_PARAM_OPTION+"=3&"+fromToString );
							break;
						default:
							Log.e(TAG, "ERR000AC Unknown status " + mNewSelectedIndex);
							ErrorUtil.handleExceptionNotifyUser("ERR000AC", (Exception)(new Exception( String.valueOf(mNewSelectedIndex) )).fillInStackTrace(), mContext);
							return;
					}
					int count = mContext.getContentResolver().update(mFilterElementUri, cv, null, null);
					if( 1 != count ){
						Log.e(TAG, "ERR000D2"); // Failed to update status for URI
						ErrorUtil.handleExceptionNotifyUser("ERR000D2", (Exception)(new Exception( mFilterElementUri.toString() )).fillInStackTrace(), mContext);
						return;
					}
					FilterUtil.applyFilterBits(mContext);

					mOrigSelectedIndex = mNewSelectedIndex;

					// Update mSelectedLabelUri in tag.
					(mTag).setData(Uri.parse(Constraint.Version1.STATUS_CONTENT_URI_STRING + "?" + cv.getAsString(Task.FilterElement._PARAMETERS)));
					mSelectedLabelUri = ((Intent)tag).getData();

				}else if( 0 <= which ){
					mNewSelectedIndex = which;
				}
			}catch(HandledException h){ // Ignore.
			}catch(Exception exp){
				Log.e(TAG, "ERR0006G", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR0006G", exp, mActivity);
			}
		}

		
		private void handleCancel(DialogInterface dialog) {
			mNewSelectedIndex = mOrigSelectedIndex;
			
			// Reset date and time here
			mFromDate.setTimeInMillis(calculateFromDate());
			mToDate.setTimeInMillis(calculateToDate());
			mFromDatePicker.init(mFromDate.get(Calendar.YEAR), mFromDate.get(Calendar.MONTH), mFromDate.get(Calendar.DAY_OF_MONTH), fromDatePickerOnDateChangedListener);
			mToDatePicker.init(mToDate.get(Calendar.YEAR), mToDate.get(Calendar.MONTH), mToDate.get(Calendar.DAY_OF_MONTH), toDatePickerOnDateChangedListener);
			//Log.v("mFromDate", String.valueOf(mFromDate.getTimeInMillis()) + " handleCancel id="+mFromDatePicker.getId());

			ListView listView = ((AlertDialog)dialog).getListView();
			
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
	
	@Override
	public boolean hasInstanceState() {
		return true;
	}
	
	// NOTE: The prefix "StatusFilterElementDelegate" is very important to prevent mixing with other participants.
	private static final String SAVE_SELECTED_LABEL_URI 	= "StatusFilterElementDelegate.SAVE_SELECTED_LABEL_URI";
	private static final String SAVE_FILTER_ELEMENT_URI 	= "StatusFilterElementDelegate.SAVE_FILTER_ELEMENT_URI";
	private static final String SAVE_TAG 					= "StatusFilterElementDelegate.SAVE_TAG";
	private static final String SAVE_NEW_SELECTED_INDEX 	= "StatusFilterElementDelegate.SAVE_NEW_SELECTED_INDEX";
	private static final String SAVE_ORIGINAL_SELECTED_INDEX= "StatusFilterElementDelegate.SAVE_ORIGINAL_SELECTED_INDEX";
	private static final String SAVE_NEW_FROM_DATE 			= "StatusFilterElementDelegate.SAVE_NEW_FROM_DATE";
	private static final String SAVE_NEW_TO_DATE 			= "StatusFilterElementDelegate.SAVE_NEW_TO_DATE";

	
	public void  onSaveInstanceState  (Bundle outState){
		//Log.v(TAG, "onSaveInstanceState(..) called.");
		outState.putParcelable(SAVE_SELECTED_LABEL_URI, mSelectedLabelUri);
		outState.putParcelable(SAVE_FILTER_ELEMENT_URI, mFilterElementUri);
		outState.putParcelable(SAVE_TAG, (Intent)tag);
		if( null != clickListener ){
			outState.putInt(SAVE_NEW_SELECTED_INDEX, clickListener.getNewSelectedIndex());
			outState.putInt(SAVE_ORIGINAL_SELECTED_INDEX, clickListener.getOrigSelectedIndex());
			outState.putLong(SAVE_NEW_FROM_DATE, mFromDate.getTimeInMillis());
			outState.putLong(SAVE_NEW_TO_DATE, mToDate.getTimeInMillis());
			//Log.v("mFromDate", String.valueOf(mFromDate.getTimeInMillis()) + " onSaveInstanceState");
		}
	}
	
	public void  onRestoreInstanceState  (Bundle savedInstanceState){
		//Log.v(TAG, "onRestoreInstanceState(..) called.");
		mSelectedLabelUri = savedInstanceState.getParcelable(SAVE_SELECTED_LABEL_URI);
		mFilterElementUri = savedInstanceState.getParcelable(SAVE_FILTER_ELEMENT_URI);
		tag = savedInstanceState.getParcelable(SAVE_TAG);
		int selectedIndex = savedInstanceState.getInt(SAVE_ORIGINAL_SELECTED_INDEX, -1);
		if( -1 != selectedIndex ){
			assert null == clickListener;
			
			long fromDate = savedInstanceState.getLong(SAVE_NEW_FROM_DATE);
			long toDate = savedInstanceState.getLong(SAVE_NEW_TO_DATE);
			//Log.v("mFromDate", String.valueOf(fromDate) + " onRestoreInstanceState");
			
			clickListener = createClickListener(fromDate, toDate, selectedIndex);
			int newSelectedIndex = savedInstanceState.getInt(SAVE_NEW_SELECTED_INDEX, -1);
			clickListener.setNewSelectedIndex(newSelectedIndex);
		}
	}
}
