package org.spring.gearman.boot.config;

import java.util.Map;

public class ActionConfigRegistry {
	
	private Map<String, ActionConfig> configMap;

	public ActionConfig getConfig(String actionName) {
		return configMap.get(actionName);
	}
	
}
