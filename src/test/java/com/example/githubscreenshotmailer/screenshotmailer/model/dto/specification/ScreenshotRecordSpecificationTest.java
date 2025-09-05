package com.example.githubscreenshotmailer.screenshotmailer.model.dto.specification;

import com.example.githubscreenshotmailer.screenshotmailer.model.entity.ScreenshotRecordEntity;
import com.example.githubscreenshotmailer.screenshotmailer.model.enums.ScreenshotStatus;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ScreenshotRecordSpecificationTest {

    @SuppressWarnings("unchecked")
    private static <T> Root<T> rootMock() {
        return mock(Root.class);
    }

    @Test
    void hasGithubUsername_buildsEqualPredicate() {
        Root<ScreenshotRecordEntity> root = rootMock();
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        // use raw Path to avoid generic mismatch
        @SuppressWarnings("rawtypes") Path path = mock(Path.class);
        when(root.get("githubUsername")).thenReturn(path);

        Predicate expected = mock(Predicate.class);
        when(cb.equal(path, "octocat")).thenReturn(expected);

        Specification<ScreenshotRecordEntity> spec =
                ScreenshotRecordSpecification.hasGithubUsername("octocat");

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(expected);
        verify(root).get("githubUsername");
        verify(cb).equal(path, "octocat");
    }

    @Test
    void hasRecipientEmail_buildsEqualPredicate() {
        Root<ScreenshotRecordEntity> root = rootMock();
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("rawtypes") Path path = mock(Path.class);
        when(root.get("recipientEmail")).thenReturn(path);

        Predicate expected = mock(Predicate.class);
        when(cb.equal(path, "user@example.com")).thenReturn(expected);

        Specification<ScreenshotRecordEntity> spec =
                ScreenshotRecordSpecification.hasRecipientEmail("user@example.com");

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(expected);
        verify(root).get("recipientEmail");
        verify(cb).equal(path, "user@example.com");
    }

    @Test
    void hasStatus_buildsEqualPredicate() {
        Root<ScreenshotRecordEntity> root = rootMock();
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("rawtypes") Path path = mock(Path.class);
        when(root.get("status")).thenReturn(path);

        Predicate expected = mock(Predicate.class);
        when(cb.equal(path, ScreenshotStatus.SUCCESS)).thenReturn(expected);

        Specification<ScreenshotRecordEntity> spec =
                ScreenshotRecordSpecification.hasStatus(ScreenshotStatus.SUCCESS);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(expected);
        verify(root).get("status");
        verify(cb).equal(path, ScreenshotStatus.SUCCESS);
    }

    @Test
    void sentAfter_buildsGreaterThanPredicate() {
        Root<ScreenshotRecordEntity> root = rootMock();
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("rawtypes") Path path = mock(Path.class);
        when(root.get("sentAt")).thenReturn(path);

        LocalDateTime after = LocalDateTime.now().minusDays(1);
        Predicate expected = mock(Predicate.class);
        // use raw Path is fine with CriteriaBuilder
        when(cb.greaterThan(any(Path.class), eq(after))).thenReturn(expected);

        Specification<ScreenshotRecordEntity> spec =
                ScreenshotRecordSpecification.sentAfter(after);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(expected);
        verify(root).get("sentAt");
        verify(cb).greaterThan(path, after);
    }

    @Test
    void sentBefore_buildsLessThanPredicate() {
        Root<ScreenshotRecordEntity> root = rootMock();
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("rawtypes") Path path = mock(Path.class);
        when(root.get("sentAt")).thenReturn(path);

        LocalDateTime before = LocalDateTime.now().plusDays(1);
        Predicate expected = mock(Predicate.class);
        when(cb.lessThan(any(Path.class), eq(before))).thenReturn(expected);

        Specification<ScreenshotRecordEntity> spec =
                ScreenshotRecordSpecification.sentBefore(before);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(expected);
        verify(root).get("sentAt");
        verify(cb).lessThan(path, before);
    }

    @Test
    void sentBetween_buildsBetweenPredicate() {
        Root<ScreenshotRecordEntity> root = rootMock();
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("rawtypes") Path path = mock(Path.class);
        when(root.get("sentAt")).thenReturn(path);

        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now();

        Predicate expected = mock(Predicate.class);
        when(cb.between(any(Path.class), eq(start), eq(end))).thenReturn(expected);

        Specification<ScreenshotRecordEntity> spec =
                ScreenshotRecordSpecification.sentBetween(start, end);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(expected);
        verify(root).get("sentAt");
        verify(cb).between(path, start, end);
    }

    @Test
    void fileSizeGte_buildsGreaterThanOrEqualToPredicate() {
        Root<ScreenshotRecordEntity> root = rootMock();
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("rawtypes") Path path = mock(Path.class);
        when(root.get("fileSizeBytes")).thenReturn(path);

        Predicate expected = mock(Predicate.class);
        when(cb.greaterThanOrEqualTo(any(Path.class), eq(1024L))).thenReturn(expected);

        Specification<ScreenshotRecordEntity> spec =
                ScreenshotRecordSpecification.fileSizeGte(1024L);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(expected);
        verify(root).get("fileSizeBytes");
        verify(cb).greaterThanOrEqualTo(path, 1024L);
    }

    @Test
    void fileSizeLte_buildsLessThanOrEqualToPredicate() {
        Root<ScreenshotRecordEntity> root = rootMock();
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("rawtypes") Path path = mock(Path.class);
        when(root.get("fileSizeBytes")).thenReturn(path);

        Predicate expected = mock(Predicate.class);
        when(cb.lessThanOrEqualTo(any(Path.class), eq(2048L))).thenReturn(expected);

        Specification<ScreenshotRecordEntity> spec =
                ScreenshotRecordSpecification.fileSizeLte(2048L);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(expected);
        verify(root).get("fileSizeBytes");
        verify(cb).lessThanOrEqualTo(path, 2048L);
    }

    @Test
    void fileNameLike_buildsLowerLikePredicate_withPercentWrappedKeyword() {
        Root<ScreenshotRecordEntity> root = rootMock();
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("rawtypes") Path fileNamePath = mock(Path.class);
        when(root.get("fileName")).thenReturn(fileNamePath);

        // lower(path) -> Expression<String>
        @SuppressWarnings("rawtypes") Expression lowered = mock(Expression.class);
        when(cb.lower(fileNamePath)).thenReturn(lowered);

        ArgumentCaptor<String> likeCaptor = ArgumentCaptor.forClass(String.class);

        Predicate expected = mock(Predicate.class);
        when(cb.like(eq(lowered), likeCaptor.capture())).thenReturn(expected);

        Specification<ScreenshotRecordEntity> spec =
                ScreenshotRecordSpecification.fileNameLike("HeLLo.PNG");

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(expected);
        assertThat(likeCaptor.getValue()).isEqualTo("%hello.png%");
        verify(root).get("fileName");
        verify(cb).lower(fileNamePath);
        verify(cb).like(eq(lowered), anyString());
    }

    @Test
    void search_buildsOrOfThreeLowerLikePredicates() {
        Root<ScreenshotRecordEntity> root = rootMock();
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        @SuppressWarnings("rawtypes") Path userPath = mock(Path.class);
        @SuppressWarnings("rawtypes") Path emailPath = mock(Path.class);
        @SuppressWarnings("rawtypes") Path namePath = mock(Path.class);
        when(root.get("githubUsername")).thenReturn(userPath);
        when(root.get("recipientEmail")).thenReturn(emailPath);
        when(root.get("fileName")).thenReturn(namePath);

        @SuppressWarnings("rawtypes") Expression lowerUser = mock(Expression.class);
        @SuppressWarnings("rawtypes") Expression lowerEmail = mock(Expression.class);
        @SuppressWarnings("rawtypes") Expression lowerName = mock(Expression.class);
        when(cb.lower(userPath)).thenReturn(lowerUser);
        when(cb.lower(emailPath)).thenReturn(lowerEmail);
        when(cb.lower(namePath)).thenReturn(lowerName);

        Predicate p1 = mock(Predicate.class);
        Predicate p2 = mock(Predicate.class);
        Predicate p3 = mock(Predicate.class);

        ArgumentCaptor<String> like1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> like2 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> like3 = ArgumentCaptor.forClass(String.class);

        when(cb.like(eq(lowerUser), like1.capture())).thenReturn(p1);
        when(cb.like(eq(lowerEmail), like2.capture())).thenReturn(p2);
        when(cb.like(eq(lowerName), like3.capture())).thenReturn(p3);

        Predicate or = mock(Predicate.class);
        when(cb.or(p1, p2, p3)).thenReturn(or);

        Specification<ScreenshotRecordEntity> spec =
                ScreenshotRecordSpecification.search("OctoCAT@Example.com");

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isSameAs(or);
        assertThat(like1.getValue()).isEqualTo("%octocat@example.com%");
        assertThat(like2.getValue()).isEqualTo("%octocat@example.com%");
        assertThat(like3.getValue()).isEqualTo("%octocat@example.com%");

        verify(root).get("githubUsername");
        verify(root).get("recipientEmail");
        verify(root).get("fileName");

        verify(cb).lower(userPath);
        verify(cb).lower(emailPath);
        verify(cb).lower(namePath);

        verify(cb).like(eq(lowerUser), anyString());
        verify(cb).like(eq(lowerEmail), anyString());
        verify(cb).like(eq(lowerName), anyString());
        verify(cb).or(p1, p2, p3);
    }
}