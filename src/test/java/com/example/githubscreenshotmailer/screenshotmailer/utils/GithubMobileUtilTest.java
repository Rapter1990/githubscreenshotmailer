package com.example.githubscreenshotmailer.screenshotmailer.utils;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GithubMobileUtilTest {

    @Test
    void extractMobileApprovalDigit_picksDigitsWithLargestFontSize() {
        WebDriver driver = mock(WebDriver.class);

        WebElement hidden = mock(WebElement.class);
        when(hidden.isDisplayed()).thenReturn(false);
        when(hidden.getText()).thenReturn("77"); // should be ignored (not displayed)

        WebElement small = mock(WebElement.class);
        when(small.isDisplayed()).thenReturn(true);
        when(small.getText()).thenReturn(" 12 ");
        when(small.getCssValue("font-size")).thenReturn("20px");

        WebElement medium = mock(WebElement.class);
        when(medium.isDisplayed()).thenReturn(true);
        when(medium.getText()).thenReturn("123"); // 3-digit is allowed
        when(medium.getCssValue("font-size")).thenReturn("48px");

        WebElement large = mock(WebElement.class);
        when(large.isDisplayed()).thenReturn(true);
        when(large.getText()).thenReturn("99");
        when(large.getCssValue("font-size")).thenReturn("56px"); // largest → should win

        when(driver.findElements(any(By.class)))
                .thenReturn(List.of(hidden, small, medium, large));

        String digits = GithubMobileUtil.extractMobileApprovalDigit(driver);
        assertEquals("99", digits, "Should select the digits with the largest font-size");
    }

    @Test
    void extractMobileApprovalDigit_returnsNullWhenNoDigitNodesMatch() {
        WebDriver driver = mock(WebDriver.class);

        WebElement txt = mock(WebElement.class);
        when(txt.isDisplayed()).thenReturn(true);
        when(txt.getText()).thenReturn("No numbers here");
        when(txt.getCssValue("font-size")).thenReturn("32px");

        WebElement empty = mock(WebElement.class);
        when(empty.isDisplayed()).thenReturn(true);
        when(empty.getText()).thenReturn("   ");
        when(empty.getCssValue("font-size")).thenReturn("40px");

        when(driver.findElements(any(By.class)))
                .thenReturn(List.of(txt, empty));

        assertNull(GithubMobileUtil.extractMobileApprovalDigit(driver),
                "Should be null if no 2–3 digit nodes are present");
    }

    @Test
    void extractMobileApprovalDigit_handlesStaleElements_andBadCssValues() {
        WebDriver driver = mock(WebDriver.class);

        WebElement stale = mock(WebElement.class);
        // Throw on isDisplayed to exercise StaleElementReferenceException catch
        when(stale.isDisplayed()).thenThrow(new StaleElementReferenceException("stale"));

        WebElement badCss = mock(WebElement.class);
        when(badCss.isDisplayed()).thenReturn(true);
        when(badCss.getText()).thenReturn("77");
        when(badCss.getCssValue("font-size")).thenReturn("not-a-number"); // parsePx → 0

        WebElement good = mock(WebElement.class);
        when(good.isDisplayed()).thenReturn(true);
        when(good.getText()).thenReturn("88");
        when(good.getCssValue("font-size")).thenReturn("30px"); // beats badCss (0)

        when(driver.findElements(any(By.class)))
                .thenReturn(List.of(stale, badCss, good));

        assertEquals("88", GithubMobileUtil.extractMobileApprovalDigit(driver),
                "Should ignore stale elements and prefer larger parsed font-size");
    }

    @Test
    void extractMobileApprovalDigit_supportsFontSizeWithoutPx() {
        WebDriver driver = mock(WebDriver.class);

        WebElement px = mock(WebElement.class);
        when(px.isDisplayed()).thenReturn(true);
        when(px.getText()).thenReturn("22");
        when(px.getCssValue("font-size")).thenReturn("40px");

        WebElement noPx = mock(WebElement.class);
        when(noPx.isDisplayed()).thenReturn(true);
        when(noPx.getText()).thenReturn("33");
        when(noPx.getCssValue("font-size")).thenReturn("42"); // no 'px' → still valid, larger → win

        when(driver.findElements(any(By.class)))
                .thenReturn(List.of(px, noPx));

        assertEquals("33", GithubMobileUtil.extractMobileApprovalDigit(driver),
                "Should parse numeric font-size even without 'px' suffix");
    }

    @Test
    void extractMobileApprovalDigit_allowsTwoOrThreeDigits_only() {
        WebDriver driver = mock(WebDriver.class);

        WebElement threeDigits = mock(WebElement.class);
        when(threeDigits.isDisplayed()).thenReturn(true);
        when(threeDigits.getText()).thenReturn("123");
        when(threeDigits.getCssValue("font-size")).thenReturn("30px");

        WebElement fourDigits = mock(WebElement.class);
        when(fourDigits.isDisplayed()).thenReturn(true);
        when(fourDigits.getText()).thenReturn("1234"); // should NOT match
        when(fourDigits.getCssValue("font-size")).thenReturn("100px");

        when(driver.findElements(any(By.class)))
                .thenReturn(List.of(threeDigits, fourDigits));

        assertEquals("123", GithubMobileUtil.extractMobileApprovalDigit(driver),
                "Should only match 2–3 digits and ignore longer sequences");
    }

}