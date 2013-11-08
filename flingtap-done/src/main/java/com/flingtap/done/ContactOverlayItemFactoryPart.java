// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.ArrayList;

import com.google.android.maps.GeoPoint;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethodsColumns;

/**
 * 
 * 
 * 
 * TODO: User should have the option to select a group of contacts to display in case they have a huge number of contacts. 
 * TODO: Warn user if they have a huge number of contacts to display. 
 *
 * TODO: ! Loading contact overlay items should be moved to a background thread/service.
 */
public class ContactOverlayItemFactoryPart extends AbstractContextActivityParticipant {
	private static final String TAG = "ContactOverlayItemFactoryPart";
	
	private Context mContext = null;
	private PostalContactMethodGeocoderPart mPostalContactMethodGeocoderPart = null;

	
	public ContactOverlayItemFactoryPart(Context context, PostalContactMethodGeocoderPart postalContactMethodGeocoderPart){
		assert null != context;
		mContext = context;
		
		assert null != postalContactMethodGeocoderPart;
		mPostalContactMethodGeocoderPart = postalContactMethodGeocoderPart;
	}
	
	public ArrayList<SelectAreaOverlayItem> buildOverlayList(){
		ContentResolver cr = mContext.getContentResolver();
		Cursor contactMethodCursor = cr.query(
				Contacts.ContactMethods.CONTENT_URI, 
				ContactMethodProjectionGps.CONTACT_METHODS_PROJECTION, // TODO: This won't work for Androic 2.x and above phones.
				ContactMethodsColumns.KIND+"=?", 
				new String[]{String.valueOf(Contacts.KIND_POSTAL)}, 
				null);
		
		ArrayList<SelectAreaOverlayItem> items = new ArrayList<SelectAreaOverlayItem>();

		if( 0 < contactMethodCursor.getCount() ){
			
			CharSequence displayName = null;
			CharSequence label = null;			
			GeoPoint geoPoint = null;
			
			while( contactMethodCursor.moveToNext() ){
				
				// Get GeoPoint
				Uri contactMethodUri = ContentUris.withAppendedId(Contacts.ContactMethods.CONTENT_URI, contactMethodCursor.getInt(ContactMethodProjectionGps.CONTACT_M_ID_INDEX));
				geoPoint = mPostalContactMethodGeocoderPart.resolveAddressPosition(contactMethodUri);
				if( null == geoPoint ){
					continue;
				}

				label = Contacts.ContactMethods.getDisplayLabel(mContext, contactMethodCursor.getInt(ContactMethodProjectionGps.CONTACT_M_KIND_INDEX), contactMethodCursor.getInt(ContactMethodProjectionGps.CONTACT_M_KIND_INDEX), contactMethodCursor.getString(ContactMethodProjectionGps.CONTACT_M_LABEL_INDEX));
				if( null == label ){
					continue;
				}
				
				String[] displayNameProjection = new String[]{Contacts.PeopleColumns.DISPLAY_NAME};
				Uri personUri = ContentUris.withAppendedId(Contacts.People.CONTENT_URI, contactMethodCursor.getLong(ContactMethodProjectionGps.CONTACT_M_PERSON_ID_INDEX)); 
				Cursor mPersonCursor = cr.query(
						personUri, 
						displayNameProjection, 
						null, 
						null,
						null);
				if( !mPersonCursor.moveToFirst() ){
					continue;
				}
				displayName = mPersonCursor.getString(0);
				mPersonCursor.close();
				
				items.add(new SelectAreaOverlayItem(geoPoint, displayName.toString(), label.toString()));			
			}
		}
		contactMethodCursor.close();
		
		return items; 
	}


	
}
