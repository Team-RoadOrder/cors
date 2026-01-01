package dev.gmpark.cors.entities;
import lombok.*;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(of = "id")
public class ShopItemImagesEntity {
    private Long id;                // BIGINT (PK)
    private Long productId;         // ProductEntity의 id와 연결 (FK)
    private String imagePath;       // 실제 저장 경로 (예: /assets/images/products/uuid_name.png)
    private String originalName;    // 사용자가 올린 원래 파일명
    private LocalDateTime createdAt;
}
/*
-- 2. 상품 이미지 테이블 (이름 줄인 버전)
CREATE TABLE `cors`.`shop_item_images` (
        `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'PK',
        `product_id`    BIGINT          NOT NULL COMMENT 'shop_items의 id (FK)',

        `image_path`    VARCHAR(255)    NOT NULL COMMENT '웹 접근 경로',
        `original_name` VARCHAR(255)    NOT NULL COMMENT '원본 파일명',

        `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',

PRIMARY KEY (`id`),

CONSTRAINT `fk_img_pid`
FOREIGN KEY (`product_id`) REFERENCES `shop_items` (`id`)
ON DELETE CASCADE
ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상품 이미지 정보';*/
