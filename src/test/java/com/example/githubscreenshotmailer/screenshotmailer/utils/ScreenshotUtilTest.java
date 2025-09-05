package com.example.githubscreenshotmailer.screenshotmailer.utils;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScreenshotUtilTest {

    @Test
    void captureFullPagePng_invokesCdpWithExpectedParams_andDecodesBase64() {
        // ----------------------
        // Given
        // ----------------------
        ChromeDriver driver = mock(ChromeDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // JS probes: width, height, DPR
        when(js.executeScript(
                "return Math.max(document.body.scrollWidth, document.documentElement.scrollWidth, " +
                        "            document.documentElement.clientWidth);"))
                .thenReturn(800); // width
        when(js.executeScript(
                "return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight, " +
                        "            document.documentElement.clientHeight);"))
                .thenReturn(1200); // height
        when(js.executeScript("return window.devicePixelRatio || 1;"))
                .thenReturn(2.0); // DPR

        // CDP mock returns
        when(driver.executeCdpCommand(eq("Emulation.setDeviceMetricsOverride"), anyMap()))
                .thenReturn(new HashMap<>());
        when(driver.executeCdpCommand(eq("Emulation.setVisibleSize"), anyMap()))
                .thenReturn(new HashMap<>());
        byte[] expected = "test-bytes-123".getBytes();
        String b64 = Base64.getEncoder().encodeToString(expected);
        Map<String, Object> screenshotResult = new HashMap<>();
        screenshotResult.put("data", b64);
        when(driver.executeCdpCommand(eq("Page.captureScreenshot"), anyMap()))
                .thenReturn(screenshotResult);
        when(driver.executeCdpCommand(eq("Emulation.clearDeviceMetricsOverride"), anyMap()))
                .thenReturn(new HashMap<>());

        // ----------------------
        // When
        // ----------------------
        byte[] actual = ScreenshotUtil.captureFullPagePng(driver);

        // ----------------------
        // Then
        // ----------------------
        assertArrayEquals(expected, actual, "Decoded screenshot bytes should match the base64 payload");

        // ----------------------
        // Verify (order + args)
        // ----------------------
        InOrder order = inOrder(js, driver);

        // JS probes first (width → height → DPR)
        order.verify(js).executeScript(
                "return Math.max(document.body.scrollWidth, document.documentElement.scrollWidth, " +
                        "            document.documentElement.clientWidth);");
        order.verify(js).executeScript(
                "return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight, " +
                        "            document.documentElement.clientHeight);");
        order.verify(js).executeScript("return window.devicePixelRatio || 1;");

        // Emulation.setDeviceMetricsOverride with expected metrics
        order.verify(driver).executeCdpCommand(
                eq("Emulation.setDeviceMetricsOverride"),
                argThat(m ->
                        Boolean.FALSE.equals(m.get("mobile")) &&
                                Integer.valueOf(800).equals(m.get("width")) &&
                                Integer.valueOf(1200).equals(m.get("height")) &&
                                Double.valueOf(2.0).equals(m.get("deviceScaleFactor")) &&
                                Integer.valueOf(1).equals(m.get("scale"))
                )
        );

        // Emulation.setVisibleSize with width/height
        order.verify(driver).executeCdpCommand(
                eq("Emulation.setVisibleSize"),
                argThat(m ->
                        Integer.valueOf(800).equals(m.get("width")) &&
                                Integer.valueOf(1200).equals(m.get("height"))
                )
        );

        // Page.captureScreenshot with flags
        order.verify(driver).executeCdpCommand(
                eq("Page.captureScreenshot"),
                argThat(m ->
                        Boolean.TRUE.equals(m.get("fromSurface")) &&
                                Boolean.TRUE.equals(m.get("captureBeyondViewport"))
                )
        );

        // Emulation.clearDeviceMetricsOverride
        order.verify(driver).executeCdpCommand(eq("Emulation.clearDeviceMetricsOverride"), anyMap());

        // Nothing else
        verifyNoMoreInteractions(driver, js);
    }

}