package com.streaming.backend.utilities;

public class Utilities {

    public static String getFileExtension(String fullFilename) {
        int dotIndex = fullFilename.lastIndexOf(".");
        return (dotIndex != -1) ? fullFilename.substring(dotIndex + 1) : "";
    }
}
