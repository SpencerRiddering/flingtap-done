// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flurry.android.FlurryAgent;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

// TODO: !!! Consider whether FlurryAgent.onStartSession(..) should be called in every onCreate(..).
/**
 * 
 */
public abstract class CoordinatedActivity extends Activity {
	private static final String TAG = "CoordinatedActivity";
	
	ParticipantOrganizer partOrg = new ParticipantOrganizer();

	protected void addParticipant(ContextActivityParticipant participant){
		participant.setIntent(getIntent());
		partOrg.addParticipant(participant);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SessionUtil.onSessionStart(this);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		try{
			super.onActivityResult(requestCode, resultCode, data);
			partOrg.onActivityResult(requestCode, resultCode, data);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00023", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00023", exp, this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		try{
			boolean result = super.onCreateOptionsMenu(menu); 
			return result || partOrg.onCreateOptionsMenu(menu);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00022", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00022", exp, this);
		}
		return false;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		try{
			boolean result = super.onPrepareOptionsMenu(menu); 
			return result || partOrg.onPrepareOptionsMenu(menu);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00024", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00024", exp, this);
		}
		return false;
	}

	protected Dialog onCreateDialog(int dialogId) {
		try{
			Dialog dialog = super.onCreateDialog(dialogId); // TODO: Verify if I need to do this.
			if( null == dialog ){
				return partOrg.onCreateDialog(dialogId);
			}else{
				return dialog;
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00025", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00025", exp, this);
		}
		return null;
	}

	protected void onPrepareDialog(int dialogId, Dialog dialog){
		try{
			super.onPrepareDialog(dialogId, dialog); 
			partOrg.onPrepareDialog(dialogId, dialog);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00026", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00026", exp, this);
		}
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
	
	
	public void  onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		try{
			partOrg.onSaveInstanceState(outState);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000E9", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000E9", exp, this);
		}
	}
	public void  onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		try{
			partOrg.onRestoreInstanceState(savedInstanceState);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000EA", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000EA", exp, this);
		}

	}

	protected void onDestroy() {
		super.onDestroy();
		partOrg.onDestroy();
	}
	
}
