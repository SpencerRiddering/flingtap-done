package com.flingtap.done.base.test;

import android.test.ActivityInstrumentationTestCase2;
import com.flingtap.done.TaskList;

public class TaskListTest extends ActivityInstrumentationTestCase2<TaskList> {

    public TaskListTest() {
        super("com.flingtap.done", TaskList.class);
    }

    public void testActivity() {
        TaskList activity = getActivity();
        assertNotNull(activity);
    }
}

