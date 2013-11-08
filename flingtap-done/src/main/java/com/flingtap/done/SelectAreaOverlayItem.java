// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

/**
 * @author spencer
 */
public class SelectAreaOverlayItem extends OverlayItem implements Parcelable {

	private Intent.ShortcutIconResource mParceableMarker = null; 


	public SelectAreaOverlayItem(Parcel in) {
		super(new GeoPoint(in.readInt(), in.readInt()), in.readString(), in.readString());
		mParceableMarker = in.readParcelable(null);
	}
	
	public SelectAreaOverlayItem(GeoPoint point, String title, String snippet) {
		super(point, title, snippet);
		assert null != snippet;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(getPoint().getLatitudeE6());
		parcel.writeInt(getPoint().getLongitudeE6());
		parcel.writeString(getTitle());
		parcel.writeString(getSnippet());
		parcel.writeParcelable(mParceableMarker, 0);
	}
	
	@Override
	public void setMarker(Drawable marker) {
		throw new UnsupportedOperationException("Use setMarkerResourceId instead.");
	}
	
	public void setParceableMarker(Intent.ShortcutIconResource parceableMarker) {
		mParceableMarker = parceableMarker;
	}
	
    public static final Parcelable.Creator<SelectAreaOverlayItem> CREATOR = new Parcelable.Creator<SelectAreaOverlayItem>() {
		public SelectAreaOverlayItem createFromParcel(Parcel in) {
		    return new SelectAreaOverlayItem(in);
		}
		
		public SelectAreaOverlayItem[] newArray(int size) {
		    return new SelectAreaOverlayItem[size];
		}
	};

}
