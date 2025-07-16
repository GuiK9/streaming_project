package com.streaming.backend.services;

import com.streaming.backend.utilities.Utilities;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoConversionService {

    private static final Logger logger = LoggerFactory.getLogger(VideoConversionService.class);

    public static ByteArrayInputStream convertToMp4(MultipartFile multipartVideo) throws IOException, InterruptedException {
        String fileExtension = Utilities.getFileExtension(Objects.requireNonNull(multipartVideo.getOriginalFilename()));
        if (fileExtension.equals("mp4")) {
            return (ByteArrayInputStream) multipartVideo.getInputStream();
        }

        Path tempInput = Files.createTempFile("upload_", "_" + multipartVideo.getOriginalFilename());
        multipartVideo.transferTo(tempInput.toFile());
        Path tempOutput = Files.createTempFile("converted_", ".mp4");

        Process process = getConvertProcess(tempInput, tempOutput);
        logProcessOutput(process);

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            logger.error("Error in converting video, code: {}", exitCode);
            throw new RuntimeException("Error in converting video: " + exitCode);
        } else {
            logger.info("Conversion completed successfully, code: {}", exitCode );
        }

        byte[] byteConvertedVideo = Files.readAllBytes(tempOutput);
        return new ByteArrayInputStream(byteConvertedVideo);
    }

    private static Process getConvertProcess(Path tempInput, Path tempOutput) throws IOException {
        String[] command = {
                "ffmpeg",
                "-i", tempInput.toString(),
                "-c:v", "libx264",
                "-preset", "medium",
                "-crf", "23",
                "-c:a", "aac",
                "-b:a", "128k",
                "-movflags", "+faststart",
                tempOutput.toString()
        };


        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        return processBuilder.start();
    }

    private static void logProcessOutput(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("[FFMPEG] {}", line);
            }
        } catch (IOException e) {
            logger.error("Error reading conversion output: ", e);
        }
    }
}
