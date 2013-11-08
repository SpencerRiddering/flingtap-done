// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.flingtap.common.HandledException;

import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ResourceCursorAdapter;

/**
 * 
 * 
 * @author spencer
 *
 */
public abstract class DelegatingListAdapter extends ResourceCursorAdapter {
	private static final String TAG = "DelegatingListAdapter";
	
	protected static final int DEFAULT_MATCH = -1;
	protected static int mapCounter = DEFAULT_MATCH + 1;
	protected static HashMap<Integer, DelegateCodeMapping> map = new HashMap<Integer, DelegateCodeMapping>();
	protected UriMatcher URL_MATCHER = new UriMatcher(DEFAULT_MATCH);
	private long lastTimeUserNotified = 0;
	
	protected DelegateCodeMapping defaultCodeMapping = new DelegateCodeMapping();
	public class DelegateCodeMapping{
		public int code;
		public DelegatingListAdapterDelegate delegate;
	}

	public DelegatingListAdapter(Context context, int layout, Cursor cursor) {
		super(context, layout, cursor);
	}

	/**
	 * Finds the ContextListActivityParticipant from within the view tag.
	 */
	public abstract ContextListActivityParticipant getParticipant(Object tag);
	
	public void bindView(View view, Context context, Cursor cursor) {	
		try{
			Uri data = findMappingUri(cursor);
			DelegateCodeMapping mapping = findDelegate(data);
			assert null != mapping;
			bindTag(view, cursor, mapping, data);
			mapping.delegate.bindView(view, context, cursor, mapping.code, data);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR0001Q", exp);
			lastTimeUserNotified = ErrorUtil.handleExceptionNotifyUser("ERR0001Q", exp, context, lastTimeUserNotified, 1000 * 20);
		}
	}

	/**
	 * 
	 */
	public abstract void bindTag(View view, Cursor cursor, DelegateCodeMapping mapping, Uri data);
	
	/**
	 * 
	 */
	public abstract Uri findMappingUri(Cursor cursor);
	
	public DelegateCodeMapping findDelegate(Uri data) {
		int code = URL_MATCHER.match(data);
		if( code == DEFAULT_MATCH ){ 
			return defaultCodeMapping;
		}
		DelegateCodeMapping mapping = map.get(code);
		return mapping;
	}

	/**
	 * TODO: Investigate whether this registry code could be moved to service which doesn't have a process boundry.
	 * TODO: If this registration process takes too long, then maybe progress bar or "loading ..." message is appropriate.
	 * @param delegate
	 */
	public void addDelegate(DelegatingListAdapterDelegate delegate){
		DelegateCodeMapping delegateCodeMapping;
		DelegatingListAdapterDelegate.UriDelegateMapping[] mappings = delegate.uriDelegateMapping;
		for(int i=0; i<mappings.length; i++){
			URL_MATCHER.addURI(mappings[i].authority, mappings[i].pathPattern, mapCounter);
			delegateCodeMapping = new DelegateCodeMapping();
			delegateCodeMapping.code = mappings[i].code;
			delegateCodeMapping.delegate = delegate;
			map.put(mapCounter, delegateCodeMapping);
			mapCounter++;
		}
	}
	public void setDefaultDelegate(DelegatingListAdapterDelegate delegate){
		assert null != delegate;
		defaultCodeMapping.delegate = delegate; 
	}
	public void addDelegate(ArrayList<DelegatingListAdapterDelegate> delegates){
        Iterator<DelegatingListAdapterDelegate> itr = delegates.iterator();
        while(itr.hasNext()){
        	addDelegate(itr.next());
        }
	}
	
}
