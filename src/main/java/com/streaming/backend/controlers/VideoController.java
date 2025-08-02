package com.streaming.backend.controlers;

import com.streaming.backend.services.VideoConversionService;
import com.streaming.backend.services.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;

    private static final Logger logger = LoggerFactory.getLogger(VideoConversionService.class);

    @Autowired
    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("")
    public ResponseEntity<?> uploadVideo(@RequestBody byte[] file) {
        try {
            videoService.processVideoUploaded(file);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            logger.error("Error when saving video", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error when saving the video: " + e.getMessage());
        }
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<String> getVideoUrl(@PathVariable Long videoId) {
        try{
            String publicUrl = videoService.getPublicURl(videoId);
            if (publicUrl == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.OK).body(publicUrl);
        } catch (Exception e) {
            logger.error("Failed to generate public video URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating video URL: " + e.getMessage());
        }
    }
}
