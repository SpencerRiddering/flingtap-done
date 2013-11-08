// Licensed under the Apache License, Version 2.0

package com.flingtap.done.addon.quicklaunch;

import com.flingtap.common.UpdateActivityBase;

public class UpdateActivity extends UpdateActivityBase {
	private static final String TAG = "UpdateActivity";

	protected String getSessionApiKey(){
		return StaticConfig.FLURRY_PROJECT_KEY;
	}
	protected int getIconResId(){
		return R.drawable.ic_launcher_quicklaunch;
	}
	protected int getTitleTextResId(){
		return R.string.app_name;
	}
	protected int getConfirmTextResId(){
		return R.string.confirm_text;
	}
	protected int getYesButtonResId(){
		return android.R.string.yes;
	}
	protected int getNoButtonResId(){
		return android.R.string.no;
	}
	protected int getConfirmStopResId(){
		return R.string.confirm_stop;
	}
	
	

}
