package com.secondzip.backend.account.controller;

import com.secondzip.backend.account.dto.request.LoginDTO;
import com.secondzip.backend.account.dto.request.SignupDTO;
import com.secondzip.backend.account.dto.response.LoginResponseDTO;
import com.secondzip.backend.account.dto.response.MessageResponseDTO;
import com.secondzip.backend.account.service.AccountService;
import com.secondzip.backend.security.jwt.JwtTokenResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final JwtTokenResolver jwtTokenResolver;

    @PostMapping("/signup")
    public ResponseEntity<MessageResponseDTO> signup(@RequestBody SignupDTO signupDTO) {
        accountService.signup(signupDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponseDTO("회원가입을 완료했습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok(accountService.login(loginDTO));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponseDTO> logout(HttpServletRequest request) {
        String accessToken = jwtTokenResolver.resolveAccessToken(request);

        accountService.logout(accessToken);

        return ResponseEntity.ok(new MessageResponseDTO("로그아웃을 완료했습니다."));
    }
}