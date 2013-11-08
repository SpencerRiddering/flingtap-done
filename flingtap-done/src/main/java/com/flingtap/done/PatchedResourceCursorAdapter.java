// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.Activity;
import android.database.Cursor;
import android.util.Log;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ResourceCursorAdapter;

public abstract class PatchedResourceCursorAdapter extends ResourceCursorAdapter implements Filterable {
	private static final String TAG = "PatchedResourceCursorAdapter";
	
	protected Activity mActivity = null;
	protected Cursor mCursor = null;
	protected Filter mFilter = null;
	protected int mConvertToStringColumn = -1;
	
	
	/**
	 * 
	 * @param activity
	 * @param resource
	 * @param cursor
	 * @param convertToStringColumn convertToString(..) will return cursor.getString(convertToStringColumn)
	 */
    public PatchedResourceCursorAdapter(Activity activity, int resource, Cursor cursor, int convertToStringColumn){
    	super(activity, resource, cursor);
    	
    	mConvertToStringColumn = convertToStringColumn;
    	assert mConvertToStringColumn >= 0;    	

        init(activity, resource, cursor);
    }
    
    public PatchedResourceCursorAdapter(Activity activity, int resource, Cursor cursor, Filter filter){
    	super(activity, resource, cursor);
    	
    	mFilter = filter;
    	assert null != mFilter;
    	
        init(activity, resource, cursor);
    }
    
    protected void init(Activity activity, int resource, Cursor cursor){
    	
    	mActivity = activity;
    	assert null != mActivity;
    	
    	mCursor = cursor;
    	assert null != mCursor;
    }


    public class CursorFilter extends Filter {
    	private static final String TAG = "PatchedResourceCursorAdapter.CursorFilter";
    	
        protected Filter.FilterResults performFiltering(CharSequence searchText){
        	//Log.v(TAG, "performFiltering(..) caled.");
            Filter.FilterResults results = new Filter.FilterResults();
            
            Cursor cursor = runQueryOnBackgroundThread(searchText);
            if(cursor != null){
                results.count = cursor.getCount();
                results.values = cursor;
            }else{
                results.count = 0;
                results.values = null;
            }
            return results;
        }
        
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
        	//Log.v(TAG, "publishResults(..) caled.");
        	if(results.values != mCursor) {
                changeCursor((Cursor)results.values);        	
        	}
        }
        
        public CharSequence convertResultToString(Object resultValue) {
        	//Log.v(TAG, "convertResultToString(..) caled.");
        	Cursor cursor = (Cursor)resultValue;
            return ((CharSequence) (cursor != null ? cursor.getString(mConvertToStringColumn) : ""));
        }        
    }

    public void changeCursor(Cursor cursor){
    	//Log.v(TAG, "changeCursor(..) caled.");
    	mActivity.stopManagingCursor(mCursor); // Required because of a bug in version 1.0 r1. 
    										   //  When changing the cursor because of a filter clause, the original _mananged_ cursor is closed by changeCurosr(..) but the activity is never informed that it should stop managing the cursor.
    										   //  TODO: Report this problem to Android issues.
    	
    	super.changeCursor(cursor);
    	mCursor = cursor;
    }
    
	 public Filter getFilter() { // TODO: Does this need a try-catch?
     	// Log.v(TAG, "getFilter(..) called.");
		 if( null == mFilter ){
	     	//Log.v(TAG, "null == mFilter");
	     	mFilter = new CursorFilter();
		 }
		 return mFilter;
	 }	

}
