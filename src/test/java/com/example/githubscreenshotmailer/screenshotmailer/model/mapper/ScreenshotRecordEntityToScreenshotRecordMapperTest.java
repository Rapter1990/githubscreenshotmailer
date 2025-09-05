package com.example.githubscreenshotmailer.screenshotmailer.model.mapper;

import com.example.githubscreenshotmailer.screenshotmailer.model.ScreenshotRecord;
import com.example.githubscreenshotmailer.screenshotmailer.model.entity.ScreenshotRecordEntity;
import com.example.githubscreenshotmailer.screenshotmailer.model.enums.ScreenshotStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScreenshotRecordEntityToScreenshotRecordMapperTest {

    private ScreenshotRecordEntityToScreenshotRecordMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = ScreenshotRecordEntityToScreenshotRecordMapper.initialize();
        assertThat(mapper).isNotNull();
    }

    // ---------- mapFromEntity ----------

    @Test
    void mapFromEntity_returnsNull_whenEntityIsNull() {
        ScreenshotRecord result = mapper.mapFromEntity(null);
        assertThat(result).isNull();
    }

    @Test
    void mapFromEntity_mapsAllFields_correctly() {
        // Given
        ScreenshotRecordEntity entity = newEntity(
                "img-1",
                "octocat",
                "user@example.com",
                "octocat.png",
                "/abs/path/octocat.png",
                12_345L,
                LocalDateTime.of(2025, 1, 1, 12, 0),
                ScreenshotStatus.SUCCESS
        );

        // When
        ScreenshotRecord result = mapper.mapFromEntity(entity);

        // Then (field-by-field)
        assertThat(result).isNotNull();
        assertThat(result.imageId()).isEqualTo("img-1");
        assertThat(result.githubUsername()).isEqualTo("octocat");
        assertThat(result.recipientEmail()).isEqualTo("user@example.com");
        assertThat(result.fileName()).isEqualTo("octocat.png");
        assertThat(result.path()).isEqualTo("/abs/path/octocat.png");
        assertThat(result.fileSize()).isEqualTo(12_345L);
        assertThat(result.sentAt()).isEqualTo(LocalDateTime.of(2025, 1, 1, 12, 0));
        assertThat(result.status()).isEqualTo(ScreenshotStatus.SUCCESS);
    }

    // ---------- map(ScreenshotRecordEntity) -> delegates to mapFromEntity ----------

    @Test
    void map_singleEntity_delegatesToMapFromEntity() {
        // Given
        ScreenshotRecordEntity entity = newEntity(
                "img-2",
                "monalisa",
                "mona@example.com",
                "mona.png",
                "/p/mona.png",
                2222L,
                LocalDateTime.of(2025, 5, 5, 5, 5),
                ScreenshotStatus.SUCCESS
        );

        // When
        ScreenshotRecord result = mapper.map(entity);

        // Then (equals to mapFromEntity result)
        ScreenshotRecord expected = mapper.mapFromEntity(entity);
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    // ---------- map(Collection<ScreenshotRecordEntity>) ----------

    @Test
    void map_collection_returnsEmptyList_whenSourcesNull() {
        List<ScreenshotRecord> result = mapper.map((Collection<ScreenshotRecordEntity>) null);
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void map_collection_filtersOutNulls_andMapsValidOnes() {
        // Given
        ScreenshotRecordEntity a = newEntity(
                "img-A", "a", "a@ex.com", "a.png", "/a/a.png", 1L,
                LocalDateTime.of(2025, 2, 2, 2, 2), ScreenshotStatus.SUCCESS
        );
        ScreenshotRecordEntity b = null; // should be filtered out
        ScreenshotRecordEntity c = newEntity(
                "img-C", "c", "c@ex.com", "c.png", "/c/c.png", 3L,
                LocalDateTime.of(2025, 3, 3, 3, 3), ScreenshotStatus.FAILED
        );

        // When: use a list impl that allows nulls
        List<ScreenshotRecord> result = mapper.map(java.util.Arrays.asList(a, b, c));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).imageId()).isEqualTo("img-A");
        assertThat(result.get(1).imageId()).isEqualTo("img-C");

    }

    @Test
    void map_collection_mapsMultipleEntities_correctly() {
        // Given
        ScreenshotRecordEntity e1 = newEntity(
                "img-10", "u1", "u1@ex.com", "f1.png", "/p/f1.png", 10L,
                LocalDateTime.of(2025, 1, 10, 10, 10), ScreenshotStatus.SUCCESS
        );
        ScreenshotRecordEntity e2 = newEntity(
                "img-11", "u2", "u2@ex.com", "f2.png", "/p/f2.png", 11L,
                LocalDateTime.of(2025, 1, 11, 11, 11), ScreenshotStatus.SUCCESS
        );

        // When
        List<ScreenshotRecord> result = mapper.map(List.of(e1, e2));

        // Then
        assertThat(result).hasSize(2);

        ScreenshotRecord r1 = result.get(0);
        assertThat(r1.imageId()).isEqualTo("img-10");
        assertThat(r1.githubUsername()).isEqualTo("u1");
        assertThat(r1.recipientEmail()).isEqualTo("u1@ex.com");
        assertThat(r1.fileName()).isEqualTo("f1.png");
        assertThat(r1.path()).isEqualTo("/p/f1.png");
        assertThat(r1.fileSize()).isEqualTo(10L);
        assertThat(r1.sentAt()).isEqualTo(LocalDateTime.of(2025, 1, 10, 10, 10));
        assertThat(r1.status()).isEqualTo(ScreenshotStatus.SUCCESS);

        ScreenshotRecord r2 = result.get(1);
        assertThat(r2.imageId()).isEqualTo("img-11");
        assertThat(r2.githubUsername()).isEqualTo("u2");
        assertThat(r2.recipientEmail()).isEqualTo("u2@ex.com");
        assertThat(r2.fileName()).isEqualTo("f2.png");
        assertThat(r2.path()).isEqualTo("/p/f2.png");
        assertThat(r2.fileSize()).isEqualTo(11L);
        assertThat(r2.sentAt()).isEqualTo(LocalDateTime.of(2025, 1, 11, 11, 11));
        assertThat(r2.status()).isEqualTo(ScreenshotStatus.SUCCESS);
    }

    // ---------- helpers ----------

    private static ScreenshotRecordEntity newEntity(String id,
                                                    String username,
                                                    String email,
                                                    String fileName,
                                                    String filePath,
                                                    long size,
                                                    LocalDateTime sentAt,
                                                    ScreenshotStatus status) {
        ScreenshotRecordEntity e = new ScreenshotRecordEntity();
        e.setId(id);
        e.setGithubUsername(username);
        e.setRecipientEmail(email);
        e.setFileName(fileName);
        e.setFilePath(filePath);
        e.setFileSizeBytes(size);
        e.setSentAt(sentAt);
        e.setStatus(status);
        return e;
    }

}