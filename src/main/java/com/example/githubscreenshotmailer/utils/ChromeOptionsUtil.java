package com.example.githubscreenshotmailer.utils;

import lombok.experimental.UtilityClass;
import org.openqa.selenium.chrome.ChromeOptions;

@UtilityClass
public class ChromeOptionsUtil {

    public ChromeOptions headless(boolean headless) {

        ChromeOptions options = new ChromeOptions();
        if (headless) options.addArguments("--headless=new");
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

}
