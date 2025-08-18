package com.streaming.backend.dto;

import com.streaming.backend.models.Video;
import com.streaming.backend.utilities.Utilities;

public record VideoResponseDTO(
        String title,
        String description,
        String publicUrl
) {

    public static VideoResponseDTO from(Video video) {
        return new VideoResponseDTO(
                video.getTitle(),
                video.getDescription(),
                Utilities.extractVarPath(video.getPathArchive())
        );
    }
}
