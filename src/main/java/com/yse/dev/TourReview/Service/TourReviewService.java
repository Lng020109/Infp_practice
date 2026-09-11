package com.yse.dev.TourReview.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yse.dev.TourReview.Entity.TourReview;
import com.yse.dev.TourReview.Entity.TourReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TourReviewService {

	private final TourReviewRepository reviewRepository;
	private final TourprofanityService profanityService;

	// 특정 관광지 리뷰 목록 조회
	public List<TourReview> getReviewsBySpot(String spotId) {
		return reviewRepository.findBySpotIdOrderByIdDesc(spotId);
	}

	// 리뷰 등록 (AI 비속어 검증 포함)
	@Transactional
	public TourReview createReview(String spotId, String spotName, String content, String writer) {
		// 비속어 검증
		if (profanityService.isProfane(content)) {
			throw new IllegalArgumentException("비속어나 부적절한 표현이 포함되어 있습니다.");
		}

		TourReview review = new TourReview();
		review.setSpotId(spotId);
		review.setSpotName(spotName);
		review.setContent(content);
		review.setWriter(writer != null ? writer : "익명 사용자");

		return reviewRepository.save(review);
	}

	// 좋아요 / 싫어요 토글 및 카운트 증가
	@Transactional
	public TourReview updateReaction(Long reviewId, String type) {
		TourReview review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new IllegalArgumentException("해당 리뷰를 찾을 수 없습니다. ID: " + reviewId));

		if ("LIKE".equalsIgnoreCase(type)) {
			review.setLikeCount(review.getLikeCount() + 1);
		} else if ("DISLIKE".equalsIgnoreCase(type)) {
			review.setDislikeCount(review.getDislikeCount() + 1);
		}

		return reviewRepository.save(review);
	}

	// 특정 사용자가 작성한 관광지 리뷰 목록 조회
	public List<TourReview> getMyTourReviews(String writer) {
		return reviewRepository.findByWriterOrderByIdDesc(writer);
	}

	// 관광지 리뷰 수정
	@Transactional
	public TourReview updateTourReview(Long reviewId, String writer, String content) {
		if (content == null || content.trim().isEmpty()) {
			throw new IllegalArgumentException("리뷰 내용을 입력해주세요.");
		}

		TourReview review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new IllegalArgumentException("해당 리뷰를 찾을 수 없습니다."));

		if (!review.getWriter().equals(writer)) {
			throw new IllegalArgumentException("본인이 작성한 리뷰만 수정할 수 있습니다.");
		}

		if (profanityService.isProfane(content)) {
			throw new IllegalArgumentException("비속어나 부적절한 표현이 포함되어 있습니다.");
		}

		review.setContent(content.trim());
		return reviewRepository.save(review);
	}

	// 관광지 리뷰 삭제
	@Transactional
	public void deleteTourReview(Long reviewId, String writer) {
		TourReview review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new IllegalArgumentException("해당 리뷰를 찾을 수 없습니다."));

		if (!review.getWriter().equals(writer)) {
			throw new IllegalArgumentException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
		}

		reviewRepository.delete(review);
	}
}