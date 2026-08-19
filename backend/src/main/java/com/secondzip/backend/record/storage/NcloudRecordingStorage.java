package com.secondzip.backend.record.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NcloudRecordingStorage implements RecordingStorage {

    private final AmazonS3 amazonS3;

    @Value("${NCLOUD_OBJECT_STORAGE_BUCKET}")
    private String bucketName;

    @Override
    public String upload(
            Long accountId,
            MultipartFile file
    ) {
        String originalFilename = sanitizeFilename(file.getOriginalFilename());

        String objectKey =
                "input/"
                        + accountId
                        + "/"
                        + UUID.randomUUID()
                        + "-"
                        + originalFilename;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try {
            amazonS3.putObject(
                    bucketName,
                    objectKey,
                    file.getInputStream(),
                    metadata
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "녹음 파일을 Object Storage에 업로드하지 못했습니다.",
                    e
            );
        }

        return objectKey;
    }

    @Override
    public void delete(
            String objectKey
    ) {

        if (objectKey == null
                || objectKey.isBlank()) {

            return;
        }

        amazonS3.deleteObject(
                bucketName,
                objectKey
        );
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "recording";
        }

        String filename = originalFilename
                .replace("\\", "/");

        filename = filename.substring(
                filename.lastIndexOf("/") + 1
        );

        return filename.replaceAll(
                "[^a-zA-Z0-9가-힣._-]",
                "_"
        );
    }
}