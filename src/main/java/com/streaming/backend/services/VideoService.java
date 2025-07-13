package com.streaming.backend.services;

import com.streaming.backend.config.VideoStorageConfig;
import com.streaming.backend.models.Video;
import com.streaming.backend.repositories.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import static com.streaming.backend.utilities.Utilities.getFileExtension;

@Service
public class VideoService {

    private final VideoRepository videoRepository;

    @Autowired
    public VideoService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public void processVideoUploaded(MultipartFile file) throws IOException, InterruptedException {
            Video video = videoRepository.save(Video.builder()
                    .title("temp_title")
                    .description("temp_description")
                    .build());

            String extension = getFileExtension(Objects.requireNonNull(file.getOriginalFilename()));
            Path destination = VideoStorageConfig.UPLOAD_DIR.resolve(video.getId() + "." + extension);
            video.setPathArchive(destination.toString());

            SaveVideoFile(file, destination);
            videoRepository.save(video);

    }

    private static void SaveVideoFile(MultipartFile file, Path destination) throws IOException, InterruptedException {
        ByteArrayInputStream convertedVideo = VideoConversionService.convertToMp4(file);
        Files.copy(convertedVideo, destination, StandardCopyOption.REPLACE_EXISTING);
    }
}
