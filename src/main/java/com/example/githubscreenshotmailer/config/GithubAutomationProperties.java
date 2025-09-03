package com.example.githubscreenshotmailer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "github-automation")
public class GithubAutomationProperties {
    private String screenshotDir;
    private boolean headless = true;
    private String loginEmail;
    private String loginPassword;
}
