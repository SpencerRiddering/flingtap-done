// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.TextView;

import com.flingtap.common.HandledException;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.ItemizedOverlay.OnFocusChangeListener;

/**
 *
 */
public class SelectAreaOverlays extends ItemizedOverlay<SelectAreaOverlayItem> {

	private static final String TAG = "SelectAreaOverlays";

	private SelectAreaOverlayItem[] mOverlayItems = null;
	private Drawable mDefaultMarker = null;
	private Context mContext = null;
	private MapController mController = null;	
	protected OverlayItem mOverlayitem = null;
	
	public SelectAreaOverlays(Drawable defaultMarker, Context context, SelectAreaOverlayItem[] overlayItems, MapController controller) {
		super(defaultMarker);
			
		assert null != defaultMarker;
		mDefaultMarker = defaultMarker;
		
		assert null != context;
		mContext = context;
		
		assert null != controller;
		mController = controller;

		assert null != overlayItems;
		mOverlayItems = overlayItems;
		
		boundCenterBottom(defaultMarker); 
		
		populate();		
	}

	/**
	 * When an item is tapped, animate over to it and then it the focus.
	 */
	protected boolean onTap(int i) {
    	super.onTap(i);
    	try{
    		//Log.v(TAG, "ontTap(int) called.");
    		SelectAreaOverlayItem item = getItem(i);
    		mController.animateTo(item.getPoint());
    		setFocus(item);
    	}catch(HandledException h){ // Ignore.
    	}catch(Exception exp){
    		Log.e(TAG, "ERR0005U", exp);
    		ErrorUtil.handleException("ERR0005U", exp, mContext);
    	}
		return true;
	}
	
	
	@Override
	protected SelectAreaOverlayItem createItem(int i) {
		return mOverlayItems[i];
	}


	@Override
	public int size() {
		return mOverlayItems.length;
	}

    public static Drawable boundCenterBottom(Drawable balloon){
    	return ItemizedOverlay.boundCenterBottom(balloon);
    }
	
    public static Drawable boundCenter(Drawable balloon){
    	return ItemizedOverlay.boundCenter(balloon);
    }	
	
}
