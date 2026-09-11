package com.yse.dev.member.Controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.yse.dev.member.DTO.MemberDTO;
import com.yse.dev.member.Entity.Member;
import com.yse.dev.member.Service.MemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody MemberDTO dto) {

        boolean isSuccess = memberService.register(dto);

        if (isSuccess) {
            return ResponseEntity.ok("회원가입 성공");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("회원가입에 실패했습니다. 입력값을 확인해주세요.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody MemberDTO dto) {

        boolean isSuccess =
                memberService.login(dto.getUsername(), dto.getPassword());

        if (isSuccess) {
            return ResponseEntity.ok("로그인 성공");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("다시 입력해주세요.");
        }
    }

    @GetMapping("/{username}")
    public ResponseEntity<MemberDTO> getMember(
            @PathVariable("username") String username) {

        MemberDTO member = memberService.getMember(username);

        if (member == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(member);
    }

    @PutMapping("/{username}")
    public ResponseEntity<String> updateMember(
            @PathVariable("username") String username,
            @RequestBody MemberDTO dto) {

        boolean isSuccess =
                memberService.updateMember(username, dto);

        if (isSuccess) {
            return ResponseEntity.ok("회원정보가 수정되었습니다.");
        }

        return ResponseEntity.badRequest()
                .body("회원정보 수정에 실패했습니다. (중복된 닉네임이거나 부적절한 단어가 포함되어 있습니다.)");
        }

    @DeleteMapping("/{username}")
    public ResponseEntity<String> deleteMember(
            @PathVariable("username") String username) {

        boolean isSuccess =
                memberService.deleteMember(username);

        if (isSuccess) {
            return ResponseEntity.ok("회원탈퇴가 완료되었습니다.");
        }

        return ResponseEntity.badRequest()
                .body("회원탈퇴에 실패했습니다.");
    }

    // 아이디 찾기
    @PostMapping("/find-id")
    public ResponseEntity<?> findId(@RequestBody Map<String, String> request) {
        try {
            String nickname = request.get("nickname");
            String question = request.get("securityQuestion");
            String answer = request.get("securityAnswer");

            String foundUsername = memberService.findUsername(nickname, question, answer);
            return ResponseEntity.ok(Map.of("username", foundUsername));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
 // 비밀번호 찾기 (조회)
    @PostMapping("/find-pw")
    public ResponseEntity<?> findPw(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String question = request.get("securityQuestion");
            String answer = request.get("securityAnswer");

            String password = memberService.findPassword(username, question, answer);
            return ResponseEntity.ok(Map.of("password", password));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 비밀번호 재설정
    @PostMapping("/reset-pw")
    public ResponseEntity<?> resetPw(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String question = request.get("securityQuestion");
            String answer = request.get("securityAnswer");
            String newPassword = request.get("newPassword");

            memberService.resetPassword(username, question, answer, newPassword);
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
 // =========================
 // 프로필 이미지 업로드
 // =========================
 @PutMapping(
         value = "/{username}/profile-image",
         consumes = MediaType.MULTIPART_FORM_DATA_VALUE
 )
 public ResponseEntity<String> uploadProfileImage(
         @PathVariable("username") String username,
         @RequestParam("file") MultipartFile file) {

     boolean isSuccess =
             memberService.updateProfileImage(username, file);

     if (isSuccess) {
         return ResponseEntity.ok("프로필 사진이 변경되었습니다.");
     }

     return ResponseEntity.badRequest()
             .body("프로필 사진 변경에 실패했습니다.");
 }


 // =========================
 // 프로필 이미지 조회
 // =========================
 @GetMapping("/{username}/profile-image")
 public ResponseEntity<byte[]> getProfileImage(
         @PathVariable("username") String username) {

     Member member =
             memberService.getMemberEntity(username);

     if (member == null ||
         member.getProfileImage() == null) {
         return ResponseEntity.notFound().build();
     }

     MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

     if (member.getProfileImageType() != null) {
         try {
             mediaType =
                     MediaType.parseMediaType(
                             member.getProfileImageType()
                     );
         } catch (Exception e) {
             mediaType =
                     MediaType.APPLICATION_OCTET_STREAM;
         }
     }

     return ResponseEntity.ok()
             .contentType(mediaType)
             .body(member.getProfileImage());
 }


 // =========================
 // 프로필 이미지 삭제
 // =========================
 @DeleteMapping("/{username}/profile-image")
 public ResponseEntity<String> deleteProfileImage(
         @PathVariable("username") String username) {

     boolean isSuccess =
             memberService.deleteProfileImage(username);

     if (isSuccess) {
         return ResponseEntity.ok("프로필 사진이 삭제되었습니다.");
     }

     return ResponseEntity.badRequest()
             .body("프로필 사진 삭제에 실패했습니다.");
 }
}