package com.streaming.backend;

import com.streaming.backend.config.VideoStorageConfig;
import com.streaming.backend.models.Video;
import com.streaming.backend.repositories.VideoRepository;
import com.streaming.backend.utilities.Utilities;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
public class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VideoRepository videoRepository;

    @BeforeEach
    public void beforeEach() throws IOException {
        cleanUploadDir();
    }

    @AfterEach
    public void afterEach() throws IOException {
        cleanUploadDir();
    }

    public static void cleanUploadDir() throws IOException {
        Path uploadDir = VideoStorageConfig.UPLOAD_DIR;
        Files.createDirectories(uploadDir);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadDir)) {
            for (Path file : stream) {
                Files.deleteIfExists(file);
            }
        }
    }

    @Test
    public void shouldSaveVideoInTheCorrectLocationWithNameBasedOnId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "conteudo fake".getBytes());

        mockMvc.perform(multipart("/api/videos")
                .file(file))
                .andExpect(status().isCreated());

        List<Video> videos = videoRepository.findAll();
        assertEquals(1, videos.size());

        Video video = videos.get(0);
        Path expectedPath = VideoStorageConfig.UPLOAD_DIR.resolve(video.getId() + ".mp4");
        assertEquals(expectedPath.toString(), video.getPathArchive());

        assertTrue(Files.exists(expectedPath));
    }

    @Test
    public void shouldConvertTheVideoThenSave() throws Exception {
        FileInputStream fileInputStream = (FileInputStream) getClass().getClassLoader().getResourceAsStream("videos/old_meme");

        MockMultipartFile file = new MockMultipartFile(
                "file", "old_meme.mp4","video/mp4", fileInputStream);

        mockMvc.perform(multipart("/api/videos")
                .file(file))
                .andExpect(status().isCreated());

        List<Video> videos = videoRepository.findAll();
        Video video = videos.get(0);
        assertEquals("mp4", Utilities.getFileExtension(video.getPathArchive()));
    }

    @Test
    public void shouldMadeRollbackInfailInsertCase() throws Exception {
        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "file", "old_mememp4","video/x-matroska", "bytes_WithError".getBytes());

        mockMvc.perform(multipart("/api/videos")
                        .file(mockMultipartFile))
                .andExpect(status().isInternalServerError());

        List<Video> videos = videoRepository.findAll();
        assertEquals(0, videos.size());
    }
}
