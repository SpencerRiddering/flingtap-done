// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.lang.ref.WeakReference;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flingtap.done.provider.Task.TaskAttachments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.flingtap.done.base.R;

/**
 *
 */
public class CompletableAttachHandler 
		extends AttachmentListAdapterDelegate 
		implements  View.OnCreateContextMenuListener, OnCheckedChangeListener { 

	public static final String TAG = "CompletableAttachHandler";

	private static final int FIRST_CODE_ID = 2500;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}		

	// Menu item IDs
	protected static final int MENU_VIEW_ID    = FIRST_CODE_ID + 5;
	protected static final int MENU_EDIT_ID    = FIRST_CODE_ID + 10;
	protected static final int MENU_DELETE_ID  = FIRST_CODE_ID + 15;
	protected static final int MENU_RENAME_ID  = FIRST_CODE_ID + 20;

	// Result Codes
	protected static final int REQUEST_EDIT   = FIRST_CODE_ID + 99;

	// Dialog IDs.
	protected static final int DIALOG_DELETE_ATTACHMENT_ID 	  = FIRST_CODE_ID + 51;	
	
	
	public CompletableAttachHandler(){
		uriMappings = new UriMappings[1];
		uriMappings[0] = new UriMappings();
		uriMappings[0].authority = Task.AUTHORITY;
		uriMappings[0].pathPattern = "completable/#";
		uriMappings[0].code = 0; // Uniquely identifies this mapping. Some Attachment handlers may handle multiple different mime-types so this allows us to distinguish between them. The value is passed into bindView(..)
	}
	
	@Override
	protected void bindView(View view, Context context, Cursor cursor, int code, Uri data) {
		try{
			// Hide second line.
			TextView secondLineText = (TextView) view.findViewById(R.id.second_line);
			secondLineText.setVisibility(View.GONE);
			
			// Hide icon.
			ImageView icon = (ImageView) view.findViewById(R.id.left_icon);
			icon.setVisibility(View.GONE);
			
			// **************************************
			// Completable content text.
			// **************************************
			TextView firstLineText = (TextView) view.findViewById(R.id.first_line);

			// **************************************
			// Set check box state.
			// **************************************
			CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
			checkBox.setVisibility(View.VISIBLE);
			
			// **************************************
			// Select completable data.
			// **************************************
			String completableUriText = cursor.getString(TaskAttachmentListTab.PROJ_ATTACH__URI_INDEX);
			Uri completeableUri = Uri.parse(completableUriText);
			Cursor completableCursor = context.getContentResolver().query(
					completeableUri, 
					new String[]{Task.Completable._ID, Task.Completable._COMPLETED, Task.Completable.TEXT_CONTENT}, 
					null, 
					null, 
					null);
			assert null != completableCursor;
			if( completableCursor.moveToFirst() ){
				firstLineText.setText(completableCursor.getString(2));
				
				checkBox.setOnCheckedChangeListener(null);
				checkBox.setChecked( !completableCursor.isNull(1) );
				checkBox.setOnCheckedChangeListener(this);
				
				checkBox.setTag( completableCursor.getString(0) ); // _id
			}else{
				firstLineText.setText(R.string.error);
			}

			completableCursor.close();
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000F1", exp);
			ErrorUtil.handleException("ERR000F1", exp, context);
		}
	}
	
	/**
	 * 
	 */
	public void onListItemClick(ListView listview, View view, int position, long id) {
		try{
			// Log.v(TAG, "onListItemClick(..) called.");
			Event.onEvent(Event.COMPLETABLE_ATTACHMENT_HANDLER_LIST_ITEM_CLICKED, null); 
			
			Uri completableUri = (Uri)((Object[]) view.getTag())[AttachmentListAdapter.TAG_ATTACHMENTS_URI_INDEX];
			
			launchCompletableEditor(completableUri);

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000FG", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000FG", exp, mActivity);
		} 
	}
	

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		
    	//Log.v(TAG, "onCreateContextMenu(..) called.");
    	
    	AdapterView.AdapterContextMenuInfo adapterMenuInfo = (AdapterView.AdapterContextMenuInfo)menuInfo;
		TextView singleLineText = (TextView) adapterMenuInfo.targetView.findViewById(R.id.first_line);
		menu.setHeaderTitle(singleLineText.getText());
		
    	// *******************
		// "Edit" menu item.
    	// *******************
		MenuItem editProximityAlertMenuItem = null;
		editProximityAlertMenuItem = menu.add(Menu.NONE, MENU_EDIT_ID,
				MENU_EDIT_ID, R.string.context_editCompletable); 

    	// *******************
		// "Delete" menu item.
    	// *******************
		MenuItem removeProximityAlertMenuItem = null;
		removeProximityAlertMenuItem = menu.add(Menu.NONE, MENU_DELETE_ID,
				MENU_DELETE_ID, R.string.context_deleteCompletable); 	
	}
	
	Uri mAttachmentUri = null;
	private Uri mCompletableUri = null;
	public boolean onContextItemSelected(MenuItem item) {
		//Log.v(TAG, "onContextItemSelected(..)");
		
    	AdapterView.AdapterContextMenuInfo mMenuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
    	Object[] tagArray = (Object[])((mMenuInfo.targetView).getTag());
    	mCompletableUri = (Uri)(tagArray[AttachmentListAdapter.TAG_ATTACHMENTS_URI_INDEX]);

        switch (item.getItemId()) {
			case MENU_EDIT_ID:
				Event.onEvent(Event.COMPLETABLE_ATTACHMENT_HANDLER_EDIT_CONTEXT_MENU_ITEM_CLICKED, null); 
				
				launchCompletableEditor(mCompletableUri);
				
				return true;
			case MENU_DELETE_ID:
				// Prepare event info.
				Event.onEvent(Event.COMPLETABLE_ATTACHMENT_HANDLER_DELETE_CONTEXT_MENU_ITEM_CLICKED, null); 
				mAttachmentUri = ContentUris.withAppendedId(TaskAttachments.CONTENT_URI, mMenuInfo.id);
				mActivity.showDialog(DIALOG_DELETE_ATTACHMENT_ID);

				return true;
			default:
				ErrorUtil.handle("ERR000FB", "onContextItemSelected() called with unknown item id" + item.getItemId(), this);
        }
        return false;

	}

	private void launchCompletableEditor(Uri attachmentUri) {
		Intent completableIntent = new Intent(Intent.ACTION_EDIT, attachmentUri);
		ComponentName cn = new ComponentName(mActivity.getPackageName(), CompletableEditor.class.getName()); // TODO: !!! update other ComponentName constructors to use this pattern.
		completableIntent.setComponent(cn);		
		mActivity.startActivityForResult(completableIntent, REQUEST_EDIT);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode){
			case REQUEST_EDIT:
				//Log.v(TAG, "onActivityResult(..) called.");
				// Notify the ListAdapter that it's cursor needs refreshing
				notifyDataSetChanged(); 
				break;
		}

	}

	// TODO: !! Delete key should also delete this attachment.
	
	public Dialog onCreateDialog(int dialogId){
		Dialog dialog = null;
		try{
			//Log.v(TAG, "onCreateDialog() called");
			if (mManagedDialogs == null) {
				mManagedDialogs = new SparseArray<Dialog>();
			}
			switch(dialogId){
				case DIALOG_DELETE_ATTACHMENT_ID:
					dialog = new AlertDialog.Builder(mActivity)
			        .setTitle(R.string.dialog_confirmDelete)
			        .setIcon(android.R.drawable.ic_dialog_alert)
			        .setMessage(R.string.dialog_areYouSure)
			        .setPositiveButton(R.string.button_yes, new android.content.DialogInterface.OnClickListener(){ // TODO: !!! Make handler implement the DialogInterface.OnClickListener.
			    		public void onClick(DialogInterface dialog, int whichButton){
			    			try{
			    				if( whichButton == android.content.DialogInterface.BUTTON_POSITIVE){
			    					if( !CompletableUtil.delete(mActivity, mAttachmentUri) ){
			    						ErrorUtil.notifyUser(mActivity); // Error already handled, so just notify user.
			    					}

			    					// Notify the ListAdapter that it's cursor needs refreshing
			    					notifyDataSetChanged(); // TODO: !! Isn't this a hack to get around the normal observer thing? NO, not always. Sometimes data changes in the db record that the URI refers to and it isn't really a change to the attachment record.
			    				}
			    			}catch(HandledException h){ // Ignore.
			    			}catch(Exception exp){
			    				Log.e(TAG, "ERR000FD", exp);
			    				ErrorUtil.handleExceptionNotifyUser("ERR000FD", exp, mActivity);
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
			Log.e(TAG, "ERR000FC", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000FC", exp, mActivity);
		}
		return dialog;
	}


	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	public void onPrepareDialog(int dialogId, Dialog dialog){
	}
	
	
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		return false;
	}

	public void onCheckedChanged(CompoundButton compoundbutton, boolean flag) {
		ContentValues cv = new ContentValues(1);
		cv.put(Task.Completable._COMPLETED, flag?Task.Completable.COMPLETED_TRUE:null);

		int count = mActivity.getContentResolver().update( 
				Task.Completable.CONTENT_URI, 
				cv, 
				Task.Completable._ID+"=?", 
				new String[]{(String)compoundbutton.getTag()});
		if( 1 != count ){
			Log.e(TAG, "ERR000G5");
			ErrorUtil.handleExceptionNotifyUser("ERR000G5", (Exception)(new Exception(String.valueOf(count))).fillInStackTrace(), mActivity);  
		}
		
	}
	
	private static final String SAVE_ATTACHMENT_URI = "NearminderAttachHandler.SAVE_ATTACHMENT_URI";
	private SparseArray<Dialog> mManagedDialogs;
	
	public void  onSaveInstanceState  (Bundle outState){
		try{
			//Log.v(TAG, "onSaveInstanceState() called");
			if( null != mAttachmentUri ){
				outState.putParcelable(SAVE_ATTACHMENT_URI, mAttachmentUri);
			}
			if( null != mCompletableUri ){
				outState.putParcelable(SAVE_ATTACHMENT_URI, mCompletableUri);
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
				mCompletableUri = savedInstanceState.getParcelable(SAVE_ATTACHMENT_URI) ;
			}
			if( savedInstanceState.containsKey(SAVE_ATTACHMENT_URI) ){
				mAttachmentUri = savedInstanceState.getParcelable(SAVE_ATTACHMENT_URI) ;
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
