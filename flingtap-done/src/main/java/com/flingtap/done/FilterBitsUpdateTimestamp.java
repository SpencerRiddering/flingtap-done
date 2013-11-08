// Licensed under the Apache License, Version 2.0

package com.flingtap.done;

import android.content.Context;

import com.flingtap.common.Timestamp;

public class FilterBitsUpdateTimestamp extends Timestamp {
	public FilterBitsUpdateTimestamp(Context context) {
		super(context, "filter_bits_applied.ts");
	}
}
