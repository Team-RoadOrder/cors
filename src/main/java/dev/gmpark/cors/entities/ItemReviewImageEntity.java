package dev.gmpark.cors.entities;

import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;
//김라희
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class ItemReviewImageEntity {
    private Long id;
    private Long reviewId;
    private String imageData; // 변수명을 데이터베이스와 똑같이 imageData로 변경!
    private LocalDateTime createdAt;
}