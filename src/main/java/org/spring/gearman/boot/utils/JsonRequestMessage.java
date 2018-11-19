package org.spring.gearman.boot.utils;

import java.io.Serializable;

public class JsonRequestMessage implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3397884424769217861L;

	private String method;
		
	private String content;

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
	

}
