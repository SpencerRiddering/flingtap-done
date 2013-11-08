// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.io.FilterInputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flingtap.common.HandledException;
import com.flingtap.done.AttachmentListAdapter.UriMapping;
import com.flingtap.done.provider.Task;
import com.flingtap.done.util.Constants;
import com.flurry.android.FlurryAgent;
import com.flingtap.done.base.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

/**
 * 
 */
public class FilterElementListActivity extends CoordinatedListActivity {
	
	private static final String TAG = "FilterElementListActivity";

	
    public static final String ACTION_UPDATE_FILTER_BITS = "com.flingtap.done.intent.action.UPDATE_FILTER_BITS";
	
	// The different distinct states the activity can be run in.
//	private static final int STATE_VIEW = 0;
//	private static final int STATE_EDIT = 1;

    // Menu item IDs.
	protected static final int DONE_ID                       = Menu.FIRST;
	protected static final int ADD_LABEL_ID                  = Menu.FIRST + 1;
	protected static final int DELETE_ID                     = Menu.FIRST + 2;
	protected static final int HELP_ID                  	 = Menu.FIRST + 12;
			
	
	// The different distinct states the activity can result in.
	private static final int RESULT_STATE_SAVE = 0;
	private static final int RESULT_STATE_CANCEL = 1;


    protected static final String SAVE_STATE_SELECTED_INDEX_KEY = "SAVE_STATE_SELECTED_INDEX_KEY";
	
	protected Cursor mCursor; // Cursor holding a list of the contacts for a single task.

	private final static int DIALOG_CREATE_LABEL_ID      = 50;

