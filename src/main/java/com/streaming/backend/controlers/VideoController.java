package com.streaming.backend.controlers;

import com.streaming.backend.config.VideoStorageConfig;
import com.streaming.backend.models.Video;
import com.streaming.backend.repositories.VideoRepository;
import com.streaming.backend.services.VideoConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import static com.streaming.backend.utilities.Utilities.getFileExtension;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoRepository videoRepository;

    public VideoController(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @PostMapping("")
    public ResponseEntity<Void> uploadVideo(@RequestParam("file") MultipartFile file) throws IOException {
        Video video = videoRepository.save(Video.builder() // TODO verify rollback
                .title("temp_title") // TODO receive title and description
                .description("temp_description")
                .build());

        Files.createDirectories(VideoStorageConfig.UPLOAD_DIR); // TODO Send to inicalization of the aplication
        String extension = getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));
        Path destination = VideoStorageConfig.UPLOAD_DIR.resolve(video.getId() + "." + extension);

        SaveVideoFile(file, destination);

        video.setPathArchive(destination.toString());
        videoRepository.save(video);

        return ResponseEntity.status(HttpStatus.CREATED).build();

    }

    private static void SaveVideoFile(MultipartFile file, Path destination) throws IOException {
        try {
            ByteArrayInputStream convertedVideo = VideoConversionService.convertToMp4(file);
            Files.copy(convertedVideo, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (InterruptedException e) {
            throw new RuntimeException(e); // TODO log system
        }
    }
}
