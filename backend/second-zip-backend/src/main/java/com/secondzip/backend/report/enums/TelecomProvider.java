package com.secondzip.backend.report.enums;

public enum TelecomProvider {
    SKT("0"),
    KT("1"),
    LG_U_PLUS("2");

    private final String code;

    TelecomProvider(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
