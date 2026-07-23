package com.secondzip.backend.account.controller;

import com.secondzip.backend.account.dto.request.UpdateAccountDTO;
import com.secondzip.backend.account.dto.response.AccountResponseDTO;
import com.secondzip.backend.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<AccountResponseDTO> getMyAccount(@AuthenticationPrincipal Long accountId) {
        return ResponseEntity.ok(accountService.getMyAccount(accountId));
    }

    @PatchMapping
    public ResponseEntity<AccountResponseDTO> updateMyAccount(@AuthenticationPrincipal Long accountId, @RequestBody UpdateAccountDTO updateDTO) {
        return ResponseEntity.ok(accountService.updateMyAccount(accountId, updateDTO));
    }
}
