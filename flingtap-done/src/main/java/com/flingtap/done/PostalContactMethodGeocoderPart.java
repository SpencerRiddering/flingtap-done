// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.android.maps.GeoPoint;
import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.ContactMethodsColumns;
import android.provider.Contacts.People;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Resolves a contact method URI into a GeoPoint, optionally prompting the user to resolve ambiguous situations. 
 *
 * TODO: Handle condition where the contact method's address changes (externally) but the GPS position is not updated.
 *       Contact Methods does not contain a "last updated" field (up to SDK 4), so it's difficult to notice changes. Could cache the address and check for changes.
 */
public class PostalContactMethodGeocoderPart extends AbstractContextActivityParticipant {
	public static final String TAG = "PostalContactMethodGeocoderPart";
	
    public static interface OnAddressGeocodedListener {
        public void onAddressGeocodeFound(GeoPoint geoPoint); 
        public void onAddressGeocodeCanceled();
    }
	
	private static final int FIRST_CODE_ID = 900;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}
	
	protected static final int DIALOG_SELECT = FIRST_CODE_ID + 01;
//	protected static final int DIALOG_UPDATE = FIRST_CODE_ID + 02;

	protected Activity mActivity = null;
	public PostalContactMethodGeocoderPart(Activity activity){
		mActivity = activity;
		settings = mActivity.getSharedPreferences(ApplicationPreference.NAME, Activity.MODE_PRIVATE);
	}
	
	protected OnAddressGeocodedListener mListener = null;
    protected final List<Address> mAddresses = new ArrayList<Address>();
    protected String mAddress = null;
    
	public GeoPoint resolveAddressPositionSDK5(Uri postalContactDataUri, OnAddressGeocodedListener listener){
		Cursor postalContactDataCursor = mActivity.getContentResolver().query(postalContactDataUri,
				ContactMethodProjectionGps.CONTACT_CONTRACT_DATA_PROJECTION, null, null, null);
		assert null != postalContactDataCursor;
		try{
			if( !postalContactDataCursor.moveToFirst()){
				// Whoops, bad data! Bail.
				Log.e(TAG, "ERR000JJ Failed to query ContactMethods for GPS info.");
				ErrorUtil.handle("ERR000JJ", "Failed to query ContactMethods for GPS info.", this);
	    		if( null != listener ){ // TODO: !! Consider not calling the onAddressGeocodeCanceled() method in this case.
    				listener.onAddressGeocodeCanceled();
    			}
    			return null;
			}
			String address = postalContactDataCursor.getString(ContactMethodProjectionGps.CONTACT_CONTRACT_DATA_FORMATTED_ADDRESS_INDEX); 
			return internalResolveAddressPosition(address, postalContactDataUri, listener);
		}finally{
			postalContactDataCursor.close();
		}
	}

	public GeoPoint resolveAddressPositionSDK3(Uri postalContactMethodUri, OnAddressGeocodedListener listener){
		// **********************************************************************
		// TODO: !! Check cache (not implemented yet) for a GPS position for this URI.
		// **********************************************************************
		
		Cursor mContactMethodPostalCursor = mActivity.getContentResolver().query(postalContactMethodUri,
				ContactMethodProjectionGps.CONTACT_METHODS_PROJECTION, ContactMethodsColumns.KIND+"=?", new String[]{String.valueOf(Contacts.KIND_POSTAL)}, null);
		assert null != mContactMethodPostalCursor;
		if( !mContactMethodPostalCursor.moveToFirst()){
			// Whoops, bad data! Bail.
			Log.e(TAG, "ERR0004R Failed to query ContactMethods for GPS info.");
			ErrorUtil.handle("ERR0004R", "Failed to query ContactMethods for GPS info.", this);
    		if( null != listener ){ // TODO: !! Consider not calling the onAddressGeocodeCanceled() method in this case.
    			listener.onAddressGeocodeCanceled();
    		}
    		return null;
		}
		String address = mContactMethodPostalCursor.getString(ContactMethodProjectionGps.CONTACT_M_DATA_INDEX); 
		mContactMethodPostalCursor.close();
		
		return internalResolveAddressPosition(address, postalContactMethodUri, listener);
	}
	
	private Uri mPostalContactMethodUri = null;
    
	public GeoPoint resolveAddressPosition(Uri postalContactMethodUri){
		return resolveAddressPositionSDK3(postalContactMethodUri, null);
	}
	public void resolveAddressPosition(String address, Uri postalContactMethodUri, OnAddressGeocodedListener listener){
		internalResolveAddressPosition(address, postalContactMethodUri, listener);
	}
