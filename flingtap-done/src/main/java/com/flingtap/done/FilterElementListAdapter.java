// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.done.base.R;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;

/**
 * 
 * @author spencer
 *
 */
public class FilterElementListAdapter extends DelegatingListAdapter {
	private static final String TAG = "FilterElementListAdapter";

	public static final String TAG_FILTER_ELEMENT_ID_INDEX = "FilterElementListAdapter.FILTER_ELEMENT_ID_INDEX";
	public static final String TAG_FILTER_ID_INDEX = "FilterElementListAdapter.FILTER_ID_INDEX";
	
	private Uri mFilterUri = null;
	
	public FilterElementListAdapter(Context context, Cursor cursor, Uri filterUri) {
		super(context, R.layout.label_list_item, cursor);
		mFilterUri = filterUri;
	}

	@Override
	public Uri findMappingUri(Cursor cursor) {
		Uri data = Uri.parse(cursor.getString(FilterElementListActivity.PROJ_FILTER_ELEMENT__CONSTRAINT_INDEX) );
		return data.buildUpon().encodedQuery(cursor.getString(FilterElementListActivity.PROJ_FILTER_ELEMENT__PARAMETERS_INDEX)).build(); // TODO: !! This assumes that the parameters in the database are already encoded.
	}
	
	@Override
	public void bindTag(View view, Cursor cursor, DelegateCodeMapping mapping, Uri data) {
	 	Intent tag = new Intent(); // TODO: !! Consider creating a parceleable object that holds two Uri objects so the filter id can be more naturally represented.
		tag.putExtra(TAG_FILTER_ELEMENT_ID_INDEX, cursor.getLong(FilterElementListActivity.PROJ_FILTER_ELEMENT__ID_INDEX));
		tag.putExtra(TAG_FILTER_ID_INDEX, mFilterUri.getLastPathSegment());
		tag.setData(data);
		
		view.setTag(tag);
	}


	@Override
	public ContextListActivityParticipant getParticipant(Object tag) {
		return findDelegate(((Intent)tag).getData()).delegate;
	}

}
