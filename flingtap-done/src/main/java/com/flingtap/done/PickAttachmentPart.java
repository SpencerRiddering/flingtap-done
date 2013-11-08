// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flingtap.done.util.LicenseUtil;
import com.flingtap.done.util.UriInfo;
import com.flurry.android.FlurryAgent;
import com.flingtap.done.base.R;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Contacts.People;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author spencer
 * 
 * FIXME: Verify shortcut creation code against stock Android reference apps. I'm getting a "Application not installed on your phone".
 * 
 */
public class PickAttachmentPart implements ContextActivityParticipant {
	
	private static final String TAG = "PickAttachmentPart";
	
	protected final static int FIRST_CODE_ID 	  = 300;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}
	
	private static final String ACTION_IMAGE_CAPTURE = "android.media.action.IMAGE_CAPTURE";
	public static final String EXTRA_DELETE_INTENT = "com.flingtap.done.intent.extra.DELETE_INTENT";
	
	// These values are all local so do not need to be globally unique
	protected static final int ATTACHMENT_TYPE_CONTACT_INDEX = 0;
	protected static final int ATTACHMENT_TYPE_MAP_INDEX 	 = 1;
	protected static final int ATTACHMENT_TYPE_NET_INDEX 	 = 2;
	protected static final int ATTACHMENT_TYPE_PHOTO_INDEX 	 = 3;	
	protected static final int ATTACHMENT_TYPE_MUSIC_INDEX 	 = 4;	
	protected static final int ATTACHMENT_TYPE_VIDEO_INDEX 	 = 5;	
	protected static final int ATTACHMENT_TYPE_TEST_INDEX 	 = 6;	
	protected static final int ATTACHMENT_TYPE_RECORDING_INDEX   = 7;	
	protected static final int ATTACHMENT_TYPE_CAMERA_INDEX 	 = 8;	
	protected static final int ATTACHMENT_TYPE_ACTIVITY_INDEX    = 9;
	protected static final int ATTACHMENT_TYPE_NEARMINDER_INDEX  = 10;	
	
	
	// From TaskEditor
	protected static final int PICK_CONTACT_REQUEST 		= FIRST_CODE_ID + 1;
	protected static final int PICK_IMAGE_REQUEST 			= FIRST_CODE_ID + 2;
	protected static final int PICK_MUSIC_REQUEST 			= FIRST_CODE_ID + 5;
	protected static final int PICK_VIDEO_REQUEST 			= FIRST_CODE_ID + 6;
//	protected static final int PICK_TEST_REQUEST 			= FIRST_CODE_ID + 7;	
//	protected static final int ADD_ATTACHMENT_REQUEST 		= FIRST_CODE_ID + 8;	
	protected static final int GET_RECORDING_CONTENT_REQUEST = FIRST_CODE_ID + 9;
	protected static final int CREATE_SHORTCUT_REQUEST 		= FIRST_CODE_ID + 10;
