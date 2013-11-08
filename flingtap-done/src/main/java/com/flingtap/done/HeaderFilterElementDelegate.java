// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import com.flingtap.done.provider.Task;
import com.flingtap.done.util.Constants;
import com.flurry.android.FlurryAgent;
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
import android.database.DatabaseUtils;
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
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.RadioGroup.OnCheckedChangeListener;


/**
 * Handles displaying header records.
 *
 * 
 */
public class HeaderFilterElementDelegate 
		extends FilterElementDelegatingListAdapterDelegate {

	private static final String TAG = "HeaderFilterElementDelegate";
	
	private final static int FIRST_CODE_ID = 2700;
	public int getFirstCodeId() {
		return FIRST_CODE_ID;
	}
	
	public HeaderFilterElementDelegate(){
		uriDelegateMapping = new UriDelegateMapping[1];
		uriDelegateMapping[0] = new UriDelegateMapping();
		uriDelegateMapping[0].authority = Task.AUTHORITY;
		uriDelegateMapping[0].pathPattern = "cosmetic/header_row/*";
		uriDelegateMapping[0].code = 0; // Uniquely identifies this mapping. Some Attachment handlers may handle multiple different mime-types so this allows us to distinguish between them. The value is passed into bindView(..)
//		// TODO: Couldn't the array index be used instead of adding the .code member? 
	}
	
	@Override
	protected void bindView(final View view, Context context, Cursor cursor,
			int code, Uri data) {

		String uriString = cursor.getString(FilterElementListActivity.PROJ_FILTER_ELEMENT__CONSTRAINT_INDEX);
		assert null != uriString;
		Uri uri = Uri.parse(uriString);
		String text = uri.getLastPathSegment();

		TextView title = (TextView)view.findViewById(R.id.header_title);
		title.setVisibility(View.VISIBLE);
		title.setText(text);
		
		// Ensure the toggle button is gone.
		view.findViewById(R.id.label_list_item_toggle_button_layout).setVisibility(View.GONE);
		view.findViewById(R.id.label_list_item_dual_line_text).setVisibility(View.GONE);
		view.findViewById(R.id.label_list_item_single_line_text).setVisibility(View.GONE);
	}
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo){
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
	
	
	public Dialog onCreateDialog(int dialogId){
		return null;
	}

	public void onPrepareDialog(int dialogId, Dialog dialog){
	}
	
	public void  onSaveInstanceState  (Bundle outState){
	}
	
	public void  onRestoreInstanceState  (Bundle savedInstanceState){
	}

	public void onListItemClick(ListView listview, View view, int position, long id) {
	}

}
