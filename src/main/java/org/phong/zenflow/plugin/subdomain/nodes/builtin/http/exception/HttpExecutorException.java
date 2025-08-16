package org.phong.zenflow.plugin.subdomain.nodes.builtin.http.exception;

import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;

public class HttpExecutorException extends ExecutorException {
    public HttpExecutorException(String message) {
        super(message);
    }
}
