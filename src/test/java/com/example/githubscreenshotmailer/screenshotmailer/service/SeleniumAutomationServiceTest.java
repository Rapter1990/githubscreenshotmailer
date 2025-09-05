package com.example.githubscreenshotmailer.screenshotmailer.service;

import com.example.githubscreenshotmailer.base.AbstractBaseServiceTest;
import com.example.githubscreenshotmailer.screenshotmailer.config.GithubAutomationProperties;
import com.example.githubscreenshotmailer.screenshotmailer.exception.GithubLoginException;
import com.example.githubscreenshotmailer.screenshotmailer.exception.ScreenshotCaptureException;
import com.example.githubscreenshotmailer.screenshotmailer.utils.FileUtil;
import com.example.githubscreenshotmailer.screenshotmailer.utils.GithubDomUtil;
import com.example.githubscreenshotmailer.screenshotmailer.utils.GithubMobileUtil;
import com.example.githubscreenshotmailer.screenshotmailer.utils.ScreenshotUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SeleniumAutomationServiceTest extends AbstractBaseServiceTest {

    @InjectMocks
    private SeleniumAutomationService service;

    @Mock
    GithubAutomationProperties props;

    @Mock
    MailService mailService;

    @TempDir
    Path tempDir;

    // --- shared mocks (created per-test via MockedConstruction) ---
    ChromeDriver driver;
    WebDriver.Navigation navigation;

    MockedConstruction<ChromeDriver> chromeConstr;
    MockedConstruction<WebDriverWait> waitConstr;

    MockedStatic<GithubDomUtil> domStatic;
    MockedStatic<GithubMobileUtil> mobileStatic;
    MockedStatic<ScreenshotUtil> screenshotStatic;
    MockedStatic<FileUtil> fileUtilStatic;

    @BeforeEach
    void setup() {
        when(props.isHeadless()).thenReturn(true);
        when(props.getMobileApprovalTimeoutSeconds()).thenReturn(35);
        when(props.getMobilePollingIntervalSeconds()).thenReturn(1);
        when(props.getScreenshotDir()).thenReturn(tempDir.toString());
        when(props.getLoginEmail()).thenReturn("user@example.com");
        when(props.getLoginPassword()).thenReturn("secret");

        // Intercept all WebDriverWait constructions and short-circuit until(...)
        waitConstr = mockConstruction(WebDriverWait.class, (mock, ctx) -> {
            when(mock.until(any())).thenReturn(true);
            // small helper for custom cases; by default it returns true
            doAnswer(inv -> true).when(mock).until(any());
        });

        // Intercept ChromeDriver construction and create a rich mock
        chromeConstr = mockConstruction(ChromeDriver.class, (mock, ctx) -> {
            this.driver = mock;

            // General driver behaviors used across tests
            this.navigation = mock(WebDriver.Navigation.class);
            when(mock.navigate()).thenReturn(navigation);

            // Default: findElements empty
            when(mock.findElements(any())).thenReturn(Collections.emptyList());
            // Default: findElement throws (so tests stub when needed)
            when(mock.findElement(any())).thenThrow(new NoSuchElementException("none"));
        });

        // Mock all static utility classes used inside the SUT
        domStatic = mockStatic(GithubDomUtil.class);
        mobileStatic = mockStatic(GithubMobileUtil.class);
        screenshotStatic = mockStatic(ScreenshotUtil.class);
        fileUtilStatic = mockStatic(FileUtil.class);

        // Default stubs for static utils
        domStatic.when(GithubDomUtil::pageLoaded)
                .thenReturn((ExpectedCondition<Boolean>) (driver -> true));
        domStatic.when(() -> GithubDomUtil.isLoggedIn(any())).thenReturn(false);
        domStatic.when(() -> GithubDomUtil.isMobileVerificationPage(any())).thenReturn(false);
        domStatic.when(() -> GithubDomUtil.isOtpPage(any())).thenReturn(false);
        domStatic.when(() -> GithubDomUtil.hasFlashError(any())).thenReturn(false);
        domStatic.when(() -> GithubDomUtil.clickIfPresent(any(), any())).thenReturn(false);
        domStatic.when(() -> GithubDomUtil.clickIfPresent(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(false);
        domStatic.when(() -> GithubDomUtil.findByTextContains(any(), anyString())).thenReturn(null);
        // no-op sleep
        domStatic.when(() -> GithubDomUtil.sleep(anyLong())).then(inv -> null);

        mobileStatic.when(() -> GithubMobileUtil.extractMobileApprovalDigit(any())).thenReturn(null);

        screenshotStatic.when(() -> ScreenshotUtil.captureFullPagePng(any())).thenReturn("PNG".getBytes());

        fileUtilStatic.when(() -> FileUtil.ensureDailyDir(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        if (domStatic != null) domStatic.close();
        if (mobileStatic != null) mobileStatic.close();
        if (screenshotStatic != null) screenshotStatic.close();
        if (fileUtilStatic != null) fileUtilStatic.close();
        if (chromeConstr != null) chromeConstr.close();
        if (waitConstr != null) waitConstr.close();
    }

    // ============ Helper to call private methods (only when needed) ============
    private void invokePrivate(Object target, String name, Class<?>[] types, Object... args) {
        try {
            Method m = SeleniumAutomationService.class.getDeclaredMethod(name, types);
            m.setAccessible(true);
            m.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ============================= Tests ======================================

    @Test
    void capture_withoutLogin_happyPath_writesFile_andQuits() throws Exception {
        Path out = tempDir.resolve("profile.png");

        Path result = service.captureProfileScreenshot("octocat", out, false);

        assertEquals(out, result);
        assertTrue(Files.exists(out));
        // Driver was constructed and used
        verify(driver).get("https://github.com/octocat");
        // Screenshot taken via static util
        screenshotStatic.verify(() -> ScreenshotUtil.captureFullPagePng(driver));
        // Ensure quit
        verify(driver).quit();
    }

    @Test
    void capture_withLogin_happyPath_loggedInAfterSubmit() throws Exception {
        // Reset per-test constructions
        if (waitConstr != null) waitConstr.close();
        if (chromeConstr != null) chromeConstr.close();

        // Arrange login page located fields & submit flow
        WebElement loginField = mock(WebElement.class);
        WebElement pwField    = mock(WebElement.class);
        WebElement commit     = mock(WebElement.class);

        // WebDriverWait: first two waits return fields, third (post-submit OR(...)) returns true
        waitConstr = mockConstruction(WebDriverWait.class, (mock, ctx) -> {
            when(mock.until(any()))
                    .thenReturn(loginField)  // presenceOfElementLocated(login_field)
                    .thenReturn(pwField)     // presenceOfElementLocated(password)
                    .thenReturn(true);       // OR(...) after clicking submit
        });

        // ChromeDriver: capture the instance and stub commit lookup on THAT instance
        chromeConstr = mockConstruction(ChromeDriver.class, (mock, ctx) -> {
            this.driver = mock;
            when(mock.findElement(argThat(by -> by.toString().contains("input[name='commit']"))))
                    .thenReturn(commit);
            when(mock.findElements(any())).thenReturn(Collections.emptyList());
            WebDriver.Navigation nav = mock(WebDriver.Navigation.class);
            when(mock.navigate()).thenReturn(nav);
        });

        // Typing side effects
        domStatic.when(() -> GithubDomUtil.type(loginField, "user@example.com")).then(inv -> null);
        domStatic.when(() -> GithubDomUtil.type(pwField, "secret")).then(inv -> null);

        // After submit, consider logged in
        domStatic.when(() -> GithubDomUtil.isLoggedIn(any())).thenReturn(true);

        Path out = tempDir.resolve("ok.png");

        // Act
        Path result = service.captureProfileScreenshot("octo", out, true);

        // Assert
        assertEquals(out, result);
        verify(driver).get("https://github.com/login");
        verify(commit).click();
        verify(driver).get("https://github.com/octo");
        verify(driver).quit();
    }

    @Test
    void capture_withLogin_mobileVerification_flow_emails_and_waits_then_ok() {
        // --- Reset per-test constructions ---
        if (waitConstr != null) waitConstr.close();
        if (chromeConstr != null) chromeConstr.close();

        // Given
        WebElement loginField = mock(WebElement.class);
        WebElement pwField    = mock(WebElement.class);
        WebElement commit     = mock(WebElement.class);

        // WebDriverWait: first two waits return fields, third (post-submit OR(...)) returns true
        waitConstr = mockConstruction(WebDriverWait.class, (mock, ctx) -> {
            when(mock.until(any()))
                    .thenReturn(loginField)  // presenceOfElementLocated(login_field)
                    .thenReturn(pwField)     // presenceOfElementLocated(password)
                    .thenReturn(true);       // OR(...) after clicking submit
        });

        // ChromeDriver: capture the instance and stub commit lookup
        chromeConstr = mockConstruction(ChromeDriver.class, (mock, ctx) -> {
            this.driver = mock;
            when(mock.findElement(argThat(by -> by.toString().contains("input[name='commit']"))))
                    .thenReturn(commit);
            when(mock.findElements(any())).thenReturn(Collections.emptyList());
            WebDriver.Navigation nav = mock(WebDriver.Navigation.class);
            when(mock.navigate()).thenReturn(nav);

            // NEW: Make emailMobileChallenge() succeed (so it sends the email)
            when(mock.getScreenshotAs(OutputType.BYTES)).thenReturn("bin".getBytes());
        });

        // Type into fields via DOM util
        domStatic.when(() -> GithubDomUtil.type(loginField, "user@example.com")).then(inv -> null);
        domStatic.when(() -> GithubDomUtil.type(pwField, "secret")).then(inv -> null);

        // Flow: not logged in → mobile verification screen
        // Make login become true on the next loop iteration (deterministic, avoids timeout)
        domStatic.when(() -> GithubDomUtil.isLoggedIn(any()))
                .thenReturn(false)  // first check
                .thenReturn(true);  // second check -> exit loop

        domStatic.when(() -> GithubDomUtil.isMobileVerificationPage(any())).thenReturn(true);
        domStatic.when(() -> GithubDomUtil.isOtpPage(any())).thenReturn(false);

        // digit shown on the mobile challenge
        mobileStatic.when(() -> GithubMobileUtil.extractMobileApprovalDigit(any())).thenReturn("3");

        // We don't need clickIfPresent to flip any state anymore; keep it permissive
        domStatic.when(() -> GithubDomUtil.clickIfPresent(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(false);

        Path out = tempDir.resolve("ok2.png");

        // When
        Path res = service.captureProfileScreenshot("octo", out, true);

        // Then
        assertEquals(out, res);
        verify(mailService).sendScreenshot(eq("user@example.com"),
                contains("Mobile sign-in challenge"),
                contains("digit: 3"),
                any());

        verify(commit).click();
        verify(driver).quit();
    }

    @Test
    void capture_withLogin_otpPage_switchToMobile_and_ok() {
        // Reset per-test constructions so we can capture the driver created by the SUT
        if (waitConstr != null) waitConstr.close();
        if (chromeConstr != null) chromeConstr.close();

        // Elements used on the login page
        WebElement loginField = mock(WebElement.class);
        WebElement pwField    = mock(WebElement.class);
        WebElement commit     = mock(WebElement.class);

        // WebDriverWait: first two waits return the fields, third (post-submit OR(...)) returns true
        waitConstr = mockConstruction(WebDriverWait.class, (mock, ctx) -> {
            when(mock.until(any()))
                    .thenReturn(loginField)  // presenceOfElementLocated(login_field)
                    .thenReturn(pwField)     // presenceOfElementLocated(password)
                    .thenReturn(true);       // OR(...) after clicking submit
        });

        // ChromeDriver: capture the instance created by the SUT and stub commit lookup on THAT instance
        chromeConstr = mockConstruction(ChromeDriver.class, (mock, ctx) -> {
            this.driver = mock;
            when(mock.findElement(argThat(by -> by.toString().contains("input[name='commit']"))))
                    .thenReturn(commit);
            when(mock.findElements(any())).thenReturn(Collections.emptyList());
            WebDriver.Navigation nav = mock(WebDriver.Navigation.class);
            when(mock.navigate()).thenReturn(nav);
            // Optional: allow emailMobileChallenge() to succeed quietly if it gets used anywhere
            when(mock.getScreenshotAs(OutputType.BYTES)).thenReturn("bin".getBytes());
        });

        // Type into fields
        domStatic.when(() -> GithubDomUtil.type(loginField, "user@example.com")).then(inv -> null);
        domStatic.when(() -> GithubDomUtil.type(pwField, "secret")).then(inv -> null);

        // We want to hit the OTP branch and then switch to Mobile
        domStatic.when(() -> GithubDomUtil.isLoggedIn(any()))
                .thenReturn(false)   // initial checks
                .thenReturn(true);   // after wait loop starts, we become logged in

        // IMPORTANT: Start on OTP page
        domStatic.when(() -> GithubDomUtil.isOtpPage(any()))
                .thenReturn(true);   // stay true long enough to enter the OTP branch

        // IMPORTANT: Not on mobile verification yet, but become mobile verification after clicking the link
        domStatic.when(() -> GithubDomUtil.isMobileVerificationPage(any()))
                .thenReturn(false)   // first check (before trySwitch...) -> not mobile yet
                .thenReturn(true);   // check inside trySwitch after link.click() -> now mobile page

        // trySwitchToMobileFromOtp(): return a link for ANY candidate text so a click definitely occurs
        WebElement link = mock(WebElement.class);
        domStatic.when(() -> GithubDomUtil.findByTextContains(any(), anyString()))
                .thenReturn(link);
        doAnswer(inv -> null).when(link).click();

        // During waitForMobileApproval, trigger immediate re-check path
        domStatic.when(() -> GithubDomUtil.clickIfPresent(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true); // so code re-checks isLoggedIn() right away

        Path out = tempDir.resolve("ok3.png");

        // Act
        Path res = service.captureProfileScreenshot("userx", out, true);

        // Assert
        assertEquals(out, res);
        // verify the OTP -> "Use GitHub Mobile" link click happened
        verify(link).click();
        verify(commit).click();
        verify(driver).quit();
    }

    @Test
    void capture_withLogin_otpPage_cannotSwitch_throws() {
        // Reset per-test constructions
        if (waitConstr != null) waitConstr.close();
        if (chromeConstr != null) chromeConstr.close();

        // Elements present on the login page
        WebElement loginField = mock(WebElement.class);
        WebElement pwField    = mock(WebElement.class);
        WebElement commit     = mock(WebElement.class);

        // WebDriverWait: first two waits return fields, third (post-submit OR(...)) returns true
        waitConstr = mockConstruction(WebDriverWait.class, (mock, ctx) -> {
            when(mock.until(any()))
                    .thenReturn(loginField)  // presenceOfElementLocated(login_field)
                    .thenReturn(pwField)     // presenceOfElementLocated(password)
                    .thenReturn(true);       // OR(...) after clicking submit
        });

        // ChromeDriver: capture the instance and stub commit lookup
        chromeConstr = mockConstruction(ChromeDriver.class, (mock, ctx) -> {
            this.driver = mock;
            when(mock.findElement(argThat(by -> by.toString().contains("input[name='commit']"))))
                    .thenReturn(commit);
            when(mock.findElements(any())).thenReturn(Collections.emptyList());
            WebDriver.Navigation nav = mock(WebDriver.Navigation.class);
            when(mock.navigate()).thenReturn(nav);
        });

        // Type into fields
        domStatic.when(() -> GithubDomUtil.type(loginField, "user@example.com")).then(inv -> null);
        domStatic.when(() -> GithubDomUtil.type(pwField, "secret")).then(inv -> null);

        // After submit: not logged in, we land on OTP page, and cannot switch to Mobile
        domStatic.when(() -> GithubDomUtil.isLoggedIn(any())).thenReturn(false);
        domStatic.when(() -> GithubDomUtil.isOtpPage(any())).thenReturn(true);
        domStatic.when(() -> GithubDomUtil.findByTextContains(any(), anyString())).thenReturn(null); // no "Use GitHub Mobile" link
        domStatic.when(() -> GithubDomUtil.isMobileVerificationPage(any())).thenReturn(false);

        Path out = tempDir.resolve("fail.png");

        GithubLoginException ex = assertThrows(
                GithubLoginException.class,
                () -> service.captureProfileScreenshot("u", out, true)
        );

        assertTrue(ex.getMessage().contains("2FA code requested"));
        verify(commit).click();
        verify(driver).quit();
    }

    @Test
    void capture_withLogin_flashError_invalidCredentials_throws() {
        // --- Given ---
        if (waitConstr != null) waitConstr.close();
        if (chromeConstr != null) chromeConstr.close();

        WebElement loginField = mock(WebElement.class);
        WebElement pwField    = mock(WebElement.class);
        WebElement commit     = mock(WebElement.class);

        // WebDriverWait: two presence waits (fields) + post-submit OR(...) wait
        waitConstr = mockConstruction(WebDriverWait.class, (mock, ctx) -> {
            when(mock.until(any()))
                    .thenReturn(loginField)  // presenceOfElementLocated(login_field)
                    .thenReturn(pwField)     // presenceOfElementLocated(password)
                    .thenReturn(true);       // OR(...) after clicking submit
        });

        // ChromeDriver mock created by SUT; capture it and stub commit lookup
        chromeConstr = mockConstruction(ChromeDriver.class, (mock, ctx) -> {
            this.driver = mock;
            when(mock.findElement(argThat(by -> by.toString().contains("input[name='commit']"))))
                    .thenReturn(commit);
            when(mock.findElements(any())).thenReturn(Collections.emptyList());
        });

        // Type into fields
        domStatic.when(() -> GithubDomUtil.type(loginField, "user@example.com")).then(inv -> null);
        domStatic.when(() -> GithubDomUtil.type(pwField, "secret")).then(inv -> null);

        // Force the "invalid credentials" branch with broad matchers (any driver instance)
        domStatic.when(() -> GithubDomUtil.isLoggedIn(any())).thenReturn(false);
        domStatic.when(() -> GithubDomUtil.isMobileVerificationPage(any())).thenReturn(false);
        domStatic.when(() -> GithubDomUtil.isOtpPage(any())).thenReturn(false);
        domStatic.when(() -> GithubDomUtil.hasFlashError(any())).thenReturn(true);

        Path out = tempDir.resolve("fail2.png");

        // --- When ---
        GithubLoginException ex = assertThrows(
                GithubLoginException.class,
                () -> service.captureProfileScreenshot("u2", out, true)
        );

        // --- Then ---
        assertNotNull(ex.getMessage());
        assertTrue(
                ex.getMessage().toLowerCase().contains("invalid credentials"),
                "Expected message to contain 'invalid credentials' but was: " + ex.getMessage()
        );

        verify(commit).click();
        // Verify we actually checked the flash error at least once (on some driver)
        domStatic.verify(() -> GithubDomUtil.hasFlashError(any()), atLeastOnce());
        verify(driver).quit();
    }

    @Test
    void capture_withLogin_missingEmailOrPass_throwsEarly_andQuits() {
        when(props.getLoginEmail()).thenReturn("");
        when(props.getLoginPassword()).thenReturn("  ");

        Path out = tempDir.resolve("x.png");
        GithubLoginException ex = assertThrows(GithubLoginException.class,
                () -> service.captureProfileScreenshot("x", out, true));
        assertTrue(ex.getMessage().contains("loginEmail/loginPassword not provided"));
        verify(driver).quit();
    }

    @Test
    void capture_webDriverThrows_wrapsAsScreenshotCaptureException_andQuits() {

        // Given
        if (chromeConstr != null) chromeConstr.close();
        chromeConstr = mockConstruction(ChromeDriver.class, (mock, ctx) -> {
            this.driver = mock;

            // basic stubs commonly used
            WebDriver.Navigation nav = mock(WebDriver.Navigation.class);
            when(mock.navigate()).thenReturn(nav);
            when(mock.findElements(any())).thenReturn(Collections.emptyList());

            // Make ANY profile visit (https://github.com/...) blow up
            doThrow(new WebDriverException("boom"))
                    .when(mock).get(startsWith("https://github.com/"));
        });

        Path out = tempDir.resolve("err.png");

        // When
        ScreenshotCaptureException ex = assertThrows(
                ScreenshotCaptureException.class,
                () -> service.captureProfileScreenshot("any", out, false)
        );

        assertTrue(ex.getMessage().contains("WebDriver failed during capture"));

        // Verify
        verify(driver).quit();

    }

    @Test
    void capture_ioFailure_wrapsAsScreenshotCaptureException_andQuits() throws Exception {

        // Given
        Path fakeParentIsFile = tempDir.resolve("as-file");
        Files.write(fakeParentIsFile, "x".getBytes());

        Path out = fakeParentIsFile.resolve("child.png"); // parent is a FILE (not a dir)

        // When
        ScreenshotCaptureException ex = assertThrows(
                ScreenshotCaptureException.class,
                () -> service.captureProfileScreenshot("any", out, false)
        );

        // Then
        assertTrue(
                ex.getMessage() != null &&
                        ex.getMessage().toLowerCase().contains("failed to write screenshot file"),
                "Expected message to mention 'Failed to write screenshot file' but was: " + ex.getMessage()
        );

        // Verify
        verify(driver).quit();

    }

    @Test
    void login_timeoutWrappedAsGithubLoginException() {
        // Reset per-test constructions
        if (waitConstr != null) waitConstr.close();
        if (chromeConstr != null) chromeConstr.close();

        // Login page elements
        WebElement loginField = mock(WebElement.class);
        WebElement pwField    = mock(WebElement.class);
        WebElement commit     = mock(WebElement.class);

        // WebDriverWait: return the two fields, then throw TimeoutException on the OR(...) wait
        waitConstr = mockConstruction(WebDriverWait.class, (mock, ctx) -> {
            when(mock.until(any()))
                    .thenReturn(loginField)               // presenceOfElementLocated(login_field)
                    .thenReturn(pwField)                  // presenceOfElementLocated(password)
                    .thenThrow(new TimeoutException("zzz")); // OR(...) after clicking submit -> timeout
        });

        // ChromeDriver: capture the instance the SUT creates and stub commit lookup on THAT instance
        chromeConstr = mockConstruction(ChromeDriver.class, (mock, ctx) -> {
            this.driver = mock;
            when(mock.findElement(argThat(by -> by.toString().contains("input[name='commit']"))))
                    .thenReturn(commit);
            when(mock.findElements(any())).thenReturn(Collections.emptyList());
            WebDriver.Navigation nav = mock(WebDriver.Navigation.class);
            when(mock.navigate()).thenReturn(nav);
        });

        // No-op typing into fields
        domStatic.when(() -> GithubDomUtil.type(loginField, "user@example.com")).then(inv -> null);
        domStatic.when(() -> GithubDomUtil.type(pwField, "secret")).then(inv -> null);

        Path out = tempDir.resolve("x.png");

        GithubLoginException ex = assertThrows(
                GithubLoginException.class,
                () -> service.captureProfileScreenshot("u", out, true)
        );

        assertTrue(
                ex.getMessage().toLowerCase().contains("timeout during login"),
                "Expected timeout message, got: " + ex.getMessage()
        );
        verify(commit).click();
        verify(driver).quit();
    }

    @Test
    void login_noSuchElementWrappedAsGithubLoginException() {
        // Close any default constructions from @BeforeEach
        if (waitConstr != null) waitConstr.close();
        if (chromeConstr != null) chromeConstr.close();

        // Make WebDriverWait.until(...) throw NoSuchElementException on the first call
        waitConstr = mockConstruction(WebDriverWait.class, (mock, ctx) -> {
            when(mock.until(any()))
                    .thenThrow(new NoSuchElementException("missing"));
        });

        // Capture the ChromeDriver instance created by the SUT (no need to stub findElement)
        chromeConstr = mockConstruction(ChromeDriver.class, (mock, ctx) -> {
            this.driver = mock;
            when(mock.findElements(any())).thenReturn(Collections.emptyList());
            WebDriver.Navigation nav = mock(WebDriver.Navigation.class);
            when(mock.navigate()).thenReturn(nav);
        });

        Path out = tempDir.resolve("x2.png");

        GithubLoginException ex = assertThrows(
                GithubLoginException.class,
                () -> service.captureProfileScreenshot("u", out, true)
        );

        assertNotNull(ex.getMessage());
        assertTrue(
                ex.getMessage().toLowerCase().contains("login form not found"),
                "Expected message to contain 'login form not found' but was: " + ex.getMessage()
        );

        // The driver created by the SUT should still be quit in finally
        verify(driver).quit();

    }


    @Test
    void emailMobileChallenge_attachesScreenshot_and_sendsEmail() {
        // Create a local driver that also implements TakesScreenshot
        ChromeDriver localDriver = mock(ChromeDriver.class, withSettings().extraInterfaces(TakesScreenshot.class));

        // Stub screenshot call
        when(((TakesScreenshot) localDriver).getScreenshotAs(OutputType.BYTES))
                .thenReturn("bin".getBytes());

        // Also stub FileUtil.ensureDailyDir so file writes succeed
        fileUtilStatic.when(() -> FileUtil.ensureDailyDir(any())).thenAnswer(inv -> inv.getArgument(0));

        // Invoke private method with localDriver
        invokePrivate(service, "emailMobileChallenge",
                new Class[]{WebDriver.class, String.class}, localDriver, "7");

        // Verify email was sent with correct subject/body
        verify(mailService).sendScreenshot(eq("user@example.com"),
                contains("Mobile sign-in challenge"),
                contains("digit: 7"),
                any());
    }

    @Test
    void waitForMobileApproval_timesOut_andThenFinalCheck_stillNotLogged_throws() {
        // Keep the timeout short
        when(props.getMobileApprovalTimeoutSeconds()).thenReturn(2);

        // Create a LOCAL driver mock (don't use the field 'driver' which is null here)
        ChromeDriver localDriver = mock(ChromeDriver.class);
        when(localDriver.findElements(any())).thenReturn(Collections.emptyList());
        WebDriver.Navigation nav = mock(WebDriver.Navigation.class);
        when(localDriver.navigate()).thenReturn(nav);
        // void methods on Navigation
        doNothing().when(nav).refresh();
        doNothing().when(nav).to(anyString());

        // Broad stubs so they always match, forcing the timeout path
        domStatic.when(() -> GithubDomUtil.isLoggedIn(any())).thenReturn(false);
        domStatic.when(() -> GithubDomUtil.isOtpPage(any())).thenReturn(false);
        domStatic.when(() -> GithubDomUtil.isMobileVerificationPage(any())).thenReturn(true);
        domStatic.when(() -> GithubDomUtil.clickIfPresent(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(false);
        // (setup() already made GithubDomUtil.sleep(...) a no-op, and WebDriverWait.until(...) returns true)

        // Invoke the private method via reflection; your helper wraps exceptions,
        // so unwrap InvocationTargetException to get the real cause.
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                invokePrivate(service, "waitForMobileApproval",
                        new Class[]{WebDriver.class, String.class, String.class},
                        localDriver, "user@example.com", "secret")
        );

        assertNotNull(ex.getCause(), "Expected a cause on the RuntimeException");
        Throwable first = ex.getCause();
        assertTrue(first instanceof java.lang.reflect.InvocationTargetException,
                "Expected InvocationTargetException but was: " + first);
        Throwable real = ((java.lang.reflect.InvocationTargetException) first).getCause();
        assertNotNull(real, "Expected a nested cause inside InvocationTargetException");
        assertTrue(real instanceof GithubLoginException,
                "Expected cause to be GithubLoginException but was: " + real);
        assertTrue(real.getMessage().contains("timed out"));
    }

    @Test
    void trySwitchToMobileFromOtp_clicksAndSuccess_returnsTrue() {
        WebElement link = mock(WebElement.class);
        domStatic.when(() -> GithubDomUtil.findByTextContains(driver, "use github mobile")).thenReturn(link);
        doAnswer(inv -> null).when(link).click();
        domStatic.when(() -> GithubDomUtil.isMobileVerificationPage(driver)).thenReturn(true);

        Boolean res = Assertions.assertDoesNotThrow(() -> {
            Method m = SeleniumAutomationService.class.getDeclaredMethod("trySwitchToMobileFromOtp", WebDriver.class);
            m.setAccessible(true);
            return (Boolean) m.invoke(service, driver);
        });

        assertTrue(res);
        verify(link).click();
    }

    @Test
    void trySwitchToMobileFromOtp_notFound_returnsFalse() throws Exception {
        domStatic.when(() -> GithubDomUtil.findByTextContains(any(), anyString())).thenReturn(null);
        Method m = SeleniumAutomationService.class.getDeclaredMethod("trySwitchToMobileFromOtp", WebDriver.class);
        m.setAccessible(true);
        boolean res = (boolean) m.invoke(service, driver);
        assertFalse(res);
    }

    @Test
    void finally_always_quits_driver_on_any_path() {
        // Close any default constructions from @BeforeEach
        if (chromeConstr != null) chromeConstr.close();

        // Recreate ChromeDriver construction and make get(..) throw immediately
        chromeConstr = mockConstruction(ChromeDriver.class, (mock, ctx) -> {
            this.driver = mock; // capture the instance created by the SUT
            // Cause an early WebDriver failure as soon as captureProfileScreenshot calls driver.get(...)
            doThrow(new WebDriverException("fail fast")).when(mock).get(anyString());

            // Provide minimal stubs often used elsewhere
            when(mock.findElements(any())).thenReturn(Collections.emptyList());
            WebDriver.Navigation nav = mock(WebDriver.Navigation.class);
            when(mock.navigate()).thenReturn(nav);
        });

        Path out = tempDir.resolve("a.png");

        assertThrows(ScreenshotCaptureException.class,
                () -> service.captureProfileScreenshot("any", out, false));

        // Verify the constructed driver was quit in the finally block
        verify(driver).quit();
    }

    @Test
    void waitForMobileApproval_passwordFieldPath_entersPw_clicksSubmit_and_returns() {

        // Given
        // Use a LOCAL driver because we're calling the private method directly
        ChromeDriver localDriver = mock(ChromeDriver.class);
        WebDriver.Navigation nav = mock(WebDriver.Navigation.class);
        when(localDriver.navigate()).thenReturn(nav);

        // Default: no elements unless we specifically match
        when(localDriver.findElements(any())).thenReturn(Collections.emptyList());

        // When
        // --- Elements & selectors used inside the password branch ---
        WebElement pw = mock(WebElement.class);
        WebElement submit = mock(WebElement.class);

        // Presence of password input (findElements)
        when(localDriver.findElements(argThat(by ->
                by != null && (
                        by.toString().contains("input[type='password']#password")
                                || by.toString().contains("input[name='password']")
                )
        ))).thenReturn(java.util.List.of(pw));

        // Direct lookup of password input (findElement)
        when(localDriver.findElement(argThat(by ->
                by != null && (
                        by.toString().contains("input[type='password']#password")
                                || by.toString().contains("input[name='password']")
                )
        ))).thenReturn(pw);

        // Presence of a submit/commit button (findElements)
        when(localDriver.findElements(argThat(by ->
                by != null && (
                        by.toString().contains("input[name='commit']")
                                || by.toString().contains("button[type='submit']")
                )
        ))).thenReturn(java.util.List.of(submit));

        // Direct lookup of submit/commit button (findElement)
        when(localDriver.findElement(argThat(by ->
                by != null && (
                        by.toString().contains("input[name='commit']")
                                || by.toString().contains("button[type='submit']")
                )
        ))).thenReturn(submit);

        // No-op interactions on elements
        doNothing().when(pw).clear();
        doNothing().when(pw).sendKeys(anyString());
        doNothing().when(submit).click();

        // --- Drive the control flow in waitForMobileApproval(...) ---
        // At loop start: not logged in; after shortWait.until(...) re-check, become logged in so it returns
        domStatic.when(() -> GithubDomUtil.isLoggedIn(any()))
                .thenReturn(false)  // first top-of-loop check
                .thenReturn(true);  // after the short wait and re-check

        // Ensure we don't short-circuit out of the loop via other branches
        domStatic.when(() -> GithubDomUtil.isOtpPage(any())).thenReturn(false);
        domStatic.when(() -> GithubDomUtil.isMobileVerificationPage(any())).thenReturn(false);
        domStatic.when(() -> GithubDomUtil.clickIfPresent(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(false);

        // Then
        // Call the private method
        assertDoesNotThrow(() ->
                invokePrivate(
                        service,
                        "waitForMobileApproval",
                        new Class[]{WebDriver.class, String.class, String.class},
                        localDriver, "user@example.com", "secret"
                )
        );

        // Verify the password path was exercised
        verify(pw).clear();
        verify(pw).sendKeys("secret");
        verify(submit).click();

    }

}