package com.USWCicrcleLink.server.club.domain;

import com.USWCicrcleLink.server.profile.domain.Profile;
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
@Table(name = "CLUB_MEMBERS_TABLE")
public class ClubMembers {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_member_id")
    private Long clubMemberId;

    @Builder.Default
    @Column(name = "club_member_uuid", unique = true, nullable = false, updatable = false)
    private UUID clubMemberUUID= UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_Id", nullable = false)
    private Club club;
}
