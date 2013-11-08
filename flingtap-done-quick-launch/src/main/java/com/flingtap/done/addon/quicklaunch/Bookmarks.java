/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.flingtap.done.addon.quicklaunch;

import java.net.URISyntaxException;

import com.flingtap.common.ErrorUtil;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;


/**
 * User-defined bookmarks and shortcuts.  The target of each bookmark is an
 * Intent URL, allowing it to be either a web page or a particular
 * application activity.
 *
 * @hide
 * 
 * NOTE: This was copied from android.provider.Settings.Bookmarks because that code was hidden. Not all the contents were needed.
 *       Also, I've added some additional methods.
 * 
 * http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob_plain;f=core/java/android/provider/Settings.java;hb=5550ef48739a7bb16f80aa6b10e9c151b1438163
 * 
 * http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/provider/Settings.java
 *   android.settings.QUICK_LAUNCH_SETTINGS
 */
public final class Bookmarks implements BaseColumns
{
    private static final String TAG = "Bookmarks";
	public static final String AUTHORITY = "settings";
	
    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI =
        Uri.parse("content://" + AUTHORITY + "/bookmarks");

    /**
     * The row ID.
     * <p>Type: INTEGER</p>
     */
    public static final String ID = "_id";
    
    /**
     * Descriptive name of the bookmark that can be displayed to the user.
     * If this is empty, the title should be resolved at display time (use
     * {@link #getTitle(Context, Cursor)} any time you want to display the
     * title of a bookmark.)
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String TITLE = "title";

    /**
     * Arbitrary string (displayed to the user) that allows bookmarks to be
     * organized into categories.  There are some special names for
     * standard folders, which all start with '@'.  The label displayed for
     * the folder changes with the locale (via {@link #getLabelForFolder}) but
     * the folder name does not change so you can consistently query for
     * the folder regardless of the current locale.
     *
     * <P>Type: TEXT</P>
     *
     */
    public static final String FOLDER = "folder";

    /**
     * The Intent URL of the bookmark, describing what it points to.  This
     * value is given to {@link android.content.Intent#getIntent} to create
     * an Intent that can be launched.
     * <P>Type: TEXT</P>
     */
    public static final String INTENT = "intent";

    /**
     * Optional shortcut character associated with this bookmark.
     * <P>Type: INTEGER</P>
     */
    public static final String SHORTCUT = "shortcut";

    /**
     * The order in which the bookmark should be displayed
     * <P>Type: INTEGER</P>
     */
    public static final String ORDERING = "ordering";

    private static final String[] sIntentProjection = { INTENT };
    private static final String[] sShortcutProjection = { ID, SHORTCUT };
    private static final String sShortcutSelection = SHORTCUT + "=?";

    /**
     * Convenience function to retrieve the bookmarked Intent for a
     * particular shortcut key.
     *
     * @param cr The ContentResolver to query.
     * @param shortcut The shortcut key.
     *
     * @return Intent The bookmarked URL, or null if there is no bookmark
     *         matching the given shortcut.
     */
    public static Intent getIntentForShortcut(ContentResolver cr, char shortcut)
    {
        Intent intent = null;

        Cursor c = cr.query(CONTENT_URI,
                sIntentProjection, sShortcutSelection,
                new String[] { String.valueOf((int) shortcut) }, ORDERING);
        // Keep trying until we find a valid shortcut
        try {
            while (intent == null && c.moveToNext()) {
                try {
                    String intentURI = c.getString(c.getColumnIndexOrThrow(INTENT));
                    intent = Intent.getIntent(intentURI);
                } catch (java.net.URISyntaxException e) {
                    // The stored URL is bad...  ignore it.
                } catch (IllegalArgumentException e) {
                    // Column not found
                    Log.e(TAG, "Intent column not found", e);
                }
            }
        } finally {
            if (c != null) c.close();
        }

        return intent;
    }

