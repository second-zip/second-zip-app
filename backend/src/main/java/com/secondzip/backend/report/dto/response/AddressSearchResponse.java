package com.secondzip.backend.report.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@ApiModel(description = "주소 검색 응답")
public class AddressSearchResponse {

    @ApiModelProperty("주소 후보 목록. 검색 결과가 없으면 빈 배열이다.")
    private List<AddressSearchItem> addresses;
}
