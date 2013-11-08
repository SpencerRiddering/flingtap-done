// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 
 * @author spencer
 *
 */
public class CompletableBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG = "CompletableBroadcastReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		//Log.v(TAG, "Broadcast action is ACTION_DELETE");
		int count = context.getContentResolver().delete(intent.getData(), null, null);
		if( 1 != count ){
    		Log.e(TAG, "ERR000FM Unable to delete Completable.");
    		ErrorUtil.handle("ERR000FM", "Unable to delete Completable. URI = "+intent.getData(), this);
		}
		
	}

}
