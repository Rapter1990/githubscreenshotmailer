package com.example.githubscreenshotmailer.logging.service;

import com.example.githubscreenshotmailer.logging.model.entity.LogEntity;

/**
 * Service interface for handling log-related operations.
 */
public interface LogService {

    /**
     * Saves the provided {@link LogEntity} to the database.
     *
     * @param logEntity the log entity to persist
     */
    void saveLogToDatabase(final LogEntity logEntity);

}
