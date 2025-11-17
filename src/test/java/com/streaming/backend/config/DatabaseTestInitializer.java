package com.streaming.backend.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public class DatabaseTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        String url = Objects.requireNonNull(context.getEnvironment().getProperty("spring.datasource.url"));
        String username = context.getEnvironment().getProperty("spring.datasource.username");
        String password = context.getEnvironment().getProperty("spring.datasource.password");
        String dbName = "streaming_test";

        String adminUrl = url.replaceFirst("/[^/]+$", "/postgres");

        try (Connection conn = DriverManager.getConnection(adminUrl, username, password)) {
            conn.createStatement().execute("CREATE DATABASE " + dbName);
            System.out.println("✅ Database " + dbName + " created successfully");
        } catch (SQLException e) {
            if (!e.getMessage().contains("already exists")) {
                throw new RuntimeException("❌ Failed to create database: " + e.getMessage(), e);
            } else {
                System.out.println("⚠️ Database " + dbName + " already exists, skipping creation");
            }
        }
    }
}
