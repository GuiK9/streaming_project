package com.streaming.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streaming.backend.config.DataSourceProperties;
import com.streaming.backend.config.DatabaseTestInitializer;
import com.streaming.backend.config.VideoStorageConfig;
import com.streaming.backend.dto.RequestCreateVideoDTO;
import com.streaming.backend.models.Video;
import com.streaming.backend.repositories.VideoRepository;
import com.streaming.backend.utilities.Util;
import com.streaming.backend.utilities.Utilities;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = DatabaseTestInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VideoControllerTest {

    @Autowired
    private Util util;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VideoRepository videoRepository;

    @Value("${logging.path.logs}")
    private String logPath;

    @Autowired
    private DataSourceProperties dataSourceProperties;

    @Autowired
    private ObjectMapper objectMapper;


    @AfterAll
    void dropDatabaseAfterTests() throws SQLException {
        util.createConnection(dataSourceProperties.getUsername(),
                dataSourceProperties.getPassword()).createStatement().execute("DROP DATABASE streaming_test");
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        videoRepository.deleteAll();
        util.resetVideoSequence();
        util.cleanUploadDir();
    }

    @AfterEach
    public void afterEach() throws IOException{
        util.cleanUploadDir();
    }


    @Test
    public void shouldSaveVideoInTheCorrectLocationWithNameBasedOnId() throws Exception {
        File file = new File(Objects
                .requireNonNull(getClass().getClassLoader().getResource("videos/test_video.webm")).getFile());

        RequestCreateVideoDTO metadata =
                new RequestCreateVideoDTO("title_temp", "description_test");

        mockMvc.perform(Util.buildVideoUploadRequest(Files.readAllBytes(file.toPath()), metadata))
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
        File file = new File(Objects
                .requireNonNull(Util.class.getClassLoader().getResource("videos/test_video.webm")).getFile());

        RequestCreateVideoDTO metadata =
                new RequestCreateVideoDTO("title_temp", "description_test");

        mockMvc.perform(Util.buildVideoUploadRequest(Files.readAllBytes(file.toPath()), metadata))
                .andExpect(status().isCreated());

        List<Video> videos = videoRepository.findAll();
        Video video = videos.get(0);
        assertEquals("mp4", Utilities.getFileExtension(video.getPathArchive()));
    }

    @Test
    public void shouldMadeRollbackInfailInsertCase() throws Exception {
        byte[] badBytes = "bytes_WithError".getBytes();

        RequestCreateVideoDTO metadata =
                new RequestCreateVideoDTO("title_temp", "description_test");

        mockMvc.perform(Util.buildVideoUploadRequest(badBytes, metadata))
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

    @Test
    public void shouldReceiveAVideoFromIdAndResolveHisLinkPath() throws Exception {
        // Need to populate database for this test
        File file = new File(Objects
                .requireNonNull(Util.class.getClassLoader().getResource("videos/test_video.webm")).getFile());

        RequestCreateVideoDTO metadata =
                new RequestCreateVideoDTO("title_temp", "description_test");

        mockMvc.perform(Util.buildVideoUploadRequest(Files.readAllBytes(file.toPath()), metadata))
                .andExpect(status().isCreated());


        MvcResult mvcResult = mockMvc.perform(get("/api/videos/1")).andExpect(status().isOk()).andReturn();
        String publicUrl = mvcResult.getResponse().getContentAsString();

        assertThat(publicUrl).startsWith("/var/www/videos/");
    }

    @Test
    public void shouldReceiveNotFoundCaseIdIsNotValid() throws Exception {
        mockMvc.perform(get("/api/videos/9999")).andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnAllDataOfVideos() throws Exception {
        File file = new File(Objects
                .requireNonNull(getClass().getClassLoader().getResource("videos/test_video.webm")).getFile());

        RequestCreateVideoDTO metadata =
                new RequestCreateVideoDTO("title_temp", "description_test");

        int amount = 5;
        for (int i = 0; i < amount; i++) {
            mockMvc.perform(Util.buildVideoUploadRequest(Files.readAllBytes(file.toPath()), metadata))
                    .andExpect(status().isCreated());
        }

        MvcResult mockResult = mockMvc.perform(get("/api/videos")).andReturn();

        assertThat(mockResult.getResponse().getStatus())
                .isEqualTo(HttpStatus.OK.value());

        ObjectMapper mapper = new ObjectMapper();
        String body = mockResult.getResponse().getContentAsString();
        Video[] videos = mapper.readValue(body, Video[].class);

        assertThat(videos.length).isEqualTo(amount);
    }

    @Test
    public void shouldReturnAnEmptyListIfThereIsNoVideo() throws Exception {
        MvcResult mockResult = mockMvc.perform(get("/api/videos")).andReturn();
        String body = mockResult.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        Video[] videos = mapper.readValue(body, Video[].class);

        assertThat(videos).isEmpty();
    }

    @Test
    public void shouldSaveVideoWithTheRightNameAndDescription() throws Exception{
        File file = new File(Objects
                .requireNonNull(getClass().getClassLoader().getResource("videos/test_video.webm")).getFile());

        String titleTempTest = "Title temp test";
        RequestCreateVideoDTO metadata =
                new RequestCreateVideoDTO(titleTempTest, "Big string with description test");

        mockMvc.perform(Util.buildVideoUploadRequest(Files.readAllBytes(file.toPath()), metadata))
                .andExpect(status().isCreated());

        Optional<Video> newVideo = videoRepository.findById(1L);
        String titleCreated = newVideo.get().getTitle();
        
        assertThat(titleCreated).isEqualTo(titleTempTest);

    }
}
