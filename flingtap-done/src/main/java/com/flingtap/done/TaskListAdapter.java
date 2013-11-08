// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flingtap.done.base.R;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * 
 * 
 */
public class TaskListAdapter extends ResourceCursorAdapter {

	public static final String TAG = "TaskListAdapter";
    private OnCheckedChangeListener taskListCheckBoxListener = null;
    private SharedPreferences settings = null;
    private SimpleDateFormat dateFormat = null;
    private Activity mActivity = null; 
    private Cursor mCursor = null;
    
	public TaskListAdapter(Activity activity, int layout, Cursor c, OnCheckedChangeListener taskListCheckBoxListener) {
		super(activity, layout, c);
		mCursor = c;
		assert mCursor != null;
		
		assert null != taskListCheckBoxListener;
		this.taskListCheckBoxListener = taskListCheckBoxListener;
		
		assert activity != null;
		mActivity = activity;
		
		settings = mActivity.getSharedPreferences(ApplicationPreference.NAME, Activity.MODE_PRIVATE);
		assert null != settings;
		
		char[] order = android.text.format.DateFormat.getDateFormatOrder(activity);
		if( android.text.format.DateFormat.MONTH == order[0]){
			dateFormat = new SimpleDateFormat("M/d"); 
		}else if( android.text.format.DateFormat.DATE == order[0]){
			dateFormat = new SimpleDateFormat("d/M"); 
		}else if( android.text.format.DateFormat.MONTH == order[1]){
			dateFormat = new SimpleDateFormat("M/d"); 
		}else if( android.text.format.DateFormat.DATE == order[1]){
			dateFormat = new SimpleDateFormat("d/M"); 
		}
		
		notCompletedColor = mActivity.getResources().getColorStateList(R.drawable.task_list_item_not_completed_colorlist);
		completedColor = mActivity.getResources().getColorStateList(R.drawable.task_list_item_completed_colorlist);
	}
 
    private TaskListFilter mFilter;
	
    public class TaskListFilter extends Filter {
    	
    	TaskListAdapter mAdapter = null;
    	public int mFilterTextLength = 0;
    	
    	public TaskListFilter(TaskListAdapter adapter){
    		mAdapter = adapter;
    		assert null != mAdapter;
    	}
    	
