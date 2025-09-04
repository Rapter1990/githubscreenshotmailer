package com.example.githubscreenshotmailer.screenshotmailer.model.dto.request;

import com.example.githubscreenshotmailer.common.model.dto.specification.Filterable;
import com.example.githubscreenshotmailer.screenshotmailer.model.dto.specification.ScreenshotRecordSpecification;
import com.example.githubscreenshotmailer.screenshotmailer.model.entity.ScreenshotRecordEntity;
import com.example.githubscreenshotmailer.screenshotmailer.model.enums.ScreenshotStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class ListScreenshotRecordRequest implements Filterable<ScreenshotRecordEntity> {

    /**
     * The filter criteria for screenshot record search.
     */
    private Filter filter;

    /**
     * Inner class representing the filter criteria for screenshot record search.
     */
    @Getter
    @Setter
    public static class Filter {

        private String githubUsername;
        private String recipientEmail;
        private ScreenshotStatus status;

        private LocalDateTime sentAtFrom;
        private LocalDateTime sentAtTo;

        private Long minFileSizeBytes;
        private Long maxFileSizeBytes;

        private String fileNameContains;
        private String keyword;
    }

    /**
     * Converts this filter object into a {@link Specification} for querying the database.
     *
     * @return the generated {@link Specification} for filtering {@link ScreenshotRecordEntity} results
     */
    @Override
    public Specification<ScreenshotRecordEntity> toSpecification() {
        if (filter == null) return null;

        List<Specification<ScreenshotRecordEntity>> specs = new ArrayList<>();

        if (filter.getGithubUsername() != null && !filter.getGithubUsername().isBlank()) {
            specs.add(ScreenshotRecordSpecification.hasGithubUsername(filter.getGithubUsername()));
        }
        if (filter.getRecipientEmail() != null && !filter.getRecipientEmail().isBlank()) {
            specs.add(ScreenshotRecordSpecification.hasRecipientEmail(filter.getRecipientEmail()));
        }
        if (filter.getStatus() != null) {
            specs.add(ScreenshotRecordSpecification.hasStatus(filter.getStatus()));
        }

        if (filter.getSentAtFrom() != null && filter.getSentAtTo() != null) {
            specs.add(ScreenshotRecordSpecification.sentBetween(filter.getSentAtFrom(), filter.getSentAtTo()));
        } else {
            if (filter.getSentAtFrom() != null) specs.add(ScreenshotRecordSpecification.sentAfter(filter.getSentAtFrom()));
            if (filter.getSentAtTo() != null)   specs.add(ScreenshotRecordSpecification.sentBefore(filter.getSentAtTo()));
        }

        if (filter.getMinFileSizeBytes() != null) specs.add(ScreenshotRecordSpecification.fileSizeGte(filter.getMinFileSizeBytes()));
        if (filter.getMaxFileSizeBytes() != null) specs.add(ScreenshotRecordSpecification.fileSizeLte(filter.getMaxFileSizeBytes()));

        if (filter.getFileNameContains() != null && !filter.getFileNameContains().isBlank()) {
            specs.add(ScreenshotRecordSpecification.fileNameLike(filter.getFileNameContains()));
        }
        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            specs.add(ScreenshotRecordSpecification.search(filter.getKeyword()));
        }

        return specs.stream().reduce(Specification::and).orElse(null);
    }
}
