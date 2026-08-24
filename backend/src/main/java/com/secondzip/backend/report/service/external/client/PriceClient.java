package com.secondzip.backend.report.service.external.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.dto.AnalysisTargetDTO;
import com.secondzip.backend.report.dto.external.PriceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.secondzip.backend.report.service.external.RealApiCondition;
import org.springframework.context.annotation.Conditional;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 국토교통부 실거래가 4종 API(아파트/연립다세대/단독다가구/오피스텔) 클라이언트.
 * buildingType에 맞는 서비스 URL을 선택해 호출하고, 지번(본번/부번)이 일치하는
 * 매물을 찾아 최근 실거래가를 반환.
 *
 * 실패/데이터없음 시 항상 null 반환 (Mock 데이터 생성 금지 원칙 유지).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Conditional(RealApiCondition.class)
public class PriceClient implements PriceDataProvider {

    private final RestTemplate restTemplate;
    /**
     * 시군구·월 단위 응답 캐시. 주입되지 않아도(단위 테스트 등) 동작해야 한다.
     */
    private final PriceMonthCache priceMonthCache;

    @Value("${REALTY_PRICE_API_KEY:}")
    private String apiKey;

    private static final Map<String, String> SERVICE_URLS = Map.of(
            "APARTMENT",       "https://apis.data.go.kr/1613000/RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade",
            "MULTI_HOUSEHOLD", "https://apis.data.go.kr/1613000/RTMSDataSvcRHTrade/getRTMSDataSvcRHTrade",
            "MULTI_FAMILY",    "https://apis.data.go.kr/1613000/RTMSDataSvcSHTrade/getRTMSDataSvcSHTrade",
            "SINGLE_FAMILY",   "https://apis.data.go.kr/1613000/RTMSDataSvcSHTrade/getRTMSDataSvcSHTrade",
            "OFFICETEL",       "https://apis.data.go.kr/1613000/RTMSDataSvcOffiTrade/getRTMSDataSvcOffiTrade"
    );

    private static final Set<String> SUCCESS_CODES = Set.of("000", "00");
    /**
     * 실거래가 소급 조회 개월 수.
     *
     * 이 API는 매물이 아니라 시군구 전체 거래를 월 단위로 내려주므로,
     * 한 달을 더 볼 때마다 (그 달의 거래 수 / 100)만큼 요청이 늘어난다.
     * 기준가로 쓸 만한 최신성과 호출량 사이에서 6개월로 둔다.
     */
    private static final int MAX_MONTHS_LOOKBACK = 6;
    private static final int ROWS_PER_PAGE = 100;
    private static final int MAX_PAGES_PER_MONTH = 100;
    private static final int MAX_PAGE_ATTEMPTS = 2;
    /**
     * 전용면적 허용 오차(㎡).
     *
     * 건축물대장과 실거래가의 면적은 반올림 자릿수가 달라 84.9540과 84.95처럼
     * 어긋나는 사례가 있다. 완전일치만 인정하면 같은 호의 거래를 놓치고
     * 공시가격 폴백으로 빠진다. 이웃한 다른 호를 잘못 잡지 않을 만큼만 넓힌다.
     */
    private static final BigDecimal AREA_TOLERANCE_SQM = new BigDecimal("0.05");
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Pattern MOUNTAIN_LOT_PATTERN =
            Pattern.compile("(?:^|\\s)산\\s*\\d");

