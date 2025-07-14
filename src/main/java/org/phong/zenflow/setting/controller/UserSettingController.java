package org.phong.zenflow.setting.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.setting.dto.CreateUserSettingRequest;
import org.phong.zenflow.setting.dto.UpdateUserSettingRequest;
import org.phong.zenflow.setting.dto.UserSettingDto;
import org.phong.zenflow.setting.service.UserSettingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user-settings")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingService userSettingService;

    @PostMapping
    public ResponseEntity<RestApiResponse<UserSettingDto>> createOrUpdateUserSetting(@Valid @RequestBody CreateUserSettingRequest request) {
        UserSettingDto userSetting = userSettingService.createOrUpdateUserSetting(request);
        return RestApiResponse.created(userSetting, "User setting created/updated successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestApiResponse<UserSettingDto>> getUserSettingById(@PathVariable UUID id) {
        UserSettingDto userSetting = userSettingService.findById(id);
        return RestApiResponse.success(userSetting, "User setting retrieved successfully");
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<RestApiResponse<UserSettingDto>> getUserSettingByUserId(@PathVariable UUID userId) {
        UserSettingDto userSetting = userSettingService.findByUserIdOrDefault(userId);
        if (userSetting == null) {
            // Return empty settings if none exist
            UserSettingDto defaultSetting = new UserSettingDto(null, null, null, userId, Collections.emptyMap());
            return RestApiResponse.success(defaultSetting, "Default user setting returned");
        }
        return RestApiResponse.success(userSetting, "User setting retrieved successfully");
    }

    @GetMapping("/user/{userId}/strict")
    public ResponseEntity<RestApiResponse<UserSettingDto>> getUserSettingByUserIdStrict(@PathVariable UUID userId) {
        UserSettingDto userSetting = userSettingService.findByUserId(userId);
        return RestApiResponse.success(userSetting, "User setting retrieved successfully");
    }

    @GetMapping
    public ResponseEntity<RestApiResponse<List<UserSettingDto>>> getAllUserSettings() {
        List<UserSettingDto> userSettings = userSettingService.findAll();
        return RestApiResponse.success(userSettings, "User settings retrieved successfully");
    }

    @GetMapping("/paginated")
    public ResponseEntity<RestApiResponse<List<UserSettingDto>>> getAllUserSettingsPaginated(Pageable pageable) {
        Page<UserSettingDto> userSettings = userSettingService.findAll(pageable);
        return RestApiResponse.success(userSettings, "User settings retrieved successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestApiResponse<UserSettingDto>> updateUserSetting(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserSettingRequest request) {
        UserSettingDto updatedUserSetting = userSettingService.updateUserSetting(id, request);
        return RestApiResponse.success(updatedUserSetting, "User setting updated successfully");
    }

    @PutMapping("/user/{userId}")
    public ResponseEntity<RestApiResponse<UserSettingDto>> updateUserSettingByUserId(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserSettingRequest request) {
        UserSettingDto updatedUserSetting = userSettingService.updateUserSettingByUserId(userId, request);
        return RestApiResponse.success(updatedUserSetting, "User setting updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RestApiResponse<Void>> deleteUserSetting(@PathVariable UUID id) {
        userSettingService.deleteUserSetting(id);
        return RestApiResponse.noContent();
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<RestApiResponse<Void>> deleteUserSettingByUserId(@PathVariable UUID userId) {
        userSettingService.deleteUserSettingByUserId(userId);
        return RestApiResponse.noContent();
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<RestApiResponse<Boolean>> checkUserSettingExists(@PathVariable UUID id) {
        boolean exists = userSettingService.existsById(id);
        return RestApiResponse.success(exists, "User setting existence checked");
    }

    @GetMapping("/user/{userId}/exists")
    public ResponseEntity<RestApiResponse<Boolean>> checkUserSettingExistsByUserId(@PathVariable UUID userId) {
        boolean exists = userSettingService.existsByUserId(userId);
        return RestApiResponse.success(exists, "User setting existence checked");
    }
}
