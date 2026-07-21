package com.secondzip.backend.account.service;

import com.secondzip.backend.account.dto.SignupDTO;

public interface AccountService {
    void signup(SignupDTO signupDTO);
}
