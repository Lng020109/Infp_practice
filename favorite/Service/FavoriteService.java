package com.yse.dev.favorite.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yse.dev.favorite.Entity.Favorite;
import com.yse.dev.favorite.Entity.FavoriteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    // 로그인 사용자의 찜 목록 조회
    public List<Favorite> getMyFavorites(String username) {
        return favoriteRepository.findByUsername(username);
    }

    // 찜 추가
    public void addFavorite(Favorite favorite) {

        if (favoriteRepository
                .findByUsernameAndPlaceId(
                        favorite.getUsername(),
                        favorite.getPlaceId())
                .isEmpty()) {

            favoriteRepository.save(favorite);
        }
    }

    // 찜 삭제
    @Transactional
    public void removeFavorite(String username, String placeId) {

        Optional<Favorite> favorite =
                favoriteRepository.findByUsernameAndPlaceId(
                        username,
                        placeId);

        if (favorite.isPresent()) {
            favoriteRepository.delete(favorite.get());
        }
    }
}
