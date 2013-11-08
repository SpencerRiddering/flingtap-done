// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.Application;
import android.content.Context;

public final class SessionUtil extends com.flingtap.common.SessionUtilBase {

		
	public static final void initErrorHandling(Application application){
		com.flingtap.common.SessionUtilBase.initErrorHandling(application, StaticConfig.FLURRY_PROJECT_KEY);
	}
	
	public static final void onSessionStart(Context context){
		com.flingtap.common.SessionUtilBase.onSessionStart(context, StaticConfig.FLURRY_PROJECT_KEY); 
	}
}
