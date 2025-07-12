package org.phong.zenflow.plugin.subdomain.executors.builtin.data_transformer.exception;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.responses.ApiErrorResponse;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@AllArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataTransformerExecutorExceptionHandler {
     @ExceptionHandler(DataTransformerExecutorException.class)
     public ResponseEntity<RestApiResponse<Void>> handleExecutorException(DataTransformerExecutorException ex) {
         log.debug("Data Transformer Executor exception occurred: {}", ex.getMessage());

         ApiErrorResponse errorResponse = new ApiErrorResponse(
                 HttpStatus.BAD_REQUEST.value(),
                 "Data Transformer Executor Error",
                 ex.getMessage()
         );

         return RestApiResponse.badRequest(errorResponse, "An error occurred while executing the plugin node: " + ex.getMessage());
     }
}
