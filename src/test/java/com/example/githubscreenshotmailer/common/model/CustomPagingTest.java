package com.example.githubscreenshotmailer.common.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CustomPagingTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validation_fails_whenPageNumberLessThanOne() {

        // Given
        CustomPaging paging = CustomPaging.builder()
                .pageNumber(0) // invalid
                .pageSize(10)
                .build();

        // When
        Set<ConstraintViolation<CustomPaging>> violations = validator.validate(paging);

        // Then
        assertThat(violations)
                .anySatisfy(v -> {
                    assertThat(v.getPropertyPath().toString()).isEqualTo("pageNumber");
                    assertThat(v.getMessage()).contains("must be bigger than 0");
                });

    }

    @Test
    void validation_fails_whenPageSizeLessThanOne() {

        // Given
        CustomPaging paging = CustomPaging.builder()
                .pageNumber(1)
                .pageSize(0) // invalid
                .build();

        // When
        Set<ConstraintViolation<CustomPaging>> violations = validator.validate(paging);

        // Then
        assertThat(violations)
                .anySatisfy(v -> {
                    assertThat(v.getPropertyPath().toString()).isEqualTo("pageSize");
                    assertThat(v.getMessage()).contains("must be bigger than 0");
                });

    }

    @Test
    void getPageNumber_returnsZeroBasedIndex() {

        // Given
        CustomPaging paging = CustomPaging.builder()
                .pageNumber(5) // human-friendly page index
                .pageSize(20)
                .build();

        // When
        Integer result = paging.getPageNumber();

        // Then
        assertThat(result).isEqualTo(4); // zero-based

    }

    @Test
    void getPageNumber_withOne_returnsZero() {

        // Given
        CustomPaging paging = CustomPaging.builder()
                .pageNumber(1)
                .pageSize(10)
                .build();

        // When / Then
        assertThat(paging.getPageNumber()).isEqualTo(0);

    }

}