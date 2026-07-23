package com.secondzip.backend.account.dto.request;

import com.secondzip.backend.account.enums.CharacterType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateAccountDTO {

    private String nickname;

    private CharacterType characterType;
}