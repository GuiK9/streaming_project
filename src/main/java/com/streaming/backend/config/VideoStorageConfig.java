package com.streaming.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class VideoStorageConfig {

    public static Path UPLOAD_DIR;

    @Value("${videos.upload-dir}")
    private void setUploadDir(String path) {
        UPLOAD_DIR = Paths.get(path).toAbsolutePath().normalize();
    }
}
