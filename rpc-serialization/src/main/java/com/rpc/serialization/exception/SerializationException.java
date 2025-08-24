package com.rpc.serialization.exception;

/**
 * @author 何杰
 * @version 1.0
 */
public class SerializationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SerializationException() {
        super();
    }

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerializationException(Throwable cause) {
        super(cause);
    }
}
