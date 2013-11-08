// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.ArrayList;
import java.util.HashMap;

import com.flingtap.common.HandledException;
import com.flingtap.done.AttachmentListAdapter.UriMapping;
import com.flurry.android.FlurryAgent;


import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * A ListActivity which supports the use of participants (parts).
 *
 * @author spencer
 */
public abstract class CoordinatedListActivity extends ListActivity {
	private static final String TAG = "CoordinatedListActivity";
	ListParticipantOrganizer partOrg = new ListParticipantOrganizer();

	protected void addParticipant(ContextActivityParticipant participant){
		participant.setIntent(getIntent());
		partOrg.addParticipant(participant);
	}

	protected void addParticipant(ContextListActivityParticipant participant){
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
			Log.e(TAG, "ERR00021", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00021", exp, this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		try{
			boolean result = super.onCreateOptionsMenu(menu); 
			return partOrg.onCreateOptionsMenu(menu) || result;
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00020", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00020", exp, this);
		}
		return false;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		try{
			boolean result = super.onPrepareOptionsMenu(menu); 
			return partOrg.onPrepareOptionsMenu(menu) || result;
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001Z", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0001Z", exp, this);
		}
		return false;
	}
	
	/**
	 *  The following line must be included somewhere in the activity for context callbacks to work.
     *	 getListView().setOnCreateContextMenuListener(this);
	 */
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo){
		try{
			super.onCreateContextMenu(menu, view, menuInfo);
			//Log.v(TAG, "onCreateContextMenu(..) called.");
			AdapterView.AdapterContextMenuInfo menuInfoDetails = (AdapterView.AdapterContextMenuInfo)menuInfo;
			View selectedItem = menuInfoDetails.targetView;
			
			// Give the DelegatingListAdapterDelegate a chance to add context menu items.
			// Map down to the DelegatingListAdapterDelegate using the authority, path, etc..
			// Note: Since DelegatingListAdapterDelegate is a Participant, it should not return true 
			//       from hasContextMenu(), otherwise onCreateContextMenu(..) will be 
			//       called twice (here and below). 
			ContextListActivityParticipant part = getParticipant(selectedItem.getTag());
			if( null != part ){
				part.onCreateContextMenu(menu, view,  menuInfo);
			}
			// Give any participants a chance to add context menu items.
			partOrg.onCreateContextMenu(menu, view, menuInfo);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001T", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0001T", exp, this);
		}
	}
	
	/**
	 * Provides default behavior finding the ContextListActivityParticipant from within the view tag.
	 */
	public ContextListActivityParticipant getParticipant(Object tag){
		return null;
	}
	
	
	/**
	 *  The following line must be included somewhere in the activity for context callbacks to work.
     *	 getListView().setOnCreateContextMenuListener(this);
	 */
	public boolean onContextItemSelected(MenuItem item){
		try{
			//Log.v(TAG, "onContextItemSelected(..) called.");
			if( super.onContextItemSelected(item) ){
				return true;
			}else{
				return partOrg.onContextItemSelected(item);
			}
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001V", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0001V", exp, this);
		}
		return false;
    	
	}

	@Override
	protected void onListItemClick(ListView listview, View view, int position, long id) {
		try{
			//Log.v(TAG, "onListItemClick(..) called.");
			super.onListItemClick(listview, view, position, id);
			ContextListActivityParticipant part = getParticipant(view.getTag());
			if( null != part ){
				part.onListItemClick(listview, view, position, id);
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001W", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0001W", exp, this);
		}
	}

	protected Dialog onCreateDialog(int dialogId) {
		try{
			Dialog dialog = super.onCreateDialog(dialogId);
			if( null == dialog ){
				return partOrg.onCreateDialog(dialogId);
			}else{
				return dialog;
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001X", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0001X", exp, this);
		}
		return null;
	}

	protected void onPrepareDialog(int dialogId, Dialog dialog){
		try{		
			super.onPrepareDialog(dialogId, dialog); 
			partOrg.onPrepareDialog(dialogId, dialog);
			
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001Y", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR0001Y", exp, this);
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
			Log.e(TAG, "ERR000E7", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000E7", exp, this);
		}
	}
	public void  onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		try{
			partOrg.onRestoreInstanceState(savedInstanceState);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR000E8", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR000E8", exp, this);
		}
		
	}

	protected void onDestroy() {
		super.onDestroy();
		partOrg.onDestroy();
	}

}
