package com.yse.dev.community.Controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/ai")
public class AiBotController {

    // 화면에 있는 그 키 그대로 사용합니다.
    private final String GEMINI_API_KEY = "AQ.Ab8RN6Ic2oYgoZAO5d9HDBQmcVYyR5RICBi2gIZgD_6ynsKaVg";
    private final RestClient restClient = RestClient.create();

    @PostMapping("/ask")
    public ResponseEntity<?> askAi(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");

        // 친절한 가이드 프롬프트 구성
        String promptText = "당신은 여행/맛집 추천 웹 서비스 '맛따라 여행따라'의 친절한 AI 가이드입니다. "
                          + "사용자의 질문에 맞춰 알맞은 장소나 맛집 2~3곳을 특징과 함께 3~4줄 이내로 깔끔하게 요약해서 추천해주세요.\n\n"
                          + "질문: " + userMessage;

        // Gemini REST API 요청 데이터 규격
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", promptText)
                ))
            )
        );

        try {
            // v1beta gemini-1.5-flash 엔드포인트 호출
        	// 기존:
        	// String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;

        	// 구글 안내 메시지에 맞춰 모델명을 gemini-3.6-flash로 지정
        	String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=" + GEMINI_API_KEY;

            Map response = restClient.post()
                    .uri(apiUrl)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            // 응답에서 텍스트 추출
            List<?> candidates = (List<?>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                List<?> parts = (List<?>) content.get("parts");
                Map<?, ?> part = (Map<?, ?>) parts.get(0);
                String reply = (String) part.get("text");
                return ResponseEntity.ok(Map.of("reply", reply));
            }

            return ResponseEntity.ok(Map.of("reply", "답변을 생성하지 못했습니다. 다시 시도해 주세요."));

        } catch (Exception e) {
            System.err.println("=== Gemini API 호출 에러 상세 ===");
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("reply", "상담 연결이 원활하지 않습니다. 잠시 후 다시 시도해주세요."));
        }
    }
}