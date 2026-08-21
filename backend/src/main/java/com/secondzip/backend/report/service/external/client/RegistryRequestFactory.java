package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisTargetDTO;
import com.secondzip.backend.report.enums.RegistryDocumentType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RegistryRequestFactory {

    public Map<String, Object> create(
            AnalysisTargetDTO target,
            RegistryDocumentType documentType,
            String detailAddress,
            String loginPhoneNo,
            String encryptedPassword,
            String ePrepayNo,
            String ePrepayPass
    ) {
        if (target == null || documentType == null) {
            throw new IllegalArgumentException("등기부 조회 대상과 문서 종류가 필요합니다.");
        }

        Map<String, Object> body = commonBody(
                documentType,
                loginPhoneNo,
                encryptedPassword,
                ePrepayNo,
                ePrepayPass
        );
        if (documentType == RegistryDocumentType.LAND) {
            applyLandAddress(body, target);
        } else {
            applyRoadAddress(body, target);
            if (documentType == RegistryDocumentType.COLLECTIVE) {
                applyDongHo(body, detailAddress);
            }
        }
        return body;
    }

    private Map<String, Object> commonBody(
            RegistryDocumentType documentType,
            String loginPhoneNo,
            String encryptedPassword,
            String ePrepayNo,
            String ePrepayPass
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("organization", "0002");
        body.put("phoneNo", require(loginPhoneNo, "CODEF 로그인 전화번호"));
        body.put("password", require(encryptedPassword, "암호화된 CODEF 비밀번호"));
        body.put("inquiryType", documentType.inquiryType());
        body.put("realtyType", documentType.realtyType());
        body.put("ePrepayNo", require(ePrepayNo, "전자민원캐시 번호"));
        body.put("ePrepayPass", require(ePrepayPass, "전자민원캐시 비밀번호"));
        body.put("issueType", "1");
        body.put("registerSummaryYN", "1");
        body.put("applicationType", "1");
        body.put("jointMortgageJeonseYN", "1");
        return body;
    }

    private void applyRoadAddress(
            Map<String, Object> body,
            AnalysisTargetDTO target
    ) {
        RegistryAddressParts parsed =
                RegistryAddressParts.fromRoadAddress(target.roadAddress());
        body.put("addr_sido", parsed.sido());
        body.put("addr_sigungu", parsed.sigungu());
        body.put("addr_roadName", parsed.roadName());
        body.put(
                "addr_buildingNumber",
                joinNumber(
                        target.roadBuildingMainNo(),
                        target.roadBuildingSubNo(),
                        "도로명 건물번호"
                )
        );
    }

    private void applyLandAddress(
            Map<String, Object> body,
            AnalysisTargetDTO target
    ) {
        RegistryAddressParts parsed =
                RegistryAddressParts.fromRoadAddress(target.roadAddress());
        body.put("addr_sido", parsed.sido());
        body.put("addr_dong", require(target.legalDongName(), "법정동명"));
        body.put(
                "addr_lotNumber",
                landLotNumber(target)
        );
    }

    private String landLotNumber(AnalysisTargetDTO target) {
        String lotNumber = joinNumber(target.mainNo(), target.subNo(), "지번");
        String platGbCd = target.platGbCd();
        if (platGbCd == null || platGbCd.isBlank() || "0".equals(platGbCd)) {
            return lotNumber;
        }
        if ("1".equals(platGbCd)) {
            return lotNumber.startsWith("산") ? lotNumber : "산" + lotNumber;
        }
        throw new IllegalArgumentException(
                "지원하지 않는 대지구분코드입니다: " + platGbCd
        );
    }

    private void applyDongHo(Map<String, Object> body, String detailAddress) {
        DongHo dongHo = DongHo.parse(detailAddress);
        if (dongHo.ho() == null) {
            throw new IllegalArgumentException("집합건물 등기부 조회에는 호수가 필요합니다.");
        }
        body.put("dong", dongHo.dong() == null ? "" : dongHo.dong());
        body.put("ho", dongHo.ho() == null ? "" : dongHo.ho());
    }

    private String joinNumber(String mainNo, String subNo, String label) {
        String main = require(mainNo, label);
        return subNo == null || subNo.isBlank() || "0".equals(subNo)
                ? main
                : main + "-" + subNo;
    }

    private String require(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + "이(가) 필요합니다.");
        }
        return value;
    }

    record RegistryAddressParts(String sido, String sigungu, String roadName) {
        private static final Map<String, String> SIDO_FULL_NAME = Map.ofEntries(
                Map.entry("서울", "서울특별시"),
                Map.entry("부산", "부산광역시"),
                Map.entry("대구", "대구광역시"),
                Map.entry("인천", "인천광역시"),
                Map.entry("광주", "광주광역시"),
                Map.entry("대전", "대전광역시"),
                Map.entry("울산", "울산광역시"),
                Map.entry("세종", "세종특별자치시"),
                Map.entry("경기", "경기도"),
                Map.entry("강원", "강원특별자치도"),
                Map.entry("충북", "충청북도"),
                Map.entry("충남", "충청남도"),
                Map.entry("전북", "전북특별자치도"),
                Map.entry("전남", "전라남도"),
                Map.entry("경북", "경상북도"),
                Map.entry("경남", "경상남도"),
                Map.entry("제주", "제주특별자치도")
        );

        static RegistryAddressParts fromRoadAddress(String roadAddress) {
            if (roadAddress == null || roadAddress.isBlank()) {
                throw new IllegalArgumentException("도로명주소가 필요합니다.");
            }
            String[] tokens = roadAddress.trim().split("\\s+");
            int roadIndex = -1;
            for (int i = 1; i < tokens.length; i++) {
                if (tokens[i].endsWith("로") || tokens[i].endsWith("길")) {
                    roadIndex = i;
                    break;
                }
            }
            if (roadIndex < 1 || roadIndex >= tokens.length - 1) {
                throw new IllegalArgumentException(
                        "도로명주소에서 시도·시군구·도로명을 분리하지 못했습니다."
                );
            }
            String sido = SIDO_FULL_NAME.getOrDefault(tokens[0], tokens[0]);
            if (roadIndex == 1 && !"세종특별자치시".equals(sido)) {
                throw new IllegalArgumentException(
                        "도로명주소에서 시도·시군구·도로명을 분리하지 못했습니다."
                );
            }
            String sigungu = roadIndex == 1
                    ? ""
                    : String.join(
                            " ",
                            java.util.Arrays.copyOfRange(tokens, 1, roadIndex)
                    );
            return new RegistryAddressParts(sido, sigungu, tokens[roadIndex]);
        }
    }

    record DongHo(String dong, String ho) {
        private static final String TOKEN = "([^\\s,()]+?)\\s*";

        static DongHo parse(String detailAddress) {
            if (detailAddress == null || detailAddress.isBlank()) {
                return new DongHo(null, null);
            }
            return new DongHo(
                    lastGroup(detailAddress, TOKEN + "동"),
                    lastGroup(detailAddress, TOKEN + "호")
            );
        }

        private static String lastGroup(String input, String regex) {
            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern.compile(regex).matcher(input);
            String result = null;
            while (matcher.find()) {
                result = matcher.group(1);
            }
            return result;
        }
    }
}
