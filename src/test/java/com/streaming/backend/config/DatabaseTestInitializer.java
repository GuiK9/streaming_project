package com.streaming.backend.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        String adminUrl = "jdbc:postgresql://localhost:5432/postgres";
        String username = context.getEnvironment().getProperty("spring.datasource.username");
        String password = context.getEnvironment().getProperty("spring.datasource.password");
        String dbName = "streaming_test";

        try (Connection conn = DriverManager.getConnection(adminUrl, username, password)) {
            conn.createStatement().execute("CREATE DATABASE " + dbName);
        } catch (SQLException e) {
            if (!e.getMessage().contains("already exists")) {
                throw new RuntimeException(e);
            }
        }
    }
}
