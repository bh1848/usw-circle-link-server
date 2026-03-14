package com.USWCicrcleLink.server.user.domain.ExistingMember;

import com.USWCicrcleLink.server.club.domain.Club;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "CLUB_MEMBER_ACCOUNTSTATUS_TABLE")
public class ClubMemberAccountStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CLUB_MEMBER_ACCOUNTSTATUS_ID")
    private Long clubMemberAccountStatusId;


    @Column(name = "clubmemberAccountStatus_uuid", unique = true, nullable = false, updatable = false)
    private UUID clubMemberAccountStatusUUID;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;


    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "CLUBMEMBERTEMP_ID", nullable = false)
    private ClubMemberTemp clubMemberTemp;


    @PrePersist
    public void prePersist() {
        if (this.clubMemberAccountStatusUUID == null) {
            this.clubMemberAccountStatusUUID = UUID.randomUUID();  // 자동 UUID 생성
        }
    }

    public static ClubMemberAccountStatus createClubMemberAccountStatus(Club club, ClubMemberTemp clubMemberTemp) {
        return ClubMemberAccountStatus.builder()
                .club(club)
                .clubMemberTemp(clubMemberTemp)
                .build();
    }

}


