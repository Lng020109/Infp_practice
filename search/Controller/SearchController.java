package com.yse.dev.search.Controller;

import com.yse.dev.search.Entity.SearchKeyword;
import com.yse.dev.search.Entity.SearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchKeywordRepository repository;

    // 1. 검색어 저장 및 카운트 증가
    @PostMapping("/count")
    @Transactional
    public void addSearchCount(@RequestParam(value = "keyword", required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return;
        
        SearchKeyword searchKeyword = repository.findByKeyword(keyword)
                .orElseGet(() -> {
                    SearchKeyword newKeyword = new SearchKeyword();
                    newKeyword.setKeyword(keyword);
                    newKeyword.setSearchCount(0);
                    return newKeyword;
                });
        
        searchKeyword.setSearchCount(searchKeyword.getSearchCount() + 1);
        repository.save(searchKeyword);
    }

    // 2. 인기 검색어 Top 3 목록 반환
    @GetMapping("/popular")
    public List<SearchKeyword> getPopularKeywords() {
        return repository.findTop3ByOrderBySearchCountDesc();
    }
}
