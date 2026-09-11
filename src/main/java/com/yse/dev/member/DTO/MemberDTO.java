package com.yse.dev.member.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberDTO {
	private String username;
	private String password;
	private String nickname;
	private String securityQuestion;
	private String securityAnswer;
}