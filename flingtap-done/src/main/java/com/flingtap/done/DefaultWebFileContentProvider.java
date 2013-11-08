package com.flingtap.done;
/**
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.lang.UnsupportedOperationException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/**
 * WebView does not support file: loading. This class wraps a file load
 * with a content provider. 
 * As HTMLViewer does not have internet access nor does it allow
 * Javascript to be run, it is safe to load file based HTML content.
*/
public class DefaultWebFileContentProvider extends ContentProvider {
    private static final String TAG = "DefaultWebFileContentProvider";
	
    public static final String BASE_URI = 
            "content://com.flingtap.done.htmlfileprovider";
    public static final int BASE_URI_LEN = BASE_URI.length();

    @Override
    public String getType(Uri uri) {
        // If the mimetype is not appended to the uri, then return an empty string
        String mimetype = uri.getQuery();
        return mimetype == null ? "" : mimetype;
    }
    
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
    	// Log.v(TAG, "openFile(...) called.");
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Bad mode for " + uri + ": " + mode);
        }
        String filename = uri.toString().substring(BASE_URI_LEN);
        
        try {
			return getContext().getAssets().openFd(filename).getParcelFileDescriptor();
		} catch (IOException e) {
			Log.e(TAG, "ERR000KU", e);
			ErrorUtil.handleException("ERR000KU", e);
		}
		return null;
    }    
    
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

}
