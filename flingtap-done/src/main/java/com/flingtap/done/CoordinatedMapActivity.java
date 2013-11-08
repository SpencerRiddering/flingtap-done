// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.common.HandledException;
import com.flurry.android.FlurryAgent;
import com.google.android.maps.MapActivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

/**
 * A MapActivity which supports the use of participants (parts).
 *
 * @author spencer
 */
public abstract class CoordinatedMapActivity extends MapActivity {
	private static final String TAG = "CoordinatedMapActivity";
	
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
			Log.e(TAG, "ERR00027", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00027", exp, this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		try{
			boolean result = super.onCreateOptionsMenu(menu); 
			return result || partOrg.onCreateOptionsMenu(menu);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00028", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00028", exp, this);
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
			Log.e(TAG, "ERR00029", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00029", exp, this);
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
			Log.e(TAG, "ERR0002A", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0002A", exp, this);
		}
		return null;
	}

	protected void onPrepareDialog(int dialogId, Dialog dialog){
		try{
			super.onPrepareDialog(dialogId, dialog); 
			partOrg.onPrepareDialog(dialogId, dialog);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0002B", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0002B", exp, this);
		}
	}		
	
	@Override
	protected void onResume() {
		super.onResume();
// super.onResume() sometimes results in this error:
//				07-03 15:43:25.428: ERROR/ActivityThread(3019): android.app.IntentReceiverLeaked: Activity com.flingtap.done.NearminderViewer has leaked IntentReceiver android.net.NetworkConnectivityListener$ConnectivityBroadcastReceiver@431ebd50 that was originally registered here. Are you missing a call to unregisterReceiver()?
//				07-03 15:43:25.428: ERROR/ActivityThread(3019):     at android.app.ActivityThread$PackageInfo$ReceiverDispatcher.<init>(ActivityThread.java:707)
//				07-03 15:43:25.428: ERROR/ActivityThread(3019):     at android.app.ActivityThread$PackageInfo.getReceiverDispatcher(ActivityThread.java:535)
//				07-03 15:43:25.428: ERROR/ActivityThread(3019):     at android.app.ApplicationContext.registerReceiverInternal(ApplicationContext.java:748)
//				07-03 15:43:25.428: ERROR/ActivityThread(3019):     at android.app.ApplicationContext.registerReceiver(ApplicationContext.java:735)
//				07-03 15:43:25.428: ERROR/ActivityThread(3019):     at android.app.ApplicationContext.registerReceiver(ApplicationContext.java:729)
//				07-03 15:43:25.428: ERROR/ActivityThread(3019):     at android.content.ContextWrapper.registerReceiver(ContextWrapper.java:278)
//				07-03 15:43:25.428: ERROR/ActivityThread(3019):     at android.net.NetworkConnectivityListener.startListening(NetworkConnectivityListener.java:138)
//				07-03 15:43:25.428: ERROR/ActivityThread(3019):     at com.google.android.maps.MapActivity.onResume(MapActivity.java:232)		
// not our code, other people have the same problem: http://groups.google.com/group/android-developers/browse_thread/thread/80dfddcc731a48b3/a66122c11cf20a4b?lnk=gst&q=ConnectivityBroadcastReceiver#a66122c11cf20a4b		
		
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
			Log.e(TAG, "ERR000EB", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000EB", exp, this);
		}
	}
	public void  onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		try{
			partOrg.onRestoreInstanceState(savedInstanceState);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000EC", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000EC", exp, this);
		}
	}
	protected void onDestroy() {
		super.onDestroy();
		partOrg.onDestroy();
	}


}
