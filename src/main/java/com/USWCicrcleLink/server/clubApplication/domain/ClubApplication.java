package com.USWCicrcleLink.server.clubApplication.domain;

import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.profile.domain.Profile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "CLUB_APPLICATION_TABLE",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_club_application_profile_club",
                columnNames = {"profile_id", "club_id"}
        )
)
public class ClubApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_application_id")
    private Long clubApplicationId;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Builder.Default
    @Column(name = "club_application_uuid", nullable = false, unique = true, updatable = false)
    private UUID clubApplicationUUID = UUID.randomUUID();

    @Column(name = "club_application_submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "club_application_status", nullable = false, length = 10)
    private ClubApplicationStatus clubApplicationStatus = ClubApplicationStatus.WAIT;

    @Column(name = "club_application_checked")
    private boolean checked;

    @Column(name = "club_application_delete_date")
    private LocalDateTime deleteDate;

    @PrePersist
    public void generateUUID() {
        if (this.clubApplicationUUID == null) {
            this.clubApplicationUUID = UUID.randomUUID();
        }
    }

    public void updateClubApplicationStatus(ClubApplicationStatus newStatus, boolean checked, LocalDateTime deleteDate) {
        this.clubApplicationStatus = newStatus;
        this.checked = checked;
        this.deleteDate = deleteDate;
    }

    public void updateFailedClubApplicationStatus(ClubApplicationStatus newStatus) {
        this.clubApplicationStatus = newStatus;
    }
}
