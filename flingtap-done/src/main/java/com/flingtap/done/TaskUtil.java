// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;
import com.flingtap.done.base.R;
import com.flingtap.done.provider.Task;

import java.util.HashMap;
import java.util.Map;

/**
 * Consolidates Task related code.
 * 
 * @author spencer
 *
 */
public class TaskUtil {
	private static final String TAG = "TaskUtil";
	
	private TaskUtil(){}
	
	public static int deleteTaskNoToast(Context context, Uri uri){
		int count = context.getContentResolver().delete(uri, null, null); 
		
		// Prepare event info.
		Map<String,String> parameters = new HashMap<String,String>();
		parameters.put(Event.DELETE_TASKS__NUMBER_OF_TASKS, String.valueOf(count));
		Event.onEvent(Event.DELETE_TASKS, parameters); 

		return count;
	}
	
	public static int deleteTask(Context context, Uri uri){
		int count = deleteTaskNoToast(context, uri); 

		if( 0 < count ){
			Toast.makeText(context, R.string.toast_taskDeleted, Toast.LENGTH_SHORT).show();
		}else{
			Toast.makeText(context, R.string.toast_noTasksDeleted, Toast.LENGTH_SHORT).show(); 
		}
		
		return count;
	}
	
	
	/**
	 * Deletes all completed tasks. 
	 * 
	 */
	public static int deleteCompleted(Context context){
		//Log.v(TAG, "onArchiveRequested() called");
		
		int count = context.getContentResolver().delete(
				Task.Tasks.CONTENT_URI, 
				Task.Tasks.COMPLETE+"=?", 
				new String[]{Task.Tasks.COMPLETE_TRUE});
		
		// Add Delete task event.
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(Event.DELETE_TASKS__NUMBER_OF_TASKS, String.valueOf(count));
		Event.onEvent(Event.DELETE_TASKS, parameters); 		

		return count;
	}
}
