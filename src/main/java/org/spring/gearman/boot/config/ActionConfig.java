package org.spring.gearman.boot.config;

public class ActionConfig {
	
	private String actionName;

	private Object action;

	private String method;
	
	private boolean isRetStream=false;

	public String getActionName() {
		return actionName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public Object getAction() {
		return action;
	}

	public void setAction(Object action) {
		this.action = action;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public boolean isRetStream() {
		return isRetStream;
	}

	public void setRetStream(boolean isRetStream) {
		this.isRetStream = isRetStream;
	}

	
}
