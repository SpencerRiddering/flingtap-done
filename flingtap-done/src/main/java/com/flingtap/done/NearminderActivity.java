// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.HashMap;
import java.util.Map;

import com.flingtap.common.HandledException;
import com.flingtap.done.provider.Task;
import com.flurry.android.FlurryAgent;
import com.google.android.maps.GeoPoint;
import com.flingtap.done.base.R;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

/**
 * 
 */
public class NearminderActivity extends CoordinatedActivity {
	public static final String TAG = "NearminderActivity";

	private static final int SELECT_AREA_REQUEST = 1;
	private Uri mUri = null;
	private String mDefaultName = null;

	public static final String EXTRA_SELECTED_URI = "com.flingtap.done.extra.SELECTED_URI";
	public static final String EXTRA_GLOBAL_POSITION = SelectAreaActivity.EXTRA_GLOBAL_POSITION;
	public static final String EXTRA_DEFAULT_NAME = "com.flingtap.done.extra.DEFAULT_NAME";

	private Uri mSelectedUri = null;

	// The different distinct states the activity can be run in.
	public static final int STATE_EDIT = 0;
	public static final int STATE_CREATE_SHORTCUT = 1; // TODO: Consider whether
														// this state should be
														// replaced with
														// STATE_INSERT. Using
														// STATE_INSERT would
														// not prevent you from
														// returning the
														// shortcut extra
														// values.

	private int mState = STATE_EDIT;

	private static final int SET_NEARMINDER_NAME_DIALOG_ID = 58;

	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		try {
			// Log.v(TAG, "onCreate(..) called.");

			Intent intent = getIntent();
			assert null != intent;

			if (Intent.ACTION_CREATE_SHORTCUT.equals(intent.getAction())) {
				mState = STATE_CREATE_SHORTCUT;
				mSelectedUri = (Uri) intent.getParcelableExtra(EXTRA_SELECTED_URI);
				mDefaultName = (String) intent.getStringExtra(EXTRA_DEFAULT_NAME);

				if (null != icicle) {
					mResultData = icicle.getParcelable(SAVE_RESULT_DATA);
					return;
				}
			} else if (Intent.ACTION_EDIT.equals(intent.getAction())) {
				mState = STATE_EDIT;
				mUri = intent.getData();
				assert null != mUri;
				if (!mUri.toString().startsWith(Task.ProximityAlerts.CONTENT_URI.toString())) {
					Log.e(TAG, "ERR0003V Invalid URI ");
					ErrorUtil.handleExceptionFinish("ERR0003V", (Exception)(new Exception( mUri.toString() )).fillInStackTrace(), this);
					return;
				}
				// Prepare event info.
				Event.onEvent(Event.EDIT_NEARMINDER, null);

			} else if (Intent.ACTION_DELETE.equals(intent.getAction())) {
				assert null != intent.getData();
				assert intent.getData().toString().startsWith(Task.ProximityAlerts.CONTENT_URI_STRING);

				if (null != intent.getData() && intent.getData().toString().startsWith(Task.ProximityAlerts.CONTENT_URI_STRING)) {
					if (0 < getContentResolver().delete(intent.getData(), null, null)) {
						// Prepare event info.
						Event.onEvent(Event.DELETE_NEARMINDER, null);
					}
				} else {
					Log.e(TAG, "ERR0003U Invalid URI " );
					ErrorUtil.handleExceptionFinish("ERR0003U", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
					return;
				}

				finish();
				return;
			} else {
				Log.e(TAG, "ERR0003W Unknown action " + intent.getAction());
				ErrorUtil.handleExceptionFinish("ERR0003W", (Exception)(new Exception( intent.toURI() )).fillInStackTrace(), this);
				return;
			}

			Intent selectAreaIntent = new Intent();
			ComponentName cn = new ComponentName(this, SelectAreaActivity.class); // "com.flingtap.done.SelectAreaActivity"
			selectAreaIntent.setComponent(cn);

			if (mState == STATE_EDIT) {
				Cursor proxCursor = getContentResolver().query(mUri, // Task.ProximityAlerts.CONTENT_URI item.
						new String[] { Task.ProximityAlerts._GEO_URI, Task.ProximityAlerts._ZOOM_LEVEL, Task.ProximityAlerts.RADIUS, Task.ProximityAlerts._SELECTED_URI,
						}, null, null, null);
				if (!proxCursor.moveToFirst()) {
					Log.e(TAG, "ERR0003X URI not found in DB. " + mUri.toString());
					ErrorUtil.handleExceptionFinish("ERR0003X", (Exception)(new Exception( mUri.toString() )).fillInStackTrace(), this);
					return;
				}

				GeoPoint geoPoint = Util.createPoint(proxCursor.getString(0));

				if (proxCursor.isNull(3)) {
					selectAreaIntent.putExtra(SelectAreaActivity.EXTRA_GLOBAL_POSITION, new ParcelableGeoPoint(geoPoint));
				} else {
					selectAreaIntent.putExtra(SelectAreaActivity.EXTRA_FIXED_POSITION, new ParcelableGeoPoint(geoPoint));
				}
				selectAreaIntent.putExtra(SelectAreaActivity.EXTRA_ZOOM, proxCursor.getInt(1));
				selectAreaIntent.putExtra(SelectAreaActivity.EXTRA_RADIUS, proxCursor.getInt(2));
				proxCursor.close();
			} else if (mState == STATE_CREATE_SHORTCUT) {
				Parcelable parcelable = getIntent().getParcelableExtra(EXTRA_GLOBAL_POSITION);
				if (null != parcelable) {
					selectAreaIntent.putExtra(SelectAreaActivity.EXTRA_FIXED_POSITION, parcelable);
				}
			}

			startActivityForResult(selectAreaIntent, SELECT_AREA_REQUEST);

		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR0003M", exp);
			ErrorUtil.handleExceptionFinish("ERR0003M", exp, this);
		}

	}

