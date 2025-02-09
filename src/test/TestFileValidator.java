package test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import utils.FileValidator;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

public class TestFileValidator {

    private final String TEST_DIR = "/tempDir";

    @AfterEach
    void tearDown() {
        File tempDir = new File("/tempDir");
        if (tempDir.exists()) {
            tempDir.delete();
        }
    }

    private void createTempDir(String dir) {
        File tempDir = new File(dir);
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
    }

    @Test
    void testThrowErrorOnMissingDir() {
        assertThrows(RuntimeException.class, () -> {
            FileValidator.throwErrorOnMissingDir(TEST_DIR);
        });
    }

    @Test
    void successOnExistingDir() {
        createTempDir(TEST_DIR);
        assertDoesNotThrow(() -> {
            FileValidator.throwErrorOnMissingDir(TEST_DIR);
        });
    }

}