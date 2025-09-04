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

        private Optional<String> githubUsername = Optional.empty();
        private Optional<String> recipientEmail = Optional.empty();
        private Optional<ScreenshotStatus> status = Optional.empty();

        // Date range
        private Optional<LocalDateTime> sentAtFrom = Optional.empty();
        private Optional<LocalDateTime> sentAtTo   = Optional.empty();

        // File attributes
        private Optional<Long> minFileSizeBytes = Optional.empty();
        private Optional<Long> maxFileSizeBytes = Optional.empty();
        private Optional<String> fileNameContains = Optional.empty();

        // Generic keyword search across username/email/fileName
        private Optional<String> keyword = Optional.empty();
    }

    /**
     * Converts this filter object into a {@link Specification} for querying the database.
     *
     * @return the generated {@link Specification} for filtering {@link ScreenshotRecordEntity} results
     */
    @Override
    public Specification<ScreenshotRecordEntity> toSpecification() {

        if (filter == null) {
            // Returning null is acceptable for Spring Data and means "no filtering"
            return null;
        }

        List<Specification<ScreenshotRecordEntity>> specs = new ArrayList<>();

        filter.getGithubUsername()
                .ifPresent(v -> specs.add(ScreenshotRecordSpecification.hasGithubUsername(v)));

        filter.getRecipientEmail()
                .ifPresent(v -> specs.add(ScreenshotRecordSpecification.hasRecipientEmail(v)));

        filter.getStatus()
                .ifPresent(v -> specs.add(ScreenshotRecordSpecification.hasStatus(v)));

        // sentAt range (supports from, to, or both)
        if (filter.getSentAtFrom().isPresent() && filter.getSentAtTo().isPresent()) {
            specs.add(ScreenshotRecordSpecification.sentBetween(
                    filter.getSentAtFrom().get(), filter.getSentAtTo().get()
            ));
        } else {
            filter.getSentAtFrom()
                    .ifPresent(v -> specs.add(ScreenshotRecordSpecification.sentAfter(v)));
            filter.getSentAtTo()
                    .ifPresent(v -> specs.add(ScreenshotRecordSpecification.sentBefore(v)));
        }

        filter.getMinFileSizeBytes()
                .ifPresent(v -> specs.add(ScreenshotRecordSpecification.fileSizeGte(v)));

        filter.getMaxFileSizeBytes()
                .ifPresent(v -> specs.add(ScreenshotRecordSpecification.fileSizeLte(v)));

        filter.getFileNameContains()
                .ifPresent(v -> specs.add(ScreenshotRecordSpecification.fileNameLike(v)));

        filter.getKeyword()
                .ifPresent(v -> specs.add(ScreenshotRecordSpecification.search(v)));

        // Combine all with AND; if none present, return null (no restriction)
        return specs.stream().reduce(Specification::and).orElse(null);

    }

}
