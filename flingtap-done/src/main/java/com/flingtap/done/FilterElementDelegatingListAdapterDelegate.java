// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.done.provider.Task;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

/**
 */
public abstract class FilterElementDelegatingListAdapterDelegate 
		extends DelegatingListAdapterDelegate {

	public static final String TAG = "FilterElementDelegatingListAdapterDelegate";

	
	protected FilterElementListActivity mActivity;
	
	public void setActivity(FilterElementListActivity activity) {// TODO: Move this initialization into the constructor.
		assert activity != null;
		mActivity = activity;
	}	
	
	protected void notifyDataSetChanged(){
		mActivity.notifyDataSetChanged();
	}
	
	// ******************************
	// Default values.
	// ******************************
	
	public boolean hasMenu(){
		return false;
	}
	
	public boolean hasContextMenu(){
		return false;
	}
	
	public boolean hasInstanceState(){
		return false;
	}

}
