package com.example.githubscreenshotmailer.screenshotmailer.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void ensureDailyDir_createsYearMonthDayFolders_andIsIdempotent() throws IOException {
        // when
        Path created = FileUtil.ensureDailyDir(tempDir);

        // then
        LocalDate today = LocalDate.now();
        Path expected = tempDir
                .resolve(String.valueOf(today.getYear()))
                .resolve(String.format("%02d", today.getMonthValue()))
                .resolve(String.format("%02d", today.getDayOfMonth()));

        assertEquals(expected.normalize(), created.normalize(), "Path returned should match Y/M/D layout");
        assertTrue(Files.exists(created), "Directory should exist after creation");
        assertTrue(Files.isDirectory(created), "Created path should be a directory");

        // idempotency: calling again should not throw and should return the same path
        Path createdAgain = FileUtil.ensureDailyDir(tempDir);
        assertEquals(created.normalize(), createdAgain.normalize(), "Second call should return the same path");
        assertTrue(Files.exists(createdAgain), "Directory should still exist");
    }

    @Test
    void suggestPngName_containsUsername_uuidAndPngExtension_andIsUniquePerCall() {
        String username = "rapter1990";

        String name1 = FileUtil.suggestPngName(username);
        String name2 = FileUtil.suggestPngName(username);

        // Format: <username>_<uuid>.png
        Pattern pattern = Pattern.compile("^" + Pattern.quote(username) + "_[0-9a-fA-F\\-]{36}\\.png$");

        assertTrue(pattern.matcher(name1).matches(), "Filename should be '<username>_<uuid>.png'");
        assertTrue(pattern.matcher(name2).matches(), "Filename should be '<username>_<uuid>.png'");

        // Different UUID each time
        assertNotEquals(name1, name2, "Two suggestions should differ (random UUID)");
    }

}