	private TextEntryDialog.OnTextSetListener listener = new TextEntryDialog.OnTextSetListener() {

		public void onTextSet(CharSequence name) {
			try {
				if( name.length() == 0 ){ 
					Toast.makeText(NearminderActivity.this, R.string.toast_pleaseEnterAName, Toast.LENGTH_SHORT).show(); 
					finish();
					return;
				}

				ParcelableGeoPoint geoPoint = (ParcelableGeoPoint) mResultData.getParcelableExtra(SelectAreaActivity.EXTRA_GLOBAL_POSITION);
				int radius = mResultData.getIntExtra(SelectAreaActivity.EXTRA_RADIUS, -1);
				if (-1 == radius) {
					// SelectAreaActivity.EXTRA_RADIUS did not contain a value
					// in result
					Log.e(TAG, "ERR00040 Response contained no radius");
					ErrorUtil.handleExceptionFinish("ERR00040", (Exception)(new Exception( mResultData.toURI() )).fillInStackTrace(), NearminderActivity.this);
					return;
				}

				int zoomLevel = mResultData.getIntExtra(SelectAreaActivity.EXTRA_ZOOM, -1);
				if (-1 == zoomLevel) {
					// SelectAreaActivity.EXTRA_ZOOM did not contain a value in
					// result.
					Log.e(TAG, "ERR00041 Response contained no zoom");
					ErrorUtil.handleExceptionFinish("ERR00041", (Exception)(new Exception( mResultData.toURI() )).fillInStackTrace(), NearminderActivity.this);
					return;
				}

				// TODO: Use SelectAreaActivity response to create a proximity
				// alert.
				Uri proximityAlertUri = Nearminder.insert(NearminderActivity.this, geoPoint, radius, zoomLevel, mSelectedUri);
				if (null == proximityAlertUri) {
					// SelectAreaActivity.EXTRA_ZOOM did not contain a value in
					// result.
					Log.e(TAG, "ERR00042 Failed to insert Nearminder.");
					ErrorUtil.handleExceptionFinish("ERR00042", (Exception)(new Exception( mResultData.toURI() )).fillInStackTrace(), NearminderActivity.this);
					return;
				}

				Intent respIntent = new Intent();

				// Return data to caller so that caller can add the attachment.
				Intent viewProximityAlert = new Intent(Intent.ACTION_VIEW, proximityAlertUri);
				Intent.ShortcutIconResource iconRes = Intent.ShortcutIconResource.fromContext(NearminderActivity.this, R.drawable.ic_launcher_nearminder);

				respIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);
				respIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, viewProximityAlert);
				respIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name.toString());
				respIntent.putExtra(PickAttachmentPart.EXTRA_DELETE_INTENT, new Intent(Intent.ACTION_DELETE, proximityAlertUri));
				setResult(RESULT_OK, respIntent);

				//**************************************************************
				// *************
				// Attachment will be created from EXTRA_SHORTCUT_* data in
				// result intent
				//**************************************************************
				// *************

