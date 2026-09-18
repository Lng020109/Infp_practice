package com.yse.dev.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

	private final NotificationRepository notificationRepository;

	/**
	 * 알림 전송 (DB 저장) 자신이 한 행동(예: 내가 쓴 글에 내가 댓글/좋아요)은 알림이 가지 않도록 방어 로직 포함
	 */
	@Transactional
	public void sendNotification(Long receiverId, Long senderId, String type, String content, String targetUrl) {
		// 본인 글에 본인이 댓글/좋아요를 단 경우 알림 생략
		if (receiverId.equals(senderId)) {
			return;
		}

		Notification notification = Notification.builder().receiverId(receiverId).senderId(senderId).type(type)
				.content(content).targetUrl(targetUrl).build();

		notificationRepository.save(notification);
	}

	/**
	 * 안 읽은 알림이 있는지 여부 확인 (빨간 점 띄우기용)
	 */
	public boolean hasUnreadNotifications(Long memberId) {
		return notificationRepository.existsByReceiverIdAndIsReadFalse(memberId);
	}

	/**
	 * 특정 회원의 전체 알림 목록 조회
	 */
	public List<Notification> getNotifications(Long memberId) {
		return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(memberId);
	}

	/**
	 * 단일 알림 읽음 처리
	 */
	@Transactional
	public void markAsRead(Long notificationId) {
		notificationRepository.findById(notificationId).ifPresent(Notification::markAsRead);
	}

	/**
	 * 모든 알림 일괄 읽음 처리 (종 아이콘 눌러서 목록 전체 열람 시 사용)
	 */
	@Transactional
	public void markAllAsRead(Long memberId) {
		List<Notification> notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(memberId);
		for (Notification notification : notifications) {
			if (!notification.isRead()) {
				notification.markAsRead();
			}
		}
	}
}