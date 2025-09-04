package com.example.githubscreenshotmailer.screenshotmailer.repository;

import com.example.githubscreenshotmailer.screenshotmailer.model.entity.ScreenshotRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScreenshotRecordRepository extends JpaRepository<ScreenshotRecordEntity, String>,
        JpaSpecificationExecutor<ScreenshotRecordEntity> {

}