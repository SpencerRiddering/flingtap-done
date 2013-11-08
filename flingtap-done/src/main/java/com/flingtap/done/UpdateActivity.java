// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Checkable;

import com.flingtap.done.base.R;
import com.flingtap.done.util.Constants;
import com.tomgibara.android.veecheck.VeecheckActivity;
import com.tomgibara.android.veecheck.VeecheckState;
import com.tomgibara.android.veecheck.util.PrefState;

/**
 * VeeCheck related.
 *
 * See: http://www.tomgibara.com/android/veecheck/
 */
public class UpdateActivity extends VeecheckActivity {
	private static final String TAG = "UpdateActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SessionUtil.onSessionStart(this);

		setContentView(R.layout.update);

		// Prevent user from ignoring required updates. 
        Intent updateIntent = getUpdateIntent();
        if(updateIntent != null && updateIntent.hasExtra(Constants.EXTRA_UPDATE_DEADLINE)){
        	CheckBox checkable = (CheckBox) findViewById(R.id.stop);
        	checkable.setChecked(false);
        	checkable.setVisibility(View.GONE);
        }
	}
	
	@Override
	protected VeecheckState createState() {
		return new PrefState(this);
	}
	
	@Override
	protected View getNoButton() {
		return findViewById(R.id.no);
	}
	
	@Override
	protected View getYesButton() {
		return findViewById(R.id.yes);
	}

	@Override
	protected Checkable getStopCheckBox() {
		return (Checkable) findViewById(R.id.stop);
	}
	@Override
	protected void onResume() {
		super.onResume();
		SessionUtil.onSessionStart(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		SessionUtil.onSessionStop(this);
	}
	

}
