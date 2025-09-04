package com.example.githubscreenshotmailer.screenshotmailer.model.dto.specification;

import com.example.githubscreenshotmailer.screenshotmailer.model.entity.ScreenshotRecordEntity;
import com.example.githubscreenshotmailer.screenshotmailer.model.enums.ScreenshotStatus;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Utility class providing factory methods for creating Spring Data JPA specifications
 * for querying and filtering {@link ScreenshotRecordEntity}.
 */
@UtilityClass
public class ScreenshotRecordSpecification {

    /**
     * Filters screenshots by GitHub username (exact match).
     *
     * @param username the GitHub username to filter by
     * @return Specification for filtering by GitHub username
     */
    public Specification<ScreenshotRecordEntity> hasGithubUsername(String username) {
        return (root, query, cb) ->
                cb.equal(root.get("githubUsername"), username);
    }

    /**
     * Filters screenshots by recipient email (exact match).
     *
     * @param email the recipient email to filter by
     * @return Specification for filtering by recipient email
     */
    public Specification<ScreenshotRecordEntity> hasRecipientEmail(String email) {
        return (root, query, cb) ->
                cb.equal(root.get("recipientEmail"), email);
    }

    /**
     * Filters screenshots by status.
     *
     * @param status the screenshot status
     * @return Specification for filtering by status
     */
    public Specification<ScreenshotRecordEntity> hasStatus(ScreenshotStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    /**
     * Filters screenshots created after the given date/time.
     *
     * @param date lower bound for sentAt
     * @return Specification for filtering
     */
    public Specification<ScreenshotRecordEntity> sentAfter(LocalDateTime date) {
        return (root, query, cb) ->
                cb.greaterThan(root.get("sentAt"), date);
    }

    /**
     * Filters screenshots created before the given date/time.
     *
     * @param date upper bound for sentAt
     * @return Specification for filtering
     */
    public Specification<ScreenshotRecordEntity> sentBefore(LocalDateTime date) {
        return (root, query, cb) ->
                cb.lessThan(root.get("sentAt"), date);
    }

    /**
     * Filters screenshots created between two dates.
     *
     * @param start start of date range
     * @param end   end of date range
     * @return Specification for filtering between two dates
     */
    public Specification<ScreenshotRecordEntity> sentBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) ->
                cb.between(root.get("sentAt"), start, end);
    }

    public Specification<ScreenshotRecordEntity> fileSizeGte(long bytes) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("fileSizeBytes"), bytes);
    }

    public Specification<ScreenshotRecordEntity> fileSizeLte(long bytes) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("fileSizeBytes"), bytes);
    }

    public Specification<ScreenshotRecordEntity> fileNameLike(String keyword) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("fileName")), "%" + keyword.toLowerCase() + "%");
    }

    /**
     * Performs a keyword search across GitHub username, email, and file name.
     *
     * @param keyword search keyword
     * @return Specification for keyword search
     */
    public Specification<ScreenshotRecordEntity> search(String keyword) {
        return (root, query, cb) -> {
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("githubUsername")), likePattern),
                    cb.like(cb.lower(root.get("recipientEmail")), likePattern),
                    cb.like(cb.lower(root.get("fileName")), likePattern)
            );
        };
    }

}

