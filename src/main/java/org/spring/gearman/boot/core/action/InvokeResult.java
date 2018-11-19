package org.spring.gearman.boot.core.action;

import java.io.Serializable;

public class InvokeResult<T> implements Serializable {


	/**
	 * 
	 */
	private static final long serialVersionUID = -1928148365670379871L;
	// 响应结果码，默认为0-成功
	private int returnCode = 0;
	// 消息
	private String message;
	// 返回的数据
	private T data;

	public InvokeResult() {
	}

	public InvokeResult(int returnCode, String message) {
		this.returnCode = returnCode;
		this.message = message;
	}

	public InvokeResult(int returnCode, String message, T data) {
		this.returnCode = returnCode;
		this.message = message;
		this.data = data;
	}

	public int getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

}
