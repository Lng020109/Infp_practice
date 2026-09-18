package com.yse.dev.community.Controller;

import com.yse.dev.community.Entity.ChatMessage;
import com.yse.dev.community.Entity.ChatMessageRepository;
import com.yse.dev.community.Entity.Community;
import com.yse.dev.community.Entity.CommunityLike;
import com.yse.dev.community.Entity.CommunityLikeRepository;
import com.yse.dev.community.Entity.CommunityReply;
import com.yse.dev.community.Entity.CommunityReplyRepository;
import com.yse.dev.community.Service.CommunityService;
import com.yse.dev.member.Entity.Member;
import com.yse.dev.member.Entity.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@CrossOrigin
public class CommunityController {

	private final CommunityService communityService;

	private final CommunityReplyRepository replyRepository;

	private final CommunityLikeRepository likeRepository;

	private final MemberRepository memberRepository;

	private final ChatMessageRepository chatMessageRepository;

	// ==========================================
	// 게시글 전체 조회
	// ==========================================
	@GetMapping
	public ResponseEntity<List<Community>> getPosts() {

		return ResponseEntity.ok(communityService.getAllPosts());
	}

	// ==========================================
	// 작성자 기준 마이페이지 작성 글 조회
	// ==========================================
	@GetMapping("/my/{username}")
	public ResponseEntity<List<Community>> getMyPosts(@PathVariable("username") String username) {

		return ResponseEntity.ok(communityService.getMyPosts(username));
	}

	// ==========================================
	// 단일 게시글 조회
	// ==========================================
	@GetMapping("/{id}")
	public ResponseEntity<Community> getPost(@PathVariable("id") Long id) {

		return ResponseEntity.ok(communityService.getPost(id));
	}

	// ==========================================
	// 게시글 작성
	// ==========================================
	@PostMapping
	public ResponseEntity<?> createPost(@RequestBody Community community) {

		try {
			return ResponseEntity.ok(communityService.createPost(community));

		} catch (IllegalArgumentException e) {

			return ResponseEntity.badRequest().body("⚠️ " + e.getMessage());
		}
	}

	// ==========================================
	// 게시글 삭제
	// ==========================================
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deletePost(@PathVariable("id") Long id) {

		communityService.deletePost(id);

		return ResponseEntity.ok("게시글이 삭제되었습니다.");
	}

	// ==========================================
	// 게시글 수정
	// ==========================================
	@PutMapping("/{id}")
	public ResponseEntity<Community> updatePost(@PathVariable("id") Long id, @RequestBody Community community) {

		Community post = communityService.getPost(id);

		post.setTitle(community.getTitle());
		post.setContent(community.getContent());

		return ResponseEntity.ok(communityService.savePost(post));
	}

	// ==========================================
	// 좋아요 / 좋아요 취소
	// ==========================================
	@PostMapping("/{id}/like")
	public ResponseEntity<Community> likePost(@PathVariable("id") Long id, @RequestParam("username") String username) {

		Community post = communityService.getPost(id);

		boolean alreadyLiked = likeRepository.existsByPostIdAndUsername(id, username);

		if (alreadyLiked) {

			// 좋아요 취소
			likeRepository.deleteByPostIdAndUsername(id, username);

			post.setLikeCount(Math.max(0, post.getLikeCount() - 1));

		} else {

			// 좋아요
			likeRepository.save(new CommunityLike(id, username));

			post.setLikeCount(post.getLikeCount() + 1);
		}

		return ResponseEntity.ok(communityService.savePost(post));
	}

	// ==========================================
	// 조회수 증가
	// ==========================================
	@PostMapping("/{id}/view")
	public ResponseEntity<Community> viewPost(@PathVariable("id") Long id) {

		Community post = communityService.getPost(id);

		post.setViewCount(post.getViewCount() + 1);

		return ResponseEntity.ok(communityService.savePost(post));
	}

	// ==========================================
	// 답글 조회
	// ==========================================
	@GetMapping("/{postId}/replies")
	public ResponseEntity<List<CommunityReply>> getReplies(@PathVariable("postId") Long postId) {

		return ResponseEntity.ok(replyRepository.findByPostIdOrderByCreatedAtAsc(postId));
	}

	// ==========================================
    // 답글 작성
    // ==========================================
    @PostMapping("/{postId}/replies")
    public ResponseEntity<CommunityReply> createReply(
            @PathVariable("postId") Long postId,
            @RequestBody CommunityReply reply) {

        Member member =
                memberRepository
                        .findByUsername(reply.getAuthor())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "존재하지 않는 회원입니다."
                                )
                        );

        reply.setPostId(postId);

        // 답글 작성자는 닉네임으로 표시하고, 이미지 조회를 위해 username 보관
        reply.setAuthor(
                member.getNickname()
        );
        reply.setUsername(
                member.getUsername()
        );

        return ResponseEntity.ok(
                replyRepository.save(reply)
        );
    }

	// ==========================================
	// 답글 수정
	// ==========================================
	@PutMapping("/replies/{replyId}")
	public ResponseEntity<?> updateReply(
			@PathVariable("replyId") Long replyId,
			@RequestParam("username") String username,
			@RequestBody CommunityReply reply) {

		try {

			CommunityReply updatedReply =
					communityService.updateReply(
							replyId,
							username,
							reply.getContent()
					);

			return ResponseEntity.ok(updatedReply);

		} catch (IllegalArgumentException e) {

			return ResponseEntity.badRequest().body(e.getMessage());

		} catch (RuntimeException e) {

			return ResponseEntity.status(404).body(e.getMessage());
		}
	}


	// ==========================================
	// 답글 삭제
	// ==========================================
	@DeleteMapping("/replies/{replyId}")
	public ResponseEntity<String> deleteReply(
			@PathVariable("replyId") Long replyId,
			@RequestParam("username") String username) {

		try {

			communityService.deleteReply(
					replyId,
					username
			);

			return ResponseEntity.ok("답글이 삭제되었습니다.");

		} catch (RuntimeException e) {

			return ResponseEntity.status(404).body(e.getMessage());
		}
	}

	// ==========================================
	// 프로필 이미지 조회
	// ==========================================
	@GetMapping("/profile-image/{username}")
	public ResponseEntity<byte[]> getProfileImage(@PathVariable("username") String username) {

		Member member = memberRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("회원이 없습니다."));

		// 프로필 사진이 없는 경우
		if (member.getProfileImage() == null || member.getProfileImage().length == 0) {

			return ResponseEntity.notFound().build();
		}

		// 이미지 타입이 없는 경우
		if (member.getProfileImageType() == null || member.getProfileImageType().isBlank()) {

			return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(member.getProfileImage());
		}

		return ResponseEntity.ok().contentType(MediaType.parseMediaType(member.getProfileImageType()))
				.body(member.getProfileImage());
	}

	// ==========================================
	// 이전 채팅 기록 조회
	// ==========================================
	@GetMapping("/chat/history")
	public List<ChatMessage> getChatHistory() {

		List<ChatMessage> messages = chatMessageRepository.findTop100ByOrderByCreatedAtDesc();

		// 최신순으로 가져온 것을 오래된 순으로 변경
		Collections.reverse(messages);

		return messages;
	}

}