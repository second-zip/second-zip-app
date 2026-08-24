package com.secondzip.backend.account.service;

import com.secondzip.backend.account.domain.Account;
import com.secondzip.backend.account.dto.request.*;
import com.secondzip.backend.account.dto.response.AccountResponseDTO;
import com.secondzip.backend.account.dto.response.ActivitySummaryDTO;
import com.secondzip.backend.account.dto.response.LoginResponseDTO;
import com.secondzip.backend.account.dto.response.MyPageResponseDTO;
import com.secondzip.backend.account.mapper.AccountMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.security.jwt.JwtTokenBlacklistService;
import com.secondzip.backend.security.jwt.JwtTokenProvider;
import com.secondzip.backend.security.service.RefreshTokenService;
import com.secondzip.backend.terms.domain.Term;
import com.secondzip.backend.terms.dto.request.TermConsentRequestDTO;
import com.secondzip.backend.terms.mapper.TermMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenBlacklistService jwtTokenBlacklistService;
    private final TermMapper termMapper;

    @Override
    @Transactional
    public void signup(SignupDTO signupDTO) {

        validateDuplicateEmail(signupDTO.getEmail());
        validateDuplicateNickname(signupDTO.getNickname());

        List<Term> latestTerms = termMapper.findLatestTerms();

        validateTermConsents(
                latestTerms,
                signupDTO.getTermConsents()
        );

        Account account = Account.builder()
                .email(signupDTO.getEmail())
                .password(passwordEncoder.encode(signupDTO.getPassword()))
                .nickname(signupDTO.getNickname())
                .characterType(signupDTO.getCharacterType())
                .build();

        int insertedCount = accountMapper.insert(account);

        if (insertedCount != 1) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "회원가입에 실패했습니다."
            );
        }

        for (TermConsentRequestDTO consent : signupDTO.getTermConsents()) {
            termMapper.upsertConsent(
                    account.getAccountId(),
                    consent.getTermId(),
                    consent.getAgreed()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginDTO loginDTO) {
        Account account = accountMapper.findByEmail(loginDTO.getEmail());

        if (account == null) {
            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 일치하지 않습니다."
            );
        }

        boolean passwordMatches = passwordEncoder.matches(
                loginDTO.getPassword(),
                account.getPassword()
        );

        if (!passwordMatches) {
            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 일치하지 않습니다."
            );
        }

        String accessToken = jwtTokenProvider.createAccessToken(account);

        String refreshToken = jwtTokenProvider.createRefreshToken(account);

        refreshTokenService.save(account.getAccountId(), refreshToken, jwtTokenProvider.getRefreshExpiration());

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accountId(account.getAccountId())
                .email(account.getEmail())
                .nickname(account.getNickname())
                .characterType(account.getCharacterType())
                .build();
    }

    @Override
    public void logout(String accessToken) {
        Claims claims;
        try {
            claims = jwtTokenProvider.parseClaims(accessToken);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "유효하지 않은 Access Token입니다."
            );
        }

        String tokenType = claims.get("type", String.class);

        if (!"ACCESS".equals(tokenType)) {
            throw new IllegalArgumentException(
                    "Access Token이 아닙니다."
            );
        }

        Long accountId = Long.valueOf(claims.getSubject());

        long remainingMillis = Math.max(claims.getExpiration().getTime() - System.currentTimeMillis(), 0L);

        jwtTokenBlacklistService.add(accessToken, remainingMillis);

        refreshTokenService.delete(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponseDTO getMyAccount(Long accountId) {

        Account account = accountMapper.findById(accountId);

        if (account == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "회원정보를 찾을 수 없습니다."
            );
        }

        return AccountResponseDTO.from(account);
    }

    @Override
    public AccountResponseDTO updateMyAccount(Long accountId, UpdateAccountDTO updateDTO) {
        Account account = accountMapper.findById(accountId);

        if (account == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "회원정보를 찾을 수 없습니다."
            );
        }

        int nicknameCount = accountMapper.countByNicknameExcludingAccount(updateDTO.getNickname(), accountId);

        if (nicknameCount > 0) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "이미 사용 중인 닉네임입니다."
            );
        }

        int updatedCount = accountMapper.updateAccount(accountId, updateDTO);

        if (updatedCount != 1) {
            throw new IllegalStateException(
                    "회원정보 수정에 실패했습니다."
            );
        }

        Account updatedAccount = accountMapper.findById(accountId);

        return AccountResponseDTO.from(updatedAccount);

    }

    @Override
    public AccountResponseDTO updateCharacter(Long accountId, UpdateCharacterDTO updateDTO) {
        Account account = accountMapper.findById(accountId);

        if (account == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "회원정보를 찾을 수 없습니다."
            );
        }

        if (account.getCharacterType() == updateDTO.getCharacterType()) {
            return AccountResponseDTO.from(account);
        }

        int updatedCount = accountMapper.updateCharacterType(accountId, updateDTO.getCharacterType());

        if (updatedCount != 1) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "캐릭터 변경에 실패했습니다."
            );
        }

        return AccountResponseDTO.from(accountMapper.findById(accountId));
    }

    @Override
    public void withdraw(Long accountId, String accessToken, WithdrawAccountDTO withdrawDTO) {
        Account account = accountMapper.findById(accountId);

        if (account == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "회원정보를 찾을 수 없습니다.");
        }

        boolean passwordMatches = passwordEncoder.matches(withdrawDTO.getPassword(), account.getPassword());

        if (!passwordMatches) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "비밀번호가 일치하지 않습니다.");
        }

        int deletedCount = accountMapper.deleteById(accountId);

        if (deletedCount != 1) {
            throw new IllegalStateException("회원 탈퇴에 실패했습니다.");
        }

        refreshTokenService.delete(accountId);

        long remainingMillis = jwtTokenProvider.getRemainingExpirationMillis(accessToken);

        jwtTokenBlacklistService.add(accessToken, remainingMillis);

    }

    @Override
    public void updatePassword(Long accountId, String accessToken, UpdatePasswordDTO updatePasswordDTO) {
        Account account = accountMapper.findById(accountId);

        if (account == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "회원정보를 찾을 수 없습니다."
            );
        }

        boolean currentPasswordMatches = passwordEncoder.matches(updatePasswordDTO.getCurrentPassword(), account.getPassword());

        if (!currentPasswordMatches) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "현재 비밀번호가 일치하지 않습니다."
            );
        }

        if (!updatePasswordDTO.getNewPassword()
                .equals(updatePasswordDTO.getNewPasswordConfirm())) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }

        boolean sameAsCurrentPassword = passwordEncoder.matches(updatePasswordDTO.getNewPassword(), account.getPassword());

        if (sameAsCurrentPassword) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "새 비밀번호는 현재 비밀번호와 달라야 합니다."
            );
        }

        String encodedPassword = passwordEncoder.encode(updatePasswordDTO.getNewPassword());

        int updatedCount = accountMapper.updatePassword(accountId, encodedPassword);

        if (updatedCount != 1) {
            throw new IllegalStateException(
                    "비밀번호 변경에 실패했습니다."
            );
        }

        refreshTokenService.delete(accountId);

        long remainingMillis = jwtTokenProvider.getRemainingExpirationMillis(accessToken);

        jwtTokenBlacklistService.add(accessToken, remainingMillis);
    }

    //마이페이지
    @Override
    @Transactional(readOnly = true)
    public MyPageResponseDTO getMyPage(Long accountId) {

        Account account = accountMapper.findById(accountId);

        if (account == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "회원정보를 찾을 수 없습니다."
            );
        }

        ActivitySummaryDTO activitySummary = accountMapper.findActivitySummaryByAccountId(accountId);

        return MyPageResponseDTO.of(account, activitySummary);
    }

    //==========내부 검증 로직===========
    private void validateDuplicateEmail(String email) {
        if (accountMapper.countByEmail(email) > 0) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "이미 사용 중인 이메일입니다."
            );
        }
    }

    private void validateDuplicateNickname(String nickname) {
        if (accountMapper.countByNickname(nickname) > 0) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "이미 사용 중인 닉네임입니다."
            );
        }
    }

    private void validateTermConsents(
            List<Term> latestTerms,
            List<TermConsentRequestDTO> consents
    ) {
        if (consents == null || consents.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.REQUIRED_TERM_NOT_AGREED
            );
        }

        Map<Long, Boolean> consentMap = new HashMap<>();

        for (TermConsentRequestDTO consent : consents) {
            Long termId = consent.getTermId();

            if (termId == null || consent.getAgreed() == null) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "약관 동의 정보가 올바르지 않습니다."
                );
            }

            if (consentMap.putIfAbsent(
                    termId,
                    consent.getAgreed()
            ) != null) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "동일한 약관이 중복으로 전달되었습니다."
                );
            }
        }

        //list 형식을 stream으로 꺼내서 각각 map으로 바꾼 후 collect로 합침
        Set<Long> latestTermIds = latestTerms.stream()
                .map(Term::getTermId)
                .collect(Collectors.toSet());

        boolean containsInvalidTerm = consentMap.keySet().stream()
                .anyMatch(termId -> !latestTermIds.contains(termId));

        if (containsInvalidTerm) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "현재 적용 중인 약관이 아닙니다."
            );
        }

        boolean hasNotAgreedRequiredTerm = latestTerms.stream()
                .filter(term ->
                        Boolean.TRUE.equals(term.getRequired())
                )
                .anyMatch(term ->
                        !Boolean.TRUE.equals(
                                consentMap.get(term.getTermId())
                        )
                );

        if (hasNotAgreedRequiredTerm) {
            throw new BusinessException(
                    ErrorCode.REQUIRED_TERM_NOT_AGREED
            );
        }
    }
}