// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flurry.android.FlurryAgent;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;

/**
 * 
 * @author spencer
 * 
 * 
 */
public class NearminderEnterIntentReceiver extends BroadcastReceiver {
	private final static String TAG = "NearminderEnterIntentReceiver";
	
	public NearminderEnterIntentReceiver() {
	}

	public void onReceive(Context context, Intent intent) {
		try{
			SessionUtil.onSessionStart(context);
			
		    boolean entering = (Boolean)intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
		    if( !entering ){
		    	//Log.v(TAG, "onReceive(..) called with entering == false.");
		    	return;
		    }else{
		    	//Log.v(TAG, "onReceive(..) called with entering == true.");
		    }
		    
		    Uri proxAlertUri = intent.getData();

		    // Verify that the alert exists and is enabled.
		    Cursor proxAlertCursor = context.getContentResolver().query(proxAlertUri, null, Task.ProximityAlerts.ENABLED+"=?", new String[]{Task.ProximityAlerts.ENABLED_TRUE}, null);
		    assert null != proxAlertCursor;
		    if( proxAlertCursor.getCount() == 1 ){ // Proximity alert exits and it is enabeled.
		    	
		    	// Prepare event info.
				Event.onEvent(Event.NEARMINDER_OCCURRED, null); // Map<String,String> parameters = new HashMap<String,String>();
				
		    	//Log.v(TAG, "Entering proxAlertUri=" + proxAlertUri);
		    	Nearminder.addNotification(context, proxAlertUri);
		    	
		    }else{
		    	Log.w(TAG, "ERR00046 Nearminder occured for non-existent URI " + proxAlertUri);
		    	ErrorUtil.handle("ERR00046", "Nearminder occured for non-existent URI", this);
		    	assert false;
		    }
		    proxAlertCursor.close();
		    
			SessionUtil.onSessionStop(context);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00045", exp);
			ErrorUtil.handleException("ERR00045", exp, context);
		}	
	}
}
