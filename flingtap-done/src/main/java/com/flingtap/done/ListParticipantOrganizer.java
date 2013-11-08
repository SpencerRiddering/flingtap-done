// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.flurry.android.FlurryAgent;

import android.app.Dialog;
import android.content.Intent;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

/**
 * 
 * @author spencer
 *
 */
public class ListParticipantOrganizer extends ParticipantOrganizer {
	protected static final String TAG = "ParticipantOrganizer";
	
	
	ArrayList<ContextListActivityParticipant> participantsWithContextMenuItems = null;
	
	protected void addParticipant(ContextListActivityParticipant participant){
		addParticipant((ContextActivityParticipant) participant);
		if( participant.hasContextMenu() ){
			if( null == participantsWithContextMenuItems){
				participantsWithContextMenuItems = new ArrayList<ContextListActivityParticipant>();
			}
			participantsWithContextMenuItems.add(participant);
		}
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
		if( null == participantsWithContextMenuItems ){
			return;
		}
		Iterator<ContextListActivityParticipant> itr = participantsWithContextMenuItems.listIterator();
		while(itr.hasNext()){
			itr.next().onCreateContextMenu(menu, v, menuInfo); 
		}
	}
	
	
	public boolean onContextItemSelected(MenuItem item){
		if(null == resultCallbackMap){
			return false;
		}
		int mod = item.getItemId() % 100;
		int firstInResultCodeRange = item.getItemId() - mod;
		//Log.v(TAG, "item.getItemId() - (item.getItemId() % 100) == " + firstInResultCodeRange );
		
    	ContextListActivityParticipant participant = (ContextListActivityParticipant)resultCallbackMap.get(firstInResultCodeRange);
    	if( null != participant ){
    		return participant.onContextItemSelected(item);
    	} else {
    		// TODO: Does this warrent a warning? I mean is it the result of bad coding?
    		//Log.v(TAG, "participant not found. Maybe none is registered.");
    	}	
		return false;
	}


}
