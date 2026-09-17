package com.yse.dev.admin.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.yse.dev.TourReview.Entity.TourReview;
import com.yse.dev.TourReview.Entity.TourReviewRepository;
import com.yse.dev.TourReview.Reaction.TourReactionRepository;
import com.yse.dev.admin.Entity.Admin;
import com.yse.dev.admin.Entity.AdminRepository;
import com.yse.dev.member.Entity.Member;
import com.yse.dev.member.Entity.MemberRepository;
import com.yse.dev.review.Entity.RestaurantReactionRepository;
import com.yse.dev.review.Entity.Review;
import com.yse.dev.review.Entity.ReviewRepository;
import com.yse.dev.review.Service.ReviewService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

	private final AdminRepository adminRepository;
	private final MemberRepository memberRepository;
	private final ReviewRepository reviewRepository;
	private final ReviewService reviewService;
	private final RestaurantReactionRepository restaurantReactionRepository;
	private final TourReviewRepository tourReviewRepository;
	private final TourReactionRepository tourReactionRepository;
	
public boolean login(String username, String password) {

    if (username == null || username.isBlank()) {
        return false;
    }

    if (password == null || password.isBlank()) {
        return false;
    }

    Optional<Admin> admin =
            adminRepository.findByUsername(username);

    if (admin.isEmpty()) {
        return false;
    }

    return admin.get().getPassword().equals(password);
	}

	// 전체 회원 조회
	public List<Member> findAllMembers() {
		return memberRepository.findByWithdrawnFalse();
	}

	// 탈퇴 회원 조회
	public List<Member> findWithdrawnMembers() {
	    return memberRepository.findByWithdrawnTrue();
	}
	
	public Optional<Member> findMemberById(Long id) {
		return memberRepository.findById(id);
	}
	
	//회원 삭제
	public void deleteMember(Long id) {
		memberRepository.deleteById(id);
	}
	
	// 탈퇴 회원 복구
	public boolean restoreMember(Long id) {

	    Optional<Member> memberOpt = memberRepository.findById(id);

	    if (memberOpt.isEmpty()) {
	        return false;
	    }

	    Member member = memberOpt.get();

	    // 이미 정상 회원이면 복구할 필요 없음
	    if (!member.isWithdrawn()) {
	        return false;
	    }

	    // 탈퇴 상태 해제
	    member.setWithdrawn(false);

	    memberRepository.save(member);

	    return true;
	}
	
	// 전체 맛집 리뷰 조회
	public List<Review> findAllReviews() {
	    return reviewService.findAll();
	}
	
	// 전체 관광지 리뷰 조회
	public List<TourReview> findAllTourReviews() {
	    return tourReviewRepository.findAll();
	}

	// 맛집 리뷰 삭제
	public void deleteReview(Long id) {
	    reviewRepository.deleteById(id);
	}
	
	public void updateReviewStatus(Long id, String status) {

	    Review review = reviewRepository.findById(id)
	            .orElseThrow(() ->
	                    new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

	    review.setStatus(status);

	    reviewRepository.save(review);
	}
	
	// 관광지 리뷰 삭제
	public void deleteTourReview(Long id) {
	    tourReviewRepository.deleteById(id);
	}
	
	public void updateTourReviewStatus(Long id, String status) {

	    TourReview review = tourReviewRepository.findById(id)
	            .orElseThrow(() ->
	                    new IllegalArgumentException("관광지 리뷰를 찾을 수 없습니다."));

	    if (!status.equals("NORMAL")
	            && !status.equals("PENDING")
	            && !status.equals("HIDDEN")) {

	        throw new IllegalArgumentException("잘못된 리뷰 상태입니다.");
	    }

	    review.setStatus(status);

	    tourReviewRepository.save(review);
	}

	// 맛집 좋아요 수
	public long getLikeCount() {
	    return restaurantReactionRepository.countByReactionType("LIKE");
	}

	// 맛집 싫어요 수
	public long getDislikeCount() {
	    return restaurantReactionRepository.countByReactionType("DISLIKE");
	}

	// 관광지 좋아요 수
	public long getTourLikeCount() {
	    return tourReactionRepository.countByReactionType("LIKE");
	}

	// 관광지 싫어요 수
	public long getTourDislikeCount() {
	    return tourReactionRepository.countByReactionType("DISLIKE");
	}
	
	// 전체 회원 수
	public long getMemberCount() {
	    return memberRepository.count();
	}

	public long getRestaurantReviewCount() {
	    return reviewRepository.count();
	}

	public long getTourReviewCount() {
	    return tourReviewRepository.count();
	}

	// 탈퇴 회원 수
	public long getWithdrawnMemberCount() {
	    return memberRepository.findByWithdrawnTrue().size();
	}

}