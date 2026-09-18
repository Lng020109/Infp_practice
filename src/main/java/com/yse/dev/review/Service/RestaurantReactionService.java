package com.yse.dev.review.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yse.dev.member.Entity.Member;
import com.yse.dev.member.Entity.MemberRepository;
import com.yse.dev.notification.NotificationService;
import com.yse.dev.review.Entity.RestaurantReaction;
import com.yse.dev.review.Entity.RestaurantReactionRepository;
import com.yse.dev.review.Entity.Review;
import com.yse.dev.review.Entity.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantReactionService {

	private final RestaurantReactionRepository reactionRepository;
	private final ReviewRepository reviewRepository; // 추가 주입
	private final MemberRepository memberRepository; // 추가 주입
	private final NotificationService notificationService; // 알림 서비스 주입

	@Transactional
	public Map<String, Object> toggleReaction(String placeId, String username, String reactionType,
			String restaurantName, String restaurantAddress, String restaurantUrl) {

		if (placeId == null || placeId.isBlank()) {
			throw new IllegalArgumentException("식당 정보가 없습니다.");
		}

		if (username == null || username.isBlank()) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		if (!"LIKE".equals(reactionType) && !"DISLIKE".equals(reactionType)) {
			throw new IllegalArgumentException("잘못된 반응입니다.");
		}

		Optional<RestaurantReaction> existing = reactionRepository.findByPlaceIdAndUsername(placeId, username);

		if (existing.isPresent()) {

			RestaurantReaction reaction = existing.get();

			// 같은 버튼을 다시 누르면 취소
			if (reaction.getReactionType().equals(reactionType)) {

				reactionRepository.delete(reaction);

			} else {

				// 좋아요 ↔ 싫어요 변경
				reaction.setReactionType(reactionType);
				reaction.setRestaurantName(restaurantName);
				reaction.setRestaurantAddress(restaurantAddress);
				reaction.setRestaurantUrl(restaurantUrl);

				reactionRepository.save(reaction);
			}

		} else {

			// 처음 누른 경우
			RestaurantReaction reaction = RestaurantReaction.builder().placeId(placeId).username(username)
					.reactionType(reactionType).restaurantName(restaurantName).restaurantAddress(restaurantAddress)
					.restaurantUrl(restaurantUrl).build();

			reactionRepository.save(reaction);

			// ⭐ 알림 전송 (LIKE인 경우에만 해당 맛집 리뷰 작성자들에게 알림)
			if ("LIKE".equals(reactionType)) {
				try {
					Member sender = memberRepository.findByUsername(username).orElse(null);

					if (sender != null) {
						List<Review> reviews = reviewRepository.findByPlaceId(placeId);

						// 중복 알림 방지를 위해 수신자 username 모으기
						for (Review rev : reviews) {
							if (!rev.getUsername().equals(username)) { // 내가 쓴 리뷰는 제외
								memberRepository.findByUsername(rev.getUsername()).ifPresent(receiver -> {
									notificationService.sendNotification(
											receiver.getId(), sender.getId(), "LIKE_REVIEW", sender.getNickname()
													+ "님이 회원님이 리뷰를 남기신 [" + restaurantName + "]을(를) 추천했습니다.",
											restaurantUrl != null ? restaurantUrl : "/map");
								});
							}
						}
					}
				} catch (Exception e) {
					System.err.println("맛집 좋아요 알림 전송 실패: " + e.getMessage());
				}
			}
		}

		return getReactionResult(placeId, username);
	}

	public Map<String, Object> getReactionResult(String placeId, String username) {

		long likeCount = reactionRepository.countByPlaceIdAndReactionType(placeId, "LIKE");

		long dislikeCount = reactionRepository.countByPlaceIdAndReactionType(placeId, "DISLIKE");

		String userReaction = null;

		if (username != null && !username.isBlank()) {

			Optional<RestaurantReaction> existing = reactionRepository.findByPlaceIdAndUsername(placeId, username);

			if (existing.isPresent()) {
				userReaction = existing.get().getReactionType();
			}
		}

		Map<String, Object> result = new HashMap<>();

		result.put("likeCount", likeCount);
		result.put("dislikeCount", dislikeCount);
		result.put("userReaction", userReaction);

		return result;
	}

	// 내가 좋아요/싫어요한 맛집 조회
	public List<RestaurantReaction> getMyReactions(String username) {

		return reactionRepository.findByUsername(username);
	}
}