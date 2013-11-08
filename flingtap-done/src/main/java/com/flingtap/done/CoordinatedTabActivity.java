// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flurry.android.FlurryAgent;

import android.app.Dialog;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

/**
 * A TabActivity which supports the use of participants (parts).
 * 
 * @author spencer
 *
 */
public abstract class CoordinatedTabActivity extends com.flingtap.done.android.TabActivity {
	private static final String TAG = "CoordinatedActivity";
	
	ParticipantOrganizer partOrg = new ParticipantOrganizer();

	protected void addParticipant(ContextActivityParticipant participant){
		participant.setIntent(getIntent());
		partOrg.addParticipant(participant);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SessionUtil.onSessionStart(this); // TabActivity is an ActivityGroup and each of the contained Activities will start/stop the session on their own.
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		try{
			super.onActivityResult(requestCode, resultCode, data);
			partOrg.onActivityResult(requestCode, resultCode, data);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0002C", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0002C", exp, this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		try{
			boolean superResult = super.onCreateOptionsMenu(menu); 
			boolean partResult  = partOrg.onCreateOptionsMenu(menu);
			return partResult || superResult;
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0002D", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0002D", exp, this);
		}		
		return false;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		try{
			boolean superResult = super.onPrepareOptionsMenu(menu); 
			boolean partResult = partOrg.onPrepareOptionsMenu(menu);
			return partResult || superResult;
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0002E", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0002E", exp, this);
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
			Log.e(TAG, "ERR0002F", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0002F", exp, this);
		}		
		return null;
	}

	protected void onPrepareDialog(int dialogId, Dialog dialog){
		try{
			super.onPrepareDialog(dialogId, dialog); 
			partOrg.onPrepareDialog(dialogId, dialog);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0002G", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0002G", exp, this);
		}		
	}		
	
	@Override
	protected void onResume() {
		super.onResume();
		SessionUtil.onSessionStart(this); // TabActivity is an ActivityGroup and each of the contained Activities will start/stop the session on their own.
	}

	@Override
	protected void onPause() {
		super.onPause();
		SessionUtil.onSessionStop(this); // TabActivity is an ActivityGroup and each of the contained Activities will start/stop the session on their own.
	}

	public void  onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		try{
			partOrg.onSaveInstanceState(outState);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000ED", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000ED", exp, this);
		}
	}
	public void  onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		try{
			partOrg.onRestoreInstanceState(savedInstanceState);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000EE", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000EE", exp, this);
		}
	}
	protected void onDestroy() {
		super.onDestroy();
		partOrg.onDestroy();
	}

}