//	protected static final int CHOOSE_SHORTCUT_TYPE_REQUEST = FIRST_CODE_ID + 11;
	protected static final int CAPTURE_IMAGE_REQUEST 		= FIRST_CODE_ID + 12;
	protected static final int PICK_ACTIVITY_REQUEST 		= FIRST_CODE_ID + 13;

	protected static final int ADD_ATTACHMENT_DIALOG_ID     = FIRST_CODE_ID + 50;
	protected static final int MINDERS_ADDON_REQUIRED_DIALOG_ID     = FIRST_CODE_ID + 51;

	final static int ATTACHMENT_MENU_ADD_ITEM_ID    = FIRST_CODE_ID + 99; // TODO: Verify that this is unique
	
	protected Activity mActivity = null;
	protected Uri mURI = null;
	public void setUri(Uri muri) {
		mURI = muri;
	}

	protected AttachmentPart mAttachPart = null;
	
	public PickAttachmentPart(Activity activity, Uri uri, AttachmentPart attachPart){
		assert null != activity;
		mActivity = activity;
		assert null != uri;
		mURI = uri;
		assert null != attachPart;
		mAttachPart = attachPart;
	}

	/**
	 * Yes, we want our onCreateOptionsMenu(..) and onPrepareOptionsMenu(..) called.
	 */
	public boolean hasMenu(){
		return true;
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		// Add attachment option
		MenuItem addAttachmentMenuItem = menu.add(0, ATTACHMENT_MENU_ADD_ITEM_ID, 20,
				R.string.option_attach);
//		addAttachmentMenuItem.setShortcut('5', 'a'); // Could also use 'T'
//		addAttachmentMenuItem.setIcon(R.drawable.addattechment);	
		addAttachmentMenuItem.setIcon(android.R.drawable.ic_menu_add);	
		addAttachmentMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener(){

			public boolean onMenuItemClick(MenuItem menuitem) {
				try{
					// Prepare event info.
					Event.onEvent(Event.ADD_ATTACHMENT_OPTIONS_MENU_ITEM, null); // Map<String,String> parameters = new HashMap<String,String>();
					
					mActivity.showDialog(ADD_ATTACHMENT_DIALOG_ID);
					
					
				}catch(HandledException h){ // Ignore.
				}catch(Exception exp){
					Log.e(TAG, "ERR0004K", exp);
					ErrorUtil.handleExceptionNotifyUser("ERR0004K", exp, mActivity);
				}
				return true;
			}
			
		});
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}

	public void setIntent(Intent intent) {
	}

	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.v(TAG, "onActivityResult(..) called");
		if( resultCode != Activity.RESULT_OK ){ // User already toasted.
			return;
		}
		switch(requestCode){
			case CREATE_SHORTCUT_REQUEST: // NOTE: This case handles both shortcuts and ACTION_PICK.
				if(resultCode == Activity.RESULT_OK) {
					if( data.getData() != null ){ // If this is an ACTION_PICK result then ...

						Intent attachIntent = new Intent(Intent.ACTION_VIEW, data.getData());
						PackageManager pm = mActivity.getPackageManager();
//						
//						ActivityInfo actInfo;
//						try {
//							// TODO: !!! Assumes that all ACTION_PICK intents (ie data.getData() != null) are from the Contact App.
//							actInfo = pm.getActivityInfo(new ComponentName("com.android.contacts", "com.android.contacts.ContactShortcut"), 0); // TODO: !!! Isn't there a way to do this generically?
//						} catch (NameNotFoundException e) {
//							Log.e(TAG, "ERR0004M Failed to get activity info for ContactShortcut.", e);
//							ErrorUtil.handleExceptionNotifyUser("ERR0004M", e, mActivity);
//							return;
//						}
//						theName = actInfo.loadLabel(pm).toString();
//						// Create the bitmap.
//						// TODO: !!! Hack because contacts pulls it's icon from the contacts content provider not from this value. 
//						// NOTE: !!! Storing the image in the database will cause the database to become large much faster!
//						if( !data.getData().toString().startsWith(Task.Tasks.CONTENT_URI_STRING) ){ // TODO: !!! Assumes that all URIs that aren't Task URIs are from the Contact App.
//							BitmapDrawable bd = (BitmapDrawable)actInfo.loadIcon(pm); 
//							theBitmap = bd.getBitmap();
//						}
//						
////						theIconResource = Intent.ShortcutIconResource.fromContext(...); // Isn't suitable in this situation. 
						
						boolean isContact = data.getData().toString().startsWith(Contacts.CONTENT_URI.toString());
						
						UriInfo uriInfo = new UriInfo(attachIntent, data.getData(), mActivity.getContentResolver(), mActivity.getPackageManager(), mActivity.getResources(), null);
						// TODO: Add a preference to decide whether to Prompt user to edit name or give user an option to always use default value.  
//						mAttachPart.addAttachment((int)ContentUris.parseId(mURI), -1, attachIntent, uriInfo.getLabel(), isContact?null:uriInfo.getIconBitmap(), isContact?null:uriInfo.getIconResource(), null, null);
						mAttachPart.addAttachment((int)ContentUris.parseId(mURI), attachIntent, uriInfo.getLabel(), isContact?null:uriInfo.getIconBitmap(), isContact?null:uriInfo.getIconResource(), null);
						return;
						
					}else { // Is ACTION_CREATE_SHORTCUT 
						Intent attachIntent = (Intent)data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT );
						if( null != attachIntent && "android.intent.action.CALL".equals(attachIntent.getAction())){
							attachIntent.setAction(Intent.ACTION_DIAL);
						}
						Intent deleteIntent = (Intent)data.getParcelableExtra(EXTRA_DELETE_INTENT);
						String theName = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
						Bitmap theBitmap = (Bitmap)data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
						Intent.ShortcutIconResource theIconResource = (Intent.ShortcutIconResource)data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
						
//						mAttachPart.addAttachment((int)ContentUris.parseId(mURI), -1, attachIntent, theName, theBitmap, theIconResource, null, deleteIntent);		
						mAttachPart.addAttachment((int)ContentUris.parseId(mURI), attachIntent, theName, theBitmap, theIconResource, deleteIntent);		
					}
				}else{
					// TODO: !!! Add error handling code here.
				}
				break;
			case PICK_ACTIVITY_REQUEST:
				if (resultCode == Activity.RESULT_OK) {
					addPickActivityAttachment(data);
				}else{
					// TODO: !!! Add error handling code here.
				}
				break;
				
//				case PICK_VIDEO_REQUEST: 
			case CAPTURE_IMAGE_REQUEST:
			case GET_RECORDING_CONTENT_REQUEST: 
			case PICK_MUSIC_REQUEST: 
			case PICK_IMAGE_REQUEST: 
			case PICK_VIDEO_REQUEST:
			case PICK_CONTACT_REQUEST: 
				if (resultCode == Activity.RESULT_OK) {
					addGenericAttachment(data);
				}else{
					// TODO: !!! Add error handling code here.
				}
				break;
			default:
				ErrorUtil.handle("ERR0003T", "onActivityResult() called with unknown resultCode " + resultCode, this);
		}
		
	}

	private void addGenericAttachment(Intent data) {
		Intent attachIntent = new Intent(Intent.ACTION_VIEW, data.getData());
		UriInfo uriInfo = new UriInfo(attachIntent, data.getData(), mActivity.getContentResolver(), mActivity.getPackageManager(), mActivity.getResources(), null);
		// TODO: Add a preference to decide whether to Prompt user to edit name or give user an option to always use default value.  
//		mAttachPart.addAttachment((int)ContentUris.parseId(mURI), -1, attachIntent, uriInfo.getLabel(), uriInfo.getIconBitmap(), uriInfo.getIconResource(), null, null);
		mAttachPart.addAttachment((int)ContentUris.parseId(mURI), attachIntent, uriInfo.getLabel(), uriInfo.getIconBitmap(), uriInfo.getIconResource(), null);
	}
	
	private void addPickActivityAttachment(Intent data) {
		Intent attachIntent = new Intent(Intent.ACTION_MAIN);
		attachIntent.setComponent(data.getComponent());
		UriInfo uriInfo = new UriInfo(attachIntent, data.getData(), mActivity.getContentResolver(), mActivity.getPackageManager(), mActivity.getResources(), null);
		// TODO: Add a preference to decide whether to Prompt user to edit name or give user an option to always use default value.  
//		mAttachPart.addAttachment((int)ContentUris.parseId(mURI), -1, attachIntent, uriInfo.getLabel(), uriInfo.getIconBitmap(), uriInfo.getIconResource(), null, null);
		mAttachPart.addAttachment((int)ContentUris.parseId(mURI), attachIntent, uriInfo.getLabel(), uriInfo.getIconBitmap(), uriInfo.getIconResource(), null);
	}
	
