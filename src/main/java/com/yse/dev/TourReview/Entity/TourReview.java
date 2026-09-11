package com.yse.dev.TourReview.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tour_review")
@Getter 
@Setter
@NoArgsConstructor
public class TourReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String spotId; // 관광지/맛집 고유 ID 또는 이름

    private String spotName; // 관광지 이름

    @Column(nullable = false, length = 1000)
    private String content; // 리뷰 내용

    private String writer; // 작성자

    private int likeCount = 0; // 좋아요 수
    private int dislikeCount = 0; // 싫어요 수

    private LocalDateTime createdAt = LocalDateTime.now(); // 작성일시
}