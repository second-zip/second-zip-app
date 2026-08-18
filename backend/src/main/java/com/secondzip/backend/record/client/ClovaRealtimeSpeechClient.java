package com.secondzip.backend.record.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;

import com.nbp.cdncp.nest.grpc.proto.v1.NestConfig;
import com.nbp.cdncp.nest.grpc.proto.v1.NestData;
import com.nbp.cdncp.nest.grpc.proto.v1.NestRequest;
import com.nbp.cdncp.nest.grpc.proto.v1.NestResponse;
import com.nbp.cdncp.nest.grpc.proto.v1.NestServiceGrpc;
import com.nbp.cdncp.nest.grpc.proto.v1.RequestType;

import com.secondzip.backend.record.dto.response.ClovaRealtimeResponse;
import com.secondzip.backend.record.grpc.ClovaGrpcSession;
import com.secondzip.backend.record.grpc.ClovaGrpcSessionManager;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

//CLOVA와 실제 gRPC 연결을 만들고 요청/응답 스트림을 시작하는 클래스
@Component
@RequiredArgsConstructor
@Slf4j
public class ClovaRealtimeSpeechClient {

    private static final String HOST = "clovaspeech-gw.ncloud.com";

    private static final int PORT = 50051;

    private final ObjectMapper objectMapper;

    private final ClovaGrpcSessionManager sessionManager;

    @Value("${CLOVA_SPEECH_SECRET_KEY}")
    private String secretKey;


    public void start(
            Long recordingSessionId,
            BiConsumer<Integer, String> transcriptConsumer
    ) {

        ManagedChannel channel =
                NettyChannelBuilder
                        .forAddress(
                                HOST,
                                PORT
                        )
                        .useTransportSecurity() //TLS 사용
                        .build();


        /*
        실제 CLOVA 서버와 실시간으로 데이터를 주고받는 진짜 클라이언트
        CLOVA의 NestService를 호출할 클라이언트 객체
        비동기 Stub == 실시간 streaming에서 주로 사용
        */
        NestServiceGrpc.NestServiceStub stub = NestServiceGrpc.newStub(channel);


        Metadata metadata = new Metadata(); //gRPC 요청에 같이 보낼 헤더 정보

        Metadata.Key<String> authorizationKey =
                Metadata.Key.of(
                        "Authorization",
                        Metadata.ASCII_STRING_MARSHALLER
                );

        metadata.put(
                authorizationKey,
                "Bearer " + secretKey
        );


        // Authorization Metadata를 앞으로 이 stub으로 보내는 gRPC 요청마다 붙여주는 것
        stub = stub.withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(
                        metadata
                )
        );


        CountDownLatch completedLatch = new CountDownLatch(1); //CLOVA 응답이 완전히 종료됐다는 신호


        // CLOVA → 우리 서버 방향의 응답을 받아주는 콜백 객체
        StreamObserver<NestResponse> responseObserver = new StreamObserver<>() {

                    @Override
                    public void onNext(
                            NestResponse response
                    ) {

                        handleResponse(
                                recordingSessionId,
                                response,
                                transcriptConsumer
                        );
                    }


                    @Override
                    public void onError(
                            Throwable throwable
                    ) {

                        log.error(
                                "CLOVA 실시간 STT 오류. recordingSessionId={}",
                                recordingSessionId,
                                throwable
                        );

                        completedLatch.countDown();
                    }


                    @Override
                    public void onCompleted() {

                        log.info(
                                "CLOVA 실시간 STT 응답 종료. recordingSessionId={}",
                                recordingSessionId
                        );

                        completedLatch.countDown();
                    }
                };


        //우리 → CLOVA
        StreamObserver<NestRequest> requestObserver =
                stub.recognize(
                        responseObserver
                );


        ClovaGrpcSession grpcSession =
                new ClovaGrpcSession(
                        channel,
                        requestObserver,
                        completedLatch
                );


        try {
            sessionManager.put(recordingSessionId, grpcSession);
            sendConfig(grpcSession);
        } catch (Exception e) {
            sessionManager.remove(recordingSessionId);
            grpcSession.shutdown();
            throw e;
        }


