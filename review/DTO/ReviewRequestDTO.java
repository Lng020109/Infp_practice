package com.yse.dev.review.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequestDTO {
	
	@NotBlank(message = "리뷰 내용은 필수입니다.")
    @Size(max = 1000, message = "리뷰는 최대 1000자까지 작성할 수 있습니다.")
	
    private String content;

    private String username;
    
    private String placeId;
    
    // 식당 이름
    private String restaurantName;

    // 식당 주소
    private String restaurantAddress;

    // 카카오맵 URL
    private String restaurantUrl;
}
