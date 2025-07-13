package org.phong.zenflow.setting.service;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.setting.dto.CreateUserSettingRequest;
import org.phong.zenflow.setting.dto.UpdateUserSettingRequest;
import org.phong.zenflow.setting.dto.UserSettingDto;
import org.phong.zenflow.setting.exception.UserSettingException;
import org.phong.zenflow.setting.infrastructure.mapstruct.UserSettingMapper;
import org.phong.zenflow.setting.infrastructure.persistence.entity.UserSetting;
import org.phong.zenflow.setting.infrastructure.persistence.repository.UserSettingRepository;
import org.phong.zenflow.user.infrastructure.persistence.entities.User;
import org.phong.zenflow.user.infrastructure.persistence.repositories.UserRepository;
import org.phong.zenflow.user.exception.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSettingService {

    private final UserSettingRepository userSettingRepository;
    private final UserRepository userRepository;
    private final UserSettingMapper userSettingMapper;

    /**
     * Create or update user setting
     */
    @Transactional
    @AuditLog(action = AuditAction.SETTING_UPDATE, targetIdExpression = "returnObject.id")
    public UserSettingDto createOrUpdateUserSetting(CreateUserSettingRequest request) {
        // Validate user exists
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId().toString()));

        // Check if user setting already exists
        return userSettingRepository.findByUserId(request.userId())
                .map(existingUserSetting -> {
                    // Update existing setting
                    existingUserSetting.setSettings(request.settings());
                    UserSetting updated = userSettingRepository.save(existingUserSetting);
                    return userSettingMapper.toDto(updated);
                })
                .orElseGet(() -> {
                    // Create new setting
                    UserSetting userSetting = userSettingMapper.toEntity(request);
                    userSetting.setUser(user);
                    UserSetting saved = userSettingRepository.save(userSetting);
                    return userSettingMapper.toDto(saved);
                });
    }

    /**
     * Find user setting by ID
     */
    public UserSettingDto findById(UUID id) {
        UserSetting userSetting = userSettingRepository.findById(id)
                .orElseThrow(() -> new UserSettingException("User setting not found with id: " + id));
        return userSettingMapper.toDto(userSetting);
    }

    /**
     * Find user setting by user ID
     */
    public UserSettingDto findByUserId(UUID userId) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId.toString());
        }

        UserSetting userSetting = userSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new UserSettingException("User setting not found for user id: " + userId));
        return userSettingMapper.toDto(userSetting);
    }

    /**
     * Find user setting by user ID or return default empty settings
     */
    public UserSettingDto findByUserIdOrDefault(UUID userId) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId.toString());
        }

        return userSettingRepository.findByUserId(userId)
                .map(userSettingMapper::toDto)
                .orElse(null); // Return null if no settings exist, let controller handle default response
    }

    /**
     * Find all user settings
     */
    public List<UserSettingDto> findAll() {
        return userSettingRepository.findAll()
                .stream()
                .map(userSettingMapper::toDto)
                .toList();
    }

    /**
     * Find user settings with pagination
     */
    public Page<UserSettingDto> findAll(Pageable pageable) {
        return userSettingRepository.findAll(pageable)
                .map(userSettingMapper::toDto);
    }

    /**
     * Update user setting
     */
    @Transactional
    @AuditLog(action = AuditAction.SETTING_UPDATE, targetIdExpression = "#id")
    public UserSettingDto updateUserSetting(UUID id, UpdateUserSettingRequest request) {
        UserSetting existingUserSetting = userSettingRepository.findById(id)
                .orElseThrow(() -> new UserSettingException("User setting not found with id: " + id));

        UserSetting updated = userSettingMapper.partialUpdate(request, existingUserSetting);
        UserSetting updatedUserSetting = userSettingRepository.save(updated);
        return userSettingMapper.toDto(updatedUserSetting);
    }

    /**
     * Update user setting by user ID
     */
    @Transactional
    @AuditLog(action = AuditAction.SETTING_UPDATE, targetIdExpression = "returnObject.id")
    public UserSettingDto updateUserSettingByUserId(UUID userId, UpdateUserSettingRequest request) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId.toString());
        }

        UserSetting existingUserSetting = userSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new UserSettingException("User setting not found for user id: " + userId));

        UserSetting updated = userSettingMapper.partialUpdate(request, existingUserSetting);
        UserSetting updatedUserSetting = userSettingRepository.save(updated);
        return userSettingMapper.toDto(updatedUserSetting);
    }

    /**
     * Delete user setting
     */
    @Transactional
    @AuditLog(action = AuditAction.SETTING_UPDATE, targetIdExpression = "#id", description = "Delete user setting")
    public void deleteUserSetting(UUID id) {
        if (!userSettingRepository.existsById(id)) {
            throw new UserSettingException("User setting not found with id: " + id);
        }
        userSettingRepository.deleteById(id);
    }

    /**
     * Delete user setting by user ID
     */
    @Transactional
    @AuditLog(action = AuditAction.SETTING_UPDATE, targetIdExpression = "#userId", description = "Delete user setting by user ID")
    public void deleteUserSettingByUserId(UUID userId) {
        if (!userSettingRepository.existsByUserId(userId)) {
            throw new UserSettingException("User setting not found for user id: " + userId);
        }
        userSettingRepository.deleteByUserId(userId);
    }

    /**
     * Check if user setting exists
     */
    public boolean existsById(UUID id) {
        return userSettingRepository.existsById(id);
    }

    /**
     * Check if user setting exists by user ID
     */
    public boolean existsByUserId(UUID userId) {
        return userSettingRepository.existsByUserId(userId);
    }
}
