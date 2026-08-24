package com.secondzip.backend.report.service.external;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 등기부등본/실거래가/건축물대장 조회를 real(과금 O)로 할지 mock(과금 X, 항상
 * "이상 없음" 응답)으로 할지 결정한다.
 *
 * 여기서 어떤 이유로든 "mock"으로 폴백되면 사기 위험이 있는 매물도 전부 안전하다고
 * 보고될 수 있다("틀린 정보 > 정보 없음" 원칙의 정반대 사례). 그래서 명시적으로
 * mock을 설정한 경우가 아니라 설정을 못 찾거나 읽는 중 오류가 나서 mock으로
 * 떨어지는 모든 경로에 WARN 로그를 남긴다 — 배포 후 로그만 보고도 "지금 mock으로
 * 돌고 있다"는 사실을 바로 알 수 있게 하기 위함이다. 판단 로직(real/mock 결정
 * 순서) 자체는 바꾸지 않는다.
 */
@Slf4j
public class ExternalApiModeResolver {

    public static String resolve() {
        String systemProperty = System.getProperty("EXTERNAL_API_MODE");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty.trim().toLowerCase();
        }

        String environmentVariable = System.getenv("EXTERNAL_API_MODE");
        if (environmentVariable != null && !environmentVariable.isBlank()) {
            return environmentVariable.trim().toLowerCase();
        }

        String externalConfigPath = System.getProperty("secondzip.config.file");
        if (externalConfigPath == null || externalConfigPath.isBlank()) {
            externalConfigPath = System.getenv("SECONDZIP_CONFIG_FILE");
        }
        if (externalConfigPath == null || externalConfigPath.isBlank()) {
            externalConfigPath = "src/main/resources/.env";
        }

        Path configPath = Path.of(externalConfigPath);
        if (Files.isRegularFile(configPath)) {
            try (InputStream is = Files.newInputStream(configPath)) {
                return readMode(is, "설정 파일(" + configPath.toAbsolutePath() + ")");
            } catch (Exception e) {
                // 클래스패스 설정으로 한 번 더 시도하되, 원인은 남긴다 — 그렇지
                // 않으면 왜 mock으로 도는지 아무 흔적도 없이 조용히 넘어가 버린다.
                log.warn(
                        "EXTERNAL_API_MODE 설정 파일을 읽는 중 오류가 발생해 classpath .env로 "
                                + "대체 시도합니다: path={}, error={}",
                        configPath.toAbsolutePath(), e.toString()
                );
            }
        }

        try (InputStream is = ExternalApiModeResolver.class
                .getClassLoader()
                .getResourceAsStream(".env")) {

            if (is == null) {
                log.warn(
                        "EXTERNAL_API_MODE를 시스템 프로퍼티/환경변수/설정 파일/classpath 어디서도 "
                                + "찾지 못해 mock 모드로 대체합니다. 실서비스 환경이라면 EXTERNAL_API_MODE "
                                + "또는 secondzip.config.file(SECONDZIP_CONFIG_FILE) 설정을 확인하세요. "
                                + "configPath={}",
                        configPath.toAbsolutePath()
                );
                return "mock";
            }
            return readMode(is, "classpath .env");

        } catch (Exception e) {
            log.warn(
                    "classpath .env를 읽는 중 오류가 발생해 mock 모드로 대체합니다: error={}",
                    e.toString()
            );
            return "mock";
        }
    }

    private static String readMode(InputStream is, String source) throws Exception {
        Properties props = new Properties();
        props.load(is);
        String mode = props.getProperty("EXTERNAL_API_MODE");
        if (mode == null || mode.isBlank()) {
            log.warn("{}에 EXTERNAL_API_MODE 설정이 없어 mock 모드로 대체합니다.", source);
            return "mock";
        }
        return mode.trim().toLowerCase();
    }
}
