package org.spring.gearman.boot.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.gearman.worker.GearmanWorkerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spring.gearman.boot.config.GearmanFunctionInfo;
import org.spring.gearman.boot.config.GearmanServerInfo;
import org.spring.gearman.boot.config.GearmanWorkInfo;
import org.spring.gearman.boot.core.action.GearmanAction;
import org.spring.gearman.boot.core.action.GearmanActionFactory;
import org.spring.gearman.boot.utils.GmException;
import org.spring.gearman.boot.utils.GmThreadFactory;
import org.spring.gearman.boot.utils.IdWorker;
import org.springframework.context.ApplicationContext;

public class GearmanWorkRunner extends Thread{
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private GearmanWorkInfo workerInfo;
	
	private long datacenterId;
	
	private long workerId;
	
	private ApplicationContext applicationContext;
	
	public GearmanWorkRunner(GearmanWorkInfo workerInfo, ApplicationContext applicationContext, long datacenterId, long workerId) throws GmException{
		this.setDaemon(true);
		if(workerInfo == null) throw new GmException("workerInfo is null");
		if(workerInfo.getServerInfos() == null || workerInfo.getServerInfos().size() == 0) throw new GmException("ServerInfos is null");
		if(workerInfo.getActions() == null || workerInfo.getActions().size() == 0) throw new GmException("actions is null");
			
		this.workerInfo = workerInfo;
		this.datacenterId = datacenterId;
		this.workerId = workerId;
		this.applicationContext = applicationContext;
	}
	
	@Override
	public void run() {
		try {
			initGearmanWorker();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void initGearmanWorker() throws ClassNotFoundException {

		int threadsNum = this.workerInfo.getThreadPoolCapacity();
		ExecutorService executorService = null;
		if (threadsNum <= 0) {
			executorService = Executors.newCachedThreadPool(new GmThreadFactory("GMACTION_"));
		} else if(threadsNum > 0) {
			executorService = Executors.newFixedThreadPool(threadsNum, new GmThreadFactory("GMACTION_"));
		}

		GearmanWorkerImpl worker = new GearmanWorkerImpl(executorService);
		
		IdWorker idWorker = null;
		if(this.datacenterId != -1){
			idWorker = new IdWorker(this.workerId, this.datacenterId);
		}
		
		for (GearmanFunctionInfo functionInfo : this.workerInfo.getActions()) {
			
			String serviceClass = functionInfo.getClazz();
			String serviceName = functionInfo.getServiceName();
			
			worker.registerFunctionFactory(new GearmanActionFactory(
					serviceName, (Class<? extends GearmanAction>) Class
							.forName(serviceClass), applicationContext, idWorker), functionInfo.getTimeout());
		}
		
		for (GearmanServerInfo server : this.workerInfo.getServerInfos()) {
			worker.addServer(server.getIp(), server.getPort());
		}
		
		worker.work();
	}
	

}
