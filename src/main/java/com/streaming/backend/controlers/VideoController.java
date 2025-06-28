package com.streaming.backend.controlers;

import com.streaming.backend.repositories.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoRepository videoRepository;

    @Value("${videos.upload-dir}")
    private String uploadDir;

    public VideoController(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }
}
