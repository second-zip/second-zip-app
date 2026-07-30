package com.secondzip.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import java.util.ArrayList;
import java.util.List;

//.env 파일 읽어오기
@Configuration
public class PropertyPlaceholderConfig {
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        List<org.springframework.core.io.Resource> locations = new ArrayList<>();

        String externalConfigPath = System.getProperty("secondzip.config.file");
        if (externalConfigPath == null || externalConfigPath.isBlank()) {
            externalConfigPath = System.getenv("SECONDZIP_CONFIG_FILE");
        }
        if (externalConfigPath == null || externalConfigPath.isBlank()) {
            externalConfigPath = "src/main/resources/.env";
        }

        locations.add(new FileSystemResource(externalConfigPath));
        locations.add(new ClassPathResource(".env"));
        configurer.setLocations(locations.toArray(new org.springframework.core.io.Resource[0]));
        configurer.setIgnoreResourceNotFound(true);
        configurer.setIgnoreUnresolvablePlaceholders(false);
        return configurer;
    }
}
