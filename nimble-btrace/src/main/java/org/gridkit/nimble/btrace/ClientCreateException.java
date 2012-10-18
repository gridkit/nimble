package org.gridkit.nimble.btrace;

@SuppressWarnings("serial")
public class ClientCreateException extends Exception {
    public ClientCreateException(String message) {
        super(message);
    }

    public ClientCreateException(String message, Throwable cause) {
        super(message, cause);
    }
}
