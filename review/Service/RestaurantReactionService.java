package com.yse.dev.review.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yse.dev.review.Entity.RestaurantReaction;
import com.yse.dev.review.Entity.RestaurantReactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantReactionService {

    private final RestaurantReactionRepository reactionRepository;

    @Transactional
    public Map<String, Object> toggleReaction(
            String placeId,
            String username,
            String reactionType,
            String restaurantName,
            String restaurantAddress,
            String restaurantUrl) {

        if (placeId == null || placeId.isBlank()) {
            throw new IllegalArgumentException("식당 정보가 없습니다.");
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        if (!"LIKE".equals(reactionType) &&
            !"DISLIKE".equals(reactionType)) {
            throw new IllegalArgumentException("잘못된 반응입니다.");
        }

        Optional<RestaurantReaction> existing =
                reactionRepository.findByPlaceIdAndUsername(
                        placeId,
                        username);

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
            RestaurantReaction reaction =
                    RestaurantReaction.builder()
                            .placeId(placeId)
                            .username(username)
                            .reactionType(reactionType)
                            .restaurantName(restaurantName)
                            .restaurantAddress(restaurantAddress)
                            .restaurantUrl(restaurantUrl)
                            .build();

            reactionRepository.save(reaction);
        }

        return getReactionResult(placeId, username);
    }

    public Map<String, Object> getReactionResult(
            String placeId,
            String username) {

        long likeCount =
                reactionRepository.countByPlaceIdAndReactionType(
                        placeId,
                        "LIKE");

        long dislikeCount =
                reactionRepository.countByPlaceIdAndReactionType(
                        placeId,
                        "DISLIKE");

        String userReaction = null;

        if (username != null && !username.isBlank()) {

            Optional<RestaurantReaction> existing =
                    reactionRepository.findByPlaceIdAndUsername(
                            placeId,
                            username);

            if (existing.isPresent()) {
                userReaction =
                        existing.get().getReactionType();
            }
        }

        Map<String, Object> result = new HashMap<>();

        result.put("likeCount", likeCount);
        result.put("dislikeCount", dislikeCount);
        result.put("userReaction", userReaction);

        return result;
    }

    // 내가 좋아요/싫어요한 맛집 조회
    public List<RestaurantReaction> getMyReactions(
            String username) {

        return reactionRepository.findByUsername(username);
    }
}
