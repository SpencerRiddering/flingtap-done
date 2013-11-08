// Licensed under the Apache License, Version 2.0

package com.flingtap.done.addon.quicklaunch;

import com.flingtap.common.UpdateRetrieverBase;

/**
 */
public class UpdateRetriever extends UpdateRetrieverBase {
	private static final String TAG = "UpdateRetriever";

	protected long getPeriod(){
		return StaticConfig.VEECHECK_PERIOD;
	}
	protected long getCheckInterval(){
		return StaticConfig.VEECHECK_CHECK_INTERVAL;
	}
	protected String getCheckUri(){
		return StaticConfig.VEECHECK_CHECK_URI;
	}

	
}
