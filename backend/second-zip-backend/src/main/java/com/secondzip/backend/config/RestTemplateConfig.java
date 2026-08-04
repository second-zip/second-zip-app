package com.secondzip.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

// 외부 API 받아오기
@Configuration
public class RestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        // 연결 타임아웃 3초: 상대 서버가 다운되었을 때 내 서버까지 멈추는 것을 방지
        requestFactory.setConnectTimeout(3000);

        // 읽기 타임아웃 10초: 연결은 됐는데 응답을 너무 늦게 줄 때 방어
        requestFactory.setReadTimeout(10000);

        return new RestTemplate(requestFactory);
    }

    @Bean("codefRestTemplate")
    public RestTemplate codefRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(300000);
        return new RestTemplate(requestFactory);
    }

    @Bean("gptRestTemplate")
    public RestTemplate gptRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(30000);

        return new RestTemplate(requestFactory);
    }
}
