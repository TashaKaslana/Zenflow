package org.phong.zenflow.setting.infrastructure.persistence.repository;

import org.phong.zenflow.setting.infrastructure.persistence.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserSettingRepository extends JpaRepository<UserSetting, UUID> {

    /**
     * Find user setting by user ID
     */
    @Query("SELECT us FROM UserSetting us WHERE us.user.id = :userId")
    Optional<UserSetting> findByUserId(@Param("userId") UUID userId);

    /**
     * Check if user setting exists by user ID
     */
    @Query("SELECT COUNT(us) > 0 FROM UserSetting us WHERE us.user.id = :userId")
    boolean existsByUserId(@Param("userId") UUID userId);

    /**
     * Delete user setting by user ID
     */
    void deleteByUserId(UUID userId);
}
