package org.spring.gearman.boot.utils;

public class NoCatchException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public NoCatchException() {
		super();
	}

	public NoCatchException(String msg) {
		super(msg);
	}
}