        protected Filter.FilterResults performFiltering(CharSequence filterText){
        	mFilterTextLength = filterText.length();
        	//Log.v(TAG, "searchText == " + searchText );
        	
        	Filter.FilterResults results = new Filter.FilterResults(); // Just a data object so should be safe.
        	try{
        		//Log.v(TAG, "performFiltering(..) caled.");
        		
        		Cursor cursor = mAdapter.runQueryOnBackgroundThread(filterText);
        		if(cursor != null){
        			results.count = cursor.getCount();
        			results.values = cursor;
        		}else{
        			results.count = 0;
        			results.values = null;
        		}
        	}catch(HandledException h){ // Ignore.
    			results.count = 0;
    			results.values = null;
        	}catch(Exception exp){
        		Log.e(TAG, "ERR0008Z", exp);
        		ErrorUtil.handleExceptionNotifyUser("ERR0008Z", exp, mAdapter.mActivity);
    			results.count = 0;
    			results.values = null;
        	}

            return results;
        }
        
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
        	try{
        		//Log.v(TAG, "publishResults(..) caled.");
        		if(results.values != mCursor)
        			changeCursor((Cursor)results.values);        	
        	}catch(HandledException h){ // Ignore.
        	}catch(Exception exp){
        		Log.e(TAG, "ERR00090", exp);
        		ErrorUtil.handleExceptionNotifyUser("ERR00090", exp, mAdapter.mActivity);
        	}
        }
    }

    public void changeCursor(Cursor cursor){
    	mActivity.stopManagingCursor(mCursor); // Required because of a bug in version 1.0 r1. 
    										   //  When changing the cursor because of a filter clause, the original _mananged_ cursor is closed by changeCurosr(..) but the activity is never informed that it should stop managing the cursor.
    										   //  TODO: Report this problem to Android issues.
    	super.changeCursor(cursor);
    	mCursor = cursor;
    }
    
	 public Filter getFilter() {
		 try{
			 //Log.v(TAG, "getFilter(..) caled.");
			 if( null == mFilter ){
				 mFilter = new TaskListFilter(this);
			 }
		 }catch(HandledException h){ // Ignore.
		 }catch(Exception exp){
		 	Log.e(TAG, "ERR00091", exp);
		 	ErrorUtil.handleExceptionNotifyUser("ERR00091", exp, mActivity);
		 }
		 return mFilter;
	 }

	 private boolean actOnIt = true;
	 private ViewHolder holder;
	 private ColorStateList completedColor ;
	 private ColorStateList notCompletedColor;
	 
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		try{
			if( null != view.getTag() ){
				holder = (ViewHolder)view.getTag();
			}else{
				holder = new ViewHolder();
				holder.taskTitle = (TextView)view.findViewById(R.id.task_title);

				// Task title
				holder.taskTitle = (TextView)view.findViewById(R.id.task_title);
				assert null != holder.taskTitle;
				
				// Task completed
				holder.taskComplete = (CheckBox)view.findViewById(R.id.task_completed);
				assert null != holder.taskComplete;
				holder.taskComplete.setOnCheckedChangeListener(taskListCheckBoxListener);
				
				// Task Priority
				holder.priority = (ImageView)view.findViewById(R.id.icon);
				assert null != holder.priority;
				holder.priority.setImageResource(R.drawable.task_list_item_priority_sped);
				
				holder.dueDate = (TextView)view.findViewById(R.id.task_due);
				assert null != holder.dueDate;

                // *************
                // Set tag
                // *************
				view.setTag(holder);
				holder.taskComplete.setTag(holder);
			}

			// *************
			// Set Title
			// *************
			holder.taskTitle.setText(cursor.getString(TaskList.CURSOR_TASK_TITLE));
			
			// Set text color
			if( cursor.getInt(TaskList.CURSOR_TASK_COMPLETE)==Task.Tasks.COMPLETE_TRUE_INT ){
				holder.taskTitle.setTextColor(completedColor);
			}else{
				holder.taskTitle.setTextColor(notCompletedColor);
			}
			
			if( holder.taskComplete.isChecked() != (cursor.getInt(TaskList.CURSOR_TASK_COMPLETE)==Task.Tasks.COMPLETE_TRUE_INT)){
				// Remove OnCheckChangeListener so that changing the CheckBox doesn't fire an event.			
				holder.taskComplete.setOnCheckedChangeListener(null);

                // Set checked state.
				holder.taskComplete.setChecked(cursor.getInt(TaskList.CURSOR_TASK_COMPLETE)==Task.Tasks.COMPLETE_TRUE_INT); // TODO: Isn't there a more effecient way to compare these values?

				// Add OnCheckedChangeListener 
				holder.taskComplete.setOnCheckedChangeListener(taskListCheckBoxListener);
			}
			
			holder.taskId = cursor.getInt(TaskList.CURSOR_TASK_ID);
			
			// *************
			// Task Priority
			// *************
			if(  cursor.getInt(TaskList.CURSOR_TASK_COMPLETE)==Task.Tasks.COMPLETE_TRUE_INT ){
				holder.priority.setImageLevel(cursor.getInt(TaskList.CURSOR_TASK_PRIORITY)+4);	
			}else{
				holder.priority.setImageLevel(cursor.getInt(TaskList.CURSOR_TASK_PRIORITY));	
			}
			
			// *************
			// Task Due Date
			// *************
			if( cursor.getInt(TaskList.CURSOR_TASK_COMPLETE)==Task.Tasks.COMPLETE_TRUE_INT ){
				holder.dueDate.setTextColor(completedColor);
			}else{
				holder.dueDate.setTextColor(notCompletedColor);
			}
			
			if( cursor.getLong(TaskList.CURSOR_TASK_DUE_DATE) == Task.Tasks.DUE_DATE_NOT_SET_LONG){
				holder.dueDate.setVisibility(View.INVISIBLE);
			}else{
				Date date = new Date(cursor.getLong(TaskList.CURSOR_TASK_DUE_DATE));
				holder.dueDate.setText(dateFormat.format(date));
				holder.dueDate.setVisibility(View.VISIBLE);
			}			

		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			if( actOnIt ){
				Log.e(TAG, "ERR00092", exp);
				ErrorUtil.handleExceptionNotifyUser("ERR00092", exp, mActivity);
				actOnIt = false;
			}
		}
	}
	public static class ViewHolder {
		TextView taskTitle;
		CheckBox taskComplete;
		ImageView priority;
		TextView dueDate;
		long taskId;
	}
}
