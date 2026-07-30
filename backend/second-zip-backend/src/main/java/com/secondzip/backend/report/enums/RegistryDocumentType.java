package com.secondzip.backend.report.enums;

public enum RegistryDocumentType {
    COLLECTIVE("3", "1"),
    LAND("2", "2"),
    BUILDING("3", "3");

    private final String inquiryType;
    private final String realtyType;

    RegistryDocumentType(String inquiryType, String realtyType) {
        this.inquiryType = inquiryType;
        this.realtyType = realtyType;
    }

    public String inquiryType() {
        return inquiryType;
    }

    public String realtyType() {
        return realtyType;
    }
}
