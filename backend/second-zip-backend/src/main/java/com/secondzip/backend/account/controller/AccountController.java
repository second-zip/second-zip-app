package com.secondzip.backend.account.controller;

import com.secondzip.backend.account.dto.request.LoginDTO;
import com.secondzip.backend.account.dto.request.SignupDTO;
import com.secondzip.backend.account.dto.response.LoginResponseDTO;
import com.secondzip.backend.account.dto.response.MessageResponseDTO;
import com.secondzip.backend.account.service.AccountService;
import com.secondzip.backend.security.jwt.JwtTokenResolver;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Api(tags = "회원 인증 API", description = "회원가입, 로그인, 로그아웃 기능을 제공합니다.")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final JwtTokenResolver jwtTokenResolver;

    @ApiOperation(value = "회원가입", notes = "이메일, 비밀번호, 닉네임, 캐릭터 유형을 입력하여 회원가입합니다.")
    @PostMapping("/signup")
    public ResponseEntity<MessageResponseDTO> signup(@RequestBody SignupDTO signupDTO) {
        accountService.signup(signupDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponseDTO("회원가입을 완료했습니다."));
    }

    @ApiOperation(value = "로그인", notes = "이메일과 비밀번호를 검증하고 Access Token과 Refresh Token을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok(accountService.login(loginDTO));
    }

    @ApiOperation(value = "로그아웃", notes = "현재 Access Token을 블랙리스트에 등록하고 Redis에 저장된 Refresh Token을 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<MessageResponseDTO> logout(HttpServletRequest request) {
        String accessToken = jwtTokenResolver.resolveAccessToken(request);

        accountService.logout(accessToken);

        return ResponseEntity.ok(new MessageResponseDTO("로그아웃을 완료했습니다."));
    }
}