    /**
     * Add a new bookmark to the system.
     *
     * @param cr The ContentResolver to query.
     * @param intent The desired target of the bookmark.
     * @param title Bookmark title that is shown to the user; null if none
     *            or it should be resolved to the intent's title.
     * @param folder Folder in which to place the bookmark; null if none.
     * @param shortcut Shortcut that will invoke the bookmark; 0 if none. If
     *            this is non-zero and there is an existing bookmark entry
     *            with this same shortcut, then that existing shortcut is
     *            cleared (the bookmark is not removed).
     * @return The unique content URL for the new bookmark entry.
     */
    public static Uri add(ContentResolver cr,
                                       Intent intent,
                                       String title,
                                       String folder,
                                       char shortcut,
                                       int ordering)
    {
        // If a shortcut is supplied, and it is already defined for
        // another bookmark, then remove the old definition.
    	Bookmarks.remove(cr, shortcut);

        ContentValues values = new ContentValues();
        if (title != null) values.put(TITLE, title);
        if (folder != null) values.put(FOLDER, folder);
        values.put(INTENT, intent.toURI());
        if (shortcut != 0) values.put(SHORTCUT, (int) shortcut);
        values.put(ORDERING, ordering);
        return cr.insert(CONTENT_URI, values);
    }
    
    public static boolean providerExists(ContentResolver cr){
    	try{
    		return null != cr.query(CONTENT_URI, new String[]{ID, TITLE, FOLDER, INTENT, SHORTCUT, ORDERING} , null, null, null);
    	}catch(SQLiteException sqle){
    		return false;
    	}
    }
    
    public static boolean exists(ContentResolver cr,
            char shortcut){
    	Cursor cursor = cr.query(CONTENT_URI, new String[]{}, sShortcutSelection, new String[]{String.valueOf((int) shortcut)}, null);
    	boolean result = ( null != cursor && cursor.getCount() > 0 );
    	cursor.close();
    	return result;
    }
    
    public static boolean remove(ContentResolver cr,
            char shortcut){
    	boolean found = false;
        if (shortcut != 0) {
            Cursor c = cr.query(CONTENT_URI,
                    sShortcutProjection, sShortcutSelection,
                    new String[] { String.valueOf((int) shortcut) }, null);
            if( c == null ){
            	return false;
            }
            try {
    			while (c.moveToNext()) {
					if( 1 != cr.delete(CONTENT_URI, "_id=?", new String[]{String.valueOf(c.getLong(0))}) ){
						Log.e(TAG, "ERR00056 Could not delete existing shortcut row");
						ErrorUtil.handle("ERR00056", "Could not delete existing shortcut row", Bookmarks.class);
						break;
					}else{
						found = true;
					}
				}                
            } finally {
                c.close();
            }
        }    	
        return found;
    }
    
    public static String lookupName(Context context, char shortcut){
    	String result = null;
    	Cursor cursor = context.getContentResolver().query(CONTENT_URI, new String[]{TITLE, INTENT}, sShortcutSelection, new String[]{String.valueOf((int) shortcut)}, null);
    	if( null != cursor && cursor.moveToFirst()){
    		result = getTitle(context, cursor).toString(); 
    		if( result.length() == 0 ){
    			result = "?";
    		}
    	}else{
    		result = "?";
    	}
    	cursor.close();
    	return result;
    }

    /**
     * Return the title as it should be displayed to the user. This takes
     * care of localizing bookmarks that point to activities.
     *
     * @param context A context.
     * @param cursor A cursor pointing to the row whose title should be
     *        returned. The cursor must contain at least the {@link #TITLE}
     *        and {@link #INTENT} columns.
     * @return A title that is localized and can be displayed to the user,
     *         or the empty string if one could not be found.
     */
    public static CharSequence getTitle(Context context, Cursor cursor) {
        int titleColumn = cursor.getColumnIndex(TITLE);
        int intentColumn = cursor.getColumnIndex(INTENT);
        if (titleColumn == -1 || intentColumn == -1) {
            throw new IllegalArgumentException(
                    "The cursor must contain the TITLE and INTENT columns.");
        }

        String title = cursor.getString(titleColumn);
        if (!TextUtils.isEmpty(title)) {
            return title;
        }

        String intentUri = cursor.getString(intentColumn);
        if (TextUtils.isEmpty(intentUri)) {
            return "";
        }

        Intent intent;
        try {
            intent = Intent.getIntent(intentUri);
        } catch (URISyntaxException e) {
            return "";
        }

        PackageManager packageManager = context.getPackageManager();
        ResolveInfo info = packageManager.resolveActivity(intent, 0);
        return info != null ? info.loadLabel(packageManager) : "";
    }

    
}



