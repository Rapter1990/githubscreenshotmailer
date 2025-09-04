package com.example.githubscreenshotmailer.screenshotmailer.model.mapper;

import com.example.githubscreenshotmailer.common.model.mapper.BaseMapper;
import com.example.githubscreenshotmailer.screenshotmailer.model.ScreenshotRecord;
import com.example.githubscreenshotmailer.screenshotmailer.model.entity.ScreenshotRecordEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Mapper
public interface ScreenshotRecordEntityToScreenshotRecordMapper
        extends BaseMapper<ScreenshotRecordEntity, ScreenshotRecord> {

    /**
     * Maps an entity to the domain record.
     */
    @Named("mapFromEntity")
    default ScreenshotRecord mapFromEntity(ScreenshotRecordEntity entity) {
        if (entity == null) return null;
        return new ScreenshotRecord(
                entity.getId(),
                entity.getGithubUsername(),
                entity.getRecipientEmail(),
                entity.getFileName(),
                entity.getFilePath(),
                entity.getFileSizeBytes(),
                entity.getSentAt(),
                entity.getStatus()
        );
    }

    @Override
    default ScreenshotRecord map(ScreenshotRecordEntity source) {
        return mapFromEntity(source);
    }

    @Override
    default List<ScreenshotRecord> map(Collection<ScreenshotRecordEntity> sources) {
        if (sources == null) return List.of();
        return sources.stream()
                .filter(Objects::nonNull)
                .map(this::mapFromEntity)
                .toList();
    }

    /**
     * Initializes and returns a mapper instance.
     */
    static ScreenshotRecordEntityToScreenshotRecordMapper initialize() {
        return Mappers.getMapper(ScreenshotRecordEntityToScreenshotRecordMapper.class);
    }

}
