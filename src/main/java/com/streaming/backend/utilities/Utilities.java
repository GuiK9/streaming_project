package com.streaming.backend.utilities;

public class Utilities {

    public static String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        return (dotIndex != -1) ? filename.substring(dotIndex + 1) : "";
    }
}
