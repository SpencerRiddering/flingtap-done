// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.done.provider.Task;

public class TaskProjection {

	
	/**
	 * Standard projection for the interesting columns of a normal task.
	 */
	public static final String[] PROJECTION = new String[] {
				Task.Tasks._ID, 			// 0
				Task.Tasks.TASK_TITLE, 		// 1
				Task.Tasks.TASK_DESC, 		// 2
				Task.Tasks.MODIFIED_DATE, 	// 3
				Task.Tasks.DUE_DATE, 		// 4
				Task.Tasks.ALARM_TIME, 		// 5
				Task.Tasks.PRIORITY, 		// 6
				Task.Tasks.COMPLETE, 		// 7
		};
	public static final int ID_INDEX = 0;
	public static final int TASK_TITLE_INDEX = 1;
	public static final int TASK_DESC_INDEX = 2;
	public static final int MODIFIED_INDEX = 3;
	public static final int DUE_DATE_INDEX = 4;
	public static final int ALARM_TIME_INDEX = 5;
	public static final int PRIORITY_INDEX = 6;
	public static final int COMPLETE_INDEX = 7;

}
