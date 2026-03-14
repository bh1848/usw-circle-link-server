package com.USWCicrcleLink.server.profile.domain;

import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.ProfileException;
import com.USWCicrcleLink.server.user.domain.User;
import com.USWCicrcleLink.server.user.dto.SignUpRequest;
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
@Table(name = "PROFILE_TABLE")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "user_name", nullable = false, length = 30)
    private String userName;

    @Column(name = "student_number", nullable = false,length = 8)
    private String studentNumber;

    @Column(name = "user_hp", nullable = false,length = 11)
    private String userHp;

    @Column(name = "major", nullable = false,length = 20)
    private String major;

    @Column(name = "profile_created_at", nullable = false)
    private LocalDateTime profileCreatedAt;

    @Column(name = "profile_updated_at", nullable = false)
    private LocalDateTime profileUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false)
    private MemberType memberType;

    @Column(name = "fcm_token")
    private String fcmToken;// 회원이 직접 로그인할 때

    @Column(name = "fcm_token_updated_at")
    private LocalDateTime fcmTokenCertificationTimestamp;

    public static Profile createProfile(User user, SignUpRequest request,String telephone) {
        return Profile.builder()
                .user(user)
                .userName(request.getUserName())
                .studentNumber(request.getStudentNumber())
                .userHp(telephone)
                .major(request.getMajor())
                .profileCreatedAt(LocalDateTime.now())
                .profileUpdatedAt(LocalDateTime.now())
                .memberType(MemberType.REGULARMEMBER) // 기본값: 정회원
                .build();
    }

    public void updateProfile(String userName, String studentNumber, String major, String userHp) {
        if (userName != null) {
            validateProfileInput(userName);
            this.userName = userName;
        }
        if (studentNumber != null) {
            validateProfileInput(studentNumber);
            this.studentNumber = studentNumber;
        }
        if (major != null) {
            validateProfileInput(major);
            this.major = major;
        }
        if (userHp != null) {
            validateProfileInput(userHp);
            this.userHp = userHp;
        }

        this.profileUpdatedAt = LocalDateTime.now();
    }


    private void validateProfileInput(String fieldValue) {
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            throw new ProfileException(ExceptionType.INVALID_INPUT);
        }
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void updateFcmTokenTime(String fcmToken, LocalDateTime fcmTokenCertificationTimestamp) {
        this.fcmToken = fcmToken;
        this.fcmTokenCertificationTimestamp = fcmTokenCertificationTimestamp;
    }

    public void updateUser(User user) {
        this.user = user;
    }

    public void updateMemberType(MemberType memberType) {
        this.memberType = memberType;
    }
}