package com.example.githubscreenshotmailer.screenshotmailer.model.dto.request;

import com.example.githubscreenshotmailer.screenshotmailer.model.dto.specification.ScreenshotRecordSpecification;
import com.example.githubscreenshotmailer.screenshotmailer.model.entity.ScreenshotRecordEntity;
import com.example.githubscreenshotmailer.screenshotmailer.model.enums.ScreenshotStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;


class ListScreenshotRecordRequestTest {

    private MockedStatic<ScreenshotRecordSpecification> specStatic;

    private static final class RecordingSpec implements Specification<ScreenshotRecordEntity> {
        boolean invoked = false;
        final Predicate predicate;

        RecordingSpec(Predicate predicate) {
            this.predicate = predicate;
        }

        @Override
        public Predicate toPredicate(Root<ScreenshotRecordEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
            invoked = true;
            return predicate;
        }
    }

    @AfterEach
    void tearDown() {
        if (specStatic != null) specStatic.close();
    }

    @Test
    void toSpecification_returnsNull_whenFilterNull() {
        ListScreenshotRecordRequest request = new ListScreenshotRecordRequest();
        request.setFilter(null);

        Specification<ScreenshotRecordEntity> spec = request.toSpecification();
        assertThat(spec).isNull();
    }

    @Test
    void toSpecification_buildsSingleSpecs_forSimpleFields() {
        // Given
        ListScreenshotRecordRequest.Filter f = new ListScreenshotRecordRequest.Filter();
        f.setGithubUsername("octocat");
        f.setRecipientEmail("user@example.com");
        f.setStatus(ScreenshotStatus.SUCCESS);

        ListScreenshotRecordRequest req = new ListScreenshotRecordRequest();
        req.setFilter(f);

        // Static mocks for three single-field specs
        specStatic = mockStatic(ScreenshotRecordSpecification.class);
        RecordingSpec userSpec = new RecordingSpec(mock(Predicate.class));
        RecordingSpec emailSpec = new RecordingSpec(mock(Predicate.class));
        RecordingSpec statusSpec = new RecordingSpec(mock(Predicate.class));

        specStatic.when(() -> ScreenshotRecordSpecification.hasGithubUsername("octocat"))
                .thenReturn(userSpec);
        specStatic.when(() -> ScreenshotRecordSpecification.hasRecipientEmail("user@example.com"))
                .thenReturn(emailSpec);
        specStatic.when(() -> ScreenshotRecordSpecification.hasStatus(ScreenshotStatus.SUCCESS))
                .thenReturn(statusSpec);

        Specification<ScreenshotRecordEntity> combined = req.toSpecification();
        assertThat(combined).isNotNull();

        // When: call the reduced spec
        Root<ScreenshotRecordEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        combined.toPredicate(root, query, cb);

        // Then: each underlying spec was used
        assertThat(userSpec.invoked).isTrue();
        assertThat(emailSpec.invoked).isTrue();
        assertThat(statusSpec.invoked).isTrue();

        specStatic.verify(() -> ScreenshotRecordSpecification.hasGithubUsername("octocat"));
        specStatic.verify(() -> ScreenshotRecordSpecification.hasRecipientEmail("user@example.com"));
        specStatic.verify(() -> ScreenshotRecordSpecification.hasStatus(ScreenshotStatus.SUCCESS));
    }

