package com.secondzip.backend.record.controller;

import com.secondzip.backend.record.dto.request.RecordingLiveStartRequestDTO;
import com.secondzip.backend.record.dto.response.*;
import com.secondzip.backend.record.service.RecordingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "녹음 분석 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recordings")
public class RecordingController {

    private final RecordingService recordingService;

    @ApiOperation(value = "녹음 파일 업로드(파일 방식)", notes = "녹음 파일을 업로드하고 분석 세션을 생성합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecordingSessionResponseDTO>
    uploadRecording(@ApiIgnore @AuthenticationPrincipal Long accountId,
                    @RequestParam Long reportChecklistId,
                    @RequestPart("file") MultipartFile file
    ) {
        RecordingSessionResponseDTO response = recordingService.createSession(accountId, reportChecklistId, file);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }

    @ApiOperation(
            value = "녹음 파일 음성 인식 시작(파일 방식)",
            notes = "업로드된 녹음 파일을 CLOVA Speech로 분석합니다."
    )
    @PostMapping("/{recordingSessionId}/transcribe")
    public ResponseEntity<Void> transcribe(
            @ApiIgnore @AuthenticationPrincipal Long accountId,
            @PathVariable Long recordingSessionId) {
        recordingService.startTranscription(
                accountId,
                recordingSessionId
        );

        return ResponseEntity.accepted().build();
    }

    @ApiOperation(
            value = "녹음 파일 변환 진행 상태(파일 방식)",
            notes = "업로드된 녹음 파일을 CLOVA Speech로 변환 진행 상태를 보여줍니다."
    )
    @GetMapping("/{recordingSessionId}")
    public ResponseEntity<RecordingStatusResponseDTO> getRecordingStatus(
            @ApiIgnore @AuthenticationPrincipal Long accountId,
            @PathVariable Long recordingSessionId
    ) {

        return ResponseEntity.ok(
                recordingService.getRecordingStatus(
                        accountId,
                        recordingSessionId
                )
        );
    }

    @ApiOperation(
            value = "실시간 녹음 시작",
            notes = "실시간 녹음을 시작합니다."
    )
    @PostMapping("/live")
    public ResponseEntity<RecordingLiveStartResponseDTO>
    startLiveRecording(
            @ApiIgnore @AuthenticationPrincipal Long accountId,
            @RequestBody RecordingLiveStartRequestDTO request
            ) {

        RecordingLiveStartResponseDTO response =
                recordingService.startLiveRecording(
                        accountId,request.getReportChecklistId()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @ApiOperation(
            value = "실시간 녹음 종료",
            notes = "실시간 STT를 종료하고 최종 GPT 체크리스트 분석을 시작합니다."
    )
    @PostMapping("/{recordingSessionId}/stop")
    public ResponseEntity<Void>
    stopLiveRecording(
            @ApiIgnore @AuthenticationPrincipal Long accountId,
            @PathVariable Long recordingSessionId
    ) {

        recordingService.stopLiveRecording(accountId, recordingSessionId);

        return ResponseEntity.accepted().build();
    }

    @ApiOperation(
            value = "녹음 조회",
            notes = "녹음 세션 정보를 조회합니다."
    )
    @GetMapping("/{recordingSessionId}/read")
    public ResponseEntity<RecordingDetailResponseDTO>
    getRecording(
            @ApiIgnore @AuthenticationPrincipal Long accountId,
            @PathVariable Long recordingSessionId
    ) {
        return ResponseEntity.ok(
                recordingService.getRecording(
                        accountId,
                        recordingSessionId
                )
        );
    }

    @ApiOperation(
            value = "녹음 Transcript 조회",
            notes = "녹음의 전체 음성 인식 결과를 조회합니다."
    )
    @GetMapping("/{recordingSessionId}/transcript")
    public ResponseEntity<RecordingTranscriptResponseDTO>
    getTranscript(
            @ApiIgnore @AuthenticationPrincipal Long accountId,
            @PathVariable Long recordingSessionId
    ) {
        return ResponseEntity.ok(
                recordingService.getTranscript(
                        accountId,
                        recordingSessionId
                )
        );
    }

    @ApiOperation(
            value = "녹음 삭제",
            notes = "녹음 세션과 연결된 분석 결과 및 녹음 파일을 삭제합니다."
    )
    @DeleteMapping("/{recordingSessionId}")
    public ResponseEntity<Void>
    deleteRecording(
            @ApiIgnore @AuthenticationPrincipal Long accountId,
            @PathVariable Long recordingSessionId
    ) {
        recordingService.deleteRecording(
                accountId,
                recordingSessionId
        );
        return ResponseEntity.noContent().build();
    }
}