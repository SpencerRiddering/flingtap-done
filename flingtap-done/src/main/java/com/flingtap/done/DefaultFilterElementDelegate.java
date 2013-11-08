// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.done.provider.Task;
import com.flingtap.done.base.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriMatcher;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * 
 * 
 * @author spencer
 *
 */
public class DefaultFilterElementDelegate 
		extends FilterElementDelegatingListAdapterDelegate 
		implements  View.OnCreateContextMenuListener{

	public static final String TAG = "DefaultFilterElementDelegate";

	private final static int FIRST_CODE_ID = 1100;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}
	
	// Item IDs
	protected final static int LABEL_MENU_REMOVE_ITEM_ID = FIRST_CODE_ID + 10;
	protected final static int LABEL_MENU_RENAME_ITEM_ID = FIRST_CODE_ID + 11;
	protected final static int LABEL_MENU_VIEW_ITEM_ID   = FIRST_CODE_ID + 12;
	
	// Result Codes
	protected final static int DELETE_LABEL_REQUEST      = FIRST_CODE_ID + 98;

	// Dialog IDs
	protected final static int SELECT_DUE_DATE_DIALOG_ID = FIRST_CODE_ID + 50;
	
	public DefaultFilterElementDelegate(){
	}
	
	@Override
	protected void bindView(final View view, Context context, Cursor cursor,
			int code, Uri data) {
		
		setupErrorView(mActivity, view);
	}

	public static void setupErrorView(Context context, final View view) {
		String name = context.getText(R.string.error).toString(); 
		TextView singleLineText = (TextView) view.findViewById(R.id.label_list_item_single_line_text);
		singleLineText.setVisibility(View.VISIBLE);
		singleLineText.setText(name);

		// Hide dual line layout is gone.
		view.findViewById(R.id.label_list_item_dual_line_text).setVisibility(View.GONE);
		// Hide toggle button.
		view.findViewById(R.id.label_list_item_toggle_button_layout).setVisibility(View.GONE);
		// Hide header title.
		view.findViewById(R.id.header_title).setVisibility(View.GONE);
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo){
    	// Log.v(TAG, "onCreateContextMenu(..) called.");
	}

	/**
	 * 
	 */
	public void onListItemClick(ListView listview, View view, int position, long id) {
		// Log.v(TAG, "onListItemClick(..) called");
	}

	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		Log.v(TAG, "onActivityResult(..) called");
		if( resultCode == SharedConstant.RESULT_ERROR ){
			ErrorUtil.notifyUser(mActivity);
			return;
		}

	}

	public boolean onContextItemSelected(MenuItem item) {
        return false;
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		return false;
	}

	protected static class SelectDueDateStruct {
		
	}
	
	public Dialog onCreateDialog(int dialogId){
		return null;
	}

	public void onPrepareDialog(int dialogId, Dialog dialog){
	}

	public void  onSaveInstanceState  (Bundle outState){
	}
	
	public void  onRestoreInstanceState  (Bundle savedInstanceState){
	}
}
