package org.spring.gearman.boot.utils;

import java.io.Serializable;

public class JsonResponseMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6869747716562281297L;

	private Integer ret;
	
	private String msg;
	
	private Object content;

	
	public Integer getRet() {
		return ret;
	}

	public void setRet(Integer ret) {
		this.ret = ret;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Object getContent() {
		return content;
	}

	public void setContent(Object content) {
		this.content = content;
	}
}
