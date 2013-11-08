// Licensed under the Apache License, Version 2.0

package com.flingtap.common;

/**
 * This exception is used when the error/exception has been handled and you just need to climb out of the current situation. 
 * Standard error handling will catch and ignore this exception so only use it once the error has been properly documented.
 *
 */
public class HandledException extends RuntimeException {

	public HandledException() {
	}

	public HandledException(String detailMessage) {
		super(detailMessage);
	}

	public HandledException(Throwable throwable) {
		super(throwable);
	}

	public HandledException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
