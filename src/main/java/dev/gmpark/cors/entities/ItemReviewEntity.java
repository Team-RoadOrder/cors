package dev.gmpark.cors.entities;

import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class ItemReviewEntity {
    private Long id;
    private Long itemId;            // shop_items 참조
    private int shopId;             // shop_info 참조
    private String userEmail;       // 작성자 (users 참조)
    private int reservationId;      // 구매 검증용 (reservations 참조)
    private String content;         // 리뷰 내용
    private int rating;             // 별점 (기본 5)
    private int usefulCount;        // 유용한 순 정렬용 카운트
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}