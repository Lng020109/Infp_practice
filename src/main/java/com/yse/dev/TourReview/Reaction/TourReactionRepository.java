package com.yse.dev.TourReview.Reaction;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TourReactionRepository
        extends JpaRepository<TourReaction, Long> {

    Optional<TourReaction> findBySpotIdAndUsername(
            String spotId,
            String username);

    long countBySpotIdAndReactionType(
            String spotId,
            String reactionType);

    long countByReactionType(String reactionType);

    void deleteBySpotId(String spotId);

    List<TourReaction> findByUsername(String username);
}