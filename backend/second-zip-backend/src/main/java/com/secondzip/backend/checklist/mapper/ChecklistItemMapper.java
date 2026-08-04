package com.secondzip.backend.checklist.mapper;

import com.secondzip.backend.record.dto.request.ChecklistItemInput;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChecklistItemMapper {

    List<ChecklistItemInput> findByCategory(
            @Param("category") String category
    );
}