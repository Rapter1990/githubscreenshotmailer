package com.example.githubscreenshotmailer.common.model.dto.request;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class CustomSortingTest {

    @Test
    void toSort_withSortByAndAscDirection_returnsAscendingSort() {

        // Given
        CustomSorting sorting = CustomSorting.builder()
                .sortBy("createdAt")
                .sortDirection("ASC")
                .build();

        // When
        Sort sort = sorting.toSort();

        // Then
        assertThat(sort.isSorted()).isTrue();
        Sort.Order order = sort.getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);

    }

    @Test
    void toSort_withSortByAndDescDirection_returnsDescendingSort() {

        // Given
        CustomSorting sorting = CustomSorting.builder()
                .sortBy("eventId")
                .sortDirection("DESC")
                .build();

        // When
        Sort sort = sorting.toSort();

        // Then
        assertThat(sort.isSorted()).isTrue();
        Sort.Order order = sort.getOrderFor("eventId");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);

    }

    @Test
    void toSort_withSortByAndInvalidDirection_defaultsToAscending() {

        // Given
        CustomSorting sorting = CustomSorting.builder()
                .sortBy("name")
                .sortDirection("INVALID")
                .build();

        // When
        Sort sort = sorting.toSort();

        // Then
        assertThat(sort.isSorted()).isTrue();
        Sort.Order order = sort.getOrderFor("name");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);

    }

    @Test
    void toSort_withNullSortBy_returnsUnsorted() {

        // Given
        CustomSorting sorting = CustomSorting.builder()
                .sortBy(null)
                .sortDirection("DESC")
                .build();

        // When
        Sort sort = sorting.toSort();

        // Then
        assertThat(sort.isUnsorted()).isTrue();

    }

    @Test
    void toSort_withBlankSortBy_returnsUnsorted() {

        // Given
        CustomSorting sorting = CustomSorting.builder()
                .sortBy("   ")
                .sortDirection("ASC")
                .build();

        // When
        Sort sort = sorting.toSort();

        // Then
        assertThat(sort.isUnsorted()).isTrue();

    }

}