//	/**
//	 * This is the correct way to initiate the add attachment process. Unfortunately the inbuilt applications (Contact, Pictures, etc..) are not configured correctly so an alternative method is used instead.
//	 * 
//	 */
//	private void addAttachment() {
//		Intent intent = new Intent(Intent.ACTION_PICK);
//		intent.setType("*/*");
//		Intent chooserIntent = Intent.createChooser(intent, "this is a title");
//		mActivity.startActivityForResult(chooserIntent , ADD_ATTACHMENT_REQUEST);
//	}
//	private void addAttachment() {
//		Intent intent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
//		Intent chooserIntent = Intent.createChooser(intent, "this is a title");
//		mActivity.startActivityForResult(chooserIntent , CREATE_SHORTCUT_REQUEST);
//	}
	
// This is just test code.
//	private void addAttachment() {
////		Intent intent = new Intent(Intent.ACTION_PICK);
////		intent.setData(Uri.parse("content:"));
////		intent.setComponent(new ComponentName("*","*"));
////		startActivityForResult(intent , ADD_ATTACHMENT_REQUEST);
//
////		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
////		Intent intent = new Intent(Intent.ACTION_PICK);
////		intent.setType("*/*");
////		startActivityForResult(intent , ADD_ATTACHMENT_REQUEST);
//		
//		Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
//		intent.setData(Uri.parse("content://contacts/people/1"));
////		startActivityForResult(intent , ADD_ATTACHMENT_REQUEST);
//		
//		Intent chooserIntent = Intent.createChooser(intent, "this is a title");
//		startActivityForResult(chooserIntent , ADD_ATTACHMENT_REQUEST);
//		
//	}
	
	
	protected static final String ADD_ATTACH_LABEL = "ADD_ATTACH_LABEL"; 
	protected static final String ADD_ATTACH_RETURN_DATA = "ADD_ATTACH_RETURN_DATA"; 
	protected static final String ADD_ATTACH_DRAWABLE_RESOURCE = "ADD_ATTACH_DRAWABLE_RESOURCE"; 
	
	protected static final String ADD_ATTACH_DRAWABLE = "ADD_ATTACH_DRAWABLE"; 
	protected static final String ADD_ATTACH_INTENT = "ADD_ATTACH_INTENT"; 
	
	/**
	 * TODO: Add support for additional data types.
	 */
	private Dialog addAttachment() {
		final ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> hm;

		final PackageManager pm = mActivity.getPackageManager();
		Intent queryCreateShortcutIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
		List<ResolveInfo> createShortcutResolveInfoList = pm.queryIntentActivities(queryCreateShortcutIntent, PackageManager.MATCH_DEFAULT_ONLY|PackageManager.GET_RESOLVED_FILTER|PackageManager.GET_INTENT_FILTERS );		
		filterByPriorityAndDefaultFlag(pm, createShortcutResolveInfoList);

		
// Works, but the standard Android application's support for PICK is spotty at best. 	
//		Intent queryPickIntent = new Intent(Intent.ACTION_PICK);
//		queryPickIntent.setType("*/*");
//		List<ResolveInfo> pickActionResolveInfoList = pm.queryIntentActivities(queryPickIntent, PackageManager.MATCH_DEFAULT_ONLY|PackageManager.GET_RESOLVED_FILTER|PackageManager.GET_INTENT_FILTERS );		
////		sortAndFilterByPriority(pm, pickActionResolveInfoList);
//		filterByPriorityAndDefaultFlag(pm, pickActionResolveInfoList);

// No need to merge if only one set (ACTION_CREATE_SHORTCUT) is used.		
//		// Merge the two lists: 
////		TreeSet<ResolveInfo> mergedSet = new TreeSet<ResolveInfo>(new android.content.pm.ResolveInfo.DisplayNameComparator(pm));
//		TreeSet<ResolveInfo> mergedSet = new TreeSet<ResolveInfo>(new Comparator<ResolveInfo>(){
//			Comparator<ResolveInfo> comparator = new android.content.pm.ResolveInfo.DisplayNameComparator(pm);
//			public int compare(ResolveInfo object1, ResolveInfo object2) {
//				Log.v(TAG, "Compare: " + object1.activityInfo.packageName + " with " + object2.activityInfo.packageName);
//				if( object1.activityInfo.packageName.equals(object2.activityInfo.packageName)){
//					Log.v(TAG, object1.activityInfo.packageName + " == " + object2.activityInfo.packageName);
//					if( object1.activityInfo.name.equals(object2.activityInfo.name)){
//						Log.v(TAG, object1.activityInfo.name + " == " + object2.activityInfo.name);
//						return 0;
//					}else{
//						return comparator.compare(object1, object2);
////						return object1.activityInfo.packageName.compareTo(object2.activityInfo.packageName);
//					}
//				}else{
//					return comparator.compare(object1, object2);
////					return object1.activityInfo.packageName.compareTo(object2.activityInfo.packageName);
//				}
//			}
//			
//		});
//      // TODO: !! This merge code seems to loose some entries as it merges,, not sure why.
//		mergedSet.addAll(createShortcutResolveInfoList);
//		mergedSet.addAll(pickActionResolveInfoList);
		List<ResolveInfo> mergedSet = createShortcutResolveInfoList;
		
//		Collections.sort(resolveInfoList, new android.content.pm.ResolveInfo.DisplayNameComparator(pm));

		final int sdkVersion = Integer.parseInt( Build.VERSION.SDK ); // Build.VERSION.SDK_INT was introduced after API level 3 and so is not compatible with 1.5 devices.
		
		// TODO: !!! Move this list to a server and download periodically. User should be able to flag applications as not correctly supporting the standard. 
		// Filter out badly behaving intents
		TreeSet<String> filterSetName = null;
		TreeSet<String> filterSetPackage = null;
		if( sdkVersion <= 4 ){
			filterSetName = new TreeSet<String>();
			filterSetName.add("com.android.music.ArtistAlbumBrowserActivity");
			filterSetName.add("com.android.music.NowPlayingActivity");
			filterSetName.add("com.android.music.TrackBrowserActivity");
			filterSetName.add("org.jetpad.quicktodofree.ItemsList");
			filterSetName.add("com.android.music.VideoBrowserActivity");
			filterSetName.add("com.android.music.MusicPicker");
			filterSetName.add("com.example.android.apis.app.CreateShortcuts");
			filterSetName.add("com.android.contacts.ContactsListActivity");
			filterSetName.add("com.android.contacts.ContactShortcut");
			filterSetPackage = new TreeSet<String>();
			filterSetPackage.add("e2m.android");
			filterSetPackage.add("mobi.doogle.doogle");
			filterSetPackage.add("net.kazed.nextaction");
			filterSetPackage.add("com.android.im");
			
		}
 		
		
		for(ResolveInfo resolveInfo: mergedSet){
			if( sdkVersion <= 4 ){
				//pm.getApplicationLabel(resolveInfo.activityInfo.applicationInfo);			
				if( filterSetName.contains(resolveInfo.activityInfo.name) || 
						filterSetPackage.contains(resolveInfo.activityInfo.packageName)){
					continue;
				}
			}
			
			hm = new HashMap<String, Object>();
			
			Intent createShortcutIntent = resolveInfo.filter.hasAction(Intent.ACTION_CREATE_SHORTCUT)?new Intent(Intent.ACTION_CREATE_SHORTCUT):new Intent(Intent.ACTION_PICK);
			ComponentName cn = new ComponentName(resolveInfo.activityInfo.packageName,resolveInfo.activityInfo.name);
			createShortcutIntent.setComponent(cn);
			hm.put(ADD_ATTACH_INTENT, createShortcutIntent);
			
			// Add icon
//			try {
//				hm.put(ADD_ATTACH_DRAWABLE, pm.getActivityIcon(cn));
//			} catch (NameNotFoundException e) {
//				Log.i(TAG, "Failed to find drawable because package returned by PackageManager was not found. Strange,, right?", e);
//			}
			hm.put(ADD_ATTACH_DRAWABLE, resolveInfo.loadIcon(pm));
			
			// Add Label
//			CharSequence label = null;
//			if( null == (label = resolveInfo.loadLabel(pm)) ){
//				if( null == (label = pm.getText(resolveInfo.activityInfo.packageName, resolveInfo.labelRes, resolveInfo.activityInfo.applicationInfo)) ){
//					label = pm.getApplicationLabel(resolveInfo.activityInfo.applicationInfo);
//				}
//			}
            CharSequence label = resolveInfo.loadLabel(pm);
            if(label == null){
                label = resolveInfo.activityInfo.name;
            }			
			hm.put(ADD_ATTACH_LABEL, label);
			data.add(hm);
		}


		hm = new HashMap<String, Object>();
		hm.put(ADD_ATTACH_LABEL,mActivity.getText(R.string.type_image));
		hm.put(ADD_ATTACH_RETURN_DATA, ATTACHMENT_TYPE_PHOTO_INDEX);
		Intent photoPickerIntent = new Intent(Intent.ACTION_VIEW);
        photoPickerIntent.setType("image/*");
		try {
			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getPackageManager().getActivityIcon(photoPickerIntent));
		} catch (NameNotFoundException e) {
			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getResources().getDrawable(R.drawable.question));
		}		
		data.add(hm);
		
