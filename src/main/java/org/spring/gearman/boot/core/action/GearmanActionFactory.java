package org.spring.gearman.boot.core.action;

import org.gearman.common.Constants;
import org.gearman.worker.GearmanFunction;
import org.gearman.worker.GearmanFunctionFactory;
import org.slf4j.LoggerFactory;
import org.spring.gearman.boot.utils.IdWorker;
import org.springframework.context.ApplicationContext;

public class GearmanActionFactory implements GearmanFunctionFactory {

	private static final org.slf4j.Logger LOG =  LoggerFactory.getLogger(
            Constants.GEARMAN_WORKER_LOGGER_NAME);
	
	private Class<? extends GearmanAction> clazz;
	private ApplicationContext applicationContext;
	private String serviceName;
	private IdWorker idWorker;
	
	public GearmanActionFactory(String serviceName, Class<? extends GearmanAction> clazz, ApplicationContext applicationContext, IdWorker idWorker){
		this.clazz = clazz;
		this.applicationContext = applicationContext;
		this.serviceName  = serviceName;
		this.idWorker = idWorker;
	}
	
	public String getFunctionName() {
		return serviceName;
	}

	public GearmanFunction getFunction() {
		
		try {
			GearmanAction func = this.clazz.newInstance();
			func.initialize(serviceName, applicationContext, idWorker);
			return func;
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return null;
		}
		
	}

}
