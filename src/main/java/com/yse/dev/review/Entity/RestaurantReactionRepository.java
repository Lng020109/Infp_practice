package com.yse.dev.review.Entity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantReactionRepository
        extends JpaRepository<RestaurantReaction, Long> {

    Optional<RestaurantReaction> findByPlaceIdAndUsername(
            String placeId,
            String username);

    long countByPlaceIdAndReactionType(
            String placeId,
            String reactionType);
    
    long countByReactionType(String reactionType);

    void deleteByPlaceId(String placeId);

    List<RestaurantReaction> findByUsername(String username);
}