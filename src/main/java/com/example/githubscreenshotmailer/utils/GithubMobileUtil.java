package com.example.githubscreenshotmailer.utils;

import lombok.experimental.UtilityClass;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;

@UtilityClass
public class GithubMobileUtil {

    /** Extracts the big 2–3 digit number shown on the GitHub Mobile verification page. May return null. */
    public String extractMobileApprovalDigit(WebDriver driver) {
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        var nodes = driver.findElements(By.cssSelector(
                "h1, h2, h3, .h0, .h1, .h2, .f0, .f1, .f2, .f3, strong, b, p, span, div"));

        var pattern = java.util.regex.Pattern.compile("^\\s*(\\d{2,3})\\s*$");
        String bestDigits = null;
        double bestFontPx = -1;

        for (WebElement el : nodes) {
            try {
                if (!el.isDisplayed()) continue;
                String txt = el.getText();
                if (txt == null) continue;
                var m = pattern.matcher(txt);
                if (!m.matches()) continue;

                String fs = el.getCssValue("font-size"); // e.g., "56px"
                double px = parsePx(fs);
                if (px > bestFontPx) {
                    bestFontPx = px;
                    bestDigits = m.group(1);
                }
            } catch (StaleElementReferenceException ignored) {
            } catch (Exception ignored) {}
        }
        return bestDigits;
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

}
