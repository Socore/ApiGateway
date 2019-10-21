package com.hx.vr.ch.gateway.exception;

public class FastRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 5090943123199837961L;

	private int code=500;
	public FastRuntimeException(){
        super();
    }
	

    public FastRuntimeException( String desc){
        super(desc);
    }
    
    public FastRuntimeException(int code, String desc){
        super(desc);
        this.code=code;
    }

    public FastRuntimeException(String desc, Throwable cause){
        super(desc, cause);
    }

    public FastRuntimeException(Throwable cause){
        super(cause);
    }
    
    public Throwable 	fillInStackTrace() {
		return null;
	}
    
    public int getCode() {
    	return code;
    }
}
