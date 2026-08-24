package com.secondzip.backend.report.integration;

import com.secondzip.backend.account.domain.Account;
import com.secondzip.backend.account.dto.request.LoginDTO;
import com.secondzip.backend.account.dto.response.LoginResponseDTO;
import com.secondzip.backend.account.mapper.AccountMapper;
import com.secondzip.backend.account.service.AccountService;
import com.secondzip.backend.account.service.AccountServiceImpl;
import com.secondzip.backend.checklist.dto.response.ReportChecklistConditionDTO;
import com.secondzip.backend.checklist.enums.Category;
import com.secondzip.backend.checklist.mapper.ReportChecklistMapper;
import com.secondzip.backend.checklist.service.ChecklistService;
import com.secondzip.backend.checklist.service.ChecklistServiceImpl;
import com.secondzip.backend.record.mapper.RecordingSessionMapper;
import com.secondzip.backend.security.jwt.JwtTokenBlacklistService;
import com.secondzip.backend.security.jwt.JwtTokenProvider;
import com.secondzip.backend.security.service.RefreshTokenService;
import com.secondzip.backend.terms.mapper.TermMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountChecklistIntegrationTest {

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtTokenBlacklistService blacklistService;

    @Mock
    private TermMapper termMapper;

    @Mock
    private ReportChecklistMapper reportChecklistMapper;

    @Mock
    private RecordingSessionMapper recordingSessionMapper;

    private AccountService accountService;
    private ChecklistService checklistService;
    private JwtTokenProvider jwtTokenProvider;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder =
                new BCryptPasswordEncoder();

        jwtTokenProvider =
                new JwtTokenProvider(
                        "integration-test-secret-key-12345678901234567890",
                        60_000L,
                        120_000L
                );

        accountService =
                new AccountServiceImpl(
                        accountMapper,
                        passwordEncoder,
                        jwtTokenProvider,
                        refreshTokenService,
                        blacklistService,
                        termMapper
                );

        checklistService =
                new ChecklistServiceImpl(
                        reportChecklistMapper,
                        recordingSessionMapper
                );
    }

    @Test
    @DisplayName("로그인한 회원의 accountId로 리포트 체크리스트를 생성한다")
    void login_thenCreateChecklist_success() {
        // given - 회원
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

        // given - 분석 완료 리포트
        ReportChecklistConditionDTO condition =
                new ReportChecklistConditionDTO();

        ReflectionTestUtils.setField(
                condition,
                "housingCategory",
                Category.APARTMENT
        );

        ReflectionTestUtils.setField(
                condition,
                "trustProperty",
                false
        );

        when(reportChecklistMapper.findReportCondition(
                10L,
                1L
        )).thenReturn(condition);

        when(reportChecklistMapper
                .findChecklistIdByReportId(10L))
                .thenReturn(null);

        when(reportChecklistMapper
                .insertChecklist(any()))
                .thenAnswer(invocation -> {

                    Object checklist =
                            invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            checklist,
                            "reportChecklistId",
                            100L
                    );

                    return 1;
                });

        // when - 로그인
        LoginResponseDTO loginResponse =
                accountService.login(
                        new LoginDTO(
                                "test@test.com",
                                "Password1!"
                        )
                );

        Long accountId =
                Long.valueOf(
                        jwtTokenProvider
                                .parseClaims(
                                        loginResponse
                                                .getAccessToken()
                                )
                                .getSubject()
                );

        // when - 해당 회원이 체크리스트 생성
        Long checklistId =
                checklistService.createChecklist(
                        accountId,
                        10L
                );

        // then
        assertEquals(1L, accountId);
        assertEquals(100L, checklistId);

        verify(reportChecklistMapper)
                .findReportCondition(
                        10L,
                        1L
                );

        verify(reportChecklistMapper)
                .insertChecklistItems(
                        100L,
                        10L,
                        Category.APARTMENT,
                        false
                );
    }
}