// Licensed under the Apache License, Version 2.0

package com.flingtap.done.addon.quicklaunch;

import com.flingtap.common.EulaPromptBase;

import android.app.Activity;

public class EulaPrompt extends EulaPromptBase {
	private final static String TAG = "EulaPrompt";
    
	public void promptWithEula(Activity activity){
		
		promptWithEula(activity, 
				R.raw.eula, 
				R.string.eula_prompt_title, 
				R.string.eula_prompt_button_accept,
				R.string.eula_prompt_button_reject);
	}
}
