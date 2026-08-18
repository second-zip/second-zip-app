package com.secondzip.backend.report.service.external;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ExternalApiModeResolverTest {

    private String originalMode;
    private String originalConfigFile;

    @BeforeEach
    void rememberSystemProperties() {
        originalMode = System.getProperty("EXTERNAL_API_MODE");
        originalConfigFile = System.getProperty("secondzip.config.file");
    }

    @AfterEach
    void restoreSystemProperties() {
        restore("EXTERNAL_API_MODE", originalMode);
        restore("secondzip.config.file", originalConfigFile);
    }

    @Test
    void systemPropertyHasHighestPriorityAndIsNormalized() {
        System.setProperty("EXTERNAL_API_MODE", "  ReAl  ");
        System.setProperty("secondzip.config.file", "does-not-matter");

        assertThat(ExternalApiModeResolver.resolve()).isEqualTo("real");
    }

    @Test
    void readsModeFromExplicitExternalPropertiesFile(@TempDir Path tempDir) throws IOException {
        assumeNoModeEnvironmentVariable();
        Path config = tempDir.resolve("secondzip.env");
        Files.writeString(config, "EXTERNAL_API_MODE=ReAl\n");
        System.clearProperty("EXTERNAL_API_MODE");
        System.setProperty("secondzip.config.file", config.toString());

        assertThat(ExternalApiModeResolver.resolve()).isEqualTo("real");
    }

    @Test
    void missingModeInFileFallsBackToMock(@TempDir Path tempDir) throws IOException {
        assumeNoModeEnvironmentVariable();
        Path config = tempDir.resolve("secondzip.env");
        Files.writeString(config, "UNRELATED=value\n");
        System.clearProperty("EXTERNAL_API_MODE");
        System.setProperty("secondzip.config.file", config.toString());

        assertThat(ExternalApiModeResolver.resolve()).isEqualTo("mock");
    }

    @Test
    void missingExternalFileFallsBackToMock(@TempDir Path tempDir) {
        assumeNoModeEnvironmentVariable();
        System.clearProperty("EXTERNAL_API_MODE");
        System.setProperty("secondzip.config.file", tempDir.resolve("missing.env").toString());

        assertThat(ExternalApiModeResolver.resolve()).isEqualTo("mock");
    }

    private void assumeNoModeEnvironmentVariable() {
        String environmentMode = System.getenv("EXTERNAL_API_MODE");
        assumeTrue(environmentMode == null || environmentMode.isBlank());
    }

    private void restore(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
