package com.example.githubscreenshotmailer.screenshotmailer.utils;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

@UtilityClass
public class FileUtil {

    public Path ensureDailyDir(Path baseDir) throws IOException {
        LocalDate d = LocalDate.now();
        Path p = baseDir
                .resolve(String.valueOf(d.getYear()))
                .resolve(String.format("%02d", d.getMonthValue()))
                .resolve(String.format("%02d", d.getDayOfMonth()));
        Files.createDirectories(p);
        return p;
    }

    public String suggestPngName(String githubUsername) {
        return "%s_%s.png".formatted(githubUsername, UUID.randomUUID());
    }

}
