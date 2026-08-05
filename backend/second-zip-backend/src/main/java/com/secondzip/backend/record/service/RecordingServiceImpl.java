package com.secondzip.backend.record.service;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.record.domain.RecordingSessionVO;
import com.secondzip.backend.record.dto.response.RecordingSessionResponseDTO;
import com.secondzip.backend.record.dto.response.RecordingStatusResponseDTO;
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

    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("m4a", "mp3", "aac", "amr", "wav");


    @Override
    @Transactional
    public RecordingSessionResponseDTO createSession(
            Long accountId,
            MultipartFile file
    ) {
        if (accountId == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        validateRecordingFile(file);

        String objectKey = recordingStorage.upload(accountId, file);

        RecordingSessionVO session =
                RecordingSessionVO.builder()
                        .accountId(accountId)
                        .originalFileName(
                                file.getOriginalFilename()
                        )
                        .storageObjectKey(objectKey)
                        .contentType(file.getContentType())
                        .fileSize(file.getSize())
                        .status(RecordingStatus.UPLOADED)
                        .build();

        int insertedCount =
                recordingSessionMapper.insert(session);

        if (insertedCount != 1) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "녹음 세션 생성에 실패했습니다."
            );
        }

        return RecordingSessionResponseDTO.from(session);
    }

    @Override
    public void startTranscription(
            Long accountId,
            Long recordingSessionId,
            String category
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
                session.getStorageObjectKey(),
                category
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
}