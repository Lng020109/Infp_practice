package com.yse.dev.review.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.yse.dev.member.Entity.MemberRepository; // 1. 회원 검증용 리포지토리 추가
import com.yse.dev.review.Entity.Review;
import com.yse.dev.review.Entity.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository; // 2. 회원 탈퇴 여부 체크용 주입

    @Value("${gemini.api.key}")
    private String apikey;

    private final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=";

    private final List<String> forbiddenWords = Arrays.asList(
        "바보", "멍청이", "존나", "씨발", "병신", "개새끼", "쓰레기", "죽어", "꺼져"
    );

    // 1. 리뷰 작성
    public String createReview(
            String placeId,
            String content,
            String username,
            String restaurantName,
            String restaurantAddress,
            String restaurantUrl) {

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("리뷰 내용을 입력해주세요.");
        }

        checkForbiddenWords(content);

        if (isContentInappropriate(content)) {
            throw new IllegalArgumentException(
                    "부적절한 내용이 포함되어있어 리뷰를 등록할 수 없습니다.");
        }

        Review review = Review.builder()
                .placeId(placeId)
                .content(content.trim())
                .username(username)
                .restaurantName(restaurantName)
                .restaurantAddress(restaurantAddress)
                .restaurantUrl(restaurantUrl)
                .build();

        reviewRepository.save(review);

        return "리뷰가 등록되었습니다.";
    }

    // 2. 리뷰 수정
    public String updateReview(
            Long reviewId,
            String username,
            String content) {

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("리뷰 내용을 입력해주세요.");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        if (!review.getUsername().equals(username)) {
            throw new IllegalArgumentException(
                    "본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        checkForbiddenWords(content);

        if (isContentInappropriate(content)) {
            throw new IllegalArgumentException(
                    "부적절한 내용이 포함되어있어 리뷰를 수정할 수 없습니다.");
        }

        review.setContent(content.trim());

        reviewRepository.save(review);

        return "리뷰가 수정되었습니다.";
    }

    // 3. 리뷰 삭제
    public String deleteReview(
            Long reviewId,
            String username) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        if (!review.getUsername().equals(username)) {
            throw new IllegalArgumentException(
                    "본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        reviewRepository.delete(review);

        return "리뷰가 삭제되었습니다.";
    }

    private void checkForbiddenWords(String content) {
        String cleanedContent =
                content.replaceAll(" ", "").toLowerCase();

        for (String word : forbiddenWords) {
            String cleanedWord =
                    word.replaceAll(" ", "").toLowerCase();

            if (cleanedContent.contains(cleanedWord)) {
                throw new IllegalArgumentException(
                        "부적절한 단어('" + word +
                        "')가 포함되어 있어 리뷰를 등록할 수 없습니다.");
            }
        }
    }

    private boolean isContentInappropriate(String content) {
        if (apikey == null || apikey.trim().isEmpty()) {
            return false;
        }

        String url = GEMINI_API_URL + apikey.trim();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String prompt =
                "다음 리뷰 내용에 욕설, 비속어, 광고성 내용, 또는 유해한 내용이 포함되어 있나요? "
                + "'true' 또는 'false'로만 답해주세요. 내용: "
                + content;

        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("parts", List.of(part));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(contentMap));

        HttpEntity<Map<String, Object>> requestEntity =
                new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(
                            url,
                            requestEntity,
                            Map.class
                    );

            String aiResponseText =
                    extractTextFromResponse(response.getBody());

            if (aiResponseText != null &&
                    aiResponseText.toLowerCase().contains("true")) {
                return true;
            }

        } catch (Exception e) {
            System.err.println("Gemini 호출 중 오류 (통과 처리): " + e.getMessage());
            return false;
        }

        return false;
    }

    private String extractTextFromResponse(Map responseBody) {
        try {
            List<Map> candidates =
                    (List<Map>) responseBody.get("candidates");
            Map content =
                    (Map) candidates.get(0).get("content");
            List<Map> parts =
                    (List<Map>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return "";
        }
    }

    // 4. 전체 리뷰 조회 (탈퇴 회원 마스킹 적용)
    public List<Review> findAll() {
        List<Review> reviews = reviewRepository.findAll();
        reviews.forEach(review -> {
            if (!memberRepository.existsByUsername(review.getUsername())) {
                review.setUsername("탈퇴한 회원");
            }
        });
        return reviews;
    }

    // 5. 특정 사용자의 리뷰 조회
    public List<Review> findMyReviews(String username) {
        return reviewRepository.findByUsername(username);
    }

    // 6. 특정 맛집의 리뷰 목록 조회 (탈퇴 회원 마스킹 적용 핵심)
    public List<Review> getReviewsByplaceId(String placeId) {
        List<Review> reviews = reviewRepository.findByPlaceId(placeId);

        // 작성자가 member 테이블에 없으면 화면에 "탈퇴한 회원"으로 출력
        reviews.forEach(review -> {
            boolean isMemberExists = memberRepository.existsByUsername(review.getUsername());
            if (!isMemberExists) {
                review.setUsername("탈퇴한 회원");
            }
        });

        return reviews;
    }
}
