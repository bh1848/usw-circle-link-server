package com.USWCicrcleLink.server.admin.admin.mapper;

import com.USWCicrcleLink.server.club.dto.ClubCategoryResponse;
import com.USWCicrcleLink.server.club.domain.ClubCategory;

import java.util.List;
import java.util.stream.Collectors;

public class ClubCategoryMapper {

    // 단일 객체 변환 (Entity → DTO)
    public static ClubCategoryResponse toDto(ClubCategory clubCategory) {
        return new ClubCategoryResponse(
                clubCategory.getClubCategoryUUID(),
                clubCategory.getClubCategoryName()
        );
    }

    // 리스트 변환 (List<Entity> → List<DTO>)
    public static List<ClubCategoryResponse> toDtoList(List<ClubCategory> categories) {
        return categories.stream()
                .map(ClubCategoryMapper::toDto)
                .collect(Collectors.toList());
    }
}