//		// ***********************************************************
//		// Music
//		// ***********************************************************
//		hm = new HashMap<String, Object>();
//		hm.put(ADD_ATTACH_LABEL,R.string.attachment_type_music_label);
//		hm.put(ADD_ATTACH_RETURN_DATA, ATTACHMENT_TYPE_MUSIC_INDEX);
//		hm.put(ADD_ATTACH_DRAWABLE, R.drawable.largephoto);
//		data.add(hm);

//		// ***********************************************************
//		// Play Music
//      //   <string name="type_recordAudio">Record audio</string>		
//		// ***********************************************************
//		hm = new HashMap<String, Object>();
//		hm.put(ADD_ATTACH_LABEL,mActivity.getText(R.string.type_recordAudio));
//		hm.put(ADD_ATTACH_RETURN_DATA, ATTACHMENT_TYPE_MUSIC_INDEX);
//		Intent playMusicIntent = new Intent(Intent.ACTION_VIEW);//ACTION_GET_CONTENT
//		playMusicIntent.setType("audio/*");
//		try {
//			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getPackageManager().getActivityIcon(playMusicIntent));
//		} catch (NameNotFoundException e) {
//			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getResources().getDrawable(R.drawable.question));
//		}		
//		data.add(hm);
		
		// ***********************************************************
		// Audio
		// ***********************************************************
		hm = new HashMap<String, Object>();
		hm.put(ADD_ATTACH_LABEL,mActivity.getText(R.string.type_audio));
		hm.put(ADD_ATTACH_RETURN_DATA, ATTACHMENT_TYPE_RECORDING_INDEX);
		Intent recodinGetContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
		recodinGetContentIntent.setComponent(new ComponentName("com.android.soundrecorder","com.android.soundrecorder.SoundRecorder"));
		recodinGetContentIntent.setType("audio/*");
		try {
			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getPackageManager().getActivityIcon(recodinGetContentIntent));
		} catch (NameNotFoundException e) {
			try{
				recodinGetContentIntent.setComponent(null);
				hm.put(ADD_ATTACH_DRAWABLE, mActivity.getPackageManager().getActivityIcon(recodinGetContentIntent));
			} catch (NameNotFoundException e2) {
				hm.put(ADD_ATTACH_DRAWABLE, mActivity.getResources().getDrawable(R.drawable.question));
			}			
		}		
		data.add(hm);
				
