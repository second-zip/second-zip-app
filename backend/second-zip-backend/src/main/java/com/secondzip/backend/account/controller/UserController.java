package com.secondzip.backend.account.controller;

import com.secondzip.backend.account.dto.request.UpdateAccountDTO;
import com.secondzip.backend.account.dto.request.UpdateCharacterDTO;
import com.secondzip.backend.account.dto.request.UpdatePasswordDTO;
import com.secondzip.backend.account.dto.request.WithdrawAccountDTO;
import com.secondzip.backend.account.dto.response.AccountResponseDTO;
import com.secondzip.backend.account.dto.response.MessageResponseDTO;
import com.secondzip.backend.account.service.AccountService;
import com.secondzip.backend.security.jwt.JwtTokenResolver;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Api(tags = "회원정보 API", description = "로그인한 회원의 정보 조회, 수정, 캐릭터 변경 및 탈퇴 기능을 제공합니다.")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final AccountService accountService;
    private final JwtTokenResolver jwtTokenResolver;

    @ApiOperation(value = "내 회원정보 조회", notes = "Access Token으로 인증된 회원의 이메일, 닉네임, 캐릭터 유형을 조회합니다.")
    @GetMapping
    public ResponseEntity<AccountResponseDTO> getMyAccount(@ApiIgnore @AuthenticationPrincipal Long accountId) {
        return ResponseEntity.ok(accountService.getMyAccount(accountId));
    }

    @ApiOperation(value = "내 회원정보 수정", notes = "로그인한 회원의 닉네임을 수정합니다.")
    @PatchMapping
    public ResponseEntity<AccountResponseDTO> updateMyAccount(@ApiIgnore @AuthenticationPrincipal Long accountId, @Valid @RequestBody UpdateAccountDTO updateDTO) {
        return ResponseEntity.ok(accountService.updateMyAccount(accountId, updateDTO));
    }

    @ApiOperation(value = "캐릭터 유형 변경", notes = "로그인한 회원의 캐릭터 유형만 별도로 변경합니다.")
    @PatchMapping("/character")
    public ResponseEntity<AccountResponseDTO> updateCharacter(@ApiIgnore @AuthenticationPrincipal Long accountId, @Valid @RequestBody UpdateCharacterDTO updateDTO) {
        return ResponseEntity.ok(accountService.updateCharacter(accountId, updateDTO));
    }

    @ApiOperation(value = "회원 탈퇴", notes = "현재 비밀번호를 검증한 후 회원정보를 삭제하고 Access Token과 Refresh Token을 무효화합니다.")
    @DeleteMapping
    public ResponseEntity<MessageResponseDTO> withdraw(@ApiIgnore @AuthenticationPrincipal Long accountId, @Valid @RequestBody WithdrawAccountDTO withdrawDTO, HttpServletRequest request) {
        String accessToken = jwtTokenResolver.resolveAccessToken(request);

        accountService.withdraw(accountId, accessToken, withdrawDTO);

        return ResponseEntity.ok(
                new MessageResponseDTO(
                        "회원 탈퇴를 완료했습니다."
                )
        );
    }

    @ApiOperation(value = "비밀번호 변경", notes = "현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다. 변경이 완료되면 기존 인증 토큰이 무효화되므로 다시 로그인해야 합니다.")
    @PatchMapping("/password")
    public ResponseEntity<MessageResponseDTO> updatePassword(@ApiIgnore @AuthenticationPrincipal Long accountId, @Valid @RequestBody UpdatePasswordDTO updatePasswordDTO, HttpServletRequest request) {
        String accessToken = jwtTokenResolver.resolveAccessToken(request);

        accountService.updatePassword(accountId, accessToken, updatePasswordDTO);

        return ResponseEntity.ok(new MessageResponseDTO("비밀번호 변경을 완료했습니다. 다시 로그인해 주세요."));
    }
}
