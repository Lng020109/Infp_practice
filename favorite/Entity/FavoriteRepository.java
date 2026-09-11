package com.yse.dev.favorite.Entity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUsername(String username);

    Optional<Favorite> findByUsernameAndPlaceId(
            String username,
            String placeId);

    void deleteByUsernameAndPlaceId(
            String username,
            String placeId);
}