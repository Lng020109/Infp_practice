package com.yse.dev.TourReview.Entity;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TourReviewRepository extends JpaRepository<TourReview, Long> {
    
    // 특정 관광지 ID의 리뷰를 최신순으로 조회
    List<TourReview> findBySpotIdOrderByIdDesc(String spotId);

    // [추가] 특정 작성자가 쓴 리뷰를 최신순으로 조회
    List<TourReview> findByWriterOrderByIdDesc(String writer);
}
