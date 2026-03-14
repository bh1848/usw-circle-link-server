package com.USWCicrcleLink.server.club.domain;

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
@Table(name = "ClUB_CATEGORY_TABLE")
public class ClubCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_category_id")
    private Long clubCategoryId;

    @Builder.Default
    @Column(name = "club_category_uuid", nullable = false, unique = true, updatable = false)
    private UUID clubCategoryUUID = UUID.randomUUID();

    @Column(name = "club_category_name", nullable = false, length = 20)
    private String clubCategoryName;

    @PrePersist
    public void generateUUID() {
        if (this.clubCategoryUUID == null) {
            this.clubCategoryUUID = UUID.randomUUID();
        }
    }
}