    /**
     * target       주소 표준화 결과 (sigunguCode, mainNo, subNo 필요)
     * buildingType SINGLE_FAMILY/MULTI_FAMILY/APARTMENT/MULTI_HOUSEHOLD/OFFICETEL
     */
    public PriceData getPriceData(
            AnalysisTargetDTO target,
            String buildingType,
            BigDecimal transactionAreaSqm,
            Integer transactionFloor
    ) {
        if (target == null || isBlank(target.sigunguCode())) {
            log.warn("AnalysisTarget 또는 시군구코드가 없어 실거래가 조회를 스킵합니다.");
            return null;
        }
        if (isBlank(target.legalDongName()) || transactionAreaSqm == null
                || transactionAreaSqm.signum() <= 0) {
            log.warn("법정동명 또는 대상 면적이 없어 실거래가를 안전하게 매칭할 수 없습니다: {}", target);
            return null;
        }
        Integer targetMainNo = parseToInt(target.mainNo());
        Integer targetSubNo = parseToInt(target.subNo());
        if (targetMainNo == null || targetMainNo <= 0
                || (!isBlank(target.subNo())
                && (targetSubNo == null || targetSubNo < 0))) {
            log.warn("대상 지번의 본번/부번을 안전하게 파싱할 수 없습니다: {}", target);
            return null;
        }
        if (requiresFloor(buildingType) && transactionFloor == null) {
            log.warn("대상 층이 없어 집합건물 실거래가를 안전하게 매칭할 수 없습니다: {}", target);
            return null;
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new PriceLookupException("실거래가 API 키가 없습니다.");
        }

        String serviceUrl = resolveServiceUrl(buildingType);
        if (serviceUrl == null) {
            log.warn("건물유형({})에 매칭되는 실거래가 API가 없습니다.", buildingType);
            return null;
        }

        try {
            YearMonth latestMonth = YearMonth.now(SEOUL_ZONE);
            for (int monthsAgo = 0; monthsAgo < MAX_MONTHS_LOOKBACK; monthsAgo++) {
                String dealYmd = latestMonth.minusMonths(monthsAgo)
                        .toString().replace("-", "");

                PriceData found = fetchAndFind(
                        serviceUrl,
                        target,
                        buildingType,
                        transactionAreaSqm,
                        transactionFloor,
                        dealYmd
                );
                if (found != null) {
                    return found;
                }
            }
            log.warn("최근 {}개월 내 일치하는 실거래 데이터를 찾지 못했습니다: {}", MAX_MONTHS_LOOKBACK, target);
            return null;

        } catch (AmbiguousTradeException e) {
            log.warn("동일 최신 거래일의 금액이 여러 개라 실거래가를 확정하지 않습니다: {}", target);
            return null;
        } catch (PriceLookupException e) {
            log.error("실거래가 응답을 완전하게 확인하지 못했습니다: {}", target, e);
            throw e;
        } catch (Exception e) {
            log.error("실거래가 조회 중 에러: {}", target, e);
            throw new PriceLookupException("실거래가 API 조회에 실패했습니다.", e);
        }
    }

    private String resolveServiceUrl(String buildingType) {
        if (buildingType == null) return null;
        return SERVICE_URLS.get(buildingType);
    }

    private PriceData fetchAndFind(
            String serviceUrl,
            AnalysisTargetDTO target,
            String buildingType,
            BigDecimal transactionAreaSqm,
            Integer transactionFloor,
            String dealYmd
    ) throws Exception {
        TradeCandidate latest = null;
        boolean latestDateAmbiguous = false;
        LocalDate latestUnreadableAmountDate = null;
        boolean hasUnreadableDealDate = false;

        List<Map<String, Object>> monthItems =
                loadMonthItems(serviceUrl, target, dealYmd);
        if (monthItems.isEmpty()) {
            log.info("{}월 거래 내역 없음: {}", dealYmd, target);
        }

        for (Map<String, Object> item : findMatchingItems(
                monthItems,
                target,
                buildingType,
                transactionAreaSqm,
                transactionFloor
        )) {
            if (isCancelled(item)) {
                continue;
            }

            LocalDate dealDate = parseDealDate(item, dealYmd);
            if (dealDate == null) {
                // 정확히 같은 매물인데 일자를 읽지 못하면 다른 후보보다
                // 최신인지 증명할 수 없다. 전체 페이지를 읽은 뒤 미확인 처리한다.
                hasUnreadableDealDate = true;
                continue;
            }

            Long amount = parseDealAmount(asText(item.get("dealAmount")));
            if (amount == null) {
                if (latestUnreadableAmountDate == null
                        || dealDate.isAfter(latestUnreadableAmountDate)) {
                    latestUnreadableAmountDate = dealDate;
                }
                continue;
            }

            if (latest == null || dealDate.isAfter(latest.dealDate())) {
                latest = new TradeCandidate(dealDate, amount);
                latestDateAmbiguous = false;
            } else if (dealDate.equals(latest.dealDate())
                    && !amount.equals(latest.amount())) {
                latestDateAmbiguous = true;
            }
        }

        boolean unreadableCandidateMayBeLatest = hasUnreadableDealDate
                || latestUnreadableAmountDate != null
                && (latest == null
                || !latestUnreadableAmountDate.isBefore(latest.dealDate()));
        if (unreadableCandidateMayBeLatest) {
            throw new PriceLookupFailedException(
                    "정확히 일치하는 거래의 최신 일자 또는 금액을 확인할 수 없습니다."
            );
        }

        if (latest == null || latestDateAmbiguous) {
            if (latestDateAmbiguous) {
                throw new AmbiguousTradeException();
            }
            return null;
        }

        PriceData data = new PriceData();
        data.setRecentSalePrice(latest.amount());
        log.info("실거래가 매칭 성공: dealYmd={}, dealDate={}, amount={}원",
                dealYmd, latest.dealDate(), latest.amount());
        return data;
    }

