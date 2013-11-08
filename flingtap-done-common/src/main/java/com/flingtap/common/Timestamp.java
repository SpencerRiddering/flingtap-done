// Licensed under the Apache License, Version 2.0

package com.flingtap.common;

import java.io.File;
import java.io.IOException;

import android.content.Context;

public class Timestamp {

	private static final String TIMESTAMP_DIR = "misc"; 
	
	private final File timestampFile;
	
	/**
	 * Note: name should contain a period ('.') within the name otherwise Android treats the file as a folder.
	 * @param name
	 */
	public Timestamp(Context context, String name) {
		timestampFile = new File(context.getDir(TIMESTAMP_DIR, Context.MODE_PRIVATE), name); 
	}

	public boolean exists() {
		return timestampFile.exists();
	}
	
	/**
	 * 
	 * 
	 * @return true if the timestamp didn't exist, false otherwise.
	 */
	public boolean ensureExists() throws TimestampException {
		try{
			if( !exists() ){
				timestampFile.getParentFile().mkdirs();
				timestampFile.createNewFile();  
				return true;
			}
		}catch(IOException ioe){
			throw new TimestampException(ioe);
		}
		return false;
	}
	
	public long update() {
		long time = System.currentTimeMillis();
		timestampFile.setLastModified(time);
		return time;
	}
	
	public long getTimeInMillis() {
		return timestampFile.lastModified();
	}

	public class TimestampException extends Exception {
		public TimestampException(Throwable throwable) {
			super(throwable);
		}
	};
}
