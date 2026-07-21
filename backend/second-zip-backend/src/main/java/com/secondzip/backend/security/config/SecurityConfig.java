package com.secondzip.backend.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Spring Security 동작 확인용 임시 계정
    @Override
    protected void configure(AuthenticationManagerBuilder auth)
            throws Exception {

        PasswordEncoder encoder = passwordEncoder();

        auth.inMemoryAuthentication()
                .passwordEncoder(encoder)

                .withUser("member")
                .password(encoder.encode("1234"))
                .roles("MEMBER")

                .and()

                .withUser("admin")
                .password(encoder.encode("1234"))
                .roles("ADMIN");
    }

    //HTTP 요청에 대한 보안 규칙 설정
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // Vue와 다른 포트를 사용하는 경우 필요
                .cors()
                .and()

                // REST API 및 JWT 구조에서는 일반적으로 비활성화(브라우저와 쿠키 기반 인증)
                .csrf().disable()

                //세션 비활성화
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

                // URL별 접근 권한 설정
                .authorizeRequests()
                .antMatchers(
                        "/auth/login",
                        "/auth/signup"
                ).permitAll()
                .anyRequest().authenticated();
    }

    //Vue와의 통신을 위한 cors 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type"
        ));

        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}