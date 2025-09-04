package com.example.githubscreenshotmailer.screenshotmailer.model.mapper;

import com.example.githubscreenshotmailer.common.model.CustomPage;
import com.example.githubscreenshotmailer.common.model.dto.response.CustomPagingResponse;
import com.example.githubscreenshotmailer.screenshotmailer.model.ScreenshotRecord;
import com.example.githubscreenshotmailer.screenshotmailer.model.dto.response.ScreenshotResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper interface for converting a {@link CustomPage} of {@link ScreenshotRecord}
 * into a {@link CustomPagingResponse} of {@link ScreenshotResponse}.
 */
@Mapper
public interface CustomPageScreenshotRecordToCustomPagingScreenshotResponseMapper {

    ScreenshotRecordToScreenshotResponseMapper recordMapper =
            Mappers.getMapper(ScreenshotRecordToScreenshotResponseMapper.class);

    /**
     * Converts a {@link CustomPage} of {@link ScreenshotRecord} into a paginated response.
     *
     * @param page the source page of domain objects
     * @return a paginated {@link CustomPagingResponse} of response DTOs
     */
    default CustomPagingResponse<ScreenshotResponse> toPagingResponse(CustomPage<ScreenshotRecord> page) {
        if (page == null) return null;

        return CustomPagingResponse.<ScreenshotResponse>builder()
                .content(toScreenshotResponseList(page.getContent()))
                .totalElementCount(page.getTotalElementCount())
                .totalPageCount(page.getTotalPageCount())
                .pageNumber(page.getPageNumber())
                .pageSize(page.getPageSize())
                .build();
    }

    /**
     * Converts a list of {@link ScreenshotRecord} to a list of {@link ScreenshotResponse}.
     *
     * @param records domain records
     * @return mapped response DTOs
     */
    default List<ScreenshotResponse> toScreenshotResponseList(List<ScreenshotRecord> records) {
        if (records == null) return null;
        return records.stream()
                .map(recordMapper::map)
                .collect(Collectors.toList());
    }

    /**
     * Initializes the mapper instance.
     *
     * @return a singleton instance of the mapper
     */
    static CustomPageScreenshotRecordToCustomPagingScreenshotResponseMapper initialize() {
        return Mappers.getMapper(CustomPageScreenshotRecordToCustomPagingScreenshotResponseMapper.class);
    }

}

