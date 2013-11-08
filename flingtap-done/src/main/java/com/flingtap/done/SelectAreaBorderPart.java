// Licensed under the Apache License, Version 2.0

package com.flingtap.done;


import com.flingtap.common.HandledException;
import com.flingtap.done.ProximityOverlay.OnProximityExceedingScreenListener;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.ItemizedOverlay.OnFocusChangeListener;
import com.flingtap.done.base.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 */
public class SelectAreaBorderPart extends AbstractContextActivityParticipant implements OnProximityExceedingScreenListener, Wizard.WizardStep{
	private static final String TAG = "SelectAreaBorderPart";

	public static float calculateDisplayRadius(int base185Radius){
		return (float)Math.pow((base185Radius + 15), 2);
	}
	
	protected final static int FIRST_CODE_ID = IGNORE_CODE_ID;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}	

	// *********************************************************
	// SelectAreaBordersPart
	// *********************************************************
	private View paeView = null;
	private Wizard mWizard = null;
	private SeekBar mSeekBar;
	private Button okButton;
	private Button cancelButton;
	private Button backButton;
	private GeoPoint mGeoPoint;
	private ProximityOverlay mProximityOverlay;
	private PushPinOverlay mPushPinOveraly;
	private int currentSliderProximityRadiusValue; // TODO: Maybe change this to float since the LocationManger uses floats?
	protected MyLocationOverlay myLocationOverlay = null;
	protected MapController mMapController = null;
	protected MapView mMapView = null;
	protected Activity mActivity = null;
	protected Intent mReturnIntent = null;
	private float mScreenDensity = 0;

	public SelectAreaBorderPart(final Activity activity, final MapView mapView, final Intent returnIntent) {
		
		mScreenDensity = activity.getResources().getDisplayMetrics().density;	

		assert null != mapView;
		mMapView = mapView;

		assert null != activity;
		mActivity = activity;
		
		setIntent(mActivity.getIntent());
		
		assert null != returnIntent;
		mReturnIntent = returnIntent;
		
		mMapController = mapView.getController();
		
		mSeekBar = (SeekBar) activity.findViewById(R.id.select_area_distance_value);
		mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
				try{
					currentSliderProximityRadiusValue = progress;
					if( null != mProximityOverlay ){ // While restoring instance state, this method gets called, but the overlay may not have been created because the first time through there is no Point object.
						mProximityOverlay.setMeterRadius((int)SelectAreaBorderPart.calculateDisplayRadius(progress));
					}
					//Log.v(TAG, String.valueOf(progress));
				//}catch(HandledException h){ // Ignore.
				}catch(Exception exp){
					Log.e(TAG, "ERR0005Q", exp);
					ErrorUtil.handleExceptionFinish("ERR0005Q", exp, mActivity);
				}
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		okButton = (Button) activity.findViewById(R.id.select_area_button_save);
		okButton.setOnClickListener(new android.view.View.OnClickListener() {
			public void onClick(View view) {
				try{
					mWizard.moveNext();
				}catch(HandledException h){ // Ignore.
				}catch(Exception exp){
					Log.e(TAG, "ERR0005R", exp);
					ErrorUtil.handleExceptionFinish("ERR0005R", exp, mActivity);
				}
			}
		});

		cancelButton = (Button) activity.findViewById(R.id.select_area_button_cancel);
		cancelButton.setOnClickListener(new android.view.View.OnClickListener() {
			public void onClick(View view) {
				try{
					mWizard.cancel();
				}catch(HandledException h){ // Ignore.
				}catch(Exception exp){
					Log.e(TAG, "ERR0005S", exp);
					ErrorUtil.handleExceptionFinish("ERR0005S", exp, mActivity);
				}
				
			}
		});
		
		backButton = (Button) activity.findViewById(R.id.select_area_button_back);
		backButton.setOnClickListener(new android.view.View.OnClickListener() {
			public void onClick(View view) {
				try{
					mWizard.movePrev();
				}catch(HandledException h){ // Ignore.
				}catch(Exception exp){
					Log.e(TAG, "ERR0005T", exp);
					ErrorUtil.handleExceptionFinish("ERR0005T", exp, mActivity);
				}
			}
		});

		paeView = mActivity.findViewById(R.id.select_area_editor_dialog);
        paeView.setOnLongClickListener(new View.OnLongClickListener() { // Prevent touches on panel from dragging map view.
            public boolean onLongClick(View v) {
                return true;
            }
        });
	}

	public void proximityExceededScreen() {
	}

	public void onArrive() {
		//Log.v(TAG, "onArrive() called.");

		// TODO: !!! Adjust the border (and slider) to fit on screen.
		
		updatePoint();
		
		mMapView.getController().setCenter(mGeoPoint);

		// ***********************
		// Setup slider 
		// ***********************
		updateCurrentSlideProximityRadiusValue();
		
		if( null == mProximityOverlay ){
			mProximityOverlay = new ProximityOverlay(mGeoPoint, (int)SelectAreaBorderPart.calculateDisplayRadius(currentSliderProximityRadiusValue), this, mMapView, mScreenDensity);
			mProximityOverlay.setController(mMapController);
		}	else{
			assert null != mProximityOverlay;
			mProximityOverlay.setPoint(mGeoPoint);
			mProximityOverlay.setMeterRadius((int)SelectAreaBorderPart.calculateDisplayRadius(currentSliderProximityRadiusValue));
		}
		mMapView.getOverlays().add(mProximityOverlay);

		// ***********************
		// Setup push pin 
		// ***********************
		if( null == mPushPinOveraly ){
			mPushPinOveraly = new PushPinOverlay(mActivity, R.drawable.push_pin_in, -10, -30, mScreenDensity);
		}
		assert null != mPushPinOveraly;
		mPushPinOveraly.setGeopoint(mGeoPoint);
		mMapView.getOverlays().add(mPushPinOveraly);
		
		mSeekBar.setProgress(currentSliderProximityRadiusValue);

		// Show
        AlphaAnimation showAnim = new AlphaAnimation(0.0F, 1.0F);
        showAnim.setStartOffset(500L);
        showAnim.setDuration(500L);
        paeView.startAnimation(showAnim);
        paeView.setVisibility(View.VISIBLE);
        showAnim.setAnimationListener(new Animation.AnimationListener(){
			public void onAnimationEnd(Animation animation) {
				Point thePixelPoint = mMapView.getProjection().toPixels(mGeoPoint, null);
				thePixelPoint.y = thePixelPoint.y - (paeView.getHeight()/2);
				mMapView.getController().animateTo(mMapView.getProjection().fromPixels(thePixelPoint.x, thePixelPoint.y));         
			}

			public void onAnimationRepeat(Animation animation) {
			}

			public void onAnimationStart(Animation animation) {
			}
        });

		if( !mWizard.hasPreviousStep() ){
			backButton.setVisibility(View.GONE);
		}
	}

	private void updatePoint() {
		// Constructor for SelectAreaBordersPart should include a GeoPoint for this.
		mGeoPoint = (ParcelableGeoPoint)mReturnIntent.getParcelableExtra(SelectAreaActivity.EXTRA_GLOBAL_POSITION);
		assert null != mGeoPoint;
	}

	private void updateCurrentSlideProximityRadiusValue() {
		int radius = mReturnIntent.getIntExtra(SelectAreaActivity.EXTRA_RADIUS, -1);
		if( -1 == radius ){
			currentSliderProximityRadiusValue = mIntent.getIntExtra(SelectAreaActivity.EXTRA_RADIUS, 100); // TODO: !!! Calculate a radius which will fill the screen.
		}else{
			currentSliderProximityRadiusValue = radius; 
		}
	}
	public void onReturn() {
		//Log.v(TAG, "onReturn() called.");
	}

	public void onBack() {
		//Log.v(TAG, "onBack() called.");
		
		mMapView.getOverlays().remove(mPushPinOveraly);
		
		Point thePixelPoint = mMapView.getProjection().toPixels(mGeoPoint, null);
		mMapView.getController().animateTo(mMapView.getProjection().fromPixels(thePixelPoint.x, thePixelPoint.y));
		
		// Hide 
        AlphaAnimation hideAnim = new AlphaAnimation(1.0F, 0.0F);
        hideAnim.setDuration(500L);
        paeView.startAnimation(hideAnim);
        paeView.setVisibility(View.GONE);
		mMapView.getOverlays().remove(mProximityOverlay);

		mMapView.invalidate();

		mReturnIntent.putExtra(SelectAreaActivity.EXTRA_RADIUS, currentSliderProximityRadiusValue); 
		mReturnIntent.putExtra(SelectAreaActivity.EXTRA_RADIUS_UNIT, 1); 
	}
	
	public void setWizard(Wizard wizard) {
		assert null != wizard;
		mWizard = wizard;
	}

	public void onDepart() {
		mReturnIntent.putExtra(SelectAreaActivity.EXTRA_RADIUS, currentSliderProximityRadiusValue); 
		// TODO: ! Add a setting for the user's prefered unit.
		mReturnIntent.putExtra(SelectAreaActivity.EXTRA_RADIUS_UNIT, 1); // TODO: Add support for different units. Also need to create db field for storing the unit,, I think?
		mReturnIntent.putExtra(SelectAreaActivity.EXTRA_ZOOM,mMapView.getZoomLevel());
	}

	protected void onLayerChanged(){
		if( null != mProximityOverlay ){
			mProximityOverlay.updateColor();
		}
	}
}
