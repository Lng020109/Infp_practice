package com.yse.dev.TourReview.Entity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TourReviewRepository extends JpaRepository<TourReview, Long> {
    
    // 특정 관광지 ID의 리뷰를 최신순으로 조회
    List<TourReview> findBySpotIdOrderByIdDesc(String spotId);

    // 특정 작성자가 쓴 리뷰를 최신순으로 조회
    List<TourReview> findByWriterOrderByIdDesc(String writer);

    // 관광지 리뷰 전체 좋아요 수
    @Query("SELECT COALESCE(SUM(t.likeCount), 0) FROM TourReview t")
    long getTotalLikeCount();

    // 관광지 리뷰 전체 싫어요 수
    @Query("SELECT COALESCE(SUM(t.dislikeCount), 0) FROM TourReview t")
    long getTotalDislikeCount();
}