				finish();

			} catch (HandledException h) { // Ignore.
			} catch (Exception exp) {
				Log.e(TAG, "ERR0003N", exp);
				ErrorUtil.handleExceptionFinish("ERR0003N", exp, NearminderActivity.this);
			}

		}

		public void onCancel() {
			try {
				//Log.d(TAG, "User cancelled the content namming part.");
				setResult(RESULT_CANCELED);
				finish();
			} catch (HandledException h) { // Ignore.
			} catch (Exception exp) {
				Log.e(TAG, "ERR0003P", exp);
				ErrorUtil.handleExceptionFinish("ERR0003P", exp, NearminderActivity.this);
			}
			return;
		}
	};

	private Intent mResultData = null;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		try {
			//Log.v(TAG, "onActivityResult(..) called.");
			if (Activity.RESULT_CANCELED == resultCode) {
				// No need to toast here because PickAttachmentPart will do it
				// for us.
				setResult(RESULT_CANCELED);
				finish();
				return;
			}
			if (SharedConstant.RESULT_ERROR == resultCode) {
				setResult(SharedConstant.RESULT_ERROR);
				finish();
				return;
			}
			switch (requestCode) {
				case SELECT_AREA_REQUEST:
					mResultData = data;

					if (mState == STATE_CREATE_SHORTCUT) {
						showDialog(SET_NEARMINDER_NAME_DIALOG_ID);
					} else if (mState == STATE_EDIT) {

						ParcelableGeoPoint geoPoint = (ParcelableGeoPoint) mResultData.getParcelableExtra(SelectAreaActivity.EXTRA_GLOBAL_POSITION);
						int radius = mResultData.getIntExtra(SelectAreaActivity.EXTRA_RADIUS, -1);
						if (-1 == radius) {
							// SelectAreaActivity.EXTRA_RADIUS did not contain a
							// value in result
							Log.e(TAG, "ERR0003Q Response contained no radius");
							ErrorUtil.handleExceptionFinish("ERR0003Q", (Exception)(new Exception( mResultData.toURI() )).fillInStackTrace(), this);
							return;
						}
						int zoomLevel = mResultData.getIntExtra(SelectAreaActivity.EXTRA_ZOOM, -1);
						if (-1 == zoomLevel) {
							// SelectAreaActivity.EXTRA_ZOOM did not contain a
							// value in result.
							Log.e(TAG, "ERR0003R Response contained no zoom");
							ErrorUtil.handleExceptionFinish("ERR0003R",(Exception)(new Exception( mResultData.toURI() )).fillInStackTrace(), this);
							return;
						}

						int updateCount = Nearminder.update(NearminderActivity.this, mUri, geoPoint, radius, zoomLevel, mSelectedUri);
						if (1 != updateCount) {
							// SelectAreaActivity.EXTRA_ZOOM did not contain a
							// value in result.
							Log.e(TAG, "ERR0003S getContentResolver().update(mUri, cv, null, null) returned " + updateCount + " instead of one as expected.");
							ErrorUtil.handleExceptionFinish("ERR0003S", (Exception)(new Exception( String.valueOf( updateCount ) )).fillInStackTrace() , this);
							return;
						}

						//******************************************************
						// *********************
						// Attachment will be created from EXTRA_SHORTCUT_* data
						// in result intent
						//******************************************************
						// *********************

						setResult(RESULT_OK);
						finish();

					}
					break;
				default:
					setResult(RESULT_CANCELED);
					finish();
			}
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR0003O", exp);
			ErrorUtil.handleExceptionFinish("ERR0003O", exp, this);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		try {
			Dialog parentDialog = super.onCreateDialog(id);
			if (null != parentDialog) {
				return parentDialog;
			}
			switch (id) {
				case SET_NEARMINDER_NAME_DIALOG_ID: {
					return TextEntryDialog.onCreateDialog(this, listener, getText(R.string.dialog_setNearminderName), null, getDefaultName()); 
				}
			}

		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR000BO", exp);
			ErrorUtil.handleExceptionFinish("ERR000BO", exp, this);
		}
		return null;
	}

	private CharSequence getDefaultName() {
		return null == mDefaultName ? "" : mDefaultName;
	}

	@Override
	protected void onPrepareDialog(int dialogId, Dialog dialog) {
		super.onPrepareDialog(dialogId, dialog);
		try {
			TextEntryDialog.onPrepareDialog(this, dialog, getDefaultName());
		} catch (HandledException h) { // Ignore.
		} catch (Exception exp) {
			Log.e(TAG, "ERR000BP", exp);
			ErrorUtil.handleExceptionFinish("ERR000BP", exp, this);
		}
	}

	private static final String SAVE_DEFAULT_NAME = "SAVE_DEFAULT_NAME";
	private static final String SAVE_RESULT_DATA = "SAVE_RESULT_DATA";

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		try {
			outState.putString(SAVE_DEFAULT_NAME, mDefaultName);
			outState.putParcelable(SAVE_RESULT_DATA, mResultData);
		} catch (Exception exp) {
			Log.e(TAG, "ERR000BQ", exp);
			ErrorUtil.handleExceptionFinish("ERR000BQ", exp, this);
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		try {
			mDefaultName = savedInstanceState.getString(SAVE_DEFAULT_NAME);
		} catch (Exception exp) {
			Log.e(TAG, "ERR000BR", exp);
			ErrorUtil.handleExceptionFinish("ERR000BR", exp, this);
		}

	}
}