//		// ***********************************************************
//		// Gallery
//		//    <string name="attachment_type_gallery_label">Gallery</string>		
//		// ***********************************************************
//		hm = new HashMap<String, Object>();
//		hm.put(ADD_ATTACH_LABEL,mActivity.getText(R.string.attachment_type_gallery_label));
//		hm.put(ADD_ATTACH_RETURN_DATA, ATTACHMENT_TYPE_MEDIA_GALLERY_INDEX);
//		Intent galleryIntent = new Intent(Intent.ACTION_PICK);
//		galleryIntent.setComponent(new ComponentName("com.android.camera","com.android.camera.ImageGallery2"));
//		try {
//			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getPackageManager().getActivityIcon(galleryIntent));
//		} catch (NameNotFoundException e) {
//			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getResources().getDrawable(R.drawable.question));
//		}		
//		data.add(hm);
		
		// ***********************************************************
		// Get Video
		// ***********************************************************
		hm = new HashMap<String, Object>();
		hm.put(ADD_ATTACH_LABEL,mActivity.getText(R.string.type_video));
		hm.put(ADD_ATTACH_RETURN_DATA, ATTACHMENT_TYPE_VIDEO_INDEX);
//		Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
		Intent videoPickIntent = new Intent(Intent.ACTION_VIEW);
		videoPickIntent.setType("video/*");
		try {
			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getPackageManager().getActivityIcon(videoPickIntent));
		} catch (NameNotFoundException e) {
			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getResources().getDrawable(R.drawable.question));
		}		
		data.add(hm);
//		
//		
//		// ***********************************************************
//		// Get Image
//		// ***********************************************************
//		hm = new HashMap<String, Object>();
//		hm.put(ADD_ATTACH_LABEL,mActivity.getText(R.string.type_image));
//		hm.put(ADD_ATTACH_RETURN_DATA, ATTACHMENT_TYPE_PHOTO_INDEX);
////		Intent imageIntent = new Intent(Intent.ACTION_GET_CONTENT);
//		Intent imageIntent = new Intent(Intent.ACTION_PICK);
//		imageIntent.setType("image/*");
//		try {
//			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getPackageManager().getActivityIcon(imageIntent));
//		} catch (NameNotFoundException e) {
//			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getResources().getDrawable(R.drawable.question));
//		}		
//		data.add(hm);		
		
		// ***********************************************************
		// Contact
		// ***********************************************************
		// This code should only be used for 1.5 --> 1.x code (API level <= 4). Doesn't work in 2.0 (API level >= 5).
		if( sdkVersion <= 4 ){
			hm = new HashMap<String, Object>();
			hm.put(ADD_ATTACH_LABEL,mActivity.getText(R.string.type_contact));
			hm.put(ADD_ATTACH_RETURN_DATA, ATTACHMENT_TYPE_CONTACT_INDEX);
	//		Intent contactGetContentIntent = new Intent(Intent.ACTION_PICK);
	//		contactGetContentIntent.setType("vnd.android.cursor.item/person");
	//		try {
	//			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getPackageManager().getActivityIcon(contactGetContentIntent));
	//		} catch (NameNotFoundException e) {
	//			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getResources().getDrawable(R.drawable.question));
	//		}
				hm.put(ADD_ATTACH_DRAWABLE, mActivity.getResources().getDrawable(R.drawable.ic_launcher_shortcut_contact));
				data.add(hm);		
		}
		
		// ***********************************************************
		// Activity
		// ***********************************************************
		hm = new HashMap<String, Object>();
		hm.put(ADD_ATTACH_RETURN_DATA, ATTACHMENT_TYPE_ACTIVITY_INDEX);
//		Intent pickActivityIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
//		Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
//		launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//		pickActivityIntent.putExtra(Intent.EXTRA_INTENT, launcherIntent);
		Intent pickActivityIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
		pickActivityIntent.setComponent(new ComponentName("com.android.settings","com.android.settings.ActivityPicker"));
//		try {6
			//hm.put(ADD_ATTACH_DRAWABLE, mActivity.getPackageManager().getActivityIcon(pickActivityIntent));
			hm.put(ADD_ATTACH_DRAWABLE_RESOURCE, R.drawable.ic_launcher_application);
			hm.put(ADD_ATTACH_LABEL, mActivity.getText(R.string.applications));
			//hm.put(ADD_ATTACH_LABEL, mActivity.getPackageManager().queryIntentActivities(pickActivityIntent, PackageManager.MATCH_DEFAULT_ONLY).get(0).activityInfo.loadLabel(mActivity.getPackageManager()));
			//ActivityInfo(new ComponentName("com.android.settings","com.android.settings.ActivityPicker"), 0).applicationInfo.loadLabel(mActivity.getPackageManager()));
//		} catch (NameNotFoundException e) { 
//			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getResources().getDrawable(R.drawable.question));
//		}
		data.add(hm);		
		
		
