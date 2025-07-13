package org.phong.zenflow.notification.exception;

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
public class NotificationExceptionHandler {
     @ExceptionHandler(NotificationException.class)
     public ResponseEntity<RestApiResponse<Void>> handleNotificationException(NotificationException ex) {
         log.debug("Notification Exception exception occurred: {}", ex.getMessage());

         ApiErrorResponse errorResponse = new ApiErrorResponse(
                 HttpStatus.BAD_REQUEST.value(),
                 "Notification Exception Error",
                 ex.getMessage()
         );

         return RestApiResponse.badRequest(errorResponse, "An error occurred with the Notification: " + ex.getMessage());
     }
}