//	public void resolveAddressPosition(String address, OnAddressGeocodedListener listener){
	private GeoPoint internalResolveAddressPosition(String address, Uri postalContactMethodUri, OnAddressGeocodedListener listener){
		//Log.v(TAG, "resolveAddressPosition(..) called.");
		
		mListener = listener;
		
		assert null != address;
		mAddress = address;
		//Log.d(TAG, "address=="+address);
		
		assert null != postalContactMethodUri;
		mPostalContactMethodUri = postalContactMethodUri;

		String addressContactData = address.replace("\n",", "); // TODO: !! Verify this logic

        Geocoder g = new Geocoder(mActivity, Locale.getDefault());

        try {
			// TODO: Re-evaluate how to handle the max number of addresses returned. Currently set to 5.
        	// TODO: Re-evaluate the longitude/latitude range. Where should this info come from?
        	List<Address> tmpAddresses = g.getFromLocationName(addressContactData, 5); // TODO: !!! This call will block, so a "waiting" box should be displayed.
        	mAddresses.clear();
        	mAddresses.addAll(tmpAddresses);
        } catch (IOException ioe2) {
        	Log.e(TAG, "ERR0004S Failed to retrieve location info for address due to communication issue. Exiting");
        	Toast.makeText(mActivity, R.string.toast_communicationError, Toast.LENGTH_SHORT).show();
			ErrorUtil.handle("ERR0004S", "Communication error", this);
    		if( null != mListener ){ // TODO: !! Consider not calling the onAddressGeocodeCanceled() method in this case.        	
    			listener.onAddressGeocodeCanceled();
    		}
    		return null;
        } 			
            
        assert null != mAddresses;
        if (mAddresses.size() == 0) {
			//Log.w(TAG, "ERR0004T No locations found for address. Exiting");
			Toast.makeText(mActivity, "Address not recognized.", Toast.LENGTH_SHORT).show();
			ErrorUtil.handle("ERR0004T", "Address not recognized.", this);
    		if( null != mListener ){
    			listener.onAddressGeocodeCanceled();
    		}
    		return null;
        }
        if (mAddresses.size() == 1) {
// The code below is the correct code, but I removed it because Geocoder is chopping off the street number which breaks this code.        	
//        	Address lookupAddress = mAddresses.get(0);
//    		StringBuilder sb = new StringBuilder();
//    		for(int i=0; i < lookupAddress.getMaxAddressLineIndex(); i++){
//    			sb.append(lookupAddress.getAddressLine(i));
//    		}        	
//        	String lookupString = sb.toString().replaceAll("[\\s\\-,]*", "");
//        	
//        	String inputString = address.replaceAll("[\\s\\-,]*", "");
//        	if( inputString.equalsIgnoreCase(lookupString)){
//        		if( null == mListener ){
//    	        	return makeGeoPoint(lookupAddress);
//        		}else{
//        			listener.onAddressGeocodeFound(makeGeoPoint(lookupAddress));
//        		}
//        	}
        	
        	// TODO !!! This is a hack, correct code is above. 
        	Address lookupAddress = mAddresses.get(0);
    		if( null == mListener ){
	        	return makeGeoPoint(lookupAddress);
			}else{
				listener.onAddressGeocodeFound(makeGeoPoint(lookupAddress));
				return null;
			}
        	
        }
        if( null != mListener ){
        	mActivity.showDialog(DIALOG_SELECT);
        }
        return null;
    }

	@Override
	public void onPrepareDialog(int dialogId, Dialog dialog) {
		switch(dialogId){
			case DIALOG_SELECT:
				prepareDialogTitle();
				break;
		}
	}
	
	private TextView dialogTitleText = null; 	
	private final SharedPreferences settings;
	private CheckBox checkBoxView = null;
	private Address address = null;
	private LeanAdapter<Address> addressAdapter = null;
	
	public Dialog onCreateDialog(int dialogId) {
		
		LayoutInflater lf = (LayoutInflater)mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		switch(dialogId){
			case DIALOG_SELECT:
				// ********************************************************************
				// It's here that the user should be prompted to resolve any ambiguity 
				// ********************************************************************
				//Log.d(TAG, "addresses.size()=="+mAddresses.size());
				assert null != mAddresses;
				addressAdapter = new LeanAdapter<Address>(mActivity, R.layout.address_list_dialog_item){

					public int getCount() {
						return mAddresses.size();
					}
					
					public Address getItem(int position) {
						return mAddresses.get(position);
					}
					boolean actOnIt = true;
					public void bindView(View view, Context context, Address data, int position) {
						try{
							((TextView)view.findViewById(R.id.address_text1)).setText(data.toString());
							
							if( data.getMaxAddressLineIndex() >= 0 ){
								((TextView)view.findViewById(R.id.address_text1)).setText(data.getAddressLine(0));
								if( data.getMaxAddressLineIndex() >= 1 ){
									StringBuilder sb = new StringBuilder();
									sb.append(data.getAddressLine(1));
									if( data.getMaxAddressLineIndex() >= 2 ){
										sb.append(", ");
										sb.append(data.getAddressLine(2));
									}
									((TextView)view.findViewById(R.id.address_text2)).setText(sb);
								}
								
							}else{
								// TODO Strange error condition.
								ErrorUtil.handle("ERR0004V", "Strange error condition.", this);
							}
						}catch(HandledException h){ // Ignore.
						}catch(Exception exp){
							if( actOnIt ){
								Log.e(TAG, "ERR0004U", exp);
								ErrorUtil.handleExceptionNotifyUser("ERR0004U", exp, context);
								actOnIt = false;
							}
						}
					}

					@Override
					public long getItemId(int position) {
						return position;
					}
					
				};
				View title = lf.inflate(R.layout.address_list_dialog_title, null);
				ImageView iv = (ImageView)title.findViewById(R.id.address_list_dialog_icon);
				iv.setImageResource(android.R.drawable.ic_dialog_info);
				dialogTitleText = (TextView)title.findViewById(R.id.address_list_dialog_title);
				// dialogTitleText will be initialized later in onPrepareDialog(..) with prepareDialogTitle();
				
				AlertDialog dialog = new AlertDialog.Builder(mActivity)
				.setCustomTitle(title)
				.setSingleChoiceItems(addressAdapter, -1, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which) {
						try{
							address = mAddresses.get(which);
							mActivity.dismissDialog(DIALOG_SELECT);//dialog.dismiss();
							
							mListener.onAddressGeocodeFound(makeGeoPoint(address));
						}catch(HandledException h){ // Ignore.
						}catch(Exception exp){
							Log.e(TAG, "ERR0004W", exp);
							ErrorUtil.handleExceptionFinish("ERR0004W", exp, mActivity);
						}
					}
					
					
				})
				.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						try{
							mActivity.dismissDialog(DIALOG_SELECT);//dialog.dismiss(); 
							mListener.onAddressGeocodeCanceled();
						}catch(HandledException h){ // Ignore.
						}catch(Exception exp){
							Log.e(TAG, "ERR0004X", exp);
							ErrorUtil.handleExceptionFinish("ERR0004X", exp, mActivity);
						}
					}
				})
				.setOnCancelListener(new DialogInterface.OnCancelListener(){
					public void onCancel(DialogInterface dialog) {
						try{
							mListener.onAddressGeocodeCanceled();
						}catch(HandledException h){ // Ignore.
						}catch(Exception exp){
							Log.e(TAG, "ERR0004Y", exp);
							ErrorUtil.handleExceptionFinish("ERR0004Y", exp, mActivity);
						}
					}
				})
				.create();
				return dialog;
			default:
				return null;
		}
	}

	private void prepareDialogTitle() {
		dialogTitleText.setText(TextUtils.expandTemplate(mActivity.getText(R.string.dialog_theAddressXWasNotUnderstood), mAddress));
	}

	/**
	 * TODO: ! We need better code for formatting this address.
	 */
	private void updateContactMethodAddress() {
		assert null != address;
		
		ContentValues cv = new ContentValues(1);
		StringBuilder sb = new StringBuilder();
		for(int i=0; i < address.getMaxAddressLineIndex(); i++){
			if( i != 0){
				sb.append('\n');
			}
			sb.append(address.getAddressLine(i));
		}

		cv.put(Contacts.ContactMethodsColumns.DATA, sb.toString());
		if( 1 != mActivity.getContentResolver().update(mPostalContactMethodUri, cv, null, null) ){
        	Log.e(TAG, "ERR0004Z Failed to update contact method address. contact method not found.");
        	ErrorUtil.handle("ERR0004Z", "Failed to update contact method address. contact method not found.", this);
        	return;
        }
	}

	private static GeoPoint makeGeoPoint(Address address) {
		return new GeoPoint((int)((address.getLatitude())*1E6), (int)((address.getLongitude())*1E6));
	}
}
