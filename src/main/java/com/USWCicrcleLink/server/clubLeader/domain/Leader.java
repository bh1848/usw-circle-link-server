package com.USWCicrcleLink.server.clubLeader.domain;

import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
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
@Table(name = "LEADER_TABLE")
public class Leader{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leaderId;

    @Column(name = "leader_account", nullable = false, unique = true)
    private String leaderAccount;

    @Builder.Default
    @Column(name = "leader_uuid",nullable = false, updatable = false)
    private UUID leaderUUID = UUID.randomUUID();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @Column(name = "leader_pw", nullable = false)
    private String leaderPw;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Builder.Default
    @Column(name = "is_agreed_terms", nullable = false)
    private boolean isAgreedTerms = false;

    @PrePersist
    public void prePersist() {
        this.leaderUUID = UUID.randomUUID();
    }

    public void setAgreeTerms(boolean isAgreed) {
        this.isAgreedTerms = isAgreed;
    }
}