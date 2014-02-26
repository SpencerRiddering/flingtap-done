// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
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

	public SelectAreaOverlayItem[] toArray(){
		return (SelectAreaOverlayItem[])overlayList.toArray(new SelectAreaOverlayItem[]{});
	}
	
}
