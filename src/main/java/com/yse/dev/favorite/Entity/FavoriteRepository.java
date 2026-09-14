package com.yse.dev.favorite.Entity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

	List<Favorite> findByUsername(String username);

	Optional<Favorite> findByUsernameAndPlaceId(String username, String placeId);

	void deleteByUsernameAndPlaceId(String username, String placeId);

	// 특정 식당의 찜 개수 조회 쿼리 메서드
	int countByPlaceId(String placeId);

	// [수정] MySQL SQL_MODE 에러 없는 표준 찜 랭킹 조회
	@org.springframework.data.jpa.repository.Query("SELECT f FROM Favorite f " + "WHERE f.favoriteId IN ("
			+ "    SELECT MIN(f2.favoriteId) FROM Favorite f2 GROUP BY f2.placeId" + ") " + "ORDER BY ("
			+ "    SELECT COUNT(f3) FROM Favorite f3 WHERE f3.placeId = f.placeId" + ") DESC")
	List<Favorite> findTopFavoritedRestaurants();
}