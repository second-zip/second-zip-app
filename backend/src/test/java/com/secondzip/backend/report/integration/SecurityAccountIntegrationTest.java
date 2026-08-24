package com.secondzip.backend.report.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.account.controller.AccountController;
import com.secondzip.backend.account.controller.UserController;
import com.secondzip.backend.account.domain.Account;
import com.secondzip.backend.account.mapper.AccountMapper;
import com.secondzip.backend.account.service.AccountService;
import com.secondzip.backend.account.service.AccountServiceImpl;
import com.secondzip.backend.security.config.SecurityConfig;
import com.secondzip.backend.security.jwt.JwtTokenBlacklistService;
import com.secondzip.backend.security.jwt.JwtTokenProvider;
import com.secondzip.backend.security.jwt.JwtTokenResolver;
import com.secondzip.backend.security.service.RefreshTokenService;
import com.secondzip.backend.terms.mapper.TermMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = SecurityAccountIntegrationTest.TestConfig.class
)
@org.springframework.test.context.web.WebAppConfiguration
class SecurityAccountIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reset(accountMapper);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("로그인으로 발급된 Access Token이 JWT Filter를 거쳐 accountId로 전달된다")
    void login_thenAccessMyAccount_success() throws Exception {
        // given
        Account account = Account.builder()
                .accountId(1L)
                .email("test@test.com")
                .password(
                        passwordEncoder.encode("Password1!")
                )
                .nickname("세컨드집")
                .build();

        when(accountMapper.findByEmail("test@test.com"))
                .thenReturn(account);

        when(accountMapper.findById(1L))
                .thenReturn(account);

        // when - 로그인
        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType(
                                                "application/json"
                                        )
                                        .content("""
                                                {
                                                  "email": "test@test.com",
                                                  "password": "Password1!"
                                                }
                                                """)
                        )
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode loginBody =
                objectMapper.readTree(
                        loginResult
                                .getResponse()
                                .getContentAsString()
                );

        String accessToken =
                loginBody.get("accessToken").asText();

        // when - 발급된 JWT로 내 정보 조회
        MvcResult userResult =
                mockMvc.perform(
                                get("/api/user")
                                        .header(
                                                "Authorization",
                                                "Bearer " + accessToken
                                        )
                        )
                        .andExpect(status().isOk())
                        .andReturn();

        // then
        JsonNode userBody =
                objectMapper.readTree(
                        userResult
                                .getResponse()
                                .getContentAsString()
                );

        assertEquals(
                1L,
                userBody.get("accountId").asLong()
        );

        assertEquals(
                "test@test.com",
                userBody.get("email").asText()
        );

        verify(accountMapper)
                .findById(1L);
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfig {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return new JwtTokenProvider(
                    "integration-test-secret-key-12345678901234567890",
                    60_000L,
                    120_000L
            );
        }

        @Bean
        JwtTokenBlacklistService jwtTokenBlacklistService() {
            return mock(
                    JwtTokenBlacklistService.class
            );
        }

        @Bean
        RefreshTokenService refreshTokenService() {
            return mock(
                    RefreshTokenService.class
            );
        }

        @Bean
        AccountMapper accountMapper() {
            return mock(AccountMapper.class);
        }

        @Bean
        TermMapper termMapper() {
            return mock(TermMapper.class);
        }

        @Bean
        JwtTokenResolver jwtTokenResolver() {
            return new JwtTokenResolver();
        }

        @Bean
        AccountService accountService(
                AccountMapper accountMapper,
                PasswordEncoder passwordEncoder,
                JwtTokenProvider jwtTokenProvider,
                RefreshTokenService refreshTokenService,
                JwtTokenBlacklistService blacklistService,
                TermMapper termMapper
        ) {
            return new AccountServiceImpl(
                    accountMapper,
                    passwordEncoder,
                    jwtTokenProvider,
                    refreshTokenService,
                    blacklistService,
                    termMapper
            );
        }

        @Bean
        AccountController accountController(
                AccountService accountService,
                JwtTokenResolver resolver
        ) {
            return new AccountController(
                    accountService,
                    resolver
            );
        }

        @Bean
        UserController userController(
                AccountService accountService,
                JwtTokenResolver resolver
        ) {
            return new UserController(
                    accountService,
                    resolver
            );
        }
    }
}