package org.jeecg.modules.flowable.common;

public class FlowableException extends RuntimeException {

    public FlowableException(String message) {
        super(message);
    }

    public FlowableException(String message, Throwable cause) {
        super(message, cause);
    }
}
