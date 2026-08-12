package com.secondzip.backend.record.grpc;

import com.nbp.cdncp.nest.grpc.proto.v1.NestRequest;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.Getter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// CLOVA Speech와 gRPC 실시간 스트리밍 통신에 필요한 객체 == 하나의 gRPC 연결 생명주기를 관리
@Getter
public class ClovaGrpcSession {

    private final ManagedChannel channel; // 우리 서버와 CLOVA gRPC 서버 사이의 네트워크 연결(Rest 같은 역할)

    /*
    CLOVA의 recognize()는 양방향 스트리밍이므로 음성 데이터를 여러번 전송 시 사용
    CLOVA로 NestRequest를 계속 흘려보내는 송신 통로
     */
    private final StreamObserver<NestRequest> requestObserver;

    /*
    CLOVA가 응답 스트림 처리를 끝낼 때까지 기다리기 위한 동기화 도구
    gRPC 스트리밍은 비동기로 동작하므로 마지막 요청보다 channel이 먼저 닫아지는 경우 고려
     */
    private final CountDownLatch completedLatch;

    private final AtomicInteger sequence = new AtomicInteger(1); //세션 내에서 순번을 생성(몇번째 음성 chunk인지)


    public ClovaGrpcSession(
            ManagedChannel channel, //CLOVA와 연결
            StreamObserver<NestRequest> requestObserver, //CLOVA로 음성 데이터 전송
            CountDownLatch completedLatch //CLOVA의 처리 완료 대기
    ) {

        this.channel = channel;
        this.requestObserver = requestObserver;
        this.completedLatch = completedLatch;
    }


    public int nextSequence() {
        return sequence.getAndIncrement();
    }


    public void completeRequest() {
        requestObserver.onCompleted();
    } //Client → Server 요청 스트림 종료(gRPC 연결 즉시 종료가 아닌 마지막 응답 의미)


    //최대 5초 동안 CLOVA의 처리 완료를 기다리는 메서드
    public void awaitCompletion() {

        try {

            completedLatch.await(
                    5,
                    TimeUnit.SECONDS
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt(); //interrupt 상태를 복구
        }
    }


    public void shutdown() {

        channel.shutdown();

        try {

            if (!channel.awaitTermination(
                    3,
                    TimeUnit.SECONDS
            )) {

                channel.shutdownNow();
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            channel.shutdownNow();
        }
    }
}