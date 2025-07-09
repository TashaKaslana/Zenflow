package org.phong.zenflow.core.superbase;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.util.UUID;

@Getter
@MappedSuperclass
public abstract class BaseIdEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;
}
