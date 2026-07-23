package com.secondzip.backend.account.dto.response;

import com.secondzip.backend.account.domain.AccountVO;
import com.secondzip.backend.account.enums.CharacterType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountResponseDTO {

    private Long accountId;
    private String email;
    private String nickname;
    private CharacterType characterType;

    public static AccountResponseDTO from(AccountVO account) {
        return AccountResponseDTO.builder()
                .accountId(account.getAccountId())
                .email(account.getEmail())
                .nickname(account.getNickname())
                .characterType(account.getCharacterType())
                .build();
    }
}