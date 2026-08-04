package com.secondzip.backend.checklist.controller;

import com.secondzip.backend.checklist.dto.request.ChecklistCheckRequestDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistResponseDTO;
import com.secondzip.backend.checklist.service.ChecklistService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "체크리스트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/checklists")
public class ChecklistController {

    private final ChecklistService checklistService;

    @ApiOperation(value = "체크리스트 조회", notes = "체크리스트를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<ChecklistResponseDTO>> getChecklists(
            @ApiIgnore @AuthenticationPrincipal Long accountId,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(
                checklistService.getChecklists(
                        accountId,
                        category
                )
        );
    }

    @ApiOperation(value = "체크리스트 체크 및 해제", notes = "체크리스트를 체크 혹은 해제합니다.")
    @PostMapping("/{checklistItemId}")
    public ResponseEntity<Void> selectChecklist(
            @ApiIgnore @AuthenticationPrincipal Long accountId,
            @PathVariable Long checklistItemId,
            @Valid @RequestBody ChecklistCheckRequestDTO request
    ) {
        checklistService.updateCheckStatus(
                accountId,
                checklistItemId,
                request
        );

        return ResponseEntity.noContent().build();
    }

    @ApiOperation(value = "체크리스트 초기화", notes = "체크리스트를 전부 해제합니다.")
    @PatchMapping("/reset")
    public ResponseEntity<Void> resetChecklist(
            @ApiIgnore @AuthenticationPrincipal Long accountId
    ) {
        checklistService.resetChecklist(accountId);

        return ResponseEntity.noContent().build();
    }
}