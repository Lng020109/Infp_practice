package com.yse.dev.community.Entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface CommunityLikeRepository extends JpaRepository<CommunityLike, Long> {

    boolean existsByPostIdAndUsername(Long postId, String username);

    @Transactional
    void deleteByPostIdAndUsername(Long postId, String username);
}