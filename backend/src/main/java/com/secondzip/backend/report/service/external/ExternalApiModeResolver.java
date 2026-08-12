package com.secondzip.backend.report.service.external;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ExternalApiModeResolver {

    public static String resolve() {
        String systemProperty = System.getProperty("EXTERNAL_API_MODE");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty.trim().toLowerCase();
        }

        String environmentVariable = System.getenv("EXTERNAL_API_MODE");
        if (environmentVariable != null && !environmentVariable.isBlank()) {
            return environmentVariable.trim().toLowerCase();
        }

        String externalConfigPath = System.getProperty("secondzip.config.file");
        if (externalConfigPath == null || externalConfigPath.isBlank()) {
            externalConfigPath = System.getenv("SECONDZIP_CONFIG_FILE");
        }
        if (externalConfigPath == null || externalConfigPath.isBlank()) {
            externalConfigPath = "src/main/resources/.env";
        }

        Path configPath = Path.of(externalConfigPath);
        if (Files.isRegularFile(configPath)) {
            try (InputStream is = Files.newInputStream(configPath)) {
                return readMode(is);
            } catch (Exception ignored) {
                // 클래스패스 설정으로 한 번 더 시도한다.
            }
        }

        try (InputStream is = ExternalApiModeResolver.class
                .getClassLoader()
                .getResourceAsStream(".env")) {

            if (is == null) return "mock";
            return readMode(is);

        } catch (Exception e) {
            return "mock";
        }
    }

    private static String readMode(InputStream is) throws Exception {
        Properties props = new Properties();
        props.load(is);
        return props.getProperty("EXTERNAL_API_MODE", "mock").trim().toLowerCase();
    }
}
