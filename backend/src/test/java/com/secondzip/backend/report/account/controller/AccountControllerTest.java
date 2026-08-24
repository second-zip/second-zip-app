package com.secondzip.backend.report.account.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.account.controller.AccountController;
import com.secondzip.backend.account.dto.request.LoginDTO;
import com.secondzip.backend.account.dto.request.SignupDTO;
import com.secondzip.backend.account.dto.response.LoginResponseDTO;
import com.secondzip.backend.account.enums.CharacterType;
import com.secondzip.backend.account.service.AccountService;
import com.secondzip.backend.security.jwt.JwtTokenResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private JwtTokenResolver jwtTokenResolver;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AccountController controller =
                new AccountController(
                        accountService,
                        jwtTokenResolver
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("정상적인 회원가입 요청이면 201 Created를 반환한다")
        void signup_success() throws Exception {
            // given
            String requestBody = """
                    {
                      "email": "test@test.com",
                      "password": "Password1!",
                      "passwordConfirm": "Password1!",
                      "nickname": "세컨드집",
                      "characterType": "MAN",
                      "termConsents": [
                        {
                          "termId": 1,
                          "agreed": true
                        }
                      ]
                    }
                    """;

            // when
            MvcResult result = mockMvc.perform(
                            post("/api/auth/signup")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isCreated())
                    .andReturn();

            // then
            JsonNode body = readBody(result);

            assertEquals(
                    "회원가입을 완료했습니다.",
                    body.get("message").asText()
            );

            verify(accountService).signup(
                    argThat(dto ->
                            dto.getEmail().equals("test@test.com")
                                    && dto.getNickname().equals("세컨드집")
                                    && dto.getCharacterType()
                                    == CharacterType.MAN
                    )
            );
        }

        @Test
        @DisplayName("캐릭터 유형이 없으면 400 Bad Request를 반환한다")
        void signup_missingCharacterType_returnsBadRequest()
                throws Exception {

            // given
            String requestBody = """
                    {
                      "email": "test@test.com",
                      "password": "Password1!",
                      "passwordConfirm": "Password1!",
                      "nickname": "세컨드집",
                      "termConsents": [
                        {
                          "termId": 1,
                          "agreed": true
                        }
                      ]
                    }
                    """;

            // when & then
            mockMvc.perform(
                            post("/api/auth/signup")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isBadRequest());

            verify(accountService, never())
                    .signup(any(SignupDTO.class));
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("로그인에 성공하면 토큰과 회원 정보를 반환한다")
        void login_success() throws Exception {
            // given
            LoginResponseDTO response =
                    LoginResponseDTO.builder()
                            .accessToken("access-token")
                            .refreshToken("refresh-token")
                            .accountId(1L)
                            .email("test@test.com")
                            .nickname("세컨드집")
                            .characterType(CharacterType.MAN)
                            .build();

            when(accountService.login(any(LoginDTO.class)))
                    .thenReturn(response);

            String requestBody = """
                    {
                      "email": "test@test.com",
                      "password": "Password1!"
                    }
                    """;

            // when
            MvcResult result = mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            JsonNode body = readBody(result);

            assertEquals(
                    "access-token",
                    body.get("accessToken").asText()
            );

            assertEquals(
                    "refresh-token",
                    body.get("refreshToken").asText()
            );

            assertEquals(
                    1L,
                    body.get("accountId").asLong()
            );

            verify(accountService).login(
                    argThat(dto ->
                            dto.getEmail().equals("test@test.com")
                                    && dto.getPassword()
                                    .equals("Password1!")
                    )
            );
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400 Bad Request를 반환한다")
        void login_invalidEmail_returnsBadRequest()
                throws Exception {

            // given
            String requestBody = """
                    {
                      "email": "invalid-email",
                      "password": "Password1!"
                    }
                    """;

            // when & then
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isBadRequest());

            verify(accountService, never())
                    .login(any(LoginDTO.class));
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("Access Token을 추출하여 로그아웃 처리한다")
        void logout_success() throws Exception {
            // given
            when(jwtTokenResolver.resolveAccessToken(
                    any(HttpServletRequest.class)
            )).thenReturn("access-token");

            // when
            MvcResult result = mockMvc.perform(
                            post("/api/auth/logout")
                                    .header(
                                            "Authorization",
                                            "Bearer access-token"
                                    )
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            JsonNode body = readBody(result);

            assertEquals(
                    "로그아웃을 완료했습니다.",
                    body.get("message").asText()
            );

            verify(accountService)
                    .logout("access-token");
        }
    }

    private JsonNode readBody(MvcResult result)
            throws Exception {

        String content =
                result.getResponse()
                        .getContentAsString(
                                StandardCharsets.UTF_8
                        );

        return objectMapper.readTree(content);
    }
}