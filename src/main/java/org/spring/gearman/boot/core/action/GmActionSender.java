package org.spring.gearman.boot.core.action;

public interface GmActionSender {
	
	public void send(byte[] data);
	
}
