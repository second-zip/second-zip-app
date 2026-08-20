package com.secondzip.backend.record.service;

import com.secondzip.backend.checklist.mapper.ReportChecklistMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.record.domain.RecordingSessionVO;
import com.secondzip.backend.record.dto.response.*;
import com.secondzip.backend.record.enums.RecordingStatus;
import com.secondzip.backend.record.mapper.RecordingSessionMapper;
import com.secondzip.backend.record.storage.RecordingStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecordingServiceImpl implements RecordingService {

    private final RecordingStorage recordingStorage;
    private final RecordingSessionMapper recordingSessionMapper;
    private final RecordingAsyncService recordingAsyncService;
    private final LiveRecordingContextManager contextManager;
    private final ReportChecklistMapper reportChecklistMapper;
    private final LiveTranscriptionService liveTranscriptionService;
    private final ChecklistAnalysisService checklistAnalysisService;

    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("m4a", "mp3", "aac", "amr", "wav");


    @Override
    @Transactional
    public RecordingSessionResponseDTO createSession(
            Long accountId,
            Long reportChecklistId,
            MultipartFile file
    ) {

        if (accountId == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        int exists =
                reportChecklistMapper.existsOwnedChecklist(
                        accountId,
                        reportChecklistId
                );

        if (exists == 0) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "체크리스트를 찾을 수 없습니다."
            );
        }

        validateRecordingFile(file);

        String objectKey =
                recordingStorage.upload(
                        accountId,
                        file
                );

        RecordingSessionVO session =
                RecordingSessionVO.builder()
                        .accountId(accountId)
                        .reportChecklistId(
                                reportChecklistId
                        )
                        .originalFileName(
                                file.getOriginalFilename()
                        )
                        .storageObjectKey(
                                objectKey
                        )
                        .contentType(
                                file.getContentType()
                        )
                        .fileSize(
                                file.getSize()
                        )
                        .status(
                                RecordingStatus.UPLOADED
                        )
                        .build();

        int insertedCount =
                recordingSessionMapper.insert(
                        session
                );

        if (insertedCount != 1) {

            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "녹음 세션 생성에 실패했습니다."
            );
        }

        return RecordingSessionResponseDTO.from(
                session
        );
    }

    @Override
    public void startTranscription(
            Long accountId,
            Long recordingSessionId
    ) {
        if (accountId == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        RecordingSessionVO session =
                recordingSessionMapper.findByIdAndAccountId(
                        recordingSessionId,
                        accountId
                );

        if (session == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "녹음 세션을 찾을 수 없습니다."
            );
        }

        if (session.getStatus() != RecordingStatus.UPLOADED) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "업로드가 완료된 녹음만 음성 인식을 시작할 수 있습니다."
            );
        }

        recordingAsyncService.transcribe(
                recordingSessionId,
                session.getStorageObjectKey()
        );
    }

    private void validateRecordingFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "녹음 파일이 비어 있습니다."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "녹음 파일 크기가 허용 범위를 초과했습니다."
            );
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "파일 형식을 확인할 수 없습니다."
            );
        }

        String extension = originalFilename
                .substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "지원하지 않는 녹음 파일 형식입니다."
            );
        }
    }

    private void validatestopRecordingFile(MultipartFile file) {

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "녹음 파일 크기가 허용 범위를 초과했습니다."
            );
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "파일 형식을 확인할 수 없습니다."
            );
        }

        String extension = originalFilename
                .substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "지원하지 않는 녹음 파일 형식입니다."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RecordingStatusResponseDTO getRecordingStatus(
            Long accountId,
            Long recordingSessionId
    ) {

        RecordingSessionVO session =
                recordingSessionMapper.findByIdAndAccountId(
                        recordingSessionId,
                        accountId
                );

        if (session == null) {
            throw new IllegalStateException(
                    "녹음 세션을 찾을 수 없습니다."
            );
        }

        return RecordingStatusResponseDTO.builder()
                .recordingSessionId(
                        session.getRecordingSessionId()
                )
                .status(
                        session.getStatus()
                )
                .transcript(
                        session.getFullTranscript()
                )
                .summary(
                        session.getSummary()
                )
                .failureReason(
                        session.getFailureReason()
                )
                .build();
    }

    @Override
    @Transactional
    public RecordingLiveStartResponseDTO startLiveRecording(
            Long accountId,
            Long reportChecklistId
    ) {

        if (accountId == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED
            );
        }

        int exists =
                reportChecklistMapper.existsOwnedChecklist(
                        accountId,
                        reportChecklistId
                );


        if (exists == 0) {

            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "체크리스트를 찾을 수 없습니다."
            );
        }

        RecordingSessionVO session =
                RecordingSessionVO.builder()
                        .accountId(accountId)
                        .reportChecklistId(reportChecklistId)
                        .status(RecordingStatus.RECORDING)
                        .build();

        recordingSessionMapper.insertLiveSession(session);

        contextManager.create(
                session.getRecordingSessionId()
        );

        return RecordingLiveStartResponseDTO.builder()
                .recordingSessionId(
                        session.getRecordingSessionId()
                )
                .status(session.getStatus())
                .build();
    }

    @Override
    @Transactional
    public void stopLiveRecording(
            Long accountId,
            Long recordingSessionId,
            MultipartFile file
    ) {

        RecordingSessionVO session =
                recordingSessionMapper
                        .findByIdAndAccountId(
                                recordingSessionId,
                                accountId
                        );


        if (session == null) {

            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "녹음 세션을 찾을 수 없습니다."
            );
        }


        if (session.getStatus() != RecordingStatus.RECORDING) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "현재 녹음 중인 세션이 아닙니다."
            );
        }

        String transcript = liveTranscriptionService.finish(recordingSessionId);

        // 2. 파일이 없으면 강제 종료 처리
        if (file == null || file.isEmpty()) {

            // 지금까지 인식된 내용이 있다면 저장 + 최종 분석
            if (transcript != null && !transcript.isBlank()) {

                recordingSessionMapper.updateTranscript(
                        recordingSessionId,
                        transcript,
                        RecordingStatus.ANALYZING
                );

                checklistAnalysisService.analyze(
                        recordingSessionId
                );

            } else {

                // 녹취도 없다면 분석할 것이 없으므로 정상 종료 처리
                recordingSessionMapper.updateStatus(
                        recordingSessionId,
                        RecordingStatus.COMPLETED
                );
            }

            return;
        }

        /*
         * 3. 정상 종료
         * 파일 존재
         */
        validatestopRecordingFile(file);

        if (transcript == null || transcript.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "분석할 녹취 내용이 없습니다."
            );
        }

        // 4. 최종 녹음 파일 Object Storage 저장
        String objectKey =
                recordingStorage.upload(
                        accountId,
                        file
                );

        // 5. 파일 정보 DB 저장
        recordingSessionMapper.updateFileInfo(
                recordingSessionId,
                file.getOriginalFilename(),
                objectKey,
                file.getContentType(),
                file.getSize()
        );

        // 6. 전체 녹음 파일 CLOVA 재-STT
        // → 최종 transcript 저장
        // → 최종 체크리스트 분석
        recordingAsyncService.transcribe(
                recordingSessionId,
                objectKey
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RecordingTranscriptResponseDTO getTranscript(
            Long accountId,
            Long recordingSessionId
    ) {

        RecordingSessionVO session =
                recordingSessionMapper.findByIdAndAccountId(
                        recordingSessionId,
                        accountId
                );

        if (session == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "녹음 세션을 찾을 수 없습니다."
            );
        }

        return RecordingTranscriptResponseDTO.builder()
                .recordingSessionId(
                        session.getRecordingSessionId()
                )
                .transcript(
                        session.getFullTranscript()
                )
                .build();
    }

    @Override
    @Transactional
    public void deleteRecording(
            Long accountId,
            Long recordingSessionId
    ) {

        RecordingSessionVO session =
                recordingSessionMapper.findByIdAndAccountId(
                        recordingSessionId,
                        accountId
                );

        if (session == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "녹음 세션을 찾을 수 없습니다."
            );
        }

        /*
         * 실시간 진행 중인 세션을 바로 삭제하는 것은
         * gRPC/WebSocket 정리 문제가 있으므로 막는 것을 권장
         */
        if (session.getStatus() == RecordingStatus.RECORDING) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "녹음 중인 세션은 삭제할 수 없습니다."
            );
        }

        // 업로드 방식에서만 파일 존재
        if (session.getStorageObjectKey() != null
                && !session.getStorageObjectKey().isBlank()) {

            recordingStorage.delete(
                    session.getStorageObjectKey()
            );
        }

        recordingSessionMapper.deleteAnalysisResults(
                recordingSessionId
        );

        int deleted =
                recordingSessionMapper.deleteByIdAndAccountId(
                        recordingSessionId,
                        accountId
                );

        if (deleted != 1) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "녹음 세션 삭제에 실패했습니다."
            );
        }

        // 혹시 메모리 Context가 남아있다면 정리
        contextManager.remove(recordingSessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public RecordingFileUrlResponseDTO getRecordingFileUrl(
            Long accountId,
            Long recordingSessionId
    ) {

        if (accountId == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        RecordingSessionVO session =
                recordingSessionMapper
                        .findByIdAndAccountId(
                                recordingSessionId,
                                accountId
                        );

        if (session == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "녹음 세션을 찾을 수 없습니다."
            );
        }

        String storageObjectKey =
                session.getStorageObjectKey();

        if (storageObjectKey == null
                || storageObjectKey.isBlank()) {

            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "저장된 녹음 파일이 없습니다."
            );
        }

        String url = recordingStorage.generatePresignedUrl(storageObjectKey);

        return RecordingFileUrlResponseDTO.builder()
                .url(url)
                .originalFileName(
                        session.getOriginalFileName()
                )
                .contentType(
                        session.getContentType()
                )
                .fileSize(
                        session.getFileSize()
                )
                .expiresIn(600L)
                .build();
    }
}