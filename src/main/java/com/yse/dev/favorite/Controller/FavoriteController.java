package com.yse.dev.favorite.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.yse.dev.favorite.Entity.Favorite;
import com.yse.dev.favorite.Service.FavoriteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

	private final FavoriteService favoriteService;

	// 로그인한 사용자의 찜 목록 조회
	@GetMapping("/{username}")
	public List<Favorite> getMyFavorites(@PathVariable("username") String username) {

		return favoriteService.getMyFavorites(username);
	}

	// 찜 추가
	@PostMapping
	public ResponseEntity<String> addFavorite(@RequestBody Favorite favorite) {

		favoriteService.addFavorite(favorite);

		return ResponseEntity.ok("찜이 저장되었습니다.");
	}

	// 찜 삭제
	@DeleteMapping("/{username}/{placeId}")
	public ResponseEntity<String> removeFavorite(@PathVariable("username") String username,
			@PathVariable("placeId") String placeId) {

		favoriteService.removeFavorite(username, placeId);

		return ResponseEntity.ok("찜이 삭제되었습니다.");
	}

	// 특정 식당의 DB 전체 찜 개수 반환 API
	@GetMapping("/count/{placeId}")
	public int getFavoriteCount(@PathVariable("placeId") String placeId) {
		return favoriteService.getFavoriteCount(placeId);
	}

	// 찜 많은 순 랭킹 식당 목록 반환 API
	@GetMapping("/ranking")
	public List<Favorite> getFavoriteRanking() {
		return favoriteService.getTopFavoritedRestaurants();
	}
}