// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import com.flingtap.common.HandledException;
import com.flingtap.done.AttachmentListAdapterDelegate.UriMappings;
import com.flingtap.done.provider.Task;
import com.flingtap.done.provider.Task.TaskAttachments;
import com.flurry.android.FlurryAgent;
import com.flingtap.done.base.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.ContactMethodsColumns;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * TODO: Refactor this code so that it sub-classes DefaultAttachHandler since there is alot of overlap.
 */
public class NearminderAttachHandler 
		extends AttachmentListAdapterDelegate 
		implements  View.OnCreateContextMenuListener { 

	public static final String TAG = "NearminderAttachHandler";

	private static final int FIRST_CODE_ID = 600;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}
	
	// Item IDs
	protected static final int PROXIMITY_MENU_VIEW_ITEM_ID    = FIRST_CODE_ID + 5;
	protected static final int PROXIMITY_MENU_EDIT_ITEM_ID    = FIRST_CODE_ID + 10;
	protected static final int PROXIMITY_MENU_DELETE_ITEM_ID  = FIRST_CODE_ID + 15;
	protected static final int PROXIMITY_MENU_RENAME_ITEM_ID  = FIRST_CODE_ID + 20;

	// Result Codes
	protected static final int DELETE_PROXIMITY_ALERT_REQUEST = FIRST_CODE_ID + 99;
	
	protected static final int RENAME_DIALOG_ID  			  = FIRST_CODE_ID + 50;
	protected static final int DIALOG_REMOVE_ATTACHMENT_ID 	  = FIRST_CODE_ID + 51;

	public NearminderAttachHandler(){
		uriMappings = new UriMappings[1];
		uriMappings[0] = new UriMappings();
		uriMappings[0].authority = Task.AUTHORITY;
		uriMappings[0].pathPattern = "proximity/#";
		uriMappings[0].code = 0; // Uniquely identifies this mapping. Some Attachment handlers may handle multiple different mime-types so this allows us to distinguish between them. The value is passed into bindView(..)
	}
	
	@Override
	protected void bindView(View view, Context context, Cursor cursor, int code, Uri data) {
		try{
			// Hide checkbox
			CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);
			checkbox.setVisibility(View.GONE);
			
			// Hide second line
			TextView secondLineText = (TextView) view.findViewById(R.id.second_line);
			secondLineText.setVisibility(View.GONE);
			
			// **************************************
			// Nearminder Name
			// **************************************
			TextView singleLineText = (TextView) view.findViewById(R.id.first_line);
			singleLineText.setMinimumHeight(64);

			String name = cursor.getString(TaskAttachmentListTab.PROJ_ATTACH_NAME_INDEX);
			singleLineText.setText(name);
			
			// **************************************
			// Nearminder primary icon
			// **************************************
			ImageView primaryImage = (ImageView) view.findViewById(R.id.left_icon);
			primaryImage.setImageResource(R.drawable.ic_launcher_nearminder);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00043", exp);
			ErrorUtil.handleException("ERR00043", exp, context);
		}
	}

	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo){
    	//Log.v(TAG, "onCreateContextMenu(..) called.");
    				
    	AdapterView.AdapterContextMenuInfo adapterMenuInfo = (AdapterView.AdapterContextMenuInfo)menuInfo;
		TextView singleLineText = (TextView) adapterMenuInfo.targetView.findViewById(R.id.first_line);
		menu.setHeaderTitle(singleLineText.getText());
		
		// *******************
		// Add "View" menu item.
		// *******************
		addViewAttachmentMenuItems(menu, PROXIMITY_MENU_VIEW_ITEM_ID, R.string.context_viewNearminder);	
		
    	// *******************
		// "Edit" menu item.
    	// *******************
		MenuItem editProximityAlertMenuItem = null;
		editProximityAlertMenuItem = menu.add(8, PROXIMITY_MENU_EDIT_ITEM_ID,
				PROXIMITY_MENU_EDIT_ITEM_ID, R.string.context_editNearminder); 

    	// *******************
		// "Remove" menu item.
    	// *******************
		MenuItem removeProximityAlertMenuItem = null;
		removeProximityAlertMenuItem = menu.add(8, PROXIMITY_MENU_DELETE_ITEM_ID,
				PROXIMITY_MENU_DELETE_ITEM_ID, R.string.context_deleteNearminder); 	

    	// *******************
		// Add "Rename" menu item.
    	// *******************
		addRenameAttachmentMenuItems(menu, PROXIMITY_MENU_RENAME_ITEM_ID);

	}
	
	Uri mAttachmentUri = null;
	Uri mProxAlertUri = null;
	public boolean onContextItemSelected(MenuItem item) {
		//Log.v(TAG, "onContextItemSelected(..)");
		
    	AdapterView.AdapterContextMenuInfo mMenuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
    	Object[] tagArray = (Object[])((mMenuInfo.targetView).getTag());
    	mProxAlertUri = (Uri)(tagArray[AttachmentListAdapter.TAG_ATTACHMENTS_URI_INDEX]);

        switch (item.getItemId()) {
			case PROXIMITY_MENU_VIEW_ITEM_ID:
				Event.onEvent(Event.NEARMINDER_ATTACHMENT_HANDLER_VIEW_CONTEXT_MENU_ITEM_CLICKED, null); 
				mActivity.startActivity(new Intent(Intent.ACTION_VIEW, mProxAlertUri));
				return true;
			case PROXIMITY_MENU_EDIT_ITEM_ID:
				Event.onEvent(Event.NEARMINDER_ATTACHMENT_HANDLER_EDIT_CONTEXT_MENU_ITEM_CLICKED, null); 
				
				Intent nearminderIntent = new Intent(Intent.ACTION_EDIT, mProxAlertUri);
				mActivity.startActivity(nearminderIntent);
				return true;
			case PROXIMITY_MENU_DELETE_ITEM_ID:
				// Prepare event info.
				Event.onEvent(Event.NEARMINDER_ATTACHMENT_HANDLER_DELETE_CONTEXT_MENU_ITEM_CLICKED, null); 
				
				mActivity.showDialog(DIALOG_REMOVE_ATTACHMENT_ID);

				return true;
			case PROXIMITY_MENU_RENAME_ITEM_ID:
				
				mAttachmentUri = ContentUris.withAppendedId(TaskAttachments.CONTENT_URI, mMenuInfo.id);
				mActivity.showDialog(RENAME_DIALOG_ID);
				
				// Prepare event info.
				Event.onEvent(Event.NEARMINDER_ATTACHMENT_HANDLER_RENAME_CONTEXT_MENU_ITEM_CLICKED, null); 
				return true;
			default:
				ErrorUtil.handle("ERR00044", "onContextItemSelected() called with unknown item id" + item.getItemId(), this);
        }
        return false;
	}
	

	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}


	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if( resultCode == SharedConstant.RESULT_ERROR ){
			ErrorUtil.notifyUser(mActivity);
			return;
		}

	}
	
	
	private WeakReference<AttachmentPart.RenameOnTextSetListener> renameOnTextSetListener = null;
	public Dialog onCreateDialog(int dialogId){
		Dialog dialog = null;
		try{
			//Log.v(TAG, "onCreateDialog() called");
			if (mManagedDialogs == null) {
				mManagedDialogs = new SparseArray<Dialog>();
			}
			switch(dialogId){
				case RENAME_DIALOG_ID:
					if( null == mAttachmentUri ){ // TODO: !! This check may be unecessary compensation for an error that occured before where mAttachmentUri wasn't restored correctly.
						Log.e(TAG, "ERR000BB");
						ErrorUtil.handleExceptionNotifyUser("ERR000BB", (Exception)(new Exception(  )).fillInStackTrace(), mActivity);
						return dialog; // I know, bad,, but what else to do here?
					}
					AttachmentPart.RenameOnTextSetListener listener = new AttachmentPart.RenameOnTextSetListener(mActivity, mAttachmentUri);
					renameOnTextSetListener = new WeakReference<AttachmentPart.RenameOnTextSetListener>(listener);
					
					dialog = AttachmentPart.onCreateDialogRenameDialog(mActivity, dialogId, mAttachmentUri, listener);
		            mManagedDialogs.put(dialogId, dialog);
					break;
				case DIALOG_REMOVE_ATTACHMENT_ID:
					dialog = new AlertDialog.Builder(mActivity)
			        .setTitle(R.string.dialog_confirmDelete)
			        .setIcon(android.R.drawable.ic_dialog_alert)
			        .setMessage(R.string.dialog_areYouSure)
			        .setPositiveButton(R.string.button_yes, new android.content.DialogInterface.OnClickListener(){
			    		public void onClick(DialogInterface dialog, int whichButton){
			    			try{
			    				if( whichButton == android.content.DialogInterface.BUTTON_POSITIVE){
			    					if( !Nearminder.delete(mActivity, mProxAlertUri) ){
			    						ErrorUtil.notifyUser(mActivity); // Error already handled, so just notify user.
			    					}
			    					// Notify the ListAdapter that it's cursor needs refreshing
			    					notifyDataSetChanged(); // TODO: !! Isn't this a hack to get around the normal observer thing.
			    				}
			    			}catch(HandledException h){ // Ignore.
			    			}catch(Exception exp){
			    				Log.e(TAG, "ERR00012", exp);
			    				ErrorUtil.handleExceptionNotifyUser("ERR00012", exp, mActivity);
			    			}
			    		}
			    	})
			        .setNegativeButton(R.string.button_no, null)
			    	.create();
		            mManagedDialogs.put(dialogId, dialog);
		            break;
					
			} 
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000BB", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000BB", exp, mActivity);
		}
		return dialog;
	}

	public void onPrepareDialog(int dialogId, Dialog dialog){
		try{
			//Log.v(TAG, "onPrepareDialog() called");
			switch(dialogId){
				case RENAME_DIALOG_ID:
					assert null != renameOnTextSetListener;
					AttachmentPart.RenameOnTextSetListener listener = renameOnTextSetListener.get();
					
					assert null != mAttachmentUri;
					listener.setAttachmentUri(mAttachmentUri);
					
					AttachmentPart.onPrepareDialogRenameDialog(mActivity, dialog, mAttachmentUri);
					break;
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000BC", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000BC", exp, mActivity);
			dialog.dismiss(); // TODO: Is this the right method to use here? 
		}
	}
	

	
	public boolean hasInstanceState() {
		return true;
	}
	
	private static final String SAVE_ATTACHMENT_URI = "NearminderAttachHandler.SAVE_ATTACHMENT_URI";
	private static final String SAVE_PROXIMITY_ALERT_URI = "NearminderAttachHandler.SAVE_PROXIMITY_ALERT_URI";
	private SparseArray<Dialog> mManagedDialogs;
	
	public void  onSaveInstanceState  (Bundle outState){
		try{
			//Log.v(TAG, "onSaveInstanceState() called");
			if( null != mAttachmentUri ){
				outState.putParcelable(SAVE_ATTACHMENT_URI, mAttachmentUri);
			}
			if( null != mProxAlertUri ){
				outState.putParcelable(SAVE_PROXIMITY_ALERT_URI, mProxAlertUri);
			}
			saveManagedDialogs(outState, mManagedDialogs, TAG);
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000BD", exp);
			ErrorUtil.handleExceptionFinish("ERR000BD", exp, mActivity);
		}
	}
	
	public void  onRestoreInstanceState  (Bundle savedInstanceState){
		try{
			//Log.v(TAG, "onRestoreInstanceState() called");
			if( savedInstanceState.containsKey(SAVE_ATTACHMENT_URI) ){
				mAttachmentUri = savedInstanceState.getParcelable(SAVE_ATTACHMENT_URI) ;
			}
			if( savedInstanceState.containsKey(SAVE_PROXIMITY_ALERT_URI) ){
				mProxAlertUri = savedInstanceState.getParcelable(SAVE_PROXIMITY_ALERT_URI);
			}
			
			restoreManagedDialogs(savedInstanceState, mManagedDialogs, TAG);
			
			// throw new RuntimeException("TEST"); // TODO: !!! An exception thrown here, results in a 0 result code in TaskList. Why is the response code from handleExceptionFinish(..) ignored?
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000BE", exp);
			ErrorUtil.handleExceptionFinish("ERR000BE", exp, mActivity);
		}
	}
	

    public void onDestroy() {
    	onDestroy(mManagedDialogs);
    }
	
}
