// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.ItemizedOverlay.OnFocusChangeListener;
import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 
 * 
 */
public class SelectGlobalPositionPart extends AbstractContextActivityParticipant implements OnFocusChangeListener, Wizard.WizardStep{
	private static final String TAG = "SelectGlobalPositionPart";

	public static final String EXTRA_OVERLAY_ITEMS = "com.flingtap.done.intent.extra.OVERLAY_ITEMS";
	
	// *********************************************************
	// SelectGlobalPositionPart
	// *********************************************************
	private Wizard mWizard = null;
	private LinearLayout selectAreaDialogView = null;
	private LinearLayout selectAreaItemView = null;
	private TextView selectAreaItemTitleView = null;
	private TextView selectAreaItemSnippetView = null;

	private SelectAreaOverlayItem[] mOverlayItems = null;
	private SelectAreaOverlays mOverlays = null;
	private MapController mMapController = null;
	private MapView mMapView = null;
	private ImageView bullsEyeView = null;
	protected Intent mReturnIntent = null;
	protected Activity mActivity = null;

	
	public SelectGlobalPositionPart(final Activity activity, final MapView mapView, final Intent returnIntent) {
		assert null != activity;
		mActivity = activity;
		
		setIntent(activity.getIntent());

		assert null != returnIntent;
		mReturnIntent = returnIntent;
		
		assert null != mapView;
		mMapView = mapView;

		
    	// **********************************************
    	// Get references to the selected item views
    	// **********************************************
    	selectAreaDialogView = (LinearLayout)activity.findViewById(R.id.select_area_select_location_dialog);
    	selectAreaDialogView.setOnLongClickListener(new View.OnLongClickListener(){ // Prevent touches on panel from dragging map view.
			public boolean onLongClick(View v) {
				return true;
			}
             });
    	selectAreaItemView = (LinearLayout)activity.findViewById(R.id.select_area_item);
    	selectAreaItemTitleView = (TextView)activity.findViewById(R.id.select_area_item_title);
    	selectAreaItemSnippetView = (TextView)activity.findViewById(R.id.select_area_item_snippet);

    	// ******************************
    	// Add marker for center point
    	// ******************************
    	bullsEyeView = (ImageView)activity.findViewById(R.id.select_area_bulls_eye); // Consider using Reticle from MapView class.

    	// This seems like a lot of work just to cast a Parcelable[] to a SelectAreaOverlayItem[]
    	//   TODO: !!! Why can't I just do this? mOverlayItems = mIntent.getParcelableArrayExtra(SelectAreaActivity.EXTRA_OVERLAY_ITEMS);
    	Parcelable[] tmpParcelArray = mIntent.getParcelableArrayExtra(SelectAreaActivity.EXTRA_OVERLAY_ITEMS);
    	if( null != tmpParcelArray ){
    		mOverlayItems = new SelectAreaOverlayItem[tmpParcelArray.length];
    		for(int ctr = 0; ctr < tmpParcelArray.length ; ctr++){
    			mOverlayItems[ctr] = (SelectAreaOverlayItem)tmpParcelArray[ctr];
    		}
    		
    		Drawable marker = activity.getResources().getDrawable(R.drawable.pins_red);
    		mOverlays = new SelectAreaOverlays(marker, activity, mOverlayItems, mapView.getController());
    		if( mOverlayItems.length != 0 ){
    			mOverlays.setFocus(mOverlayItems[0]);
    		}
    		mapView.getOverlays().add(mOverlays);
    		mOverlays.setOnFocusChangeListener(this);	
    	}

    	mMapController = mapView.getController();
    	
    	// **********************************************
    	// Handle the "next" button
    	// **********************************************
    	
		// Listen for "OK" button clicks.
    	// TODO: !!! If user doesn't have touch screen, then they should be able to use the center D-Pad key (and this button should be displayed so it doesn't take focus).
    	final Button nextButton = (Button) activity.findViewById(R.id.select_area_select_location_button_next);
		nextButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				handlePositionSelectedAction(activity);
			}

		});		
	}

	private void handlePositionSelectedAction(final Activity activity) {
		try{
			// Hide SelectGlobalPositionPart views
			selectAreaItemView.setVisibility(View.GONE);
			bullsEyeView.setVisibility(View.GONE);
			
			// Hide 
			AlphaAnimation hideAnim = new AlphaAnimation(1.0F, 0.0F);
			hideAnim.setDuration(500L);
			selectAreaDialogView.startAnimation(hideAnim);
			selectAreaDialogView.setVisibility(View.GONE);
			
			mWizard.moveNext();
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0005V", exp);
			ErrorUtil.handleExceptionFinish("ERR0005V", exp, activity);
		}
	}
	
	protected final static int FIRST_CODE_ID = IGNORE_CODE_ID;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}	
	
	public void onFocusChanged(ItemizedOverlay itemizedoverlay, OverlayItem overlayItem) {
		try{
			//Log.v(TAG, "onFocusChanged(..) called.");
			//Log.d(TAG, "focused OverlayItem overlayitem == " + overlayItem );
			
			// Make the selected view visible. 
			if( null == overlayItem ){
				selectAreaItemView.setVisibility(View.GONE);
			}else{
				// Set the selected view's properties.
				selectAreaItemTitleView.setText(overlayItem.getTitle());
				selectAreaItemSnippetView.setText(overlayItem.getSnippet());
				selectAreaItemView.setVisibility(View.VISIBLE);
				// TODO: Add icon image when available.
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0005W", exp);
			ErrorUtil.handleExceptionFinish("ERR0005W", exp, mActivity);
		}
	}

	public void onArrive() {
		//Log.v(TAG, "onArrive() called.");
		// TODO: Center user on the given point.
		ParcelableGeoPoint geoPoint = (ParcelableGeoPoint)mIntent.getParcelableExtra(SelectAreaActivity.EXTRA_GLOBAL_POSITION);
		if( null != geoPoint ){
			mMapController.setCenter(geoPoint);
		}else{
			// TODO: Maybe animate to their actual GPS position? 
			BasicMapActivity.centerOnCurrentLocation(mActivity, mMapController);		
		}
    	bullsEyeView.setVisibility(View.VISIBLE);
    	selectAreaDialogView.setVisibility(View.VISIBLE);
	}

	public void onReturn() {
		//Log.v(TAG, "onReturn() called.");
		// TODO: Re-center on the current GeoPoint? or just leave user where they are?
		ParcelableGeoPoint geoPoint = (ParcelableGeoPoint)mIntent.getParcelableExtra(SelectAreaActivity.EXTRA_GLOBAL_POSITION);
		if( null != geoPoint ){
			mMapController.animateTo(geoPoint);
		}
    	bullsEyeView.setVisibility(View.VISIBLE);
    	
		// Show 
        AlphaAnimation showAnim = new AlphaAnimation(0.0F, 1.0F);
        showAnim.setStartOffset(500L);
        showAnim.setDuration(500L);
        selectAreaDialogView.startAnimation(showAnim);
    	selectAreaDialogView.setVisibility(View.VISIBLE);
    	
	}
	public void setWizard(Wizard wizard) {
		assert null != wizard;
		mWizard = wizard;
	}
	public void onDepart() {
		GeoPoint point = mMapView.getMapCenter();
		mReturnIntent.putExtra(SelectAreaActivity.EXTRA_GLOBAL_POSITION, new ParcelableGeoPoint(point));
		mReturnIntent.putExtra(SelectAreaActivity.EXTRA_ZOOM,mMapView.getZoomLevel());
	}
	public void onBack() {
		//Log.v(TAG, "onBack() called.");
	}

}
