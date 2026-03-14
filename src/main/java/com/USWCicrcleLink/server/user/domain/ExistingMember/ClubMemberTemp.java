package com.USWCicrcleLink.server.user.domain.ExistingMember;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "CLUB_MEMBERTEMP_TABLE")
public class ClubMemberTemp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CLUB_MEMBERTEMP_ID")
    private Long clubMemberTempId;

    @Column(nullable = false,length = 20)
    private String profileTempAccount;

    @Column(nullable = false)
    private String profileTempPw;

    @Column(nullable = false,length = 30)
    private String profileTempName;

    @Column(nullable = false,length = 8)
    private String profileTempStudentNumber;

    @Column(nullable = false,length = 11)
    private String profileTempHp;

    @Column(nullable = false,length = 20)
    private String profileTempMajor;

    @Column(nullable = false,length = 30)
    private String profileTempEmail;

    @Column(nullable = false)
    private int totalClubRequest; // 총 지원한 동아리 수

    @Column(nullable = false)
    @Builder.Default
    private int clubRequestCount=0; // 동아리 회장이 수락한 횟수

    @Column(nullable = false)
    private LocalDateTime clubMemberTempExpiryDate;  // 요청  마감 날짜

    public void updateClubRequestCount() {
        this.clubRequestCount = this.clubRequestCount + 1;
    }

}


