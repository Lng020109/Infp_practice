package com.yse.dev.TourReview.Reaction;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tour-reactions")
@RequiredArgsConstructor
public class TourReactionController {

	private final TourReactionService reactionService;

	// 관광지 좋아요/싫어요 토글
	@PostMapping
	public ResponseEntity<?> toggleReaction(@RequestBody Map<String, String> request) {

		try {
			String spotId = request.get("spotId");
			String username = request.get("username");
			String reactionType = request.get("reactionType");
			String spotName = request.get("spotName");

			TourReaction reaction = reactionService.toggleReaction(spotId, username, reactionType, spotName);

			// 같은 버튼을 다시 눌러 반응을 취소한 경우
			if (reaction == null) {
				return ResponseEntity.ok(Map.of("message", "반응이 취소되었습니다."));
			}

			return ResponseEntity.ok(reaction);

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	// 관광지 좋아요/싫어요 개수 조회
	@GetMapping("/{spotId}")
	public ResponseEntity<?> getReaction(@PathVariable("spotId") String spotId,
			@RequestParam(value = "username", required = false) String username) {

		try {
			TourReactionService.TourReactionResult result = reactionService.getReactionResult(spotId, username);

			return ResponseEntity.ok(result);

		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	// TourReactionController.java에 추가 (import java.util.List; 확인)
	@GetMapping("/my/{username}")
	public ResponseEntity<List<TourReaction>> getMyReactions(@PathVariable("username") String username) {
		List<TourReaction> list = reactionService.getMyReactions(username);
		return ResponseEntity.ok(list);
	}
}