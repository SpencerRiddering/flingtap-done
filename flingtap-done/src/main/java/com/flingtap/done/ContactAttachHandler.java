// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import com.flingtap.common.HandledException;
import com.flingtap.done.AttachmentListAdapterDelegate.UriMappings;
import com.flingtap.done.provider.Task;
import com.flingtap.done.util.Constants;
import com.flurry.android.FlurryAgent;
import com.google.android.maps.GeoPoint;
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
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.net.Uri;
import android.os.Build;
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
 * 
 * @author spencer
 *
 */
public class ContactAttachHandler 
		extends AttachmentListAdapterDelegate 
		implements  View.OnCreateContextMenuListener, PostalContactMethodGeocoderPart.OnAddressGeocodedListener {

	public static final String TAG = "ContactAttachHandler";

	private final static int FIRST_CODE_ID = 400;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}
	
	// Menu Item IDs
	private final static int ATTACHMENT_MENU_VIEW_ITEM_ID   = FIRST_CODE_ID + 10;
	private static final int NEARMINDER_MENU_ADD_ITEM_ID 	= FIRST_CODE_ID + 15;
	private final static int ATTACHMENT_MENU_REMOVE_ITEM_ID = FIRST_CODE_ID + 20;
//	private final static int ATTACHMENT_MENU_RENAME_ITEM_ID = FIRST_CODE_ID + 25;

	// Dialogs
	private static final int DIALOG_REMOVE_ATTACHMENT_ID    = FIRST_CODE_ID + 50;
	
	// Result Codes
	private static final int INSERT_NEARMINDER_REQUEST      = FIRST_CODE_ID + 99;
//	protected final static int DELETE_ATTACHMENT_REQUEST      = FIRST_CODE_ID + 98;
	private static final int RESOLVE_POSITION_REQUEST       = FIRST_CODE_ID + 97;
	
	private static final int PICK_POSTAL_CONTACT_METHOD_REQUEST = FIRST_CODE_ID + 96;
