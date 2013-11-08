// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flingtap.done.provider.Task.TaskAttachments;
import com.flingtap.done.util.Constants;
import com.flingtap.done.util.UriInfo;
import com.flurry.android.FlurryAgent;
import com.flingtap.done.base.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.provider.Contacts;
import android.provider.OpenableColumns;
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
 * 
 * 
 * @author spencer
 *
 */
public class DefaultAttachHandler extends AttachmentListAdapterDelegate implements  View.OnCreateContextMenuListener  {

	public static final String TAG = "DefaultAttachHandler";
	
	protected final static int FIRST_CODE_ID 	  = 200;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}

	// Menu Item IDs
	private final static int ATTACHMENT_MENU_VIEW_ITEM_ID   = FIRST_CODE_ID + 5;
	private final static int ATTACHMENT_MENU_REMOVE_ITEM_ID = FIRST_CODE_ID + 10;
	private final static int ATTACHMENT_MENU_RENAME_ITEM_ID = FIRST_CODE_ID + 15;
	
	// Dialog IDs.
	private static final int RENAME_DIALOG_ID  			    = FIRST_CODE_ID + 50;
	private static final int DIALOG_REMOVE_ATTACHMENT_ID 	= FIRST_CODE_ID + 51;
//	private static final int RENAME_DISPLAY_NAME_DIALOG_ID  = FIRST_CODE_ID + 52;
	
	
	
	protected Bitmap defualtBitmap = null;
	
	public DefaultAttachHandler(){
	}
	
	private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat();
	
	/**
	 * TODO: The code in this method could be simplified and/or streamlined. 
	 * @Override
	 */
	protected void bindView(View view, Context context, Cursor cursor,
			int code, Uri data) {
		
		// Hide checkbox
		CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);
		checkbox.setVisibility(View.GONE);

		// Find the date created.
		Uri attachURI = ContentUris.withAppendedId(Task.TaskAttachments.CONTENT_URI, (Long) ((Object[])view.getTag())[AttachmentListAdapter.TAG_ATTACHMENTS_ID_INDEX]);
		
		Cursor c = context.getContentResolver().query(attachURI,
				new String[] {  Task.TaskAttachments.NAME, Task.TaskAttachments._ICON, Task.TaskAttachments._ICON_RESOURCE, Task.TaskAttachments._INTENT }, //Task.TaskAttachments.CREATED_DATE,  
				null, 
				null,
				null);
		if( !c.moveToFirst() ){
			Toast.makeText(context, R.string.toast_removedBrokenAttachmentLink, Toast.LENGTH_LONG).show();
			Exception exp = (Exception)(new Exception("Failed to find attachment record.").fillInStackTrace());
			Log.e(TAG, "ERR0001P", exp);
			ErrorUtil.handleExceptionAndThrow("ERR0001P", exp, context);
		}

		PackageManager pm = context.getPackageManager();
		
		String attachName = c.getString(0);
		Bitmap iconBitmap = null;
		if( !c.isNull(1)){
			iconBitmap = BitmapFactory.decodeByteArray(c.getBlob(1), 0, c.getBlob(1).length);
		}
		Drawable iconDrawable = null; // TODO: There may be some redundancy between this icon code and what is discovered below by UriInfo. not sure...
		if( !c.isNull(2)){
			Resources rs;
			try {
				rs = pm.getResourcesForApplication(c.getString(2).substring(0,c.getString(2).indexOf(':'))); // TODO: Retrieve the package from the original Intent.ShortcutIconResource so it can be used here. Not sure if they are different.
				int iconResource = rs.getIdentifier(c.getString(2), null, null);  
				iconDrawable = rs.getDrawable(iconResource);
			} catch (NameNotFoundException e) {
				// Just ignore.
			}
		}
		// Get launch intent from db.
		String intentString = c.getString(3);
		Intent launchIntent = AttachmentPart.expandIntent(intentString, context);

		c.close();
		
		UriInfo uriInfo = null;
		
		TextView dualLineText1 = (TextView) view.findViewById(R.id.first_line);		
		TextView dualLineText2 = (TextView) view.findViewById(R.id.second_line);		
		dualLineText2.setVisibility(View.GONE);
		
		if( null == attachName ){
			uriInfo = new UriInfo(launchIntent, data, context.getContentResolver(), pm, context.getResources(), attachName);
			String label = uriInfo.getLabel();
			
			dualLineText1.setText(label); // TODO: It's possible (although not likely) that label is null.
		}else{
			dualLineText1.setText(attachName); 
		}

		ImageView primaryIcon = (ImageView) view.findViewById(R.id.left_icon);
		primaryIcon.setVisibility(View.VISIBLE);
		if( null != iconBitmap ){
			primaryIcon.setImageDrawable(new BitmapDrawable(iconBitmap));
		}else if( null != iconDrawable ){
			primaryIcon.setImageDrawable(iconDrawable);
		}else{
			if( null == uriInfo ){
				uriInfo = new UriInfo(launchIntent, data, context.getContentResolver(), pm, context.getResources(), attachName);
			}
			if( null != uriInfo.getIconBitmap() ){
				primaryIcon.setImageDrawable(new BitmapDrawable(uriInfo.getIconBitmap()));
			}else if(null != uriInfo.getIconResource()){
				try {
					Resources rs = pm.getResourcesForApplication(uriInfo.getIconResource().packageName);
					int iconResource = rs.getIdentifier(uriInfo.getIconResource().resourceName, null, null);  
					iconDrawable = rs.getDrawable(iconResource);
					primaryIcon.setImageDrawable(iconDrawable);
				} catch (NameNotFoundException e) {
					// ignore
				} 
			}
		}		
	}


	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo){
		try{
			//Log.v(TAG, "onCreateContextMenu(..) called.");
			
	    	AdapterView.AdapterContextMenuInfo adapterMenuInfo = (AdapterView.AdapterContextMenuInfo)menuInfo;
			TextView singleLineText = (TextView) adapterMenuInfo.targetView.findViewById(R.id.first_line);
			menu.setHeaderTitle(singleLineText.getText());
			
			
			// *******************
			// Add "Remove" menu item.
			// *******************
			addRemoveAttachmentMenuItems(menu, ATTACHMENT_MENU_REMOVE_ITEM_ID);
			
			// *******************
			// Add "Rename" menu item.
			// *******************
			addRenameAttachmentMenuItems(menu, ATTACHMENT_MENU_RENAME_ITEM_ID);	
			
			// *******************
			// Add "View" menu item.
			// *******************
			addViewAttachmentMenuItems(menu, ATTACHMENT_MENU_VIEW_ITEM_ID);	
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001C", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0001C", exp, mActivity);			
		} 	
		
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		try{
			if( resultCode == SharedConstant.RESULT_ERROR ){
				ErrorUtil.notifyUser(mActivity);
				return;
			}
			//Log.v(TAG, "onActivityResult(..) called");
			switch(requestCode){
//				case DELETE_ATTACHMENT_REQUEST:
//					// Notify the ListAdapter that it's cursor needs refreshing
//					notifyDataSetChanged();
//					break;
			};
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001M", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0001M", exp, mActivity);			
		} 	
	}

	Uri mAttachmentUri = null;
	// Events are handled by AttachmentPart
	public boolean onContextItemSelected(MenuItem item) {
		try{
			//Log.v(TAG, "onContextItemSelected(..) called.");
			AdapterView.AdapterContextMenuInfo mMenuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			mAttachmentUri = ContentUris.withAppendedId(TaskAttachments.CONTENT_URI, mMenuInfo.id);
			switch (item.getItemId()) {
				case ATTACHMENT_MENU_REMOVE_ITEM_ID:					
					mActivity.showDialog(DIALOG_REMOVE_ATTACHMENT_ID);
					return true;
				case ATTACHMENT_MENU_RENAME_ITEM_ID:
					mActivity.showDialog(RENAME_DIALOG_ID);
					return true;
				case ATTACHMENT_MENU_VIEW_ITEM_ID:
					AttachmentPart.launchAttachDefaultAction(mActivity, mAttachmentUri, mMenuInfo.targetView);
					return true;
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001N", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0001N", exp, mActivity);			
		} 	

        return false;
	}

	
	// TODO: Can't this be moved up to AttachmentListAdapterDelegate ? 
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	// TODO: Can't this be moved up to AttachmentListAdapterDelegate ? 
	public boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}

	
	private WeakReference<AttachmentPart.RenameOnTextSetListener> renameOnTextSetListener = null;
	public Dialog onCreateDialog(int dialogId){
		Dialog dialog = null;
		try{
			// Log.v(TAG, "onCreateDialog() called");
			if (mManagedDialogs == null) {
				mManagedDialogs = new SparseArray<Dialog>();
			}
			switch(dialogId){
				case RENAME_DIALOG_ID:
					AttachmentPart.RenameOnTextSetListener listener = new AttachmentPart.RenameOnTextSetListener(mActivity, mAttachmentUri);
					renameOnTextSetListener = new WeakReference<AttachmentPart.RenameOnTextSetListener>(listener); 
					
					dialog = AttachmentPart.onCreateDialogRenameDialog(mActivity, dialogId, mAttachmentUri, listener);
					
			        mManagedDialogs.put(dialogId, dialog);
			        break;
				case DIALOG_REMOVE_ATTACHMENT_ID:
					dialog = new AlertDialog.Builder(mActivity)
			        .setTitle(R.string.dialog_confirmRemoval)
			        .setIcon(android.R.drawable.ic_dialog_alert)
			        .setMessage(R.string.dialog_areYouSure)
			        .setPositiveButton(R.string.button_yes, new android.content.DialogInterface.OnClickListener(){
			    		public void onClick(DialogInterface dialog, int whichButton){
			    			try{
			    				if( whichButton == android.content.DialogInterface.BUTTON_POSITIVE){
			    					AttachmentPart.removeAttachmentImmediately(mActivity, mAttachmentUri); 		
			    				}
			    			}catch(HandledException h){ // Ignore.
			    			}catch(Exception exp){
			    				Log.e(TAG, "ERR000C6", exp);
			    				ErrorUtil.handleExceptionNotifyUser("ERR000C6", exp, mActivity);
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
			Log.e(TAG, "ERR000B6", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000B6", exp, mActivity);
		}
		return dialog;
	}

	public void onPrepareDialog(int dialogId, Dialog dialog){
		try{
			// Log.v(TAG, "onPrepareDialog() called");
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
			Log.e(TAG, "ERR000B7", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000B7", exp, mActivity);
			dialog.dismiss(); // TODO: Is this the right method to use here? 
		}
	}
	

	
	public boolean hasInstanceState() {
		return true;
	}
	
	private static final String SAVE_ATTACHMENT_URI = "DefaultAttachHandler.SAVE_ATTACHMENT_URI";
	private SparseArray<Dialog> mManagedDialogs;
	
	public void  onSaveInstanceState  (Bundle outState){
		try{
			// Log.v(TAG, "onSaveInstanceState() called");
			if( null != mAttachmentUri ){
				outState.putParcelable(SAVE_ATTACHMENT_URI, mAttachmentUri);
			}
			
			saveManagedDialogs(outState, mManagedDialogs, TAG);
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000BA", exp);
			ErrorUtil.handleExceptionFinish("ERR000BA", exp, mActivity);
		}
	}
	
	public void  onRestoreInstanceState  (Bundle savedInstanceState){
		try{
			// Log.v(TAG, "onRestoreInstanceState() called");
			if( savedInstanceState.containsKey(SAVE_ATTACHMENT_URI) ){
				mAttachmentUri = savedInstanceState.getParcelable(SAVE_ATTACHMENT_URI) ;
			}
			
			restoreManagedDialogs(savedInstanceState, mManagedDialogs, TAG);
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000B9", exp);
			ErrorUtil.handleExceptionFinish("ERR000B9", exp, mActivity);
		}
	}
	

    public void onDestroy() {
    	onDestroy(mManagedDialogs);
    }
    

}
