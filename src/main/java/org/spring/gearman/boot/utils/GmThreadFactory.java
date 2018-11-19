package org.spring.gearman.boot.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class GmThreadFactory implements ThreadFactory{

	private final AtomicLong threadIndex = new AtomicLong(0);
    private final String threadNamePrefix;


    public GmThreadFactory(final String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

	public Thread newThread(Runnable r) {
		return new Thread(r, Thread.currentThread().getName() + "_" + threadNamePrefix + this.threadIndex.incrementAndGet());
	}

}
