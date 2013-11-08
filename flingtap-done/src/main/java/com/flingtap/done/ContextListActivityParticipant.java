// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.content.Context;
import android.content.Intent;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

/**
 * Interface for those objects that want to participate in lifecycle and other ListActivity events.
 */
public interface ContextListActivityParticipant extends ContextActivityParticipant{

	/**
	 * Defines the smallest integer used by this object for the selected item id.
	 * 
	 * Convention is to use round 100*n values. ex 500, 600, 700, etc.... 
	 * The implementing class should limit themselves to using no more than 100 request 
	 *   codes starting with the value returned by this method. 
	 *   Example: 500-599 
	 *   
	 * The 100-199 range is reserved for use by the main Activity.
	 *   
	 * Returning 0 will cause ContextActivityParticipant to be ignored.
	 * 
	 * @return The smallest integer used by this object for the selected item id.
	 */
	public int getFirstCodeId();
	
	/**
	 * Indicates whether this participant has a context menu for _all_ list items. 
	 * 
	 * DelegatingListAdapterDelegate's always receive context menu events for their entry types 
	 *   and so should not return true from this method. Returning true in this case will result in 
	 *   duplicate calls to the context menu event methods. 
	 *    
	 * @return
	 */
	public boolean hasContextMenu();
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo);
	
	public boolean onContextItemSelected(MenuItem item);
	
	public void onListItemClick(ListView listview, View view, int position, long id);
	
}
