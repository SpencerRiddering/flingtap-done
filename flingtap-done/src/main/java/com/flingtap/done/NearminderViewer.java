// Licensed under the Apache License, Version 2.0

package com.flingtap.done;


import com.flingtap.common.HandledException;
import com.flingtap.done.ProximityOverlay.OnProximityExceedingScreenListener;
import com.flingtap.done.provider.Task;
import com.flurry.android.FlurryAgent;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.flingtap.done.base.R;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 *  
 * Original source code comes from ProximityAlertViewer.
 * 
 * 
 */
public class NearminderViewer extends BasicMapActivity implements OnProximityExceedingScreenListener {

	static final String TAG = "NearminderViewer";
	
//	public static final String EXTRA_TASK_ID 		= "com.flingtap.done.ProximityAlertViewer.EXTRA_TASK_ID";

	public static final String ACTION_PROXIMITY_ALERT_NOTIFY = "com.flingtap.done.ACTION_PROXIMITY_ALERT_NOTIFY"; 

	protected final static int FIRST_CODE_ID 	  = 2300;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}	
	
	protected final static int DIRECTIONS_MENU_ITEM_ID    = FIRST_CODE_ID + 99;

	
	/**
	 * Standard projection for the interesting columns of a proximity alert.
	 */
	private static final String[] PROXIMITY_PROJECTION = new String[] {
			Task.ProximityAlerts._ID,          // 0
			Task.ProximityAlerts.RADIUS,       // 1
			Task.ProximityAlerts._GEO_URI,     // 2
			Task.ProximityAlerts._IS_SATELLITE,// 3
			Task.ProximityAlerts._IS_TRAFFIC,  // 4
			Task.ProximityAlerts._ZOOM_LEVEL,  // 5
	};	
	private int ID_INDEX 						= 0;
	private int RADIUS_INDEX 					= 1;
	private int GEO_URI_INDEX 					= 2;
	private int IS_SATELLITE_INDEX 				= 3;
	private int IS_TRAFFIC_INDEX 				= 4;
	private int ZOOM_LEVEL_INDEX 				= 5;
	private Cursor mProximityCursor;
	
	private String locationName = null;
	

	private GeoPoint mGeoPoint;
	protected Uri mUri = null; // TODO: !! The name "mUri" is too generic. Use a more specific name like mProximityAlertUri.
	private float mScreenDensity = 0;

	
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		try{
			mScreenDensity = getResources().getDisplayMetrics().density;
// Moved into BasicMapActvity.			
//			initMap();
			//Log.v(TAG, "onCreate(..) called.");
			
	        // Allow user to use menu shortcuts without opening menu.
			final Intent intent = getIntent();
			String action = intent.getAction(); // TODO: When an Activity receives an MAIN_ACTION,, shouldn't intent.getAction() return MAIN_ACTION ?
			
	        // We don't create it,, we just view it.
			mUri = intent.getData();
			if( mUri == null){
				Log.e(TAG, "ERR00048 Call to Activity did not contain data uri");
				ErrorUtil.handleExceptionFinish("ERR00048", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
				return;
			}
			assert mUri.toString().startsWith(Task.ProximityAlerts.CONTENT_URI.toString());

	        if( ACTION_PROXIMITY_ALERT_NOTIFY.equals(action)){
	        	NearminderViewerNotifyPart nearViewNotifyPart = new NearminderViewerNotifyPart(this, mUri.getLastPathSegment(), mUri);
	        	addParticipant(nearViewNotifyPart);
	        }else if( !Intent.ACTION_VIEW.equals(action) ){
	        	// Whoops, unknown action! Bail.
				Log.e(TAG, "ERR00049 Unknown action. " + action);
				ErrorUtil.handleExceptionFinish("ERR00049", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
				return;
			}
			
		    doPrepareView();
			
//			EulaPromptPart.checkAndPrompt(this); // Not needed because will only be triggered after app update and so user has already accepted an earlier EULA, and wil have to accept the new version of EULA if they want to keep using the app.
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			// TODO: !!! If this activity is called from a notification then it causes the Activity to finish without user ever seeing a message!
			Log.e(TAG, "ERR00047", exp);
			ErrorUtil.handleExceptionFinish("ERR00047", exp, this);
		}
	}

	private void doPrepareView() {
		// Get Proximity Alert Info
		mProximityCursor = getContentResolver().query(mUri, PROXIMITY_PROJECTION, null, null, null);
		if( !mProximityCursor.moveToFirst() ){
			Log.e(TAG, "ERR0004A mUri is invalid: " + mUri);
			ErrorUtil.handleExceptionFinish("ERR0004A", (Exception)(new Exception( mUri.toString() )).fillInStackTrace(), this);
			return;
		}

		// ****************************
		// Setup view data.
		// ****************************
		
		String geoUri = mProximityCursor.getString(GEO_URI_INDEX);
		GeoPoint geoPoint = Util.createPoint(geoUri);
		int latitude = geoPoint.getLatitudeE6();
		int longitude = geoPoint.getLongitudeE6();
		mGeoPoint = new GeoPoint(latitude,longitude);
		mapController.setCenter(mGeoPoint);
		
		boolean isSatellite = (0==mProximityCursor.getInt(IS_SATELLITE_INDEX)?false:true);
		mapView.setSatellite(isSatellite);
		
		boolean isTraffic = (0==mProximityCursor.getInt(IS_TRAFFIC_INDEX)?false:true);
		mapView.setTraffic(isTraffic);
		
		int zoomLevel = mProximityCursor.getInt(ZOOM_LEVEL_INDEX);
		mapController.setZoom(zoomLevel);

		radius = mProximityCursor.getInt(RADIUS_INDEX);
		
		mProximityCursor.close();
	}
	private int radius = 0;

	
	
	@Override
	protected void onResume() {
		super.onResume();
		try{
			if( !mProximityCursor.moveToFirst()){
				// TODO: !!! If this activity is called from a notification then it causes the Activity to finish without user ever seeing a message!
				Log.e(TAG, "ERR0004B mProximityCursor.moveToFirst() failed.");
				ErrorUtil.handleExceptionFinish("ERR0004B", (Exception)(new Exception(  )).fillInStackTrace(), this);
				return;
			}
			
			if( null == overlay ){
				overlay = new ProximityOverlay(mGeoPoint, (int)SelectAreaBorderPart.calculateDisplayRadius(radius), this, mapView, mScreenDensity);
			}	else{
				assert null != overlay;
				overlay.setPoint(mGeoPoint);
				overlay.setMeterRadius((int)SelectAreaBorderPart.calculateDisplayRadius(radius));
			}
			mapView.getOverlays().add(overlay);
			
			
//			LocationDescriptionOverlay overlay = new LocationDescriptionOverlay(this, mapView, mGeoPoint, locationName);
			
			// ***********************
			// Setup push pin 
			// ***********************
			if( null == mPushPinOveraly ){
				mPushPinOveraly = new PushPinOverlay(this, R.drawable.push_pin_in, -10, -30, mScreenDensity);
			}
			assert null != mPushPinOveraly;
			mPushPinOveraly.setGeopoint(mGeoPoint);
			mapView.getOverlays().add(mPushPinOveraly);
			
			mapView.postInvalidate(); // Make the overlay visible.
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0004C", exp);
			ErrorUtil.handleExceptionFinish("ERR0004C", exp, this);
		}
	}

	private PushPinOverlay mPushPinOveraly;
	private ProximityOverlay overlay = null;
//    final Handler mHandler = new Handler();
    
	public void proximityExceededScreen() {// TODO: Remove this code.
//		Log.v(TAG, "proximityExceededScreen(..) called.");
//		mapController.zoomOut();
//		overlay.setMeterRadius(radius);
//		mapView.invalidate(); // TODO: Restrict the invalidate to a smaller region.		
	}

	protected void onPause() {
		super.onPause();
		try{
			//Log.v(TAG, "onPause() called.");	
			mapView.getOverlays().clear(); // TODO: !!! This is removing overlays in the parent class,,, is it _really_ necessary? 
//			mapView.postInvalidate(); 
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0004D", exp);
			ErrorUtil.handleExceptionFinish("ERR0004D", exp, this);
		}
	}
	
	
	protected void initMap() {
//		try{
			setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
			
			setContentView(R.layout.proximity_alert_view);
			mapView = (MapView)findViewById(R.id.map_view);		
			mapController = mapView.getController();

			// Add zoom controls.
//			LinearLayout zoomLayout = (LinearLayout) findViewById(R.id.map_zoom);
//			zoomLayout.addView(mapView.getZoomControls(),
//					new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT,
//							LayoutParams.WRAP_CONTENT));		
//			zoomLayout.addView(,
//					new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT,
//							LayoutParams.WRAP_CONTENT));		
			mapView.setBuiltInZoomControls(true);
//		}catch(HandledException h){ // Ignore.
//		}catch(Exception exp){
//			Log.e(TAG, "ERR0004E", exp);
//			ErrorUtil.handleExceptionFinish("ERR0004E", exp, this);
//		}
	}

	protected void onLayerChanged(){
		super.onLayerChanged();
		if( null != overlay ){
			overlay.updateColor();
		}
	}

	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		try{
//	        MenuItem helpMenuItem = menu.add(0, HELP_ID, Menu.NONE, R.string.option_help);
////	        helpMenuItem.setAlphabeticShortcut('h');
//	        helpMenuItem.setIcon(android.R.drawable.ic_menu_help); 

	        MenuItem editMenuItem = menu.add(0, EDIT_ID, 50, R.string.option_edit);
//	        editMenuItem.setAlphabeticShortcut('e');
//	        editMenuItem.setIcon(R.drawable.editproximityalert); 
	        editMenuItem.setIcon(android.R.drawable.ic_menu_edit); 

	        MenuItem directionsMenuItem = menu.add(0, DIRECTIONS_MENU_ITEM_ID, 20, R.string.option_directions);
//	        directionsMenuItem.setAlphabeticShortcut('e');
	        directionsMenuItem.setIcon(android.R.drawable.ic_menu_directions); 
        
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000CO", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000CO", exp, this);
		}
        return true;
	}
		
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if( !super.onOptionsItemSelected(item) ){
			try{
				switch(item.getItemId()){
					case EDIT_ID:
						Event.onEvent(Event.NEARMINDER_ATTACHMENT_HANDLER_EDIT_OPTIONS_MENU_ITEM_CLICKED, null); 
						
						Intent nearminderIntent = new Intent(Intent.ACTION_EDIT, mUri);
						// ComponentName cn = new ComponentName(StaticConfig.PACKAGE_NAME, "com.flingtap.done.NearminderActivity");
						// nearminderIntent.setComponent(cn);		
						// nearminderIntent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
						startActivityForResult(nearminderIntent, REQUEST_EDIT); // TODO: !!! Consider changing all calls to startActivity to startActivityForResult so the error message can be displayed.
						return true;
					case HELP_ID:
						Intent helpIntent = StaticDisplayActivity.createIntent(this, R.layout.help_nearminder_viewer, R.string.help_select_area); 
						assert null != helpIntent;
						startActivity(helpIntent); // TODO: !!! Consider changing all calls to startActivity to startActivityForResult so the error message can be displayed.  
						return true;
					case DIRECTIONS_MENU_ITEM_ID:
						Nearminder.launchGetDirections(this, mUri);
						return true;
						
//					default:
						// May be participant or child activity's item. Not necessarily a problem.
//						Log.w(TAG, "ERR000CQ Unrecognized options item selected. " + item.getItemId());
//						ErrorUtil.handleNotifyUser("ERR000CQ", "Unrecognized options item selected. " + item.getItemId(), this, this);
				}

			}catch(HandledException h){ // Ignore.
			}catch(Exception exp){
				Log.e(TAG, "ERR000CP", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR000CP", exp, this);
			}
		}
		return false;
	}
	
	private static final int REQUEST_EDIT = 1;
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		try{
			switch(requestCode){
				case REQUEST_EDIT:
					doPrepareView();
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000CR", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000CR", exp, this);
		}
	}
	
}
