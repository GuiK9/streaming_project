package com.streaming.backend.utilities;

public class Utilities {

    public static String getFileExtension(String fullFilename) {
        int dotIndex = fullFilename.lastIndexOf(".");
        return (dotIndex != -1) ? fullFilename.substring(dotIndex + 1) : "";
    }

    public static String extractVarPath(String fullPath) {
        String marker = "/var/";
        int index = fullPath.indexOf(marker);
        if (index != -1) {
            return fullPath.substring(index);
        } else {
            throw new IllegalArgumentException("Path does not contain /var/: " + fullPath);
        }
    }

    public static String extractNamePath(String fullPath) {
        String marker = "/videos/";
        int index = fullPath.indexOf(marker);
        if (index != -1) {
            String substring = fullPath.substring(index + marker.length());
            return substring;
        } else {
            throw new IllegalArgumentException("Path does not contain /var/www/videos/: " + fullPath);
        }
    }
}
