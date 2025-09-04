package com.example.githubscreenshotmailer.screenshotmailer.utils;

import lombok.experimental.UtilityClass;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;

@UtilityClass
public class GithubDomUtil {

    public ExpectedCondition<Boolean> pageLoaded() {
        return d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState"));
    }

    /** Consider session established if any of these are present. */
    public boolean isLoggedIn(WebDriver driver) {
        try {
            Cookie userSession = driver.manage().getCookieNamed("user_session");
            Cookie loggedIn    = driver.manage().getCookieNamed("logged_in");
            Cookie dotcomUser  = driver.manage().getCookieNamed("dotcom_user");
            if (userSession != null) return true;
            if (loggedIn != null && "yes".equalsIgnoreCase(loggedIn.getValue())) return true;
            if (dotcomUser != null && dotcomUser.getValue() != null && !dotcomUser.getValue().isBlank()) return true;
        } catch (Exception ignored) {}

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

    public boolean hasFlashError(WebDriver driver) {
        return !driver.findElements(By.cssSelector(".flash-error")).isEmpty();
    }

    public boolean isOtpPage(WebDriver driver) {
        return !driver.findElements(By.cssSelector("input#otp, input[name='otp'], input[name='verification_code']")).isEmpty();
    }

    public boolean isMobileVerificationPage(WebDriver driver) {
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

    public WebElement findByTextContains(WebDriver driver, String textLower) {
        String xpath = "//*[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '"
                + textLower + "')]";
        var els = driver.findElements(By.xpath(xpath));
        return els.isEmpty() ? null : els.get(0);
    }

    public boolean clickIfPresent(WebDriver driver, By... locators) {
        for (By by : locators) {
            var els = driver.findElements(by);
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

    public void type(WebElement el, String text) {
        el.clear();
        el.sendKeys(text);
    }

    public void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

}
