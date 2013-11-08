// Licensed under the Apache License, Version 2.0

package com.flingtap.done;


import java.util.HashMap;
import java.util.Map;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flingtap.done.provider.Task.TaskAttachments;
import com.flurry.android.FlurryAgent;
import com.flingtap.done.base.R;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;
/**
 * 
 * 
 * @author spencer
 *
 */
public abstract class AttachmentListAdapterDelegate 
		implements ContextListActivityParticipant { //, TextEntryDialogPart.OnTextSetListener
	private static final String TAG = "AttachmentListAdapterDelegate";
	
	protected Intent mIntent;
	protected TaskAttachmentListTab mActivity;
	protected long mTaskId = -1;

	// Isn't this uncessary since we are passing a reference to the Activity as well?
	public void setIntent(Intent intent){ // TODO: Move this initialization into the constructor.
		assert intent != null;
		mIntent = intent;
	}
	
	public void setActivity(TaskAttachmentListTab activity) {// TODO: Move this initialization into the constructor.
		assert activity != null;
		mActivity = activity;
	}	
	
	public void setTaskId(long taskId) {
		//Log.d(TAG, "setTaskId("+taskId+")");
		assert taskId != -1; 
		mTaskId = taskId;
	}	
	
	protected abstract void bindView(View view, Context context, Cursor cursor, int code, Uri data); // TODO: Perhaps data is not necessary here. Could get it from ATTACHMENTS_URI_INDEX, right?   
	
	
	
	/**
	 * TODO: ! Consider replacing UriMappings with the IntentFilter class and its filter.match(..) method. 
	 */
	public class UriMappings {
		public String authority;
		public String pathPattern;
		public String componentName;
		public int code; // TODO: What is the purpose of this value? 
	};
	public UriMappings[] uriMappings = null;
	
	/**
	 * 
	 */
	public void onListItemClick(ListView listview, View view, int position, long id) {
		try{
			// Log.v(TAG, "onListItemClick(..) called.");
			
			Uri attachUri = ContentUris.withAppendedId(Task.TaskAttachments.CONTENT_URI, (Long)((Object[]) view.getTag())[AttachmentListAdapter.TAG_ATTACHMENTS_ID_INDEX]);
			AttachmentPart.launchAttachDefaultAction(mActivity, attachUri, view);
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0000V", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0000V", exp, mActivity);
		} 
	}

	public boolean hasMenu(){
		return false;
	}
	
	public boolean hasContextMenu(){
		return false;
	}
	
	public boolean hasInstanceState(){
		return false;
	}
	
