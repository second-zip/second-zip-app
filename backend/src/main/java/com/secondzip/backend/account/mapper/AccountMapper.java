package com.secondzip.backend.account.mapper;

import com.secondzip.backend.account.domain.Account;
import com.secondzip.backend.account.dto.request.UpdateAccountDTO;
import com.secondzip.backend.account.dto.response.ActivitySummaryDTO;
import com.secondzip.backend.account.enums.CharacterType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {

    int countByEmail(String email);

    int countByNickname(String nickname);

    int insert(Account account);

    Account findByEmail(String email);

    Account findById(Long accountId);

    int countByNicknameExcludingAccount(@Param("nickname") String nickname, @Param("accountId") Long accountId);

    int updateAccount(@Param("accountId") Long accountId,@Param("updateDTO") UpdateAccountDTO updateDTO);

    int updateCharacterType(@Param("accountId") Long accountId, @Param("characterType") CharacterType characterType);

    int deleteById(Long accountId);

    int updatePassword(@Param("accountId") Long accountId, @Param("password") String password);

    ActivitySummaryDTO findActivitySummaryByAccountId(@Param("accountId") Long accountId);
}