package com.secondzip.backend.report.record.storage;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.secondzip.backend.record.storage.NcloudRecordingStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NcloudRecordingStorageTest {

    @Mock
    private AmazonS3 amazonS3;

    private NcloudRecordingStorage storage;

    @BeforeEach
    void setUp() {
        storage = new NcloudRecordingStorage(amazonS3);

        ReflectionTestUtils.setField(
                storage,
                "bucketName",
                "test-bucket"
        );
    }

    @Nested
    @DisplayName("파일 업로드")
    class Upload {

        @Test
        @DisplayName("녹음 파일을 Object Storage에 업로드하고 objectKey를 반환한다")
        void upload_success() {
            // given
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "recording.mp3",
                            "audio/mpeg",
                            "audio-data".getBytes()
                    );

            // when
            String objectKey =
                    storage.upload(1L, file);

            // then
            assertTrue(
                    objectKey.startsWith("input/1/")
            );

            assertTrue(
                    objectKey.endsWith("-recording.mp3")
            );

            ArgumentCaptor<ObjectMetadata> metadataCaptor =
                    ArgumentCaptor.forClass(ObjectMetadata.class);

            verify(amazonS3).putObject(
                    eq("test-bucket"),
                    eq(objectKey),
                    any(InputStream.class),
                    metadataCaptor.capture()
            );

            ObjectMetadata metadata =
                    metadataCaptor.getValue();

            assertEquals(
                    file.getSize(),
                    metadata.getContentLength()
            );

            assertEquals(
                    "audio/mpeg",
                    metadata.getContentType()
            );
        }

        @Test
        @DisplayName("파일명에 경로와 특수문자가 포함되면 안전한 파일명으로 변경한다")
        void upload_unsafeFilename_sanitizesFilename() {
            // given
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "C:\\temp\\my voice.mp3",
                            "audio/mpeg",
                            "audio".getBytes()
                    );

            // when
            String objectKey =
                    storage.upload(1L, file);

            // then
            assertTrue(
                    objectKey.endsWith("-my_voice.mp3")
            );
        }
    }

    @Nested
    @DisplayName("파일 삭제")
    class Delete {

        @Test
        @DisplayName("objectKey가 존재하면 Object Storage 파일을 삭제한다")
        void delete_success() {
            // when
            storage.delete("input/1/test.mp3");

            // then
            verify(amazonS3).deleteObject(
                    "test-bucket",
                    "input/1/test.mp3"
            );
        }

        @Test
        @DisplayName("objectKey가 null이면 삭제 요청을 보내지 않는다")
        void delete_nullKey_doesNothing() {
            // when
            storage.delete(null);

            // then
            verifyNoInteractions(amazonS3);
        }

        @Test
        @DisplayName("objectKey가 빈 문자열이면 삭제 요청을 보내지 않는다")
        void delete_blankKey_doesNothing() {
            // when
            storage.delete("   ");

            // then
            verifyNoInteractions(amazonS3);
        }
    }

    @Test
    @DisplayName("녹음 파일 조회용 Presigned URL을 생성한다")
    void generatePresignedUrl_success() throws Exception {
        // given
        URL expectedUrl =
                new URL(
                        "https://storage.example.com/test.mp3"
                );

        when(amazonS3.generatePresignedUrl(
                any(GeneratePresignedUrlRequest.class)
        )).thenReturn(expectedUrl);

        // when
        String result =
                storage.generatePresignedUrl(
                        "input/1/test.mp3"
                );

        // then
        assertEquals(
                expectedUrl.toString(),
                result
        );

        ArgumentCaptor<GeneratePresignedUrlRequest> captor =
                ArgumentCaptor.forClass(
                        GeneratePresignedUrlRequest.class
                );

        verify(amazonS3)
                .generatePresignedUrl(
                        captor.capture()
                );

        assertEquals(
                "test-bucket",
                captor.getValue().getBucketName()
        );

        assertEquals(
                "input/1/test.mp3",
                captor.getValue().getKey()
        );

        assertEquals(
                HttpMethod.GET,
                captor.getValue().getMethod()
        );

        assertNotNull(
                captor.getValue().getExpiration()
        );
    }
}