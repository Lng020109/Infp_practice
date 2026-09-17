package com.yse.dev.member.Service;

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.yse.dev.member.DTO.MemberDTO;
import com.yse.dev.member.Entity.Member;
import com.yse.dev.member.Entity.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9]{4,12}$");

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^[a-zA-Z0-9]{4,16}$");

    private static final Pattern NICKNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9가-힣]{2,20}$");

    @org.springframework.beans.factory.annotation.Value("${gemini.api.key}")
    private String apikey;

    private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=";

    private final java.util.List<String> forbiddenWords = java.util.Arrays.asList(
    		"바보", "qkqhdi", 
            "멍청이", "audcjdgl", 
            "시발", "씨발", "tlqkf", "씨바", "시바", "ㅅㅂ", "ㅆㅂ",
            "존나", "whssk", 
            "병신", "qudtls", 
            "개새끼", "쓰레기", "죽어", "꺼져"
     );
    

 // =========================
    // 회원가입 (디버깅 로그 포함)
    // =========================
    public boolean register(MemberDTO dto) {

        if (dto.getUsername() == null ||
            dto.getPassword() == null ||
            dto.getNickname() == null ||
            dto.getSecurityQuestion() == null ||
            dto.getSecurityAnswer() == null) {
            System.out.println("❌ [회원가입 실패] 필수값 null 감지: " +
                    "username=" + dto.getUsername() + 
                    ", password=" + dto.getPassword() + 
                    ", nickname=" + dto.getNickname() + 
                    ", question=" + dto.getSecurityQuestion() + 
                    ", answer=" + dto.getSecurityAnswer());
            return false;
        }

        if (dto.getSecurityQuestion().trim().isEmpty() || 
            dto.getSecurityAnswer().trim().isEmpty()) {
            System.out.println("❌ [회원가입 실패] 질문/답변 공백 감지");
            return false;
        }

        if (!USERNAME_PATTERN.matcher(dto.getUsername()).matches()) {
            System.out.println("❌ [회원가입 실패] 아이디 정규식 불일치 (4~12자 영문/숫자): " + dto.getUsername());
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(dto.getPassword()).matches()) {
            System.out.println("❌ [회원가입 실패] 비밀번호 정규식 불일치 (4~16자 영문/숫자): " + dto.getPassword());
            return false;
        }

        if (!NICKNAME_PATTERN.matcher(dto.getNickname()).matches()) {
            System.out.println("❌ [회원가입 실패] 닉네임 정규식 불일치 (2~20자 한글/영문/숫자): " + dto.getNickname());
            return false;
        }

        if (memberRepository.existsByUsername(dto.getUsername())) {
            System.out.println("❌ [회원가입 실패] 이미 존재하는 아이디: " + dto.getUsername());
            return false;
        }

        if (memberRepository.existsByNickname(dto.getNickname())) {
            System.out.println("❌ [회원가입 실패] 이미 존재하는 닉네임: " + dto.getNickname());
            return false;
        }

        if (checkForbiddenWords(dto.getNickname())) {
            System.out.println("❌ [회원가입 실패] 1차 금칙어 감지: " + dto.getNickname());
            return false;
        }

        if (isContentInappropriate(dto.getNickname())) {
            System.out.println("❌ [회원가입 실패] 2차 비속어 감지: " + dto.getNickname());
            return false;
        }

        try {
            Member member = new Member();
            member.setUsername(dto.getUsername());
            member.setPassword(dto.getPassword());
            member.setNickname(dto.getNickname());
            member.setSecurityQuestion(dto.getSecurityQuestion());
            member.setSecurityAnswer(dto.getSecurityAnswer().trim());

            memberRepository.save(member);
            System.out.println("⭕ [회원가입 성공] DB 저장 완료: " + dto.getUsername());
            return true;

        } catch (Exception e) {
            System.out.println("❌ [회원가입 실패] DB 저장 중 에러 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // =========================
    // 아이디 찾기 (추가)[cite: 17]
    // =========================
    public String findUsername(String nickname, String question, String answer) {
        if (nickname == null || question == null || answer == null) {
            throw new IllegalArgumentException("모든 정보를 입력해주세요.");
        }

        Optional<Member> member = memberRepository
                .findByNicknameAndSecurityQuestionAndSecurityAnswer(nickname.trim(), question.trim(), answer.trim()); //[cite: 17]

        if (member.isEmpty()) {
            throw new IllegalArgumentException("일치하는 회원 정보를 찾을 수 없습니다.");
        }

        return member.get().getUsername(); //[cite: 16]
    }
    
 // =========================
    // 비밀번호 찾기 (조회)
    // =========================
    public String findPassword(String username, String question, String answer) {
        if (username == null || question == null || answer == null) {
            throw new IllegalArgumentException("모든 정보를 입력해주세요.");
        }

        Optional<Member> memberOpt = memberRepository
                .findByUsernameAndSecurityQuestionAndSecurityAnswer(username.trim(), question.trim(), answer.trim());

        if (memberOpt.isEmpty()) {
            throw new IllegalArgumentException("일치하는 회원 정보를 찾을 수 없습니다.");
        }

        return memberOpt.get().getPassword();
    }

    // =========================
    // 비밀번호 재설정 (추가)[cite: 17]
    // =========================
    public boolean resetPassword(String username, String question, String answer, String newPassword) {
        if (username == null || question == null || answer == null || newPassword == null) {
            throw new IllegalArgumentException("모든 정보를 입력해주세요.");
        }

        // 새 비밀번호 유효성 검사 (4~16자리)
        if (!PASSWORD_PATTERN.matcher(newPassword.trim()).matches()) {
            throw new IllegalArgumentException("비밀번호는 영문 또는 숫자 4~16자리로 입력해주세요.");
        }

        Optional<Member> memberOpt = memberRepository
                .findByUsernameAndSecurityQuestionAndSecurityAnswer(username.trim(), question.trim(), answer.trim()); //[cite: 17]

        if (memberOpt.isEmpty()) {
            throw new IllegalArgumentException("일치하는 회원 정보를 찾을 수 없습니다.");
        }

        Member member = memberOpt.get();
        member.setPassword(newPassword.trim());
        memberRepository.save(member);

        return true;
    }

    private boolean checkForbiddenWords(String content) {
        String cleanedContent = content.replaceAll(" ", "").toLowerCase();
        for (String word : forbiddenWords) {
            String cleanedWord = word.replaceAll(" ", "").toLowerCase();
            if (cleanedContent.contains(cleanedWord)) {
                return true;
            }
        }
        return false;
    }

    private String extractTextFromResponse(java.util.Map responseBody) {
        try {
            java.util.List<java.util.Map> candidates = (java.util.List<java.util.Map>) responseBody.get("candidates");
            java.util.Map content = (java.util.Map) candidates.get(0).get("content");
            java.util.List<java.util.Map> parts = (java.util.List<java.util.Map>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return "";
        }
    }

    // =========================
    // 로그인(9/14 수정)
    // =========================
    public boolean login(String username, String password) {

        Optional<Member> findMember =
                memberRepository.findByUsername(username);

        if (findMember.isEmpty()) {
            return false;
        }

        Member member = findMember.get();

        if (member.isWithdrawn()) {
            return false;
        }

        return member.getPassword().equals(password);
    }

    // =========================
    // 회원정보 조회
    // =========================
    public MemberDTO getMember(String username) {

        Optional<Member> findMember =
                memberRepository.findByUsername(username);

        if (findMember.isEmpty()) {
            return null;
        }

        Member member = findMember.get();

        MemberDTO dto = new MemberDTO();
        dto.setUsername(member.getUsername());
        dto.setPassword(member.getPassword());
        dto.setNickname(member.getNickname());
        dto.setSecurityQuestion(member.getSecurityQuestion()); // 질문 전달[cite: 15, 18]
        dto.setSecurityAnswer(member.getSecurityAnswer());     // 답변 전달[cite: 15, 18]

        return dto;
    }

 // =========================
    // 회원정보 수정 (null 방어 코드 적용)
    // =========================
    public boolean updateMember(String username, MemberDTO dto) {

        Optional<Member> findMember =
                memberRepository.findByUsername(username);

        if (findMember.isEmpty()) {
            System.out.println("[회원정보 수정 실패] 회원을 찾을 수 없음");
            return false;
        }

        Member member = findMember.get();

        // 닉네임 변경 요청이 들어온 경우에만 닉네임 유효성 및 비속어 검사 수행
        if (dto.getNickname() != null && !dto.getNickname().trim().isEmpty()) {
            
            // 1. 닉네임 정규식 유효성 검사 (2~20자 한글/영문/숫자)
            if (!NICKNAME_PATTERN.matcher(dto.getNickname()).matches()) {
                System.out.println("❌ [회원정보 수정 실패] 2~20자 한글/영문/숫자로 입력해주세요: " + dto.getNickname());
                return false;
            }

            // 2. 닉네임 중복 검사 (기존 본인 닉네임과 다를 때만)
            if (!member.getNickname().equals(dto.getNickname()) &&
                memberRepository.existsByNickname(dto.getNickname())) {
                System.out.println("❌ [회원정보 수정 실패] 이미 존재하는 닉네임: " + dto.getNickname());
                return false;
            }

            // 3. 1차 금지어 리스트 검사
            if (checkForbiddenWords(dto.getNickname())) {
                System.out.println("❌ [회원정보 수정 실패] 1차 금칙어 감지: " + dto.getNickname());
                return false;
            }

            // 4. 2차 Gemini AI 비속어 검사
            if (isContentInappropriate(dto.getNickname())) {
                System.out.println("❌ [회원정보 수정 실패] 2차 비속어 감지: " + dto.getNickname());
                return false;
            }

            member.setNickname(dto.getNickname().trim());
        }

        // 비밀번호 변경 요청이 들어온 경우 업데이트
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            member.setPassword(dto.getPassword().trim());
        }

        // 보안질문/답변 업데이트 (추가부분)
        if (dto.getSecurityQuestion() != null && !dto.getSecurityQuestion().trim().isEmpty()) {
            member.setSecurityQuestion(dto.getSecurityQuestion().trim());
        }

        if (dto.getSecurityAnswer() != null && !dto.getSecurityAnswer().trim().isEmpty()) {
            member.setSecurityAnswer(dto.getSecurityAnswer().trim());
        }

        memberRepository.save(member);
        return true;
    }

    // =========================
    // 프로필 이미지 저장
    // =========================
    public boolean updateProfileImage(
            String username,
            MultipartFile file) {

        try {
            if (file == null || file.isEmpty()) {
                return false;
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                return false;
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return false;
            }

            Optional<Member> findMember =
                    memberRepository.findByUsername(username);

            if (findMember.isEmpty()) {
                return false;
            }

            Member member = findMember.get();
            member.setProfileImage(file.getBytes());
            member.setProfileImageType(contentType);

            memberRepository.save(member);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================
    // 프로필 이미지 조회
    // =========================
    public Member getMemberEntity(String username) {
        Optional<Member> findMember =
                memberRepository.findByUsername(username);
        return findMember.orElse(null);
    }

    // =========================
    // 프로필 이미지 삭제
    // =========================
    public boolean deleteProfileImage(String username) {
        Optional<Member> findMember =
                memberRepository.findByUsername(username);

        if (findMember.isEmpty()) {
            return false;
        }

        Member member = findMember.get();
        member.setProfileImage(null);
        member.setProfileImageType(null);

        memberRepository.save(member);
        return true;
    }

    // =========================
    // 회원탈퇴 (9/14 변경)
    // =========================
    public boolean deleteMember(String username) {
        Optional<Member> findMember =
                memberRepository.findByUsername(username);

        if (findMember.isEmpty()) {
            return false;
        }

        Member member = findMember.get();

        member.setWithdrawn(true);

        memberRepository.save(member);

        return true;
    }

    // =========================
    // 비속어 검증 메서드
    // =========================
//    private boolean isContentInappropriate(String content) {
//        if (apikey == null || apikey.trim().isEmpty()) {
//            return false;
//        }
//        String url = GEMINI_API_URL + apikey.trim();
//        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
//        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
//        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
//
//        String prompt = "다음 닉네임에 욕설, 비속어, 또는 유해한 내용이 포함되어 있나요? 'true' 또는 'false'로만 답해주세요. 내용: " + content;
//
//        java.util.Map<String, Object> part = new java.util.HashMap<>();
//        part.put("text", prompt);
//        java.util.Map<String, Object> contentMap = new java.util.HashMap<>();
//        contentMap.put("parts", java.util.List.of(part));
//        java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
//        requestBody.put("contents", java.util.List.of(contentMap));
//
//        org.springframework.http.HttpEntity<java.util.Map<String, Object>> requestEntity =
//                new org.springframework.http.HttpEntity<>(requestBody, headers);
//
//        try {
//            org.springframework.http.ResponseEntity<java.util.Map> response =
//                    restTemplate.postForEntity(url, requestEntity, java.util.Map.class);
//
//            String aiResponseText = extractTextFromResponse(response.getBody());
//
//            if (aiResponseText != null && aiResponseText.toLowerCase().contains("true")) {
//                return true;
//            }
//        } catch (Exception e) {
//            return false;
//        }
//        return false;
//    }
    
 // =========================
    // 비속어 검증 메서드 (AI 키 오류로 인한 가입 거절 방지)
    // =========================
    private boolean isContentInappropriate(String content) {
        return false;
    }
}