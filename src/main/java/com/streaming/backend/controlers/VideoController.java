package com.streaming.backend.controlers;

import com.streaming.backend.dto.RequestCreateVideoDTO;
import com.streaming.backend.dto.VideoResponseDTO;
import com.streaming.backend.services.VideoConversionService;
import com.streaming.backend.services.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;

    private static final Logger logger = LoggerFactory.getLogger(VideoConversionService.class);

    @Autowired
    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    // TODO update to use a public url with proxy from nginx
    @PostMapping("")
    public ResponseEntity<?> uploadVideo(
            @RequestPart("metadata") RequestCreateVideoDTO requestCreateVideoDTO,
            @RequestPart("video") MultipartFile fileVideo
    ) { // update just for CI/CD demonstration
        try {
            VideoResponseDTO videoResponseDTO = videoService.processVideoUploaded(fileVideo, requestCreateVideoDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(videoResponseDTO);
        } catch (Exception e) {
            logger.error("Error when saving video", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error when saving the video");
        }
    }
//TODO Use DTO
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating video URL");
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("ok");
    }

    @GetMapping(value = "/pix", produces = "application/json")
    public ResponseEntity<Map<String, Object>> pixLocation() {

        Map<String, Object> calendario = new HashMap<>();
        calendario.put("expiracao", 3600);

        Map<String, Object> valor = new HashMap<>();
        valor.put("original", "250.00");

        Map<String, Object> response = new HashMap<>();
        response.put("calendario", calendario);
        response.put("valor", valor);
        response.put("chave", "4a98c3ed-6a2c-4c46-99ab-544f62e5584a");
        response.put("txid", "735e8b809966477ab59eb3d4bb9adb5d");

        return ResponseEntity.ok(response);
    }

    @GetMapping("")
    public ResponseEntity<?> getAllVideos() {
        try {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(videoService.videosReturnAllVideos());
        } catch (Exception e) {
            logger.error("Failed to return all videos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error returning all videos");
        }
    }
}
