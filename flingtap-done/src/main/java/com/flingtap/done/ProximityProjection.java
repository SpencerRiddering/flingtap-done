// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import com.flingtap.done.provider.Task;


/**
* TODO: REmove this unused class.
*/
public class ProximityProjection {

	/**
	 * Standard projection for the interesting columns of a proximity alert.
	 */
	public static final String[] PROXIMITY_PROJECTION = new String[] {
			Task.ProximityAlerts._ID, // 0
//			Task.ProximityAlerts.TASK_ID, // 1
//			Task.ProximityAlerts.ATTACHMENT_ID, // 1
			Task.ProximityAlerts.RADIUS, // 1
			Task.ProximityAlerts.RADIUS_UNIT, // 2
			Task.ProximityAlerts.ENABLED, // 3
//			"(SELECT " + Task.TaskAttachments.URI_ID + " FROM "
//					+ TaskProvider.ATTACHMENT_TABLE_NAME + " AS p WHERE t."
//					+ Task.ProximityAlerts.ATTACHMENT_ID
//					+ " = p._id) AS location_id", // 4
//			"(SELECT " + Task.TaskAttachments.URI_TYPE + " FROM "
//			+ TaskProvider.ATTACHMENT_TABLE_NAME + " AS p WHERE t."
//			+ Task.ProximityAlerts.ATTACHMENT_ID
//			+ " = p._id) AS location_type" // 5
	};
	public static final int ID_INDEX = 0;
//	public static final int TASK_ID_INDEX = 1;
//	public static final int ATTACHEMENT_ID_INDEX = 1;
	public static final int RADIUS_INDEX = 1;
	public static final int RADIUS_UNIT_INDEX = 2;
	public static final int ENABLED_INDEX = 3;
//	public static final int LOCATION_ID_INDEX = 5;
//	public static final int LOCATION_TYPE_INDEX = 7;
	
}
