package com.yse.dev.review.Entity;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String content;

    // 리뷰 작성자 아이디
    @Column(nullable = false)
    private String username;

    // 리뷰 작성자 닉네임
    @Column(nullable = true)
    private String nickname;

    // 카카오 장소 ID
    @Column(nullable = true)
    private String placeId;

    // 식당 이름
    @Column(nullable = true)
    private String restaurantName;

    // 식당 주소
    @Column(nullable = true)
    private String restaurantAddress;

    // 카카오맵 URL
    @Column(nullable = true)
    private String restaurantUrl;

    @CreationTimestamp
    @Column(nullable = true, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Builder.Default
    private String status = "NORMAL";
    
    @Column(length = 100)
    private String hiddenReason;
}