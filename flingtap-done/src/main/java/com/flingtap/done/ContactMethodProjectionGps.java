// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.provider.BaseColumns;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.ContactMethodsColumns;

/**
 * 
 * 
 * @author spencer
 */
public class ContactMethodProjectionGps {
	
	/**
	 * Columns from the ContactMethods table which are useful for GPS work.
	 */
	public static final String[] CONTACT_METHODS_PROJECTION = new String[] {
		ContactMethods.PERSON_ID,       // 0
		ContactMethodsColumns.KIND,     // 1
		ContactMethodsColumns.DATA,     // 2
		ContactMethodsColumns.AUX_DATA, // 3
		ContactMethodsColumns.TYPE,     // 4
		ContactMethodsColumns.LABEL,    // 5
		ContactMethods._ID,				// 6
	};
	
	public static int CONTACT_M_PERSON_ID_INDEX = 0;
	public static int CONTACT_M_KIND_INDEX      = 1;
	public static int CONTACT_M_DATA_INDEX      = 2;
	public static int CONTACT_M_AUX_DATA_INDEX  = 3;
	public static int CONTACT_M_TYPE_INDEX      = 4;
	public static int CONTACT_M_LABEL_INDEX     = 5;
	public static int CONTACT_M_ID_INDEX        = 6;

	
	/**
	 * Columns from the ??? table which are useful for GPS work.
	 * 
	 * 
	 * 
	 */
	public static final String[] CONTACT_CONTRACT_DATA_PROJECTION = new String[] { // TODO: !!! The code that uses this array doesn't need all this data. Rewrite the code so only the required fields are requested.
		"_id", 			// ContactsContract.Data._ID, 											// 0
		"contact_id", 	// ContactsContract.Data.CONTACT_ID, 									// 1
		"data2", 		// ContactsContract.CommonDataKinds.StructuredPostal.TYPE, 				// 2 
		"data3", 		// ContactsContract.CommonDataKinds.StructuredPostal.LABEL, 			// 3
		"data1", 		// ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, // 4
	};
	
	public static int CONTACT_CONTRACT_DATA_ID_INDEX        		= 0;	
	public static int CONTACT_CONTRACT_DATA_CONTACT_ID_INDEX 		= 1;
	public static int CONTACT_CONTRACT_DATA_TYPE_INDEX      		= 2;
	public static int CONTACT_CONTRACT_DATA_LABEL_INDEX     		= 3;
	public static int CONTACT_CONTRACT_DATA_FORMATTED_ADDRESS_INDEX	= 4;
}
