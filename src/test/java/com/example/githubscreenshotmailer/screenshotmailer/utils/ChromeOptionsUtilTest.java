package com.example.githubscreenshotmailer.screenshotmailer.utils;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChromeOptionsUtilTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractGoog(ChromeOptions options) {
        Map<String, Object> caps = options.asMap();
        return (Map<String, Object>) caps.get("goog:chromeOptions");
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractArgs(ChromeOptions options) {
        Map<String, Object> goog = extractGoog(options);
        assertNotNull(goog, "goog:chromeOptions should be present");
        return (List<String>) goog.get("args");
    }

    /** Normalizes excludeSwitches to a List<String>, whether it is stored as List or String[]. */
    private static List<String> extractExcludeSwitches(ChromeOptions options) {
        Object raw = extractGoog(options).get("excludeSwitches");
        assertNotNull(raw, "excludeSwitches should be present");
        if (raw instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> asList = (List<String>) raw;
            return asList;
        }
        if (raw instanceof String[]) {
            return Arrays.asList((String[]) raw);
        }
        fail("Unexpected type for excludeSwitches: " + raw.getClass());
        return List.of();
    }

    @Test
    void headless_true_includesHeadlessAndAllOtherArguments_andSetsExperimentalOptions() {
        ChromeOptions options = ChromeOptionsUtil.headless(true);

        // 1) Arguments
        List<String> args = extractArgs(options);
        assertNotNull(args);
        assertTrue(args.contains("--headless=new"), "Should include --headless=new when headless=true");
        assertTrue(args.containsAll(List.of(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--window-size=1366,768",
                "--lang=en-US",
                "--disable-blink-features=AutomationControlled"
        )));
        assertTrue(args.stream().anyMatch(a -> a.startsWith("user-agent=Mozilla/5.0")),
                "Should include a user-agent argument");

        // 2) Experimental options
        Map<String, Object> goog = extractGoog(options);
        List<String> exclude = extractExcludeSwitches(options);
        assertTrue(exclude.contains("enable-automation"),
                "excludeSwitches must contain enable-automation");
        assertEquals(Boolean.FALSE, goog.get("useAutomationExtension"),
                "useAutomationExtension should be false");
    }

    @Test
    void headless_false_omitsHeadlessArgument_butKeepsOthers_andSetsExperimentalOptions() {
        ChromeOptions options = ChromeOptionsUtil.headless(false);

        // 1) Arguments
        List<String> args = extractArgs(options);
        assertNotNull(args);
        assertFalse(args.contains("--headless=new"), "Should NOT include --headless=new when headless=false");
        assertTrue(args.containsAll(List.of(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--window-size=1366,768",
                "--lang=en-US",
                "--disable-blink-features=AutomationControlled"
        )));
        assertTrue(args.stream().anyMatch(a -> a.startsWith("user-agent=Mozilla/5.0")),
                "Should include a user-agent argument");

        // 2) Experimental options
        Map<String, Object> goog = extractGoog(options);
        List<String> exclude = extractExcludeSwitches(options);
        assertTrue(exclude.contains("enable-automation"),
                "excludeSwitches must contain enable-automation");
        assertEquals(Boolean.FALSE, goog.get("useAutomationExtension"));
    }

}
