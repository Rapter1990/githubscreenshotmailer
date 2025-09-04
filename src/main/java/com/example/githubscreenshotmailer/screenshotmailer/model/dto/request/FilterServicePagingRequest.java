package com.example.githubscreenshotmailer.screenshotmailer.model.dto.request;

import com.example.githubscreenshotmailer.common.model.dto.request.CustomPagingRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Wrapper request object combining service filter criteria and pagination details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FilterServicePagingRequest {

    @Valid
    private ListScreenshotRecordRequest filterRequest;

    @Valid
    private CustomPagingRequest pagingRequest;
}
