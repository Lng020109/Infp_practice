package com.yse.dev.review.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yse.dev.review.DTO.ReviewRequestDTO;
import com.yse.dev.review.Entity.RestaurantReaction;
import com.yse.dev.review.Entity.Review;
import com.yse.dev.review.Service.RestaurantReactionService;
import com.yse.dev.review.Service.ReviewService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final RestaurantReactionService restaurantReactionService;

    @ResponseBody
    @PostMapping("/api/reviews")
    public ResponseEntity<String> createReview(
            @RequestBody ReviewRequestDTO requestDTO) {

        try {

            String resultMessage =
                    reviewService.createReview(
                            requestDTO.getPlaceId(),
                            requestDTO.getContent(),
                            requestDTO.getUsername(),
                            requestDTO.getRestaurantName(),
                            requestDTO.getRestaurantAddress(),
                            requestDTO.getRestaurantUrl()
                    );

            return ResponseEntity.ok(resultMessage);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @ResponseBody
    @PutMapping("/api/reviews/{reviewId}")
    public ResponseEntity<String> updateReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestBody Map<String, String> request) {

        try {

            String username = request.get("username");
            String content = request.get("content");

            String result =
                    reviewService.updateReview(
                            reviewId,
                            username,
                            content
                    );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @ResponseBody
    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<String> deleteReview(
            @PathVariable("reviewId") Long reviewId,
            @RequestParam("username") String username) {

        try {

            String result =
                    reviewService.deleteReview(
                            reviewId,
                            username
                    );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @ResponseBody
    @GetMapping("/api/reviews")
    public List<Review> getReviews() {
        return reviewService.findAll();
    }

    @ResponseBody
    @GetMapping("/api/reviews/my/{username}")
    public List<Review> getMyReviews(
            @PathVariable("username") String username) {

        return reviewService.findMyReviews(username);
    }

    @ResponseBody
    @GetMapping("/api/reviews/place/{placeId}")
    public List<Review> getReviewsByPlaceId(
            @PathVariable("placeId") String placeId) {

        return reviewService.getReviewsByplaceId(placeId);
    }

    @ResponseBody
    @PostMapping("/api/reactions")
    public ResponseEntity<?> toggleReaction(
            @RequestBody Map<String, String> request) {

        try {

            String placeId = request.get("placeId");
            String username = request.get("username");
            String reactionType = request.get("reactionType");
            String restaurantName = request.get("restaurantName");
            String restaurantAddress = request.get("restaurantAddress");
            String restaurantUrl = request.get("restaurantUrl");

            Map<String, Object> result =
                    restaurantReactionService.toggleReaction(
                            placeId,
                            username,
                            reactionType,
                            restaurantName,
                            restaurantAddress,
                            restaurantUrl
                    );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @ResponseBody
    @GetMapping("/api/reactions/{placeId}")
    public ResponseEntity<?> getReaction(
            @PathVariable("placeId") String placeId,
            @RequestParam(
                    value = "username",
                    required = false
            ) String username) {

        Map<String, Object> result =
                restaurantReactionService.getReactionResult(
                        placeId,
                        username
                );

        return ResponseEntity.ok(result);
    }

    @ResponseBody
    @GetMapping("/api/reactions/my/{username}")
    public List<RestaurantReaction> getMyReactions(
            @PathVariable("username") String username) {

        return restaurantReactionService
                .getMyReactions(username);
    }
}
