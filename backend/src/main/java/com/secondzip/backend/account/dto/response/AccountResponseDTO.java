package com.secondzip.backend.account.dto.response;

import com.secondzip.backend.account.domain.Account;
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

    public static AccountResponseDTO from(Account account) {
        return AccountResponseDTO.builder()
                .accountId(account.getAccountId())
                .email(account.getEmail())
                .nickname(account.getNickname())
                .characterType(account.getCharacterType())
                .build();
    }
}