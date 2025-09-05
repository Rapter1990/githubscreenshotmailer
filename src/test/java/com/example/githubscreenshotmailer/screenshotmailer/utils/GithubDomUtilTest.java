package com.example.githubscreenshotmailer.screenshotmailer.utils;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.openqa.selenium.*;
import org.openqa.selenium.WebDriver.Options;
import org.openqa.selenium.support.ui.ExpectedCondition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GithubDomUtilTest {

    // --- Tests ---------------------------------------------------------------

    @Test
    void pageLoaded_returnsTrueWhenDocumentReadyStateIsComplete_andFalseOtherwise() {
        WebDriver d = mockJsDriver();

        stubReadyState(d, "complete");
        ExpectedCondition<Boolean> cond = GithubDomUtil.pageLoaded();
        assertEquals(Boolean.TRUE, cond.apply(d));

        stubReadyState(d, "loading");
        assertEquals(Boolean.FALSE, cond.apply(d));
    }

    @Test
    void isLoggedIn_trueWhenUserSessionCookieExists() {
        WebDriver d = mock(WebDriver.class);
        Options opts = mockCookieOptions(d);

        when(opts.getCookieNamed("user_session")).thenReturn(new Cookie("user_session", "xyz"));
        assertTrue(GithubDomUtil.isLoggedIn(d));
    }

    @Test
    void isLoggedIn_trueWhenLoggedInCookieIsYes() {
        WebDriver d = mock(WebDriver.class);
        Options opts = mockCookieOptions(d);

        when(opts.getCookieNamed("user_session")).thenReturn(null);
        when(opts.getCookieNamed("logged_in")).thenReturn(new Cookie("logged_in", "YES")); // case-insensitive check
        assertTrue(GithubDomUtil.isLoggedIn(d));
    }

    @Test
    void isLoggedIn_trueWhenDotcomUserCookieHasNonBlank() {
        WebDriver d = mock(WebDriver.class);
        Options opts = mockCookieOptions(d);

        when(opts.getCookieNamed("user_session")).thenReturn(null);
        when(opts.getCookieNamed("logged_in")).thenReturn(null);
        when(opts.getCookieNamed("dotcom_user")).thenReturn(new Cookie("dotcom_user", "rapter"));

        assertTrue(GithubDomUtil.isLoggedIn(d));
    }

    @Test
    void isLoggedIn_trueWhenProfileMenuElementsExist() {
        WebDriver d = mock(WebDriver.class);
        Options opts = mockCookieOptions(d);
        when(opts.getCookieNamed(anyString())).thenReturn(null);

        // Return an element for one of the profile selectors (tolerant matcher)
        when(d.findElements(argThat(bySelector("summary[aria-label*='View profile']"))))
                .thenReturn(List.of(mock(WebElement.class)));
        when(d.findElements(argThat(bySelector("summary[aria-label='View profile and more']"))))
                .thenReturn(List.of());
        when(d.findElements(argThat(bySelector("details[aria-label='View profile and more']"))))
                .thenReturn(List.of());

        // Prevent NPE: meta probe should throw NoSuchElementException (caught in util)
        when(d.findElement(argThat(bySelector("meta[name='user-login']"))))
                .thenThrow(new NoSuchElementException("no meta"));

        assertTrue(GithubDomUtil.isLoggedIn(d));
    }
    
    @Test
    void isLoggedIn_trueWhenMetaUserLoginPresent_andNoCookiesOrProfileMenu() {
        WebDriver d = mock(WebDriver.class);
        Options opts = mockCookieOptions(d);
        when(opts.getCookieNamed(anyString())).thenReturn(null);

        // No profile menu
        when(d.findElements(any(By.class))).thenReturn(List.of());

        // meta[name='user-login'] exists and has content
        WebElement meta = mock(WebElement.class);
        when(meta.getAttribute("content")).thenReturn("rapter1990");
        // 🔧 tolerant matcher so it works across Selenium variants
        when(d.findElement(argThat(bySelector("meta[name='user-login']")))).thenReturn(meta);

        assertTrue(GithubDomUtil.isLoggedIn(d));
    }

    @Test
    void isLoggedIn_falseWhenNoCookiesNoProfileMenuNoMeta() {
        WebDriver d = mock(WebDriver.class);
        Options opts = mockCookieOptions(d);
        when(opts.getCookieNamed(anyString())).thenReturn(null);

        // No profile menu (empty lists)
        when(d.findElements(any(By.class))).thenReturn(List.of());

        // meta lookup throws NoSuchElementException path
        when(d.findElement(any(By.class))).thenThrow(new NoSuchElementException("no meta"));

        assertFalse(GithubDomUtil.isLoggedIn(d));
    }

    @Test
    void hasFlashError_trueWhenFlashErrorClassExists_falseOtherwise() {
        WebDriver d = mock(WebDriver.class);

        when(d.findElements(argThat(bySelector(".flash-error"))))
                .thenReturn(List.of(mock(WebElement.class)));
        assertTrue(GithubDomUtil.hasFlashError(d));

        // Now return empty for the same selector
        when(d.findElements(argThat(bySelector(".flash-error"))))
                .thenReturn(List.of());
        assertFalse(GithubDomUtil.hasFlashError(d));
    }

    @Test
    void isOtpPage_trueWhenAnyOtpInputExists_falseOtherwise() {
        WebDriver d = mock(WebDriver.class);

        // 1) Non-empty once for the exact selector used in the util
        String otpSelector = "input#otp, input[name='otp'], input[name='verification_code']";
        when(d.findElements(argThat(bySelector(otpSelector))))
                .thenReturn(List.of(mock(WebElement.class)));

        assertTrue(GithubDomUtil.isOtpPage(d));

        // 2) Then empty for the same selector
        when(d.findElements(argThat(bySelector(otpSelector))))
                .thenReturn(List.of());

        assertFalse(GithubDomUtil.isOtpPage(d));
    }

    @Test
    void isMobileVerificationPage_respectsIsLoggedInShortCircuit() {
        WebDriver d = mock(WebDriver.class);

        // Simulate logged-in via cookie path
        Options opts = mockCookieOptions(d);
        when(opts.getCookieNamed("user_session")).thenReturn(new Cookie("user_session", "x"));

        assertFalse(GithubDomUtil.isMobileVerificationPage(d)); // should short-circuit to false
    }

    @Test
    void isMobileVerificationPage_trueOnVerifiedDeviceUrl_orOnRecognizedPageSource() {
        WebDriver d = mock(WebDriver.class);
        Options opts = mockCookieOptions(d);

        // Not logged in via cookies
        when(opts.getCookieNamed(anyString())).thenReturn(null);

        // No profile menu elements
        when(d.findElements(any(By.class))).thenReturn(List.of());

        // Meta <meta name="user-login"> not found (avoid NPE in isLoggedIn)
        when(d.findElement(any(By.class))).thenThrow(new NoSuchElementException("no meta"));

        // Case 1: URL contains verified-device
        when(d.getCurrentUrl()).thenReturn("https://github.com/sessions/verified-device/setup");
        when(d.getPageSource()).thenReturn(""); // ignored because URL already matches
        assertTrue(GithubDomUtil.isMobileVerificationPage(d));

        // Case 2: URL not matching, but page source has recognizable text (case-insensitive)
        when(d.getCurrentUrl()).thenReturn("https://github.com/login");
        when(d.getPageSource()).thenReturn("... Check Your Phone to Approve Sign In via GitHub Mobile ...");
        assertTrue(GithubDomUtil.isMobileVerificationPage(d));

        // Negative
        when(d.getPageSource()).thenReturn("just a regular login screen");
        assertFalse(GithubDomUtil.isMobileVerificationPage(d));
    }

    @Test
    void findByTextContains_returnsFirstMatch_orNullWhenEmpty() {
        WebDriver d = mock(WebDriver.class);

        WebElement el1 = mock(WebElement.class);
        WebElement el2 = mock(WebElement.class);

        // Return two elements for the xpath (we don’t assert the exact xpath, just that it calls findElements)
        when(d.findElements(argThat(byStringContains("xpath: //*[contains(translate(normalize-space("))))
                .thenReturn(list(el1, el2));
        assertSame(el1, GithubDomUtil.findByTextContains(d, "approve sign in"));

        // Now return empty → null
        when(d.findElements(any(By.class))).thenReturn(List.of());
        assertNull(GithubDomUtil.findByTextContains(d, "approve sign in"));
    }

    @Test
    void clickIfPresent_clicksFirstDisplayedAndEnabledElement_andReturnsTrue_elseFalse() {
        WebDriver d = mock(WebDriver.class);

        By first = By.cssSelector("a.first");
        By second = By.cssSelector("button.second");

        WebElement hidden = mock(WebElement.class);
        when(hidden.isDisplayed()).thenReturn(false);
        when(hidden.isEnabled()).thenReturn(true);

        WebElement good = mock(WebElement.class);
        when(good.isDisplayed()).thenReturn(true);
        when(good.isEnabled()).thenReturn(true);

        // Use tolerant matcher so it works across Selenium variants
        when(d.findElements(argThat(bySelector("a.first")))).thenReturn(List.of(hidden));
        when(d.findElements(argThat(bySelector("button.second")))).thenReturn(List.of(good));

        assertTrue(GithubDomUtil.clickIfPresent(d, first, second));
        verify(good, times(1)).click();

        // None clickable → false
        reset(d);
        when(d.findElements(any(By.class))).thenReturn(List.of());
        assertFalse(GithubDomUtil.clickIfPresent(d, first, second));
    }

    @Test
    void type_clearsThenSendsKeys() {
        WebElement el = mock(WebElement.class);
        GithubDomUtil.type(el, "hello");
        verify(el).clear();
        verify(el).sendKeys("hello");
    }

    @Test
    void sleep_doesNotThrow() {
        // Just exercise the try/catch path
        GithubDomUtil.sleep(1);
    }

    // --- Helpers -------------------------------------------------------------

    /** Make a WebDriver mock that also implements JavascriptExecutor. */
    private static WebDriver mockJsDriver() {
        return mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
    }

    private static void stubReadyState(WebDriver driver, String state) {
        when(((JavascriptExecutor) driver).executeScript("return document.readyState")).thenReturn(state);
    }

    private static Options mockCookieOptions(WebDriver d) {
        Options opts = mock(Options.class);
        when(d.manage()).thenReturn(opts);
        return opts;
    }

    private static ArgumentMatcher<By> byStringContains(String needle) {
        return by -> by != null && by.toString().contains(needle);
    }

    private static ArgumentMatcher<By> bySelector(String selector) {
        return by -> by != null && by.toString().toLowerCase().contains(selector.toLowerCase());
    }

    private static List<WebElement> list(WebElement... els) {
        return new ArrayList<>(Arrays.asList(els));
    }

    private static <T> List<T> list(T... xs) { return java.util.Arrays.asList(xs); }

}