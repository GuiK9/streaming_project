package com.streaming.backend.services;

import com.streaming.backend.utilities.Utilities;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class VideoConversionService {

    public static ByteArrayInputStream convertToMp4(MultipartFile multipartVideo) throws IOException, InterruptedException {
        String fileExtension = Utilities.getFileExtension(Objects.requireNonNull(multipartVideo.getOriginalFilename()));
        if (fileExtension.equals("mp4")) {
            return (ByteArrayInputStream) multipartVideo.getInputStream();
        }

        Path tempInput = Files.createTempFile("upload_", "_" + multipartVideo.getOriginalFilename());
        multipartVideo.transferTo(tempInput.toFile());
        Path tempOutput = Files.createTempFile("converted_", ".mp4");

        Process process = getConvertProcess(tempInput, tempOutput);

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error in converting video: " + exitCode); // TODO log
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
}
