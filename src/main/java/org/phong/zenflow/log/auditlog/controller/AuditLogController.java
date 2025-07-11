package org.phong.zenflow.log.auditlog.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.log.auditlog.dtos.AuditLogDto;
import org.phong.zenflow.log.auditlog.service.AuditLogService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
class AuditLogController {
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<RestApiResponse<List<AuditLogDto>>> logPage(@ParameterObject Pageable pageable) {
        return RestApiResponse.success(auditLogService.logPage(pageable));
    }
}
