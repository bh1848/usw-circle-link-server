package com.USWCicrcleLink.server.club.domain;

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
@Table(name = "CLUB_MAIN_PHOTO_TABLE")
public class ClubMainPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_main_photo_id")
    private Long clubMainPhotoId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "club_main_photo_name")
    private String clubMainPhotoName;

    @Column(name = "club_main_photo_s3key")
    private String clubMainPhotoS3Key;
}
