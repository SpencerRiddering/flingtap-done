// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.flingtap.done.provider.Task;
import com.flingtap.done.base.R;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * 
 * @author spencer
 *
 */
public class FeedbackPart extends AbstractContextActivityParticipant {
	private static final String TAG = "FeedbackPart";
	
	public static boolean addOptionsMenuItem(final Context context, Menu menu, int menuId, int order){
		menu.removeItem(menuId);
		MenuItem menuItemSearch = menu.add(Menu.NONE, menuId, order, R.string.option_feedback);
		return true;
	}
	
	public static void onOptionsItemSelected(Context context){
		launchFeedbackDialog(context, null);
	}

	protected static Uri createAttachment(Context context){
		return createAttachment(context, null);
	}
	
	static void launchFeedbackDialog(Context context, Throwable throwable) {
		Event.onEvent(Event.SEND_FEEDBACK, null); 
		
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		
	    String[] mailto = { StaticConfig.USER_FEEDBACK_EMAIL_ADDRESS };
	    sendIntent.putExtra(Intent.EXTRA_EMAIL, mailto);				
		sendIntent.setType("text/html");
        Uri uri = createAttachment(context);
        Log.v(TAG, uri.toString());
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        context.startActivity(Intent.createChooser(sendIntent, context.getText(R.string.chooser_selectEmailClient)));
	}

	/**
	 * @param throwable
	 * @return
	 */
	protected static Uri createAttachment(Context context, Throwable throwable){

		StringBuffer text = new StringBuffer();
		text.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		text.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" dir=\"ltr\" lang=\"en\">");
		text.append("<body>");

		text.append("<h3>-- Basic --</h3>");
		// Add date and time
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss Z");
		text.append("<p><span style=\"font-weight:bold;\">DATE_TIME:</span> " + simpleDateFormat.format(new Date())+"</p>");
		
		// Add system locale.
		text.append("<p><span style=\"font-weight:bold;\">DEFAULT_LOCALE:</span> " + Locale.getDefault()+"</p>");
		
		// Add position
		LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setCostAllowed(false);
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		if( null != lm){
			String locProviderName = lm.getBestProvider(criteria, true);
			if( null != locProviderName){
				Location loc = lm.getLastKnownLocation(locProviderName);
				if( null != loc ){
					text.append("<p><span style=\"font-weight:bold;\">LOCATION:</span> " + loc.getLatitude() +","+ loc.getLongitude()+" from provider \"" + loc.getProvider() + "\".</p>");
					text.append("<p><span style=\"font-weight:bold;\">LOCATION_GOOGLE_MAPS:</span> http://maps.google.com/maps?q=" + loc.getLatitude() +","+ loc.getLongitude()+"&z=4</p>");
				}else{
					text.append("<p><span style=\"font-weight:bold;\">LOCATION:</span> Unknown</p>");
				}
			}else{
				text.append("<p><span style=\"font-weight:bold;\">LOCATION:</span> Unknown</p>");
			}
		}else{
			text.append("<p><span style=\"font-weight:bold;\">LOCATION:</span> Unknown</p>");
		}
		
		
		// ****************
		// Add System Info
		// ****************

