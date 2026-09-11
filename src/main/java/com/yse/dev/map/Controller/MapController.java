package com.yse.dev.map.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yse.dev.map.Service.MapService;

@Controller
public class MapController {

    private final MapService ms;

    public MapController(MapService ms) {
        this.ms = ms;
    }

    @GetMapping("/map")
    public String map() {
        return "map";
    }
    @GetMapping("/mobile")
    public String mobileMap() {
        return "mobile";
    }
    @GetMapping("/mobile-mypage")
    public String mobileMypage() {
        return "mobile-mypage";
    }
    @GetMapping("/api/search")
    @ResponseBody
    public String search(
            @RequestParam("keyword") String keyword,
            @RequestParam("type") String type) {

        return ms.search(keyword, type);
    }

    // 선택한 맛집 주변 관광지 검색
    @GetMapping("/api/nearby-tour")
    @ResponseBody
    public String nearbyTour(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng) {

        return ms.nearbyTour(lat, lng);
    }

    @GetMapping("/trip")
    public String trip() {
        return "trip";
    }

    @GetMapping("/mypage")
    public String mypage() {
        return "mypage";
    }
}