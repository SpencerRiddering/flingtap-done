// Licensed under the Apache License, Version 2.0

package com.flingtap.done.provider;

import android.content.SearchRecentSuggestionsProvider;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Convenience definitions for TaskProvider
 */
public final class TaskSearchRecentSuggestions {
	
	private static final String TAG = "TaskSearchRecentSuggestions";
	public static final String AUTHORITY = "com.flingtap.done.provider.TaskSearchRecentSuggestions"; 
	public static final int MODE = SearchRecentSuggestionsProvider.DATABASE_MODE_QUERIES;
	
    
}
