package com.secondzip.backend.account.service;

import com.secondzip.backend.account.domain.AccountVO;
import com.secondzip.backend.account.dto.request.*;
import com.secondzip.backend.account.dto.response.AccountResponseDTO;
import com.secondzip.backend.account.dto.response.LoginResponseDTO;
import com.secondzip.backend.account.mapper.AccountMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.security.jwt.JwtTokenBlacklistService;
import com.secondzip.backend.security.jwt.JwtTokenProvider;
import com.secondzip.backend.security.service.RefreshTokenService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenBlacklistService jwtTokenBlacklistService;

    @Override
    @Transactional
    public void signup(SignupDTO signupDTO) {

        if (accountMapper.countByEmail(signupDTO.getEmail()) > 0) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 이메일입니다."
            );
        }

        if (accountMapper.countByNickname(signupDTO.getNickname()) > 0) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 닉네임입니다."
            );
        }

        AccountVO account = AccountVO.builder()
                .email(signupDTO.getEmail())
                .password(
                        passwordEncoder.encode(
                                signupDTO.getPassword()
                        )
                )
                .nickname(signupDTO.getNickname())
                .characterType(signupDTO.getCharacterType())
                .build();

        int result = accountMapper.insert(account);

        if (result != 1) {
            throw new IllegalStateException(
                    "회원가입에 실패했습니다."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginDTO loginDTO) {
        AccountVO account = accountMapper.findByEmail(loginDTO.getEmail());

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

        long remainingMillis = Math.max(claims.getExpiration().getTime()- System.currentTimeMillis(),0L);

        jwtTokenBlacklistService.add(accessToken, remainingMillis);

        refreshTokenService.delete(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponseDTO getMyAccount(Long accountId) {

        AccountVO account = accountMapper.findById(accountId);

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
        AccountVO account = accountMapper.findById(accountId);

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

        AccountVO updatedAccount = accountMapper.findById(accountId);

        return AccountResponseDTO.from(updatedAccount);

    }

    @Override
    public AccountResponseDTO updateCharacter(Long accountId, UpdateCharacterDTO updateDTO) {
        AccountVO account = accountMapper.findById(accountId);

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
        AccountVO account = accountMapper.findById(accountId);

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
        AccountVO account = accountMapper.findById(accountId);

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
}