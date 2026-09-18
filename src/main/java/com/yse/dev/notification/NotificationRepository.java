package com.yse.dev.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	// 1. 특정 사용자의 안 읽은 알림이 있는지 확인 (종 옆에 빨간 점 띄울지 판단용)
	boolean existsByReceiverIdAndIsReadFalse(Long receiverId);

	// 2. 특정 사용자의 안 읽은 알림 개수 조회 (필요 시 숫자 표시용)
	long countByReceiverIdAndIsReadFalse(Long receiverId);

	// 3. 특정 사용자의 알림 목록 최신순 조회
	List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);
}