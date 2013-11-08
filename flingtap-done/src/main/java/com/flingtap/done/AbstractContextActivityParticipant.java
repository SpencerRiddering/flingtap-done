// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

public abstract class AbstractContextActivityParticipant implements
		ContextActivityParticipant {

	protected Intent mIntent = null;
	
	public int getFirstCodeId() {
		return IGNORE_CODE_ID;
	}

	public boolean hasMenu(){
		return false;
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	}

	public Dialog onCreateDialog(int dialogId) {
		return null;
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	public void onPrepareDialog(int dialogId, Dialog dialog) {
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}

	/**
	 * TODO: !! Remove setIntent(..) and let each implementation take care of deciding whether to store it.
	 */
	public void setIntent(Intent intent) {
		assert null != intent;
		mIntent = intent;
	}

	public boolean hasInstanceState(){
		return false;
	}
	
	public void  onSaveInstanceState  (Bundle outState){
	}
	
	public void  onRestoreInstanceState  (Bundle savedInstanceState){
	}

    public void onDestroy() {
    }
	
}
