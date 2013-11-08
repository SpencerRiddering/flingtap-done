// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;

public class ParcelableGeoPoint extends GeoPoint implements Parcelable {

	public ParcelableGeoPoint(int latitudeE6, int longitudeE6) {
		super(latitudeE6, longitudeE6);
	}
	public ParcelableGeoPoint(Parcel in) {
		super(in.readInt(), in.readInt());
	}
	public ParcelableGeoPoint(GeoPoint geoPoint) {
		super(geoPoint.getLatitudeE6(), geoPoint.getLongitudeE6());
	}
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(getLatitudeE6());
		parcel.writeInt(getLongitudeE6());
	}
	
    public static final Parcelable.Creator<ParcelableGeoPoint> CREATOR = new Parcelable.Creator<ParcelableGeoPoint>() {
		public ParcelableGeoPoint createFromParcel(Parcel in) {
		    return new ParcelableGeoPoint(in);
		}
		
		public ParcelableGeoPoint[] newArray(int size) {
		    return new ParcelableGeoPoint[size];
		}
	};

}
