package com.example.githubscreenshotmailer.screenshotmailer.utils;

import lombok.experimental.UtilityClass;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class ScreenshotUtil {

    /** Full-page PNG via CDP (no manual scrolling). */
    public byte[] captureFullPagePng(ChromeDriver driver) {

        JavascriptExecutor js = driver;

        Number width  = (Number) js.executeScript(
                "return Math.max(document.body.scrollWidth, document.documentElement.scrollWidth, " +
                        "            document.documentElement.clientWidth);");
        Number height = (Number) js.executeScript(
                "return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight, " +
                        "            document.documentElement.clientHeight);");
        Number dpr    = (Number) js.executeScript("return window.devicePixelRatio || 1;");

        int w = Math.max(1, width.intValue());
        int h = Math.max(1, height.intValue());
        double scale = dpr.doubleValue();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("mobile", false);
        metrics.put("width", w);
        metrics.put("height", h);
        metrics.put("deviceScaleFactor", scale);
        metrics.put("scale", 1);

        driver.executeCdpCommand("Emulation.setDeviceMetricsOverride", metrics);

        Map<String, Object> visible = new HashMap<>();
        visible.put("width", w);
        visible.put("height", h);
        driver.executeCdpCommand("Emulation.setVisibleSize", visible);

        Map<String, Object> shotParams = new HashMap<>();
        shotParams.put("fromSurface", true);
        shotParams.put("captureBeyondViewport", true);
        Map<String, Object> result = driver.executeCdpCommand("Page.captureScreenshot", shotParams);

        driver.executeCdpCommand("Emulation.clearDeviceMetricsOverride", new HashMap<>());

        String base64 = (String) result.get("data");
        return Base64.getDecoder().decode(base64);
    }

}
