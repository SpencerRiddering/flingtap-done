// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.Map;

import com.flingtap.common.HandledException;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;


/**
 * Cuts down the busy work required by ResourceCursorAdapter.
 *
 * @author spencer
 */
public class ViewTypeCursorAdapter extends ResourceCursorAdapter {
	private static final String TAG = "ViewTypeCursorAdapter";
    protected int from[];
    protected int to[];
    protected FieldType type[];
    protected int idColumnIndex = 0;
    private boolean actOnIt = true;

   
	public static final class FieldType {
		public final static FieldType STRING 	= new FieldType();
		public final static FieldType LONG 		= new FieldType();
		public final static FieldType INTEGER 	= new FieldType();
		public final static FieldType CURSOR 	= new FieldType();
		private FieldType(){}
	}
	
	public ViewTypeCursorAdapter(Context context, int layout, Cursor c, String from[], int to[], ViewTypeCursorAdapter.FieldType type[]) {
		super(context, layout, c);

		assert null != c;

        this.to = to;
        this.type = type;        
        this.idColumnIndex = c.getColumnIndex("_id");        
        // Translate the column name to the index.
        int length = from.length;
        this.from = new int[length];
        for(int i=0; i < length; i++){
            this.from[i] = c.getColumnIndex(from[i]);
        }
	}

	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2) {
		View v = super.getView(arg0, arg1, arg2);
		return v;
	}

	/**
	 * Get values from cursor and delegate setting TextView to setViewText().
	 * 
	 * Cursor field type is preserved.
	 * 
	 * TODO: type[i] should be calculated once.
	 * TODO: Implement View holder pattern.
	 */
	public void bindView(View view, Context context, Cursor cursor) {
		try{
			initView(view, cursor);
			initView(view, context, cursor);
			
			int rowId = cursor.getInt(idColumnIndex);
			for(int i=0; i < to.length; i++){
				View childView = view.findViewById(to[i]);
				if(null == childView){
					continue;
				}
				if(FieldType.STRING == type[i]){
					String text = cursor.getString(from[i]);
					setViewData(childView, text, rowId);            
				}else if(FieldType.LONG == type[i]){
					long value = cursor.getLong(from[i]);
					setViewData(childView, value, rowId);            	
				}else if(FieldType.INTEGER == type[i]){
					int integer = cursor.getInt(from[i]);
					setViewData(childView, integer, rowId); // cursor.position()            	
				}else if(FieldType.CURSOR == type[i]){
					setViewData(childView, cursor, rowId);          	
				}
			}
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			if( actOnIt ){
				Log.e(TAG, "ERR0009M", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR0009M", exp, context);
				actOnIt = false;
			}
		}
	}

	/**
	 * Called once just prior to a new/recycled view being bound.
	 * @deprecated Use initView(View view, Context context, Cursor cursor) instead.
	 * @param view
	 */
    public void initView(View view, Cursor cursor){
    	
    }
    
    
	/**
	 * Called once just prior to a new/recycled view being bound.
	 */
    public void initView(View view, Context context, Cursor cursor){
    	
    }

    /**
     * Override to perform any custom transformations on STRING database fields. 
     * 
     * @param view
     * @param value
     * @param rowId
     * @override
     */
    public void setViewData(View view, CharSequence value, int rowId){
        if(null == value){
        	value = "";
        }
        ((TextView)view).setText(value);
    }
 
    /**
     * Override to perform any custom transformations on INTEGER database fields. 
     * 
     * @param view
     * @param value
     * @param rowId
     * @override
     */
    public void setViewData(View view, long value, int rowId){
    	((TextView)view).setText(String.valueOf(value));
    }    
    
    /**
     * Override to perform any custom transformations on INTEGER database fields. 
     * 
     * @param view
     * @param value
     * @param rowId
     * @override
     */
    public void setViewData(View view, int value, int rowId){
    	((TextView)view).setText(String.valueOf(value));
    }
    
    /**
     * Override to perform any custom transformations on STRING database fields. 
     * 
     * @param view 
     * @param rowId
     * @override
     */
    public void setViewData(View view, Cursor cursor, int rowId){
    	// No default behavior
    }    
}
