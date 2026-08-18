package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class BuildingRegisterDocumentSelectorTest {

    @ParameterizedTest
    @ValueSource(strings = {"SINGLE_FAMILY", "MULTI_FAMILY"})
    void detachedHousingUsesGeneralRegister(String buildingType) {
        assertThat(BuildingRegisterDocumentSelector.select(buildingType))
                .containsExactly(BuildingRegisterDocumentType.GENERAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"APARTMENT", "MULTI_HOUSEHOLD", "OFFICETEL"})
    void collectiveHousingUsesTitleAndExclusiveRegisters(String buildingType) {
        assertThat(BuildingRegisterDocumentSelector.select(buildingType))
                .containsExactly(
                        BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                        BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE
                );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "UNKNOWN", "apartment"})
    void unsupportedBuildingTypeReturnsExternalApiError(String buildingType) {
        BusinessException thrown = catchThrowableOfType(
                () -> BuildingRegisterDocumentSelector.select(buildingType),
                BusinessException.class
        );

        assertThat(thrown.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
        assertThat(thrown.getMessage()).contains("건축물 유형");
    }
}
