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

import com.yse.dev.member.Entity.MemberRepository;
import com.yse.dev.review.Entity.Review;
import com.yse.dev.review.Entity.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;

    @Value("${gemini.api.key}")
    private String apikey;

    private final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=";

    private final List<String> forbiddenWords = Arrays.asList(
            "바보",
            "멍청이",
            "존나",
            "씨발",
            "병신",
            "개새끼",
            "쓰레기",
            "죽어",
            "꺼져"
    );

    // =========================================================
    // 1. 리뷰 작성
    // =========================================================
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

        String trimmedContent = content.trim();

        // 금칙어 / Gemini 검사
        String initialStatus = determineReviewStatus(trimmedContent);

        // 회원 닉네임 가져오기
        String nickname = memberRepository.findByUsername(username)
                .map(member -> member.getNickname())
                .orElse(null);

        Review review = Review.builder()
                .placeId(placeId)
                .content(trimmedContent)
                .username(username)
                .nickname(nickname)
                .restaurantName(restaurantName)
                .restaurantAddress(restaurantAddress)
                .restaurantUrl(restaurantUrl)
                .status(initialStatus)
                .hiddenReason(
                        "HIDDEN".equals(initialStatus)
                                ? "AI/금칙어 검사"
                                : null
                )
                .build();

        reviewRepository.save(review);

        if ("HIDDEN".equals(initialStatus)) {
            return "부적절한 단어 사용으로 자동 숨김처리 되었습니다.";
        }

        if ("PENDING".equals(initialStatus)) {
            return "리뷰가 등록되었습니다. (관리자 검토 후 노출 예정)";
        }

        return "리뷰가 정상적으로 등록되었습니다!";
    }

    // =========================================================
    // 2. 리뷰 수정
    // =========================================================
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

        String trimmedContent = content.trim();

        // 수정된 내용 다시 검사
        String updatedStatus = determineReviewStatus(trimmedContent);

        review.setContent(trimmedContent);
        review.setStatus(updatedStatus);

        if ("HIDDEN".equals(updatedStatus)) {
            review.setHiddenReason("AI/금칙어 검사");
        } else {
            review.setHiddenReason(null);
        }

        reviewRepository.save(review);

        return "리뷰가 수정되었습니다.";
    }

    // =========================================================
    // 3. 리뷰 삭제
    // =========================================================
    public String deleteReview(Long reviewId, String username) {

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

    // =========================================================
    // 관리자 리뷰 상태 변경
    // =========================================================
    public void updateReviewStatus(Long reviewId, String status) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        if (!status.equals("NORMAL")
                && !status.equals("PENDING")
                && !status.equals("HIDDEN")) {

            throw new IllegalArgumentException("잘못된 리뷰 상태입니다.");
        }

        review.setStatus(status);

        /*
         * 관리자가 NORMAL 또는 PENDING으로 변경하면
         * 기존 숨김 사유 제거
         *
         * HIDDEN으로 직접 변경한 경우에는
         * 기존 사유를 그대로 유지하도록 함.
         */
        if (!"HIDDEN".equals(status)) {
            review.setHiddenReason(null);
        }

        reviewRepository.save(review);
    }

    // =========================================================
    // AI 및 금칙어 검사
    // =========================================================
    private String determineReviewStatus(String content) {

        // -----------------------------------------------------
        // 1. 직접 지정한 금칙어 검사
        // -----------------------------------------------------
        String cleanedContent =
                content.replaceAll(" ", "").toLowerCase();

        for (String word : forbiddenWords) {

            String cleanedWord =
                    word.replaceAll(" ", "").toLowerCase();

            if (cleanedContent.contains(cleanedWord)) {
                return "HIDDEN";
            }
        }

        // -----------------------------------------------------
        // 2. Gemini API 키가 없으면 NORMAL
        // -----------------------------------------------------
        if (apikey == null || apikey.trim().isEmpty()) {
            return "NORMAL";
        }

        String url = GEMINI_API_URL + apikey.trim();

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // -----------------------------------------------------
        // 3. Gemini에게 리뷰 상태 판별 요청
        // -----------------------------------------------------
        String prompt =
                "당신은 리뷰 검토 AI입니다. "
                + "다음 리뷰 내용을 판단하여 반드시 "
                + "[NORMAL, PENDING, HIDDEN] 중 딱 한 단어만 대답하세요.\n"
                + "- NORMAL: 정상적이고 유용한 후기\n"
                + "- PENDING: 무의미한 자음/모음 나열, 짧은 도배, "
                + "비방 의심 등 관리자 확인이 필요한 경우\n"
                + "- HIDDEN: 명백한 욕설, 비속어, 성희롱, "
                + "심한 혐오 표현\n\n"
                + "리뷰 내용: "
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
                    extractTextFromResponse(response.getBody())
                            .trim()
                            .toUpperCase();

            if (aiResponseText.contains("HIDDEN")) {
                return "HIDDEN";
            }

            if (aiResponseText.contains("PENDING")) {
                return "PENDING";
            }

        } catch (Exception e) {

            System.err.println(
                    "Gemini 호출 중 오류 (NORMAL 처리): "
                    + e.getMessage()
            );

            return "NORMAL";
        }

        return "NORMAL";
    }

    // =========================================================
    // Gemini 응답에서 텍스트 추출
    // =========================================================
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

    // =========================================================
    // 4. 전체 리뷰 조회
    // 탈퇴 회원 마스킹 적용
    // =========================================================
    public List<Review> findAll() {

        List<Review> reviews =
                reviewRepository.findAll();

        reviews.forEach(review -> {

            memberRepository
                    .findByUsername(review.getUsername())
                    .ifPresentOrElse(member -> {

                        review.setNickname(member.getNickname());

                        if (member.isWithdrawn()) {
                            review.setUsername("탈퇴한 회원");
                        }

                    }, () -> {

                        review.setUsername("탈퇴한 회원");

                    });
        });

        return reviews;
    }

    // =========================================================
    // 5. 특정 사용자의 리뷰 조회
    // =========================================================
    public List<Review> findMyReviews(String username) {

        List<Review> reviews =
                reviewRepository.findByUsername(username);

        reviews.forEach(review -> {

            memberRepository
                    .findByUsername(review.getUsername())
                    .ifPresent(member ->
                            review.setNickname(
                                    member.getNickname()
                            )
                    );
        });

        return reviews;
    }

    // =========================================================
    // 6. 특정 맛집 리뷰 조회
    // HIDDEN 리뷰는 사용자 화면에서 제외
    // =========================================================
    public List<Review> getReviewsByplaceId(String placeId) {

        List<Review> reviews =
                reviewRepository.findByPlaceId(placeId);

        // 사용자 화면에서는 HIDDEN 리뷰를 보여주지 않음
        reviews.removeIf(
                review -> "HIDDEN".equals(review.getStatus())
        );

        reviews.forEach(review -> {

            memberRepository
                    .findByUsername(review.getUsername())
                    .ifPresentOrElse(member -> {

                        review.setNickname(member.getNickname());

                        if (member.isWithdrawn()) {
                            review.setUsername("탈퇴한 회원");
                        }

                    }, () -> {

                        review.setUsername("탈퇴한 회원");

                    });
        });

        return reviews;
    }
}