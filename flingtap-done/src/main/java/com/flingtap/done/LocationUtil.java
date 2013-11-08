// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import com.flingtap.done.provider.Task;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.ContactMethodsColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 
 * @author spencer
 *
 */
public class LocationUtil {
	
	private static final String TAG = "LocationUtil";

	public static boolean doesUriContainLocationInfo(Context ctx, Uri uri) throws RuntimeException {
		//Log.d(TAG, "uri.toString()=="+uri.toString());
		
// Location extension point. 
//		if(uri.toString().startsWith(GeoMap.GeoMapBookmarks.CONTENT_URI.toString())) {
//			return true;
//		}else 
		
		// TODO: !!!! Do this parsing in the ApplicationContext at startup time.
		final int sdkVersion = Integer.parseInt( Build.VERSION.SDK ); // Build.VERSION.SDK_INT was introduced after API level 3 and so is not compatible with 1.5 devices.
		
		if( 5 > sdkVersion ){ // Anrdoid 1.x series code.
			if(uri.toString().startsWith(android.provider.Contacts.People.CONTENT_URI.toString())){
				return LocationUtil.doesContactUriContainLocationInfoSDK3(ctx, uri);
			}else{
				return false;
			}
			
		}else{ // Android 2.x series code.
			if(uri.toString().startsWith("content://com.android.contacts/contacts/lookup")){
				return LocationUtil.doesContactUriContainLocationInfoSDK5(ctx, uri);
			}else{
				return false;
			}			
		}			
//		}else{
//			// TODO: !!! Choosing option menu "search services" from the attachments tab results in this exception being thrown.
//			// TODO: !!! Long clicking a "Task" task attachment results in this exception being thrown.
//
//// NOTE: Why is the error code below here? If there is no location info,, just return false,, right? 			
////			Exception exp = (Exception)(new Exception("Wrong URI type. " + uri).fillInStackTrace());
////			Log.e(TAG, "ERR0003G Wrong URI type.", exp);
////			ErrorUtil.handleExceptionNotifyUserAndThrow("ERR0003G", exp, ctx);
//			return false;
//		}
	}
	
	
	/**
	 * Checks whether the contact refered to by the URI contains any postal contact methods.
	 *    Postal contact methods usually can be geocoded and thus can be used for proximity alerts.
	 *    
	 * TODO Move this out to some sort of plugin.
	 * 
	 * @param uri A android.provider.Contacts.People.CONTENT_URI with id.
	 * @return
	 */
	protected static boolean doesContactUriContainLocationInfoSDK3(Context ctx, Uri uri) throws RuntimeException {
		
		Long personId = ContentUris.parseId(uri);
		if( personId < 0 ){
			// Whoops, bad data! Bail.
			Exception exp = (Exception)(new Exception("URI missing id.").fillInStackTrace());
			Log.e(TAG, "ERR0003F URI missing id.");
			ErrorUtil.handleExceptionNotifyUserAndThrow("ERR0003F", exp, ctx);
		}
		
		// ******************************************************
		// Find the postal address that the user wants to use
		// ******************************************************
		
		// Get a list of the postal addresses
		Cursor mContactMethodPostalCursor = ctx.getContentResolver().query(ContactMethods.CONTENT_URI,
				ContactMethodProjectionGps.CONTACT_METHODS_PROJECTION, 
				ContactMethods.PERSON_ID+"=? AND "+ContactMethodsColumns.KIND+"=?", 
				new String[]{personId.toString(), String.valueOf(Contacts.KIND_POSTAL)}, 
				null); 
		int count = mContactMethodPostalCursor.getCount();
		mContactMethodPostalCursor.close();

		// Zero postal addresses
		if( count < 1){ 
			// No postal addresses exist, so no GPS locations can exist, right? 
			return false;			
		}
		return true;
	}

	
	/**
	 * Checks whether the contact refered to by the URI contains any postal contact methods.
	 *    Postal contact methods usually can be geocoded and thus can be used for proximity alerts.
	 *    
	 * TODO Move this out to some sort of plugin.
	 * 
	 * @param uri A android.provider.Contacts.People.CONTENT_URI with id.
	 * @return
	 */
	protected static boolean doesContactUriContainLocationInfoSDK5(Context ctx, Uri uri) throws RuntimeException {
		// Need to convert the lookup id to a contact id.
		Uri contactUri = null;
		try {
			contactUri = LocationUtil.lookupContactSDK5(ctx, uri);
		} catch (Exception e) {
			Log.e(TAG, "ERR000JA");
			ErrorUtil.handleExceptionNotifyUserAndThrow("ERR000JA", e, ctx);
		}
		if( null == contactUri ){
			Exception exp = (Exception)(new Exception("Unable to find contactUri.").fillInStackTrace());
			Log.e(TAG, "ERR000JB Unable to find contactUri.");
			ErrorUtil.handleExceptionNotifyUserAndThrow("ERR000JB", exp, ctx);			
		}
		
		Long contactId = ContentUris.parseId(contactUri);
		if( contactId < 0 ){
			// Whoops, bad data! Bail.
			Exception exp = (Exception)(new Exception("URI missing id.").fillInStackTrace());
			Log.e(TAG, "ERR000J4 URI missing id.");
			ErrorUtil.handleExceptionNotifyUserAndThrow("ERR000J4", exp, ctx);
		}
		
		// ******************************************************
		// Find the postal address that the user wants to use
		// ******************************************************

//		Cursor mimetypeCursor = ctx.getContentResolver().query(Uri.parse("content://com.android.contacts/mimetypes"),
//				new String[]{"_id"},
//		                 "mimetype='vnd.android.cursor.item/postal-address_v2'", // ContactsContract.Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE
//		          null, null);		
//		assert null != mimetypeCursor;
//		assert mimetypeCursor.getCount() == 1;
//		String mimeTypeId = null;
//		try{
//			mimeTypeId = mimetypeCursor.getString(0);
//		}finally{
//			mimetypeCursor.close();
//		}
//		if( null == mimeTypeId ){
//			Exception exp = (Exception)(new Exception("Unable to find mimetype.").fillInStackTrace());
//			Log.e(TAG, "ERR000JK Unable to find mimetype.");
//			ErrorUtil.handleException("ERR000JK", exp, ctx);			
//			return false;
//		}
		
		// Get a list of the postal contact data (ie addresses)
		Cursor mContactMethodPostalCursor = ctx.getContentResolver().query(Uri.parse("content://com.android.contacts/data"),
				null,
//				ContactMethodProjectionGps.CONTACT_CONTRACT_DATA_PROJECTION, // TODO: !!!! Why bother passing a projection?
		          "contact_id=?" + " AND " // ContactsContract.Data.CONTACT_ID
                  + "mimetype=?", // ContactsContract.Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE
//                + "mimetype_id=?", // ContactsContract.Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE
		          new String[] {String.valueOf(contactId), "vnd.android.cursor.item/postal-address_v2"}, null);		
//		          new String[] {String.valueOf(contactId), mimeTypeId}, null);		
		assert null != mContactMethodPostalCursor;
		try{
			// Zero postal addresses
			if( mContactMethodPostalCursor.getCount() == 0){ 
				// No postal addresses exist, so no GPS locations can exist, right? 
				return false;			
			}
		}finally{
			mContactMethodPostalCursor.close();
		}
		return true;
	}	

