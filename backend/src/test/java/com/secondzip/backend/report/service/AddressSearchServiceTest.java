package com.secondzip.backend.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AddressCandidate;
import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.response.AddressSearchResponse;
import com.secondzip.backend.report.service.external.client.AddressClient;
import com.secondzip.backend.report.service.external.client.AddressSearchCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressSearchServiceTest {

    @Test
    @DisplayName("검색 결과마다 토큰을 발급해 함께 내려준다")
    void issuesTokenForEveryResult() {
        AddressSearchService service = service(
                new StubAddressClient(
                        candidate("서울 강남구 테헤란로 152", "06236"),
                        candidate("서울 서초구 테헤란로 152", "06611")
                ),
                new CountingRedis()
        );

        AddressSearchResponse response = service.search(1L, "테헤란로 152", 1, 30);

        assertEquals(2, response.getAddresses().size());
        for (AddressSearchResponse.AddressItem item : response.getAddresses()) {
            assertNotNull(item.getAddressToken(), "토큰이 없으면 재검색 경로로 떨어진다");
        }
        assertEquals("06236", response.getAddresses().get(0).getZoneNo());
        assertEquals(
                "서울 강남구 테헤란로 152",
                response.getAddresses().get(0).getRoadAddress()
        );
    }

    @Test
    @DisplayName("분당 허용량을 넘기면 429를 던진다")
    void rejectsWhenRateLimitExceeded() {
        AddressSearchService service = service(
                new StubAddressClient(candidate("서울 강남구 테헤란로 152", "06236")),
                new CountingRedis()
        );

        for (int i = 0; i < 30; i++) {
            service.search(1L, "테헤란로", 1, 30);
        }

        BusinessException e = assertThrows(
                BusinessException.class,
                () -> service.search(1L, "테헤란로", 1, 30)
        );
        assertEquals(ErrorCode.TOO_MANY_REQUESTS, e.getErrorCode());
    }

    @Test
    @DisplayName("레이트 리밋은 계정별로 따로 센다")
    void countsRateLimitPerAccount() {
        AddressSearchService service = service(
                new StubAddressClient(candidate("서울 강남구 테헤란로 152", "06236")),
                new CountingRedis()
        );

        for (int i = 0; i < 30; i++) {
            service.search(1L, "테헤란로", 1, 30);
        }

        // 한 계정이 한도를 채워도 다른 계정은 영향받지 않아야 한다
        assertEquals(1, service.search(2L, "테헤란로", 1, 30).getAddresses().size());
    }

    @Test
    @DisplayName("Redis 장애로 카운터를 못 세도 검색은 통과시킨다")
    void allowsSearchWhenRedisIsDown() {
        AddressSearchService service = service(
                new StubAddressClient(candidate("서울 강남구 테헤란로 152", "06236")),
                new BrokenRedis()
        );

        assertEquals(1, service.search(1L, "테헤란로 152", 1, 30).getAddresses().size());
    }

    @Test
    @DisplayName("size는 30을 넘지 않고 page는 1 미만으로 내려가지 않는다")
    void clampsPagingParameters() {
        StubAddressClient client =
                new StubAddressClient(candidate("서울 강남구 테헤란로 152", "06236"));
        AddressSearchService service = service(client, new CountingRedis());

        service.search(1L, "테헤란로 152", 0, 500);

        assertEquals(1, client.lastPage);
        assertEquals(30, client.lastSize);
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenNoResult() {
        AddressSearchService service =
                service(new StubAddressClient(), new CountingRedis());

        assertTrue(service.search(1L, "없는 주소", 1, 30).getAddresses().isEmpty());
    }

    // ---------- 헬퍼 ----------

    private AddressSearchService service(
            AddressClient addressClient,
            StringRedisTemplate redisTemplate
    ) {
        return new AddressSearchService(
                addressClient,
                new SequentialTokenCache(),
                redisTemplate
        );
    }

    private AddressCandidate candidate(String roadAddress, String zoneNo) {
        return new AddressCandidate(
                new AnalysisTarget(
                        roadAddress, roadAddress, "1168010100", "11680",
                        "10100", "737", "", "152", "", ""
                ),
                zoneNo
        );
    }

    private static class StubAddressClient extends AddressClient {
        private final List<AddressCandidate> candidates;
        private int lastPage;
        private int lastSize;

        private StubAddressClient(AddressCandidate... candidates) {
            super(new RestTemplate());
            this.candidates = new ArrayList<>(List.of(candidates));
        }

        @Override
        public List<AddressCandidate> search(String inputAddress, int page, int size) {
            this.lastPage = page;
            this.lastSize = size;
            return candidates;
        }
    }

    /** 매번 새 토큰을 돌려주는 캐시. Redis 없이 동작한다. */
    private static class SequentialTokenCache extends AddressSearchCache {
        private final AtomicLong sequence = new AtomicLong();

        private SequentialTokenCache() {
            super(null, new ObjectMapper());
        }

        @Override
        public String put(AnalysisTarget target) {
            return "token-" + sequence.incrementAndGet();
        }
    }

    /**
     * INCR만 흉내 내는 최소 구현.
     *
     * <p>{@code ValueOperations}는 메서드가 많고 spring-data-redis 버전에 따라
     * 시그니처가 달라지므로, 직접 구현하지 않고 동적 프록시로 만든다.
     * 이러면 라이브러리를 올려도 이 테스트가 깨지지 않는다.
     */
    private static class CountingRedis extends StringRedisTemplate {
        private final Map<String, Long> counters = new ConcurrentHashMap<>();

        @Override
        public ValueOperations<String, String> opsForValue() {
            return valueOpsProxy(key -> counters.merge(key, 1L, Long::sum));
        }

        @Override
        public Boolean expire(String key, Duration timeout) {
            return true;
        }
    }

    /** opsForValue 자체가 터지는 상황. */
    private static class BrokenRedis extends StringRedisTemplate {
        @Override
        public ValueOperations<String, String> opsForValue() {
            throw new IllegalStateException("redis down");
        }
    }

    @SuppressWarnings("unchecked")
    private static ValueOperations<String, String> valueOpsProxy(
            java.util.function.Function<String, Long> incrementByKey
    ) {
        return (ValueOperations<String, String>) Proxy.newProxyInstance(
                ValueOperations.class.getClassLoader(),
                new Class<?>[]{ValueOperations.class},
                (proxy, method, args) -> {
                    if ("increment".equals(method.getName())
                            && args != null && args.length == 1) {
                        return incrementByKey.apply((String) args[0]);
                    }
                    throw new UnsupportedOperationException(
                            "테스트가 기대하지 않은 호출: " + method.getName()
                    );
                }
        );
    }
}
