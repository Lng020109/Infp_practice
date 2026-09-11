package com.yse.dev.map.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MapService {

    private final String REST_API_KEY = "e52666e6bae05c61e7906f72e252a161";

    // 맛집 / 관광지 검색
    public String search(String keyword, String type) {

        String category;

        if (type.equals("restaurant")) {
            category = "FD6";
        } else {
            category = "AT4";
        }

        RestClient client = RestClient.create();

        String result = client.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("dapi.kakao.com")
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", keyword)
                        .queryParam("category_group_code", category)
                        .queryParam("size", 15)
                        .build())
                .header("Authorization", "KakaoAK " + REST_API_KEY)
                .retrieve()
                .body(String.class);

        return result;
    }


    // 선택한 맛집 주변 관광지 검색
    public String nearbyTour(double lat, double lng) {

        RestClient client = RestClient.create();

        String result = client.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("dapi.kakao.com")
                        .path("/v2/local/search/category.json")
                        .queryParam("category_group_code", "AT4")
                        .queryParam("x", lng)
                        .queryParam("y", lat)
                        .queryParam("radius", 3000)
                        .queryParam("sort", "distance")
                        .queryParam("size", 10)
                        .build())
                .header("Authorization", "KakaoAK " + REST_API_KEY)
                .retrieve()
                .body(String.class);

        return result;
    }
}