package com.secondzip.backend.report.account.service;

import com.secondzip.backend.account.domain.Account;
import com.secondzip.backend.account.dto.request.*;
import com.secondzip.backend.account.dto.response.AccountResponseDTO;
import com.secondzip.backend.account.dto.response.LoginResponseDTO;
import com.secondzip.backend.account.enums.CharacterType;
import com.secondzip.backend.account.mapper.AccountMapper;
import com.secondzip.backend.account.service.AccountServiceImpl;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.security.jwt.JwtTokenBlacklistService;
import com.secondzip.backend.security.jwt.JwtTokenProvider;
import com.secondzip.backend.security.service.RefreshTokenService;
import com.secondzip.backend.terms.domain.Term;
import com.secondzip.backend.terms.dto.request.TermConsentRequestDTO;
import com.secondzip.backend.terms.mapper.TermMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtTokenBlacklistService jwtTokenBlacklistService;

    @Mock
    private TermMapper termMapper;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("정상적인 정보와 필수 약관 동의를 전달하면 회원가입에 성공한다")
        void signup_success() {
            // given
            TermConsentRequestDTO consent = mock(TermConsentRequestDTO.class);

            when(consent.getTermId()).thenReturn(1L);
            when(consent.getAgreed()).thenReturn(true);

            SignupDTO signupDTO = SignupDTO.builder()
                    .email("test@test.com")
                    .password("Password1!")
                    .passwordConfirm("Password1!")
                    .nickname("세컨드집")
                    .characterType(CharacterType.MAN)
                    .termConsents(List.of(consent))
                    .build();

            Term requiredTerm = Term.builder()
                    .termId(1L)
                    .required(true)
                    .build();

            when(accountMapper.countByEmail("test@test.com"))
                    .thenReturn(0);

            when(accountMapper.countByNickname("세컨드집"))
                    .thenReturn(0);

            when(termMapper.findLatestTerms())
                    .thenReturn(List.of(requiredTerm));

            when(passwordEncoder.encode("Password1!"))
                    .thenReturn("encoded-password");

            // 실제 MyBatis에서는 insert 이후 generated key가 account.accountId에 들어간다고 가정
            when(accountMapper.insert(any(Account.class)))
                    .thenAnswer(invocation -> {
                        Account account = invocation.getArgument(0);
                        account.setAccountId(1L);
                        return 1;
                    });

            // when
            accountService.signup(signupDTO);

            // then
            ArgumentCaptor<Account> accountCaptor =
                    ArgumentCaptor.forClass(Account.class);

            verify(accountMapper).insert(accountCaptor.capture());

            Account savedAccount = accountCaptor.getValue();

            assertEquals("test@test.com", savedAccount.getEmail());
            assertEquals("encoded-password", savedAccount.getPassword());
            assertEquals("세컨드집", savedAccount.getNickname());
            assertEquals(CharacterType.MAN, savedAccount.getCharacterType());

            verify(termMapper).upsertConsent(
                    1L,
                    1L,
                    true
            );
        }

        @Test
        @DisplayName("이미 사용 중인 이메일이면 회원가입에 실패한다")
        void signup_duplicateEmail_throwsException() {
            // given
            SignupDTO signupDTO = SignupDTO.builder()
                    .email("test@test.com")
                    .nickname("세컨드집")
                    .build();

            when(accountMapper.countByEmail("test@test.com"))
                    .thenReturn(1);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> accountService.signup(signupDTO)
            );

            // then
            assertEquals(
                    ErrorCode.DUPLICATE_RESOURCE,
                    exception.getErrorCode()
            );

            assertEquals(
                    "이미 사용 중인 이메일입니다.",
                    exception.getMessage()
            );

            verify(accountMapper, never())
                    .insert(any(Account.class));
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 회원가입에 실패한다")
        void signup_duplicateNickname_throwsException() {
            // given
            SignupDTO signupDTO = SignupDTO.builder()
                    .email("test@test.com")
                    .nickname("세컨드집")
                    .build();

            when(accountMapper.countByEmail("test@test.com"))
                    .thenReturn(0);

            when(accountMapper.countByNickname("세컨드집"))
                    .thenReturn(1);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> accountService.signup(signupDTO)
            );

            // then
            assertEquals(
                    ErrorCode.DUPLICATE_RESOURCE,
                    exception.getErrorCode()
            );

            assertEquals(
                    "이미 사용 중인 닉네임입니다.",
                    exception.getMessage()
            );

            verify(accountMapper, never())
                    .insert(any(Account.class));
        }

        @Test
        @DisplayName("필수 약관에 동의하지 않으면 회원가입에 실패한다")
        void signup_requiredTermNotAgreed_throwsException() {
            // given
            TermConsentRequestDTO consent =
                    mock(TermConsentRequestDTO.class);

            when(consent.getTermId()).thenReturn(1L);
            when(consent.getAgreed()).thenReturn(false);

            SignupDTO signupDTO = SignupDTO.builder()
                    .email("test@test.com")
                    .nickname("세컨드집")
                    .termConsents(List.of(consent))
                    .build();

            Term requiredTerm = Term.builder()
                    .termId(1L)
                    .required(true)
                    .build();

            when(accountMapper.countByEmail("test@test.com"))
                    .thenReturn(0);

            when(accountMapper.countByNickname("세컨드집"))
                    .thenReturn(0);

            when(termMapper.findLatestTerms())
                    .thenReturn(List.of(requiredTerm));

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> accountService.signup(signupDTO)
            );

            // then
            assertEquals(
                    ErrorCode.REQUIRED_TERM_NOT_AGREED,
                    exception.getErrorCode()
            );

            verify(accountMapper, never())
                    .insert(any(Account.class));
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("이메일과 비밀번호가 일치하면 토큰을 발급한다")
        void login_success() {
            // given
            LoginDTO loginDTO =
                    new LoginDTO("test@test.com", "Password1!");

            Account account = Account.builder()
                    .accountId(1L)
                    .email("test@test.com")
                    .password("encoded-password")
                    .nickname("세컨드집")
                    .characterType(CharacterType.MAN)
                    .build();

            when(accountMapper.findByEmail("test@test.com"))
                    .thenReturn(account);

            when(passwordEncoder.matches(
                    "Password1!",
                    "encoded-password"
            )).thenReturn(true);

            when(jwtTokenProvider.createAccessToken(account))
                    .thenReturn("access-token");

            when(jwtTokenProvider.createRefreshToken(account))
                    .thenReturn("refresh-token");

            when(jwtTokenProvider.getRefreshExpiration())
                    .thenReturn(120_000L);

            // when
            LoginResponseDTO response =
                    accountService.login(loginDTO);

            // then
            assertEquals("access-token", response.getAccessToken());
            assertEquals("refresh-token", response.getRefreshToken());
            assertEquals(1L, response.getAccountId());
            assertEquals("test@test.com", response.getEmail());
            assertEquals("세컨드집", response.getNickname());
            assertEquals(CharacterType.MAN, response.getCharacterType());

            verify(refreshTokenService).save(
                    1L,
                    "refresh-token",
                    120_000L
            );
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 로그인에 실패한다")
        void login_accountNotFound_throwsException() {
            // given
            LoginDTO loginDTO =
                    new LoginDTO("none@test.com", "Password1!");

            when(accountMapper.findByEmail("none@test.com"))
                    .thenReturn(null);

            // when
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> accountService.login(loginDTO)
            );

            // then
            assertEquals(
                    "이메일 또는 비밀번호가 일치하지 않습니다.",
                    exception.getMessage()
            );

            verifyNoInteractions(jwtTokenProvider);
            verifyNoInteractions(refreshTokenService);
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
        void login_wrongPassword_throwsException() {
            // given
            LoginDTO loginDTO =
                    new LoginDTO("test@test.com", "WrongPassword!");

            Account account = Account.builder()
                    .accountId(1L)
                    .email("test@test.com")
                    .password("encoded-password")
                    .build();

            when(accountMapper.findByEmail("test@test.com"))
                    .thenReturn(account);

            when(passwordEncoder.matches(
                    "WrongPassword!",
                    "encoded-password"
            )).thenReturn(false);

            // when
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> accountService.login(loginDTO)
            );

            // then
            assertEquals(
                    "이메일 또는 비밀번호가 일치하지 않습니다.",
                    exception.getMessage()
            );

            verifyNoInteractions(jwtTokenProvider);
            verifyNoInteractions(refreshTokenService);
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("정상 Access Token이면 블랙리스트 등록 후 Refresh Token을 삭제한다")
        void logout_success() {
            // given
            String accessToken = "access-token";

            Claims claims = mock(Claims.class);

            when(jwtTokenProvider.parseClaims(accessToken))
                    .thenReturn(claims);

            when(claims.get("type", String.class))
                    .thenReturn("ACCESS");

            when(claims.getSubject())
                    .thenReturn("1");

            when(claims.getExpiration())
                    .thenReturn(
                            new Date(System.currentTimeMillis() + 60_000L)
                    );

            // when
            accountService.logout(accessToken);

            // then
            verify(jwtTokenBlacklistService).add(
                    eq(accessToken),
                    longThat(value ->
                            value > 0 && value <= 60_000L
                    )
            );

            verify(refreshTokenService)
                    .delete(1L);
        }

        @Test
        @DisplayName("유효하지 않은 토큰이면 로그아웃에 실패한다")
        void logout_invalidToken_throwsException() {
            // given
            String accessToken = "invalid-token";

            when(jwtTokenProvider.parseClaims(accessToken))
                    .thenThrow(new IllegalArgumentException());

            // when
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> accountService.logout(accessToken)
            );

            // then
            assertEquals(
                    "유효하지 않은 Access Token입니다.",
                    exception.getMessage()
            );

            verifyNoInteractions(jwtTokenBlacklistService);
            verifyNoInteractions(refreshTokenService);
        }

        @Test
        @DisplayName("Refresh Token으로 로그아웃을 시도하면 실패한다")
        void logout_refreshToken_throwsException() {
            // given
            String refreshToken = "refresh-token";

            Claims claims = mock(Claims.class);

            when(jwtTokenProvider.parseClaims(refreshToken))
                    .thenReturn(claims);

            when(claims.get("type", String.class))
                    .thenReturn("REFRESH");

            // when
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> accountService.logout(refreshToken)
            );

            // then
            assertEquals(
                    "Access Token이 아닙니다.",
                    exception.getMessage()
            );

            verifyNoInteractions(jwtTokenBlacklistService);
            verifyNoInteractions(refreshTokenService);
        }
    }

    @Nested
    @DisplayName("회원 조회")
    class GetMyAccount {

        @Test
        @DisplayName("회원이 존재하면 회원정보를 반환한다")
        void getMyAccount_success() {
            // given
            Account account = createAccount();

            when(accountMapper.findById(1L))
                    .thenReturn(account);

            // when
            AccountResponseDTO response =
                    accountService.getMyAccount(1L);

            // then
            assertEquals(1L, response.getAccountId());
            assertEquals("test@test.com", response.getEmail());
            assertEquals("세컨드집", response.getNickname());
            assertEquals(CharacterType.MAN, response.getCharacterType());
        }

        @Test
        @DisplayName("회원이 존재하지 않으면 조회에 실패한다")
        void getMyAccount_notFound_throwsException() {
            // given
            when(accountMapper.findById(1L))
                    .thenReturn(null);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> accountService.getMyAccount(1L)
            );

            // then
            assertEquals(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    exception.getErrorCode()
            );
        }
    }

    @Nested
    @DisplayName("회원정보 수정")
    class UpdateMyAccount {

        @Test
        @DisplayName("중복되지 않은 닉네임이면 회원정보 수정에 성공한다")
        void updateMyAccount_success() {
            // given
            UpdateAccountDTO updateDTO =
                    mock(UpdateAccountDTO.class);

            when(updateDTO.getNickname())
                    .thenReturn("새닉네임");

            Account account = createAccount();

            Account updatedAccount = Account.builder()
                    .accountId(1L)
                    .email("test@test.com")
                    .password("encoded-password")
                    .nickname("새닉네임")
                    .characterType(CharacterType.MAN)
                    .build();

            when(accountMapper.findById(1L))
                    .thenReturn(account, updatedAccount);

            when(accountMapper.countByNicknameExcludingAccount(
                    "새닉네임",
                    1L
            )).thenReturn(0);

            when(accountMapper.updateAccount(1L, updateDTO))
                    .thenReturn(1);

            // when
            AccountResponseDTO response =
                    accountService.updateMyAccount(
                            1L,
                            updateDTO
                    );

            // then
            assertEquals("새닉네임", response.getNickname());

            verify(accountMapper)
                    .updateAccount(1L, updateDTO);
        }

        @Test
        @DisplayName("다른 회원이 사용 중인 닉네임이면 수정에 실패한다")
        void updateMyAccount_duplicateNickname_throwsException() {
            // given
            UpdateAccountDTO updateDTO =
                    mock(UpdateAccountDTO.class);

            when(updateDTO.getNickname())
                    .thenReturn("중복닉네임");

            when(accountMapper.findById(1L))
                    .thenReturn(createAccount());

            when(accountMapper.countByNicknameExcludingAccount(
                    "중복닉네임",
                    1L
            )).thenReturn(1);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> accountService.updateMyAccount(
                            1L,
                            updateDTO
                    )
            );

            // then
            assertEquals(
                    ErrorCode.DUPLICATE_RESOURCE,
                    exception.getErrorCode()
            );

            verify(accountMapper, never())
                    .updateAccount(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class UpdatePassword {

        @Test
        @DisplayName("현재 비밀번호가 일치하고 새 비밀번호가 유효하면 변경에 성공한다")
        void updatePassword_success() {
            // given
            String accessToken = "access-token";

            UpdatePasswordDTO updateDTO =
                    mock(UpdatePasswordDTO.class);

            when(updateDTO.getCurrentPassword())
                    .thenReturn("OldPassword1!");

            when(updateDTO.getNewPassword())
                    .thenReturn("NewPassword1!");

            when(updateDTO.getNewPasswordConfirm())
                    .thenReturn("NewPassword1!");

            Account account = createAccount();

            when(accountMapper.findById(1L))
                    .thenReturn(account);

            when(passwordEncoder.matches(
                    "OldPassword1!",
                    "encoded-password"
            )).thenReturn(true);

            when(passwordEncoder.matches(
                    "NewPassword1!",
                    "encoded-password"
            )).thenReturn(false);

            when(passwordEncoder.encode("NewPassword1!"))
                    .thenReturn("new-encoded-password");

            when(accountMapper.updatePassword(
                    1L,
                    "new-encoded-password"
            )).thenReturn(1);

            when(jwtTokenProvider
                    .getRemainingExpirationMillis(accessToken))
                    .thenReturn(30_000L);

            // when
            accountService.updatePassword(
                    1L,
                    accessToken,
                    updateDTO
            );

            // then
            verify(accountMapper).updatePassword(
                    1L,
                    "new-encoded-password"
            );

            verify(refreshTokenService)
                    .delete(1L);

            verify(jwtTokenBlacklistService)
                    .add(accessToken, 30_000L);
        }

        @Test
        @DisplayName("현재 비밀번호가 일치하지 않으면 변경에 실패한다")
        void updatePassword_wrongCurrentPassword_throwsException() {
            // given
            UpdatePasswordDTO updateDTO =
                    mock(UpdatePasswordDTO.class);

            when(updateDTO.getCurrentPassword())
                    .thenReturn("WrongPassword1!");

            when(accountMapper.findById(1L))
                    .thenReturn(createAccount());

            when(passwordEncoder.matches(
                    "WrongPassword1!",
                    "encoded-password"
            )).thenReturn(false);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> accountService.updatePassword(
                            1L,
                            "access-token",
                            updateDTO
                    )
            );

            // then
            assertEquals(
                    ErrorCode.INVALID_REQUEST,
                    exception.getErrorCode()
            );

            assertEquals(
                    "현재 비밀번호가 일치하지 않습니다.",
                    exception.getMessage()
            );

            verify(accountMapper, never())
                    .updatePassword(anyLong(), anyString());
        }

        @Test
        @DisplayName("새 비밀번호와 비밀번호 확인이 다르면 변경에 실패한다")
        void updatePassword_confirmationMismatch_throwsException() {
            // given
            UpdatePasswordDTO updateDTO =
                    mock(UpdatePasswordDTO.class);

            when(updateDTO.getCurrentPassword())
                    .thenReturn("OldPassword1!");

            when(updateDTO.getNewPassword())
                    .thenReturn("NewPassword1!");

            when(updateDTO.getNewPasswordConfirm())
                    .thenReturn("DifferentPassword1!");

            when(accountMapper.findById(1L))
                    .thenReturn(createAccount());

            when(passwordEncoder.matches(
                    "OldPassword1!",
                    "encoded-password"
            )).thenReturn(true);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> accountService.updatePassword(
                            1L,
                            "access-token",
                            updateDTO
                    )
            );

            // then
            assertEquals(
                    ErrorCode.INVALID_REQUEST,
                    exception.getErrorCode()
            );

            assertEquals(
                    "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.",
                    exception.getMessage()
            );

            verify(accountMapper, never())
                    .updatePassword(anyLong(), anyString());
        }

        @Test
        @DisplayName("새 비밀번호가 현재 비밀번호와 같으면 변경에 실패한다")
        void updatePassword_sameAsCurrentPassword_throwsException() {
            // given
            UpdatePasswordDTO updateDTO =
                    mock(UpdatePasswordDTO.class);

            when(updateDTO.getCurrentPassword())
                    .thenReturn("OldPassword1!");

            when(updateDTO.getNewPassword())
                    .thenReturn("OldPassword1!");

            when(updateDTO.getNewPasswordConfirm())
                    .thenReturn("OldPassword1!");

            when(accountMapper.findById(1L))
                    .thenReturn(createAccount());

            when(passwordEncoder.matches(
                    "OldPassword1!",
                    "encoded-password"
            )).thenReturn(true);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> accountService.updatePassword(
                            1L,
                            "access-token",
                            updateDTO
                    )
            );

            // then
            assertEquals(
                    ErrorCode.INVALID_REQUEST,
                    exception.getErrorCode()
            );

            assertEquals(
                    "새 비밀번호는 현재 비밀번호와 달라야 합니다.",
                    exception.getMessage()
            );

            verify(accountMapper, never())
                    .updatePassword(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class Withdraw {

        @Test
        @DisplayName("현재 비밀번호가 일치하면 회원을 삭제하고 토큰을 무효화한다")
        void withdraw_success() {
            // given
            String accessToken = "access-token";

            WithdrawAccountDTO withdrawDTO =
                    mock(WithdrawAccountDTO.class);

            when(withdrawDTO.getPassword())
                    .thenReturn("Password1!");

            when(accountMapper.findById(1L))
                    .thenReturn(createAccount());

            when(passwordEncoder.matches(
                    "Password1!",
                    "encoded-password"
            )).thenReturn(true);

            when(accountMapper.deleteById(1L))
                    .thenReturn(1);

            when(jwtTokenProvider
                    .getRemainingExpirationMillis(accessToken))
                    .thenReturn(30_000L);

            // when
            accountService.withdraw(
                    1L,
                    accessToken,
                    withdrawDTO
            );

            // then
            verify(accountMapper)
                    .deleteById(1L);

            verify(refreshTokenService)
                    .delete(1L);

            verify(jwtTokenBlacklistService)
                    .add(accessToken, 30_000L);
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 회원 탈퇴에 실패한다")
        void withdraw_wrongPassword_throwsException() {
            // given
            WithdrawAccountDTO withdrawDTO =
                    mock(WithdrawAccountDTO.class);

            when(withdrawDTO.getPassword())
                    .thenReturn("WrongPassword1!");

            when(accountMapper.findById(1L))
                    .thenReturn(createAccount());

            when(passwordEncoder.matches(
                    "WrongPassword1!",
                    "encoded-password"
            )).thenReturn(false);

            // when
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> accountService.withdraw(
                            1L,
                            "access-token",
                            withdrawDTO
                    )
            );

            // then
            assertEquals(
                    ErrorCode.INVALID_REQUEST,
                    exception.getErrorCode()
            );

            assertEquals(
                    "비밀번호가 일치하지 않습니다.",
                    exception.getMessage()
            );

            verify(accountMapper, never())
                    .deleteById(anyLong());

            verifyNoInteractions(refreshTokenService);
            verifyNoInteractions(jwtTokenBlacklistService);
        }
    }


    // 여러 테스트에서 공통으로 사용할 회원 데이터
    private Account createAccount() {
        return Account.builder()
                .accountId(1L)
                .email("test@test.com")
                .password("encoded-password")
                .nickname("세컨드집")
                .characterType(CharacterType.MAN)
                .build();
    }
}