    @Test
    void toSpecification_usesSentBetween_whenBothDatesPresent_otherwise_AfterOrBefore() {
        // Both dates -> sentBetween()
        ListScreenshotRecordRequest.Filter f1 = new ListScreenshotRecordRequest.Filter();
        LocalDateTime from = LocalDateTime.now().minusDays(3);
        LocalDateTime to   = LocalDateTime.now().minusDays(1);
        f1.setSentAtFrom(from);
        f1.setSentAtTo(to);

        ListScreenshotRecordRequest req1 = new ListScreenshotRecordRequest();
        req1.setFilter(f1);

        specStatic = mockStatic(ScreenshotRecordSpecification.class);
        RecordingSpec betweenSpec = new RecordingSpec(mock(Predicate.class));
        specStatic.when(() -> ScreenshotRecordSpecification.sentBetween(from, to))
                .thenReturn(betweenSpec);

        Specification<ScreenshotRecordEntity> s1 = req1.toSpecification();
        assertThat(s1).isNotNull();

        s1.toPredicate(mock(Root.class), mock(CriteriaQuery.class), mock(CriteriaBuilder.class));
        assertThat(betweenSpec.invoked).isTrue();
        specStatic.verify(() -> ScreenshotRecordSpecification.sentBetween(from, to));
        specStatic.close();

        // Only from -> sentAfter()
        ListScreenshotRecordRequest.Filter f2 = new ListScreenshotRecordRequest.Filter();
        f2.setSentAtFrom(from);

        ListScreenshotRecordRequest req2 = new ListScreenshotRecordRequest();
        req2.setFilter(f2);

        specStatic = mockStatic(ScreenshotRecordSpecification.class);
        RecordingSpec afterSpec = new RecordingSpec(mock(Predicate.class));
        specStatic.when(() -> ScreenshotRecordSpecification.sentAfter(from))
                .thenReturn(afterSpec);

        Specification<ScreenshotRecordEntity> s2 = req2.toSpecification();
        s2.toPredicate(mock(Root.class), mock(CriteriaQuery.class), mock(CriteriaBuilder.class));
        assertThat(afterSpec.invoked).isTrue();
        specStatic.verify(() -> ScreenshotRecordSpecification.sentAfter(from));
        specStatic.close();

        // Only to -> sentBefore()
        ListScreenshotRecordRequest.Filter f3 = new ListScreenshotRecordRequest.Filter();
        f3.setSentAtTo(to);

        ListScreenshotRecordRequest req3 = new ListScreenshotRecordRequest();
        req3.setFilter(f3);

        specStatic = mockStatic(ScreenshotRecordSpecification.class);
        RecordingSpec beforeSpec = new RecordingSpec(mock(Predicate.class));
        specStatic.when(() -> ScreenshotRecordSpecification.sentBefore(to))
                .thenReturn(beforeSpec);

        Specification<ScreenshotRecordEntity> s3 = req3.toSpecification();
        s3.toPredicate(mock(Root.class), mock(CriteriaQuery.class), mock(CriteriaBuilder.class));
        assertThat(beforeSpec.invoked).isTrue();
        specStatic.verify(() -> ScreenshotRecordSpecification.sentBefore(to));
    }

    @Test
    void toSpecification_buildsSizeAndLikeAndSearchSpecs_whenProvided() {
        // Given
        ListScreenshotRecordRequest.Filter f = new ListScreenshotRecordRequest.Filter();
        f.setMinFileSizeBytes(1024L);
        f.setMaxFileSizeBytes(4096L);
        f.setFileNameContains("png");
        f.setKeyword("octo");

        ListScreenshotRecordRequest req = new ListScreenshotRecordRequest();
        req.setFilter(f);

        // Static mocks & recording specs
        specStatic = mockStatic(ScreenshotRecordSpecification.class);

        RecordingSpec gteSpec = new RecordingSpec(mock(Predicate.class));
        RecordingSpec lteSpec = new RecordingSpec(mock(Predicate.class));
        RecordingSpec likeSpec = new RecordingSpec(mock(Predicate.class));
        RecordingSpec searchSpec = new RecordingSpec(mock(Predicate.class));

        specStatic.when(() -> ScreenshotRecordSpecification.fileSizeGte(1024L)).thenReturn(gteSpec);
        specStatic.when(() -> ScreenshotRecordSpecification.fileSizeLte(4096L)).thenReturn(lteSpec);
        specStatic.when(() -> ScreenshotRecordSpecification.fileNameLike("png")).thenReturn(likeSpec);
        specStatic.when(() -> ScreenshotRecordSpecification.search("octo")).thenReturn(searchSpec);

        Specification<ScreenshotRecordEntity> combined = req.toSpecification();
        assertThat(combined).isNotNull();

        // When
        combined.toPredicate(mock(Root.class), mock(CriteriaQuery.class), mock(CriteriaBuilder.class));

        // Then
        assertThat(gteSpec.invoked).isTrue();
        assertThat(lteSpec.invoked).isTrue();
        assertThat(likeSpec.invoked).isTrue();
        assertThat(searchSpec.invoked).isTrue();

        specStatic.verify(() -> ScreenshotRecordSpecification.fileSizeGte(1024L));
        specStatic.verify(() -> ScreenshotRecordSpecification.fileSizeLte(4096L));
        specStatic.verify(() -> ScreenshotRecordSpecification.fileNameLike("png"));
        specStatic.verify(() -> ScreenshotRecordSpecification.search("octo"));
    }

    @Test
    void toSpecification_ignoresBlankStrings_andNulls() {
        // Given: blanks & nulls everywhere -> results in empty list -> returns null
        ListScreenshotRecordRequest.Filter f = new ListScreenshotRecordRequest.Filter();
        f.setGithubUsername("   ");
        f.setRecipientEmail(null);
        f.setStatus(null);
        f.setFileNameContains(" ");
        f.setKeyword("");

        ListScreenshotRecordRequest req = new ListScreenshotRecordRequest();
        req.setFilter(f);

        // No static mocking needed; nothing should be called
        Specification<ScreenshotRecordEntity> spec = req.toSpecification();
        assertThat(spec).isNull();
    }

}