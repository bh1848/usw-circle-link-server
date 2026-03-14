package com.USWCicrcleLink.server.admin.admin.service;

import com.USWCicrcleLink.server.admin.admin.dto.AdminClubCategoryCreationRequest;
import com.USWCicrcleLink.server.admin.admin.mapper.ClubCategoryMapper;
import com.USWCicrcleLink.server.club.dto.ClubCategoryResponse;
import com.USWCicrcleLink.server.club.domain.ClubCategory;
import com.USWCicrcleLink.server.club.repository.ClubCategoryMappingRepository;
import com.USWCicrcleLink.server.club.repository.ClubCategoryRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.BaseException;
import com.USWCicrcleLink.server.global.exception.errortype.ClubCategoryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminClubCategoryService {

    private final ClubCategoryRepository clubCategoryRepository;
    private final ClubCategoryMappingRepository clubCategoryMappingRepository;

    /**
     * 동아리 카테고리 설정(ADMIN, LEADER) - 카테고리 조회
     */
    @Transactional(readOnly = true)
    public List<ClubCategoryResponse> getAllClubCategories() {
        List<ClubCategory> clubCategories = clubCategoryRepository.findAll();
        log.debug("동아리 카테고리 조회 성공 - {}개 카테고리 반환", clubCategories.size());

        return ClubCategoryMapper.toDtoList(clubCategories);
    }

    /**
     * 동아리 카테고리 설정(ADMIN) - 카테고리 추가
     */
    public ClubCategoryResponse addClubCategory(AdminClubCategoryCreationRequest request) {
        String normalizedCategoryName = request.getClubCategoryName().toLowerCase();

        if (clubCategoryRepository.existsByClubCategoryName(normalizedCategoryName)) {
            throw new ClubCategoryException(ExceptionType.DUPLICATE_CATEGORY);
        }

        ClubCategory clubCategory = ClubCategory.builder()
                .clubCategoryName(normalizedCategoryName)
                .build();

        ClubCategory savedClubCategory = clubCategoryRepository.save(clubCategory);
        log.info("동아리 카테고리 추가 성공 - ID: {}, Name: {}", savedClubCategory.getClubCategoryId(), savedClubCategory.getClubCategoryName());

        return ClubCategoryMapper.toDto(savedClubCategory);
    }

    /**
     * 동아리 카테고리 설정(ADMIN) - 카테고리 삭제
     */
    public ClubCategoryResponse deleteClubCategory(UUID clubCategoryUUID) {
        ClubCategory clubCategory = clubCategoryRepository.findByClubCategoryUUID(clubCategoryUUID)
                .orElseThrow(() -> new ClubCategoryException(ExceptionType.CATEGORY_NOT_FOUND));

        try {
            clubCategoryMappingRepository.deleteByClubCategoryId(clubCategory.getClubCategoryId());
            clubCategoryRepository.delete(clubCategory);
            log.info("동아리 카테고리 삭제 성공 - ID: {}", clubCategory.getClubCategoryId());
        } catch (Exception e) {
            log.error("카테고리 삭제 중 예외 발생 - UUID: {}, 오류: {}", clubCategoryUUID, e.getMessage());
            throw new BaseException(ExceptionType.SERVER_ERROR, e);
        }

        return ClubCategoryMapper.toDto(clubCategory);
    }

}
