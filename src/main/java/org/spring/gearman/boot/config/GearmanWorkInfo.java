package org.spring.gearman.boot.config;

import java.util.List;

public class GearmanWorkInfo {

	private int count = 1;

	private int threadPoolCapacity = 1;
	
	private List<GearmanServerInfo> serverInfos;

	private List<GearmanFunctionInfo> actions;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getThreadPoolCapacity() {
		return threadPoolCapacity;
	}

	public void setThreadPoolCapacity(int threadPoolCapacity) {
		this.threadPoolCapacity = threadPoolCapacity;
	}

	public List<GearmanFunctionInfo> getActions() {
		return actions;
	}

	public void setActions(List<GearmanFunctionInfo> actions) {
		this.actions = actions;
	}

	public List<GearmanServerInfo> getServerInfos() {
		return serverInfos;
	}

	public void setServerInfos(List<GearmanServerInfo> serverInfos) {
		this.serverInfos = serverInfos;
	}

	
}
