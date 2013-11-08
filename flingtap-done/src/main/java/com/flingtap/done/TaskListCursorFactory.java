// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.Activity;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.util.Log;
import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 */
public class TaskListCursorFactory implements android.widget.FilterQueryProvider {
	private static final String TAG = "TaskListCursorFactory";
	
	private Activity mActivity = null;
	private StringBuffer taskIdWhereClause = null; 
	
	public TaskListCursorFactory(Activity activity) {
		mActivity = activity;
	}

	public void setTaskIds(ArrayList<Integer> taskIdList){

		if( (null != taskIdList) && (taskIdList.size() != 0) ){
			taskIdWhereClause = new StringBuffer();

			Iterator<Integer> itr = taskIdList.iterator();
			Integer taskId = null;
			taskIdWhereClause.append(Task.Tasks._ID).append(" IN (");
			while(itr.hasNext()){
				taskId = itr.next();
				taskIdWhereClause.append(taskId);
				if( itr.hasNext() ){
					taskIdWhereClause.append(',');
				}
			}
			taskIdWhereClause.append(')');
		}else{
			taskIdWhereClause = null;
		}
	}
	
	/**
	 * Composes a where clause which is appropriate for searching tasks titles. 
	 * 
	 * @param filterText The text to search for.
	 * @return An appropriate WHERE clause.
	 */
	public static final String composeWhereClause(CharSequence filterText){
		return "("+Task.Tasks.TASK_TITLE + " LIKE " + DatabaseUtils.sqlEscapeString(filterText.toString() + "%") + " OR " + Task.Tasks.TASK_TITLE + " LIKE " + DatabaseUtils.sqlEscapeString("% " + filterText + "%")+")";
	}
	
	/**
	 * @return A managed cursor that includes the items included by the filter or null if a filter is not specified.
	 */
	public Cursor runQuery(CharSequence filterText){
		try{
			StringBuffer where = new StringBuffer();
			
			if( null != taskIdWhereClause ){// Add task id list where clause.
				where.append(taskIdWhereClause);
			}else{ // Apply mask.
				where.append(Task.TasksColumns._FILTER_BIT);
				where.append(" = ");
				where.append(Task.TasksColumns.FILTER_IN);
			}
			if( filterText != null && filterText.length() != 0 ){ 
				where.append( " AND " + composeWhereClause(filterText) );
			}
			
			return mActivity.managedQuery(Task.Tasks.CONTENT_URI, TaskList.PROJECTION, where==null?null:where.toString(), null, Task.Tasks.DEFAULT_SORT_ORDER); 
		}catch(HandledException h){ // Ignore.
		}catch(Exception exp){
			Log.e(TAG, "ERR00093", exp);
			ErrorUtil.handleExceptionNotifyUser("ERR00093", exp, mActivity);
		}
		
		// Return an empty cursor when an error occurs.
		MatrixCursor emptyCursor = new MatrixCursor(TaskList.PROJECTION);
		return emptyCursor;
	}
	
}
