package dev.gmpark.cors.entities;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

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

    // 메인 이미지 인덱스 또는 경로 (조회 편의성)
    private Integer mainImageIndex;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt; // 삭제 시 데이터 보존을 위한 Soft Delete용
    private String imagePath;
    
    private double rating; // 별점
    private int reviewCount; // 리뷰 수

}

/*
CREATE TABLE `cors`.`shop_items` (
        `id`               BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'PK',
        `shop_id`          INT UNSIGNED    NOT NULL COMMENT '매장 PK (FK)',
        `item_name`        VARCHAR(100)    NOT NULL COMMENT '상품명',
        `color`            VARCHAR(50)     NOT NULL COMMENT '색상',
        `size`             VARCHAR(50)     NOT NULL COMMENT '사이즈',
        `price`            BIGINT          NOT NULL DEFAULT 0 COMMENT '가격',
        `style`            VARCHAR(20)          NULL DEFAULT '없음' COMMENT '스타일',
        `main_category`    VARCHAR(50)     NOT NULL COMMENT '대분류',
        `sub_category`     VARCHAR(50)     NOT NULL COMMENT '중분류',
        `detail_category`  VARCHAR(50)     NULL     COMMENT '소분류 (선택)',

        `main_image_index` INT             NOT NULL DEFAULT 0 COMMENT '메인 이미지 인덱스 번호',

        `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
        `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정일',
        `deleted_at`       DATETIME        NULL     COMMENT '삭제일 (Soft Delete용)',

        PRIMARY KEY (`id`),
        CONSTRAINT `fk_items_shop_id`
        FOREIGN KEY (`shop_id`) REFERENCES `cors`.`shop_info` (`shop_id`)
        ON DELETE CASCADE ON UPDATE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상품 정보';*/
