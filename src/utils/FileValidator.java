package utils;

import java.io.File;

public class FileValidator {

    public static void throwErrorOnMissingDir(String path) {
        if (!isValidDirectory(path)) {
            throw new RuntimeException("The path [" + path + "] is not a directory or does not exist.");
        }
    }

    private static boolean isValidDirectory(String path) {
        File file = new File(path);
        return file.isDirectory() && file.exists();
    }
}
