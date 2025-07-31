package com.streaming.backend.services;

import com.streaming.backend.utilities.Utilities;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoConversionService {

    private static final Logger logger = LoggerFactory.getLogger(VideoConversionService.class);

    public static ByteArrayInputStream convertToMp4(File inputFile) throws IOException, InterruptedException {
        String fileExtension = Utilities.getFileExtension(inputFile.getName());
        if (fileExtension.equalsIgnoreCase("mp4")) {
            // Ler diretamente o conteúdo do arquivo mp4
            byte[] videoBytes = Files.readAllBytes(inputFile.toPath());
            return new ByteArrayInputStream(videoBytes);
        }

        Path tempOutput = Files.createTempFile("converted_", ".mp4");
        Process process = getConvertProcess(inputFile.toPath(), tempOutput);
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
                "-y",
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
            logger.info("Conversion video log");
            while ((line = reader.readLine()) != null) {
                logger.info("[FFMPEG] {}", line);
            }
            logger.info("End conversion video log");
        } catch (IOException e) {
            logger.error("Error reading conversion output: ", e);
        }
    }
}
