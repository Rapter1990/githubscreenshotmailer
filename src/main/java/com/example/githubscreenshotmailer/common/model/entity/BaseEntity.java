package com.example.githubscreenshotmailer.common.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Abstract base class for JPA entities, providing common auditing fields.
 * This class should be extended by all entities that require auditing.
 */
@Getter
@Setter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {

    @Column(name = "CREATED_AT")
    protected LocalDateTime createdAt;

    /**
     * Callback method invoked before the entity is persisted.
     * Sets {@code createdAt} to the current system time if not already set.
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

}