    /**
     * The columns we are interested in from the database
     */
	protected static final String[] FILTER_ELEMENT_PROJECTION = new String[] {
		TaskProvider.FILTER_ELEMENT_TABLE_NAME+"."+Task.FilterElement._ID + " AS " + Task.FilterElement._ID , 	// 0
		Task.FilterElementColumns._FILTER_ID, 								// 1 // TODO: !!! Is this really required? Can't the filter id be found in the intent's uri? 
		Task.FilterElementColumns._PARAMETERS, 								// 2
		Task.FilterElementColumns._CONSTRAINT_URI,							// 3
		TaskProvider.FILTER_ELEMENT_TABLE_NAME+"."+Task.FilterElementColumns._ORDER, 							// 4
	};
	public static final int PROJ_FILTER_ELEMENT__ID_INDEX 			= 0;
	public static final int PROJ_FILTER_ELEMENT__FILTER_ID_INDEX 	= 1;
	public static final int PROJ_FILTER_ELEMENT__PARAMETERS_INDEX   = 2;
	public static final int PROJ_FILTER_ELEMENT__CONSTRAINT_INDEX 	= 3;


    
    protected void onNewIntent(Intent intent){
    	super.onNewIntent(intent);
    	try{
    		//Log.v(TAG, "onNewIntent(..) called.");
    		doSetupWithIntent(intent);
    	}catch(HandledException h){ // Ignore.
			setResult(RESULT_CANCELED);
			finish();
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0002Z", exp);
    		ErrorUtil.handleExceptionFinish("ERR0002Z", exp, this);
    	}
    }
    
   
    protected void doSetupWithIntent(final Intent intent) {
        	
    	Cursor filterCursor = getContentResolver().query(intent.getData(), new String[]{Task.Filter.DISPLAY_NAME, Task.Filter._DISPLAY_NAME_ARRAY_INDEX}, null, null, null);
    	if( filterCursor.moveToFirst() ){
    		if( filterCursor.isNull(1)){
    			setTitle( TextUtils.expandTemplate(getString(R.string.filterCOLONX), filterCursor.getString(0)).toString() );
    		}else{
    			setTitle( TextUtils.expandTemplate(getString(R.string.filterCOLONX), getResources().getStringArray(R.array.array_stockFilters)[filterCursor.getInt(1)]).toString() );
    		}
    	}else{
    		setTitle( getString(R.string.filterCOLON) );
    	}
    	filterCursor.close();
    	
    	String filterId = intent.getData().getLastPathSegment();
		Uri uri = Task.Filter.CONTENT_URI.buildUpon().appendPath(filterId).appendPath("filter_elements").build(); // TODO: ! Externalize this twig.

		// If the Unlabeled filter element is not present, then this means that filter elements are not used to create the set of possible tasks during the explode phase. Thus, label filter elements should not be displayed to user since this would change this (explode all by default) expectation. This is often the case for permanent filters such as "All" and "Archive". 
		Cursor unlabeledCursor = getContentResolver().query(uri, null, Task.FilterElement._CONSTRAINT_URI + "=? AND "+Task.FilterElement._ACTIVE+"=?", new String[]{Constraint.Version1.UNLABELED_CONTENT_URI_STRING, Task.FilterElement.ACTIVE_TRUE}, null);
		boolean areAllTasksIncludedByDefault = unlabeledCursor.getCount() == 0;
		unlabeledCursor.close();

		// The current list of 
		String nonPermanentFilterWhere = "";
		String[] selectionArgs = null;
		if( !areAllTasksIncludedByDefault ){ // Union the label filter elements with the labels that don't have a corresponding filter element for this filter.
			nonPermanentFilterWhere = ") UNION SELECT "+ TaskProvider.FILTER_ELEMENT_TABLE_NAME+"."+Task.FilterElement._ID + " AS " + Task.FilterElement._ID + "," + intent.getData().getLastPathSegment() + ",''," + Task.FilterElementColumns._CONSTRAINT_URI + ", (1000 + substr("+ Task.FilterElementColumns._CONSTRAINT_URI+",61)) AS "+Task.FilterElement._ORDER +" FROM "+ TaskProvider.FILTER_ELEMENT_TABLE_NAME + " WHERE " + Task.FilterElement._FILTER_ID+"=? AND " + Task.FilterElementColumns._ACTIVE+"=? AND "+Task.FilterElementColumns._CONSTRAINT_URI+" LIKE 'content://com.flingtap.done.taskprovider/tasks/mask/1/label/%'"
    		+ " UNION SELECT "+Long.MAX_VALUE+" AS "+Task.FilterElement._ID+", "+intent.getData().getLastPathSegment()+",'','content://com.flingtap.done.taskprovider/tasks/mask/1/label/'||" + TaskProvider.LABELS_TABLE_NAME + "." + Task.Labels._ID + ", (1000+"+TaskProvider.LABELS_TABLE_NAME + "." + Task.Labels._ID +") AS " + Task.FilterElement._ORDER + " FROM " + TaskProvider.LABELS_TABLE_NAME + " WHERE (" + Task.Labels._ID + " NOT IN ( SELECT substr("+Task.FilterElement._CONSTRAINT_URI+",61) FROM "+TaskProvider.FILTER_ELEMENT_TABLE_NAME + " WHERE "+Task.FilterElement._FILTER_ID +"=? AND "+Task.FilterElement._CONSTRAINT_URI+" LIKE 'content://com.flingtap.done.taskprovider/tasks/mask/1/label/%') AND " + Task.Labels._USER_APPLIED + "=?"
			+ createHeaderRecord(intent.getData().getLastPathSegment(), "Labels", 999);
			selectionArgs = new String[]{Task.FilterElementColumns.ACTIVE_TRUE, intent.getData().getLastPathSegment(), Task.FilterElementColumns.ACTIVE_TRUE, intent.getData().getLastPathSegment(), Task.Labels.USER_APPLIED_TRUE};
		}else{ // If all tasks are included by default, then no need to include label filter elements. 
			selectionArgs = new String[]{Task.FilterElementColumns.ACTIVE_TRUE};
		}

		if( ArchiveUtil.isArchiveEnabled(this) ){
			nonPermanentFilterWhere += createHeaderRecord(intent.getData().getLastPathSegment(), "Folders", FilterUtil.FOLDERS_HEADER_ROW_FILTER_ELEMENT_ORDER); 
		}
		
		boolean addDetailsHeader =  
			!TaskProvider.ID_FILTER_ALL.equals(filterId) &&
			!TaskProvider.ID_FILTER_ARCHIVE.equals(filterId);
		
        mCursor = managedQuery(
        		uri, 
        		FILTER_ELEMENT_PROJECTION, 
        		TaskProvider.FILTER_ELEMENT_TABLE_NAME+"."+Task.FilterElementColumns._ACTIVE+"=? AND "+Task.FilterElementColumns._CONSTRAINT_URI+" NOT LIKE 'content://com.flingtap.done.taskprovider/tasks/mask/1/label/%' "
    			+ (addDetailsHeader?createHeaderRecord(intent.getData().getLastPathSegment(), "Details", 0):"")
        		+ nonPermanentFilterWhere,
        		selectionArgs,
        		Task.FilterElementColumns._ORDER);

        // Update our list when changes are made to the underlying data.
        //   Example: When a new filter element is added.
        Handler handler = new Handler(); 
        getContentResolver().registerContentObserver(Task.FilterElement.CONTENT_URI, true, new ContentObserver(handler){
        	@Override
        	public void onChange(boolean selfChange) {
        		super.onChange(selfChange);
        		try{
            		if( null != mCursor && !mCursor.isClosed() ){
            			mCursor.requery();
            		}
            	}catch(HandledException h){ // Ignore.
        		}catch(Exception exp){
        			Log.e(TAG, "ERR00037", exp);
        			ErrorUtil.handleExceptionNotifyUser("ERR00037", exp, FilterElementListActivity.this);
        		}
        	}
        });
        ListAdapter adapter = doSetupWithCursor(mCursor);
        setListAdapter(adapter);
    }
    
