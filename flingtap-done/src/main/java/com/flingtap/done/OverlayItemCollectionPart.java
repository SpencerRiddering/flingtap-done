// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO: Add menu items/shortcuts which enable user to toggle the display of different sets of overlays.
 *
 */
public class OverlayItemCollectionPart extends AbstractContextActivityParticipant {
	
	ArrayList<SelectAreaOverlayItem> overlayList = new ArrayList<SelectAreaOverlayItem>();
	
	public boolean addOverlayItem(SelectAreaOverlayItem item){
		return overlayList.add(item);
	}
	public boolean addOverlayAllItems(Collection<? extends SelectAreaOverlayItem> items){
		return overlayList.addAll(items);
	}
	public void clear(){
		overlayList.clear();
	}
	
//	public boolean addOverlayAllItems(SelectAreaOverlayItem[] items){
//		boolean retValue = true;
//		for(SelectAreaOverlayItem item : items){
//			retValue = retValue && overlayList.add(item);
//		}
//		return retValue;
//	}
	
	public SelectAreaOverlayItem[] toArray(){
		return (SelectAreaOverlayItem[])overlayList.toArray(new SelectAreaOverlayItem[]{});
	}
	
}
