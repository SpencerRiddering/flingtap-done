// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public abstract class AbstractContextListActivityParticipant extends AbstractContextActivityParticipant implements
		ContextListActivityParticipant {

	public boolean hasContextMenu(){
		return false;
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
		return;
	}
	
	public boolean onContextItemSelected(MenuItem item){
		return false;
	}
	
	public void onListItemClick(ListView listview, View view, int position, long id){
		return;
	}
	
}
