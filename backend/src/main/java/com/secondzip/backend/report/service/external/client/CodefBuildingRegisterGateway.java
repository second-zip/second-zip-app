package com.secondzip.backend.report.service.external.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.AnalysisSelectionOption;
import com.secondzip.backend.report.dto.AnalysisWorkflowState;
import com.secondzip.backend.report.dto.CodefTwoWayState;
import com.secondzip.backend.report.dto.request.ContinueAnalysisAuthRequest;
import com.secondzip.backend.report.dto.request.StartAnalysisAuthRequest;
import com.secondzip.backend.report.enums.AnalysisNextAction;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import com.secondzip.backend.report.enums.SimpleAuthProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.secondzip.backend.report.service.external.RealApiCondition;
import org.springframework.context.annotation.Conditional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Conditional(RealApiCondition.class)
public class CodefBuildingRegisterGateway implements BuildingRegisterGateway {

    private static final Map<BuildingRegisterDocumentType, String> ENDPOINTS = Map.of(
            BuildingRegisterDocumentType.GENERAL,
            "/v1/kr/public/mw/building-register/general",
            BuildingRegisterDocumentType.COLLECTIVE_TITLE,
            "/v1/kr/public/mw/building-register/set",
            BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE,
            "/v1/kr/public/mw/multiowned-buildings/possession-ledger"
    );

    private final RestTemplate restTemplate;
    private final CodefTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    @Value("${CODEF_BUILDING_REGISTER_ENABLED:false}")
    private boolean enabled;

    @Value("${CODEF_BUILDING_REGISTER_BASE_URL:https://development.codef.io}")
    private String baseUrl;

    public CodefBuildingRegisterGateway(
            @Qualifier("codefRestTemplate") RestTemplate restTemplate,
            CodefTokenProvider tokenProvider,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public BuildingRegisterGatewayResult start(
            AnalysisWorkflowState state,
            BuildingRegisterDocumentType documentType,
            StartAnalysisAuthRequest authRequest
    ) {
        if (!enabled) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "CODEF 건축물대장 연동이 아직 활성화되지 않았습니다."
            );
        }
        validateAuthRequest(authRequest);

