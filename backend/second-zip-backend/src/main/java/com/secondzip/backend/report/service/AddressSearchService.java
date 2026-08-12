package com.secondzip.backend.report.service;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AddressCandidate;
import com.secondzip.backend.report.dto.response.AddressSearchResponse;
import com.secondzip.backend.report.service.external.client.AddressClient;
import com.secondzip.backend.report.service.external.client.AddressSearchCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressSearchService {

    /** 카카오가 허용하는 페이지 크기 상한. */
    private static final int MAX_SIZE = 30;

    /**
     * 계정당 분당 허용 호출 수.
     *
     * 자동완성은 타이핑마다 호출되므로 디바운스가 있어도 분당 수십 건이 나온다.
     * 한 계정이 카카오 일일 쿼터를 태우지 못하게 막는 것이 목적이며,
     * 정상 사용을 막지 않을 만큼은 넉넉해야 한다.
     */
    private static final int LIMIT_PER_MINUTE = 30;

    private final AddressClient addressClient;
    private final AddressSearchCache addressSearchCache;
    private final StringRedisTemplate redisTemplate;

    public AddressSearchResponse search(Long accountId, String query, int page, int size) {
        checkRateLimit(accountId);

        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), MAX_SIZE);

        List<AddressCandidate> candidates =
                addressClient.search(query, safePage, safeSize);

        List<AddressSearchResponse.AddressItem> items = candidates.stream()
                .map(c -> new AddressSearchResponse.AddressItem(
                        addressSearchCache.put(c.target()),
                        c.target().roadAddress(),
                        c.target().lotAddress(),
                        c.zoneNo()
                ))
                .collect(Collectors.toList());

        return new AddressSearchResponse(items);
    }

    /**
     * 분 단위 고정 윈도우 카운터.
     *
     * 키에 분(minute)을 넣어 윈도우가 자동으로 넘어가게 한다.
     * 슬라이딩 윈도우보다 경계에서 최대 2배까지 허용되지만,
     * 목적이 정밀 제어가 아니라 쿼터 남용 차단이므로 이 정도로 충분하다.
     *
     * Redis 장애로 카운터를 못 읽으면 통과시킨다. 검색이 막히는 것보다 낫다.
     */
    private void checkRateLimit(Long accountId) {
        String key = "ratelimit:address:" + accountId + ":"
                + (System.currentTimeMillis() / 60_000);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                return;
            }
            if (count == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(2));
            }
            if (count > LIMIT_PER_MINUTE) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            // Redis 장애는 검색을 막지 않음.
        }
    }
}