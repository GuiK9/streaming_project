package com.streaming.backend.services;

import com.streaming.backend.config.VideoStorageConfig;
import com.streaming.backend.models.Video;
import com.streaming.backend.repositories.VideoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class VideoService {

    private final VideoRepository videoRepository;

    @Autowired
    public VideoService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(rollbackFor = Exception.class)
    public void processVideoUploaded(byte[] fileBytes) throws IOException, InterruptedException {
        Video video = Video.builder()
                .title("temp_title")
                .description("temp_description")
                .build();

        Path tempFilePath = Files.createTempFile("upload_", ".tmp");
        Files.write(tempFilePath, fileBytes);
        File tempFile = tempFilePath.toFile();

        Path destination = VideoStorageConfig.UPLOAD_DIR.resolve(getNextSeqVideo() + ".mp4");
        video.setPathArchive(destination.toString());

        saveVideoFile(tempFile, destination);

        //noinspection ResultOfMethodCallIgnored
        tempFile.delete();

        videoRepository.save(video);
    }


    private static void saveVideoFile(File file, Path destination) throws IOException, InterruptedException {
        ByteArrayInputStream convertedVideo = VideoConversionService.convertToMp4(file);
        Files.copy(convertedVideo, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public Long getNextSeqVideo() {
        return ((Number) entityManager
                .createNativeQuery("SELECT nextval('video_file_seq')")
                .getSingleResult())
                .longValue();
    }

    public String getPublicURl(Long videoId) {
        try {
            String pathArchive = videoRepository.getReferenceById(videoId).getPathArchive();
            return extractVarPath(pathArchive);
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    public String extractVarPath(String fullPath) {
        String marker = "/var/";
        int index = fullPath.indexOf(marker);
        if (index != -1) {
            return fullPath.substring(index);
        } else {
            throw new IllegalArgumentException("Path does not contain /var/: " + fullPath);
        }
    }
}
