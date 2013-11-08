// Licensed under the Apache License, Version 2.0

package com.flingtap.done.addon.quicklaunch;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;

import com.flingtap.common.UpdateServiceBase;

public class UpdateService extends UpdateServiceBase {
	private static final String TAG = "UpdateService";
	
	public static final int NOTIFICATION_ID = 1; // StartupBroadcastReceiver reserves this id for our exclusive use.

	protected int getUpdateNotifyResId(){
		return R.drawable.update_notif;
	}
	protected int getNotifyTickerResId(){
		return R.string.notify_ticker;
	}
	protected int getNotifyTitleResId(){
		return R.string.notify_title;
	}
	protected int getNotifyMessageResId(){
		return R.string.notify_message;
	}
	
	protected Class getComponentClass(){
		return UpdateActivity.class;
	}
	
	protected IntentFilter getIntentFilter() throws MalformedMimeTypeException {
		IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
		filter.addDataScheme("http");
		filter.addDataAuthority("beta.leankeen.com", null);
		filter.addDataAuthority("www.leankeen.com", null);
		filter.addDataAuthority("www.flingtap.com", null);
		filter.addDataAuthority("market.android.com", null);
		
		return filter;
	}

	

	
}
