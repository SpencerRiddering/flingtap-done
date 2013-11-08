// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.flurry.android.FlurryAgent;
import com.flingtap.common.EulaPromptBase;
import com.flingtap.common.HandledException;
import com.flingtap.done.base.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;


public class EulaPromptPart extends EulaPromptBase {
	private final static String TAG = "EulaPromptPart";

	public void promptWithEula(Activity activity){
		
		promptWithEula(activity, 
				R.raw.privacy,
				R.string.agreement,
				R.string.button_accept,
				R.string.button_decline);
	}
	
	public void onAccept(Activity activity){
		if( FeatureIntroduction.intro_introductionNeeded(activity) ){
			FeatureIntroduction.launch(activity, true);
		}
	}
}
