// Licensed under the Apache License, Version 2.0

package com.flingtap.done;


import java.util.HashMap;
import java.util.Map;

import com.flingtap.done.provider.Task;
import com.flurry.android.FlurryAgent;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.flingtap.done.base.R;
/**
 * Collection of logic for the Archive Menu.
 * 
 * @author spencer
 *
 */
public class ArchiveMenuPart 
		//extends AbstractContextListActivityParticipant 
		{
	private static final String TAG = "ArchiveMenuPart";
	
	protected static void archiveTask(Context context, long taskId){
		int count = ArchiveUtil.archiveTask(context, taskId);

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(Event.ARCHIVE_TASKS__NUMBER_OF_TASKS, String.valueOf(count));
		Event.onEvent(Event.ARCHIVE_TASKS, parameters); 
		
		if( 0 < count ){
			Toast.makeText(context, R.string.toast_taskArchived, Toast.LENGTH_LONG).show();
		}else{
			Toast.makeText(context, R.string.toast_nothingArchived, Toast.LENGTH_LONG).show();
		}
		// Update the filter bits.
		FilterUtil.applyFilterBits(context, ContentUris.appendId(Task.Tasks.CONTENT_URI.buildUpon(), taskId).build()); 
	}
	protected static void unarchiveTask(Context context, long taskId){
		int count = ArchiveUtil.unarchiveTask(context, taskId);
		
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(Event.UNARCHIVE_TASKS__NUMBER_OF_TASKS, String.valueOf(count));
		Event.onEvent(Event.UNARCHIVE_TASKS, parameters); 
		
		if( 0 < count ){
			Toast.makeText(context, R.string.toast_unarchivedTask, Toast.LENGTH_LONG).show();
		}else{
			Toast.makeText(context, R.string.toast_noTasksUnarchived, Toast.LENGTH_LONG).show();
		}
		
		// Update the filter bits.
		FilterUtil.applyFilterBits(context, ContentUris.appendId(Task.Tasks.CONTENT_URI.buildUpon(), taskId).build()); 
	}
	
	public static void archiveCompletedTasks(Context context){
		int count = ArchiveUtil.archiveCompletedTasks(context);
		
		if( 0 < count ){
			Toast.makeText(context, TextUtils.expandTemplate(context.getText(R.string.toast_archivedNTasks), String.valueOf(count)) , Toast.LENGTH_LONG).show();// TODO: !! Export string
		}else{
			Toast.makeText(context, R.string.toast_noTasksArchived, Toast.LENGTH_LONG).show();// TODO: !! Export string
		}
		
		// Update the filter bits.
		FilterUtil.applyFilterBits(context); 
	}
	
}
