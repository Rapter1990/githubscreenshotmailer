package com.example.githubscreenshotmailer.repository;

import com.example.githubscreenshotmailer.model.entity.ScreenshotRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenshotRecordRepository extends JpaRepository<ScreenshotRecordEntity, String> {

}