		text.append("\n");
		text.append("<h3>-- Application Info --</h3>");
		// Add Current Version Number.
		String versionName = "Unknown";
		int versionCode = 0;
		String applicationName = "Unknown";
		try {
			PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			versionName = pi.versionName;
			versionCode = pi.versionCode;
			applicationName = context.getString(pi.applicationInfo.labelRes);
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "ERR0002X Package name not found", e);
			ErrorUtil.handleException("ERR0002X", e, context);
		};
		text.append("<p><span style=\"font-weight:bold;\">APP_NAME:</span> " + applicationName+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">APP_VERSION_NAME:</span> " + versionName+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">APP_VERSION_CODE:</span> " + (versionCode==0?"Unknown":versionCode)+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">APP_CACHE_DIR:</span> " + context.getCacheDir()+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">APP_FILES_DIR:</span> " + context.getFilesDir()+"</p>");
		if( Integer.valueOf(Build.VERSION.SDK) < 11 ){
			text.append("<p><span style=\"font-weight:bold;\">APP_INSTANCE_COUNT:</span> " + Activity.getInstanceCount()+"</p>");
		}
		
		Cursor infoCursor = context.getContentResolver().query(Uri.parse("content://"+Task.AUTHORITY+"/db_info/version") , null, null, null, null);
		infoCursor.moveToFirst();
		text.append("<p><span style=\"font-weight:bold;\">APP_DATABASE_VERSION:</span> " + infoCursor.getInt(0)+"</p>"); 
		infoCursor.close();
		
		infoCursor = context.getContentResolver().query(Uri.parse("content://"+Task.AUTHORITY+"/db_info/file_size") , null, null, null, null);
		infoCursor.moveToFirst();
		text.append("<p><span style=\"font-weight:bold;\">APP_DATABASE_FILE_SIZE:</span> " + infoCursor.getLong(0)+"</p>"); 
		infoCursor.close();
		
		infoCursor = context.getContentResolver().query(Uri.parse("content://"+Task.AUTHORITY+"/db_info/file_path") , null, null, null, null);
		infoCursor.moveToFirst();
		text.append("<p><span style=\"font-weight:bold;\">APP_DATABASE_FILE_PATH:</span> " + infoCursor.getString(0)+"</p>"); 
		infoCursor.close();
		
		infoCursor = context.getContentResolver().query(Uri.parse("content://"+Task.AUTHORITY+"/db_info/max_size") , null, null, null, null);
		infoCursor.moveToFirst();
		text.append("<p><span style=\"font-weight:bold;\">APP_DATABASE_MAX_SIZE:</span> " + infoCursor.getLong(0)+"</p>"); 
		infoCursor.close();
		
		infoCursor = context.getContentResolver().query(Uri.parse("content://"+Task.AUTHORITY+"/db_info/page_size") , null, null, null, null);
		infoCursor.moveToFirst();
		text.append("<p><span style=\"font-weight:bold;\">APP_DATABASE_PAGE_SIZE:</span> " + infoCursor.getLong(0)+"</p>"); 
		infoCursor.close();
		
		// ****************
		// Add System Info
		// ****************
		
		text.append("<h3>-- System Version --</h3>");
		text.append("<p><span style=\"font-weight:bold;\">INCREMENTAL:</span> "+Build.VERSION.INCREMENTAL+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">RELEASE:</span> " 	+ Build.VERSION.RELEASE+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">SDK:</span> " 		+ Build.VERSION.SDK+"</p>");
		
		text.append("<h3>-- System Build --</h3>");
		text.append("<p><span style=\"font-weight:bold;\">BOARD:</span> " 	+ Build.BOARD+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">BRAND:</span> " 	+ Build.BRAND+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">DEVICE:</span> " 	+ Build.DEVICE+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">FINGERPRINT:</span> "+Build.FINGERPRINT+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">HOST:</span> " 		+ Build.HOST+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">ID:</span> " 		+ Build.ID+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">MODEL:</span> " 	+ Build.MODEL+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">PRODUCT:</span> " 	+ Build.PRODUCT+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">TAGS:</span> " 		+ Build.TAGS+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">TIME:</span> " 		+ Build.TIME+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">TYPE:</span> " 		+ Build.TYPE+"</p>");
		text.append("<p><span style=\"font-weight:bold;\">USER:</span> " 		+ Build.USER+"</p>");
		
		text.append("<h3>-- File System --</h3>");

		text.append("<p><span style=\"font-weight:bold;\">EXTERNAL_STORAG_STATE:</span> " + Environment.getExternalStorageState()+"</p>");
		
		text.append("<p><span style=\"font-weight:bold;\">ROOT_DIRECTORY:</span> " + Environment.getRootDirectory()+"</p>");
		StatFs sf = new StatFs(Environment.getRootDirectory().getAbsolutePath());
		text.append("<ul>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCKS_FREE:</span> " + sf.getFreeBlocks()+"</p></li>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCKS_AVAILABLE:</span> " + sf.getAvailableBlocks()+"</p></li>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCK_COUNT:</span> " + sf.getBlockCount()+"</p></li>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCK_SIZE:</span> " + sf.getBlockSize()+"</p></li>");
		text.append("</ul>");
		
		sf.restat(Environment.getDataDirectory().getAbsolutePath());
		text.append("<p><span style=\"font-weight:bold;\">DATA_DIRECTORY:</span> " + Environment.getDataDirectory()+"</p>");
		text.append("<ul>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCKS_FREE:</span> " + sf.getFreeBlocks()+"</p></li>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCKS_AVAILABLE:</span> " + sf.getAvailableBlocks()+"</p></li>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCK_COUNT:</span> " + sf.getBlockCount()+"</p></li>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCK_SIZE:</span> " + sf.getBlockSize()+"</p></li>");
		text.append("</ul>");
		
		sf.restat(Environment.getDownloadCacheDirectory().getAbsolutePath());
		text.append("<p><span style=\"font-weight:bold;\">DOWNLOAD_CACHE_DIRECTORY:</span> " + Environment.getDownloadCacheDirectory()+"</p>");
		text.append("<ul>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCKS_FREE:</span> " + sf.getFreeBlocks()+"</p></li>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCKS_AVAILABLE:</span> " + sf.getAvailableBlocks()+"</p></li>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCK_COUNT:</span> " + sf.getBlockCount()+"</p></li>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCK_SIZE:</span> " + sf.getBlockSize()+"</p></li>");
		text.append("</ul>");
		
		sf.restat(Environment.getExternalStorageDirectory().getAbsolutePath());
		text.append("<p><span style=\"font-weight:bold;\">EXTERNAL_STORAGE_DIRECTORY:</span> " + Environment.getExternalStorageDirectory()+"</p>");
		text.append("<ul>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCKS_FREE:</span> " + sf.getFreeBlocks()+"</p></li>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCKS_AVAILABLE:</span> " + sf.getAvailableBlocks()+"</p></li>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCK_COUNT:</span> " + sf.getBlockCount()+"</p></li>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tBLOCK_SIZE:</span> " + sf.getBlockSize()+"</p></li>");
		text.append("</ul>");
		
		
		text.append("<h3>-- SQLite --</h3>");
		Cursor sqliteVersionCursor = context.getContentResolver().query(Task.TempFile.CONTENT_URI, new String[]{"sqlite_version()"}, null, null, null);
		assert null != sqliteVersionCursor;
		text.append("<p><span style=\"font-weight:bold;\">\tSQLITE_VERSION:</span> " + (sqliteVersionCursor.moveToFirst()?sqliteVersionCursor.getString(0):"Unknown")+"</p>");
		sqliteVersionCursor.close();
        
		text.append("<p><span style=\"font-weight:bold;\">APP_TABLE_SIZES:</span></p>");
		Cursor countCursor = context.getContentResolver().query(Task.Tasks.CONTENT_URI, null, null, null, null);
		text.append("<ul>");
		text.append("<li><p><span style=\"font-weight:bold;\">\tTABLE_T_SIZE:</span> " + countCursor.getCount()+"</p></li>");
		countCursor.close();
		countCursor = context.getContentResolver().query(Task.TaskAttachments.CONTENT_URI, null, null, null, null);
		text.append("<li><p><span style=\"font-weight:bold;\">\tTABLE_A_SIZE:</span> " + countCursor.getCount()+"</p></li>");
		countCursor.close();
		countCursor = context.getContentResolver().query(Task.ProximityAlerts.CONTENT_URI, null, null, null, null);
		text.append("<li><p><span style=\"font-weight:bold;\">\tTABLE_P_SIZE:</span> " + countCursor.getCount()+"</p></li>");
		countCursor.close();
		countCursor = context.getContentResolver().query(Task.Labels.CONTENT_URI, null, null, null, null);
		text.append("<li><p><span style=\"font-weight:bold;\">\tTABLE_L_SIZE:</span> " + countCursor.getCount()+"</p></li>");
		countCursor.close();
		countCursor = context.getContentResolver().query(Task.LabeledContent.CONTENT_URI, null, null, null, null);
		text.append("<li><p><span style=\"font-weight:bold;\">\tTABLE_LC_SIZE:</span> " + countCursor.getCount()+"</p></li>");
		countCursor.close();
		countCursor = context.getContentResolver().query(Task.Filter.CONTENT_URI, null, null, null, null);
		text.append("<li><p><span style=\"font-weight:bold;\">\tTABLE_F_SIZE:</span> " + countCursor.getCount()+"</p></li>");
		countCursor.close();
		countCursor = context.getContentResolver().query(Task.FilterElement.CONTENT_URI, null, null, null, null);
		text.append("<li><p><span style=\"font-weight:bold;\">\tTABLE_FE_SIZE:</span> " + countCursor.getCount()+"</p></li>");
		countCursor.close();
		countCursor = context.getContentResolver().query(Task.TempFile.CONTENT_URI, null, null, null, null);
		text.append("<li><p><span style=\"font-weight:bold;\">\tTABLE_TF_SIZE:</span> " + countCursor.getCount()+"</p></li>");
		countCursor.close();
		countCursor = context.getContentResolver().query(Task.Notification.CONTENT_URI, null, null, null, null);
		text.append("<li><p><span style=\"font-weight:bold;\">\tTABLE_N_SIZE:</span> " + countCursor.getCount()+"</p></li>");
		countCursor.close();
		countCursor = context.getContentResolver().query(Task.Completable.CONTENT_URI, null, null, null, null);
		text.append("<li><p><span style=\"font-weight:bold;\">\tTABLE_C_SIZE:</span> " + countCursor.getCount()+"</p></li>");
		countCursor.close();
		text.append("</ul>");
		
		
		// Add Stacktrace.
		if( null != throwable ){
			text.append("<h3>-- Stacktrace --</h3>");
			text.append("<p><span style=\"font-weight:bold;\">STACKTRACE:</p>");
			text.append(Log.getStackTraceString(throwable));
		}

		text.append("</body>");
		text.append("</html>");
		
		ContentValues values = new ContentValues();
		try {
			values.put(Task.TempFile.DATA, text.toString().getBytes("UTF-8"));
			values.put(Task.TempFile._PRESERVE_UNTIL, System.currentTimeMillis() + StaticConfig.FEEDBACK_SYSTEM_DETAILS_FILE_LIFE_TIME); 
		} catch (UnsupportedEncodingException e1) {
			Log.e(TAG, "ERR0002Y Package name not found", e1);
			ErrorUtil.handleExceptionAndThrow("ERR0002Y", e1, context);
		}
		values.put(Task.TempFile.DISPLAY_NAME, "SystemDetails.html");
		values.put(Task.TempFile.MIME_TYPE, "text/html");
		return context.getContentResolver().insert(Task.TempFile.CONTENT_URI, values);
	}
}
