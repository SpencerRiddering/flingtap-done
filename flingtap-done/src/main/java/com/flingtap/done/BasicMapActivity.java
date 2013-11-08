// Licensed under the Apache License, Version 2.0

package com.flingtap.done;


import java.lang.reflect.Method;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.flingtap.done.base.R;

/**
 * 
 * 
 * 
 * TODO: !!! Add a "My location" menu option.
 * TODO: !!! Standardize initializing the position of the map. Use the user's last known position as a default. 
 * 
 */
public abstract class BasicMapActivity extends CoordinatedMapActivity {
	private static final String TAG = "BasicMapActivity";
	
	public BasicMapActivity() {
		super();
	}
	public static final int ZOOM_ID 		= 1;
	public static final int INFO_ID 		= 2;
	public static final int IS_SATELLITE_ID = 3;
	public static final int IS_TRAFFIC_ID 	= 4;
	public static final int LAYERS_ID 		= 80;
	public static final int HELP_ID 		= 81;
	public static final int MY_LOCATION_ID 	= 82;
	public static final int EDIT_ID 		= 83;
	
	
	protected static final int LAYER_SATELLITE_INDEX = 1;
	protected static final int LAYER_TRAFFIC_INDEX = 2;
	
	
	protected boolean satelliteChecked;
	protected boolean trafficChecked;
	protected MapView mapView = null;
	protected MapController mapController = null;
	protected LinearLayout zoomView = null;
	protected MyLocationOverlay myLocationOverlay = null;
	protected boolean mIsMyLocationEnabled = false;
	protected Method enableCompassMethod = null;
	protected Method disableCompassMethod = null;
	
	
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		//Log.e(TAG, "onCreate(..) called.");
		try{
			initMap();
			myLocationOverlay = new MyLocationOverlay(this, mapView);
			if( Integer.parseInt(Build.VERSION.SDK) >= 4){
				Class<MyLocationOverlay> myLocationOverlayClass = MyLocationOverlay.class;
				try{
					enableCompassMethod = myLocationOverlayClass.getMethod("enableCompass");
					disableCompassMethod = myLocationOverlayClass.getMethod("disableCompass");
				}catch(NoSuchMethodException nsme){
					Log.e(TAG, "ERR000IX", nsme);
					ErrorUtil.handleException("ERR000IX", nsme, this);
				}
			}
			mapView.getOverlays().add(myLocationOverlay);
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000IW", exp);
			ErrorUtil.handleExceptionFinish("ERR000IW", exp, this);
		}
	}

	public static void centerOnCurrentLocation(Context context, MapController mapController) {
		PendingIntent pIntent = PendingIntent.getBroadcast(context, -1, new Intent() , PendingIntent.FLAG_ONE_SHOT); 
		assert pIntent != null;
		
		LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE); 
		Criteria criteria = new Criteria();
		String provider = locationManager.getBestProvider(criteria, true);
		if( null != provider ){
			LocationProvider locationProvider = locationManager.getProvider(provider);
			if( null != locationProvider ){
				locationManager.requestLocationUpdates(locationProvider.getName(), 0, 0, pIntent); // I think this is needed to start the location hardware (I read that somewhere and I was not able to get a position without it).
				Location location = locationManager.getLastKnownLocation(locationProvider.getName());
				locationManager.removeUpdates(pIntent);
				if( null != location ){
					mapController.setCenter(new GeoPoint((int)((location.getLatitude())*1E6), (int)((location.getLongitude())*1E6)));
				}
			}
		}
	}
	
	protected abstract void initMap(); // TODO: !!! Why is this method used? It is always the first method called in onCreate from the subclass. Why not have this class call it.

	@Override
	protected void onPause() {
		super.onPause();
		try{
			myLocationOverlay.disableMyLocation();
			if( null != disableCompassMethod ){
				// myLocationOverlay.enableCompass();
				disableCompassMethod.invoke(myLocationOverlay);
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000CZ", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000CZ", exp, this);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
//		updateZopaProxy();
		try{
//			if( null != myLocationOverlay ){
//				myLocationOverlay.enableMyLocation();
//			}
			if( !mapView.getOverlays().contains(myLocationOverlay) ){
				mapView.getOverlays().add(myLocationOverlay);
			}
			if( mIsMyLocationEnabled ){
				myLocationOverlay.enableMyLocation();
			}
			if( null != enableCompassMethod ){
				// myLocationOverlay.enableCompass();
				enableCompassMethod.invoke(myLocationOverlay);
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000CY", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000CY", exp, this);
		}
	}

	/**
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		try{
			MenuItem layersMenuItem = menu.add(0, LAYERS_ID, 30, R.string.option_mapMode); 
			layersMenuItem.setIcon(android.R.drawable.ic_menu_mapmode);

			MenuItem zoomMenuItem = menu.add(0, ZOOM_ID, 60, R.string.option_zoom);
			zoomMenuItem.setIcon(android.R.drawable.ic_menu_zoom);

			MenuItem myLocationMenuItem = menu.add(0, MY_LOCATION_ID, 40, R.string.option_myLocation);
			myLocationMenuItem.setIcon(android.R.drawable.ic_menu_mylocation);

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000D0", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000D0", exp, this);
		}
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		boolean superResult = super.onOptionsItemSelected(item);
		try{
			if( !superResult ){
				// Handle all of the possible menu actions.
				switch (item.getItemId()) {
					case MY_LOCATION_ID:
						mIsMyLocationEnabled = true;
						myLocationOverlay.enableMyLocation();
						myLocationOverlay.runOnFirstFix(new Runnable() {
							public void run() {
								mapController.animateTo(myLocationOverlay.getMyLocation());
							}
						});
						
						return true;
					case ZOOM_ID:
						mapView.displayZoomControls(true);
						return true;
					case LAYERS_ID:
						showDialog(DIALOG_MAP_MODE_ID);
						return true;
				}
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000D1", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000D1", exp, this);
		}
		return superResult;
	}
	
	public static final int DIALOG_MAP_MODE_ID = 50;
	
	@Override
	protected Dialog onCreateDialog(int dialogId) {
		Dialog dialog = super.onCreateDialog(dialogId);
		try{
			switch(dialogId){
				case DIALOG_MAP_MODE_ID:
					int selectedIndex = mapView.isSatellite()?1:(mapView.isTraffic()?2:0);
					clickListener = createClickListener(selectedIndex);
					
			   		dialog =  new AlertDialog.Builder(this)
		            .setIcon(R.drawable.ic_dialog_menu_generic)
		            .setTitle(R.string.dialog_mapMode)
		            .setSingleChoiceItems(R.array.basic_map_map_modes, selectedIndex, clickListener)
		            .setPositiveButton(R.string.button_ok, clickListener)
		            .setNegativeButton(R.string.button_cancel,clickListener)
		           .create();	
					break;
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000CT", exp);
			ErrorUtil.handleException("ERR000CT", exp, this);
		}		
		return dialog;
	}

	// NOTE: This is called twice during a config change. First because of the restore, and then again as the dialog is created.
	// TODO: !! Is there a cleaner way to do this?
	private SelectLayerOnClickListener createClickListener(int selectedIndex) {
		if( null == clickListener ){ // This is very important because it prevents the intialization by the createDialog(..) from overriding the restored state.
			clickListener = new SelectLayerOnClickListener(this, selectedIndex);
		}
		return clickListener;
	}

	// mapView.isSatellite()?1:(mapView.isTraffic()?2:0)
	private SelectLayerOnClickListener clickListener = null;	
	private class SelectLayerOnClickListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener{
		private int mOrigSelectedIndex = -1;
		private int mNewSelectedIndex = -1;
		private Context mContext = null;
		
		
		public SelectLayerOnClickListener(Context context, int selectedIndex){
			assert 0 <= selectedIndex;
			mOrigSelectedIndex = selectedIndex;
			mNewSelectedIndex = mOrigSelectedIndex;
			
			assert null != context;
			mContext = context;
		}
		
		public void onClick(DialogInterface dialog, int which) {
			try{
				
				if( DialogInterface.BUTTON_NEGATIVE == which ){
					handleCancel(dialog);
				}else if( DialogInterface.BUTTON_POSITIVE == which ){
					if( mNewSelectedIndex == mOrigSelectedIndex ){
						return;
					}
					onLayerChanged();			

					// No event needed.
					
					mOrigSelectedIndex = mNewSelectedIndex;
					
				}else if( 0 <= which ){
					mNewSelectedIndex = which;
				}
			}catch(HandledException h){ // Ignore.
			}catch(Exception exp){
				Log.e(TAG, "ERR000CU", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR000CU", exp, mContext);
			}

		}

		private void handleCancel(DialogInterface dialog) {
			mNewSelectedIndex = mOrigSelectedIndex;
			ListView listView = ((AlertDialog)dialog).getListView();
			
			// listView.setSelection(mNewSelectedIndex); // NOTE: setSelection(..) does not "select" a radio group item. It's a different type of "selection". 
			if( !listView.isItemChecked(mOrigSelectedIndex) ){
				listView.setItemChecked(mOrigSelectedIndex, true);
			}
		}

		public int getOrigSelectedIndex() {
			return mOrigSelectedIndex;
		}
		public void setOrigSelectedIndex(int origSelectedIndex) {
			mOrigSelectedIndex = origSelectedIndex;
		}
		
		public int getNewSelectedIndex() {
			return mNewSelectedIndex;
		}
		public void setNewSelectedIndex(int newSelectedIndex) {
			mNewSelectedIndex = newSelectedIndex;
		}
		public void onCancel(DialogInterface dialog) {
			handleCancel(dialog);
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private static final String STATE_ZOOM_LEVEL 			 = "BasicMapActivity.STATE_ZOOM_LEVEL";
	private static final String SAVE_MY_LOCATION 			 = "BasicMapActivity.SAVE_MY_LOCATION";
	private static final String SAVE_NEW_SELECTED_LAYER 	 = "BasicMapActivity.SAVE_NEW_SELECTED_LAYER";
	private static final String SAVE_ORIGINAL_SELECTED_LAYER = "BasicMapActivity.SAVE_ORIGINAL_SELECTED_LAYER";

	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		try{
			//Log.v(TAG, "onSaveInstanceState(..) called.");	
			outState.putInt(STATE_ZOOM_LEVEL, mapView.getZoomLevel());
			outState.putBoolean(SAVE_MY_LOCATION, ((null != myLocationOverlay) && myLocationOverlay.isMyLocationEnabled()));
			
			
			if( null == myLocationOverlay ){
				myLocationOverlay = new MyLocationOverlay(this, mapView);
				mapView.getOverlays().add(myLocationOverlay);
			}
			myLocationOverlay.enableMyLocation();
			
			if( null != clickListener ){
				outState.putInt(SAVE_NEW_SELECTED_LAYER,      clickListener.getNewSelectedIndex());
				outState.putInt(SAVE_ORIGINAL_SELECTED_LAYER, clickListener.getOrigSelectedIndex());
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000CW", exp);
			ErrorUtil.handleExceptionFinish("ERR000CW", exp, this);
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		try{
			mapController.setZoom( savedInstanceState.getInt(STATE_ZOOM_LEVEL) );
			if(	savedInstanceState.getBoolean(SAVE_MY_LOCATION, false) ){
				myLocationOverlay = new MyLocationOverlay(this, mapView);
				mapView.getOverlays().add(myLocationOverlay);
				myLocationOverlay.enableMyLocation();
			}
			
			if( savedInstanceState.containsKey(SAVE_NEW_SELECTED_LAYER ) ){
				int newSelectedIndex = savedInstanceState.getInt(SAVE_NEW_SELECTED_LAYER, 0);
				int selectedIndex = savedInstanceState.getInt(SAVE_ORIGINAL_SELECTED_LAYER, 0);
				assert null == clickListener;
				clickListener = createClickListener(selectedIndex);
				clickListener.setNewSelectedIndex(newSelectedIndex);
				onLayerChanged();			
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000CX", exp);
			ErrorUtil.handleExceptionFinish("ERR000CX", exp, this);
		}
	}

	protected void onLayerChanged() {
		mapView.setSatellite(clickListener.getNewSelectedIndex()==LAYER_SATELLITE_INDEX); 
		mapView.setTraffic(clickListener.getNewSelectedIndex()==LAYER_TRAFFIC_INDEX);
	}
	
}
