package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.report.dto.response.ExternalApiReadinessResponse;
import com.secondzip.backend.report.service.external.ExternalApiModeResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExternalApiReadinessService {

    @Value("${KAKAO_REST_API_KEY:}")
    private String kakaoKey;

    @Value("${BUILDING_HUB_API_KEY:}")
    private String buildingHubKey;

    @Value("${REALTY_PRICE_API_KEY:}")
    private String realtyPriceKey;

    @Value("${CODEF_CLIENT_ID:}")
    private String codefClientId;

    @Value("${CODEF_CLIENT_SECRET:}")
    private String codefClientSecret;

    @Value("${CODEF_PUBLIC_KEY:}")
    private String codefPublicKey;

    @Value("${CODEF_LOGIN_PHONE_NO:}")
    private String codefLoginPhoneNo;

    @Value("${CODEF_LOGIN_PASSWORD:}")
    private String codefLoginPassword;

    @Value("${CODEF_EPREPAY_NO:}")
    private String codefPrepayNo;

    @Value("${CODEF_EPREPAY_PASS:}")
    private String codefPrepayPass;

    @Value("${CODEF_REGISTRY_ENABLED:false}")
    private boolean registryEnabled;

    @Value("${CODEF_BUILDING_REGISTER_ENABLED:false}")
    private boolean buildingRegisterEnabled;

    @Value("${CODEF_REGISTRY_BASE_URL:https://development.codef.io}")
    private String registryBaseUrl;

    @Value("${CODEF_BUILDING_REGISTER_BASE_URL:https://development.codef.io}")
    private String buildingRegisterBaseUrl;

    public ExternalApiReadinessResponse check() {
        List<String> missing = new ArrayList<>();
        require(missing, "KAKAO_REST_API_KEY", kakaoKey);
        require(missing, "BUILDING_HUB_API_KEY", buildingHubKey);
        require(missing, "REALTY_PRICE_API_KEY", realtyPriceKey);
        require(missing, "CODEF_CLIENT_ID", codefClientId);
        require(missing, "CODEF_CLIENT_SECRET", codefClientSecret);
        require(missing, "CODEF_PUBLIC_KEY", codefPublicKey);
        require(missing, "CODEF_LOGIN_PHONE_NO", codefLoginPhoneNo);
        require(missing, "CODEF_LOGIN_PASSWORD", codefLoginPassword);
        require(missing, "CODEF_EPREPAY_NO", codefPrepayNo);
        require(missing, "CODEF_EPREPAY_PASS", codefPrepayPass);

        List<String> warnings = new ArrayList<>();
        if (!registryEnabled) {
            warnings.add("CODEF 등기부 조회가 비활성화되어 있습니다.");
        }
        if (!buildingRegisterEnabled) {
            warnings.add("CODEF 건축물대장 조회가 비활성화되어 있습니다.");
        }
        if (!isDemoUrl(registryBaseUrl)) {
            warnings.add("등기부 URL이 CODEF 데모 서버가 아닙니다.");
        }
        if (!isDemoUrl(buildingRegisterBaseUrl)) {
            warnings.add("건축물대장 URL이 CODEF 데모 서버가 아닙니다.");
        }

        String mode = ExternalApiModeResolver.resolve();
        if (!"real".equals(mode)) {
            warnings.add("EXTERNAL_API_MODE가 'real'이 아닙니다 (현재: " + mode + ").");
        }

        return new ExternalApiReadinessResponse(
                missing.isEmpty() && warnings.isEmpty(),
                isDemoUrl(registryBaseUrl)
                        && isDemoUrl(buildingRegisterBaseUrl)
                        ? "DEMO"
                        : "CUSTOM",
                List.copyOf(missing),
                List.copyOf(warnings)
        );
    }

    private void require(
            List<String> missing,
            String key,
            String value
    ) {
        if (value == null || value.isBlank()) {
            missing.add(key);
        }
    }

    private boolean isDemoUrl(String url) {
        return url != null
                && url.startsWith("https://development.codef.io");
    }
}
