package com.yse.dev.search.Entity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Long> {
    Optional<SearchKeyword> findByKeyword(String keyword);
    
    // 검색 횟수가 많은 상위 3개 키워드 조회
    List<SearchKeyword> findTop3ByOrderBySearchCountDesc();
}
