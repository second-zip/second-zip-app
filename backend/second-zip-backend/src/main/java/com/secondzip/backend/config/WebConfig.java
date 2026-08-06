package com.secondzip.backend.config;

import com.secondzip.backend.security.config.SecurityConfig;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;

public class WebConfig
        extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{
                RootConfig.class,
                PropertyPlaceholderConfig.class,
                RedisConfig.class,
                SecurityConfig.class,
                FlywayConfig.class,
                ClovaSpeechConfig.class,
                ObjectStorageConfig.class,
                AsyncConfig.class
        };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{
                ServletConfig.class,
                com.secondzip.backend.config.SwaggerConfig.class,
                RecordingWebSocketConfig.class
        };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{
                "/"
        };
    }

    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter filter =
                new CharacterEncodingFilter("UTF-8", true);

        return new Filter[]{filter};
    }

    @Override
    protected void customizeRegistration(
            ServletRegistration.Dynamic registration
    ) {
        MultipartConfigElement multipartConfig =
                new MultipartConfigElement(
                        "",
                        200L * 1024 * 1024,
                        200L * 1024 * 1024,
                        0
                );

        registration.setMultipartConfig(multipartConfig);
    }
}