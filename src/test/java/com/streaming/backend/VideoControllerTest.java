package com.streaming.backend;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.streaming.backend.config.VideoStorageConfig;
import com.streaming.backend.models.Video;
import com.streaming.backend.repositories.VideoRepository;
import com.streaming.backend.utilities.Utilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Iterator;
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

    @Value("${logging.path.logs}")
    private String logPath;

    @BeforeEach
    public void beforeEach() throws IOException {
        cleanUploadDir();
    }

    @AfterEach
    public void afterEach() throws IOException {
        cleanUploadDir();
    }

    private void cleanUpLogDir() throws IOException {
        Path path = Paths.get(logPath);

        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    private void cleanUploadDir() throws IOException {
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
                "file", "video.mp4", "video/mp4", "content fake".getBytes());

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
                "file", "videomp4","video/x-matroska", "bytes_WithError".getBytes());

        mockMvc.perform(multipart("/api/videos")
                        .file(mockMultipartFile))
                .andExpect(status().isInternalServerError());

        List<Video> videos = videoRepository.findAll();
        assertEquals(0, videos.size());
    }

    @Test
    public void shouldCreateLogAndTestHisLocations() throws Exception {
        LocalDate fixedDate = LocalDate.now();
        String month = String.format("%02d", fixedDate.getMonthValue());
        String day = String.format("%02d", fixedDate.getDayOfMonth());

        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "file", "videomp4","video/x-matroska", "bytes_WithError".getBytes());

        mockMvc.perform(multipart("/api/videos")
                        .file(mockMultipartFile));

        Path logFile = Paths.get(logPath, month, day + ".log");
        assertTrue(Files.exists(logFile), "Log file should exist: " + logFile.toAbsolutePath());
    }
}
