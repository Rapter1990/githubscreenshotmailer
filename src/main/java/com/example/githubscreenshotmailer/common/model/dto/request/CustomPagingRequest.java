package com.example.githubscreenshotmailer.common.model.dto.request;

import com.example.githubscreenshotmailer.common.model.CustomPaging;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Base request object that encapsulates pagination and optional sorting information.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CustomPagingRequest {

    @NotNull
    private CustomPaging pagination;

    private CustomSorting sorting; // Optional

    /**
     * Converts the request into a Spring Data {@link Pageable} object
     * with optional sorting.
     *
     * @return Pageable instance based on pagination and sorting settings
     */
    public Pageable toPageable() {
        Sort sort = (sorting != null) ? sorting.toSort() : Sort.unsorted();

        return PageRequest.of(
                Math.toIntExact(pagination.getPageNumber()),
                Math.toIntExact(pagination.getPageSize()),
                sort
        );

    }

}
