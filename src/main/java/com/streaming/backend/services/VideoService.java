package com.streaming.backend.services;

import com.streaming.backend.config.VideoStorageConfig;
import com.streaming.backend.dto.RequestCreateVideoDTO;
import com.streaming.backend.dto.VideoResponseDTO;
import com.streaming.backend.models.Video;
import com.streaming.backend.repositories.VideoRepository;
import com.streaming.backend.utilities.Utilities;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public VideoResponseDTO processVideoUploaded(MultipartFile fileBytes, RequestCreateVideoDTO requestCreateVideoDTO) throws IOException, InterruptedException {
        Video video = Video.builder()
                .title(requestCreateVideoDTO.title())
                .description(requestCreateVideoDTO.description())
                .build();

        Path tempFilePath = Files.createTempFile("upload_", ".tmp");
        Files.write(tempFilePath, fileBytes.getBytes());
        File tempFile = tempFilePath.toFile();

        Path destination = VideoStorageConfig.UPLOAD_DIR.resolve(getNextSeqVideo() + ".mp4");
        video.setPathArchive(destination.toString());

        saveVideoFile(tempFile, destination);

        //noinspection ResultOfMethodCallIgnored
        tempFile.delete();

        VideoResponseDTO videoDTO = new VideoResponseDTO(
                video.getTitle(), video.getDescription(), Utilities.extractVarPath(video.getPathArchive()));

        videoRepository.save(video);

        return videoDTO;
    }


    private static void saveVideoFile(File file, Path destination) throws IOException, InterruptedException {
        //ByteArrayInputStream convertedVideo = VideoConversionService.convertToMp4(file);
        Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_WRITE);
        perms.add(PosixFilePermission.OTHERS_READ);
        Files.setPosixFilePermissions(destination, perms);
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
            return Utilities.extractVarPath(pathArchive);
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    public List<VideoResponseDTO> videosReturnAllVideos() {
        return videoRepository.findAll().stream()
                .map(VideoResponseDTO::from)
                .toList();
    }
}
