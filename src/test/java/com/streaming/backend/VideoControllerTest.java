package com.streaming.backend;

import com.streaming.backend.models.Video;
import com.streaming.backend.repositories.VideoRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VideoRepository videoRepository;

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
        Path expectedPath = Paths.get("/var/www/videos", video.getId() + ".mp4");
        assertEquals(expectedPath.toString(), video.getPathArchive());

        assertTrue(Files.exists(expectedPath));
    }


}
