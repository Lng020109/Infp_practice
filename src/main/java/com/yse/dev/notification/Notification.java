package com.yse.dev.notification;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "notification_id")
	private Long id;

	@Column(name = "receiver_id", nullable = false)
	private Long receiverId; // 알림 받을 회원 ID

	@Column(name = "sender_id", nullable = false)
	private Long senderId; // 행위를 한 회원 ID

	@Column(nullable = false, length = 20)
	private String type; // 'LIKE_POST', 'COMMENT', 'LIKE_REVIEW' 등

	@Column(nullable = false)
	private String content;

	@Column(name = "target_url")
	private String targetUrl;

	@Column(name = "is_read", nullable = false)
	private boolean isRead = false;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	@Builder
	public Notification(Long receiverId, Long senderId, String type, String content, String targetUrl) {
		this.receiverId = receiverId;
		this.senderId = senderId;
		this.type = type;
		this.content = content;
		this.targetUrl = targetUrl;
		this.isRead = false;
		this.createdAt = LocalDateTime.now();
	}

	// 알림 읽음 처리 메서드
	public void markAsRead() {
		this.isRead = true;
	}
}