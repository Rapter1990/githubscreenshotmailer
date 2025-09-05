package com.example.githubscreenshotmailer.screenshotmailer.controller;

import com.example.githubscreenshotmailer.base.AbstractRestControllerTest;
import com.example.githubscreenshotmailer.common.model.CustomPage;
import com.example.githubscreenshotmailer.common.model.CustomPaging;
import com.example.githubscreenshotmailer.common.model.dto.request.CustomPagingRequest;
import com.example.githubscreenshotmailer.common.model.dto.request.CustomSorting;
import com.example.githubscreenshotmailer.common.model.dto.response.CustomPagingResponse;
import com.example.githubscreenshotmailer.common.model.dto.response.CustomResponse;
import com.example.githubscreenshotmailer.screenshotmailer.model.ScreenshotRecord;
import com.example.githubscreenshotmailer.screenshotmailer.model.dto.request.FilterServicePagingRequest;
import com.example.githubscreenshotmailer.screenshotmailer.model.dto.request.ListScreenshotRecordRequest;
import com.example.githubscreenshotmailer.screenshotmailer.model.dto.request.ScreenshotRequest;
import com.example.githubscreenshotmailer.screenshotmailer.model.enums.ScreenshotStatus;
import com.example.githubscreenshotmailer.screenshotmailer.model.mapper.CustomPageScreenshotRecordToCustomPagingScreenshotResponseMapper;
import com.example.githubscreenshotmailer.screenshotmailer.model.mapper.ScreenshotRecordToScreenshotResponseMapper;
import com.example.githubscreenshotmailer.screenshotmailer.service.GithubScreenshotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GithubScreenshotControllerTest extends AbstractRestControllerTest {

    @MockitoBean
    private GithubScreenshotService service;

    private static final String BASE_URL = "/api/github-screenshots";

    private static final ScreenshotRecordToScreenshotResponseMapper DOMAIN_TO_RESPONSE =
            ScreenshotRecordToScreenshotResponseMapper.initialize();

    private static final CustomPageScreenshotRecordToCustomPagingScreenshotResponseMapper PAGE_MAPPER =
            CustomPageScreenshotRecordToCustomPagingScreenshotResponseMapper.initialize();


    // ------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------

    @Test
    @DisplayName("POST /api/github-screenshots -> 201 Created; delegates to service.process and returns payload")
    void create_HappyPath_Returns201() throws Exception {

        // Given
        ScreenshotRecord domain = sampleDomain();
        ScreenshotRequest request = new ScreenshotRequest("octocat",
                "user@example.com",
                false);

        // Expected response from mapper
        var expectedResponse = DOMAIN_TO_RESPONSE.map(domain);

        // When -> Mockito.when().thenReturn()
        when(service.process(any())).thenReturn(domain);

        // Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpStatus").value("CREATED"))
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.githubUsername").value(expectedResponse.githubUsername()))
                .andExpect(jsonPath("$.response.recipientEmail").value(expectedResponse.recipientEmail()))
                .andExpect(jsonPath("$.response.fileName").value(expectedResponse.fileName()))
                .andExpect(jsonPath("$.response.fileSize").value(expectedResponse.fileSize()))
                .andExpect(jsonPath("$.response.status").value(expectedResponse.status()));

        // Verify
        verify(service).process(any());

    }

    @Test
    @DisplayName("POST /api/github-screenshots -> withLogin=true also returns 201; still delegates to service")
    void create_WithLoginTrue_Returns201() throws Exception {
        // Given
        ScreenshotRecord domain = sampleDomain();
        ScreenshotRequest request = new ScreenshotRequest("octocat", "user@example.com", true);

        // Expected response from mapper
        var expectedResponse = DOMAIN_TO_RESPONSE.map(domain);

        // When
        when(service.process(any())).thenReturn(domain);

        // Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpStatus").value("CREATED"))
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.githubUsername").value(expectedResponse.githubUsername()))
                .andExpect(jsonPath("$.response.recipientEmail").value(expectedResponse.recipientEmail()))
                .andExpect(jsonPath("$.response.fileName").value(expectedResponse.fileName()))
                .andExpect(jsonPath("$.response.fileSize").value(expectedResponse.fileSize()))
                .andExpect(jsonPath("$.response.status").value(expectedResponse.status()));

        // Verify
        verify(service).process(any());

    }

    @Test
    @DisplayName("POST /api/github-screenshots -> 400 when email invalid; service not invoked")
    void create_ValidationFailure_InvalidEmail_Returns400_AndServiceNotCalled() throws Exception {
        // Given
        when(service.process(any())).thenReturn(sampleDomain());

        // Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("octocat", "not-an-email", false)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatus").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message",
                        anyOf(is("Validation failed"), is("Constraint violation"), is("Invalid parameter type"), is("Bad Request"))));

        // Verify -> Mockito.verify()
        verify(service, never()).process(any());
    }

    // ------------------------------------------------------------
    // SEARCH (paged)
    // ------------------------------------------------------------

    @Test
    @DisplayName("POST /api/github-screenshots/search -> 200 OK; delegates to service.getScreenshots")
    void search_Paged_Returns200() throws Exception {
        // Given request object
        FilterServicePagingRequest req = new FilterServicePagingRequest();

        ListScreenshotRecordRequest filter = new ListScreenshotRecordRequest();
        ListScreenshotRecordRequest.Filter f = new ListScreenshotRecordRequest.Filter();
        f.setGithubUsername("octocat");
        filter.setFilter(f);

        CustomPagingRequest paging = CustomPagingRequest.builder()
                .pagination(CustomPaging.builder()
                        .pageNumber(1)
                        .pageSize(10)
                        .build())
                .sorting(CustomSorting.builder()
                        .sortBy("sentAt")
                        .sortDirection("DESC")
                        .build())
                .build();

        req.setFilterRequest(filter);
        req.setPagingRequest(paging);

        // Expected page from service
        ScreenshotRecord r1 = sampleDomain();
        CustomPage<ScreenshotRecord> page = CustomPage.<ScreenshotRecord>builder()
                .content(List.of(r1))
                .pageNumber(1)
                .pageSize(10)
                .totalElementCount(1L)
                .totalPageCount(1)
                .build();

        // When -> Mockito.when().thenReturn()
        when(service.getScreenshots(any(ListScreenshotRecordRequest.class), any(CustomPagingRequest.class)))
                .thenReturn(page);

        // For sanity/consistency (not strictly required), compute expected envelope
        CustomPagingResponse<?> expected = PAGE_MAPPER.toPagingResponse(page);
        CustomResponse<?> expectedEnvelope = CustomResponse.successOf(expected);

        // Then
        mockMvc.perform(post(BASE_URL + "/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpStatus").value("OK"))
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.totalElementCount").value(expected.getTotalElementCount()))
                .andExpect(jsonPath("$.response.pageNumber").value(expected.getPageNumber()))
                .andExpect(jsonPath("$.response.pageSize").value(expected.getPageSize()))
                .andExpect(jsonPath("$.response.totalPageCount").value(expected.getTotalPageCount()))
                .andExpect(jsonPath("$.response.content[0].githubUsername").value(r1.githubUsername()))
                .andExpect(jsonPath("$.response.content[0].recipientEmail").value(r1.recipientEmail()))
                .andExpect(jsonPath("$.response.content[0].status").value(r1.status().name()));

        // Verify -> Mockito.verify()
        verify(service).getScreenshots(any(ListScreenshotRecordRequest.class), any(CustomPagingRequest.class));
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private String requestJson(String username, String email, boolean withLogin) {
        return """
               {
                 "githubUsername": %s,
                 "recipientEmail": %s,
                 "withLogin": %s
               }
               """.formatted(
                toJsonStr(username),
                toJsonStr(email),
                String.valueOf(withLogin)
        );
    }

    private String toJsonStr(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    private ScreenshotRecord sampleDomain() {
        return new ScreenshotRecord(
                "img-100",                     // imageId
                "octocat",                     // githubUsername
                "user@example.com",            // recipientEmail
                "octocat.png",                 // fileName
                "/screenshots/octocat.png",    // path
                12_345L,                       // fileSize
                LocalDateTime.of(2025, 1, 1, 12, 0), // sentAt
                ScreenshotStatus.SUCCESS          // status (or SUCCESS/FAILED depending on your enum)
        );
    }

}