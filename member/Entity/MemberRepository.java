package com.yse.dev.member.Entity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

//class 대신 interface 로 변경
public interface MemberRepository extends JpaRepository<Member, Long>{
	Optional<Member> findByUsername(String username);
	// 아이디로 회원 정보 찾기 (로그인 검증용)
	
	boolean existsByUsername(String username);
	// 아이디 중복 확인 (회원가입 검증용)

	boolean existsByNickname(String nickname);
	// 닉네임 중복 확인
	
	// 닉네임, 질문, 답변으로 회원 찾기 (아이디 찾기용)
	Optional<Member> findByNicknameAndSecurityQuestionAndSecurityAnswer(
	        String nickname, String securityQuestion, String securityAnswer);

	// 아이디, 질문, 답변으로 회원 찾기 (비밀번호 찾기/재설정용)
	Optional<Member> findByUsernameAndSecurityQuestionAndSecurityAnswer(
	        String username, String securityQuestion, String securityAnswer);

}
