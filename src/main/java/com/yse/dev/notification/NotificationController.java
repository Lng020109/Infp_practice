package com.yse.dev.notification;

import com.yse.dev.member.Entity.Member;
import com.yse.dev.member.Entity.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin
public class NotificationController {

	private final NotificationService notificationService;
	private final MemberRepository memberRepository;

	/**
	 * 1. 안 읽은 알림이 있는지 확인 (빨간 점 표시 여부 반환: true / false) GET
	 * /api/notifications/unread-status?username=xxx
	 */
	@GetMapping("/unread-status")
	public ResponseEntity<Boolean> hasUnreadNotifications(@RequestParam("username") String username) {
		Member member = memberRepository.findByUsername(username).orElse(null);
		if (member == null) {
			return ResponseEntity.ok(false);
		}
		boolean hasUnread = notificationService.hasUnreadNotifications(member.getId());
		return ResponseEntity.ok(hasUnread);
	}

	/**
	 * 2. 내 알림 전체 목록 조회 GET /api/notifications?username=xxx
	 */
	@GetMapping
	public ResponseEntity<List<Notification>> getNotifications(@RequestParam("username") String username) {
		Member member = memberRepository.findByUsername(username).orElse(null);
		if (member == null) {
			return ResponseEntity.ok(Collections.emptyList());
		}
		List<Notification> list = notificationService.getNotifications(member.getId());
		return ResponseEntity.ok(list);
	}

	/**
	 * 3. 특정 알림 1개 읽음 처리 PATCH /api/notifications/{id}/read
	 */
	@PatchMapping("/{id}/read")
	public ResponseEntity<Void> markAsRead(@PathVariable("id") Long id) {
		notificationService.markAsRead(id);
		return ResponseEntity.ok().build();
	}

	/**
	 * 4. 내 모든 알림 일괄 읽음 처리 (종 아이콘을 눌러 목록을 열었을 때 호출) PATCH
	 * /api/notifications/read-all?username=xxx
	 */
	@PatchMapping("/read-all")
	public ResponseEntity<Void> markAllAsRead(@RequestParam("username") String username) {
		Member member = memberRepository.findByUsername(username).orElse(null);
		if (member != null) {
			notificationService.markAllAsRead(member.getId());
		}
		return ResponseEntity.ok().build();
	}
}