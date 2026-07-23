package com.secondzip.backend.account.service;

import com.secondzip.backend.account.dto.request.*;
import com.secondzip.backend.account.dto.response.AccountResponseDTO;
import com.secondzip.backend.account.dto.response.LoginResponseDTO;

public interface AccountService {
    void signup(SignupDTO signupDTO);

    LoginResponseDTO login(LoginDTO loginDTO);

    void logout(String accessToken);

    AccountResponseDTO getMyAccount(Long accountId);

    AccountResponseDTO updateMyAccount(Long accountId, UpdateAccountDTO updateDTO);

    AccountResponseDTO updateCharacter(Long accountId, UpdateCharacterDTO updateDTO);

    void withdraw(Long accountId, String accessToken, WithdrawAccountDTO withdrawDTO);

    void updatePassword(Long accountId, String accessToken, UpdatePasswordDTO updatePasswordDTO);
}
