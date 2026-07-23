package com.secondzip.backend.account.service;

import com.secondzip.backend.account.dto.request.LoginDTO;
import com.secondzip.backend.account.dto.request.SignupDTO;
import com.secondzip.backend.account.dto.request.UpdateAccountDTO;
import com.secondzip.backend.account.dto.response.AccountResponseDTO;
import com.secondzip.backend.account.dto.response.LoginResponseDTO;

public interface AccountService {
    void signup(SignupDTO signupDTO);

    LoginResponseDTO login(LoginDTO loginDTO);

    void logout(String accessToken);

    AccountResponseDTO getMyAccount(Long accountId);

    AccountResponseDTO updateMyAccount(Long accountId, UpdateAccountDTO updateDTO);
}
