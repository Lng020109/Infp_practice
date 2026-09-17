package com.yse.dev.admin.Controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yse.dev.TourReview.Entity.TourReview;
import com.yse.dev.admin.Service.AdminService;
import com.yse.dev.community.Service.CommunityService;
import com.yse.dev.member.Entity.Member;
import com.yse.dev.review.Entity.Review;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CommunityService communityService;

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    @GetMapping("/dashboard")
    public String dashboardPage(Model model) {

    	// 회원/리뷰 통계
    	model.addAttribute("memberCount", adminService.getMemberCount());

    	model.addAttribute("restaurantReviewCount",
    	        adminService.getRestaurantReviewCount());

    	model.addAttribute("tourReviewCount",
    	        adminService.getTourReviewCount());

    	model.addAttribute("withdrawnMemberCount",
    	        adminService.getWithdrawnMemberCount());

        // 맛집 좋아요/싫어요
        model.addAttribute("likeCount", adminService.getLikeCount());
        model.addAttribute("dislikeCount", adminService.getDislikeCount());

        // 관광지 리뷰 좋아요/싫어요
        model.addAttribute("tourLikeCount",
                adminService.getTourLikeCount());
        model.addAttribute("tourDislikeCount",
                adminService.getTourDislikeCount());

        return "admin/dashboard";
    }
    
    @GetMapping("/reviews")
    public String reviewsPage(Model model) {

        List<Review> reviews = adminService.findAllReviews();
        List<TourReview> tourReviews = adminService.findAllTourReviews();

        model.addAttribute("reviews", reviews);
        model.addAttribute("tourReviews", tourReviews);

        return "admin/reviews";
    }
    
    @GetMapping("/members/{id}")
    public String memberDetail(
            @PathVariable("id") Long id,
            Model model) {

        Optional<Member> member = adminService.findMemberById(id);

        if (member.isEmpty()) {
            return "redirect:/api/admin/members";
        }

        model.addAttribute("member", member.get());

        return "admin/member_detail";
    }
    
    @DeleteMapping("/members/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteMember(
            @PathVariable("id") Long id) {

        adminService.deleteMember(id);

        return ResponseEntity.ok("회원 삭제 성공");
    }

    @GetMapping("/members")
    public String membersPage(Model model) {

        List<Member> members = adminService.findAllMembers();

        model.addAttribute("members", members);

        return "admin/members";
    }
    
    @GetMapping("/withdrawn-members")
    public String withdrawnMembersPage(Model model) {

        List<Member> members = adminService.findWithdrawnMembers();

        model.addAttribute("members", members);

        return "admin/withdrawn_members";
    }
    @GetMapping("/community")
    public String communityPage(Model model) {
        model.addAttribute("posts", communityService.getAllPosts());
        return "admin/community";
    }
    
    @PostMapping("/members/{id}/restore")
    @ResponseBody
    public ResponseEntity<String> restoreMember(
            @PathVariable("id") Long id) {

        boolean success = adminService.restoreMember(id);

        if (success) {
            return ResponseEntity.ok("회원 복구 성공");
        }

        return ResponseEntity.badRequest()
                .body("회원 복구에 실패했습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(
            @RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");

        boolean success = adminService.login(username, password);

        if (!success) {
            return ResponseEntity
                    .status(401)
                    .body("관리자 로그인에 실패하였습니다.");
        }

        return ResponseEntity.ok("관리자 로그인 성공");
    }
    
    @PutMapping("/reviews/{id}/status")
    @ResponseBody
    public ResponseEntity<String> updateReviewStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");

        try {
            adminService.updateReviewStatus(id, status);
            return ResponseEntity.ok("리뷰 상태 변경 성공");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
    
    
    @DeleteMapping("/reviews/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteReview(
            @PathVariable("id") Long id) {

        adminService.deleteReview(id);

        return ResponseEntity.ok("맛집 리뷰 삭제 성공");
    }
    
    @PutMapping("/tour-reviews/{id}/status")
    @ResponseBody
    public ResponseEntity<String> updateTourReviewStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");

        try {
            adminService.updateTourReviewStatus(id, status);
            return ResponseEntity.ok("관광지 리뷰 상태 변경 성공");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
    
    @DeleteMapping("/tour-reviews/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteTourReview(
            @PathVariable("id") Long id) {

        adminService.deleteTourReview(id);

        return ResponseEntity.ok("관광지 리뷰 삭제 성공");
    }
}