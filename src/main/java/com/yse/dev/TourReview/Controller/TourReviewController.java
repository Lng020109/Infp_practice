package com.yse.dev.TourReview.Controller;

import com.yse.dev.TourReview.Entity.TourReview;
import com.yse.dev.TourReview.Service.TourReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tour-reviews")
@RequiredArgsConstructor
public class TourReviewController {

	private final TourReviewService reviewService;

	// 1. 리뷰 목록 조회 API
	@GetMapping
	public ResponseEntity<List<TourReview>> getReviews(@RequestParam("spotId") String spotId) {
		List<TourReview> reviews = reviewService.getReviewsBySpot(spotId);
		return ResponseEntity.ok(reviews);
	}

	// 2. 리뷰 등록 API (비속어 감지 시 400 Bad Request 반환)
	@PostMapping
	public ResponseEntity<?> createReview(@RequestBody Map<String, String> request) {
		try {
			String spotId = request.get("spotId");
			String spotName = request.get("spotName");
			String content = request.get("content");
			String writer = request.get("writer"); // 로그인한 사용자 정보가 있다면 연동 가능

			TourReview savedReview = reviewService.createReview(spotId, spotName, content, writer);
			return ResponseEntity.ok(savedReview);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	// 3. 좋아요 / 싫어요 반응 API
	@PostMapping("/{reviewId}/reaction")
	public ResponseEntity<?> updateReaction(@PathVariable Long reviewId, @RequestBody Map<String, String> request) {
		try {
			String type = request.get("type"); // "LIKE" 또는 "DISLIKE"
			TourReview updatedReview = reviewService.updateReaction(reviewId, type);
			return ResponseEntity.ok(updatedReview);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	// 4. 내가 작성한 관광지 리뷰 목록 조회 API
	@GetMapping("/my/{writer}")
	public ResponseEntity<List<TourReview>> getMyTourReviews(@PathVariable("writer") String writer) {
		List<TourReview> reviews = reviewService.getMyTourReviews(writer);
		return ResponseEntity.ok(reviews);
	}

	// 관광지 리뷰 수정 API
	@PutMapping("/{reviewId}")
	public ResponseEntity<?> updateReview(@PathVariable("reviewId") Long reviewId,
			@RequestBody Map<String, String> request) {
		try {
			String writer = request.get("writer");
			String content = request.get("content");
			TourReview updated = reviewService.updateTourReview(reviewId, writer, content);
			return ResponseEntity.ok(updated);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	// 관광지 리뷰 삭제 API
	@DeleteMapping("/{reviewId}")
	public ResponseEntity<?> deleteReview(@PathVariable("reviewId") Long reviewId,
			@RequestParam("writer") String writer) {
		try {
			reviewService.deleteTourReview(reviewId, writer);
			return ResponseEntity.ok("관광지 리뷰가 삭제되었습니다.");
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}