package com.secondzip.backend.account.dto.request;

import com.secondzip.backend.account.enums.CharacterType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupDTO {

    private String email;
    private String password;
    private String nickname;
    private CharacterType characterType;
}