package org.phong.zenflow.core.superbase;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
@MappedSuperclass
public abstract class BaseIdEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @NotNull
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
}
