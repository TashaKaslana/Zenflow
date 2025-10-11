package org.phong.zenflow.plugin.subdomain.node.definition.decorator.handler.execption;

import org.phong.zenflow.core.services.DebugFlag;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.util.concurrent.CancellationException;

class ExceptionMapping {
    public static ExecutionResult mapException(Exception e, NodeLogPublisher log) {
        return switch (e) {
            case Exception ex when ex instanceof InterruptedException || ex instanceof CancellationException -> {
                Thread.currentThread().interrupt();
                log.warn("Execution was interrupted: " + e.getMessage());
                yield ExecutionResult.error(ExecutionError.INTERRUPTED, e.getMessage(), debug(e));
            }
            case SocketTimeoutException ste -> {
                log.warn("Network timeout occurred: " + ste.getMessage());
                yield ExecutionResult.retry(ste.getMessage(), debug(e));
            }
            case IOException ioe -> {
                log.warn("IO error occurred: " + ioe.getMessage());
                yield ExecutionResult.retry(ioe.getMessage(), debug(e));
            }
            default -> {
                log.error("Non-retriable error occurred: " + e.getMessage());
                yield ExecutionResult.error(ExecutionError.NON_RETRIABLE, e.getMessage(), debug(e));
            }
        };
    }

    private static String debug(Exception e) {
        return DebugFlag.isDebug() ? stacktraceAsString(e) : null;
    }

    private static String stacktraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
