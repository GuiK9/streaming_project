package com.streaming.backend.controlers;

import com.streaming.backend.config.VideoStorageConfig;
import com.streaming.backend.models.Video;
import com.streaming.backend.repositories.VideoRepository;
import org.hibernate.tool.schema.internal.StandardTableCleaner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        Video video = videoRepository.save(Video.builder()
                .title("temp_title")
                .description("temp_description")
                .build());

        Files.createDirectories(VideoStorageConfig.UPLOAD_DIR);

        String extension = getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));
        Path destination = VideoStorageConfig.UPLOAD_DIR.resolve(video.getId() + "." + extension);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        video.setPathArchive(destination.toString());
        videoRepository.save(video);

        return ResponseEntity.status(HttpStatus.CREATED).build();

    }


}
