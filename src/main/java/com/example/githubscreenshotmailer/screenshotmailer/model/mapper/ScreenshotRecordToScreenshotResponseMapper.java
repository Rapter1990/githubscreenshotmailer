package com.example.githubscreenshotmailer.screenshotmailer.model.mapper;

import com.example.githubscreenshotmailer.common.model.mapper.BaseMapper;
import com.example.githubscreenshotmailer.screenshotmailer.model.ScreenshotRecord;
import com.example.githubscreenshotmailer.screenshotmailer.model.dto.response.ScreenshotResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Mapper
public interface ScreenshotRecordToScreenshotResponseMapper
        extends BaseMapper<ScreenshotRecord, ScreenshotResponse> {

    /**
     * Maps domain record to API response DTO.
     */
    @Named("mapToResponse")
    default ScreenshotResponse mapToResponse(ScreenshotRecord record) {
        if (record == null)
            return null;

        return new ScreenshotResponse(
                record.imageId(),
                record.githubUsername(),
                record.recipientEmail(),
                record.fileName(),
                record.path(),
                record.fileSize(),
                record.sentAt(),
                record.status() != null ? record.status().name() : null
        );
    }

    @Override
    default ScreenshotResponse map(ScreenshotRecord source) {
        return mapToResponse(source);
    }

    @Override
    default List<ScreenshotResponse> map(Collection<ScreenshotRecord> sources) {
        if (sources == null) return List.of();
        return sources.stream()
                .filter(Objects::nonNull)
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Initializes and returns a mapper instance.
     */
    static ScreenshotRecordToScreenshotResponseMapper initialize() {
        return Mappers.getMapper(ScreenshotRecordToScreenshotResponseMapper.class);
    }

}
