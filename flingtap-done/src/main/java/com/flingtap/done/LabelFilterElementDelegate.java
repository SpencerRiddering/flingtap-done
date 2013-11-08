// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;
import com.flingtap.done.util.Constants;

import java.lang.ref.WeakReference;


/**
 * Handles both label filter elements and the un-labeled filter element.
 *
 * 
 */
public class LabelFilterElementDelegate 
		extends FilterElementDelegatingListAdapterDelegate 
		implements  View.OnCreateContextMenuListener, CompoundButton.OnCheckedChangeListener{

	public static final String TAG = "LabelFilterElementDelegate";
	
	private static final String TAG_CODE = "TAG_CODE"; // TODO: !! Describe what this is for.

	private static final int CODE_LABEL = 0;
	private static final int CODE_UNLABELD = 1;
	

	private final static int FIRST_CODE_ID = 1800;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}
	
	// Item IDs
	protected final static int LABEL_MENU_DELETE_ITEM_ID = FIRST_CODE_ID + 10;
	protected final static int LABEL_MENU_RENAME_ITEM_ID = FIRST_CODE_ID + 11;
	protected final static int LABEL_MENU_VIEW_ITEM_ID   = FIRST_CODE_ID + 12;
	
	// Result Codes
	protected final static int DELETE_LABEL_REQUEST      = FIRST_CODE_ID + 98;

	// Dialog IDs
	protected final static int RENAME_LABEL_DIALOG_ID = FIRST_CODE_ID + 50;
	protected final static int DELETE_LABEL_DIALOG_ID = FIRST_CODE_ID + 51;
	
	protected final static String FILTER_ELEMENT_INDEX_PARAMETER = "INDEX"; // TODO: How would this applied?
	public LabelFilterElementDelegate(){

		uriDelegateMapping = new UriDelegateMapping[2];
		uriDelegateMapping[0] = new UriDelegateMapping();
		uriDelegateMapping[0].authority = Task.AUTHORITY;
		uriDelegateMapping[0].pathPattern = "tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+Constraint.Version1.LABEL+"/#";
		uriDelegateMapping[0].code = CODE_LABEL; // Uniquely identifies this mapping. Some Attachment handlers may handle multiple different mime-types so this allows us to distinguish between them. The value is passed into bindView(..)
		
		uriDelegateMapping[1] = new UriDelegateMapping();
		uriDelegateMapping[1].authority = Task.AUTHORITY;
		uriDelegateMapping[1].pathPattern = "tasks/"+Constraint.CONSTRAINT+"/"+Constraint.Version1.VERSION+"/"+Constraint.Version1.UNLABELED;
		uriDelegateMapping[1].code = CODE_UNLABELD; // Uniquely identifies this mapping. Some Attachment handlers may handle multiple different mime-types so this allows us to distinguish between them. The value is passed into bindView(..)
//		// TODO: Couldn't the array index be used instead of adding the .code member? 
	}
	
	@Override
	protected void bindView(final View view, Context context, Cursor cursor,
			int code, Uri data) {

		// Hide header title.
		view.findViewById(R.id.header_title).setVisibility(View.GONE);

		// **************************************
		// Label display text
		// **************************************
		TextView singleLineText = (TextView) view.findViewById(R.id.label_list_item_single_line_text);

		
		ToggleButton tButton = (ToggleButton)view.findViewById(R.id.label_list_item_toggle_button);
		tButton.setTag(view.getTag());	
		
		
		switch(code){
			case 0: // Regular label.
				// TODO: !! FilterElements don't have a name in their database record so I don't know what to display here now (was able to pull from the label name).
				String uriString = cursor.getString(FilterElementListActivity.PROJ_FILTER_ELEMENT__CONSTRAINT_INDEX);
				assert null != uriString;
				Uri uri = Uri.parse(uriString);
				String labelId = uri.getLastPathSegment();

				String name = null;
				String description = "";
				Uri correctUri = Uri.withAppendedPath(Task.Labels.CONTENT_URI,labelId);
				Cursor displayTextCursor = mActivity.getContentResolver().query(
						correctUri, 
						new String[]{Task.LabelsColumns.DISPLAY_NAME, Task.LabelsColumns.DESCRIPTION},
						null, 
						null, 
						Task.LabelsColumns._ID);
				if( ! displayTextCursor.moveToFirst() ){
					DefaultFilterElementDelegate.setupErrorView(mActivity, view);
					Exception exp = (Exception)(new Exception().fillInStackTrace());
					Log.e(TAG, "ERR0003A", exp);
					ErrorUtil.handleExceptionAndThrow("ERR0003A", exp, context);
				}
				name = displayTextCursor.getString(0);
				description = displayTextCursor.getString(1);
				displayTextCursor.close();
				
				if( null == name ){ // "Error. Unknown delegate type."
					DefaultFilterElementDelegate.setupErrorView(mActivity, view);
					Exception exp = (Exception)(new Exception().fillInStackTrace());
					Log.e(TAG, "ERR00039", exp);
					ErrorUtil.handleExceptionAndThrow("ERR00039", exp, context);
				}
				
				if( null == description || description.trim().length() == 0 ){ // TODO: !! This value is "" rather than null, maybe not a big deal.
					setSingleLineText(view, singleLineText, name);
				}else{
					setDualLineText(view, singleLineText, name, description);
				}
				
				long filterElementId = ((Intent)view.getTag()).getLongExtra(FilterElementListAdapter.TAG_FILTER_ELEMENT_ID_INDEX, Constants.DEFAULT_NON_ID);
				
				tButton.setOnCheckedChangeListener(null); // Need to remove listener before setting value, otherwise the onCheckChangedListener gets called.
				tButton.setChecked(Long.MAX_VALUE != filterElementId);
				
				break;
			case 1: // Unlabeled.
				setDualLineText(view, singleLineText, mActivity.getString(R.string.unlabeled), context.getString(R.string.tasksWithNoLabel));

				final Uri mConstraintUri = ((Intent) view.getTag()).getData();

				int enabledIntValue = null==mConstraintUri.getQueryParameter(Constraint.Version1.LABEL_PARAM_ENABLED)?Integer.parseInt(Constraint.Version1.LABEL_PARAM_ENABLED_VALUE_TRUE):Integer.parseInt(mConstraintUri.getQueryParameter(Constraint.Version1.LABEL_PARAM_ENABLED));
				boolean included = enabledIntValue!=0;
				
				tButton.setOnCheckedChangeListener(null); // Need to remove listener before setting value, otherwise the onCheckChangedListenr gets called. 
				tButton.setChecked(included);
				break;
			default:
				DefaultFilterElementDelegate.setupErrorView(mActivity, view);
				return;
		}

		((Intent)tButton.getTag()).putExtra(TAG_CODE, code);
		
		// Ensure the toggle button is visible.
		view.findViewById(R.id.label_list_item_toggle_button_layout).setVisibility(View.VISIBLE);

		tButton.setOnCheckedChangeListener(this);	
	}
	

	private int findDelegateCode(View v) {
		return ((Intent)v.getTag()).getIntExtra(TAG_CODE,Integer.MAX_VALUE);
	}


	private void setDualLineText(final View view, TextView singleLineText, String name, String description) {
		singleLineText.setVisibility(View.GONE);
		view.findViewById(R.id.label_list_item_dual_line_text).setVisibility(View.VISIBLE);
		
		TextView primaryLineText = (TextView) view.findViewById(R.id.label_list_item_dual_line_text_1);
		primaryLineText.setText(name);
		TextView secondaryLineText = (TextView) view.findViewById(R.id.label_list_item_dual_line_text_2);
		secondaryLineText.setText(description);
	}

	private void setSingleLineText(final View view, TextView singleLineText, String name) {
		singleLineText.setVisibility(View.VISIBLE);
		singleLineText.setText(name);
		view.findViewById(R.id.label_list_item_dual_line_text).setVisibility(View.GONE);
	}
	
	static String getFilterElementParameters(final boolean enabled) {
		String parameters = FILTER_ELEMENT_INDEX_PARAMETER + "=10&" + Constraint.Version1.LABEL_PARAM_ENABLED+"="+(enabled?Constraint.Version1.LABEL_PARAM_ENABLED_VALUE_TRUE:Constraint.Version1.LABEL_PARAM_ENABLED_VALUE_FALSE);
		return parameters;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo){
    	//Log.v(TAG, "onCreateContextMenu(..) called.");
		
    	AdapterView.AdapterContextMenuInfo adapterMenuInfo = (AdapterView.AdapterContextMenuInfo)menuInfo;
    	Uri constraintUri = ((Intent)((adapterMenuInfo.targetView).getTag())).getData();
    	assert null != constraintUri;
    	if( constraintUri.toString().startsWith(Constraint.Version1.UNLABELED_CONTENT_URI_STRING) ){
    		// Unlabeled.
    		return;
    	}
    	
    	// TODO: !!! This is just messy code. Consolidate the single and dual line layouts to simplify the code.  
    	TextView singleLineText = (TextView) adapterMenuInfo.targetView.findViewById(R.id.label_list_item_single_line_text);
    	if( singleLineText.getVisibility() == View.GONE ){
    		TextView primaryLineText = (TextView) adapterMenuInfo.targetView.findViewById(R.id.label_list_item_dual_line_text_1);
    		menu.setHeaderTitle(primaryLineText.getText());
    	}else{
    		menu.setHeaderTitle(singleLineText.getText());
    	}
    	
		MenuItem renameMenuItem = menu.add(Menu.NONE, LABEL_MENU_RENAME_ITEM_ID, 2, R.string.context_renameLabel);

		MenuItem deletMenuItem = menu.add(Menu.NONE, LABEL_MENU_DELETE_ITEM_ID, 3, R.string.context_deleteLabel);

	}

	/**
	 * 
	 */
	public void onListItemClick(ListView listview, View view, int position, long id) {
		// Log.v(TAG, "onListItemClick(..) called");
		
		ToggleButton toggleButton = (ToggleButton)view.findViewById(R.id.label_list_item_toggle_button);
		toggleButton.performClick();
	}

	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		// Log.v(TAG, "onActivityResult(..) called");
		if( resultCode == SharedConstant.RESULT_ERROR ){
			ErrorUtil.notifyUser(mActivity);
			return;
		}
	}

	long labelId = Constants.DEFAULT_NON_ID;
	public boolean onContextItemSelected(MenuItem item) {
		try{
			AdapterView.AdapterContextMenuInfo adapterMenuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			Uri constraintUri = ((Intent)((adapterMenuInfo.targetView).getTag())).getData();
			
			labelId = Long.parseLong(constraintUri.getLastPathSegment()); 
			
			switch (item.getItemId()) {
				case LABEL_MENU_DELETE_ITEM_ID:
					
					// Prepare event info.
					Event.onEvent(Event.FILTER_ELEMENT_LIST_DELETE_LABEL_CONTEXT_MENU_ITEM_CLICKED, null); // Map<String,String> parameters = new HashMap<String,String>();
					
					mActivity.showDialog(DELETE_LABEL_DIALOG_ID);

					return true;
				case LABEL_MENU_RENAME_ITEM_ID:
					
					// Prepare event info.
					Event.onEvent(Event.FILTER_ELEMENT_LIST_ADD_LABEL_OPTIONS_MENU_ITEM_CLICKED, null); // Map<String,String> parameters = new HashMap<String,String>();
					
					mActivity.showDialog(RENAME_LABEL_DIALOG_ID);
					return true;
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000AU", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000AU", exp, mActivity);
		}

        return false;
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}
	
	
	private WeakReference<LabelUtil.RenameLabelOnTextSetListener> renameLabelOnTextSetListener = null;
	public Dialog onCreateDialog(int dialogId){
		try{
			switch(dialogId){
				case RENAME_LABEL_DIALOG_ID:
					assert Constants.DEFAULT_NON_ID != labelId; 
					LabelUtil.RenameLabelOnTextSetListener listener = LabelUtil.createRenameLabelOnTextSetListener(mActivity, labelId);
					renameLabelOnTextSetListener = new WeakReference<LabelUtil.RenameLabelOnTextSetListener>(listener);
					return LabelUtil.onCreateDialogRenameLabel(mActivity, labelId, listener);
					
				case DELETE_LABEL_DIALOG_ID:	
					return new AlertDialog.Builder(mActivity)
			        .setTitle(R.string.dialog_confirmDelete)
			        .setIcon(android.R.drawable.ic_dialog_alert)
			        .setMessage(R.string.dialog_areYouSure)
			        .setPositiveButton(R.string.button_yes, new android.content.DialogInterface.OnClickListener(){
			    		public void onClick(DialogInterface dialog, int whichButton){
			    			try{
								assert Constants.DEFAULT_NON_ID != labelId; 
								LabelUtil.deleteUserAppliedLabel(mActivity, labelId);
								labelId = Constants.DEFAULT_NON_ID;					
			    			}catch(HandledException h){ // Ignore.
			    			}catch(Exception exp){
			    				Log.e(TAG, "ERR000C6", exp);
			    				ErrorUtil.handleExceptionNotifyUser("ERR000C6", exp, mActivity);
			    			}
			    		}
			    	})
			        .setNegativeButton(R.string.button_no, null)
			    	.create();		
					
					
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000AV", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000AV", exp, mActivity);
		}
		return null;
	}

	public void onPrepareDialog(int dialogId, Dialog dialog){
		try{
			switch(dialogId){
				case RENAME_LABEL_DIALOG_ID:
					assert Constants.DEFAULT_NON_ID != labelId;
					
					assert null != renameLabelOnTextSetListener;
					LabelUtil.RenameLabelOnTextSetListener listener = renameLabelOnTextSetListener.get();
					
					assert null != listener;
					listener.setId(labelId);
					
					LabelUtil.onPrepareDialogRenameLabel(mActivity, dialog, labelId);
					break;
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000AW", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000AW", exp, mActivity);
			dialog.dismiss(); // TODO: Is this the right method to use here? 
		}
	}
	
	public boolean hasInstanceState() {
		return true;
	}
	private static final String SAVE_LABEL_ID = "LabelFilterElementDelegate.SAVE_LABEL_ID";
	public void  onSaveInstanceState  (Bundle outState){
		outState.putLong(SAVE_LABEL_ID, labelId);
	}
	
	public void  onRestoreInstanceState  (Bundle savedInstanceState){
		labelId = savedInstanceState.getLong(SAVE_LABEL_ID);
	}

	public void onCheckedChanged(CompoundButton v, boolean isChecked) {
		try{
			switch(v.getId()){
				case R.id.label_list_item_toggle_button:
					long filterElementId = ((Intent)v.getTag()).getLongExtra(FilterElementListAdapter.TAG_FILTER_ELEMENT_ID_INDEX, Constants.DEFAULT_NON_ID);
					String filterId = ((Intent)v.getTag()).getStringExtra(FilterElementListAdapter.TAG_FILTER_ID_INDEX);
					if( Long.MAX_VALUE == filterElementId ){ // TODO: !!! This is a bit of a hack, the order of the filter elements is based on the _id so I had to set the ID for un-enabled labels to Long.MAX_VALUE. Really, there should be a separate column for controlling the presentation order (maybe _display_order).
						
						// Create the filter element record.
						
						Uri newFilterElementUri = null;
						newFilterElementUri = LabelUtil.createUserAppliedLabelFilterElement(mActivity, ((Intent)v.getTag()).getData().getLastPathSegment(), filterId);
								
						if( null != newFilterElementUri ){
							((Intent)v.getTag()).putExtra(FilterElementListAdapter.TAG_FILTER_ELEMENT_ID_INDEX, ContentUris.parseId(newFilterElementUri));
							notifyDataSetChanged();
						}
					}else{
						// FileterElement record exists and so the label _is_ applied. remove it to de-activate the label.
						Uri filterElementUri = ContentUris.withAppendedId(Task.FilterElement.CONTENT_URI, filterElementId);
						int count = 0;
						switch(findDelegateCode(v)){
							case CODE_UNLABELD:
								ContentValues cv = new ContentValues(1);
								// cv.put(Task.FilterElement._ACTIVE, tButton.isChecked()?Task.FilterElement.ACTIVE_TRUE:Task.FilterElement.ACTIVE_FALSE);
								String parameters = getFilterElementParameters(isChecked);
								cv.put(Task.FilterElement._PARAMETERS, parameters);
								count = mActivity.getContentResolver().update(filterElementUri, cv, null, null);
								if( 1 != count ){
//									Log.e(TAG, "ERR000EG Failed to update label for URI");
//									ErrorUtil.handleNotifyUser("ERR000EG", "Failed to update label for URI " + filterElementUri, this, mActivity);
									return;
								}
								
								// Update the constraint URI in our tag.
								((Intent)v.getTag()).setData(  ((Intent)v.getTag()).getData().buildUpon().encodedQuery(parameters).build()  );

								break;
							case CODE_LABEL:
								count = mActivity.getContentResolver().delete(filterElementUri, null, null);
								if( 1 != count ){
//									Log.e(TAG, "ERR000D3 Failed to update label for URI");
//									ErrorUtil.handleNotifyUser("ERR000D3", "Failed to update label for URI " + filterElementUri, this, mActivity);
									return;
								}
								((Intent)v.getTag()).putExtra(FilterElementListAdapter.TAG_FILTER_ELEMENT_ID_INDEX, Long.MAX_VALUE);
								
								break;
							default:
								Log.e(TAG, "ERR000EF Unknown code " + findDelegateCode(v) );
								ErrorUtil.handleExceptionNotifyUser("ERR000EF", (Exception)(new Exception( String.valueOf( findDelegateCode(v) ) )).fillInStackTrace(), mActivity);
								return;
						}
					}
					
					FilterUtil.applyFilterBits(mActivity);
					break;
			}

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0003B", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0003B", exp, mActivity);
		}

	}
}
