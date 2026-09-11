package com.yse.dev.TourReview.Service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class TourprofanityService {

    // 기본적인 비속어/금칙어 리스트 (필요에 따라 확장 또는 외부 AI API로 교체 가능)
    private final List<String> profanityList = Arrays.asList("욕설1", "욕설2", "비속어");

    /**
     * 텍스트에 부적절한 표현이 포함되어 있는지 검증
     * @return true: 비속어 포함됨 (차단 대상), false: 정상 텍스트
     */
    public boolean isProfane(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        String lowerContent = content.toLowerCase();

        // 금칙어 포함 여부 단순 체크 (추후 LLM 또는 외부 AI moderation API로 고도화 가능)
        for (String word : profanityList) {
            if (lowerContent.contains(word)) {
                return true; // 비속어 감지됨
            }
        }

        return false; // 정상
    }
}