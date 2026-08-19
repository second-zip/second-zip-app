package com.secondzip.backend.checklist.controller;

import com.secondzip.backend.checklist.dto.request.ChecklistCheckRequestDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistCreateResponseDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistDetailResponseDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistListResponseDTO;
import com.secondzip.backend.checklist.dto.response.ChecklistResponseDTO;
import com.secondzip.backend.checklist.service.ChecklistService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

@Api(
        tags = "체크리스트 API",
        description = "녹음 분석 결과를 기반으로 계약 체크리스트를 생성하고 관리합니다."
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/checklists")
public class ChecklistController {

    private final ChecklistService checklistService;


    @ApiOperation(
            value = "체크리스트 목록",
            notes = "리포트 순서로 체크리스트 생성 여부를 조회합니다."
    )
    @GetMapping
    public ResponseEntity<List<ChecklistListResponseDTO>>
    getChecklistList(
            @ApiIgnore
            @AuthenticationPrincipal Long accountId
    ) {

        return ResponseEntity.ok(
                checklistService.getChecklistList(
                        accountId
                )
        );
    }


    @ApiOperation(
            value = "리포트 체크리스트 생성",
            notes = "리포트의 카테고리와 검증 결과를 기준으로 체크리스트를 생성합니다."
    )
    @PostMapping("/reports/{analysisReportId}")
    public ResponseEntity<ChecklistCreateResponseDTO>
    createChecklist(
            @ApiIgnore
            @AuthenticationPrincipal Long accountId,

            @PathVariable
            Long analysisReportId
    ) {

        Long checklistId =
                checklistService.createChecklist(
                        accountId,
                        analysisReportId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ChecklistCreateResponseDTO.builder()
                                .reportChecklistId(
                                        checklistId
                                )
                                .build()
                );
    }


    @ApiOperation(
            value = "체크리스트 상세 조회",
            notes = "리포트에 생성된 체크리스트 항목을 조회합니다."
    )
    @GetMapping("/{reportChecklistId}")
    public ResponseEntity<ChecklistDetailResponseDTO>
    getChecklist(
            @ApiIgnore
            @AuthenticationPrincipal Long accountId,

            @PathVariable
            Long reportChecklistId
    ) {

        return ResponseEntity.ok(
                checklistService.getChecklist(
                        accountId,
                        reportChecklistId
                )
        );
    }


    @ApiOperation(
            value = "체크리스트 체크/해제"
    )
    @PatchMapping(
            "/{reportChecklistId}/items/{checklistItemId}"
    )
    public ResponseEntity<Void> updateChecklist(
            @ApiIgnore
            @AuthenticationPrincipal Long accountId,

            @PathVariable
            Long reportChecklistId,

            @PathVariable
            Long checklistItemId,

            @Valid
            @RequestBody
            ChecklistCheckRequestDTO request
    ) {

        checklistService.updateCheckStatus(
                accountId,
                reportChecklistId,
                checklistItemId,
                request
        );

        return ResponseEntity.noContent().build();
    }


    @ApiOperation(
            value = "체크리스트 초기화"
    )
    @PatchMapping("/{reportChecklistId}/reset")
    public ResponseEntity<Void> resetChecklist(
            @ApiIgnore
            @AuthenticationPrincipal Long accountId,

            @PathVariable
            Long reportChecklistId
    ) {

        checklistService.resetChecklist(
                accountId,
                reportChecklistId
        );

        return ResponseEntity.noContent().build();
    }
}