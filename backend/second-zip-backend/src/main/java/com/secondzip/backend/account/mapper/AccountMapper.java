package com.secondzip.backend.account.mapper;

import com.secondzip.backend.account.domain.AccountVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper {

    int countByEmail(String email);

    int countByNickname(String nickname);

    int insert(AccountVO account);

    AccountVO findByEmail(String email);
}