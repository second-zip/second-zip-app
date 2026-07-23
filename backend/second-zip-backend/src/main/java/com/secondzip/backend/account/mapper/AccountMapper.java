package com.secondzip.backend.account.mapper;

import com.secondzip.backend.account.domain.AccountVO;
import com.secondzip.backend.account.dto.request.UpdateAccountDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {

    int countByEmail(String email);

    int countByNickname(String nickname);

    int insert(AccountVO account);

    AccountVO findByEmail(String email);

    AccountVO findById(Long accountId);

    int countByNicknameExcludingAccount(@Param("nickname") String nickname, @Param("accountId") Long accountId);

    int updateAccount(@Param("accountId") Long accountId,@Param("updateDTO") UpdateAccountDTO updateDTO);
}