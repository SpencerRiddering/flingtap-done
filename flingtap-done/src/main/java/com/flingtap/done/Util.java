// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import com.google.android.maps.GeoPoint;

/**
 * Misc utility functions.
 */
public final class Util {

	private static final String TAG = "Util";
	
	public static final GeoPoint createPoint(final String geoUri){
		int commaIndex = geoUri.indexOf(','); // Assumes no altitude exists.
		GeoPoint point = new GeoPoint((int)(Float.parseFloat(geoUri.substring(4, commaIndex))*1E6),(int)(Float.parseFloat(geoUri.substring(commaIndex+1))*1E6));
		return point;
	}

	public static final CharSequence createGeoUri(GeoPoint geoPoint){
		StringBuilder geoUri = new StringBuilder();
		geoUri.append("geo:");
		geoUri.append(geoPoint.getLatitudeE6()/1E6);
		geoUri.append(',');
		geoUri.append(geoPoint.getLongitudeE6()/1E6);
		return geoUri;
	}
	
	public static final void displayUriDetails(Uri data){
		Log.v(TAG, "authority = " + data.getAuthority());
		Log.v(TAG, "encodedAuthority = " + data.getEncodedAuthority());
		Log.v(TAG, "encodedFragment = " + data.getEncodedFragment());
		Log.v(TAG, "encodedPath = " + data.getEncodedPath());
		Log.v(TAG, "encodedQuery = " + data.getEncodedQuery());
		Log.v(TAG, "encodedSchemeSpecificPart = " + data.getEncodedSchemeSpecificPart());
		Log.v(TAG, "encodedUserInfo = " + data.getEncodedUserInfo());
		Log.v(TAG, "fragment = " + data.getFragment());
		Log.v(TAG, "host = " + data.getHost());
		Log.v(TAG, "lastPathSegment = " + data.getLastPathSegment());
		Log.v(TAG, "path = " + data.getPath());
		Log.v(TAG, "query = " + data.getQuery());
		Log.v(TAG, "scheme = " + data.getScheme());
		Log.v(TAG, "schemaSpecificPart = " + data.getSchemeSpecificPart());
		Log.v(TAG, "userInfo = " + data.getUserInfo());
		Log.v(TAG, "int    port = " + data.getPort());

	}
	
    /**
	 * Get current version number.
	 * 
	 * @return
	 */
	public static String getVersionNumber(Context context) {
		String version = "?";
		try {
			PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			version = pi.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Package name not found", e); // TODO: !! Event?
		};
		return version;
	} 	
}
