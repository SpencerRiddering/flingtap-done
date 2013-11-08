// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import com.flingtap.done.base.R;

public class AddonRequiredDialogPart {
	
	public static Dialog onCreateDialog(final Context context, int titleResId, int messageResId, final Uri marketUri) {
		return new AlertDialog.Builder(context)
		.setTitle(titleResId)
		.setMessage(messageResId)
		.setPositiveButton(R.string.button_tellMeMore, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Redirect user to Market page for add-on.
				Intent theIntent = new Intent(Intent.ACTION_VIEW);
				theIntent.addFlags( Intent.FLAG_ACTIVITY_NO_HISTORY );
				theIntent.setData(marketUri);
				context.startActivity(theIntent);
			}
		})
		.setNegativeButton(R.string.button_noThanks, null)
		.create();
		
	}
	
}
