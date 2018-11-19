package org.spring.gearman.boot.utils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdWorker {
	
	protected static final Logger LOG = LoggerFactory.getLogger(IdWorker.class);
    
    private long workerId;
    private long datacenterId;
    private long sequence = 0L;
 
    private long twepoch = 1288834974657L;
 
    private long workerIdBits = 8L; //8L 3L
    private long datacenterIdBits = 6L; //6L 1L
    private long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    private long sequenceBits = 8L; //8L
 
    private long workerIdShift = sequenceBits;
    private long datacenterIdShift = sequenceBits + workerIdBits;
    private long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private long sequenceMask = -1L ^ (-1L << sequenceBits);
 
    private long lastTimestamp = -1L;
    
    private Lock locker;
 
    public IdWorker(long workerId, long datacenterId) {
        // sanity check for workerId
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        
        locker = new ReentrantLock();
        //LOG.info(String.format("worker starting. timestamp left shift %d, datacenter id bits %d, worker id bits %d, sequence bits %d, workerid %d", timestampLeftShift, datacenterIdBits, workerIdBits, sequenceBits, workerId));
    }
 
    public long nextId() {
    	try{
	    	locker.lock();
	    	
	        long timestamp = timeGen();
	 
	        if (timestamp < lastTimestamp) {
	            LOG.error(String.format("clock is moving backwards.  Rejecting requests until %d.", lastTimestamp));
	            throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
	        }
	 
	        if (lastTimestamp == timestamp) {
	            sequence = (sequence + 1) & sequenceMask;
	            if (sequence == 0) {
	                timestamp = tilNextMillis(lastTimestamp);
	            }
	        } else {
	            sequence = 0L;
	        }
	 
	        lastTimestamp = timestamp;
	        //((timestamp - twepoch) << timestampLeftShift) | (workerId << workerIdShift) | sequence;
	        //((timestamp - twepoch) << timestampLeftShift) | (datacenterId << datacenterIdShift) | (workerId << workerIdShift) | sequence;
	 
	        return ((timestamp - twepoch) << timestampLeftShift) | (datacenterId << datacenterIdShift) | (workerId << workerIdShift) | sequence;
    	}finally{
    		locker.unlock();
    	}
    }
 
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }
 
    private long timeGen() {
        return System.currentTimeMillis();
    }
    
    public static void main(String args[]) {
		try {
			IdWorker idWorker = new IdWorker(1, 1);
			System.out.println(idWorker.nextId());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