////		hm = new HashMap<String, Object>();
////		hm.put(ADD_ATTACH_LABEL, R.string.type_contact);
////		hm.put(ADD_ATTACH_RETURN_DATA, ATTACHMENT_TYPE_CONTACT_INDEX);
////		hm.put(ADD_ATTACH_DRAWABLE_RESOURCE, R.drawable.largecontact);
////		data.add(hm);
//		
//		
////		// ***********************************************************
////		// Capture Image
////		//    <string name="attachment_type_capture_image">Take photo</string>		
////		// ***********************************************************
////		hm = new HashMap<String, Object>();
////		hm.put(ADD_ATTACH_LABEL,mActivity.getText(R.string.attachment_type_capture_image));
////		hm.put(ADD_ATTACH_RETURN_DATA, ATTACHMENT_TYPE_CAMERA_INDEX);
////		Intent captureImageIntent = new Intent(ACTION_IMAGE_CAPTURE);
////		try {
////			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getPackageManager().getActivityIcon(captureImageIntent));
////		} catch (NameNotFoundException e) {
////			hm.put(ADD_ATTACH_DRAWABLE, mActivity.getResources().getDrawable(R.drawable.question));
////		}		
////		data.add(hm);
				

		
		// ***********************************************************
		// Nearminder
		// ***********************************************************
		hm = new HashMap<String, Object>();
		hm.put(ADD_ATTACH_LABEL,mActivity.getText(R.string.type_nearminder));
		hm.put(ADD_ATTACH_RETURN_DATA, ATTACHMENT_TYPE_NEARMINDER_INDEX);		
		hm.put(ADD_ATTACH_DRAWABLE, mActivity.getResources().getDrawable(R.drawable.ic_launcher_nearminder));
		data.add(hm);		
		
		
		// ***********************************************************
		// Sub-task 
		// ***********************************************************
		hm = new HashMap<String, Object>();
		hm.put(ADD_ATTACH_LABEL,mActivity.getText(R.string.activity_completable));
		Intent completableIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT, Task.Completable.CONTENT_URI);
		ComponentName cn = new ComponentName(mActivity.getPackageName(), CompletableEditor.class.getName());
		completableIntent.setComponent(cn);
		hm.put(ADD_ATTACH_INTENT, completableIntent);
		hm.put(ADD_ATTACH_DRAWABLE, mActivity.getResources().getDrawable(R.drawable.ic_launcher_completable));
		data.add(hm);		

		
		// ***********************************************************
		// Sort entries into alphabetic order. 
		// ***********************************************************
		Collections.sort(data, new Comparator<HashMap<String,Object>>(){

			public int compare(HashMap<String,Object> object1, HashMap<String,Object> object2) {
				CharSequence charSec1 = (CharSequence)object1.get(ADD_ATTACH_LABEL);
				CharSequence charSec2 = (CharSequence)object2.get(ADD_ATTACH_LABEL);
				return charSec1.toString().compareTo(charSec2.toString());
			}
			
		});		
		
		
		Collections.sort(data, new Comparator<HashMap<String,Object>>(){

			public int compare(HashMap<String,Object> object1, HashMap<String,Object> object2) {
				CharSequence charSec1 = (CharSequence)object1.get(ADD_ATTACH_LABEL);
				CharSequence charSec2 = (CharSequence)object2.get(ADD_ATTACH_LABEL);
				return charSec1.toString().compareTo(charSec2.toString());
			}
			
		});

		
		LeanAdapter<HashMap<String, Object>> attachmentAdapter = new LeanAdapter<HashMap<String, Object>>(mActivity, R.layout.list_dialog_item) {

			@Override
			public void bindView(View view, Context context, HashMap<String, Object> data,	int position) {
				try{ 
					// Set icon
					Drawable icon = (Drawable)data.get(ADD_ATTACH_DRAWABLE);
					if( null == icon ){
						icon = mActivity.getResources().getDrawable((Integer)data.get(ADD_ATTACH_DRAWABLE_RESOURCE));
					}
					ImageView iv = (ImageView)view.findViewById(R.id.list_dialog_item_icon);
					iv.setImageDrawable(icon);
					
					// Set dialog text 1
					String label = (String)data.get(ADD_ATTACH_LABEL);
					TextView tv = (TextView)view.findViewById(R.id.list_dialog_item_text1);
					tv.setText(label);
					
//				// Set tag
//				Intent launchIntent = (Intent)data.get(ADD_ATTACH_INTENT);
//				view.setTag(launchIntent);
				}catch(HandledException h){ // Ignore.
				}catch(Exception exp){
					Log.e(TAG, "ERR0004L", exp);
					ErrorUtil.handleExceptionNotifyUser("ERR0004L", exp, mActivity);
				}

			}
			@Override
			public HashMap<String, Object> getItem(int position) {
				return data.get(position);
			}
			public int getCount() {
				return data.size();
			}
			@Override
			public long getItemId(int position) {
				return position;
			}
		};		
		
		AlertDialog dialog = new AlertDialog.Builder(mActivity)
//     dialog.setIcon(mIcon);
       		.setTitle(R.string.dialog_attachmentType)
       		.setAdapter(attachmentAdapter, new DialogInterface.OnClickListener() {
       			public void onClick(DialogInterface dialog, int whichButton) {
       				try{
           				//Log.v(TAG, "whichButton == " + whichButton);
           				
    					Integer choice = (Integer)data.get(whichButton).get(ADD_ATTACH_RETURN_DATA);
           				if( null == choice ){
           					Intent launchIntent = (Intent)data.get(whichButton).get(ADD_ATTACH_INTENT);
           					// launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // DON'T!!! New tasks cannot return a result! 
           					mActivity.startActivityForResult(launchIntent, CREATE_SHORTCUT_REQUEST);
           				}else{
           					launchAttachementPicker(choice);
           				}
    					if( null != dialog ){
    						dialog.dismiss();
    					}
       				}catch(HandledException h){ // Ignore.
       				}catch(Exception exp){
       					Log.e(TAG, "ERR0004P", exp);
       					ErrorUtil.handleExceptionNotifyUser("ERR0004P", exp, mActivity);
       				}
	           }
       		})
       		.create();
