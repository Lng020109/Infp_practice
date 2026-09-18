package com.yse.dev.admin.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.yse.dev.member.Entity.Member;
import com.yse.dev.member.Entity.MemberRepository;
import com.yse.dev.TourReview.Entity.TourReview;
import com.yse.dev.TourReview.Entity.TourReviewRepository;
import com.yse.dev.TourReview.Reaction.TourReaction;
import com.yse.dev.TourReview.Reaction.TourReactionRepository;
import com.yse.dev.admin.Entity.Admin;
import com.yse.dev.admin.Entity.AdminRepository;
import com.yse.dev.admin.ReactionStats.ReactionStats;
import com.yse.dev.review.Entity.Review;
import com.yse.dev.review.Entity.ReviewRepository;
import com.yse.dev.review.Service.ReviewService;
import com.yse.dev.review.Entity.RestaurantReaction;
import com.yse.dev.review.Entity.RestaurantReactionRepository;

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


    // =========================
    // 관리자 로그인
    // =========================

    public boolean login(String username, String password) {

        if (username == null || username.isBlank()) {
            return false;
        }

        if (password == null || password.isBlank()) {
            return false;
        }

        Optional<Admin> admin = adminRepository.findByUsername(username);

        if (admin.isEmpty()) {
            return false;
        }

        return admin.get().getPassword().equals(password);
    }


    // =========================
    // 회원 관리
    // =========================

    // 전체 회원 조회
    public List<Member> findAllMembers() {
        return memberRepository.findByWithdrawnFalse();
    }

    // 탈퇴 회원 조회
    public List<Member> findWithdrawnMembers() {
        return memberRepository.findByWithdrawnTrue();
    }

    // 회원 상세 조회
    public Optional<Member> findMemberById(Long id) {
        return memberRepository.findById(id);
    }

    // 회원 삭제
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

        if (!member.isWithdrawn()) {
            return false;
        }

        member.setWithdrawn(false);
        memberRepository.save(member);

        return true;
    }


    // =========================
    // 리뷰 관리
    // =========================

    // 전체 맛집 리뷰 조회
    public List<Review> findAllReviews() {

        List<Review> reviews = reviewRepository.findAll();

        // 먼저 등록된 리뷰가 첫 번째가 되도록 정렬
        reviews.sort(
            Comparator.comparing(
                Review::getId,
                Comparator.nullsLast(
                    Comparator.naturalOrder()
                )
            )
        );

        /*
         * 중복 리뷰 검사
         *
         * 같은
         * ① username
         * ② placeId
         * ③ content
         * 를 가진 리뷰가 중복이면
         *
         * 가장 먼저 등록된 리뷰 = 정상
         * 그 이후 리뷰 = 숨김
         */
        Set<String> seenReviews = new HashSet<>();

        for (Review review : reviews) {

            String username = review.getUsername();
            String placeId = review.getPlaceId();
            String content = review.getContent();

            // 비교할 정보가 하나라도 없으면 중복 검사하지 않음
            if (username == null
                    || placeId == null
                    || content == null) {
                continue;
            }

            String duplicateKey =
                    username.trim()
                    + "||"
                    + placeId.trim()
                    + "||"
                    + content.trim();

            // 이미 같은 리뷰가 있었다면 → 중복 리뷰
            if (seenReviews.contains(duplicateKey)) {

                review.setStatus("HIDDEN");
                review.setHiddenReason("중복 리뷰");

                reviewRepository.save(review);

            } else {

                // 처음 나온 리뷰 → 정상 원본으로 인정
                seenReviews.add(duplicateKey);
            }
        }

        return reviews;
    }


    // 전체 관광지 리뷰 조회
    public List<TourReview> findAllTourReviews() {
        return tourReviewRepository.findAll();
    }


    // 맛집 리뷰 삭제
    public void deleteReview(Long id) {
        reviewRepository.deleteById(id);
    }


    // 맛집 리뷰 상태 변경
    public void updateReviewStatus(Long id, String status) {

        Review review = reviewRepository.findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "리뷰를 찾을 수 없습니다."
                )
            );

        if (!status.equals("NORMAL")
                && !status.equals("PENDING")
                && !status.equals("HIDDEN")) {

            throw new IllegalArgumentException(
                "잘못된 리뷰 상태입니다."
            );
        }

        review.setStatus(status);

        reviewRepository.save(review);
    }


    // 관광지 리뷰 삭제
    public void deleteTourReview(Long id) {
        tourReviewRepository.deleteById(id);
    }


    // 관광지 리뷰 상태 변경
    public void updateTourReviewStatus(Long id, String status) {

        TourReview review = tourReviewRepository.findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "관광지 리뷰를 찾을 수 없습니다."
                )
            );

        if (!status.equals("NORMAL")
                && !status.equals("PENDING")
                && !status.equals("HIDDEN")) {

            throw new IllegalArgumentException(
                "잘못된 리뷰 상태입니다."
            );
        }

        review.setStatus(status);

        tourReviewRepository.save(review);
    }


    // =========================
    // 좋아요 / 싫어요
    // =========================

    // 맛집 좋아요
    public long getLikeCount() {
        return restaurantReactionRepository.countByReactionType("LIKE");
    }

    // 맛집 싫어요
    public long getDislikeCount() {
        return restaurantReactionRepository.countByReactionType("DISLIKE");
    }

    // 관광지 좋아요
    public long getTourLikeCount() {
        return tourReactionRepository.countByReactionType("LIKE");
    }

    // 관광지 싫어요
    public long getTourDislikeCount() {
        return tourReactionRepository.countByReactionType("DISLIKE");
    }

 // 맛집별 좋아요 통계
    public List<ReactionStats> getRestaurantLikeStats() {

        return restaurantReactionRepository.findAll()
            .stream()
            .filter(r -> "LIKE".equals(r.getReactionType()))
            .filter(r -> r.getRestaurantName() != null
                    && !r.getRestaurantName().isBlank())
            .collect(
                java.util.stream.Collectors.groupingBy(
                    RestaurantReaction::getPlaceId,
                    java.util.stream.Collectors.toList()
                )
            )
            .entrySet()
            .stream()
            .map(entry -> {

                RestaurantReaction reaction = entry.getValue().get(0);

                return new ReactionStats(
                    entry.getKey(),
                    reaction.getRestaurantName(),
                    entry.getValue().size()
                );
            })
            .sorted(
                java.util.Comparator.comparingLong(
                    ReactionStats::getCount
                ).reversed()
            )
            .toList();
    }
    
 // 관광지별 좋아요 통계
    public List<ReactionStats> getTourLikeStats() {

        return tourReactionRepository.findAll()
            .stream()
            .filter(r -> "LIKE".equals(r.getReactionType()))
            .filter(r -> r.getSpotName() != null
                    && !r.getSpotName().isBlank())
            .collect(
                java.util.stream.Collectors.groupingBy(
                    TourReaction::getSpotId,
                    java.util.stream.Collectors.toList()
                )
            )
            .entrySet()
            .stream()
            .map(entry -> {

                TourReaction reaction = entry.getValue().get(0);

                return new ReactionStats(
                    entry.getKey(),
                    reaction.getSpotName(),
                    entry.getValue().size()
                );
            })
            .sorted(
                java.util.Comparator.comparingLong(
                    ReactionStats::getCount
                ).reversed()
            )
            .toList();
    }
    
 // 맛집별 싫어요 통계
    public List<ReactionStats> getRestaurantDislikeStats() {

        return restaurantReactionRepository.findAll()
            .stream()
            .filter(r -> "DISLIKE".equals(r.getReactionType()))
            .filter(r -> r.getRestaurantName() != null
                    && !r.getRestaurantName().isBlank())
            .collect(
                java.util.stream.Collectors.groupingBy(
                    RestaurantReaction::getPlaceId,
                    java.util.stream.Collectors.toList()
                )
            )
            .entrySet()
            .stream()
            .map(entry -> {

                RestaurantReaction reaction = entry.getValue().get(0);

                return new ReactionStats(
                    entry.getKey(),
                    reaction.getRestaurantName(),
                    entry.getValue().size()
                );
            })
            .sorted(
                java.util.Comparator.comparingLong(
                    ReactionStats::getCount
                ).reversed()
            )
            .toList();
    }

 // 관광지별 싫어요 통계
    public List<ReactionStats> getTourDislikeStats() {

        return tourReactionRepository.findAll()
            .stream()
            .filter(r -> "DISLIKE".equals(r.getReactionType()))
            .filter(r -> r.getSpotName() != null
                    && !r.getSpotName().isBlank())
            .collect(
                java.util.stream.Collectors.groupingBy(
                    TourReaction::getSpotId,
                    java.util.stream.Collectors.toList()
                )
            )
            .entrySet()
            .stream()
            .map(entry -> {

                TourReaction reaction = entry.getValue().get(0);

                return new ReactionStats(
                    entry.getKey(),
                    reaction.getSpotName(),
                    entry.getValue().size()
                );
            })
            .sorted(
                java.util.Comparator.comparingLong(
                    ReactionStats::getCount
                ).reversed()
            )
            .toList();
    }
    
    // =========================
    // 관리자 대시보드 통계
    // =========================

    // 전체 회원 수
    public long getMemberCount() {
        return memberRepository.count();
    }

    // 전체 맛집 리뷰 수
    public long getRestaurantReviewCount() {
        return reviewRepository.count();
    }

    // 전체 관광지 리뷰 수
    public long getTourReviewCount() {
        return tourReviewRepository.count();
    }

    // 탈퇴 회원 수
    public long getWithdrawnMemberCount() {
        return memberRepository.findByWithdrawnTrue().size();
    }
}