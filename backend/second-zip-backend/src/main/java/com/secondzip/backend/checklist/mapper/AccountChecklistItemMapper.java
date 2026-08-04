package com.secondzip.backend.checklist.mapper;

import com.secondzip.backend.checklist.dto.response.ChecklistResponseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccountChecklistItemMapper {

    List<ChecklistResponseDTO> findByAccountIdAndCategory(
            @Param("accountId") Long accountId,
            @Param("category") String category
    );

    int insertChecked(
            @Param("accountId") Long accountId,
            @Param("checklistItemId") Long checklistItemId
    );

    int delete(
            @Param("accountId") Long accountId,
            @Param("checklistItemId") Long checklistItemId
    );

    int deleteAllByAccountId(
            @Param("accountId") Long accountId
    );
}