//		dialog.show();
		return dialog;

	}

	/**
	 * Code from ResolverActivity.ResolveListAdapter which is used by ChooserActivity.
	 */
	private void filterByPriorityAndDefaultFlag(PackageManager pm,
			List<ResolveInfo> resolveInfoList) {
		if(resolveInfoList != null) {
            int N = resolveInfoList.size();
            if(N > 1) {
                ResolveInfo r0 = (ResolveInfo)resolveInfoList.get(0);
                for(int i = 1; i < N; i++){
                    ResolveInfo ri = (ResolveInfo)resolveInfoList.get(i);
                    if(r0.priority == ri.priority && r0.isDefault == ri.isDefault)
                        continue;
                    for(; i < N; N--)
                    	resolveInfoList.remove(i);
                }
//                Collections.sort(resolveInfoList, new android.content.pm.ResolveInfo.DisplayNameComparator(pm));
            }
        }
	}
	
	public void launchAttachementPicker(int choice) {
		switch( choice ){
//			case ATTACHMENT_TYPE_CONTACT_INDEX:
//				mActivity.startActivityForResult(new Intent(Intent.ACTION_PICK,
//					android.provider.Contacts.People.CONTENT_URI), // new ContentURI("content://contacts/people");
//					PICK_CONTACT_REQUEST);
//				break;
//			case ATTACHMENT_TYPE_CONTACT_INDEX:
//				Intent contactIntent = new Intent(Intent.ACTION_GET_CONTENT); // ACTION_PICK
//				contactIntent.setType(android.provider.Contacts.People.CONTENT_ITEM_TYPE);
//				mActivity.startActivityForResult(contactIntent, PICK_CONTACT_REQUEST);
//			break;
			case ATTACHMENT_TYPE_CONTACT_INDEX:
				Intent contactIntent = new Intent(Intent.ACTION_PICK); //ACTION_GET_CONTENT, ACTION_PICK , ACTION_CREATE_SHORTCUT
				contactIntent.setClass(mActivity, ContactsListActivity.class);
				contactIntent.setType(android.provider.Contacts.People.CONTENT_TYPE); // CONTENT_ITEM_TYPE
				mActivity.startActivityForResult(contactIntent, PICK_CONTACT_REQUEST);
			break;
			
			case ATTACHMENT_TYPE_ACTIVITY_INDEX:
				Intent activityPickerIntent = new Intent(Intent.ACTION_PICK_ACTIVITY); 
				//activityPickerIntent.setComponent(new ComponentName("com.android.settings","com.android.settings.ActivityPicker"));
				Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
				launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
				activityPickerIntent.putExtra(Intent.EXTRA_INTENT, launcherIntent);
				mActivity.startActivityForResult(activityPickerIntent, PICK_ACTIVITY_REQUEST); // TODO: !!! Long delay here!
			break;
			case ATTACHMENT_TYPE_PHOTO_INDEX:
				Intent photoPickerIntent = new Intent();
				photoPickerIntent.setAction(Intent.ACTION_PICK);
//				photoPickerIntent.setAction("android.media.action.IMAGE_CAPTURE"); // Doesn't work. Says "inline-data" but I don't get a URI.
//				photoPickerIntent.setAction(Intent.ACTION_GET_CONTENT); // Doesn't currently work.
//				photoPickerIntent.setComponent(new ComponentName("com.android.camera", "com.android.camera.Camera")); // Starts the camera but does not receive the result afterwards.
                photoPickerIntent.setType("image/*");
                
                mActivity.startActivityForResult(photoPickerIntent, PICK_IMAGE_REQUEST); 
				break;
			case ATTACHMENT_TYPE_MUSIC_INDEX:
				Intent musicPickerIntent = new Intent();
				musicPickerIntent.setAction(Intent.ACTION_PICK);
//				musicPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
//				musicPickerIntent.setAction("android.intent.action.RINGTONE_PICKER");
//				musicPickerIntent.setComponent(new ComponentName("com.android.music", "MediaPickerActivity")); 
				musicPickerIntent.setType("audio/*");
                mActivity.startActivityForResult(musicPickerIntent, PICK_MUSIC_REQUEST); 
				break;
			case ATTACHMENT_TYPE_RECORDING_INDEX:
				//MediaStore.Audio.Media.RECORD_SOUND_ACTION;
				Intent recodinGetContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
				recodinGetContentIntent.setType("audio/*");
                mActivity.startActivityForResult(recodinGetContentIntent, GET_RECORDING_CONTENT_REQUEST); 
				break;
			case ATTACHMENT_TYPE_CAMERA_INDEX:
				Intent captureImageIntent = new Intent(ACTION_IMAGE_CAPTURE);
                mActivity.startActivityForResult(captureImageIntent, CAPTURE_IMAGE_REQUEST); 
				break;
				
			case ATTACHMENT_TYPE_NEARMINDER_INDEX:
				if(LicenseUtil.hasLicense(mActivity, LicenseUtil.FEATURE_NEARMINDER)){
					Intent nearminderIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT, Task.ProximityAlerts.CONTENT_URI);
	                mActivity.startActivityForResult(nearminderIntent, CREATE_SHORTCUT_REQUEST); 
//					//Intent nearminderIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
////					ComponentName cn = new ComponentName(StaticConfig.PACKAGE_NAME, "com.flingtap.done.NearminderActivity");
////					nearminderIntent.setComponent(cn);
////					nearminderIntent.putExtra(NearminderActivity.EXTRA_TASK_ID, (int)ContentUris.parseId(mURI));
//					hm.put(ADD_ATTACH_INTENT, nearminderIntent);
				}else{
					mActivity.showDialog(MINDERS_ADDON_REQUIRED_DIALOG_ID);
				}
				break;
				
				
				
			case ATTACHMENT_TYPE_VIDEO_INDEX:
				Intent videoPickerIntent = new Intent();
				videoPickerIntent.setAction(Intent.ACTION_PICK);
//				videoPickerIntent.setAction(Intent.ACTION_GET_CONTENT); // Doesn't work.
				videoPickerIntent.setType("video/*");
//				videoPickerIntent.setComponent(new ComponentName("com.android.music", "com.android.music.VideoBrowserActivity")); // Starts the camera but does not receive the result afterwards.
				
				
                mActivity.startActivityForResult(videoPickerIntent, PICK_VIDEO_REQUEST); 
				break;
				
			case ATTACHMENT_TYPE_TEST_INDEX:
	//			try {
					Intent intent = new Intent();
	//				intent.setType("application/vnd.google-earth.kml+xml");
	//				intent.setType("application/vnd.google-earth.kmz");
	//				intent.setType("audio/mpeg");  
	//				intent.setType("text/plain");  
	//				intent.setType("text/html");  
	//				intent.setType("application/x-gps");  
	//				intent.setType("application/vnd.google.gmm");
	//				intent.setType("video/mpeg");  
	//				intent.setType("video/mp4");  
	//				intent.setType("application/mp4");  
					intent.setType("image/jpeg"); // Unsupported action - That action not currently supported.
	//				intent.setType("image/png");  // Unsupported action - That action not currently supported.
	//				intent.setType("com.google.android.maps/com.google.android.maps.Maps");  
	//				intent.setType("com.google.android.browser/com.google.android.browser.BrowserBookmarksPage");  
	//				intent.setType("com.google.android.browser/com.google.android.browser.AddBookmarkPage");  
					
	//				ComponentName cn = new ComponentName("com.google.android.maps", "com.google.android.maps.Maps");
	//				ComponentName cn = new ComponentName("com.google.android.browser", "com.google.android.browser.BrowserBookmarksPage");
	//				ComponentName cn = new ComponentName("com.google.android.browser", "com.google.android.browser.AddBookmarkPage");
	//				intent.setComponent(cn); 
	//				intent.setType("vnd.riddering.cursor.item/vnd.riddering.task");  
	//				getContentResolver().getProvider(com.google.android.providers.settings.SettingsProvider);
					
					
					// action=android.intent.action.MAIN launchFlags=4 comp={com.google.android.maps/com.google.android.maps.Maps} }
					
					
	//				intent.setAction(Intent.PICK_ACTION);
					intent.setAction(Intent.ACTION_GET_CONTENT);
	//				intent.setAction(Intent.MAIN_ACTION);
	//				intent.setAction(Intent.SEARCH_ACTION);
	
	// action=android.intent.action.SEARCH
	// launchFlags=4 
	// comp={com.google.android.maps/com.google.android.maps.Maps} 
	// extras=Bundle[{app_data=Bundle[{centerLatitude=44.903004, zoomLevel=13, latitudeSpan=0.110402, longitudeSpan=0.109862, centerLongitude=-93.00394}], query=pizza}] }   				
					
					Bundle appDataBundle = new Bundle();
					appDataBundle.putString("centerLatitude","44.903004");
					appDataBundle.putString("centerLongitude","-93.00394");
					appDataBundle.putString("zoomLevel","13");
					appDataBundle.putString("latitudeSpan","0.110402");
					appDataBundle.putString("longitudeSpan","0.109862");
					intent.putExtra("app_data",appDataBundle);
					intent.putExtra("query", "pizza");
					mActivity.startActivityForResult(intent,
	//				startSubActivity(new Intent(Intent.GET_CONTENT_ACTION),// 
							
	//						new Intent(Intent.VIEW_ACTION,
	//						new Intent(Intent.PICK_ACTION,
							// android.provider.SubscribedFeeds.Feeds.CONTENT_URI),
							// android.provider.Calendar.Events.CONTENT_URI),
							//android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI),
	//								new ContentURI("geo:38.899533,-77.036476")),
	//								new ContentURI("geo:")),
							PICK_IMAGE_REQUEST);
	//			} catch (URISyntaxException e) {
	//				Log.e(TAG, "Failed to start GEO pick activity.", e);
	//			}
				break;
			default:
				ErrorUtil.handleExceptionNotifyUser("ERR0004N", (Exception)(new Exception( String.valueOf(choice) )).fillInStackTrace(), mActivity);
		}
	}
	
	public Dialog onCreateDialog(int dialogId){
		switch(dialogId){
			case ADD_ATTACHMENT_DIALOG_ID:
				return addAttachment();
			case MINDERS_ADDON_REQUIRED_DIALOG_ID:
				return AddonRequiredDialogPart.onCreateDialog(mActivity, 
						R.string.dialog_addOnNeeded, 
						R.string.dialog_thisFeatureIsPartOfTheMindersAddOnAndMustBeDownloadedSeparately, 
						Uri.parse("http://market.android.com/search?q=pname:com.flingtap.done.addon.minders"));
			default:
				return null;
		}
	}

	public void onPrepareDialog(int dialogId, Dialog dialog){
	}
	
	public boolean hasInstanceState(){
		return false;
	}

	public void  onSaveInstanceState  (Bundle outState){
	}
	
	public void  onRestoreInstanceState  (Bundle savedInstanceState){
	}
	
    public void onDestroy() {
    }


}
