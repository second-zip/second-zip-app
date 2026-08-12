package com.secondzip.backend.account.dto.response;

import com.secondzip.backend.account.enums.CharacterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String accessToken;
    private String refreshToken;

    private Long accountId;
    private String email;
    private String nickname;
    private CharacterType characterType;
}