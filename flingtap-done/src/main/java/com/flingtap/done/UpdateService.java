// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import static com.tomgibara.android.veecheck.Veecheck.LOG_TAG;

import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.PatternMatcher;
import android.util.Log;

import com.flingtap.common.HandledException;
import com.flingtap.done.util.Constants;
import com.flingtap.done.base.R;
import com.tomgibara.android.veecheck.VeecheckNotifier;
import com.tomgibara.android.veecheck.VeecheckService;
import com.tomgibara.android.veecheck.VeecheckState;
import com.tomgibara.android.veecheck.util.DefaultNotifier;
import com.tomgibara.android.veecheck.util.PrefSettings;
import com.tomgibara.android.veecheck.util.PrefState;

/**
 * VeeCheck related.
 *
 * See: http://www.tomgibara.com/android/veecheck/
 */
public class UpdateService extends VeecheckService {
	private static final String TAG = "UpdateService";
	
	public static final int NOTIFICATION_ID = 1; // StartupBroadcastReceiver reserves this id for our exclusive use.

	@Override
	/**
	 * @returns A notifier implementation.
	 */
	protected VeecheckNotifier createNotifier() {
		try{
			//Log.v(TAG, "createNotifier(..) called.");
			//Log.v(TAG, "createNotifier() called.");
			//it's good practice to set up filters to help guard against malicious intents 
			IntentFilter[] filters = new IntentFilter[1];

            IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
            filter.addDataScheme("http");
            filter.addDataAuthority("beta.leankeen.com", null);
            filter.addDataAuthority("www.leankeen.com", null);
            filter.addDataAuthority("www.flingtap.com", null);
            filter.addDataAuthority("market.android.com", null);
            filters[0] = filter;

			// Returns the notifier implementation
			return new DefaultNotifier(this, NOTIFICATION_ID, filters,
					new Intent(this, UpdateActivity.class),
					R.drawable.update_notif, // TODO: !! Change this icon to something smaller and maybe designed specifically for this situation.
					R.string.notify_updateAvailable,
					R.string.notify_updateAvailable,
					R.string.notify_newVersionOfFlingTapDoneFound){
				
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
						SharedPreferences settings = getSharedPreferences(ApplicationPreference.NAME, Context.MODE_PRIVATE); 
						Editor editor = settings.edit();
						if( null != intent && intent.hasExtra(Constants.EXTRA_UPDATE_DEADLINE)){
							// A required update exists.
							String dueDate = intent.getStringExtra(Constants.EXTRA_UPDATE_DEADLINE);
							if( null != dueDate ){
								long dueDateMillis = Long.parseLong(dueDate);
								
								editor.putString(ApplicationPreference.UPDATE_REQUIRED_URI, intent.toURI());
								editor.putLong(ApplicationPreference.UPDATE_REQUIRED_DEADLINE, dueDateMillis);
							}else{
								// A required update _does not_ exist.
								editor.remove(ApplicationPreference.UPDATE_REQUIRED_URI);
								editor.remove(ApplicationPreference.UPDATE_REQUIRED_DEADLINE);
							}
							
						}else{
							// A required update _does not_ exist.
							editor.remove(ApplicationPreference.UPDATE_REQUIRED_URI);
							editor.remove(ApplicationPreference.UPDATE_REQUIRED_DEADLINE);
						}
						editor.commit();
						
					}catch(HandledException h){ // Ignore.
					}catch(Exception exp){
						Log.e(TAG, "ERR000DC", exp);
						ErrorUtil.handleException("ERR000DC", exp, UpdateService.this);
					}
					return intent;
				}
			};

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0009L", exp);
			ErrorUtil.handleException("ERR0009L", exp, this);
		}
		return null; // TODO: !!! Reconsider this default return value.
	}
	
	@Override
	protected VeecheckState createState() {
		return new PrefState(this);
	}
	
}
