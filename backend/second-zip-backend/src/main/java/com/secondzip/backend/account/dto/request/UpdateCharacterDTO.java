package com.secondzip.backend.account.dto.request;

import com.secondzip.backend.account.enums.CharacterType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
public class UpdateCharacterDTO {

    @ApiModelProperty(value = "변경할 캐릭터 유형", example = "MAN", allowableValues = "MAN, WOMAN, CAT", required = true)
    @NotNull(message = "캐릭터 종류는 필수입니다.")
    private CharacterType characterType;
}