package com.yse.dev.search.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // 루트 경로(/)로 접속 시 map.html(또는 index.html)을 띄워줌
    @GetMapping("/")
    public String index() {
        return "map"; // templates/map.html 파일이 메인인 경우
    }

    @GetMapping("/terms.html")
    public String termsPage() {
        return "terms";
    }

    @GetMapping("/privacy.html")
    public String privacyPage() {
        return "privacy";
    }

    @GetMapping("/help.html")
    public String helpPage() {
        return "help";
    }
}