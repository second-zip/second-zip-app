package com.secondzip.backend.account.dto.request;

import com.secondzip.backend.account.enums.CharacterType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateCharacterDTO {

    @ApiModelProperty(value = "변경할 캐릭터 유형", example = "MAN", allowableValues = "MSN, WOMAN, CAT", required = true)
    private CharacterType characterType;
}