	public static Class findContactsContractsContactsClass() throws Exception {
		Class contactsContractClass = Class.forName("android.provider.ContactsContract");
				
//		Class[] contactsContractClasses = contactsContractClass.getDeclaredClasses();
//		//Class contactsClass = Class.forName("android.provider.ContactsContract.Contacts");
//		for(Class checkClass: contactsContractClasses){
//			String canonicalName = checkClass.getCanonicalName();
//			if("android.provider.ContactsContract.Contacts".equals(checkClass.getCanonicalName())){
//				return checkClass;				
//			}
//		}
//		return null;
		return findInnerClass(contactsContractClass, "android.provider.ContactsContract.Contacts");
	}
	
	public static Class findInnerClass(Class theClass, String name){
		if( (null == theClass) || (null == name) ){
			return null;
		}
		//Class contactsClass = Class.forName("android.provider.ContactsContract.CommonDataKinds");
		Class[] contactsContractClasses = theClass.getDeclaredClasses();
		for(Class checkClass: contactsContractClasses){
			if(name.equals(checkClass.getCanonicalName())){
				return checkClass;				
			}
		}
		return null;
	}
	
	public static Class findContactsContractsCommonDataKindsStruturedPostalClass() throws Exception {
		Class contactsContractClass = Class.forName("android.provider.ContactsContract");
		
		//Class contactsClass = Class.forName("android.provider.ContactsContract.CommonDataKinds");
//		Class[] contactsContractClasses = contactsContractClass.getDeclaredClasses();
//		for(Class checkClass: contactsContractClasses){
//			if("android.provider.ContactsContract.CommonDataKinds".equals(checkClass.getCanonicalName())){
//				return checkClass;				
//			}
//		}
		Class commonDataKindsClass = findInnerClass(contactsContractClass, "android.provider.ContactsContract.CommonDataKinds");
		if( null != commonDataKindsClass ){
			return findInnerClass(commonDataKindsClass, "android.provider.ContactsContract.CommonDataKinds.StructuredPostal");
		}
		return null;
	}
	
	public static Uri lookupContactSDK5(Context context, Uri lookupUri) throws Exception {
		Class contactsClass = LocationUtil.findContactsContractsContactsClass();
		if( null != contactsClass ){
			Method lookupContactMethod = contactsClass.getMethod("lookupContact", ContentResolver.class, Uri.class);
			Object contactUriObject = lookupContactMethod.invoke(null, context.getContentResolver(), lookupUri);
			if( null == contactUriObject ){
				// Whoops, bad data! Bail.
				Log.e(TAG, "ERR000J9");
				ErrorUtil.handleException("ERR000J9", (Exception)(new Exception( lookupUri.toString() )).fillInStackTrace(), context);
				return null;						
			}
			return (Uri)contactUriObject;
		}else{
			Log.e(TAG, "ERR000JN contactsClass not found.");
			ErrorUtil.handle("ERR000JN", "contactsClass not found.", context);
			return null;
		}
		
	}

	
}
