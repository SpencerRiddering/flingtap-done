// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.lang.ref.WeakReference;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flingtap.done.util.Constants;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.ResourceCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.flingtap.done.base.R;
/**
 * 
 * @author spencer
 *
 */
public class FilterList 
		extends ListActivity 
		implements AdapterView.OnItemSelectedListener {
	private static final String TAG = "FilterList";

	
    // TODO: !!! There seems to be some confusion between menu id and request id here.
    private static final int DEFAULT_ID	= Menu.FIRST; // Used when we only care about error conditions.
    private static final int ADD_ID 					= Menu.FIRST + 1;
    private static final int EDIT_ID 					= Menu.FIRST + 2;
    private static final int RENAME_ID 					= Menu.FIRST + 3;
    private static final int DELETE_ID 					= Menu.FIRST + 4;
    
	private final static int DIALOG_CREATE_FILTER_ID      = 50;
	private final static int DELETE_FILTER_DIALOG_ID    = 51;	
	private final static int RENAME_FILTER_DIALOG_ID    = 52;	
	
    private static final int HELP_ID 					= Menu.FIRST + 10;

	
    private Cursor mCursor = null;
    private Spinner spinner = null;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	try{
    		SessionUtil.onSessionStart(this);    		
    		
    		setContentView(R.layout.filter_list);
    		//                                               0                1                         2                    3                                      4                       5                      6
    		String[] theProjection = new String[]{Task.Filter._ID, Task.Filter.DISPLAY_NAME, Task.Filter._ACTIVE, Task.Filter._DISPLAY_NAME_ARRAY_INDEX, Task.Filter._PERMANENT, Task.Filter._SELECTED, Task.Filter.DESCRIPTION};
    		
    		mCursor = managedQuery(Task.Filter.CONTENT_URI, theProjection, Task.Filter._ACTIVE+"=?", new String[]{Task.Filter.ACTIVE_TRUE_STRING}, Task.Filter.DISPLAY_NAME); // TODO: !! Consider adding an explicit _order in the table. (may be useful for correct i18n display).
    		assert null != mCursor;
    		
    		ResourceCursorAdapter la = new ResourceCursorAdapter(this, R.layout.dual_line_icon_right, mCursor, true){
    			
    			@Override
    			public void bindView(View view, Context context, Cursor cursor) {
    				TextView tv = (TextView)view.findViewById(R.id.firstLine);
    				if( cursor.isNull(3)){
    					tv.setText(cursor.getString(1));
    				}else{
    					tv.setText(getResources().getStringArray(R.array.array_stockFilters)[cursor.getInt(3)]);
    				}
    				tv = (TextView)view.findViewById(R.id.secondLine);
    				if( !cursor.isNull(6)){
    					tv.setVisibility(View.VISIBLE);
    					tv.setText(cursor.getString(6));
    				}else{
    					tv.setVisibility(View.GONE);
    					tv.setText(null);
    				}
    				
    				ImageView iv = (ImageView)view.findViewById(R.id.icon); // TODO: !!! Re-do with Image level xml file.
    				if( !cursor.isNull(4) ){
    					iv.setImageResource(R.drawable.lock);
    				}else{
    					iv.setImageResource(R.drawable.editable);
    				}
    			}
    		};
    		
    		setListAdapter(la);
    		
			// Register context menu listener (so we hear about context menu events) 
			getListView().setOnCreateContextMenuListener(this);
    		
    		spinner = (Spinner) findViewById(R.id.spinner);
    		
    		ResourceCursorAdapter spinnerAdapter = new ResourceCursorAdapter(this, android.R.layout.simple_spinner_item, mCursor, true){
    			
    			@Override
    			public void bindView(View view, Context context, Cursor cursor) {
    				TextView tv = (TextView)view.findViewById(android.R.id.text1);
    				if( cursor.isNull(3)){
    					tv.setText(cursor.getString(1));
    				}else{
    					tv.setText(getResources().getStringArray(R.array.array_stockFilters)[cursor.getInt(3)]);
    				}
    			}

    		};
    		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    		spinner.setAdapter(spinnerAdapter);

            // ********************************************************************
    		// The spinner calls onItemSelected(..) every time the page is rendered (both normal and rotation)
    		// This would normally cause a big problem because by default the selected id is the first id, 
    		//   but here I have initialized the position of the Spinner to the correct position. 
    		//   The call to onItemSelected(..) still occurs when the widget renders but since it is at the right position
    		//   it just changes the active filter to the current active filter (and thus not actually changing its value).
    		//   NOTE: I avoid the performance issue with this (because the filter bits are reset)
    		//         by checking if the active filter is actually being changed. 
    		// ********************************************************************
    		mCursor.moveToPosition(-1);
    		boolean found = false;
    		while(mCursor.moveToNext()){
    			if( Task.Filter.SELECTED_TRUE ==mCursor.getInt(5) ){
    				found = true;
    				spinner.setSelection(mCursor.getPosition());
    				break;
    			}
    		}
    		if( !found ){ // FIXME: Apparently this condition can occur. Review code that updates the selected filter to look for holes.
        		Log.e(TAG, "ERR000HE");
        		ErrorUtil.handleExceptionNotifyUser("ERR000HE", (Exception)(new Exception(  )).fillInStackTrace(), this);

        		// Reset the user to the all filter.
				FilterUtil.switchSelectedFilter(this, TaskProvider.ID_FILTER_ALL);
				FilterUtil.applyFilterBits(this);
				spinner.setSelection(0);
        	
        		return;
    		}
    		
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000DU", exp);
    		ErrorUtil.handleExceptionFinish("ERR000DU", exp, this);
    	}

	}
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
	    try{
	    	AdapterView.AdapterContextMenuInfo menuInfo2 = (AdapterView.AdapterContextMenuInfo)menuInfo;
	    	
	    	mCursor.moveToPosition(menuInfo2.position);
	    	if( mCursor.isNull(4) ){
	    		MenuItem menuItemDelete = menu.add(0, DELETE_ID, DELETE_ID, R.string.context_deleteFilter);

	    		MenuItem menuItemRename = menu.add(0, RENAME_ID, RENAME_ID, R.string.context_renameFilter);

	    		MenuItem menuItemEdit = menu.add(0, EDIT_ID, EDIT_ID, R.string.context_editFilter);
	    	}
	    	
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000FT", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR000FT", exp, this);
    	}
		
	}
	
	Uri mSelectedUri = null;
	public boolean onContextItemSelected(MenuItem item) {
    	try{
    		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
    		Long id = menuInfo.id;

    		switch (item.getItemId()) {
    			case DELETE_ID:
//					Event.onEvent(Event.FILTER_LIST_DELETE_CONTEXT_MENU_ITEM_CLICKED, null);  // TODO: !!!! Add event here.
					mSelectedUri = ContentUris.withAppendedId(Task.Filter.CONTENT_URI, id);    	
					showDialog( DELETE_FILTER_DIALOG_ID );
    				return true;
    			case EDIT_ID:
//					Event.onEvent(Event.FILTER_LIST_EDIT_CONTEXT_MENU_ITEM_CLICKED, null); // TODO: !!!! Add event here.
					Intent labelListActivityIntent = FilterElementListActivity.createViewIntent(ContentUris.withAppendedId(Task.Filter.CONTENT_URI, id));
					startActivityForResult(labelListActivityIntent, DEFAULT_ID);
    				return true;
    			case RENAME_ID:
//					Event.onEvent(Event.FILTER_LIST_VIEW_CONTEXT_MENU_ITEM_CLICKED, null);  // TODO: !!!! Add event here.
					mSelectedUri = ContentUris.withAppendedId(Task.Filter.CONTENT_URI, id);    	
					showDialog( RENAME_FILTER_DIALOG_ID );
    				return true;
    		}
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000FS", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR000FS", exp, this);
    	}

        return super.onContextItemSelected(item);
    }    
	
	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
    	MenuItem addMenuItem = menu.add(0, ADD_ID, 1, R.string.option_newFilter);
    	addMenuItem.setIcon(android.R.drawable.ic_menu_add);
		
    	MenuItem helpMenuItem = menu.findItem(HELP_ID); 
    	if( null == helpMenuItem ){
    		helpMenuItem = menu.add(0, HELP_ID, 10, R.string.option_help);
    		helpMenuItem.setIcon(android.R.drawable.ic_menu_help);
    	}
    	
		return true; 
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch(item.getItemId()){
			case ADD_ID:
				showDialog(DIALOG_CREATE_FILTER_ID);
				
				return true;
				// break;
            case HELP_ID:
        		Event.onEvent(Event.FILTER_LIST_HELP_OPTIONS_MENU_ITEM_CLICKED, null); 	
            	launchHelp();
				return true;				
		}
		return false;
	}
	
	private void launchHelp() {
		Intent helpIntent = null;
		helpIntent = StaticDisplayActivity.createIntent(this, R.layout.help_filter_list, R.string.help_filter_list);  
		assert null != helpIntent;
		startActivityForResult(helpIntent, DEFAULT_ID);
	}
	
	private WeakReference<FilterUtil.RenameFilterOnTextSetListener> renameFilterOnTextSetListener = null;
	@Override
	protected Dialog onCreateDialog(int dialogId) {
		Dialog dialog = super.onCreateDialog(dialogId);
		try{
			if( null == dialog){
				switch(dialogId){
					case DIALOG_CREATE_FILTER_ID:
						
						FilterUtil.CreateFilterOnTextSetListener listener = FilterUtil.createCreateFilterOnTextSetListener(this);

						dialog = FilterUtil.onCreateDialogCreateFilter(this, listener);
						break;
					case DELETE_FILTER_DIALOG_ID:
						dialog = new AlertDialog.Builder(this)
					        .setTitle(R.string.dialog_confirmDelete)
					        .setIcon(android.R.drawable.ic_dialog_alert)
					        .setMessage(R.string.dialog_areYouSure)
					        .setNegativeButton(R.string.button_no,null)
					        .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener(){
					    		public void onClick(DialogInterface dialog, int whichButton){
					    			try{
										FilterUtil.actuallyDeleteFilter(FilterList.this, mSelectedUri);
					    			}catch(HandledException h){ // Ignore.
					    			}catch(Exception exp){
					    				Log.e(TAG, "ERR000FV", exp);
					    				ErrorUtil.handleExceptionNotifyUser("ERR000FV", exp, FilterList.this);
					    			}
					    		}
					   		})
					   		.create();		
				   		break;
					case RENAME_FILTER_DIALOG_ID:
						long filterId = ContentUris.parseId(mSelectedUri);
						FilterUtil.RenameFilterOnTextSetListener filterOnTextSetListener = FilterUtil.createRenameFilterOnTextSetListener(this, filterId);
						renameFilterOnTextSetListener = new WeakReference<FilterUtil.RenameFilterOnTextSetListener>(filterOnTextSetListener);
						dialog = FilterUtil.onCreateDialogRenameFilter(this, filterId, filterOnTextSetListener);				   		
						break;
				}
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000DZ", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000DZ", exp, this);
		}		

		return dialog;
	}	
	
	@Override
	protected void onPrepareDialog(int dialogId, Dialog dialog) {
		super.onPrepareDialog(dialogId, dialog);
		try{
			switch(dialogId){
				case DIALOG_CREATE_FILTER_ID:
					FilterUtil.onPrepareDialogCreateFilter(this, dialog);
					break;
				case RENAME_FILTER_DIALOG_ID:
					assert null != renameFilterOnTextSetListener;
					FilterUtil.RenameFilterOnTextSetListener listener = renameFilterOnTextSetListener.get();
					
					assert null != listener;
					listener.setId(ContentUris.parseId(mSelectedUri));
					
					FilterUtil.onPrepareDialogRenameFilter(this, dialog, ContentUris.parseId(mSelectedUri));
					break;					
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000E5", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000E5", exp, this);
			dialog.dismiss(); 
		}
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		try{
			spinner.setOnItemSelectedListener(this);
    		SessionUtil.onSessionStart(this);    		
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000DV", exp);
    		ErrorUtil.handleExceptionFinish("ERR000DV", exp, this);
    	}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		try{
			spinner.setOnItemSelectedListener(null);
    		SessionUtil.onSessionStop(this);    		
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000DW", exp);
    		ErrorUtil.handleExceptionFinish("ERR000DW", exp, this);
    	}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		try{
			//Log.v(TAG, "onListItemClick(..) called");
			
			Intent filterElementListActivityIntent = FilterElementListActivity.createViewIntent(ContentUris.withAppendedId(Task.Filter.CONTENT_URI, id));
			startActivityForResult(filterElementListActivityIntent, DEFAULT_ID);
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000DX", exp);
    		ErrorUtil.handleExceptionFinish("ERR000DX", exp, this);
    	}
	}

	
	public void onItemSelected(AdapterView<?> adpaterView, View view, int position, long id) {
		try{
			Cursor cursor = getContentResolver().query(Task.Filter.CONTENT_URI, new String[]{Task.Filter._ID}, Task.Filter._SELECTED+"=?", new String[]{String.valueOf(Task.Filter.SELECTED_TRUE)}, null);
			if( !cursor.moveToFirst() ){
	    		Log.e(TAG, "ERR000HF");
	    		ErrorUtil.handleExceptionNotifyUser("ERR000HF", (Exception)(new Exception(  )).fillInStackTrace(), this);
	    		return;
			}
			long currentSelectedFilterId = cursor.getLong(0);
			cursor.close();
			if( currentSelectedFilterId != id ){
				FilterUtil.switchSelectedFilter(this, String.valueOf(id));
				FilterUtil.applyFilterBits(this);
			}
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR000DY", exp);
    		ErrorUtil.handleExceptionFinish("ERR000DY", exp, this);
    	}
	}

	public void onNothingSelected(AdapterView<?> adpaterView) {
		assert false;
	}
	

	public static Intent createViewIntent(){
		ComponentName cn = new ComponentName(StaticConfig.PACKAGE_NAME, FilterList.class.getName());
		Intent filterListActivityIntent = new Intent();				
		filterListActivityIntent.setComponent(cn);
		filterListActivityIntent.setAction(Intent.ACTION_VIEW);
		return filterListActivityIntent;
		
	}

	private static final String SAVE_SELECTED_URI = "SAVE_SELECTED_URI";
	public void  onSaveInstanceState  (Bundle outState){
		outState.putParcelable(SAVE_SELECTED_URI, mSelectedUri);
	}
	
	public void  onRestoreInstanceState  (Bundle savedInstanceState){
		mSelectedUri = savedInstanceState.getParcelable(SAVE_SELECTED_URI);
	}

}
