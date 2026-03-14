package com.USWCicrcleLink.server.clubIntro.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "CLUB_INTRO_PHOTO_TABLE")
public class ClubIntroPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_intro_photo_id")
    private Long clubIntroPhotoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_intro_id", nullable = false)
    private ClubIntro clubIntro;

    @Column(name = "club_intro_photo_name")
    private String clubIntroPhotoName;

    @Column(name = "club_intro_photo_s3key")
    private String clubIntroPhotoS3Key;

    @Column(name = "photo_order", nullable = false)
    private int order;

    public void updateClubIntroPhoto(String clubIntroPhotoName, String clubIntroPhotoS3Key, int order) {
        this.clubIntroPhotoName = clubIntroPhotoName;
        this.clubIntroPhotoS3Key = clubIntroPhotoS3Key;
        this.order = order;
    }
}
