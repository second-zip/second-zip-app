package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;

import java.util.List;

public final class BuildingRegisterDocumentSelector {

    private BuildingRegisterDocumentSelector() {
    }

    public static List<BuildingRegisterDocumentType> select(String buildingType) {
        if ("SINGLE_FAMILY".equals(buildingType) || "MULTI_FAMILY".equals(buildingType)) {
            return List.of(BuildingRegisterDocumentType.GENERAL);
        }
        if ("APARTMENT".equals(buildingType)
                || "MULTI_HOUSEHOLD".equals(buildingType)
                || "OFFICETEL".equals(buildingType)) {
            return List.of(
                    BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                    BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE
            );
        }
        throw new BusinessException(
                ErrorCode.EXTERNAL_API_ERROR,
                "건축물 유형을 확인할 수 없습니다."
        );
    }
}
