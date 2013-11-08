// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Address;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.ContactMethodsColumns;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

/**
 *  
 * 
 * @author spencer
 *
 */
public class ContactLocationOverlayItemsPart extends AbstractContextActivityParticipant implements PostalContactMethodGeocoderPart.OnAddressGeocodedListener {
	private static final String TAG = "ContactLocationOverlayItemsPart";
	
    public static interface OnOverlayItemsResolvedListener {
        public void onOverlayItemsResolved(ArrayList<OverlayItem> overlayItems); 
//        public void onOverlayItemsResolutionCanceled();
    }
	
	private Context mContext = null;
	private PostalContactMethodGeocoderPart mGeocodeAddressPart = null;
	public ContactLocationOverlayItemsPart(Context context, PostalContactMethodGeocoderPart geocodeAddressPart) {
		assert null != context;
		mContext = context;
		
		assert null != geocodeAddressPart;
		mGeocodeAddressPart = geocodeAddressPart;
	}
	
	@Override
	public int getFirstCodeId() {
		return IGNORE_CODE_ID;
	}

	private OnOverlayItemsResolvedListener mListener = null;
	
	private ArrayList<OverlayItem> overlayItems = null;
	private Iterator<Uri> itr = null;
	private Cursor mContactMethodPostalCursor = null;
	private String mContactData = null;
	private Long personId = -1l;
	/**
	 * Selects all the contact locations (contact methods) for the specified Uri (Person).
	 *    
	 * TODO Move this out to some sort of plugin.
	 * 
	 * @param uri A android.provider.Contacts.People.CONTENT_URI with id.
	 * @return
	 */
	public void queryContactLocationOverlayItems(ArrayList<Uri> uris, OnOverlayItemsResolvedListener listener) throws RuntimeException {

		assert null != listener;
		mListener = listener;
		
		overlayItems = new ArrayList<OverlayItem>();
		
		itr = uris.listIterator();
		if(itr.hasNext()){
			resolveForPerson(overlayItems);
		}else{
			mListener.onOverlayItemsResolved(overlayItems);
		}
	}

	
	private void resolveForPerson(ArrayList<OverlayItem> overlayItems) {
		personId = ContentUris.parseId(itr.next());
		if( personId < 0 ){
			// Whoops, bad data! Bail.
			Exception exp = (Exception)(new Exception("Missing or incomplete Contacts.People.CONTENT_URI.").fillInStackTrace());
			Log.e(TAG, "ERR0001H", exp);
			ErrorUtil.handleExceptionNotifyUserAndThrow("ERR0001H", exp, mContext);
		}
		
		// ******************************************************
		// Find the postal address that the user wants to use
		// ******************************************************
		
		// Get a list of the postal addresses
		mContactMethodPostalCursor = mContext.getContentResolver().query(ContactMethods.CONTENT_URI,
				ContactMethodProjectionGps.CONTACT_METHODS_PROJECTION, // TODO: This won't work for Androic 2.x and above phones. 
				ContactMethods.PERSON_ID+"=? AND "+ContactMethodsColumns.KIND+"=?", 
				new String[]{personId.toString(), String.valueOf(Contacts.KIND_POSTAL)}, 
				null); 
		if(mContactMethodPostalCursor.moveToNext()){
			Uri postalContactMethodUri = ContentUris.withAppendedId(ContactMethods.CONTENT_URI, mContactMethodPostalCursor.getInt(ContactMethodProjectionGps.CONTACT_M_ID_INDEX));
			mContactData = mContactMethodPostalCursor.getString(ContactMethodProjectionGps.CONTACT_M_DATA_INDEX);
			mGeocodeAddressPart.resolveAddressPosition(mContactData, postalContactMethodUri, this);
		}else{
			mContactMethodPostalCursor.close();
			personId = -1l;
			if(itr.hasNext()){
				resolveForPerson(overlayItems);
			}else{
				mListener.onOverlayItemsResolved(overlayItems);
			}			
		}
	}

	public void onAddressGeocodeCanceled() {
		// Ignored
	}

	public void onAddressGeocodeFound(GeoPoint geoPoint) {
		addOverlayItem(overlayItems, personId, geoPoint.getLatitudeE6(), geoPoint.getLongitudeE6(), mContactData);
		mContactData = null;
		
		if(mContactMethodPostalCursor.moveToNext()){
			mContactData = mContactMethodPostalCursor.getString(ContactMethodProjectionGps.CONTACT_M_DATA_INDEX);
			Uri postalContactMethodUri = ContentUris.withAppendedId(ContactMethods.CONTENT_URI, mContactMethodPostalCursor.getInt(ContactMethodProjectionGps.CONTACT_M_ID_INDEX));
			mGeocodeAddressPart.resolveAddressPosition(mContactData, postalContactMethodUri, this);
		}else{
			mContactMethodPostalCursor.close();
			personId = -1l;
			if(itr.hasNext()){
				resolveForPerson(overlayItems);
			}else{
				mListener.onOverlayItemsResolved(overlayItems);
			}
		}		
	}
	
	private void addOverlayItem(ArrayList<OverlayItem> overlayItems, Long personId, int latitudeE6, int longitudeE6, final String contactData) {
		final Context context = mContext;
		
		String description = ContactAttachHandler.queryForContactName(context.getContentResolver(), personId);
		// TODO: !! description could be null,, is that all right?
		
		overlayItems.add(new PlacemarkNavigatorOverlayItem(new GeoPoint(latitudeE6, longitudeE6), 
						description, 
						contactData, mContext));
	}	
}