        String token = tokenProvider.getToken();
        if (token == null) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "CODEF 인증 토큰을 발급받지 못했습니다."
            );
        }

        Map<String, Object> requestBody =
                buildStartRequest(state, documentType, authRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + ENDPOINTS.get(documentType),
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );
            return parseResponse(response.getBody());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "CODEF 건축물대장 인증 요청에 실패했습니다."
            );
        }
    }

    @Override
    public BuildingRegisterGatewayResult continueRequest(
            AnalysisWorkflowState state,
            ContinueAnalysisAuthRequest request
    ) {
        if (!enabled) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "CODEF 건축물대장 연동이 아직 활성화되지 않았습니다."
            );
        }
        if (state.getPendingDocument() == null || state.getTwoWayState() == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "계속 진행할 CODEF 추가인증 정보가 없습니다."
            );
        }
        validateAuthRequest(request.getAuthentication());

        String token = tokenProvider.getToken();
        if (token == null) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "CODEF 인증 토큰을 발급받지 못했습니다."
            );
        }

        Map<String, Object> requestBody = buildStartRequest(
                state,
                state.getPendingDocument(),
                request.getAuthentication()
        );
        applyTwoWayInput(requestBody, state, request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + ENDPOINTS.get(state.getPendingDocument()),
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );
            return parseResponse(response.getBody());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "CODEF 건축물대장 추가인증 요청에 실패했습니다."
            );
        }
    }

    private void validateAuthRequest(StartAnalysisAuthRequest request) {
        if (request.getProvider() == SimpleAuthProvider.PASS
                && request.getTelecom() == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "PASS 인증은 통신사 선택이 필요합니다."
            );
        }
    }

    private Map<String, Object> buildStartRequest(
            AnalysisWorkflowState state,
            BuildingRegisterDocumentType documentType,
            StartAnalysisAuthRequest auth
    ) {
        AnalysisTarget target = state.getTarget();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("organization", "0001");
        body.put("loginType", "5");
        body.put("userName", auth.getUserName());
        body.put("identity", auth.getBirthDate());
        body.put("loginTypeLevel", auth.getProvider().getCode());
        body.put("phoneNo", auth.getPhoneNo());
        body.put("telecom", auth.getTelecom() != null ? auth.getTelecom().getCode() : "");
        body.put("address", buildCodefRoadAddress(target));
        body.put("dong", extractDetailPart(state.getDetailAddress(), "동"));
        if (documentType == BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE) {
            body.put("ho", extractDetailPart(state.getDetailAddress(), "호"));
        }
        body.put("type", "0");
        body.put("originDataYN", "0");
        return body;
    }

    private void applyTwoWayInput(
            Map<String, Object> body,
            AnalysisWorkflowState state,
            ContinueAnalysisAuthRequest request
    ) {
        switch (state.getNextAction()) {
            case SIMPLE_AUTH -> body.put("simpleAuth", "1");
            case ADDRESS_SELECTION ->
                    body.put("reqAddress", requiredSelection(request));
            case DONG_SELECTION ->
                    body.put("dongNum", requiredSelection(request));
            case HO_SELECTION ->
                    body.put("hoNum", requiredSelection(request));
            case CAPTCHA -> {
                if (request.getSecureNo() == null || request.getSecureNo().isBlank()) {
                    throw new BusinessException(
                            ErrorCode.INVALID_REQUEST,
                            "보안문자 입력이 필요합니다."
                    );
                }
                body.put("secureNo", request.getSecureNo());
                body.put("secureNoRefresh", "0");
            }
            default -> throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "처리할 추가인증 단계가 없습니다."
            );
        }
        CodefTwoWayState twoWay = state.getTwoWayState();
        if (twoWay == null
                || twoWay.getJobIndex() == null
                || twoWay.getThreadIndex() == null
                || twoWay.getJti() == null
                || twoWay.getJti().isBlank()
                || twoWay.getTwoWayTimestamp() == null
                || twoWay.getTwoWayTimestamp().isBlank()) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "추가인증 연결 정보가 없거나 만료되었습니다. 인증을 다시 시작해주세요."
            );
        }
        body.put("is2Way", true);
        Map<String, Object> twoWayInfo = new LinkedHashMap<>();
        twoWayInfo.put("jobIndex", twoWay.getJobIndex());
        twoWayInfo.put("threadIndex", twoWay.getThreadIndex());
        twoWayInfo.put("jti", twoWay.getJti());
        twoWayInfo.put("twoWayTimestamp", twoWay.getTwoWayTimestamp());
        body.put("twoWayInfo", twoWayInfo);
    }

    private String requiredSelection(ContinueAnalysisAuthRequest request) {
        if (request.getSelectionValue() == null
                || request.getSelectionValue().isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "주소·동·호 선택값이 필요합니다."
            );
        }
        return request.getSelectionValue();
    }

    private String buildCodefRoadAddress(AnalysisTarget target) {
        String roadAddress = target.roadAddress();
        String[] tokens = roadAddress == null
                ? new String[0]
                : roadAddress.trim().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].endsWith("로") || tokens[i].endsWith("길")) {
                String number = target.roadBuildingMainNo();
                if (target.roadBuildingSubNo() != null
                        && !target.roadBuildingSubNo().isBlank()
                        && !"0".equals(target.roadBuildingSubNo())) {
                    number += "-" + target.roadBuildingSubNo();
                }
                return tokens[i] + " " + number;
            }
        }
        throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "CODEF 요청용 도로명주소를 만들지 못했습니다."
        );
    }

    private String extractDetailPart(String detailAddress, String suffix) {
        if (detailAddress == null || detailAddress.isBlank()) {
            return "";
        }
        Matcher matcher = Pattern.compile("([^\\s]+)\\s*" + suffix)
                .matcher(detailAddress);
        return matcher.find() ? matcher.group(1) : "";
    }

    @SuppressWarnings("unchecked")
    private BuildingRegisterGatewayResult parseResponse(String rawBody) throws Exception {
        if (rawBody == null || rawBody.isBlank()) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "CODEF 건축물대장 응답이 비어있습니다."
            );
        }

        Map<String, Object> body;
        try {
            body = objectMapper.readValue(rawBody, Map.class);
        } catch (Exception plainJsonFailure) {
            body = objectMapper.readValue(
                    URLDecoder.decode(rawBody, StandardCharsets.UTF_8),
                    Map.class
            );
        }

        Map<String, Object> result = asMap(body.get("result"));
        String code = result != null && result.get("code") != null
                ? result.get("code").toString()
                : null;
        Map<String, Object> data = asMap(body.get("data"));

        if ("CF-03002".equals(code) && data != null) {
            Map<String, Object> extraInfo = asMap(data.get("extraInfo"));
            return new BuildingRegisterGatewayResult(
                    false,
                    resolveNextAction(extraInfo),
                    new CodefTwoWayState(
                            asInteger(data.get("jobIndex")),
                            asInteger(data.get("threadIndex")),
                            asString(data.get("jti")),
                            asString(data.get("twoWayTimestamp"))
                    ),
                    extractSelectionOptions(extraInfo),
                    extractCaptchaImage(extraInfo),
                    null
            );
        }
        if (code != null && code.startsWith("CF-0000") && data != null) {
            return new BuildingRegisterGatewayResult(
                    true,
                    AnalysisNextAction.NONE,
                    null,
                    List.of(),
                    null,
                    data
            );
        }
        throw new BusinessException(
                ErrorCode.EXTERNAL_API_ERROR,
                "CODEF 건축물대장 응답 오류: " + (code != null ? code : "UNKNOWN")
        );
    }

    private AnalysisNextAction resolveNextAction(Map<String, Object> extraInfo) {
        if (extraInfo == null) return AnalysisNextAction.NONE;
        if (hasValues(extraInfo.get("reqAddrList"))) return AnalysisNextAction.ADDRESS_SELECTION;
        if (hasValues(extraInfo.get("reqDongNumList"))) return AnalysisNextAction.DONG_SELECTION;
        if (hasValues(extraInfo.get("reqHoNumList"))) return AnalysisNextAction.HO_SELECTION;
        if (hasText(extraInfo.get("reqSecureNo"))) return AnalysisNextAction.CAPTCHA;
        return AnalysisNextAction.SIMPLE_AUTH;
    }

    private List<AnalysisSelectionOption> extractSelectionOptions(
            Map<String, Object> extraInfo
    ) {
        if (extraInfo == null) return List.of();
        List<AnalysisSelectionOption> options = new ArrayList<>();
        addOptions(options, extraInfo.get("reqAddrList"), "reqAddress", "reqAddress");
        addOptions(options, extraInfo.get("reqDongNumList"), "commDongNum", "reqDong");
        addOptions(options, extraInfo.get("reqHoNumList"), "commHoNum", "reqHo");
        return options;
    }

    private String extractCaptchaImage(Map<String, Object> extraInfo) {
        if (extraInfo == null) return null;
        return asString(extraInfo.get("reqSecureNo"));
    }

    private void addOptions(
            List<AnalysisSelectionOption> destination,
            Object source,
            String valueKey,
            String labelKey
    ) {
        if (!(source instanceof Collection<?> collection)) return;
        for (Object item : collection) {
            Map<String, Object> map = asMap(item);
            if (map == null) continue;
            String value = asString(map.get(valueKey));
            String label = asString(map.get(labelKey));
            if (value != null && !value.isBlank()) {
                destination.add(new AnalysisSelectionOption(
                        value,
                        label != null && !label.isBlank() ? label : value
                ));
            }
        }
    }

    private boolean hasValues(Object value) {
        return value instanceof java.util.Collection<?> collection
                && !collection.isEmpty();
    }

    private boolean hasText(Object value) {
        return value != null && !value.toString().isBlank();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : null;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return null;
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
