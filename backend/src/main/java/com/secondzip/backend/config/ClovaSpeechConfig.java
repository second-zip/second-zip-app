package com.secondzip.backend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ClovaSpeechConfig {

    @Value("${CLOVA_SPEECH_INVOKE_URL}")
    private String invokeUrl;

    @Value("${CLOVA_SPEECH_SECRET_KEY}")
    private String secretKey;
}