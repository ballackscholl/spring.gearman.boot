package org.spring.gearman.boot.core;

import org.gearman.client.GearmanJobResult;

public class ActionJobResult implements GearmanJobResult {

	private long denominator = 0;
    private long numerator = 0;
    private byte[] results;
    private byte[] warnings;
    private byte[] exceptions;
    private byte[] handle;
    boolean succeeded = true;
    
    public ActionJobResult(byte[] results){
    	this.results = this.copyArray(results);
    }
    
    public ActionJobResult(boolean succeeded){
    	this.succeeded = succeeded;
    }
    
    public ActionJobResult(boolean succeeded, byte[] results){
    	this.results = this.copyArray(results);
    	this.succeeded = succeeded;
    }
	
	public byte[] getResults() {
		return results;
	}

	public byte[] getWarnings() {
		return warnings;
	}

	public byte[] getExceptions() {
		return exceptions;
	}

	public long getDenominator() {
		return denominator;
	}

	public long getNumerator() {
		return numerator;
	}

	public byte[] getJobHandle() {
		return handle;
	}

	public boolean jobSucceeded() {
		return succeeded;
	}
	
	private byte[] copyArray(byte[] src) {
        if (src == null) {
            return new byte[0];
        }
        byte[] copy = new byte[src.length];
        System.arraycopy(src, 0, copy, 0, src.length);
        return copy;
    }


}
