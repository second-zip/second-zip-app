package com.secondzip.backend.terms.controller;

import com.secondzip.backend.terms.dto.request.UpdateTermConsentDTO;
import com.secondzip.backend.terms.dto.response.TermConsentResponseDTO;
import com.secondzip.backend.terms.dto.response.TermResponseDTO;
import com.secondzip.backend.terms.service.TermService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "동의서 API", description = "개인정보 동의 내역과 서비스 이용약관을 제공합니다.")
@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;

    @ApiOperation(
            value = "개인정보 동의 내역 조회",
            notes = "로그인한 회원의 약관 동의 상태를 조회합니다."
    )
    @GetMapping("/consents")
    public ResponseEntity<List<TermConsentResponseDTO>> getMyConsents(@ApiIgnore @AuthenticationPrincipal Long accountId) {
        return ResponseEntity.ok(termService.getMyConsents(accountId));
    }

    @ApiOperation(value = "개인정보 선택 동의 변경", notes = "로그인한 회원의 약관 동의 여부를 변경합니다.")
    @PatchMapping("/consents/{termId}")
    public ResponseEntity<TermConsentResponseDTO> updateConsent(
            @ApiIgnore
            @AuthenticationPrincipal Long accountId,
            @PathVariable Long termId,
            @Valid @RequestBody UpdateTermConsentDTO request
    ) {
        return ResponseEntity.ok(termService.updateConsent(accountId, termId, request));
    }

    @ApiOperation(value = "최신 서비스 이용약관 조회", notes = "현재 적용 중인 최신 서비스 이용약관 및 개인정보 처리방침을 조회합니다.")
    @GetMapping("/latest")
    public ResponseEntity<List<TermResponseDTO>> getLatestTerms() {
        return ResponseEntity.ok(termService.getLatestTerms());
    }
}
