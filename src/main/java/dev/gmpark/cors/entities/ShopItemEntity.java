package dev.gmpark.cors.entities;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(of = "id")
public class ShopItemEntity {

    private Long id;                // BIGINT (PK)
    private int shopId;
    private String itemName;         // 상품명
    private String color;            // 색상
    private String size;             // 사이즈
    private Long price;              // 가격
    private String style;
    // 카테고리 (문자열 저장)
    private String mainCategory;
    private String subCategory;
    private String detailCategory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt; // 삭제 시 데이터 보존을 위한 Soft Delete용
    private String imagePath;
    private double rating; // 별점
    private int reviewCount; // 리뷰 수
    private String shopName; // 매장명 추가

    // 영업중 여부위함
    private String shopTime;
    public boolean getIsOpen() {
        if (this.shopTime == null || this.shopTime.trim().isEmpty()) {
            return false;
        }
        try {
            String[] times = this.shopTime.split("[~-]");
            if (times.length < 2) return false;

            LocalTime now = LocalTime.now();
            LocalTime startTime = LocalTime.parse(times[0].trim());
            LocalTime endTime = LocalTime.parse(times[1].trim());

            if (endTime.isAfter(startTime)) {
                return !now.isBefore(startTime) && !now.isAfter(endTime);
            } else {
                return !now.isBefore(startTime) || !now.isAfter(endTime);
            }
        } catch (Exception e) {
            return false;
        }
    }

}
