package com.example.githubscreenshotmailer.screenshotmailer.model.mapper;

import com.example.githubscreenshotmailer.screenshotmailer.model.ScreenshotRecord;
import com.example.githubscreenshotmailer.screenshotmailer.model.dto.response.ScreenshotResponse;
import com.example.githubscreenshotmailer.screenshotmailer.model.enums.ScreenshotStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScreenshotRecordToScreenshotResponseMapperTest {

    private static ScreenshotRecordToScreenshotResponseMapper mapper;

    @BeforeAll
    static void setUp() {
        mapper = ScreenshotRecordToScreenshotResponseMapper.initialize();
    }

    // ---------- mapToResponse (single) ----------

    @Test
    void mapToResponse_mapsAllFields_andConvertsStatusToName() {
        // Given
        LocalDateTime sentAt = LocalDateTime.of(2025, 1, 1, 12, 0);
        ScreenshotRecord record = new ScreenshotRecord(
                "img-100",
                "octocat",
                "user@example.com",
                "octo.png",
                "/screenshots/octo.png",
                12_345L,
                sentAt,
                ScreenshotStatus.SUCCESS
        );

        // When
        ScreenshotResponse resp = mapper.mapToResponse(record);

        // Then
        assertThat(resp).isNotNull();
        assertThat(prop(resp, "imageId")).isEqualTo("img-100");
        assertThat(prop(resp, "githubUsername")).isEqualTo("octocat");
        assertThat(prop(resp, "recipientEmail")).isEqualTo("user@example.com");
        assertThat(prop(resp, "fileName")).isEqualTo("octo.png");
        assertThat(prop(resp, "path")).isEqualTo("/screenshots/octo.png");
        assertThat(prop(resp, "fileSize")).isEqualTo(12_345L);
        assertThat(prop(resp, "sentAt")).isEqualTo(sentAt);
        // status is mapped to String (enum name)
        assertThat(prop(resp, "status")).isEqualTo("SUCCESS");
    }

    @Test
    void mapToResponse_returnsNull_whenInputIsNull() {
        assertThat(mapper.mapToResponse(null)).isNull();
    }

    @Test
    void mapToResponse_mapsNullStatus_toNull() {
        // Given
        ScreenshotRecord record = new ScreenshotRecord(
                "img-101",
                "octo2",
                "u2@example.com",
                "o2.png",
                "/p/o2.png",
                1L,
                LocalDateTime.of(2025, 2, 2, 2, 2),
                null // status is null
        );

        // When
        ScreenshotResponse resp = mapper.mapToResponse(record);

        // Then
        assertThat(resp).isNotNull();
        assertThat(prop(resp, "status")).isNull(); // string status is null when enum is null
    }

    // ---------- map(Collection) ----------

    @Test
    void map_collection_returnsEmptyList_whenInputIsNull() {
        assertThat(mapper.map((List<ScreenshotRecord>) null)).isEmpty();
    }

    @Test
    void map_collection_filtersOutNulls_andMapsValidOnes() {
        // Given
        ScreenshotRecord a = new ScreenshotRecord(
                "img-A", "a", "a@ex.com", "a.png", "/a/a.png", 1L,
                LocalDateTime.of(2025, 2, 2, 2, 2), ScreenshotStatus.SUCCESS
        );
        ScreenshotRecord b = null; // should be filtered out
        ScreenshotRecord c = new ScreenshotRecord(
                "img-C", "c", "c@ex.com", "c.png", "/c/c.png", 3L,
                LocalDateTime.of(2025, 3, 3, 3, 3), ScreenshotStatus.FAILED
        );

        // When: use a list impl that allows nulls
        List<ScreenshotResponse> result = mapper.map(Arrays.asList(a, b, c));

        // Then
        assertThat(result).hasSize(2);
        assertThat(prop(result.get(0), "imageId")).isEqualTo("img-A");
        assertThat(prop(result.get(1), "imageId")).isEqualTo("img-C");
        assertThat(prop(result.get(0), "status")).isEqualTo("SUCCESS");
        assertThat(prop(result.get(1), "status")).isEqualTo("FAILED");
    }

    // ---------- tiny reflection helper (supports record or POJO getters) ----------

    private static Object prop(Object target, String name) {
        try {
            // record-style accessor: fieldName()
            Method m = target.getClass().getMethod(name);
            return m.invoke(target);
        } catch (NoSuchMethodException e) {
            try {
                // bean-style getter: getFieldName()
                String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                Method m = target.getClass().getMethod(getter);
                return m.invoke(target);
            } catch (Exception inner) {
                throw new AssertionError("Cannot read property '" + name + "' from " + target.getClass(), inner);
            }
        } catch (Exception e) {
            throw new AssertionError("Error invoking accessor for '" + name + "'", e);
        }
    }

}