//    private static final int RESOLVE_COORDINATES_REQUEST    = FIRST_CODE_ID + 95;
	private static final int CREATE_SHORTCUT_REQUEST 	      = FIRST_CODE_ID + 94;

	private Intent insertNearminderIntent = null;
	private AttachmentPart mAttachPart = null;
	private Uri proximityAlertUri = null;
	private PostalContactMethodGeocoderPart mGeocodeAddressPart = null;
	private Context mContext = null;
	
	public ContactAttachHandler(Context context, AttachmentPart attachPart, PostalContactMethodGeocoderPart geocodeAddressPart){
		try{
			assert null != attachPart;
			mAttachPart = attachPart;
			
			mContext = context;
			assert null != mContext;
			
			assert null != geocodeAddressPart;
			mGeocodeAddressPart = geocodeAddressPart;
			
			uriMappings = new UriMappings[3];
			
			// TODO: Couldn't the array index be used instead of adding the .code member? 
			uriMappings[0] = new UriMappings();
			uriMappings[0].authority = Contacts.AUTHORITY;
			uriMappings[0].pathPattern = "people/#";
			uriMappings[0].code = 0; // Uniquely identifies this mapping. Some Attachment handlers may handle multiple different mime-types so this allows us to distinguish between them. The value is passed into bindView(..)

			// content://com.android.contacts/contacts/lookup/*/#
			uriMappings[1] = new UriMappings();
			uriMappings[1].authority = "com.android.contacts";
			uriMappings[1].pathPattern = "contacts/lookup/*/#";
			uriMappings[1].code = 1; // Uniquely identifies this mapping. Some Attachment handlers may handle multiple different mime-types so this allows us to distinguish between them. The value is passed into bindView(..)
			
			// content://com.android.contacts/contacts/lookup/*
			uriMappings[2] = new UriMappings();
			uriMappings[2].authority = "com.android.contacts";
			uriMappings[2].pathPattern = "contacts/lookup/*";
			uriMappings[2].code = 2; // Uniquely identifies this mapping. Some Attachment handlers may handle multiple different mime-types so this allows us to distinguish between them. The value is passed into bindView(..)
						
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001B", exp);
			ErrorUtil.handleException("ERR0001B", exp, mContext);
		} 	
	}
	
	@Override
	protected void bindView(View view, Context context, Cursor cursor,
			int code, Uri data) {

		// Set the item height.
		RelativeLayout itemLayout = (RelativeLayout) view.findViewById(R.id.item_layout);

		// **************************************
		// Contact Name
		// **************************************
		TextView singleLineText = (TextView) view.findViewById(R.id.first_line);

		Cursor c = null;
		// TODO: !!!! Do this parsing in the ApplicationContext at startup time.
		final int sdkVersion = Integer.parseInt( Build.VERSION.SDK ); // Build.VERSION.SDK_INT was introduced after API level 3 and so is not compatible with 1.5 devices.
		if( 5 > sdkVersion ){ // Anrdoid 1.x series code.
			c = context.getContentResolver().query(data,
					new String[] { Contacts.People.NAME }, null, null,
					null);
		}else{ // Android 2.x series code.

			c = context.getContentResolver().query(data, // CONTENT_LOOKUP_URI
					new String[] { "display_name" }, null, null, // ContactsContract.ContactsColumns.DISPLAY_NAME
					null);
		}
			
		assert null != c;
		
		if( !c.moveToFirst() ){
			Toast toast = Toast.makeText(context, R.string.toast_removedBrokenAttachmentLink, Toast.LENGTH_LONG);
			toast.show();							
			Log.e(TAG, "OperationFailed. Failed to find attachment name." );
			
			// This condition is not really an error, but rather an opportunity to clean up.
			// Remove the broken attachment link.
			Uri attachURI = ContentUris.withAppendedId(Task.TaskAttachments.CONTENT_URI, cursor.getLong(TaskAttachmentListTab.PROJ_ATTACH_ID_INDEX));
			AttachmentPart.removeAttachmentImmediately(context, attachURI);
			c.close();
			return;
		}
		
		String personName = c.getString(0); // I know it's 0 index because it's the only column in the projection.
		c.close();
		singleLineText.setText(personName);

		// **************************************
		// Contact photo 
		// **************************************		
		ImageView contactPhoto = (ImageView) view.findViewById(R.id.left_icon);
		contactPhoto.setVisibility(View.VISIBLE);
		Bitmap contactPhotoBitmap = null;
		if( 5 > sdkVersion ){ // Anrdoid 1.x series code.
			contactPhotoBitmap = Contacts.People.loadContactPhoto(context, data, R.drawable.picture_holder , null); // android.graphics.BitmapFactory.Options options
		}else{ // Android 2.x series code.

			try {
				Uri contactUri = LocationUtil.lookupContactSDK5(mContext, data);
				if(null==contactUri){ 
					Log.e(TAG, "ERR000JM null==contactUri");
					ErrorUtil.handle("ERR000JM", "null==contactUri", mContext);
					contactPhotoBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.picture_holder); 
				}else{
					Class contactsClass = LocationUtil.findContactsContractsContactsClass();
					if( null == contactsClass ){
						// Error already documented.
						contactPhotoBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.picture_holder);
					}else{
						Method openMethod = contactsClass.getDeclaredMethod("openContactPhotoInputStream", ContentResolver.class, Uri.class);
						// InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(mContext.getContentResolver(), uri);
						InputStream input = (InputStream) openMethod.invoke(null, mContext.getContentResolver(), contactUri); // ContactsContract.Contacts.openContactPhotoInputStream(mContext.getContentResolver(), uri);
						try{
							if (input == null) {
								contactPhotoBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.picture_holder);
							}else{
								contactPhotoBitmap = BitmapFactory.decodeStream(input);
							}
						}finally{
							if(null != input){
								input.close();
							}
						}
					}
				}
			} catch (Exception e) {
				Log.e(TAG, "ERR000JL", e);
				ErrorUtil.handleException("ERR000JL", e, mContext);
				contactPhotoBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.picture_holder);
			}
		}
		contactPhoto.setImageBitmap(contactPhotoBitmap);
		
		// Hide checkbox
		CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);
		checkbox.setVisibility(View.GONE);
		
		// Hide second line
		TextView secondLineText = (TextView) view.findViewById(R.id.second_line);
		secondLineText.setVisibility(View.GONE);

	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo){
		try{
			
			//Log.v(TAG, "onCreateContextMenu(..) called.");
			AdapterView.AdapterContextMenuInfo mMenuInfo = (AdapterView.AdapterContextMenuInfo)menuInfo;
			Object[] tagArray = (Object[])((mMenuInfo.targetView).getTag());
			
			Uri locationUri = (Uri)(tagArray[AttachmentListAdapter.TAG_ATTACHMENTS_URI_INDEX]);
			
			TextView singleLineText = (TextView) mMenuInfo.targetView.findViewById(R.id.first_line);
			menu.setHeaderTitle(singleLineText.getText());
			
			if(LocationUtil.doesUriContainLocationInfo(mActivity, locationUri)){
				addProximityAlertMenuItems(menu, mMenuInfo.targetView);
			}
			
			// *******************
			// Add "Remove" menu item.
			// *******************
			addRemoveAttachmentMenuItems(menu, ATTACHMENT_MENU_REMOVE_ITEM_ID, R.string.context_removeContact);

			// *******************
			// Add "View" menu item.
			// *******************
			addViewAttachmentMenuItems(menu, ATTACHMENT_MENU_VIEW_ITEM_ID, R.string.context_viewContact);	
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001D", exp);
			ErrorUtil.handleException("ERR0001D", exp, mContext);
		} 	

	}
	
	protected void addProximityAlertMenuItems(ContextMenu menu, View selectedView){
		// Log.v(TAG, "addProximityAlertMenuItems(..) called.");

		// ***********************************
		// Add "Add Nearminder" menu item
		// ***********************************
		MenuItem addProximityAlertMenuItem = null;
		addProximityAlertMenuItem = menu.add(8, NEARMINDER_MENU_ADD_ITEM_ID,
				NEARMINDER_MENU_ADD_ITEM_ID, R.string.context_addNearminder);
	}

	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		try{
			if( resultCode == SharedConstant.RESULT_ERROR ){
				ErrorUtil.notifyUser(mActivity);
				return;
			}
			//Log.v(TAG, "onActivityResult(..) called");
			switch(requestCode){
				case PICK_POSTAL_CONTACT_METHOD_REQUEST:
					if( resultCode == Activity.RESULT_CANCELED){
						Log.e(TAG, "onActivityResult(PICK_POSTAL_CONTACT_METHOD_REQUEST) returned RESULT_CANCELED.");
						Toast.makeText(mActivity, R.string.toast_operationCanceled, Toast.LENGTH_SHORT).show();
						insertNearminderIntent = null;
						return;
					}
					handlePickPostalContactMethodActivityResult(data);
					return;
				case CREATE_SHORTCUT_REQUEST:
					if(resultCode == Activity.RESULT_OK && Intent.ACTION_CREATE_SHORTCUT.equals(insertNearminderIntent.getAction())) {
						mAttachPart.addAttachment((int)ContentUris.parseId(mIntent.getData()), (Intent)data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT ), data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME), (Bitmap)data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON), (Intent.ShortcutIconResource)data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE), null);
					}else{
						// TODO: Consider adding an event here.
						Toast.makeText(mActivity,  R.string.toast_operationCanceled, Toast.LENGTH_SHORT).show();
					}
				default:
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001E", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0001E", exp, mActivity);
		} 	

	}

	public boolean onContextItemSelected(MenuItem item) {
		try{
			AdapterView.AdapterContextMenuInfo mMenuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			
			switch (item.getItemId()) {
				case NEARMINDER_MENU_ADD_ITEM_ID:
					handleAddNearminderMenuItem(mMenuInfo);
					return true;
				case ATTACHMENT_MENU_REMOVE_ITEM_ID:
					mSelectedAttachId = mMenuInfo.id;
					mActivity.showDialog(DIALOG_REMOVE_ATTACHMENT_ID);
					return true;
				case ATTACHMENT_MENU_VIEW_ITEM_ID:
					Uri attachUri = ContentUris.withAppendedId(Task.TaskAttachments.CONTENT_URI, mMenuInfo.id);
					AttachmentPart.launchAttachDefaultAction(mActivity, attachUri, mMenuInfo.targetView);
					return true;
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001F", exp);
			ErrorUtil.handleException("ERR0001F", exp, mContext);
		} 	

        return false;
	}
	
	private void handleAddNearminderMenuItem(AdapterView.AdapterContextMenuInfo mMenuInfo) {
		
		// Start preparing the Intent.
		insertNearminderIntent = new Intent();

		// TODO: !!!! Do this parsing in the ApplicationContext at startup time.
		final int sdkVersion = Integer.parseInt( Build.VERSION.SDK ); // Build.VERSION.SDK_INT was introduced after API level 3 and so is not compatible with 1.5 devices.
		
		if( 5 > sdkVersion ){ // Anrdoid 1.x series code.
			// Call out to a sub activity to select the postal contact method. 
			// Get Person ID
			Object[] tagArray = (Object[])((mMenuInfo.targetView).getTag());
			Uri personUri = (Uri)(tagArray[AttachmentListAdapter.TAG_ATTACHMENTS_URI_INDEX]);
			// Create Intent
			Intent selectPostalContactMethodIntent = new Intent(Constants.PICK_POSTAL_CONTACT_METHOD_ACTION, personUri); 
			selectPostalContactMethodIntent.setComponent(new ComponentName(mActivity.getPackageName(),SelectPostalContactMethodActivity.class.getName()));		
			selectPostalContactMethodIntent.putExtra(SelectPostalContactMethodActivity.EXTRA_TASK_ID, mTaskId);
			mActivity.startActivityForResult(selectPostalContactMethodIntent, PICK_POSTAL_CONTACT_METHOD_REQUEST);
			
		}else{ // Android 2.x series code.
			
			// Call out to a sub activity to select the postal contact method. 
			// Get Person ID
			Object[] tagArray = (Object[])((mMenuInfo.targetView).getTag());
			Uri lookupUri = (Uri)(tagArray[AttachmentListAdapter.TAG_ATTACHMENTS_URI_INDEX]);
			
			try {
				Uri contactUri = LocationUtil.lookupContactSDK5(mActivity, lookupUri);
				
				// Create Intent
				Intent selectPostalContactMethodIntent = SelectPostalContactMethodActivitySDK5.getIntent(mActivity, contactUri, mTaskId);			
				mActivity.startActivityForResult(selectPostalContactMethodIntent, PICK_POSTAL_CONTACT_METHOD_REQUEST);
			} catch (Exception e) {
				Log.e(TAG, "ERR000JO", e);
				ErrorUtil.handleExceptionNotifyUser("ERR000JO", e, mContext);
				return;
			}
		}		
	}

	protected void handlePickPostalContactMethodActivityResult(Intent data){
		// ****************************
		// * Postal Contact Method URI
		// ****************************
		Uri selectedUri = data.getData();
		// Log.v(TAG, "postalContactMethodUri == " + postalContactMethodUri);
		if(selectedUri == null){
			// Whoops, bad data! Bail.
			Exception exp = (Exception)(new Exception("Missing or incomplete Contacts.People.CONTENT_URI.").fillInStackTrace());
			Log.e(TAG, "ERR0001G", exp);
			ErrorUtil.handleExceptionNotifyUserAndThrow("ERR0001G", exp, mActivity);
		}
		// TODO: Validate the postalContactMethodUri URI.
//		assert postalContactMethodUri.toString().startsWith(prefix);

		// ****************************************************************************
		// TODO: !! The efficiency of the following code would be better if you used a straight db connection and joined the tables.
		// ****************************************************************************
		
		// Create a Set of all the proximity ID's which are attached to the task.
		Cursor proximityAlertIdCursor = mActivity.getContentResolver().query(Task.TaskAttachments.CONTENT_URI, new String[]{Task.TaskAttachments._URI}, Task.TaskAttachments.TASK_ID + "=? AND " + Task.TaskAttachments._URI + " LIKE " + DatabaseUtils.sqlEscapeString(Task.ProximityAlerts.CONTENT_URI.toString() + "/%") , new String[]{String.valueOf((int)ContentUris.parseId(mIntent.getData()))}, null);
		assert null != proximityAlertIdCursor;
		final HashSet<String> proximityAlertIdSet = new HashSet<String>();
		while(proximityAlertIdCursor.moveToNext()){
			proximityAlertIdSet.add(proximityAlertIdCursor.getString(0).substring(proximityAlertIdCursor.getString(0).lastIndexOf('/')+1));
		}
		proximityAlertIdCursor.close();

		Iterator<String> itr = proximityAlertIdSet.iterator();
		StringBuffer sb = new StringBuffer();
		while(itr.hasNext()){
			sb.append(itr.next());
			if( itr.hasNext()){
				sb.append(',');
			}
		}
		// Find the proximity alert uri for this task and contact method.
		Cursor mContactMethodWithNearminderCursor = mActivity.getContentResolver().query(Task.ProximityAlerts.CONTENT_URI, new String[]{Task.ProximityAlerts._ID}, Task.ProximityAlerts._ID + " IN (" + sb + ") AND " + Task.ProximityAlerts._SELECTED_URI + " =?", new String[]{selectedUri.toString()}, null); 
		assert null != mContactMethodWithNearminderCursor;
		assert mContactMethodWithNearminderCursor.getCount() == 0 || mContactMethodWithNearminderCursor.getCount() == 1;

		if( mContactMethodWithNearminderCursor.moveToFirst() ){
			proximityAlertUri = ContentUris.withAppendedId(Task.ProximityAlerts.CONTENT_URI, Long.parseLong(mContactMethodWithNearminderCursor.getString(0)));
		}else{
			proximityAlertUri = null;
		}
		mContactMethodWithNearminderCursor.close();		
		
		if( null == proximityAlertUri ){ // Are we creating a new nearminder?
			insertNearminderIntent.setAction(Intent.ACTION_CREATE_SHORTCUT);
			insertNearminderIntent.setData(Task.ProximityAlerts.CONTENT_URI);
			insertNearminderIntent.putExtra(NearminderActivity.EXTRA_SELECTED_URI, selectedUri);
			
			// TODO: !!!! Do this parsing in the ApplicationContext at startup time.
			final int sdkVersion = Integer.parseInt( Build.VERSION.SDK ); // Build.VERSION.SDK_INT was introduced after API level 3 and so is not compatible with 1.5 devices.
			if( 5 > sdkVersion ){ // Anrdoid 1.x series code.

					Cursor personCursor = mActivity.getContentResolver().query(selectedUri, new String[]{Contacts.People.NAME}, null, null, null);
					assert null != personCursor;
					assert personCursor.getCount() == 1;
					if( personCursor.moveToFirst() ){
						insertNearminderIntent.putExtra(NearminderActivity.EXTRA_DEFAULT_NAME, personCursor.getString(0));
					}
					personCursor.close();

				mGeocodeAddressPart.resolveAddressPositionSDK3(selectedUri, this);
			}else{ // Android 2.x series code.
				
				// selectedUri is a Contact Data of type Postal. 
				Cursor postalContactDataCursor = mActivity.getContentResolver().query(selectedUri, new String[]{"display_name"}, null, null, null);
				assert null != postalContactDataCursor;
				assert postalContactDataCursor.getCount() > 0;
				try{
					if( postalContactDataCursor.moveToFirst() ){
						insertNearminderIntent.putExtra(NearminderActivity.EXTRA_DEFAULT_NAME, postalContactDataCursor.getString(0));
					}
				}finally{
					postalContactDataCursor.close();
				}
				
				mGeocodeAddressPart.resolveAddressPositionSDK5(selectedUri, this);
			}
			
			
		}else{ // no, we are editing an existing nearminder.
			Toast.makeText(mActivity, R.string.toast_nearminderAlreadyExists, Toast.LENGTH_LONG).show();
			
			insertNearminderIntent.setAction(Intent.ACTION_EDIT);
			insertNearminderIntent.setData( proximityAlertUri );

			// Ready to start NearminderActivity
			mActivity.startActivity(insertNearminderIntent);
			return;
		}
		
	}
	
	public void onAddressGeocodeCanceled() {
		//Log.v(TAG, "onAddressGeocodeCanceled() called.");
		Toast.makeText(mActivity, R.string.toast_operationCanceled, Toast.LENGTH_SHORT).show();
		return;	
	}

	public void onAddressGeocodeFound(GeoPoint geoPoint) {
		//Log.v(TAG, "onAddressGeocodeFound(...) called.");

		ParcelableGeoPoint parcelableGeoPoint = new ParcelableGeoPoint(geoPoint);
		insertNearminderIntent.putExtra(NearminderActivity.EXTRA_GLOBAL_POSITION, parcelableGeoPoint);
		 
		mActivity.startActivityForResult(insertNearminderIntent, CREATE_SHORTCUT_REQUEST);
	}	

	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}

	private long mSelectedAttachId = Constants.DEFAULT_NON_ID;
	public Dialog onCreateDialog(int dialogId){
		Dialog dialog = null;
		try{
			if (mManagedDialogs == null) {
				mManagedDialogs = new SparseArray<Dialog>();
			}
			switch(dialogId){
				case DIALOG_REMOVE_ATTACHMENT_ID:
					dialog = new AlertDialog.Builder(mActivity) // TODO: !! Refactor this code out and share it among all the attachment handlers. (need a separate onClickListener)
				        .setTitle(R.string.dialog_confirmRemoval)
				        .setIcon(android.R.drawable.ic_dialog_alert)
				        .setMessage(R.string.dialog_areYouSure)
				        .setPositiveButton(R.string.button_yes, new android.content.DialogInterface.OnClickListener(){
				    		public void onClick(DialogInterface dialog, int whichButton){
				    			try{
				    				if( whichButton == android.content.DialogInterface.BUTTON_POSITIVE){
				    					AttachmentPart.removeAttachmentImmediately(mActivity, ContentUris.withAppendedId(com.flingtap.done.provider.Task.TaskAttachments.CONTENT_URI, mSelectedAttachId)); 		
				    				}
				    			}catch(HandledException h){ // Ignore.
				    			}catch(Exception exp){
				    				Log.e(TAG, "ERR000C7", exp);
				    				ErrorUtil.handleExceptionNotifyUser("ERR000C7", exp, mActivity);
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
			Log.e(TAG, "ERR000C5", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000C5", exp, mActivity);
		}
		return dialog;
	}

	public void onPrepareDialog(int dialogId, Dialog dialog){
	}
	
	public static String queryForContactName(ContentResolver cr, long personId) {
		Cursor personCursor = cr.query(ContentUris.withAppendedId(android.provider.Contacts.People.CONTENT_URI, personId), new String[]{android.provider.Contacts.PeopleColumns.NAME}, null, null, null);
		assert null != personCursor;
		
		String description = null;
		if( personCursor.moveToFirst() ){
			description = personCursor.getString(0);
		}
		personCursor.close();
		return description;
	}

	@Override
	public boolean hasInstanceState() {
		return true;
	}

	private static final String SAVE_SELECTED_ATTACH_ID = "ContactAttachHandler.SELECTED_ATTACH_ID";
	private static final String SAVE_DEFAULT_NAME = "ContactAttachHandler.DEFAULT_NAME";
	private static final String SAVE_INSERT_NEARMINDER_INTENT = "ContactAttachHandler.INSERT_NEARMINDER_INTENT";
	private SparseArray<Dialog> mManagedDialogs;
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		try{
			//Log.v(TAG, "onSaveInstanceState() called");
			outState.putLong(SAVE_SELECTED_ATTACH_ID, mSelectedAttachId);
			if( null != insertNearminderIntent ){
				outState.putParcelable(SAVE_INSERT_NEARMINDER_INTENT,insertNearminderIntent);
			}
			
			saveManagedDialogs(outState, mManagedDialogs, TAG);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000C1", exp);
			ErrorUtil.handleExceptionFinish("ERR000C1", exp, mActivity);
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		try{
			//Log.v(TAG, "onRestoreInstanceState() called");
			mSelectedAttachId = savedInstanceState.getLong(SAVE_SELECTED_ATTACH_ID);
			insertNearminderIntent = savedInstanceState.getParcelable(SAVE_INSERT_NEARMINDER_INTENT);
			restoreManagedDialogs(savedInstanceState, mManagedDialogs, TAG);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000C2", exp);
			ErrorUtil.handleExceptionFinish("ERR000C2", exp, mActivity);
		}
	}
	
    public void onDestroy() {
    	onDestroy(mManagedDialogs);
    }	
}
