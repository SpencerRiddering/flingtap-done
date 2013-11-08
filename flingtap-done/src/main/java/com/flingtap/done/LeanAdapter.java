// Licensed under the Apache License, Version 2.0

package com.flingtap.done;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * 
 * @author spencer
 * 
 * @param <T>
 */
public abstract class LeanAdapter <T> extends BaseAdapter {

	private static final String TAG = "LeanAdapter";
	
	protected int mResource;
    protected Context mContext;
    protected LayoutInflater mInflate;	
	
    public LeanAdapter(Context context, int resource){
        mContext = context;
        mResource = resource;
        mInflate = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
	
	
	abstract public long getItemId(int position);

	public abstract T getItem(int position);
	
	public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        
        if(convertView == null){
        	v = newView(mContext, position, getItem(position), parent, mResource);
        }else{
        	v = convertView;
        	//Log.v(TAG, "getView(..) returned convertView.");	
        }
        bindView(v, mContext, getItem(position), position);
        return v;
	}

    public View newView(Context context, int position, T data, ViewGroup viewgroup, int resource){
    	//Log.v(TAG, "newView(..) called.");	
		View v = mInflate.inflate(resource, viewgroup, false);
		return v;
    }

	/**
	 * Get values from cursor and delegate setting TextView to setViewText().
	 * 
	 */
	public abstract void bindView(View view, Context context, T data, int position);
    
}
