// Licensed under the Apache License, Version 2.0

package com.flingtap.done.util;

import com.flingtap.done.base.R;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.OpenableColumns;

/**
 * Logic for extracting label and image info about a URI.
 */
public class UriInfo {

	private String label = null;
	private Intent.ShortcutIconResource iconResource = null;
	private Bitmap iconBitmap = null;
	
	public UriInfo(Intent standardIntent, Uri data, ContentResolver cr, PackageManager pm, Resources resources, String name){
		label = name;
		
		if( null != data ){
			String mimeType = cr.getType(data);
			if( null == label && null != mimeType ){
				// We know that the content provider should support OpenableColumns.
				
				Cursor openableCursor = cr.query(data, null, null, null, null);
				int columnIndex = openableCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
				if( -1 != columnIndex ){
					if( openableCursor.moveToFirst()){
						label = openableCursor.getString(columnIndex);
					}
				}
				openableCursor.close();
				
			}
		}
		if( label == null ){
			ResolveInfo resolveInfo = pm.resolveActivity(standardIntent, PackageManager.MATCH_DEFAULT_ONLY);
			if( null != resolveInfo ){
				CharSequence charSeqLabel = pm.getApplicationLabel(resolveInfo.activityInfo.applicationInfo);
				if( null != charSeqLabel ){  // Use application name.
					label = charSeqLabel.toString(); 
				}else if( null != data ){ // Use URI authority for unknown attachment name.
					label = data.getAuthority(); 
				}else{
					label = resources.getText(R.string.unknown).toString();
				}
				
				setupIconResource(pm, resolveInfo);
				
			}
		}

		// *********************
		// Find icon
		// *********************
		if( null == iconResource ){
			ResolveInfo resolveInfo = pm.resolveActivity(standardIntent, PackageManager.MATCH_DEFAULT_ONLY);
			if( null != resolveInfo ){
				setupIconResource(pm, resolveInfo);
			}
		}
		
		if( null == iconResource ){
			Drawable activityIcon = null;
			try {
				activityIcon = pm.getActivityIcon(standardIntent);

				if (activityIcon instanceof BitmapDrawable) { // Determine whether
																// activityIcon is
																// the default icon
																// so we can use our
																// own default icon.
					Bitmap defaultIconBitmap = ((BitmapDrawable) (pm.getDefaultActivityIcon())).getBitmap();
					if (defaultIconBitmap.equals(((BitmapDrawable) activityIcon).getBitmap())) {
						iconResource = new Intent.ShortcutIconResource();
						iconResource.packageName = resources.getResourcePackageName(R.drawable.question);
						iconResource.resourceName = resources.getResourceName(R.drawable.question);
					}else{
						iconBitmap = ((BitmapDrawable) activityIcon).getBitmap();
					}
				}else{
					// Cannot persist drawables that are not: 
					//  - A resource
					//  - A Bitmap
					
				}
			} catch (NameNotFoundException e) {
				iconResource = new Intent.ShortcutIconResource();
				iconResource.packageName = resources.getResourcePackageName(R.drawable.question);
				iconResource.resourceName = resources.getResourceName(R.drawable.question);
			}
		}		
	}


	private void setupIconResource(PackageManager pm, ResolveInfo resolveInfo) {
		Resources resources = null;
		try {
			resources = pm.getResourcesForApplication(resolveInfo.activityInfo.packageName);
			
			// Icon
			int id = resolveInfo.getIconResource();
			if( 0 == id ){
				return;
			}
			iconResource = new Intent.ShortcutIconResource();
			iconResource.packageName = resources.getResourcePackageName(id);
			iconResource.resourceName = resources.getResourceName(id);
		} catch (NameNotFoundException e) {
			// Ignore
		}

	}
	

	public String getLabel() {
		return label;
	}

	public Intent.ShortcutIconResource getIconResource() {
		return iconResource;
	}

	public Bitmap getIconBitmap() {
		return iconBitmap;
	}
	
}
