package com.example.githubscreenshotmailer.service;

import com.example.githubscreenshotmailer.config.GithubAutomationProperties;
import com.example.githubscreenshotmailer.exception.GithubLoginException;
import com.example.githubscreenshotmailer.exception.ScreenshotCaptureException;
import com.example.githubscreenshotmailer.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
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

    /** Main entry: headless if configured; supports email+password + GitHub Mobile approval. */
    public Path captureProfileScreenshot(String githubUsername, Path targetFile, boolean withLogin) {
        WebDriver driver = new ChromeDriver(buildHeadlessOptions());
        try {
            if (withLogin) {
                loginWithEmailPasswordAndMobile(driver);
            }

            driver.get("https://github.com/" + githubUsername);
            new WebDriverWait(driver, Duration.ofSeconds(25)).until(pageLoaded());

            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
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

        if (isBlank(email) || isBlank(pass)) {
            throw new GithubLoginException("withLogin=true but github-automation.loginEmail/loginPassword not provided");
        }

        try {
            driver.get("https://github.com/login");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            WebElement loginField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login_field")));
            WebElement passwordField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("password")));

            type(loginField, email);
            type(passwordField, pass);
            driver.findElement(By.cssSelector("input[name='commit']")).click();

            // Wait for one of: success, OTP page, mobile/device verification page, or flash error
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("summary[aria-label*='View profile']")), // success
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("input#otp, input[name='otp'], input[name='verification_code']")), // OTP
                    ExpectedConditions.urlContains("/sessions/verified-device"), // mobile/device verification
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".flash-error")) // banner
            ));

            if (isLoggedIn(driver)) return;

            if (isMobileVerificationPage(driver)) {
                String digit = extractMobileApprovalDigit(driver);
                emailMobileChallenge(driver, digit);
                waitForMobileApproval(driver, email, pass);
                if (isLoggedIn(driver)) return;
            }

            if (isOtpPage(driver)) {
                if (trySwitchToMobileFromOtp(driver)) {
                    String digit = extractMobileApprovalDigit(driver);
                    emailMobileChallenge(driver, digit);
                    waitForMobileApproval(driver, email, pass);
                    if (isLoggedIn(driver)) return;
                }
                throw new GithubLoginException(
                        "2FA code requested but this service only supports GitHub Mobile approval. " +
                                "Please choose 'Use GitHub Mobile' on the 2FA screen."
                );
            }

            // Only consider flash-error after verification paths
            if (hasFlashError(driver)) {
                throw new GithubLoginException("invalid credentials");
            }

            throw new GithubLoginException("login did not complete");

        } catch (TimeoutException te) {
            throw new GithubLoginException("timeout during login: " + te.getMessage());
        } catch (NoSuchElementException nse) {
            throw new GithubLoginException("login form not found (GitHub DOM changed?): " + nse.getMessage());
        }
    }

    // -------------------- Mobile approval: extract digit + email + wait --------------------

    // REPLACE your existing extractMobileApprovalDigit(...) with this:
    private String extractMobileApprovalDigit(WebDriver driver) {
        // give the page a moment to render the big number
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // Scan only visible text nodes likely to contain the big confirmation digits
        java.util.List<WebElement> nodes = driver.findElements(
                By.cssSelector("h1, h2, h3, .h0, .h1, .h2, .f0, .f1, .f2, .f3, " +
                        "strong, b, p, span, div"));

        java.util.regex.Pattern digitsOnly = java.util.regex.Pattern.compile("^\\s*(\\d{2,3})\\s*$");

        String bestDigits = null;
        double bestFontPx = -1;

        for (WebElement el : nodes) {
            try {
                if (!el.isDisplayed()) continue;
                String txt = el.getText();
                if (txt == null) continue;
                var m = digitsOnly.matcher(txt);
                if (!m.matches()) continue;

                // rank by font-size; pick the largest (the big center number)
                String fs = el.getCssValue("font-size"); // e.g. "56px"
                double px = parsePx(fs);
                if (px > bestFontPx) {
                    bestFontPx = px;
                    bestDigits = m.group(1);
                }
            } catch (StaleElementReferenceException ignored) {
                // DOM updated; skip this node
            } catch (Exception ignored) {}
        }

        return bestDigits; // may be null if not found
    }

    private double parsePx(String fontSizeCss) {
        if (fontSizeCss == null) return 0;
        try {
            String s = fontSizeCss.trim().toLowerCase();
            if (s.endsWith("px")) s = s.substring(0, s.length() - 2);
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

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

        // capture the first seen digit (to help debug double-challenge)
        String initialDigit = extractMobileApprovalDigit(driver);

        while ((System.currentTimeMillis() - start) / 1000 < timeoutSec) {

            if (isLoggedIn(driver)) return;

            // If there is a post-approval interstitial, click it.
            if (clickIfPresent(driver,
                    By.xpath("//button[normalize-space()='Continue']"),
                    By.xpath("//button[normalize-space()='Verify']"),
                    By.xpath("//a[normalize-space()='Continue']"),
                    By.xpath("//a[normalize-space()='Verify']"),
                    By.xpath("//*[self::button or self::a][contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'continue')]"),
                    By.xpath("//*[self::button or self::a][contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'verify')]"),
                    By.cssSelector("button[type='submit'], input[type='submit']"))) {
                sleep(800);
                if (isLoggedIn(driver)) return;
            }

            // Only if GitHub explicitly asks for password again (rare), fill and submit.
            if (!driver.findElements(By.cssSelector("input[type='password']#password, input[name='password']")).isEmpty()) {
                try {
                    WebElement pw = driver.findElement(By.cssSelector("input[type='password']#password, input[name='password']"));
                    pw.clear(); pw.sendKeys(pass);
                    if (!driver.findElements(By.cssSelector("input[name='commit'], button[type='submit']")).isEmpty()) {
                        driver.findElement(By.cssSelector("input[name='commit'], button[type='submit']")).click();
                    } else {
                        pw.sendKeys(Keys.ENTER);
                    }
                    shortWait.until(d -> isLoggedIn(d) || isMobileVerificationPage(d) || isOtpPage(d));
                    if (isLoggedIn(driver)) return;
                } catch (Exception ignored) {}
            }

            // If still on verified-device, keep waiting and lightly nudge WITHOUT hitting /login.
            polls++;
            if (polls % 3 == 0) {
                try { driver.navigate().refresh(); } catch (Exception ignored) {}
                sleep(600);
            }
            if (polls % 5 == 0) {
                try { driver.navigate().to("https://github.com/"); } catch (Exception ignored) {}
                sleep(900);
            }

            // Optional: detect if a brand-new challenge appeared (digit changed) => stop nudging.
            String currentDigit = extractMobileApprovalDigit(driver);
            if (currentDigit != null && initialDigit != null && !currentDigit.equals(initialDigit)) {
                log.info("Detected new mobile challenge digit {} (was {}). Avoiding re-login to prevent loops.", currentDigit, initialDigit);
                // Do NOT navigate or resubmit; just wait for approval of the latest one.
            }

            // If OTP page appears (org policy), exit to caller (we don't handle codes here).
            if (isOtpPage(driver)) return;

            sleep(pollSec * 1000L);
        }

        // Final gentle pickup.
        try {
            driver.navigate().to("https://github.com/");
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(d -> isLoggedIn(d) || isOtpPage(d) || isMobileVerificationPage(d));
            if (isLoggedIn(driver)) return;
        } catch (Exception ignored) {}

        throw new GithubLoginException("waiting for GitHub Mobile approval timed out");
    }

    private boolean clickIfPresent(WebDriver driver, By... locators) {
        for (By by : locators) {
            java.util.List<WebElement> els = driver.findElements(by);
            for (WebElement el : els) {
                try {
                    if (el.isDisplayed() && el.isEnabled()) {
                        el.click();
                        return true;
                    }
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    private boolean trySwitchToMobileFromOtp(WebDriver driver) {
        String[] candidates = new String[] {
                "use github mobile", "approve a sign in on your phone",
                "use a different method", "try another way", "more options", "check your phone"
        };
        for (String text : candidates) {
            WebElement el = findByTextContains(driver, text);
            if (el != null) {
                try { el.click(); } catch (Exception ignore) {}
                sleep(800);
                if (isMobileVerificationPage(driver)) return true;
            }
        }
        return false;
    }

    // -------------------- Options & helpers --------------------

    private ChromeOptions buildHeadlessOptions() {
        ChromeOptions options = new ChromeOptions();
        if (props.isHeadless()) options.addArguments("--headless=new");
        options.addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--window-size=1366,768",
                "--lang=en-US",
                "--disable-blink-features=AutomationControlled"
        );
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36");
        return options;
    }

    private ExpectedCondition<Boolean> pageLoaded() {
        return d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState"));
    }

    /** Consider session established if any of these are present. */
    private boolean isLoggedIn(WebDriver driver) {
        try {
            // user_session is the main session cookie; logged_in=yes is also common
            Cookie userSession = driver.manage().getCookieNamed("user_session");
            Cookie loggedIn    = driver.manage().getCookieNamed("logged_in");
            Cookie dotcomUser  = driver.manage().getCookieNamed("dotcom_user"); // username value

            if (userSession != null) return true;
            if (loggedIn != null && "yes".equalsIgnoreCase(loggedIn.getValue())) return true;
            if (dotcomUser != null && dotcomUser.getValue() != null && !dotcomUser.getValue().isBlank()) return true;
        } catch (Exception ignored) {}

        // UI hints
        boolean profileMenu =
                !driver.findElements(By.cssSelector("summary[aria-label*='View profile']")).isEmpty()
                        || !driver.findElements(By.cssSelector("summary[aria-label='View profile and more']")).isEmpty()
                        || !driver.findElements(By.cssSelector("details[aria-label='View profile and more']")).isEmpty();

        boolean metaUserLogin = false;
        try {
            WebElement meta = driver.findElement(By.cssSelector("meta[name='user-login']"));
            String content = meta.getAttribute("content");
            metaUserLogin = content != null && !content.isBlank();
        } catch (NoSuchElementException ignored) {}

        return profileMenu || metaUserLogin;
    }

    private boolean hasFlashError(WebDriver driver) {
        return !driver.findElements(By.cssSelector(".flash-error")).isEmpty();
    }

    private boolean isOtpPage(WebDriver driver) {
        return !driver.findElements(By.cssSelector("input#otp, input[name='otp'], input[name='verification_code']")).isEmpty();
    }

    private boolean isMobileVerificationPage(WebDriver driver) {
        // If we already have a session cookie, we are *not* in verification anymore.
        if (isLoggedIn(driver)) return false;

        String url = driver.getCurrentUrl();
        if (url != null && url.contains("/sessions/verified-device")) return true;
        String src = driver.getPageSource().toLowerCase();
        return src.contains("check your phone")
                || src.contains("approve sign in")
                || src.contains("confirm digit")
                || src.contains("github mobile")
                || src.contains("verify your identity")
                || src.contains("device verification");
    }


    private WebElement findByTextContains(WebDriver driver, String textLower) {
        String xpath = "//*[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" +
                textLower + "')]";
        var els = driver.findElements(By.xpath(xpath));
        return els.isEmpty() ? null : els.get(0);
    }

    private void type(WebElement el, String text) {
        el.clear();
        el.sendKeys(text);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

}
