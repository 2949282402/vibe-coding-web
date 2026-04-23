package com.hejulian.blog.agent.exception;

import com.hejulian.blog.exception.BusinessException;

public class AgentToolRuntimeException extends BusinessException {

    public AgentToolRuntimeException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}

