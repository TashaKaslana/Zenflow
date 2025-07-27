package org.phong.zenflow.plugin.subdomain.schema.exception;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.responses.ApiErrorResponse;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.core.utils.HttpRequestUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
@AllArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaExceptionHandler {
     @ExceptionHandler(NodeSchemaException.class)
     public ResponseEntity<RestApiResponse<Void>> handlePluginException(NodeSchemaException ex, WebRequest request) {
         log.debug("Schema exception occurred: {}", ex.getMessage());

         ApiErrorResponse errorResponse = new ApiErrorResponse(
                 HttpStatus.BAD_REQUEST.value(),
                 "Schema Error",
                 HttpRequestUtils.getRequestPath(request),
                 ex.getMessage()
         );

         return RestApiResponse.badRequest(errorResponse, "An error occurred with the Schema" );
     }

    @ExceptionHandler(NodeSchemaMissingException.class)
    public ResponseEntity<RestApiResponse<Void>> handlePluginException(NodeSchemaMissingException ex, WebRequest request) {
        log.debug("Node Schema Missing exception occurred: {}", ex.getMessage());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "One or more plugin nodes are unavailable or incorrectly configured.",
                HttpRequestUtils.getRequestPath(request),
                ex.getMessage(),
                ex.getMissingFields(),
                null
        );

        return RestApiResponse.badRequest(errorResponse, "Some plugin nodes used in this workflow are not available." );
    }
}