        log.info(
                "CLOVA gRPC 세션 시작. recordingSessionId={}",
                recordingSessionId
        );
    }


    private void sendConfig(
            ClovaGrpcSession session
    ) {

        try {

            String configJson =
                    objectMapper.writeValueAsString(
                            Map.of(
                                    "transcription",
                                    Map.of(
                                            "language",
                                            "ko"
                                    ),
                                    "semanticEpd",
                                    Map.of(
                                            "skipEmptyText",
                                            true,
                                            "useWordEpd",
                                            true,
                                            "usePeriodEpd",
                                            true,
                                            "gapThreshold",
                                            2000
                                    )
                            )
                    );


            NestConfig config =
                    NestConfig.newBuilder()
                            .setConfig(
                                    configJson
                            )
                            .build();


            NestRequest request =
                    NestRequest.newBuilder()
                            .setType(
                                    RequestType.CONFIG
                            )
                            .setConfig(
                                    config
                            )
                            .build();


            session.getRequestObserver()
                    .onNext(
                            request
                    );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "CLOVA 실시간 STT 설정 전송에 실패했습니다.",
                    e
            );
        }
    }

    public void sendAudio(
            Long recordingSessionId,
            byte[] audioChunk
    ) {

        if (audioChunk == null
                || audioChunk.length == 0) {

            return;
        }


        ClovaGrpcSession session =
                sessionManager.get(
                        recordingSessionId
                );


        String extraContents =
                """
                {
                  "seqId": %d,
                  "epFlag": false
                }
                """.formatted(
                        session.nextSequence()
                );


        NestData data =
                NestData.newBuilder()
                        .setChunk(
                                ByteString.copyFrom(
                                        audioChunk
                                )
                        )
                        .setExtraContents(
                                extraContents
                        )
                        .build();


        NestRequest request =
                NestRequest.newBuilder()
                        .setType(
                                RequestType.DATA
                        )
                        .setData(
                                data
                        )
                        .build();


        session.getRequestObserver()
                .onNext(
                        request
                );
    }

    // CLOVA 응답 → onTranscript()
    private void handleResponse(
            Long recordingSessionId,
            NestResponse response,
            BiConsumer<Integer, String> transcriptConsumer
    ) {

        try {
            String contents = response.getContents();

            if (contents == null || contents.isBlank()) {
                return;
            }

            ClovaRealtimeResponse result =
                    objectMapper.readValue(
                            contents,
                            ClovaRealtimeResponse.class
                    );


            if (result.getResponseType() == null
                    || !result.getResponseType()
                    .contains("transcription")) {

                return;
            }


            if (result.getTranscription() == null) {

                return;
            }


            String text = result.getTranscription().getText();

            Integer position = result.getTranscription().getPosition();


            if (text == null || text.isBlank()) {
                return;
            }

            if (position == null) {
                log.warn(
                        "CLOVA transcription position 없음. recordingSessionId={}, text={}",
                        recordingSessionId,
                        text
                );
                return;
            }

            log.debug(
                    "CLOVA transcription. recordingSessionId={}, position={}, seqId={}, text={}",
                    recordingSessionId,
                    position,
                    result.getTranscription().getSeqId(),
                    text
            );


            transcriptConsumer.accept(position,text);


        } catch (Exception e) {

            log.warn(
                    "CLOVA 실시간 응답 파싱 실패. recordingSessionId={}",
                    recordingSessionId,
                    e
            );
        }
    }

    // gRPC 종료
    public void finish(
            Long recordingSessionId
    ) {

        ClovaGrpcSession session =
                sessionManager.remove(
                        recordingSessionId
                );

        if (session == null) {

            return;
        }


        try {

            /*
             * 마지막 요청까지 전달했다는 의미로
             * client stream 완료
             */
            session.completeRequest();


            /*
             * 서버가 남은 응답을 전송할 시간을 조금 기다린다.
             */
            session.awaitCompletion();

        } finally {

            session.shutdown();
        }


        log.info(
                "CLOVA gRPC 세션 종료. recordingSessionId={}",
                recordingSessionId
        );
    }
}