package com.example.githubscreenshotmailer.screenshotmailer.repository;

import com.example.githubscreenshotmailer.screenshotmailer.model.entity.ScreenshotRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenshotRecordRepository extends JpaRepository<ScreenshotRecordEntity, String> {

}
