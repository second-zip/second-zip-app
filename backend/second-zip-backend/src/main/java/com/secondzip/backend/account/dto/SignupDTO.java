package com.secondzip.backend.account.dto;

import com.secondzip.backend.account.enums.CharacterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupDTO {

    private String email;
    private String password;
    private String nickname;
    private CharacterType characterType;
}