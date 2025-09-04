package com.example.githubscreenshotmailer.screenshotmailer.service;

import com.example.githubscreenshotmailer.screenshotmailer.config.GithubAutomationProperties;
import com.example.githubscreenshotmailer.screenshotmailer.exception.GithubLoginException;
import com.example.githubscreenshotmailer.screenshotmailer.exception.ScreenshotCaptureException;
import com.example.githubscreenshotmailer.screenshotmailer.utils.FileUtil;
import com.example.githubscreenshotmailer.screenshotmailer.utils.ChromeOptionsUtil;
import com.example.githubscreenshotmailer.screenshotmailer.utils.GithubDomUtil;
import com.example.githubscreenshotmailer.screenshotmailer.utils.GithubMobileUtil;
import com.example.githubscreenshotmailer.screenshotmailer.utils.ScreenshotUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeleniumAutomationService {

    private final GithubAutomationProperties props;
    private final MailService mailService;

    public Path captureProfileScreenshot(String githubUsername, Path targetFile, boolean withLogin) {
        ChromeDriver driver = new ChromeDriver(ChromeOptionsUtil.headless(props.isHeadless()));
        try {
            if (withLogin) {
                loginWithEmailPasswordAndMobile(driver);
            }

            driver.get("https://github.com/" + githubUsername);
            new WebDriverWait(driver, Duration.ofSeconds(30)).until(GithubDomUtil.pageLoaded());

            byte[] png = ScreenshotUtil.captureFullPagePng(driver);
            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, png);
            return targetFile;

        } catch (WebDriverException wde) {
            throw new ScreenshotCaptureException("WebDriver failed during capture: " + wde.getMessage(), wde);
        } catch (IOException ioe) {
            throw new ScreenshotCaptureException("Failed to write screenshot file: " + ioe.getMessage(), ioe);
        } finally {
            try { driver.quit(); } catch (Exception ignore) {}
        }
    }

    // -------------------- Login (email + password, GitHub Mobile only) --------------------
    private void loginWithEmailPasswordAndMobile(WebDriver driver) {
        String email = props.getLoginEmail();
        String pass  = props.getLoginPassword();

        if (email == null || email.isBlank() || pass == null || pass.isBlank()) {
            throw new GithubLoginException("withLogin=true but github-automation.loginEmail/loginPassword not provided");
        }

        try {
            driver.get("https://github.com/login");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            WebElement loginField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login_field")));
            WebElement passwordField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("password")));

            GithubDomUtil.type(loginField, email);
            GithubDomUtil.type(passwordField, pass);
            driver.findElement(By.cssSelector("input[name='commit']")).click();

            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("summary[aria-label*='View profile']")), // success
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("input#otp, input[name='otp'], input[name='verification_code']")), // OTP
                    ExpectedConditions.urlContains("/sessions/verified-device"), // mobile/device verification
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".flash-error")) // banner
            ));

            if (GithubDomUtil.isLoggedIn(driver)) return;

            if (GithubDomUtil.isMobileVerificationPage(driver)) {
                String digit = GithubMobileUtil.extractMobileApprovalDigit(driver);
                emailMobileChallenge(driver, digit);
                waitForMobileApproval(driver, email, pass);
                if (GithubDomUtil.isLoggedIn(driver)) return;
            }

            if (GithubDomUtil.isOtpPage(driver)) {
                if (trySwitchToMobileFromOtp(driver)) {
                    String digit = GithubMobileUtil.extractMobileApprovalDigit(driver);
                    emailMobileChallenge(driver, digit);
                    waitForMobileApproval(driver, email, pass);
                    if (GithubDomUtil.isLoggedIn(driver)) return;
                }
                throw new GithubLoginException(
                        "2FA code requested but this service only supports GitHub Mobile approval. " +
                                "Please choose 'Use GitHub Mobile' on the 2FA screen."
                );
            }

            if (GithubDomUtil.hasFlashError(driver)) {
                throw new GithubLoginException("invalid credentials");
            }

            throw new GithubLoginException("login did not complete");

        } catch (TimeoutException te) {
            throw new GithubLoginException("timeout during login: " + te.getMessage());
        } catch (NoSuchElementException nse) {
            throw new GithubLoginException("login form not found (GitHub DOM changed?): " + nse.getMessage());
        }
    }

    // -------------------- Mobile approval: email + wait --------------------
    private void emailMobileChallenge(WebDriver driver, String digit) {
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            Path baseDir = Path.of(props.getScreenshotDir()).toAbsolutePath();
            Path dailyDir = FileUtil.ensureDailyDir(baseDir.resolve("_auth"));
            String fileName = "github_mobile_challenge_" + LocalDateTime.now().toString().replace(":", "-") + ".png";
            Path path = dailyDir.resolve(fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, png);

            String subject = "[GitHub] Mobile sign-in challenge" + (digit != null ? (" — confirm digit: " + digit) : "");
            String body = (digit != null)
                    ? "Approve the sign-in on your phone by selecting digit: " + digit
                    : "Approve the sign-in on your phone (screenshot attached shows the number).";

            mailService.sendScreenshot(props.getLoginEmail(), subject, body, path.toFile());
            log.info("Emailed GitHub Mobile challenge to {} (digit: {}) at {}", props.getLoginEmail(), digit, path);
        } catch (Exception e) {
            log.warn("Failed to email GitHub Mobile challenge: {}", e.toString());
        }
    }

    private void waitForMobileApproval(WebDriver driver, String email, String pass) {
        long timeoutSec = Math.max(30, props.getMobileApprovalTimeoutSeconds());
        long pollSec    = Math.max(1,  props.getMobilePollingIntervalSeconds());
        long start      = System.currentTimeMillis();
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(6));

        int polls = 0;
        String initialDigit = GithubMobileUtil.extractMobileApprovalDigit(driver);

        while ((System.currentTimeMillis() - start) / 1000 < timeoutSec) {

            if (GithubDomUtil.isLoggedIn(driver)) return;

            if (GithubDomUtil.clickIfPresent(driver,
                    By.xpath("//button[normalize-space()='Continue']"),
                    By.xpath("//button[normalize-space()='Verify']"),
                    By.xpath("//a[normalize-space()='Continue']"),
                    By.xpath("//a[normalize-space()='Verify']"),
                    By.xpath("//*[self::button or self::a][contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'continue')]"),
                    By.xpath("//*[self::button or self::a][contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'verify')]"),
                    By.cssSelector("button[type='submit'], input[type='submit']"))) {
                GithubDomUtil.sleep(800);
                if (GithubDomUtil.isLoggedIn(driver)) return;
            }

            if (!driver.findElements(By.cssSelector("input[type='password']#password, input[name='password']")).isEmpty()) {
                try {
                    WebElement pw = driver.findElement(By.cssSelector("input[type='password']#password, input[name='password']"));
                    pw.clear(); pw.sendKeys(pass);
                    if (!driver.findElements(By.cssSelector("input[name='commit'], button[type='submit']")).isEmpty()) {
                        driver.findElement(By.cssSelector("input[name='commit'], button[type='submit']")).click();
                    } else {
                        pw.sendKeys(Keys.ENTER);
                    }
                    shortWait.until(d -> GithubDomUtil.isLoggedIn(d) || GithubDomUtil.isMobileVerificationPage(d) || GithubDomUtil.isOtpPage(d));
                    if (GithubDomUtil.isLoggedIn(driver)) return;
                } catch (Exception ignored) {}
            }

            polls++;
            if (polls % 3 == 0) {
                try { driver.navigate().refresh(); } catch (Exception ignored) {}
                GithubDomUtil.sleep(600);
            }
            if (polls % 5 == 0) {
                try { driver.navigate().to("https://github.com/"); } catch (Exception ignored) {}
                GithubDomUtil.sleep(900);
            }

            String currentDigit = GithubMobileUtil.extractMobileApprovalDigit(driver);
            if (currentDigit != null && initialDigit != null && !currentDigit.equals(initialDigit)) {
                log.info("Detected new mobile challenge digit {} (was {}). Avoiding re-login to prevent loops.", currentDigit, initialDigit);
            }

            if (GithubDomUtil.isOtpPage(driver)) return;
            GithubDomUtil.sleep(pollSec * 1000L);
        }

        try {
            driver.navigate().to("https://github.com/");
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(d -> GithubDomUtil.isLoggedIn(d) || GithubDomUtil.isOtpPage(d) || GithubDomUtil.isMobileVerificationPage(d));
            if (GithubDomUtil.isLoggedIn(driver)) return;
        } catch (Exception ignored) {}

        throw new GithubLoginException("waiting for GitHub Mobile approval timed out");
    }

    private boolean trySwitchToMobileFromOtp(WebDriver driver) {
        String[] candidates = new String[] {
                "use github mobile", "approve a sign in on your phone",
                "use a different method", "try another way", "more options", "check your phone"
        };
        for (String text : candidates) {
            WebElement el = GithubDomUtil.findByTextContains(driver, text);
            if (el != null) {
                try { el.click(); } catch (Exception ignore) {}
                GithubDomUtil.sleep(800);
                if (GithubDomUtil.isMobileVerificationPage(driver)) return true;
            }
        }
        return false;
    }

}