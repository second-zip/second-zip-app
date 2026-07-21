package com.secondzip.backend.account.service;

import com.secondzip.backend.account.domain.AccountVO;
import com.secondzip.backend.account.dto.request.LoginDTO;
import com.secondzip.backend.account.dto.request.SignupDTO;
import com.secondzip.backend.account.dto.response.LoginResponseDTO;
import com.secondzip.backend.account.enums.CharacterType;
import com.secondzip.backend.account.mapper.AccountMapper;
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
}