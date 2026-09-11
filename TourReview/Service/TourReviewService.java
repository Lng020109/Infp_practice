package com.yse.dev.TourReview.Service;

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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.yse.dev.TourReview.Entity.TourReview;
import com.yse.dev.TourReview.Entity.TourReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TourReviewService {

	private final TourReviewRepository reviewRepository;
	private final TourprofanityService profanityService;

	@Value("${gemini.api.key}")
    private String apikey;

    private final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=";

    private final List<String> forbiddenWords = Arrays.asList(
        "바보", "멍청이", "존나", "씨발", "병신", "개새끼", "쓰레기", "죽어", "꺼져"
    );
	
	// 특정 관광지 리뷰 목록 조회
	public List<TourReview> getReviewsBySpot(String spotId) {
		return reviewRepository.findBySpotIdOrderByIdDesc(spotId);
	}

	// 리뷰 등록 (금칙어 및 AI 비속어 검증 포함)
    @Transactional
    public TourReview createReview(String spotId, String spotName, String content, String writer) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("리뷰 내용을 입력해주세요.");
        }

        // 1. 단어 리스트 금칙어 검사
        checkForbiddenWords(content);

        // 2. Gemini AI 부적절 내용 검사
        if (isContentInappropriate(content)) {
            throw new IllegalArgumentException("부적절한 내용이 포함되어있어 리뷰를 등록할 수 없습니다.");
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

		// 1. 단어 리스트 금칙어 검사
		checkForbiddenWords(content);

		// 2. Gemini AI 부적절 내용 검사
		if (isContentInappropriate(content)) {
			throw new IllegalArgumentException("부적절한 내용이 포함되어있어 리뷰를 수정할 수 없습니다.");
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

	// --- 💡 하단에 추가된 비속어 및 AI 검증 관련 보조 메서드들 ---

	private void checkForbiddenWords(String content) {
		String cleanedContent = content.replaceAll(" ", "").toLowerCase();

		for (String word : forbiddenWords) {
			String cleanedWord = word.replaceAll(" ", "").toLowerCase();

			if (cleanedContent.contains(cleanedWord)) {
				throw new IllegalArgumentException(
						"부적절한 단어('" + word + "')가 포함되어 있어 리뷰를 등록할 수 없습니다.");
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
					restTemplate.postForEntity(url, requestEntity, Map.class);

			String aiResponseText = extractTextFromResponse(response.getBody());

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
			List<Map> candidates = (List<Map>) responseBody.get("candidates");
			Map content = (Map) candidates.get(0).get("content");
			List<Map> parts = (List<Map>) content.get("parts");
			return (String) parts.get(0).get("text");
		} catch (Exception e) {
			return "";
		}
	}
}
