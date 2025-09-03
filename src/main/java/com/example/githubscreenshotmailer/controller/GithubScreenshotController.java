package com.example.githubscreenshotmailer.web.controller;

import com.example.githubscreenshotmailer.model.ScreenshotRecord;
import com.example.githubscreenshotmailer.model.dto.request.ScreenshotRequest;
import com.example.githubscreenshotmailer.model.dto.response.ScreenshotResponse;
import com.example.githubscreenshotmailer.model.mapper.ScreenshotRecordToScreenshotResponseMapper;
import com.example.githubscreenshotmailer.service.GithubScreenshotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github-screenshots")
@RequiredArgsConstructor
@Validated
public class GithubScreenshotController {

    private final GithubScreenshotService service;

    private static final ScreenshotRecordToScreenshotResponseMapper DOMAIN_TO_RESPONSE =
            ScreenshotRecordToScreenshotResponseMapper.initialize();

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<ScreenshotResponse> create(@Valid @RequestBody ScreenshotRequest request) {
        ScreenshotRecord domain = service.process(request);
        ScreenshotResponse response = DOMAIN_TO_RESPONSE.map(domain);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