    /**
     * 한 시군구·한 달의 거래를 전부 읽어온다.
     *
     * 이 API는 매물 단위 조회가 없고 시군구 전체 거래를 월 단위로 내려주므로,
     * 같은 시군구의 다른 매물을 분석할 때도 완전히 같은 응답이 필요하다.
     * 캐시가 없으면 리포트 한 건마다 (개월 수 × 페이지 수)만큼 공공데이터포털을
     * 다시 호출하게 되어 일일 트래픽 한도를 금방 소진한다.
     */
    private List<Map<String, Object>> loadMonthItems(
            String serviceUrl,
            AnalysisTargetDTO target,
            String dealYmd
    ) throws Exception {
        String cacheKey = serviceUrl + "|" + target.sigunguCode().trim() + "|" + dealYmd;
        List<Map<String, Object>> cached = findCachedMonth(cacheKey);
        if (cached != null) {
            log.info("실거래가 월별 캐시 사용: dealYmd={}, items={}", dealYmd, cached.size());
            return cached;
        }

        List<Map<String, Object>> allItems = new ArrayList<>();
        Integer declaredTotalCount = null;

        for (int pageNo = 1; pageNo <= MAX_PAGES_PER_MONTH; pageNo++) {
            ApiPage apiPage = requestPage(serviceUrl, target, dealYmd, pageNo);
            if (!SUCCESS_CODES.contains(apiPage.resultCode())) {
                throw new PriceLookupFailedException(
                        "실거래가 응답 에러: code=" + apiPage.resultCode()
                                + ", msg=" + apiPage.resultMessage()
                );
            }

            int totalCount = apiPage.totalCount();
            if (declaredTotalCount != null && declaredTotalCount != totalCount) {
                throw new PriceLookupFailedException(
                        "실거래가 페이지별 totalCount가 일치하지 않습니다."
                );
            }
            declaredTotalCount = totalCount;

            List<Map<String, Object>> items = apiPage.items();
            if (items == null || items.isEmpty()) {
                if (totalCount > 0) {
                    throw new PriceLookupFailedException(
                            "totalCount와 items가 일치하지 않습니다: totalCount=" + totalCount
                    );
                }
                break;
            }
            allItems.addAll(items);
            if (allItems.size() > totalCount) {
                throw new PriceLookupFailedException(
                        "실거래가 응답 item 수가 totalCount를 초과합니다."
                );
            }

            int totalPages = Math.max(1, (totalCount + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
            if (totalPages > MAX_PAGES_PER_MONTH) {
                throw new PriceLookupFailedException(
                        "월 전체 거래를 페이지 상한 내에서 확인할 수 없습니다: totalPages=" + totalPages
                );
            }
            if (pageNo >= totalPages) {
                break;
            }
        }

        if (declaredTotalCount == null || allItems.size() != declaredTotalCount) {
            throw new PriceLookupFailedException(
                    "월 전체 거래를 모두 읽지 못했습니다: fetched=" + allItems.size()
                            + ", totalCount=" + declaredTotalCount
            );
        }

        putCachedMonth(cacheKey, allItems);
        return allItems;
    }

    /**
     * 한 페이지를 읽는다. 일시적인 네트워크·5xx 오류는 한 번 더 시도한다.
     * 여기서 그대로 실패하면 그 달을 "거래 없음"으로 오해하게 되므로,
     * 재시도 후에도 실패하면 반드시 예외로 알린다.
     */
    private ApiPage requestPage(
            String serviceUrl,
            AnalysisTargetDTO target,
            String dealYmd,
            int pageNo
    ) throws Exception {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(serviceUrl)
                .queryParam("serviceKey", apiKey)
                .queryParam("LAWD_CD", target.sigunguCode())
                .queryParam("DEAL_YMD", dealYmd)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", ROWS_PER_PAGE)
                .build(true)   // 이중 인코딩 방지
                .toUri();

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_PAGE_ATTEMPTS; attempt++) {
            log.info("실거래가 조회 요청: dealYmd={}, pageNo={}, attempt={}, target={}",
                    dealYmd, pageNo, attempt, target);
            try {
                ResponseEntity<String> response =
                        restTemplate.getForEntity(uri, String.class);
                String payload = response.getBody();
                if (payload == null || payload.isBlank()) {
                    throw new PriceLookupFailedException("실거래가 응답이 비어있습니다.");
                }
                return parsePayload(payload);
            } catch (RestClientException e) {
                lastFailure = e;
                log.warn("실거래가 요청 실패, 재시도 여부 판단: attempt={}, type={}",
                        attempt, e.getClass().getSimpleName());
            }
        }
        throw new PriceLookupFailedException(
                "실거래가 요청이 반복 실패했습니다: dealYmd=" + dealYmd + ", pageNo=" + pageNo,
                lastFailure
        );
    }

    private List<Map<String, Object>> findCachedMonth(String cacheKey) {
        if (priceMonthCache == null) {
            return null;
        }
        try {
            return priceMonthCache.find(cacheKey);
        } catch (Exception e) {
            // 캐시는 최적화일 뿐이다. 조회 실패가 분석을 막지 않는다.
            log.warn("실거래가 캐시 조회를 건너뜁니다: type={}", e.getClass().getSimpleName());
            return null;
        }
    }

    private void putCachedMonth(String cacheKey, List<Map<String, Object>> items) {
        if (priceMonthCache == null) {
            return;
        }
        try {
            priceMonthCache.put(cacheKey, items);
        } catch (Exception e) {
            log.warn("실거래가 캐시 저장을 건너뜁니다: type={}", e.getClass().getSimpleName());
        }
    }

    private int parseRequiredTotalCount(Object value) {
        if (value == null) {
            throw new PriceLookupFailedException("실거래가 응답 totalCount가 없습니다.");
        }
        try {
            int count = value instanceof Number
                    ? ((Number) value).intValue()
                    : Integer.parseInt(value.toString());
            if (count < 0) {
                throw new NumberFormatException("negative totalCount");
            }
            return count;
        } catch (NumberFormatException e) {
            throw new PriceLookupFailedException("실거래가 응답 totalCount가 숫자가 아닙니다.");
        }
    }

    private ApiPage parsePayload(String payload) throws Exception {
        String trimmed = payload.trim();
        if (trimmed.startsWith("<")) {
            return parseXmlPayload(trimmed);
        }
        return parseJsonPayload(trimmed);
    }

    /** 공공데이터포털의 공식 XML 응답을 XXE가 불가능한 설정으로 읽는다. */
    private ApiPage parseXmlPayload(String payload) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        setXmlAttributeIfSupported(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setXmlAttributeIfSupported(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        Document document = factory.newDocumentBuilder().parse(
                new InputSource(new StringReader(payload))
        );
        if (document.getElementsByTagName("header").getLength() == 0
                || document.getElementsByTagName("body").getLength() == 0
                || document.getElementsByTagName("totalCount").getLength() == 0) {
            throw new PriceLookupFailedException("실거래가 XML 응답 필수 구조가 없습니다.");
        }
        String resultCode = firstElementText(document, "resultCode");
        String resultMessage = firstElementText(document, "resultMsg");
        int totalCount = parseRequiredTotalCount(firstElementText(document, "totalCount"));

        List<Map<String, Object>> items = new ArrayList<>();
        NodeList itemNodes = document.getElementsByTagName("item");
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Node itemNode = itemNodes.item(i);
            Map<String, Object> item = new LinkedHashMap<>();
            NodeList children = itemNode.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    item.put(child.getNodeName(), child.getTextContent().trim());
                }
            }
            items.add(item);
        }
        return new ApiPage(resultCode, resultMessage, totalCount, items);
    }

    private void setXmlAttributeIfSupported(
            DocumentBuilderFactory factory,
            String attribute,
            String value
    ) {
        try {
            factory.setAttribute(attribute, value);
        } catch (IllegalArgumentException e) {
            // 구버전 Xerces는 JAXP 1.5 속성을 모른다. 위 네 가지 XXE 차단 feature는 유지된다.
            log.debug("XML parser가 보안 속성을 지원하지 않습니다: {}", attribute);
        }
    }

    private String firstElementText(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent().trim();
    }

    /** 과거 JSON 프록시/테스트 응답도 읽되, 공식 요청은 XML을 사용한다. */
    @SuppressWarnings("unchecked")
    private ApiPage parseJsonPayload(String payload) throws Exception {
        Map<String, Object> root = JSON_MAPPER.readValue(
                payload,
                new TypeReference<Map<String, Object>>() {
                }
        );
        Object responseObject = root.get("response");
        if (!(responseObject instanceof Map<?, ?> responseMap)) {
            return new ApiPage(null, null, 0, List.of());
        }
        Object headerObject = responseMap.get("header");
        Map<String, Object> header = headerObject instanceof Map<?, ?>
                ? (Map<String, Object>) headerObject
                : Map.of();
        Object bodyObject = responseMap.get("body");
        if (!(bodyObject instanceof Map<?, ?>)) {
            throw new PriceLookupFailedException("실거래가 JSON 응답 body가 없습니다.");
        }
        Map<String, Object> body = (Map<String, Object>) bodyObject;
        if (!body.containsKey("totalCount")) {
            throw new PriceLookupFailedException("실거래가 JSON 응답 totalCount가 없습니다.");
        }
        return new ApiPage(
                asText(header.get("resultCode")),
                asText(header.get("resultMsg")),
                parseRequiredTotalCount(body.get("totalCount")),
                extractItems(body)
        );
    }

    private record ApiPage(
            String resultCode,
            String resultMessage,
            int totalCount,
            List<Map<String, Object>> items
    ) {
    }

    private static String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private List<Map<String, Object>> extractItems(Map<String, Object> bodyMap) {
        Object itemsObj = bodyMap.get("items");
        if (!(itemsObj instanceof Map)) return null;
        Object itemObj = ((Map<String, Object>) itemsObj).get("item");

        if (itemObj instanceof List) {
            return (List<Map<String, Object>>) itemObj;
        }
        if (itemObj instanceof Map) {
            return List.of((Map<String, Object>) itemObj);
        }
        return null;
    }

    /**
     * 응답 목록(items) 중 target의 본번/부번과 일치하는 매물을 찾는다.
     * jibun 형식: "178-84"(본번-부번) 또는 "75"(본번만, 부번 없음).
     * 단독/다가구는 jibun이 비어있는 경우가 있어 매칭 실패(null)로 처리될 수 있음.
     */
    private List<Map<String, Object>> findMatchingItems(
            List<Map<String, Object>> items,
            AnalysisTargetDTO target,
            String buildingType,
            BigDecimal transactionAreaSqm,
            Integer transactionFloor
    ) {
        Integer targetMain = parseToInt(target.mainNo());
        Integer targetSub = parseToInt(target.subNo());

        if (targetMain == null) {
            log.warn("target의 본번을 알 수 없어 매물 매칭을 할 수 없습니다: {}", target);
            return List.of();
        }

        List<Map<String, Object>> matched = new ArrayList<>();
        for (Map<String, Object> item : items) {
            if (!sameLegalDong(item, target)) continue;

            String jibun = asText(item.get("jibun"));
            if (jibun == null || jibun.isBlank()) continue;
            if (!sameLotType(item, target, jibun)) continue;

            int[] parsed = parseItemLotNumbers(item, jibun);
            if (parsed == null) continue;
            int itemMain = parsed[0];
            int itemSub = parsed[1];

            boolean mainMatches = itemMain == targetMain;
            boolean subMatches = (targetSub == null ? 0 : targetSub) == itemSub;

            if (mainMatches && subMatches
                    && sameArea(item, buildingType, transactionAreaSqm)
                    && sameFloor(item, buildingType, transactionFloor)) {
                matched.add(item);
            }
        }

        if (matched.isEmpty()) {
            log.warn("법정동/본번/부번/면적이 모두 일치하는 거래를 찾지 못했습니다. target={}, 후보 {}건",
                    target, items.size());
        }
        return matched;
    }

    private boolean sameLotType(
            Map<String, Object> item,
            AnalysisTargetDTO target,
            String jibun
    ) {
        Boolean targetMountain = targetMountainFlag(target);
        if (targetMountain == null) {
            return false;
        }

        Boolean landCodeMountain = parseLandCode(item.get("landCd"));
        if (item.containsKey("landCd") && landCodeMountain == null
                && !isBlank(asText(item.get("landCd")))) {
            return false;
        }
        Boolean platCodeMountain = parsePlatCode(item.get("platGbCd"));
        if (item.containsKey("platGbCd") && platCodeMountain == null
                && !isBlank(asText(item.get("platGbCd")))) {
            return false;
        }
        if (landCodeMountain != null && platCodeMountain != null
                && !landCodeMountain.equals(platCodeMountain)) {
            return false;
        }

        Boolean explicitMountain = landCodeMountain != null
                ? landCodeMountain
                : platCodeMountain;
        boolean jibunSaysMountain = MOUNTAIN_LOT_PATTERN.matcher(jibun.trim()).find();
        if (explicitMountain != null && jibunSaysMountain && !explicitMountain) {
            return false;
        }
        if (explicitMountain != null) {
            return explicitMountain.equals(targetMountain);
        }

        // 산지 대상은 응답이 산지임을 스스로 증명해야 한다.
        // 단독·다가구 API처럼 지번이 부분 공개되어 숫자만 오면 선택하지 않는다.
        return targetMountain ? jibunSaysMountain : !jibunSaysMountain;
    }

    private Boolean targetMountainFlag(AnalysisTargetDTO target) {
        String platGbCd = target.platGbCd();
        if (!isBlank(platGbCd)) {
            return parsePlatCode(platGbCd);
        }
        if (isBlank(target.lotAddress())) {
            return null;
        }
        return MOUNTAIN_LOT_PATTERN.matcher(target.lotAddress().trim()).find();
    }

    private Boolean parseLandCode(Object raw) {
        if (raw == null) return null;
        return switch (asText(raw).trim()) {
            case "1", "대지" -> false;
            case "2", "산" -> true;
            default -> null;
        };
    }

    private Boolean parsePlatCode(Object raw) {
        if (raw == null) return null;
        return switch (asText(raw).trim()) {
            case "0", "대지" -> false;
            case "1", "산" -> true;
            default -> null;
        };
    }

    private boolean sameLegalDong(Map<String, Object> item, AnalysisTargetDTO target) {
        String itemLegalDong = firstNonBlank(item, "umdNm", "legalDong");
        if (itemLegalDong == null
                || !normalizeDong(itemLegalDong).equals(normalizeDong(target.legalDongName()))) {
            return false;
        }
        String itemSigunguCode = firstNonBlank(item, "sggCd");
        if (itemSigunguCode != null
                && !itemSigunguCode.equals(target.sigunguCode().trim())) {
            return false;
        }
        String itemDongCode = firstNonBlank(item, "umdCd", "bjdongCode");
        return itemDongCode == null || isBlank(target.bjdongCode())
                || itemDongCode.equals(target.bjdongCode().trim());
    }

    private int[] parseItemLotNumbers(Map<String, Object> item, String jibun) {
        int[] displayed = parseJibun(jibun);
        Object rawMain = firstPresent(item, "bonbun", "mainNo");
        if (rawMain == null) {
            return displayed;
        }

        Integer codedMain = parseToInt(asText(rawMain));
        Object rawSub = firstPresent(item, "bubun", "subNo");
        Integer codedSub = rawSub == null ? 0 : parseToInt(asText(rawSub));
        if (codedMain == null || codedMain <= 0 || codedSub == null || codedSub < 0) {
            return null;
        }
        if (displayed != null
                && (displayed[0] != codedMain || displayed[1] != codedSub)) {
            return null;
        }
        return new int[]{codedMain, codedSub};
    }

    private String normalizeDong(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }

    private boolean sameArea(
            Map<String, Object> item,
            String buildingType,
            BigDecimal targetArea
    ) {
        Object rawArea = switch (buildingType) {
            case "SINGLE_FAMILY", "MULTI_FAMILY" -> firstPresent(
                    item,
                    "totalFloorAr",
                    "buildingArea",
                    "buildArea"
            );
            case "APARTMENT", "MULTI_HOUSEHOLD", "OFFICETEL" -> item.get("excluUseAr");
            default -> null;
        };
        BigDecimal itemArea = parseArea(rawArea);
        if (itemArea == null) {
            return false;
        }
        return itemArea.subtract(targetArea).abs()
                .compareTo(AREA_TOLERANCE_SQM) <= 0;
    }

    private boolean sameFloor(
            Map<String, Object> item,
            String buildingType,
            Integer targetFloor
    ) {
        if (!requiresFloor(buildingType)) {
            return true;
        }
        Integer itemFloor = parseToInt(asText(item.get("floor")));
        return itemFloor != null && itemFloor.equals(targetFloor);
    }

    private boolean requiresFloor(String buildingType) {
        return "APARTMENT".equals(buildingType)
                || "MULTI_HOUSEHOLD".equals(buildingType)
                || "OFFICETEL".equals(buildingType);
    }

    private Object firstPresent(Map<String, Object> item, String... keys) {
        for (String key : keys) {
            Object value = item.get(key);
            if (value != null && !asText(value).isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(Map<String, Object> item, String... keys) {
        Object value = firstPresent(item, keys);
        return value == null ? null : asText(value).trim();
    }

    private BigDecimal parseArea(Object raw) {
        if (raw == null) return null;
        try {
            return new BigDecimal(asText(raw).replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isCancelled(Map<String, Object> item) {
        String cancellationType = asText(item.get("cdealType"));
        String cancellationDay = asText(item.get("cdealDay"));
        return (cancellationType != null && !cancellationType.isBlank())
                || (cancellationDay != null && !cancellationDay.isBlank());
    }

    private LocalDate parseDealDate(Map<String, Object> item, String requestedDealYmd) {
        String year = firstNonBlank(item, "dealYear");
        String month = firstNonBlank(item, "dealMonth");
        String day = firstNonBlank(item, "dealDay");
        if (day == null) {
            log.warn("계약일자가 없어 최신 거래를 판단할 수 없습니다: {}", item);
            return null;
        }

        if (year == null) year = requestedDealYmd.substring(0, 4);
        if (month == null) month = requestedDealYmd.substring(4, 6);
        try {
            LocalDate parsed = LocalDate.of(
                    Integer.parseInt(year),
                    Integer.parseInt(month),
                    Integer.parseInt(day)
            );
            YearMonth requestedMonth = YearMonth.of(
                    Integer.parseInt(requestedDealYmd.substring(0, 4)),
                    Integer.parseInt(requestedDealYmd.substring(4, 6))
            );
            if (!YearMonth.from(parsed).equals(requestedMonth)) {
                log.warn("요청 거래월과 응답 계약월이 다릅니다: requested={}, actual={}",
                        requestedDealYmd, YearMonth.from(parsed));
                return null;
            }
            return parsed;
        } catch (NumberFormatException | DateTimeException e) {
            log.warn("계약일자 파싱 실패: year={}, month={}, day={}", year, month, day);
            return null;
        }
    }

    private record TradeCandidate(LocalDate dealDate, Long amount) {
    }

    private static final class AmbiguousTradeException extends RuntimeException {
    }

    public static class PriceLookupException extends RuntimeException {
        private PriceLookupException(String message) {
            super(message);
        }

        private PriceLookupException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final class PriceLookupFailedException extends PriceLookupException {
        private PriceLookupFailedException(String message) {
            super(message);
        }

        private PriceLookupFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** "178-84" -> [178, 84], "75" -> [75, 0] */
    private int[] parseJibun(String jibun) {
        String normalized = jibun.trim().replaceFirst("^산\\s*", "");
        String[] parts = normalized.split("-", -1);
        if (parts.length > 2 || parts[0].isBlank()
                || (parts.length == 2 && parts[1].isBlank())) {
            return null;
        }
        int main = parseIntSafe(parts[0]);
        int sub = parts.length > 1 ? parseIntSafe(parts[1]) : 0;
        if (main <= 0 || sub < 0) return null;
        return new int[]{main, sub};
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    /** "0178" -> 178, null/blank -> null */
    private Integer parseToInt(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** "12,000"(만원, 콤마포함) -> 120000000L(원) */
    private Long parseDealAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            long manwon = Long.parseLong(raw.replace(",", "").trim());
            if (manwon <= 0L) return null;
            return Math.multiplyExact(manwon, 10_000L);
        } catch (NumberFormatException | ArithmeticException e) {
            log.warn("거래금액 파싱 실패: {}", raw);
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
