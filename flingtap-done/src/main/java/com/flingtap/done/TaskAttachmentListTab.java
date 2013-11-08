// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.Toast;
import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;

import java.util.ArrayList;

/**
 * 
 * 
 * @author spencer
 *
 */
public class TaskAttachmentListTab 
		extends CoordinatedListActivity 
		implements View.OnClickListener, View.OnKeyListener {

	private static final String TAG = "TaskAttachmentListTab";

    // Identifiers for our menu items.
    private static final int DEFAULT_ID     = Menu.FIRST + 101;
    private static final int HELP_ID        = Menu.FIRST + 102;
    protected static final int DELETE_ID    = Menu.FIRST + 103;
    protected static final int ARCHIVE_ID   = Menu.FIRST + 104;


    protected static final String SAVE_STATE_SELECTED_INDEX_KEY = "SAVE_STATE_SELECTED_INDEX_KEY";
	
	private Uri mURI; 		  // Requested URI
	protected Cursor mCursor; // Cursor holding a list of the contacts for a single task.

	protected final static int ATTACHMENT_MENU_REMOVE_ITEM_ID = 101;

	protected final static int DELETE_ATTACHMENT_REQUEST      = 199;
	protected final static int ADD_ATTACHMENT_REQUEST      = 198;

	protected static final int DIALOG_DELETE_TASK_ID 	  = 151; // TODO: !! add a FIRST_CODE_ID for the 100-199 subclass range.

	protected ArrayList<AttachmentListAdapterDelegate> handlers = new ArrayList<AttachmentListAdapterDelegate>();
	protected AttachmentListAdapter adapter = null;
	private EditText mCompletableEditText = null;

    protected long mTaskId = -1;

    /**
     * The columns we are interested in from the database
     */
	protected static final String[] ATTACHMENTS_PROJECTION = new String[] {
		com.flingtap.done.provider.Task.TaskAttachments._ID, 			// 0
		com.flingtap.done.provider.Task.TaskAttachments.TASK_ID, 		// 1
		com.flingtap.done.provider.Task.TaskAttachments._URI, 		    // 2
		com.flingtap.done.provider.Task.TaskAttachments.NAME, 		    // 3
		com.flingtap.done.provider.Task.TaskAttachments._PACKAGE,	    // 4
		com.flingtap.done.provider.Task.TaskAttachments._CLASS_NAME,    // 5
	};
	public static final int PROJ_ATTACH_ID_INDEX 			= 0;
	public static final int PROJ_ATTACH_TASK_ID_INDEX 		= 1;
	public static final int PROJ_ATTACH__URI_INDEX 		    = 2;
	public static final int PROJ_ATTACH_NAME_INDEX 		    = 3;
	public static final int PROJ_ATTACH__PACKAGE_INDEX 		= 4;
	public static final int PROJ_ATTACH__CLASS_NAME_INDEX 	= 5;

    
    protected void onNewIntent(Intent intent){
    	super.onNewIntent(intent);
    	try{
    		//Log.v(TAG, "onNewIntent(..) called.");
    		doSetupWithIntent(intent);
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0006I", exp);
    		ErrorUtil.handleExceptionFinish("ERR0006I", exp, this);
    	}
    }

    protected void doSetupWithIntent(final Intent intent) {
    	
		// Do some setup based on the action being performed.
		final String action = intent.getAction();
		if (action.equals(Intent.ACTION_EDIT)) {
			// Requested to edit: set that state, and the data being edited.
			mURI = intent.getData();
			setResult(RESULT_OK);
		} else if (action.equals(Intent.ACTION_INSERT)) {
			// Requested to edit: set that state, and the data being edited.
			mURI = intent.getData();
			setResult(RESULT_OK);
		} else if (action.equals(Intent.ACTION_VIEW)) { // User can alwasy add/remove/edit attachments. 
			// Requested to edit: set that state, and the data being edited.
			mURI = intent.getData();
			setResult(RESULT_OK);
		} else {
			// Whoops, unknown action! Bail.
			Log.e(TAG, "ERR0006O Unknown action. " + action);
			ErrorUtil.handleExceptionNotifyUserFinish("ERR0006O", (Exception)(new Exception( action )).fillInStackTrace(), this);
			return;
		}
		//Log.v(TAG, "intent.getAction() == " + action);
    	//Log.v(TAG, "getIntent().getData() == " + mURI);

        // ****************
        // task id
        // ****************
    	mTaskId = ContentUris.parseId(mURI);

        mCursor = managedQuery(Task.TaskAttachments.CONTENT_URI, ATTACHMENTS_PROJECTION, Task.TaskAttachments.TASK_ID + "=?", new String[]{String.valueOf(mTaskId)}, Task.TaskAttachments._ID + " DESC");
        ListAdapter adapter = doSetupWithCursor(mCursor);
        setListAdapter(adapter);
    }
	
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		try{
			//Log.v(TAG, "onCreate(..) called");
			
			// Prepare event info.
			Event.onEvent(Event.VIEW_TASK_ATTACHMENT_LIST_TAB, null); // Map<String,String> parameters = new HashMap<String,String>();

        	this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); 
			
			setContentView(R.layout.task_attachment_list);
			
	        // Inform the list we provide context menus for items
	        getListView().setOnCreateContextMenuListener(this);
	        
	        // ****************
	        // intent
	        // ****************
	        final Intent intent = getIntent();

			// **********************************
			// Completable adder
			// **********************************	        
			((ImageButton)findViewById(R.id.task_attachment_add_completable_button)).setOnClickListener(this);
			mCompletableEditText = (EditText)findViewById(R.id.task_attachment_add_completable_text);
			mCompletableEditText.setOnClickListener(this);
			mCompletableEditText.setOnKeyListener(this);

			// **********************************
			// Add Attachment Participant
			// **********************************
	        AttachmentPart attachmentpart = new AttachmentPart(this);
	        addParticipant(attachmentpart);
	        
			// **********************************
			// Add Pick Attachment Participant
			// **********************************
	        PickAttachmentPart mPickAttachmentPart = new PickAttachmentPart(this, mURI, attachmentpart);
        	addParticipant(mPickAttachmentPart);

	        
			// **********************************
			// Add Geocode Address Participant
			// **********************************
	        PostalContactMethodGeocoderPart geocodeAddressPart = new PostalContactMethodGeocoderPart(this);
			addParticipant(geocodeAddressPart);
			        
	        
	        AttachmentListAdapterDelegate contactHandler = new ContactAttachHandler(this, attachmentpart, geocodeAddressPart);
	        
			contactHandler.setActivity(this);
			contactHandler.setIntent(intent);
			handlers.add(contactHandler);
			addParticipant(contactHandler);
			
			// *****************
			// Add NearminderAttachHandler
			// *****************
			NearminderAttachHandler nearminderHander = new NearminderAttachHandler();
			nearminderHander.setActivity(this);
			nearminderHander.setIntent(intent);
			handlers.add(nearminderHander);
			addParticipant(nearminderHander);
			
			// *****************
			// Add CompletableAttachHandler
			// *****************
			CompletableAttachHandler completableHander = new CompletableAttachHandler();
			completableHander.setActivity(this);
			completableHander.setIntent(intent);
			handlers.add(completableHander);
			addParticipant(completableHander);
			
			
			// ***************************************************
			// All Attachment Handlers must be intialized before here.
			// ***************************************************
			
	    	doSetupWithIntent(intent);
	    	
			// ***************************************************
			// Attachment Handlers may optionally add the task id here.
			// ***************************************************
			contactHandler.setTaskId(mTaskId);
			mPickAttachmentPart.setUri(mURI);
			
	        // Restore state to killed list.
			if( null != icicle){
				// M5 Migration
				int selectedIndex = icicle.getInt(SAVE_STATE_SELECTED_INDEX_KEY, -1);
				if( -1 != selectedIndex){
					setSelection(selectedIndex);
				}
			}    	
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0006H", exp);
			ErrorUtil.handleExceptionFinish("ERR0006H", exp, this);
		}
	}
	
	
	
    protected ListAdapter doSetupWithCursor(Cursor cursor){

    	adapter = new AttachmentListAdapter(
    			this,
        		R.layout.dual_line_icon_checkbox,
        		mCursor){
        };

    	adapter.addDelegate(handlers);
    	
    	AttachmentListAdapterDelegate defaultDelegate = new DefaultAttachHandler(); 
    	defaultDelegate.setActivity(this);
    	addParticipant(defaultDelegate);
    	adapter.setDefaultDelegate(defaultDelegate);
        return adapter;
    }	
    
	/**
	 * Finds the ContextListActivityParticipant from within the view tag.
	 */
	public ContextListActivityParticipant getParticipant(Object tag){
		return adapter.getParticipant(tag);
	}    
    
    
    public boolean onContextItemSelected(MenuItem item) {
    	try{
    		//Log.v(TAG, "onContextItemSelected(..) called.");
    		
    		switch (item.getItemId()) {
    			default:
    				return super.onContextItemSelected(item);
    		}
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0006J", exp);
    		ErrorUtil.handleExceptionNotifyUser("ERR0006J", exp, this);
    	}
    	return true;
    }

	public void onActivityResult(int requestCode, int resultCode, Intent data){
		try{
			//Log.v(TAG, "onActivityResult(..) called.");
			if( SharedConstant.RESULT_ERROR == resultCode ){
				ErrorUtil.notifyUser(this);
				return;
			}			
			
			
			switch (requestCode) {
				case ADD_ATTACHMENT_REQUEST:
					// TODO: ! Implement. Check if we have a handler for this type,, if not then ask the user to type in a description.
					break;
				default:
					super.onActivityResult(requestCode, resultCode, data);
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0006K", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0006K", exp, this);
		}
		
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		try{
			menu.removeItem(HELP_ID);
			MenuItem helpMenuItem = menu.add(0, HELP_ID, 100, R.string.option_help);
			helpMenuItem.setAlphabeticShortcut('h');
			helpMenuItem.setIcon(android.R.drawable.ic_menu_help);

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0006L", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0006L", exp, this);
		}
		return true;
	}	

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		try{
            // Do nothing.
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0006M", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0006M", exp, this);
		}
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		try {
			// Handle all of the possible menu actions.
			switch (item.getItemId()) {
				case DELETE_ID:
					Event.onEvent(Event.EDIT_TASK_DELETE_OPTIONS_MENU_ITEM_CLICKED, null); 
	
					showDialog(DIALOG_DELETE_TASK_ID);
					
					break;
				case ARCHIVE_ID:
					// TODO: !!! Consider whether to return to the task list immediately when a task is archived this way (or at least indicate in the task editor that the current task is archived).
            		Event.onEvent(Event.TASK_ATTACHMENT_LIST_ARCHIVE_BUTTON_BAR_BUTTON_CLICKED, null); 		
            		ArchiveMenuPart.archiveTask(this, Long.parseLong(mURI.getLastPathSegment()));
            		finish();
					break;
				case HELP_ID:
					// Prepare event info.
					Event.onEvent(Event.VIEW_TASK_ATTACHMENT_LIST_TAB_HELP, null); // Map<String,String> parameters = new HashMap<String,String>();

					Intent helpIntent = StaticDisplayActivity.createIntent(this, R.layout.help_task_editor_attach, R.string.help_task_editor_attach); 
					assert null != helpIntent;
					
					startActivityForResult(helpIntent, DEFAULT_ID);

					break;
			}
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR000A0", exp);
			ErrorUtil.handleExceptionFinish("ERR000A0", exp, this);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int dialogId) {
		Dialog dialog = super.onCreateDialog(dialogId);;
		try{
			if( null != dialog ){
				return dialog;
			}
			switch(dialogId){
				case DIALOG_DELETE_TASK_ID: // TODO: !!! Is this code used anymore? 
					
					dialog = new AlertDialog.Builder(this)
			        .setTitle(R.string.dialog_confirmDelete)
			        .setIcon(android.R.drawable.ic_dialog_alert)
			        .setMessage(R.string.dialog_areYouSure)
			        .setNegativeButton(R.string.button_no, null)
			        .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener(){
			    		public void onClick(DialogInterface dialog, int whichButton){
			    			try{
		    					int count = TaskUtil.deleteTask(TaskAttachmentListTab.this, mURI); 
		    					if(1 != count){
		    	    				ErrorUtil.handle("ERR00087", "Delete of URI " + mURI + " removed " + count + " record(s).", TaskAttachmentListTab.this);
		    					}
		    					Toast.makeText(TaskAttachmentListTab.this, R.string.toast_taskDeleted, Toast.LENGTH_SHORT).show();

		    					finish();
			    			}catch(HandledException h){ // Ignore.
			    			}catch(Exception exp){
			    				Log.e(TAG, "ERR00086", exp);
			    				ErrorUtil.handleExceptionNotifyUser("ERR00086", exp, TaskAttachmentListTab.this);
			    			}

			    		}
			   		})
			        .create();
					
			}
	        if (mManagedDialogs == null) {
	            mManagedDialogs = new SparseArray<Dialog>();
	        }				
	        mManagedDialogs.put(dialogId, dialog); // This may replace a dialog (with the same id) which was restored after a config change. Maybe you want to dismiss it first, just in case.

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000C9", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000C9", exp, this);
		}
		return dialog;
	}
	
	public void onClick(View v) {
		try{
			switch(v.getId()){
				case R.id.task_attachment_add_completable_button:
					handleSubmitCompletableEvent();
					break;
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000D6", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000D6", exp, this);
		}
	}

	// NOTE: Assumes only Completable fast adder EditText events.
	public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) { 
            switch (keyCode) {
            	case KeyEvent.KEYCODE_ENTER:
            		if( event.isAltPressed() ){
            			return false;
            		}
            		// Fall through
                case KeyEvent.KEYCODE_DPAD_CENTER:
                	handleSubmitCompletableEvent();
		            return true;
            }
        }
		return false;
	}
    
	private void handleSubmitCompletableEvent() {
		try{
			String text = mCompletableEditText.getText().toString().trim();
			if( text.length() > 0 ){
				ContentValues cv = new ContentValues(1);
				
				// Add the Completable record.
				cv.put(Task.Completable.TEXT_CONTENT, text);
				Uri resultUri = getContentResolver().insert(Task.Completable.CONTENT_URI, cv);

				if( resultUri != null ){
					// Add the attachment record.
					Intent editCompletableIntent = new Intent(Intent.ACTION_EDIT, resultUri); 
					
                    //Code is good, but later when the delete intent is started, the activity is not started reliabily. See CompletableUtil for details.
                    //This may not be true anymore.
					Intent deleteCompletableIntent = new Intent(Intent.ACTION_DELETE, resultUri); 
					ComponentName cn = new ComponentName(getPackageName(), CompletableEditor.class.getName()); 
					deleteCompletableIntent.setComponent(cn);		

					if( null != (new AttachmentPart(this)).addAttachment(mTaskId, editCompletableIntent, null, null, null, deleteCompletableIntent) ){
						// Event.
						Event.onEvent(Event.CREATE_COMPLETABLE);
					}
				}else{
					Log.e(TAG, "ERR000FJ");
					ErrorUtil.handleExceptionNotifyUser("ERR000FJ", (Exception)(new Exception(  )).fillInStackTrace(), this);
					return;
				}

			}
			mCompletableEditText.setText(""); // Clear out old value.
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000FP", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000FP", exp, this);
		}
	}

	private int selectedIndex = 0;
	
	@Override
	protected void onRestart() {
		super.onRestart();
		try{
			setSelection(selectedIndex);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0006N", exp);
			ErrorUtil.handleException("ERR0006N", exp, this);
		}
	}

	protected void onStop() {
		super.onStop();
		try{
			//Log.v(TAG, "onStop(..) called.");			
			if( !isFinishing() ){
				selectedIndex = getSelectedItemPosition();
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000B8", exp);
			ErrorUtil.handleException("ERR000B8", exp, this);
		}
	}

	public void notifyDataSetChanged(){
		mCursor.requery();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		try{
			saveManagedDialogs(outState);
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR000CC", exp);
			ErrorUtil.handleExceptionFinish("ERR000CC", exp, this);
		}
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		try{
			restoreManagedDialogs(savedInstanceState);
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR000CD", exp);
			ErrorUtil.handleExceptionFinish("ERR000CD", exp, this);
		}
	}
	
	
    protected void onDestroy() {
    	super.onDestroy();
    	destroyManagedDialogs();
    }    

	
    // ******************************************************************
    // TabActivity children do not store/restore managed dialogs. (Bug)
    //
    //   The code below is my workaround for storing/restoring 
    //   managed dialogs. The source code comes from private methods
    //   in Activity.java.
    // 
    // -- Begin: AAAAAAC --- 
    // ******************************************************************
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
    
    private static final String SAVED_DIALOG_IDS_KEY    = "TaskEditor:savedDialogIds";
    private static final String SAVED_DIALOGS_TAG       = "TaskEditor:savedDialogs";
    private static final String SAVED_DIALOG_KEY_PREFIX = "TaskEditor:dialog_";
	private SparseArray<Dialog> mManagedDialogs;
	
    /**
     * Save the state of any managed dialogs.
     *
     * @param outState place to store the saved state.
     */
    protected void saveManagedDialogs(Bundle outState) {
        if (mManagedDialogs == null) {
            return;
        }

        final int numDialogs = mManagedDialogs.size();
        if (numDialogs == 0) {
            return;
        }

        Bundle dialogState = new Bundle();

        int[] ids = new int[mManagedDialogs.size()];

        // save each dialog's bundle, gather the ids
        for (int i = 0; i < numDialogs; i++) {
            final int key = mManagedDialogs.keyAt(i);
            ids[i] = key;
            final Dialog dialog = mManagedDialogs.valueAt(i);
            dialogState.putBundle(savedDialogKeyFor(key), dialog.onSaveInstanceState());
        }

        dialogState.putIntArray(SAVED_DIALOG_IDS_KEY, ids);
        outState.putBundle(SAVED_DIALOGS_TAG, dialogState);
    }
    private String savedDialogKeyFor(int key) {
        return SAVED_DIALOG_KEY_PREFIX + key;
    }
    /**
     * Restore the state of any saved managed dialogs.
     *
     * @param savedInstanceState The bundle to restore from.
     */
    protected void restoreManagedDialogs(Bundle savedInstanceState) {
        final Bundle b = savedInstanceState.getBundle(SAVED_DIALOGS_TAG);
        if (b == null) {
            return;
        }

        final int[] ids = b.getIntArray(SAVED_DIALOG_IDS_KEY);
        final int numDialogs = ids.length;
        mManagedDialogs = new SparseArray<Dialog>(numDialogs);
        for (int i = 0; i < numDialogs; i++) {
            final Integer dialogId = ids[i];
            Bundle dialogState = b.getBundle(savedDialogKeyFor(dialogId));
            if (dialogState != null) {
                final Dialog dialog = onCreateDialog(dialogId);
                dialog.onRestoreInstanceState(dialogState);
                mManagedDialogs.put(dialogId, dialog);
            }
        }
    }

    protected void destroyManagedDialogs(){
    	// dismiss any dialogs we are managing.
    	if (mManagedDialogs != null) {
    		
    		final int numDialogs = mManagedDialogs.size();
    		for (int i = 0; i < numDialogs; i++) {
    			final Dialog dialog = mManagedDialogs.valueAt(i);
    			if (dialog.isShowing()) {
    				dialog.dismiss();
    			}
    		}
    	}
    }
    // **************************************************************
    // -- END: AAAAAAC --- 
    // **************************************************************

}
