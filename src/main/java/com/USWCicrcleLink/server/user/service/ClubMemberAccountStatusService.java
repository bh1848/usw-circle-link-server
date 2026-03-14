package com.USWCicrcleLink.server.user.service;

import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.ClubMemberAccountStatusException;
import com.USWCicrcleLink.server.user.domain.ExistingMember.ClubMemberAccountStatus;
import com.USWCicrcleLink.server.user.domain.ExistingMember.ClubMemberTemp;
import com.USWCicrcleLink.server.user.dto.ClubDTO;
import com.USWCicrcleLink.server.user.dto.ExistingMemberSignUpRequest;
import com.USWCicrcleLink.server.user.repository.ClubMemberAccountStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ClubMemberAccountStatusService {
    private final ClubMemberAccountStatusRepository clubMemberAccountStatusRepository;

    // clubMemberAccountStatus 객체 생성 메서드
    public void createAccountStatus(Club club, ClubMemberTemp clubMemberTemp){

        // ClubMemberAccountStatus 객체 생성
        ClubMemberAccountStatus status;
        try{
            status = ClubMemberAccountStatus.createClubMemberAccountStatus(club, clubMemberTemp);
            log.debug("clubMember_Account_Status 객체 생성 완료- Club ID: {}, 사용자 ID : {}", club.getClubId(), clubMemberTemp.getClubMemberTempId());
        }catch (Exception e){
            log.error("clubMember_Account_Status 객체 생성 실패- Club ID: {},  사용자 ID : {}", club.getClubId(), clubMemberTemp.getClubMemberTempId());
            throw new ClubMemberAccountStatusException(ExceptionType.CLUB_MEMBER_ACCOUNTSTATUS_CREATE_FAILED);
        }

        // 생성된 ClubMemberAccountStatus 저장
        try{
            clubMemberAccountStatusRepository.save(status);
            log.debug("clubMember_Account_Status 객체 저장 완료- Club ID: {}, 사용자 ID : {}", club.getClubId(), clubMemberTemp.getClubMemberTempId());
        }catch (Exception e){
            log.error("clubMember_Account_Status 객체 저장 실패- Club ID: {}, 사용자 ID : {}", club.getClubId(), clubMemberTemp.getClubMemberTempId());
            throw new ClubMemberAccountStatusException(ExceptionType.CLUB_MEMBER_ACCOUNTSTATUS_CREATE_FAILED);
        }
    }

    // 각 동아리에 대한 요청 전송이 제대로 되었는지 검증
    public void checkRequest(ExistingMemberSignUpRequest request, ClubMemberTemp clubMemberTemp){

        log.debug("가입신청 검증 시작");

        // 총 생성된 ClubMemberAccountStatus 개수 확인
        long savedCount = clubMemberAccountStatusRepository.countByClubMemberTemp_ClubMemberTempId(clubMemberTemp.getClubMemberTempId());
        int expectedCount = request.getClubs().size();

        log.debug("개수 검증 결과 - 사용자 ID: {}, 저장된 개수: {}, 예상 개수: {}",
                clubMemberTemp.getClubMemberTempId(), savedCount, expectedCount);

        if (savedCount == expectedCount) {
            log.debug("요청 개수 일치 - 사용자 ID: {}", clubMemberTemp.getClubMemberTempId());
        } else {
            log.error("요청 개수 검증 실패 - 사용자 ID: {}, 저장된 개수: {}, 예상 개수: {}",
                    clubMemberTemp.getClubMemberTempId(), savedCount, expectedCount);
            throw new ClubMemberAccountStatusException(ExceptionType.CLUB_MEMBER_ACCOUNTSTATUS_COUNT_NOT_MATCH);
        }

        // 사용자가 선택한 동아리에 올바르게 전송 되었는지 확인

        // 사용자가 선택한 동아리 List
        Set<UUID> expected_clubUUID = request.getClubs().stream()
                .map(ClubDTO::getClubUUID)
                .collect(Collectors.toSet());

        // 요청을 실제로 보낸 동아리 List
        Set<UUID> saved_clubUUID = clubMemberAccountStatusRepository.findAllByClubMemberTemp_ClubMemberTempId(clubMemberTemp.getClubMemberTempId())
                .stream()
                .map(accountStatus -> accountStatus.getClub().getClubUUID())
                .collect(Collectors.toSet());

        // clubUUID가 모두 일치하는지 확인하기
        if(expected_clubUUID.equals(saved_clubUUID)){
            log.debug("사용자가 요청한 동아리 UUID와 저장된 동아리 UUID 값이 모두 일치합니다");
        }
        else{
            log.error("사용자가 요청한 동아리 UUID 와 저장된 동아리 UUID 값이 일치하지않습니다");
            throw new ClubMemberAccountStatusException(ExceptionType.CLUB_MEMBER_ACCOUNTSTATUS_REQEUST_NOT_MATCH);
        }
        log.debug("가입신청 검증 완료");
    }

    // clubMemberTemp -> accountStatus 객체 삭제
    @Transactional
    public void deleteAccountStatus(ClubMemberTemp expired) {

        List<ClubMemberAccountStatus> relatedStatuses = clubMemberAccountStatusRepository.findAllByClubMemberTemp_ClubMemberTempId(expired.getClubMemberTempId());

        try {
            if (!relatedStatuses.isEmpty()) {
                clubMemberAccountStatusRepository.deleteAll(relatedStatuses);
                log.debug("clubMemberTemp ID: {} -> 연관된 모든 AccountStatus 삭제 성공 완료", expired.getClubMemberTempId());
            } else {
                log.debug("삭제할 clubMemberTemp 없음: {}", expired.getClubMemberTempId());
            }
        } catch (Exception e) {
            log.error("clubMemberTemp ID: {} - 삭제시 오류 발생", expired.getClubMemberTempId(), e);
        }
    }

}
