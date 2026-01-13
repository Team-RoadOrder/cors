package dev.gmpark.cors.vos;

import dev.gmpark.cors.entities.ItemReviewEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.List;
//김라희
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class ReviewVo extends ItemReviewEntity {
    private String userName;        // 작성자 닉네임/이름 =>JOIN
    private List<String> images;    // 해당 리뷰의 이미지 경로 리스트
}