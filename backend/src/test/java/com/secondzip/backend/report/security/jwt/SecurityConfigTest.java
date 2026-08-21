package com.secondzip.backend.report.security.jwt;

import com.secondzip.backend.security.config.SecurityConfig;
import com.secondzip.backend.security.jwt.JwtTokenBlacklistService;
import com.secondzip.backend.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {
                SecurityConfig.class,
                SecurityConfigTest.TestConfig.class
        }
)
@org.springframework.test.context.web.WebAppConfiguration
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtTokenBlacklistService jwtTokenBlacklistService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(
                jwtTokenProvider,
                jwtTokenBlacklistService
        );

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }


    @Nested
    @DisplayName("Security Bean 설정")
    class BeanTest {

        @Test
        @DisplayName("비밀번호 암호화기로 BCryptPasswordEncoder를 사용한다")
        void passwordEncoder_isBCrypt() {
            assertInstanceOf(
                    BCryptPasswordEncoder.class,
                    passwordEncoder
            );
        }
    }


    @Nested
    @DisplayName("현재 접근 권한 설정")
    class AuthorizationTest {

        @Test
        @DisplayName("현재 설정에서는 Access Token이 없어도 요청을 허용한다")
        void requestWithoutToken_isPermitted()
                throws Exception {

            mockMvc.perform(
                            get("/test/security")
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().string("OK"));
        }


        @Test
        @DisplayName("정상 Access Token이면 Controller에서 인증된 accountId를 사용할 수 있다")
        void validAccessToken_setsAuthentication()
                throws Exception {

            // given
            Claims claims = mock(Claims.class);

            when(jwtTokenBlacklistService.contains("access-token"))
                    .thenReturn(false);

            when(jwtTokenProvider.parseClaims("access-token"))
                    .thenReturn(claims);

            when(claims.get("type", String.class))
                    .thenReturn("ACCESS");

            when(claims.getSubject())
                    .thenReturn("1");

            // when & then
            mockMvc.perform(
                            get("/test/security/account-id")
                                    .header(
                                            "Authorization",
                                            "Bearer access-token"
                                    )
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));
        }


        @Test
        @DisplayName("OPTIONS 요청은 인증 없이 허용한다")
        void optionsRequest_isPermitted()
                throws Exception {

            mockMvc.perform(
                            options("/test/security")
                                    .header(
                                            "Origin",
                                            "http://localhost:5173"
                                    )
                                    .header(
                                            "Access-Control-Request-Method",
                                            "GET"
                                    )
                    )
                    .andExpect(status().isOk());
        }
    }


    @Configuration
    @EnableWebMvc
    static class TestConfig {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return mock(JwtTokenProvider.class);
        }

        @Bean
        JwtTokenBlacklistService jwtTokenBlacklistService() {
            return mock(JwtTokenBlacklistService.class);
        }

        @Bean
        TestController testController() {
            return new TestController();
        }
    }


    @RestController
    static class TestController {

        @GetMapping("/test/security")
        String test() {
            return "OK";
        }

        @GetMapping("/test/security/account-id")
        String accountId(Authentication authentication) {

            if (authentication == null) {
                return "NONE";
            }

            return String.valueOf(
                    authentication.getPrincipal()
            );
        }
    }
}