package org.spring.gearman.boot.config;

import java.util.List;

public class GearmanConfigure {
	
	private int datacenterId = -1;
	
	private List<GearmanWorkInfo> workersInfo;

	public int getDatacenterId() {
		return datacenterId;
	}

	public void setDatacenterId(int datacenterId) {
		this.datacenterId = datacenterId;
	}

	public List<GearmanWorkInfo> getWorkersInfo() {
		return workersInfo;
	}

	public void setWorkersInfo(List<GearmanWorkInfo> workersInfo) {
		this.workersInfo = workersInfo;
	};
	
}
