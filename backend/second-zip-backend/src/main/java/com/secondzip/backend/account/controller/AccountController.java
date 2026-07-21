package com.secondzip.backend.account.controller;

import com.secondzip.backend.account.dto.SignupDTO;
import com.secondzip.backend.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody SignupDTO signupDTO) {
        accountService.signup(signupDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }
}