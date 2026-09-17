package com.yse.dev.TourReview.Reaction;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TourReactionService {

	private final TourReactionRepository reactionRepository;

	// 관광지 좋아요/싫어요 토글
	@Transactional
	public TourReaction toggleReaction(String spotId, String username, String reactionType, String spotName) {

		if (spotId == null || spotId.isBlank()) {
			throw new IllegalArgumentException("관광지 ID가 없습니다.");
		}

		if (username == null || username.isBlank()) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		if (!"LIKE".equalsIgnoreCase(reactionType) && !"DISLIKE".equalsIgnoreCase(reactionType)) {
			throw new IllegalArgumentException("잘못된 반응 타입입니다.");
		}

		Optional<TourReaction> existing = reactionRepository.findBySpotIdAndUsername(spotId, username);

		// 이미 반응이 있는 경우
		if (existing.isPresent()) {

			TourReaction reaction = existing.get();

			// 같은 버튼을 다시 누르면 취소
			if (reaction.getReactionType().equalsIgnoreCase(reactionType)) {

				reactionRepository.delete(reaction);
				return null;
			}

			// 다른 버튼을 누르면 반대 반응으로 변경
			reaction.setReactionType(reactionType.toUpperCase());

			return reactionRepository.save(reaction);
		}

		// 처음 반응하는 경우
		TourReaction reaction = TourReaction.builder().spotId(spotId).username(username)
				.reactionType(reactionType.toUpperCase()).spotName(spotName).build();

		return reactionRepository.save(reaction);
	}

	// 관광지 좋아요/싫어요 개수 조회
	public TourReactionResult getReactionResult(String spotId, String username) {

		long likeCount = reactionRepository.countBySpotIdAndReactionType(spotId, "LIKE");

		long dislikeCount = reactionRepository.countBySpotIdAndReactionType(spotId, "DISLIKE");

		String myReaction = null;

		if (username != null && !username.isBlank()) {

			Optional<TourReaction> reaction = reactionRepository.findBySpotIdAndUsername(spotId, username);

			if (reaction.isPresent()) {
				myReaction = reaction.get().getReactionType();
			}
		}

		return new TourReactionResult(likeCount, dislikeCount, myReaction);
	}

	// 관광지 반응 결과 DTO
	public record TourReactionResult(long likeCount, long dislikeCount, String myReaction) {
	}
	
	// TourReactionService.java에 추가
	public List<TourReaction> getMyReactions(String username) {
	    return reactionRepository.findByUsername(username);
	}
}