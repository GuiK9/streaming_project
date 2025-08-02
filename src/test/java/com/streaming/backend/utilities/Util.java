package com.streaming.backend.utilities;

import com.streaming.backend.config.VideoStorageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Comparator;

@Service
public class Util {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Connection createConnection(String username, String password) throws SQLException {
        String adminUrl = "jdbc:postgresql://localhost:5432/postgres";

        Connection conn = DriverManager.getConnection(adminUrl, username, password);
        conn.createStatement().execute(
                "SELECT pg_terminate_backend(pid) " +
                        "FROM pg_stat_activity " +
                        "WHERE datname = 'streaming_test' AND pid <> pg_backend_pid();"
        );
        return conn;
    }

    public void cleanUpLogDir(String logPath) throws IOException {
        Path path = Paths.get(logPath);

        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    public void cleanUploadDir() throws IOException {
        Path uploadDir = VideoStorageConfig.UPLOAD_DIR;
        Files.createDirectories(uploadDir);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadDir)) {
            for (Path file : stream) {
                Files.deleteIfExists(file);
            }
        }
    }

    public void resetVideoSequence() {
        jdbcTemplate.execute("ALTER SEQUENCE video_file_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE video_id_seq RESTART WITH 1");

    }
}
