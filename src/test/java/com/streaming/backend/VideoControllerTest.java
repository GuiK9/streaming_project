package com.streaming.backend;

import com.streaming.backend.config.DataSourceProperties;
import com.streaming.backend.config.DatabaseTestInitializer;
import com.streaming.backend.config.VideoStorageConfig;
import com.streaming.backend.models.Video;
import com.streaming.backend.repositories.VideoRepository;
import com.streaming.backend.utilities.ProfileChecker;
import com.streaming.backend.utilities.Utilities;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = DatabaseTestInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VideoRepository videoRepository;

    @Value("${logging.path.logs}")
    private String logPath;

    @Autowired
    private DataSourceProperties dataSourceProperties;

    ProfileChecker profileChecker;

    @AfterAll
    void dropDatabaseAfterTests() throws SQLException {
        String adminUrl = "jdbc:postgresql://localhost:5432/postgres";
        String username = dataSourceProperties.getUsername();
        String password = dataSourceProperties.getPassword();

        try (Connection conn = DriverManager.getConnection(adminUrl, username, password)) {
            conn.createStatement().execute(
                    "SELECT pg_terminate_backend(pid) " +
                            "FROM pg_stat_activity " +
                            "WHERE datname = 'streaming_test' AND pid <> pg_backend_pid();"
            );

            conn.createStatement().execute("DROP DATABASE streaming_test");
        }
    }

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
        File file = new File(Objects
                .requireNonNull(getClass().getClassLoader().getResource("videos/test_video.webm")).getFile());

        mockMvc.perform(post("/api/videos")
                        .contentType("application/octet-stream")
                        .content(Files.readAllBytes(file.toPath())))
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
        File file = new File(getClass().getClassLoader().getResource("videos/old_meme.mp4").getFile());

        mockMvc.perform(post("/api/videos")
                        .contentType("application/octet-stream")
                        .content(Files.readAllBytes(file.toPath())))
                .andExpect(status().isCreated());

        List<Video> videos = videoRepository.findAll();
        Video video = videos.get(0);
        assertEquals("mp4", Utilities.getFileExtension(video.getPathArchive()));
    }

    @Test
    public void shouldMadeRollbackInfailInsertCase() throws Exception {
        byte[] badBytes = "bytes_WithError".getBytes();

        mockMvc.perform(post("/api/videos")
                        .contentType("application/octet-stream")
                        .content(badBytes))
                .andExpect(status().isInternalServerError());

        List<Video> videos = videoRepository.findAll();
        assertEquals(0, videos.size());
    }

    @Test
    public void shouldCreateLogAndTestHisLocations() throws Exception {
        LocalDate fixedDate = LocalDate.now();
        String month = String.format("%02d", fixedDate.getMonthValue());
        String day = String.format("%02d", fixedDate.getDayOfMonth());

        byte[] badBytes = "bytes_WithError".getBytes();

        mockMvc.perform(post("/api/videos")
                .contentType("application/octet-stream")
                .content(badBytes));

        Path logFile = Paths.get(logPath, month, day + ".log");
        assertTrue(Files.exists(logFile), "Log file should exist: " + logFile.toAbsolutePath());
    }
}
