package com.secondzip.backend.account.service;

import com.secondzip.backend.account.domain.AccountVO;
import com.secondzip.backend.account.dto.SignupDTO;
import com.secondzip.backend.account.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

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
}