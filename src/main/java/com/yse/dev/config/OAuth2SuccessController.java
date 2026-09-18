package com.yse.dev.config;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yse.dev.member.Entity.Member;
import com.yse.dev.member.Entity.MemberRepository;

@Controller
public class OAuth2SuccessController {

	@Autowired
	private MemberRepository memberRepository;

	@GetMapping(value = "/api/oauth2/success", produces = "text/html; charset=UTF-8")
	@ResponseBody
	public String loginSuccess(@AuthenticationPrincipal OAuth2User oauth2User) {
		String nickname = "소셜회원";
		String username = "social_user";

		if (oauth2User != null) {
			Map<String, Object> attributes = oauth2User.getAttributes();

			// 1. 카카오
			if (attributes.containsKey("kakao_account")) {
				Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
				Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
				if (profile != null && profile.get("nickname") != null) {
					nickname = profile.get("nickname").toString();
				}
				username = "kakao_" + attributes.get("id");
			}
			// 2. 네이버
			else if (attributes.containsKey("response")) {
				Map<String, Object> response = (Map<String, Object>) attributes.get("response");
				if (response.get("nickname") != null) {
					nickname = response.get("nickname").toString();
				} else if (response.get("name") != null) {
					nickname = response.get("name").toString();
				}
				username = "naver_" + response.get("id");
			}
			// 3. 구글
			else if (attributes.containsKey("sub")) {
				if (attributes.get("name") != null) {
					nickname = attributes.get("name").toString();
				}
				username = "google_" + attributes.get("sub");
			}

			// 소셜 로그인 회원이 DB에 없으면 자동 저장(가입)
			Optional<Member> existingMember = memberRepository.findByUsername(username);
			if (existingMember.isEmpty()) {
				Member newMember = new Member();
				newMember.setUsername(username);
				newMember.setNickname(nickname);
				newMember.setPassword("OAUTH2_USER");
				newMember.setSecurityQuestion("소셜 로그인 계정");
				newMember.setSecurityAnswer("OAuth2");
				memberRepository.save(newMember);
			} else {
				nickname = existingMember.get().getNickname();
			}
		}

		return "<script>" + "sessionStorage.setItem('currentLoggedInUser', JSON.stringify({" + "    id: '" + username
				+ "'," + "    username: '" + username + "'," + "    nickname: '" + nickname + "'" + "}));" + "alert('"
				+ nickname + "님 환영합니다!');" + "location.href = '/map';" + "</script>";
	}
}