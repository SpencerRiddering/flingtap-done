// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

/**
 * Interface for those objects that want to participate in lifecycle and other Activity events.
 *
 * TODO: !! Add a Context/Activity parameter to all methods so that the implementer doesn't need to store the reference.
 */
public interface ContextActivityParticipant {

	public void setIntent(Intent intent);

	/**
	 * Will always be called regardless of whether getFirstCodeId() returns 0.
	 * 
	 * @param menu
	 * @return
	 */
	public boolean onCreateOptionsMenu(Menu menu);
	
	/**
	 * Will always be called regardless of whether getFirstCodeId() returns 0.
	 * 
	 * @param menu
	 * @return
	 */
	public boolean onPrepareOptionsMenu(Menu menu);
	
	/**
	 * Defines the smallest integer used by this object for the request code or dialog id.
	 * 
	 * Convention is to use round 100*n values. ex 500, 600, 700, etc.... 
	 * The implementing class should limit themselves to using no more than 100 request 
	 *   codes starting with the value returned by this method. 
	 *   Example: 500-599 
	 *   
	 * The 100-199 range is reserved for use by the CoordinatedActivity subclass.
	 *   
	 * Returning 0 (IGNORE_CODE_ID) will cause ContextActivityParticipant to be ignored.
	 * 
	 * @return The smallest integer used by this object for the requestCode.
	 */
	public int getFirstCodeId();
	public static final int IGNORE_CODE_ID = 0;
		
	/**
	 * Flag indicating whether this participant has menu items to add.
	 * 
	 * Controls whether onCreateOptionsMenu(..) and onPrepareOptionsMenu(..) methods called.   
	 * 
	 * @return true if this participant has one or more menu items to add, false otherwise.
	 */
	public boolean hasMenu();
	
	/**
	 * Will only be called if getFirstCodeId() does not returns 0.
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data);

	/**
	 * Will only be called if getFirstCodeId() does not returns 0.
	 */
	public Dialog onCreateDialog(int dialogId);

	/**
	 * Will only be called if getFirstCodeId() does not returns 0.
	 */
	public void onPrepareDialog(int dialogId, Dialog dialog);

	/**
	 * Flag indicating whether this participant has instance state that needs to be saved/restored.
	 * 
	 * Controls whether onCreateOptionsMenu(..) and onPrepareOptionsMenu(..) methods called.   
	 * 
	 * @return true if this participant has one or more menu items to add, false otherwise.
	 */
	public boolean hasInstanceState();
	
	
	public void  onSaveInstanceState  (Bundle outState);
	public void  onRestoreInstanceState  (Bundle savedInstanceState);
	
	public void onDestroy();
}