	private static String createHeaderRecord(String filterId, String text, long order){
		return ") UNION SELECT "+Long.MAX_VALUE+" AS "+Task.FilterElement._ID+", "+filterId+",'','content://com.flingtap.done.taskprovider/cosmetic/header_row/"+text+"', "+order+" AS " + Task.FilterElement._ORDER +" FROM "+TaskProvider.FILTER_TABLE_NAME +" WHERE ( _id=1 "; // TODO: !!! Is there a better way to have a WHERE clause so we can add the required '(' character? I did this to get a single row. I think there will always be 1 filter _id with value 1. 
	}

	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		try{
			//Log.v(TAG, "onCreate(..) called");
			
			setContentView(R.layout.filter_element_list);

			// ****************
			// intent
			// ****************
			final Intent intent = getIntent();
			
			if( ACTION_UPDATE_FILTER_BITS.equals(intent.getAction()) ){ // TODO: !!! Shouldn't this be an IntentReceiver so 3rd party Activities can call it? 
				FilterUtil.applyFilterBits(this);
				setResult(RESULT_OK);
				finish();
				return;
			}

			SessionUtil.onSessionStart(this);
			// Prepare event info.
			Event.onEvent(Event.VIEW_FILTER, null); // Map<String,String> parameters = new HashMap<String,String>();

			// Inform the list we provide context menus for items
			// ContextMenu Step 1.
			getListView().setOnCreateContextMenuListener(this);
			
			// **********************************
			// Add Delegates
			// **********************************
			
			DueDateFilterElementDelegate dueDateLabelDelegate = new DueDateFilterElementDelegate();
			dueDateLabelDelegate.setActivity(this); 
			dueDateLabelDelegate.setIntent(intent);
			handlers.add(dueDateLabelDelegate);
			addParticipant(dueDateLabelDelegate);
			
			PriorityFilterElementDelegate priorityLabelDelegate = new PriorityFilterElementDelegate();
			priorityLabelDelegate.setActivity(this); 
			priorityLabelDelegate.setIntent(intent);
			handlers.add(priorityLabelDelegate);
			addParticipant(priorityLabelDelegate);
			
			RepositoryFilterElementDelegate repositoryLabelDelegate = new RepositoryFilterElementDelegate();
			repositoryLabelDelegate.setActivity(this); 
			repositoryLabelDelegate.setIntent(intent);
			handlers.add(repositoryLabelDelegate);
			addParticipant(repositoryLabelDelegate);
			
			StatusFilterElementDelegate statusLabelDelegate = new StatusFilterElementDelegate();
			statusLabelDelegate.setActivity(this); 
			statusLabelDelegate.setIntent(intent);
			handlers.add(statusLabelDelegate);
			addParticipant(statusLabelDelegate);
			
			LabelFilterElementDelegate labelFilterElementDelegate = new LabelFilterElementDelegate();
			labelFilterElementDelegate.setActivity(this); 
			labelFilterElementDelegate.setIntent(intent);
			handlers.add(labelFilterElementDelegate); // ContextMenu Step 3.
			addParticipant(labelFilterElementDelegate);

			HeaderFilterElementDelegate headerFilterElementDelegate = new HeaderFilterElementDelegate();
			handlers.add(headerFilterElementDelegate);
						
			// ***************************************************
			// All Attachment Handlers must be initialized before here.
			// ***************************************************
			
			doSetupWithIntent(intent);

			// Restore state to killed list.
			if( null != icicle){
				int selectedIndex = icicle.getInt(SAVE_STATE_SELECTED_INDEX_KEY, -1);
				if( -1 != selectedIndex){
					setSelection(selectedIndex);
				}
			}    	
		}catch(HandledException h){ // Ignore.
			setResult(RESULT_CANCELED);
			finish();
		}catch(Exception exp){
			Log.e(TAG, "ERR00030", exp);
			ErrorUtil.handleExceptionNotifyUserFinish("ERR00030", exp, this);
		}
		
	}
	protected ArrayList<DelegatingListAdapterDelegate> handlers = new ArrayList<DelegatingListAdapterDelegate>();
	protected FilterElementListAdapter adapter = null;
	
	
    protected ListAdapter doSetupWithCursor(Cursor cursor){

    	adapter = new FilterElementListAdapter(this, mCursor, getIntent().getData());
        
    	// ContextMenu Step 4.
    	adapter.addDelegate(handlers);
    	
    	DefaultFilterElementDelegate defaultDelegate = new DefaultFilterElementDelegate(); 
    	defaultDelegate.setActivity(this);
    	addParticipant(defaultDelegate);
    	adapter.setDefaultDelegate(defaultDelegate);
    	
        return adapter;
    }	
    
	/**
	 * Finds the ContextListActivityParticipant from within the view tag.
	 * ContextMenu Step 2.
	 */
	public ContextListActivityParticipant getParticipant(Object tag){
		return adapter.getParticipant(tag);
	}    
	
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		try{
			// Done option
			MenuItem doneMenuItem = menu.add(0, DONE_ID, 10, R.string.option_save);
			doneMenuItem.setIcon(android.R.drawable.ic_menu_save);
			
			boolean labelsEnabled = LabelUtil.isLabelsEnabled(this);
			if( labelsEnabled ){
				MenuItem addLabelMenuItem = menu.add(0, ADD_LABEL_ID, 11, R.string.option_addLabel);
				addLabelMenuItem.setIcon(R.drawable.add_label);
			}

			MenuItem helpMenuItem = menu.add(0, HELP_ID, 12, R.string.option_help);
			helpMenuItem.setIcon(android.R.drawable.ic_menu_help);

			return true;
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00031", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00031", exp, this);
		}
		return false;
	}	
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		SharedPreferences settings = getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE);
		boolean labelsEnabled = LabelUtil.isLabelsEnabled(this);
		boolean archiveEnabled = ArchiveUtil.isArchiveEnabled(this);
		if( labelsEnabled || archiveEnabled ){
			if( null == menu.findItem(DELETE_ID) ){
				MenuItem addLabelMenuItem = menu.add(0, DELETE_ID, 11, R.string.option_delete);
				addLabelMenuItem.setIcon(android.R.drawable.ic_menu_delete);
			}
		}
		
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		try{
			switch (item.getItemId()) {
				case DONE_ID:
					finish();
					break;
				case HELP_ID:
					Intent helpIntent = null;
					if( ArchiveUtil.isArchiveEnabled(this) || LabelUtil.isLabelsEnabled(this)){
						helpIntent = StaticDisplayActivity.createIntent(this, R.layout.help_filter_all, R.string.help_filter);
					}else{
						helpIntent = StaticDisplayActivity.createIntent(this, R.layout.help_filter, R.string.help_filter);  
					}
					assert null != helpIntent;
					startActivity(helpIntent);
					break;
				case ADD_LABEL_ID:
					Event.onEvent(Event.FILTER_ELEMENT_LIST_ADD_LABEL_OPTIONS_MENU_ITEM_CLICKED, null); // Map<String,String> parameters = new HashMap<String,String>();
					showDialog(DIALOG_CREATE_LABEL_ID);
					break;
				case DELETE_ID:
					int count = getContentResolver().delete(getIntent().getData(), null, null);
					if( 1 != count ){
						Log.e(TAG, "ERR000FU");
						ErrorUtil.handleExceptionNotifyUser("ERR000FU", (Exception)(new Exception( getIntent().getData().toString() )).fillInStackTrace(), this);
					}
					finish();
					break;
				default:
					Log.e(TAG, "ERR000G4");
					ErrorUtil.handleExceptionNotifyUser("ERR000G4", (Exception)(new Exception( String.valueOf(item.getItemId()) )).fillInStackTrace(), this);
				}
				return super.onOptionsItemSelected(item);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00032", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00032", exp, this);
		}		
		return false;
	}	
	

	@Override
	protected Dialog onCreateDialog(int dialogId) {
		Dialog dialog = super.onCreateDialog(dialogId);
		try{
			if( null == dialog){
				switch(dialogId){
					case DIALOG_CREATE_LABEL_ID:
						LabelUtil.CreateLabelOnTextSetListener listener = LabelUtil.createCreateLabelOnTextSetListener(this);
						dialog = LabelUtil.onCreateDialogCreateLabel(this, listener);
				}
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000E3", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000E3", exp, this);
		}		

		return dialog;
	}

	@Override
	protected void onPrepareDialog(int dialogId, Dialog dialog) {
		super.onPrepareDialog(dialogId, dialog);
		try{
			switch(dialogId){
				case DIALOG_CREATE_LABEL_ID:
					LabelUtil.onPrepareDialogCreateLabel(this, dialog);
					break;
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000E4", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000E4", exp, this);
			dialog.dismiss(); 
		}
	}
	
	
	private int selectedIndex = 0;	
	
	@Override
	protected void onPause() {
		super.onPause();
		try{
			SessionUtil.onSessionStop(this);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00033", exp);
			ErrorUtil.handleExceptionFinish("ERR00033", exp, this);
		}		
	}
	
	@Override
	protected void onResume() {
		try{
			//Log.v(TAG, "onResume(..) called.");			
			SessionUtil.onSessionStart(this);

			super.onResume();
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00034", exp);
			ErrorUtil.handleExceptionFinish("ERR00034", exp, this);
		}
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		try{
			//Log.v(TAG, "onRestart(..) called.");			
			setSelection(selectedIndex);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00035", exp);
			ErrorUtil.handleExceptionFinish("ERR00035", exp, this);
		}		
	}

	protected void onStop() {
		super.onStop();
		try{
			//Log.v(TAG, "onStop(..) called.");			
			selectedIndex = getSelectedItemPosition();
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00036", exp);
			ErrorUtil.handleExceptionFinish("ERR00036", exp, this);
		}		
	}

	public void notifyDataSetChanged(){
		mCursor.requery();
	}
	
	public static Intent createViewIntent(Uri uri) {
		ComponentName cn = new ComponentName(StaticConfig.PACKAGE_NAME, FilterElementListActivity.class.getName());
		Intent labelListActivityIntent = new Intent();			
		labelListActivityIntent.setData(uri);
		labelListActivityIntent.setComponent(cn);
		labelListActivityIntent.setAction(Intent.ACTION_VIEW);
		return labelListActivityIntent;
	}
}
