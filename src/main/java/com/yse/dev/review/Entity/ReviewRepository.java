package com.yse.dev.review.Entity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 사용자가 작성한 리뷰 조회
    List<Review> findByUsername(String username);

    // 특정 맛집의 리뷰 조회
    List<Review> findByPlaceId(String placeId);

    // 특정 사용자가 작성한 특정 리뷰 조회
    Optional<Review> findByIdAndUsername(Long id, String username);
}