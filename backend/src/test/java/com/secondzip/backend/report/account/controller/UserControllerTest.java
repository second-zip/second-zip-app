package com.secondzip.backend.report.account.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.account.controller.UserController;
import com.secondzip.backend.account.dto.request.UpdateAccountDTO;
import com.secondzip.backend.account.dto.request.UpdateCharacterDTO;
import com.secondzip.backend.account.dto.request.UpdatePasswordDTO;
import com.secondzip.backend.account.dto.request.WithdrawAccountDTO;
import com.secondzip.backend.account.dto.response.AccountResponseDTO;
import com.secondzip.backend.account.dto.response.ActivitySummaryDTO;
import com.secondzip.backend.account.dto.response.MyPageResponseDTO;
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
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private JwtTokenResolver jwtTokenResolver;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        UserController controller =
                new UserController(
                        accountService,
                        jwtTokenResolver
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        accountIdArgumentResolver()
                )
                .build();

        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("내 회원정보 조회")
    class GetMyAccount {

        @Test
        @DisplayName("인증된 회원의 정보를 조회한다")
        void getMyAccount_success() throws Exception {
            // given
            AccountResponseDTO response =
                    AccountResponseDTO.builder()
                            .accountId(1L)
                            .email("test@test.com")
                            .nickname("세컨드집")
                            .characterType(CharacterType.MAN)
                            .build();

            when(accountService.getMyAccount(1L))
                    .thenReturn(response);

            // when
            MvcResult result = mockMvc.perform(
                            get("/api/user")
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            JsonNode body = readBody(result);

            assertEquals(1L, body.get("accountId").asLong());
            assertEquals(
                    "test@test.com",
                    body.get("email").asText()
            );
            assertEquals(
                    "세컨드집",
                    body.get("nickname").asText()
            );

            verify(accountService)
                    .getMyAccount(1L);
        }
    }

    @Nested
    @DisplayName("회원정보 수정")
    class UpdateAccount {

        @Test
        @DisplayName("닉네임을 수정한다")
        void updateMyAccount_success() throws Exception {
            // given
            AccountResponseDTO response =
                    AccountResponseDTO.builder()
                            .accountId(1L)
                            .email("test@test.com")
                            .nickname("새닉네임")
                            .characterType(CharacterType.MAN)
                            .build();

            when(accountService.updateMyAccount(
                    eq(1L),
                    any(UpdateAccountDTO.class)
            )).thenReturn(response);

            // when
            MvcResult result = mockMvc.perform(
                            patch("/api/user")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                              "nickname": "새닉네임"
                                            }
                                            """)
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            JsonNode body = readBody(result);

            assertEquals(
                    "새닉네임",
                    body.get("nickname").asText()
            );

            verify(accountService)
                    .updateMyAccount(
                            eq(1L),
                            argThat(dto ->
                                    dto.getNickname()
                                            .equals("새닉네임")
                            )
                    );
        }
    }

    @Nested
    @DisplayName("캐릭터 변경")
    class UpdateCharacter {

        @Test
        @DisplayName("회원의 캐릭터 유형을 변경한다")
        void updateCharacter_success() throws Exception {
            // given
            AccountResponseDTO response =
                    AccountResponseDTO.builder()
                            .accountId(1L)
                            .email("test@test.com")
                            .nickname("세컨드집")
                            .characterType(CharacterType.CAT)
                            .build();

            when(accountService.updateCharacter(
                    eq(1L),
                    any(UpdateCharacterDTO.class)
            )).thenReturn(response);

            // when
            MvcResult result = mockMvc.perform(
                            patch("/api/user/character")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                              "characterType": "CAT"
                                            }
                                            """)
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            JsonNode body = readBody(result);

            assertEquals(
                    "CAT",
                    body.get("characterType").asText()
            );

            verify(accountService)
                    .updateCharacter(
                            eq(1L),
                            argThat(dto ->
                                    dto.getCharacterType()
                                            == CharacterType.CAT
                            )
                    );
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class UpdatePassword {

        @Test
        @DisplayName("Access Token과 비밀번호 정보를 전달하여 변경한다")
        void updatePassword_success() throws Exception {
            // given
            when(jwtTokenResolver.resolveAccessToken(
                    any(HttpServletRequest.class)
            )).thenReturn("access-token");

            String requestBody = """
                    {
                      "currentPassword": "Password1!",
                      "newPassword": "NewPassword1!",
                      "newPasswordConfirm": "NewPassword1!"
                    }
                    """;

            // when
            MvcResult result = mockMvc.perform(
                            patch("/api/user/password")
                                    .header(
                                            "Authorization",
                                            "Bearer access-token"
                                    )
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content(requestBody)
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            JsonNode body = readBody(result);

            assertEquals(
                    "비밀번호 변경을 완료했습니다. 다시 로그인해 주세요.",
                    body.get("message").asText()
            );

            verify(accountService)
                    .updatePassword(
                            eq(1L),
                            eq("access-token"),
                            argThat(dto ->
                                    dto.getCurrentPassword()
                                            .equals("Password1!")
                                            &&
                                    dto.getNewPassword()
                                            .equals("NewPassword1!")
                            )
                    );
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class Withdraw {

        @Test
        @DisplayName("비밀번호와 Access Token을 전달하여 회원 탈퇴한다")
        void withdraw_success() throws Exception {
            // given
            when(jwtTokenResolver.resolveAccessToken(
                    any(HttpServletRequest.class)
            )).thenReturn("access-token");

            // when
            MvcResult result = mockMvc.perform(
                            delete("/api/user")
                                    .header(
                                            "Authorization",
                                            "Bearer access-token"
                                    )
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content("""
                                            {
                                              "password": "Password1!"
                                            }
                                            """)
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            JsonNode body = readBody(result);

            assertEquals(
                    "회원 탈퇴를 완료했습니다.",
                    body.get("message").asText()
            );

            verify(accountService)
                    .withdraw(
                            eq(1L),
                            eq("access-token"),
                            argThat(dto ->
                                    dto.getPassword()
                                            .equals("Password1!")
                            )
                    );
        }
    }

    @Nested
    @DisplayName("마이페이지 조회")
    class MyPage {

        @Test
        @DisplayName("회원 정보와 리포트 활동 요약을 조회한다")
        void getMyPage_success() throws Exception {
            // given
            ActivitySummaryDTO summary =
                    new ActivitySummaryDTO();

            summary.setTotalReportCount(10);
            summary.setSafeCount(5);
            summary.setCautionCount(3);
            summary.setDangerCount(2);

            MyPageResponseDTO response =
                    MyPageResponseDTO.builder()
                            .accountId(1L)
                            .email("test@test.com")
                            .nickname("세컨드집")
                            .characterType(CharacterType.MAN)
                            .activitySummary(summary)
                            .build();

            when(accountService.getMyPage(1L))
                    .thenReturn(response);

            // when
            MvcResult result = mockMvc.perform(
                            get("/api/user/mypage")
                    )
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            JsonNode body = readBody(result);

            assertEquals(
                    10,
                    body.get("activitySummary")
                            .get("totalReportCount")
                            .asInt()
            );

            assertEquals(
                    5,
                    body.get("activitySummary")
                            .get("safeCount")
                            .asInt()
            );

            verify(accountService)
                    .getMyPage(1L);
        }
    }

    private HandlerMethodArgumentResolver
    accountIdArgumentResolver() {

        return new HandlerMethodArgumentResolver() {

            @Override
            public boolean supportsParameter(
                    MethodParameter parameter
            ) {
                return parameter.hasParameterAnnotation(
                        AuthenticationPrincipal.class
                );
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {
                return 1L;
            }
        };
    }

    private JsonNode readBody(MvcResult result)
            throws Exception {

        return objectMapper.readTree(
                result.getResponse()
                        .getContentAsString(
                                StandardCharsets.UTF_8
                        )
        );
    }
}