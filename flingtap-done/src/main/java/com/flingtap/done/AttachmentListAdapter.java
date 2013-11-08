// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.flingtap.common.HandledException;
import com.flingtap.done.AttachmentListAdapterDelegate.UriMappings;
import com.flingtap.done.DelegatingListAdapter.DelegateCodeMapping;
import com.flurry.android.FlurryAgent;


import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class AttachmentListAdapter extends ResourceCursorAdapter {
	private static final String TAG = "AttachmentListAdapter";

	protected static final int TAG_ATTACHMENTS_ID_INDEX 	= 0;
	public static final int TAG_ATTACHMENTS_URI_INDEX 		= 1;
	public static final int TAG_LIST_ADAPTER_DELEGATE_INDEX = 2;
	
	protected static final int DEFAULT_MATCH = -1;
	protected static int mapCounter = DEFAULT_MATCH + 1;
	protected static HashMap<Integer, UriMapping> map = new HashMap<Integer, UriMapping>();
	protected UriMatcher URL_MATCHER = new UriMatcher(DEFAULT_MATCH);
	protected HashMap<String, Integer> COMPONENT_NAME_MATCHER = new HashMap<String, Integer>();
	
	private LayoutInflater mInflater = null;
	private Cursor mCursor = null;
	private Context mContext = null;
	
	private boolean actOnIt = true;
	
	protected UriMapping defaultMapping = new UriMapping();
	public class UriMapping{
		public int code;
		public AttachmentListAdapterDelegate delegate;
	}

	public AttachmentListAdapter(Context context, int layout, Cursor cursor) {
		super(context, layout, cursor);
		mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mCursor = cursor;
		mContext = context;
	}

	/**
	 * Finds the ContextListActivityParticipant from within the view tag.
	 */
	public ContextListActivityParticipant getParticipant(Object tag){
		return (ContextListActivityParticipant)((Object[])tag)[AttachmentListAdapter.TAG_LIST_ADAPTER_DELEGATE_INDEX];
	}

	public void bindView(View view, Context context, Cursor cursor) {
		try{
			Uri data = findMappingUri(cursor);
			ComponentName cn = findMappingComponentName(cursor);
			UriMapping mapping = findDelegate(data, cn);
			assert null != mapping;
			bindTag(view, cursor, mapping, data);
			mapping.delegate.bindView(view, context, cursor, mapping.code, data);
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			if( actOnIt ){
				Log.e(TAG, "ERR0000S", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR0000S", exp, context);
				actOnIt = false;
			}
		}	
	}

	public Uri findMappingUri(Cursor cursor) {
		String uriString = cursor.getString(TaskAttachmentListTab.PROJ_ATTACH__URI_INDEX);
		if( null == uriString ){
			return null;
		}
		return Uri.parse( uriString );  
	}

	public ComponentName findMappingComponentName(Cursor cursor) {
		String packageString = cursor.getString(TaskAttachmentListTab.PROJ_ATTACH__PACKAGE_INDEX);
		String classString = cursor.getString(TaskAttachmentListTab.PROJ_ATTACH__CLASS_NAME_INDEX);
		if( null == packageString || null == classString ){
			return null;
		}
		return new ComponentName( packageString, classString );  
	}

	
	public void bindTag(View view, Cursor cursor, UriMapping mapping, Uri data){
		Object[] tagArray = new Object[]{
				cursor.getLong(TaskAttachmentListTab.PROJ_ATTACH_ID_INDEX), // TODO: !! Why is this here? Can't the handler just get the ID from the cursor itself? 
				data,
				mapping.delegate};	
		view.setTag(tagArray);

    	//Log.v(TAG, "tagArray[TAG_ATTACHMENTS_ID_INDEX]=="+tagArray[TAG_ATTACHMENTS_ID_INDEX]);
    	//Log.v(TAG, "tagArray[TAG_ATTACHMENTS_URI_INDEX]=="+tagArray[TAG_ATTACHMENTS_URI_INDEX]);			
	}
	
	public UriMapping findDelegate(Uri data, ComponentName cn) {
		assert (null != data) || (null != cn);
		UriMapping mapping = null;		
		int code = DEFAULT_MATCH;
		if( null != cn ){
			String flattenedContentName = cn.flattenToString();
			Integer codeInteger = COMPONENT_NAME_MATCHER.get( flattenedContentName ); // TODO: How to handle both normal and abbreviated forms?
			code = (null == codeInteger ? DEFAULT_MATCH : codeInteger );
		}
		if( DEFAULT_MATCH == code && null != data ){
			code = URL_MATCHER.match(data);
		}
		if( code == DEFAULT_MATCH ){ 
			return defaultMapping;
		}
		mapping = map.get(code);
		return mapping;
	}

	/**
	 * TODO: Investigate whether this registry code could be moved to service which doesn't have a process boundry.
	 * TODO: If this registration process takes too long, then maybe progress bar or "loading ..." message is appropriate.
	 * @param delegate
	 */
	public void addDelegate(AttachmentListAdapterDelegate delegate){
		UriMapping uriMapping;
		AttachmentListAdapterDelegate.UriMappings[] mappings = delegate.uriMappings;
		for(int i=0; i<mappings.length; i++){
			if( null != mappings[i].componentName ){
				COMPONENT_NAME_MATCHER.put(mappings[i].componentName, mapCounter);
			}else if( null != mappings[i].authority ){ // TODO: Should this also validate the pattern? 
				URL_MATCHER.addURI(mappings[i].authority, mappings[i].pathPattern, mapCounter);
			}else {
				Exception exp = (Exception)(new Exception("Adapter delegate info is incomplete.").fillInStackTrace());
				Log.e(TAG, "ERR0000T Adapter delegate info is incomplete.", exp);
				ErrorUtil.handleException("ERR0000T", exp, mContext);
			}
			uriMapping = new UriMapping();
			uriMapping.code = mappings[i].code;
			uriMapping.delegate = delegate;
			map.put(mapCounter, uriMapping);
			mapCounter++;
		}
	}
	public void setDefaultDelegate(AttachmentListAdapterDelegate delegate){
		assert null != delegate;
		defaultMapping.delegate = delegate; 
	}
	public void addDelegate(ArrayList<AttachmentListAdapterDelegate> delegates){
        Iterator<AttachmentListAdapterDelegate> itr = delegates.iterator();
        while(itr.hasNext()){
        	addDelegate(itr.next());
        }
	}
	
}
