package com.yse.dev.community.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CommunityPageController {

    @GetMapping("/community")
    public String communityPage() {
        return "community";
    }
    @GetMapping("/mobile-community")
    public String mobileCommunityPage() {
        return "mobile-community";
    }
}