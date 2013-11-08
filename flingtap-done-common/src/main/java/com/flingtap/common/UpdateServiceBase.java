// Licensed under the Apache License, Version 2.0

package com.flingtap.common;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import com.tomgibara.android.veecheck.VeecheckNotifier;
import com.tomgibara.android.veecheck.VeecheckService;
import com.tomgibara.android.veecheck.VeecheckState;
import com.tomgibara.android.veecheck.util.DefaultNotifier;
import com.tomgibara.android.veecheck.util.PrefState;

import java.util.Map;

public abstract class UpdateServiceBase extends VeecheckService {
	private static final String TAG = "UpdateServiceBase";
	
	public static final int NOTIFICATION_ID = 1; // StartupBroadcastReceiver reserves this id for our exclusive use.

	protected abstract int getUpdateNotifyResId(); // R.drawable.update_notif // TODO: !! Change this icon to something smaller and maybe designed specifically for this situation.
	protected abstract int getNotifyTickerResId(); // R.string.notify_updateAvailable
	protected abstract int getNotifyTitleResId(); // R.string.notify_updateAvailable
	protected abstract int getNotifyMessageResId(); // R.string.notify_newVersionOfFlingTapDoneFound
	protected abstract Class getComponentClass(); // UpdateActivityBase.class

	protected abstract IntentFilter getIntentFilter() throws MalformedMimeTypeException;

	@Override
	/**
	 * @returns A notifier implementation.
	 */
	protected VeecheckNotifier createNotifier() {
		try{
			//Log.v(TAG, "createNotifier() called.");
			//it's good practice to set up filters to help guard against malicious intents
			IntentFilter[] filters = new IntentFilter[1];
			try {
				filters[0] = getIntentFilter();
			} catch (MalformedMimeTypeException e) {
				Log.e(TAG, "Invalid data type for filter.", e);
				ErrorUtil.handleException("ERR000IJ", e, this);
			}
			
			// Returns the notifier implementation
			return new DefaultNotifier(this, NOTIFICATION_ID, filters,
					new Intent(this, getComponentClass()),
					getUpdateNotifyResId(), 
					getNotifyTickerResId(),
					getNotifyTitleResId(),
					getNotifyMessageResId()){

				/**
				 * This method:
				 *  1. Identifies whether this is a required update.
				 *  2. Saves the required update into the preferences file.
				 *  3. Saves the deadline date into the preferences file. 
				 * 
				 * @Override
				 */
				public Intent createIntent(String action, String data, String type, Map<String, String> extras) {
					//Log.v(TAG, "createIntent(...) called.");
					//Log.v(TAG, "action="+action+", data="+data+", type="+type);
					//generate an intent from the supplied parameters
					Intent intent = super.createIntent(action, data, type, extras);
					try{
						SharedPreferences settings = getSharedPreferences(ApplicationPreferenceBase.NAME, Context.MODE_PRIVATE); 
						Editor editor = settings.edit();
						if( null != intent && intent.hasExtra(UpdateActivityBase.EXTRA_UPDATE_DEADLINE)){
							// A required update exists.
							String dueDate = intent.getStringExtra(UpdateActivityBase.EXTRA_UPDATE_DEADLINE);
							if( null != dueDate ){
								long dueDateMillis = Long.parseLong(dueDate);
								
								editor.putString(ApplicationPreferenceBase.UPDATE_REQUIRED_URI, intent.toURI());
								editor.putLong(ApplicationPreferenceBase.UPDATE_REQUIRED_DEADLINE, dueDateMillis);
							}else{
								// A required update _does not_ exist.
								editor.remove(ApplicationPreferenceBase.UPDATE_REQUIRED_URI);
								editor.remove(ApplicationPreferenceBase.UPDATE_REQUIRED_DEADLINE);
							}
							
						}else{
							// A required update _does not_ exist.
							editor.remove(ApplicationPreferenceBase.UPDATE_REQUIRED_URI);
							editor.remove(ApplicationPreferenceBase.UPDATE_REQUIRED_DEADLINE);
						}
						editor.commit();
						
					}catch(HandledException h){ // Ignore.
					}catch(Exception exp){
						Log.e(TAG, "ERR000IK", exp);
						ErrorUtil.handleException("ERR000IK", exp, UpdateServiceBase.this);
					}
					return intent;
				}
			};

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000IL", exp);
			ErrorUtil.handleException("ERR000IL", exp, this);
		}
		return null; // TODO: !!! Reconsider this default return value.
	}
	
	@Override
	protected VeecheckState createState() {
		return new PrefState(this);
	}
	
}
