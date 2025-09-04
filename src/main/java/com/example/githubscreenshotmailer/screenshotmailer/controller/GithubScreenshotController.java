package com.example.githubscreenshotmailer.screenshotmailer.controller;

import com.example.githubscreenshotmailer.common.model.CustomPage;
import com.example.githubscreenshotmailer.common.model.dto.response.CustomPagingResponse;
import com.example.githubscreenshotmailer.common.model.dto.response.CustomResponse;
import com.example.githubscreenshotmailer.screenshotmailer.model.ScreenshotRecord;
import com.example.githubscreenshotmailer.screenshotmailer.model.dto.request.FilterServicePagingRequest;
import com.example.githubscreenshotmailer.screenshotmailer.model.dto.request.ScreenshotRequest;
import com.example.githubscreenshotmailer.screenshotmailer.model.dto.response.ScreenshotResponse;
import com.example.githubscreenshotmailer.screenshotmailer.model.mapper.CustomPageScreenshotRecordToCustomPagingScreenshotResponseMapper;
import com.example.githubscreenshotmailer.screenshotmailer.model.mapper.ScreenshotRecordToScreenshotResponseMapper;
import com.example.githubscreenshotmailer.screenshotmailer.service.GithubScreenshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github-screenshots")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "GitHub Screenshots",
        description = "Capture GitHub profile screenshots, email results, and list persisted screenshot records."
)
public class GithubScreenshotController {

    private final GithubScreenshotService service;

    private static final ScreenshotRecordToScreenshotResponseMapper DOMAIN_TO_RESPONSE =
            ScreenshotRecordToScreenshotResponseMapper.initialize();

    private static final CustomPageScreenshotRecordToCustomPagingScreenshotResponseMapper PAGE_MAPPER =
            CustomPageScreenshotRecordToCustomPagingScreenshotResponseMapper.initialize();

    /**
     * Capture GitHub profile screenshot and persist record
     */
    @Operation(
            summary = "Capture a GitHub profile screenshot",
            description = "Captures the GitHub profile page of the given username (optionally with login), "
                    + "emails the image to the recipient, and stores a screenshot record."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Screenshot captured, emailed, and persisted successfully.",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error in the request.",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Github Authentication required or not provided (if secured).",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error while capturing or emailing.",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))
            )
    })
    @PostMapping(consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomResponse<ScreenshotResponse> create(@Valid @RequestBody ScreenshotRequest request) {
        ScreenshotRecord domain = service.process(request);
        ScreenshotResponse response = DOMAIN_TO_RESPONSE.map(domain);
        return CustomResponse.createdOf(response);
    }

    /**
     * Get paginated screenshot records with optional filters
     */
    @Operation(
            summary = "List screenshot records (paged)",
            description = "Returns a paginated list of screenshot records filtered by username, email, status, "
                    + "date range, file size, filename, or keyword."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paged screenshot records returned successfully.",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter or paging parameters.",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))
            )
    })
    @PostMapping(value = "/search")
    public CustomResponse<CustomPagingResponse<ScreenshotResponse>> getScreenshots(
            @Valid @RequestBody FilterServicePagingRequest filterServicePagingRequest) {

        CustomPage<ScreenshotRecord> page = service.getScreenshots(filterServicePagingRequest.getFilterRequest(), filterServicePagingRequest.getPagingRequest());
        CustomPagingResponse<ScreenshotResponse> response = PAGE_MAPPER.toPagingResponse(page);
        return CustomResponse.successOf(response);
    }

}
