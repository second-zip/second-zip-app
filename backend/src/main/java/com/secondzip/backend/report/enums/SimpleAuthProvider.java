package com.secondzip.backend.report.enums;

public enum SimpleAuthProvider {
    KAKAO("1"),
    SAMSUNG_PASS("3"),
    KB_MOBILE("4"),
    PASS("5"),
    NAVER("6"),
    SHINHAN("7"),
    TOSS("8"),
    HANA("9"),
    NH("10");

    private final String code;

    SimpleAuthProvider(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
