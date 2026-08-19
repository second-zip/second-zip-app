package com.secondzip.backend.map.jeonseprice.service;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.map.jeonseprice.client.RebJeonsePriceClient;
import com.secondzip.backend.map.jeonseprice.domain.JeonsePriceIndexVO;
import com.secondzip.backend.map.jeonseprice.domain.SigunguRegionMappingVO;
import com.secondzip.backend.map.jeonseprice.domain.enums.RebSidoRegion;
import com.secondzip.backend.map.jeonseprice.dto.external.RebJeonsePriceRowDTO;
import com.secondzip.backend.map.jeonseprice.mapper.JeonsePriceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JeonsePriceSyncServiceImpl
        implements JeonsePriceSyncService {

    private static final String INDEX_ITEM_NAME = "지수";
    private static final String KEY_SEPARATOR = "|";

    private final RebJeonsePriceClient rebJeonsePriceClient;
    private final JeonsePriceMapper jeonsePriceMapper;

    @Override
    @Transactional
    public int sync(YearMonth targetMonth) {
        YearMonth previousMonth = targetMonth.minusMonths(1);

        // 한국부동산원 현재월 호출
        List<RebJeonsePriceRowDTO> currentRows = rebJeonsePriceClient.getMonthlyPriceIndices(targetMonth);

        // 한국부동산원 전월 호출
        List<RebJeonsePriceRowDTO> previousRows = rebJeonsePriceClient.getMonthlyPriceIndices(previousMonth);

        // 우리 DB의 시군구 148개 조회
        List<SigunguRegionMappingVO> sigunguRegions = jeonsePriceMapper.selectAllSigunguRegions();

        /*
         * 예:
         * 41|영통구 → 41117
         * 41|군포시 → 41410
         * 43|청원구 → 43114
         */
        Map<String, String> sigunguCodeMap = createSigunguCodeMap(sigunguRegions);

        /*
         * 한국부동산원 행을 내부 region_code 기준으로 변환
         *
         * 11    → 서울
         * 41117 → 영통구
         */
        Map<String, BigDecimal> currentIndexMap = createInternalIndexMap(currentRows, sigunguCodeMap);

        Map<String, BigDecimal> previousIndexMap = createInternalIndexMap(previousRows, sigunguCodeMap);

        Set<String> sidoCodes =
                Arrays.stream(RebSidoRegion.values())
                        .map(RebSidoRegion::getRegionCode)
                        .collect(Collectors.toSet());

        int sidoSavedCount = 0;
        int sigunguSavedCount = 0;

        for (Map.Entry<String, BigDecimal> entry
                : currentIndexMap.entrySet()) {

            String regionCode = entry.getKey();
            BigDecimal currentIndex = entry.getValue();

            BigDecimal previousIndex = previousIndexMap.get(regionCode);

            /*
             * 전월 데이터가 없으면 변동률을 계산할 수 없으므로
             * 해당 지역만 건너뜀
             */
            if (previousIndex == null) {
                log.warn(
                        "전월 전세가격지수를 찾지 못해 저장을 건너뜁니다. "
                                + "regionCode={}, previousMonth={}",
                        regionCode,
                        previousMonth
                );

                continue;
            }

            BigDecimal changeRate =
                    calculateChangeRate(
                            currentIndex,
                            previousIndex
                    );

            JeonsePriceIndexVO jeonsePriceIndex =
                    JeonsePriceIndexVO.builder()
                            .regionCode(regionCode)
                            .baseMonth(
                                    targetMonth.atDay(1)
                            )
                            .priceIndex(
                                    currentIndex.setScale(
                                            4,
                                            RoundingMode.HALF_UP
                                    )
                            )
                            .changeRate(changeRate)
                            .build();

            int affectedRows =
                    jeonsePriceMapper
                            .upsertJeonsePriceIndex(
                                    jeonsePriceIndex
                            );

            if (affectedRows == 0) {
                throw new BusinessException(
                        ErrorCode.INTERNAL_SERVER_ERROR, "전세가격지수 지역 매핑에 실패했습니다."
                );
            }

            if (sidoCodes.contains(regionCode)) {
                sidoSavedCount++;
            } else {
                sigunguSavedCount++;
            }
        }

        /*
         * 시도 17개는 반드시 모두 저장돼야 함.
         * 하나라도 빠지면 전체 트랜잭션 롤백.
         */
        if (sidoSavedCount != RebSidoRegion.values().length) {

            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR, "시도 전세가격지수가 모두 저장되지 않았습니다."
            );
        }

        int totalSavedCount = sidoSavedCount + sigunguSavedCount;

        log.info(
                "전세가격지수 동기화 완료. "
                        + "targetMonth={}, "
                        + "sidoSavedCount={}, "
                        + "sigunguSavedCount={}, "
                        + "totalSavedCount={}",
                targetMonth,
                sidoSavedCount,
                sigunguSavedCount,
                totalSavedCount
        );

        return totalSavedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAlreadySynced(YearMonth targetMonth) {
        int savedSidoCount = jeonsePriceMapper
                        .countSidoJeonsePriceIndicesByMonth(
                                targetMonth.atDay(1)
                        );

        return savedSidoCount == RebSidoRegion.values().length;
    }

    /**
     * 우리 DB의 시군구 목록을
     * "부모 시도 코드|시군구 이름" 형태로 변환
     */
    private Map<String, String> createSigunguCodeMap(
            List<SigunguRegionMappingVO> regions
    ) {
        Map<String, String> regionCodeMap = new HashMap<>();

        for (SigunguRegionMappingVO region : regions) {
            String key = createRegionKey(
                    region.getParentRegionCode(),
                    region.getRegionName()
            );

            String existingRegionCode =
                    regionCodeMap.put(
                            key,
                            region.getRegionCode()
                    );

            if (existingRegionCode != null) {
                throw new BusinessException(
                        ErrorCode.INTERNAL_SERVER_ERROR, "중복된 시군구 지역 매핑이 존재합니다."
                );
            }
        }

        return regionCodeMap;
    }

    /**
     * 한국부동산원 데이터를
     * 우리 regions.region_code 기준 Map으로 변환
     */
    private Map<String, BigDecimal> createInternalIndexMap(
            List<RebJeonsePriceRowDTO> rows,
            Map<String, String> sigunguCodeMap
    ) {
        Map<String, BigDecimal> indexMap = new HashMap<>();

        for (RebJeonsePriceRowDTO row : rows) {
            if (!INDEX_ITEM_NAME.equals(
                    row.getItemName()
            )) {
                continue;
            }

            if (row.getPriceIndex() == null) {
                continue;
            }

            /*
             * 시도 행 처리
             *
             * 서울 / 경기 / 부산 등은
             * CLS_FULLNM에 '>'가 없음
             */
            Optional<RebSidoRegion> sidoRegion =
                    RebSidoRegion
                            .findByRebRegionName(
                                    row.getRegionName()
                            );

            if (sidoRegion.isPresent()
                    && isSidoRow(row)) {

                indexMap.put(
                        sidoRegion.get().getRegionCode(),
                        row.getPriceIndex()
                );

                continue;
            }



            /*
             * 시군구 행 처리
             *
             * 경기>경부2권>수원시>영통구
             * → 첫 경로 경기
             * → CLS_NM 영통구
             */
            String fullRegionName = row.getFullRegionName();

            if (fullRegionName == null || fullRegionName.isBlank()) {
                continue;
            }

            Optional<RebSidoRegion> parentSido = findParentSido(fullRegionName);

            if (parentSido.isEmpty()) {
                continue;
            }

            String regionKey = createRegionKey(
                    parentSido.get()
                            .getRegionCode(),
                    row.getRegionName()
            );

            /*
             * 경부1권, 중부산권처럼
             * 우리 regions에 없는 통계 권역은 null이므로 제외됨
             */
            String internalRegionCode = sigunguCodeMap.get(regionKey);

            if (internalRegionCode == null) {
                continue;
            }

            indexMap.put(
                    internalRegionCode,
                    row.getPriceIndex()
            );
        }

        return indexMap;
    }

    private String createRegionKey(
            String parentRegionCode,
            String regionName
    ) {
        return parentRegionCode
                + KEY_SEPARATOR
                + regionName;
    }

    private BigDecimal calculateChangeRate(
            BigDecimal currentIndex,
            BigDecimal previousIndex
    ) {
        if (previousIndex.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "전월 지수가 0이므로 변동률을 계산할 수 없습니다."
            );
        }

        return currentIndex
                .subtract(previousIndex)
                .divide(
                        previousIndex,
                        8,
                        RoundingMode.HALF_UP
                )
                .multiply(
                        BigDecimal.valueOf(100)
                )
                .setScale(
                        4,
                        RoundingMode.HALF_UP
                );
    }

    private boolean isSidoRow(
            RebJeonsePriceRowDTO row
    ) {
        String fullRegionName = row.getFullRegionName();

        if (fullRegionName == null || fullRegionName.isBlank()) {
            return true;
        }

        String[] path = fullRegionName.split(">");

        // 서울, 경기 등 일반적인 시도 행
        if (path.length == 1) {
            return true;
        }

        // 전남광주>전남, 전남광주>광주처럼
        // 통계 권역 아래에 포함된 시도 행
        return path.length == 2
                && path[path.length - 1]
                .equals(row.getRegionName());
    }

    private Optional<RebSidoRegion> findParentSido(
            String fullRegionName
    ) {
        if (fullRegionName == null || fullRegionName.isBlank()) {
            return Optional.empty();
        }

        String[] path = fullRegionName.split(">");

        for (String regionName : path) {
            Optional<RebSidoRegion> sido =
                    RebSidoRegion.findByRebRegionName(
                            regionName.trim()
                    );

            if (sido.isPresent()) {
                return sido;
            }
        }

        return Optional.empty();
    }
}