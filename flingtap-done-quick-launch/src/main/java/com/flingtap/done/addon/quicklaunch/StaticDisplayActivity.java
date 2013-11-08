// Licensed under the Apache License, Version 2.0

package com.flingtap.done.addon.quicklaunch;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;


public class StaticDisplayActivity extends com.flingtap.common.StaticDisplayActivityBase {

	public String getSessionKey(){
		return StaticConfig.FLURRY_PROJECT_KEY;
	}

	public static final Intent createIntent(Context context, int layout, int title){
		Intent helpIntent = new Intent();
		ComponentName cn = new ComponentName(context.getPackageName(), StaticDisplayActivity.class.getName());
		helpIntent.setComponent(cn);
		helpIntent.putExtra(StaticDisplayActivity.EXTRA_TITLE_RES_ID, title);
		helpIntent.putExtra(StaticDisplayActivity.EXTRA_LAYOUT_RES_ID, layout);
		return helpIntent;
	}
	
}
