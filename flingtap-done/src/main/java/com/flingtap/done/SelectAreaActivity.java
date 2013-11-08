// Licensed under the Apache License, Version 2.0

package com.flingtap.done;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;
import com.google.android.maps.MapView;



/**
 * Prompts the user to select a GPS position and then choose the size of the area around that position.
 * 
 * The area is specified as using either a circle or square boundary. 
 * A number of placemarks may be included and (if one is selected) the placemark info will be included in the response. 
 *  
 * @author spencer
 *
 */
public class SelectAreaActivity extends BasicMapActivity {
    public static final String TAG = "SelectAreaActivity";

	private static final String OUT_STATE_WIZARD_STEP = "WIZARD_STEP";

	private static final String OUT_STATE_INTENT = "INTENT";

	// **************
	// From caller
	// **************
	public static final String EXTRA_OVERLAY_ITEMS = SelectGlobalPositionPart.EXTRA_OVERLAY_ITEMS;
	// 0 for circle (i.e. radius) or 1 for square (i.e. bounded box).
	public static final String EXTRA_BORDER_TYPE = "com.flingtap.done.intent.extra.BORDER_TYPE"; // TODO: !!! Implement this feature.
	public static final String EXTRA_FIXED_POSITION = "com.flingtap.done.intent.extra.FIXED_POSITION"; // TODO: !!! Implement this feature.

    /**
     * The selected resource used when creating this proximity alert.
     * 
     * 0 for circle (i.e. radius).
     * 1 for square (i.e. bounded box).
     * 
     * <P>Type: INTEGER</P>
     */
    public static final String _BORDER_TYPE = "_border_type";

	// To caller
	
	// To/From caller.
	public static final String EXTRA_GLOBAL_POSITION = "com.flingtap.done.extra.GLOBAL_POSITION";
	public static final String EXTRA_RADIUS = "EXTRA_RADIUS";
	public static final String EXTRA_RADIUS_UNIT = "EXTRA_RADIUS_UNIT";
	public static final String EXTRA_ZOOM = "EXTRA_ZOOM";
	
	private Intent intent = null;
	private Intent returnIntent = null;
	SelectAreaBorderPart mAreaBorderPart = null;
	protected Wizard wizard = null;
	
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		try{			
			//Log.v(TAG, "onCreate(..) called.");
			// Prepare event info.
			Event.onEvent(Event.SELECT_AREA, null); // Map<String,String> parameters = new HashMap<String,String>();
			
			intent = getIntent();

			// Restore state.
			if( null != icicle ){
				returnIntent = icicle.getParcelable(OUT_STATE_INTENT);
				int zoomLevel = returnIntent.getIntExtra(EXTRA_ZOOM, 10); // TODO: Consider a better method for deciding the default zoom.
				mapView.getController().setZoom(zoomLevel);			
			}else{
				int zoomLevel = intent.getIntExtra(EXTRA_ZOOM, 10); // TODO: Consider a better method for deciding the default zoom.
				mapView.getController().setZoom(zoomLevel);
			}
			if( null == returnIntent ){
				returnIntent = new Intent();
			}		
	    	wizard = new Wizard(){
	    		protected void onCancel() {
	    			//Log.v(TAG, "wizard.cancel() called.");
	    			setResult(RESULT_CANCELED);
	    			finish();
	    		}
	    		protected void onMoveError(int moveStep) {
	    			Log.e(TAG, "ERR000G9 A Wizard move error occured. moveStep == " + moveStep);
	    			ErrorUtil.handleExceptionFinish("ERR000G9", (Exception)(new Exception( String.valueOf( moveStep ) )).fillInStackTrace(), SelectAreaActivity.this);
	    			return;
	    		}
	    	};
	    	
	    	if( null == intent.getParcelableExtra(EXTRA_FIXED_POSITION)){
	    		SelectGlobalPositionPart positionPart = new SelectGlobalPositionPart(this, mapView, returnIntent);
	    		addParticipant(positionPart);
	    		wizard.addStep(positionPart);    	
	    	}else{
	    		returnIntent.putExtra(SelectAreaActivity.EXTRA_GLOBAL_POSITION, intent.getParcelableExtra(EXTRA_FIXED_POSITION));
	    	}

	    	mAreaBorderPart = new SelectAreaBorderPart(this, mapView, returnIntent);
	    	addParticipant(mAreaBorderPart);
	    	wizard.addStep(mAreaBorderPart);    	
	    	
	    	// Last Step.
	    	wizard.addStep(new AbstractWizardStep(){
				public void onArrive() {
					setResult(RESULT_OK, returnIntent);
					finish();
				}
	    	});
	    	
			if( null != icicle ){
				// Restore state.
				wizard.moveToStep(icicle.getInt(OUT_STATE_WIZARD_STEP, Wizard.FIRST_STEP)); // Start the wizard process.
			}else{
				wizard.moveToFirst();
			}

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0005N", exp);
			ErrorUtil.handleExceptionFinish("ERR0005N", exp, this);
		}
		
	}

	@Override
	protected void onLayerChanged() {
		super.onLayerChanged();
		mAreaBorderPart.onLayerChanged();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		try{
			wizard.exit();
			outState.putInt(OUT_STATE_WIZARD_STEP, wizard.getCurrentStep());
			outState.putParcelable(OUT_STATE_INTENT, returnIntent);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0005O", exp);
			ErrorUtil.handleExceptionFinish("ERR0005O", exp, this);
		}
	}
	
	protected void initMap(){
		setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
		
		setContentView(R.layout.select_area);
		mapView = (MapView)findViewById(R.id.map_view);		
		mapController = mapView.getController();
		
		// Add zoom controls.
	    LinearLayout zoomLayout = (LinearLayout) findViewById(R.id.map_zoom);
	    zoomLayout.addView(mapView.getZoomControls(),
	        new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT,
	            LayoutParams.WRAP_CONTENT));		
	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		try{
	        MenuItem helpMenuItem = menu.add(0, HELP_ID, Menu.NONE, R.string.option_help);
	        helpMenuItem.setAlphabeticShortcut('h');
	        helpMenuItem.setIcon(android.R.drawable.ic_menu_help); 

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0005P", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0005P", exp, this);
		}
        return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if( !super.onOptionsItemSelected(item) ){
			try{
				switch(item.getItemId()){
					case HELP_ID:
						Intent helpIntent = StaticDisplayActivity.createIntent(SelectAreaActivity.this, R.layout.help_select_area, R.string.help_select_area);
						assert null != helpIntent;
						startActivity(helpIntent); // TODO: !!! Consider changing all calls to startActivity to startActivityForResult so the error message can be displayed.  
						return true;
					default:
						ErrorUtil.handleExceptionNotifyUser("ERR000CN", (Exception)(new Exception( String.valueOf(item.getItemId()) )).fillInStackTrace(), this);
				}

			}catch(HandledException h){ // Ignore.
			}catch(Exception exp){
				Log.e(TAG, "ERR000CM", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR000CM", exp, SelectAreaActivity.this);
			}
		}
		return false;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		try{
			if( keyCode == KeyEvent.KEYCODE_BACK && wizard.getCurrentStep() > Wizard.FIRST_STEP ) { // TODO: !! Move this code into Wizard.checkAndHandleBackButton(..)
				wizard.movePrev();
				return true;
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000A3", exp);
			ErrorUtil.handleExceptionFinish("ERR000A3", exp, this);
		}		
		return super.onKeyDown(keyCode, event);
	}
}
