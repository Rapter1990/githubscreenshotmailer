package com.example.githubscreenshotmailer.common.model.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BaseEntityTest {

    // Simple concrete entity for testing
    static class TestEntity extends BaseEntity {
        // no extra fields / methods needed for this test
    }

    @Test
    void prePersist_setsCreatedAt_whenNull() {
        // Given
        TestEntity e = new TestEntity();
        e.setCreatedAt(null);

        // When
        LocalDateTime before = LocalDateTime.now();
        e.prePersist();
        LocalDateTime after = LocalDateTime.now();

        // Then
        assertThat(e.getCreatedAt()).isNotNull();
        // createdAt is within [before, after]
        assertThat(!e.getCreatedAt().isBefore(before) && !e.getCreatedAt().isAfter(after)).isTrue();
    }

    @Test
    void prePersist_overwritesExistingTimestamp_currentImplementation() {
        // Given: an already-set timestamp
        TestEntity e = new TestEntity();
        LocalDateTime original = LocalDateTime.now().minusDays(1);
        e.setCreatedAt(original);

        // When
        e.prePersist();

        // Then: current implementation overwrites the value
        assertThat(e.getCreatedAt()).isNotNull();
        assertThat(e.getCreatedAt()).isAfter(original);
    }

}