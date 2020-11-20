package com.ibm.distributedlock.exception;

/**
 * LockException
 * @author seanyu
 */
public class LockException extends RuntimeException {

    private static final long serialVersionUID = -4147467240172878091L;

    public LockException() {
        super();
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockException(String message) {
        super(message);
    }

    public LockException(Throwable cause) {
        super(cause);
    }
}
