package org.spring.gearman.boot;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spring.gearman.boot.config.GearmanConfigure;
import org.spring.gearman.boot.config.GearmanWorkInfo;
import org.spring.gearman.boot.core.GearmanWorkRunner;
import org.spring.gearman.boot.utils.GmException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class GearmanBootstrap implements ApplicationContextAware {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private GearmanConfigure configure;

	private ApplicationContext applicationContext;
	
	private List<Thread> runnerList = new ArrayList<Thread>();

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void start() throws GmException {
		
		long workerId = 0;
		for(GearmanWorkInfo workerInfo : this.configure.getWorkersInfo()){
			for(int i=0; i < workerInfo.getCount(); i++){
				Thread runner = new GearmanWorkRunner(workerInfo, this.applicationContext,
												this.configure.getDatacenterId(), workerId); 
				this.runnerList.add(runner);
				runner.start();
				workerId++;
			}
		}
		
		Object waiter = new Object();
		synchronized(waiter){
			try {
				waiter.wait();
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}

	}
}
