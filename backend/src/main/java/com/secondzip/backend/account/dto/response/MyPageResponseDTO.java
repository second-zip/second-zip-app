package com.secondzip.backend.account.dto.response;

import com.secondzip.backend.account.domain.AccountVO;
import com.secondzip.backend.account.enums.CharacterType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyPageResponseDTO {
    private Long accountId;
    private String email;
    private String nickname;
    private CharacterType characterType;
    private ActivitySummaryDTO activitySummary;

    public static MyPageResponseDTO of(AccountVO account, ActivitySummaryDTO activitySummary) {
        return MyPageResponseDTO.builder()
                .accountId(account.getAccountId())
                .email(account.getEmail())
                .nickname(account.getNickname())
                .characterType(account.getCharacterType())
                .activitySummary(activitySummary)
                .build();
    }
}
