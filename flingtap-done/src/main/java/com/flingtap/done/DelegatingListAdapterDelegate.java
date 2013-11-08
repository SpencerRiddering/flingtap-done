// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;

/**
 * 
 * 
 * @author spencer
 *
 */
public abstract class DelegatingListAdapterDelegate 
		implements ContextListActivityParticipant { //, TextEntryDialogPart.OnTextSetListener
	private static final String TAG = "DelegatingListAdapterDelegate";
	
	protected Intent mIntent;
	protected long mTaskId = -1;
	
	public UriDelegateMapping[] uriDelegateMapping = null; 
	public class UriDelegateMapping {
		public String authority;
		public String pathPattern;
		public int code; // When the delegate handles the bindView(..) this code is included so it knows which mapping invoked it. This covers the case where a single delegate handles multiple mappings. 
	};

	// Isn't this uncessary since we are passing a reference to the Activity as well?
	public void setIntent(Intent intent){ // TODO: Move this initialization into the constructor.
		assert intent != null;
		mIntent = intent;
	}
	
	public void setTaskId(long taskId) {
		// Log.d(TAG, "setTaskId("+taskId+")");
		assert taskId != -1;
		mTaskId = taskId;
	}	
	
	protected abstract void bindView(View view, Context context, Cursor cursor, int code, Uri data); // TODO: Perhaps data is not necessary here. Could get it from ATTACHMENTS_URI_INDEX, right?   
	
    public void onDestroy() {
    }


}
