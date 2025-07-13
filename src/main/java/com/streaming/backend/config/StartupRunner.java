package com.streaming.backend.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;

@Component
public class StartupRunner {

    @EventListener(ApplicationReadyEvent.class)
    public static void createFilesDirectories() {
        try {
            Files.createDirectories(VideoStorageConfig.UPLOAD_DIR);
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO log
        }

    }

}