//	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo){
//    	Log.v(TAG, "onCreateContextMenu(..) called.");
//		final String action = mIntent.getAction();
//		if (action.equals(Intent.ACTION_EDIT) || action.equals(Intent.ACTION_INSERT)) {
//
//			// *******************
//			// Add "Remove" menu item.
//			// *******************
//			addRemoveAttachmentMenuItems(menu);
//			
//			// *******************
//			// Add "Rename" menu item.
//			// *******************
//			addRenameAttachmentMenuItems(menu);
//		}
//			
//	}

	/**
	 * Used to add a "Remove" context menu item to the attachment.
	 */
	protected void addRemoveAttachmentMenuItems(ContextMenu menu, int itemId) {
		addRemoveAttachmentMenuItems(menu, itemId, R.string.context_removeAttachment);
	}
	protected void addRemoveAttachmentMenuItems(ContextMenu menu, int itemId, int nameResId) {
		MenuItem removeAttachmentMenuItem = menu.add(0, itemId, itemId, nameResId);
				
//		removeAttachmentMenuItem.setAlphabeticShortcut('r');
		//removeAttachmentMenuItem.setIcon(R.drawable.removeattechment);
	}


	
	protected void addRenameAttachmentMenuItems(ContextMenu menu, int itemId) {
		MenuItem renameAttachmentMenuItem = menu.add(0, itemId, itemId,
				R.string.context_renameAttachment);
//		renameAttachmentMenuItem.setAlphabeticShortcut('n');
//		removeAttachmentMenuItem.setIcon(R.drawable.removeattechment);
	}

	protected void addViewAttachmentMenuItems(ContextMenu menu, int itemId) { // TODO: !! reconsider these methods,, really no need, right?
		addViewAttachmentMenuItems(menu, itemId, R.string.context_viewAttachment); 
	}
	protected void addViewAttachmentMenuItems(ContextMenu menu, int itemId, int nameResId) {
		menu.add(0, itemId, itemId, nameResId);
	}
	
	protected void notifyDataSetChanged(){ // TODO: !! Isn't there a better way to do this?
		mActivity.notifyDataSetChanged();
	}

	public void  onSaveInstanceState  (Bundle outState){
	}
	
	public void  onRestoreInstanceState  (Bundle savedInstanceState){
	}
	// TODO: !!! Define empty defaults for these methods: onCreateOptionsMenu() onPrepareOptionsMenu() onCreateDialog() onPrepareDialog() onActivityResult()
	
    public void onDestroy() {
    }
    
    public int getLayoutId(){
    	return -1; // TODO: !!! Verify that this is a valid non-id.
    }
    
    // ******************************************************************
    // TabActivity children do not store/restore managed dialogs. (Bug)
    //
    //   The code below is my workaround for storing/restoring 
    //   managed dialogs. The source code comes from private methods
    //   in Activity.java.
    // 
    // -- Begin: AAAAAAA --- 
    // ******************************************************************
    
    private static final String SAVED_DIALOG_IDS_KEY = "AttachmentListAdapterDelegate:savedDialogIds";
    private static final String SAVED_DIALOGS_TAG = "AttachmentListAdapterDelegate:savedDialogs";
    private static final String SAVED_DIALOG_KEY_PREFIX = "AttachmentListAdapterDelegate:dialog_";

    /**
     * Save the state of any managed dialogs.
     *
     * @param outState place to store the saved state.
     */
    protected void saveManagedDialogs(Bundle outState, SparseArray<Dialog> managedDialogs, String prefix) {
        if (managedDialogs == null) {
            return;
        }

        final int numDialogs = managedDialogs.size();
        if (numDialogs == 0) {
            return;
        }

        Bundle dialogState = new Bundle();

        int[] ids = new int[managedDialogs.size()];

        // save each dialog's bundle, gather the ids
        for (int i = 0; i < numDialogs; i++) {
            final int key = managedDialogs.keyAt(i);
            ids[i] = key;
            final Dialog dialog = managedDialogs.valueAt(i);
            dialogState.putBundle(savedDialogKeyFor(key, prefix), dialog.onSaveInstanceState());
        }

        dialogState.putIntArray(prefix+SAVED_DIALOG_IDS_KEY, ids);
        outState.putBundle(prefix+SAVED_DIALOGS_TAG, dialogState);
    }
    private String savedDialogKeyFor(int key, String prefix) {
        return prefix + SAVED_DIALOG_KEY_PREFIX + key;
    }
    /**
     * Restore the state of any saved managed dialogs.
     *
     * @param savedInstanceState The bundle to restore from.
     */
    protected void restoreManagedDialogs(Bundle savedInstanceState, SparseArray<Dialog> managedDialogs, String prefix) {
        final Bundle b = savedInstanceState.getBundle(prefix+SAVED_DIALOGS_TAG);
        if (b == null) {
            return;
        }

        final int[] ids = b.getIntArray(prefix+SAVED_DIALOG_IDS_KEY);
        final int numDialogs = ids.length;
        managedDialogs = new SparseArray<Dialog>(numDialogs);
        for (int i = 0; i < numDialogs; i++) {
            final Integer dialogId = ids[i];
            Bundle dialogState = b.getBundle(savedDialogKeyFor(dialogId, prefix));
            if (dialogState != null) {
                final Dialog dialog = onCreateDialog(dialogId);
                dialog.onRestoreInstanceState(dialogState);
                managedDialogs.put(dialogId, dialog);
            }
        }
    }

    protected void onDestroy(SparseArray<Dialog> managedDialogs) {
    	// dismiss any dialogs we are managing.
    	if (managedDialogs != null) {
    		
    		final int numDialogs = managedDialogs.size();
    		for (int i = 0; i < numDialogs; i++) {
    			final Dialog dialog = managedDialogs.valueAt(i);
    			if (null != dialog && dialog.isShowing()) { // TODO: !!! Figure out how managedDialogs can contain a null value.
    				dialog.dismiss();
    			}
    		}
    	}
    }    
    
    // **************************************************************
    // -- END: AAAAAAA --- 
    // **************************************************************
}
