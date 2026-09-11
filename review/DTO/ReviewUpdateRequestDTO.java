package com.yse.dev.review.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewUpdateRequestDTO {

    @NotBlank(message = "수정할 리뷰 내용을 입력해주세요.")
    @Size(max = 1000, message = "리뷰는 최대 1000자까지 작성할 수 있습니다.")
    private String content;
}