package org.phong.zenflow.log.systemlog.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.log.systemlog.dto.SystemLogDto;
import org.phong.zenflow.log.systemlog.enums.SystemLogType;
import org.phong.zenflow.log.systemlog.service.SystemLogService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RestController
@RequestMapping("/system-logs")
@RequiredArgsConstructor
class SystemLogController {
    private final SystemLogService systemLogService;

    @GetMapping()
    public ResponseEntity<RestApiResponse<List<SystemLogDto>>> getSystemLogService(@ParameterObject Pageable pageable, SystemLogType type) {
        return RestApiResponse.success(systemLogService.getLogsByType(pageable, type));
    }
}
