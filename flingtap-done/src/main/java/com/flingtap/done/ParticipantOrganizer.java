// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

/**
 * 
 * @author spencer
 * 
 */
public class ParticipantOrganizer {
	protected static final String TAG = "ParticipantOrganizer";
	
	
	ArrayList<ContextActivityParticipant> participantsWithInstanceState = null;
	ArrayList<ContextActivityParticipant> participantsWithMenuItems = null;
	protected HashMap<Integer, ContextActivityParticipant> resultCallbackMap = null;
	
	protected void addParticipant(ContextActivityParticipant participant){
		if( participant.getFirstCodeId() != ContextActivityParticipant.IGNORE_CODE_ID){ 
			if(null == resultCallbackMap){
				resultCallbackMap = new HashMap<Integer, ContextActivityParticipant>();
			}
			resultCallbackMap.put(participant.getFirstCodeId(), participant);
		}
		if( participant.hasMenu() ){
			if( null == participantsWithMenuItems){
				participantsWithMenuItems = new ArrayList<ContextActivityParticipant>();
			}
			participantsWithMenuItems.add(participant);
		}
		if( participant.hasInstanceState() ){
			if( null == participantsWithInstanceState){
				participantsWithInstanceState = new ArrayList<ContextActivityParticipant>();
			}
			participantsWithInstanceState.add(participant);
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(null == resultCallbackMap){
			return;
		}
		int mod = requestCode % 100;
		int firstInResultCodeRange = requestCode - mod;
		//Log.v(TAG, "resulCode - (requestCode % 100) == " + firstInResultCodeRange );
		
    	ContextActivityParticipant participant = resultCallbackMap.get(firstInResultCodeRange);
    	if( null != participant ){
    		participant.onActivityResult(requestCode, resultCode, data);
    	} else {
    		//Log.v(TAG, "participant not found. Maybe none is registered.");
    	}		
	}
	
	protected void onSaveInstanceState(Bundle outState){ 
		if( null == participantsWithInstanceState ){
			return;
		}
		Iterator<ContextActivityParticipant> itr = participantsWithInstanceState.listIterator();
		while(itr.hasNext()){
			 itr.next().onSaveInstanceState(outState);
		}
	}
	
	protected void onRestoreInstanceState(Bundle savedInstanceState){ 
		if( null == participantsWithInstanceState ){
			return;
		}
		Iterator<ContextActivityParticipant> itr = participantsWithInstanceState.listIterator();
		while(itr.hasNext()){
			 itr.next().onRestoreInstanceState(savedInstanceState);
		}
	}

	
	public boolean onCreateOptionsMenu(Menu menu) {
		if( null == participantsWithMenuItems ){
			return false;
		}
		boolean result = false; 
		Iterator<ContextActivityParticipant> itr = participantsWithMenuItems.listIterator();
		while(itr.hasNext()){
			 result = itr.next().onCreateOptionsMenu(menu) || result; 
		}
		return result;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		if( null == participantsWithMenuItems ){
			return false;
		}		
		boolean result = false;
		Iterator<ContextActivityParticipant> itr = participantsWithMenuItems.listIterator();
		while(itr.hasNext()){
			 result = itr.next().onPrepareOptionsMenu(menu) || result;
		}
		return result;
	}

	
	
	
	
	public Dialog onCreateDialog(int dialogId) {
		if(null == resultCallbackMap){
			return null;
		}
		int mod = dialogId % 100;
		int firstInCodeIdRange = dialogId - mod;
		//Log.v(TAG, "resulId - (dialogId % 100) == " + firstInCodeIdRange );
		
    	ContextActivityParticipant participant = resultCallbackMap.get(firstInCodeIdRange);
    	if( null != participant ){
    		return participant.onCreateDialog(dialogId);
    	} else {
    		//Log.v(TAG, "participant not found. Maybe none is registered.");
    		return null;
    	}				
	}

	public void onPrepareDialog(int dialogId, Dialog dialog){
		if(null == resultCallbackMap){
			return;
		}
		int mod = dialogId % 100;
		int firstInCodeIdRange = dialogId - mod;
		//Log.v(TAG, "resulId - (dialogId % 100) == " + firstInCodeIdRange );
		
    	ContextActivityParticipant participant = resultCallbackMap.get(firstInCodeIdRange);
    	if( null != participant ){
    		participant.onPrepareDialog(dialogId, dialog);
    	} 			

	}	
	
	public void onDestroy(){ 
		if( null == participantsWithInstanceState ){
			return;
		}
		Iterator<ContextActivityParticipant> itr = participantsWithInstanceState.listIterator();
		while(itr.hasNext()){
			 itr.next().onDestroy();
		}
	}	
	
	
}
