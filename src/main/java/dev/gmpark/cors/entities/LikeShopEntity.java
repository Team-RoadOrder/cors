package dev.gmpark.cors.entities;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;



@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LikeShopEntity {
    private String userEmail;
    private int shopId;
    private LocalDateTime createdAt;
}
/*
CREATE TABLE `cors`.`like_shops` (
        `user_email` VARCHAR(40) NOT NULL COMMENT '사용자 이메일 (FK)',
        `shop_id`    INT UNSIGNED NOT NULL COMMENT '매장 고유 ID (FK)',
        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '좋아요 누른 시간',

PRIMARY KEY (`user_email`, `shop_id`),

CONSTRAINT `fk_like_shops_user`
FOREIGN KEY (`user_email`) REFERENCES `users` (`email`)
ON UPDATE CASCADE
ON DELETE CASCADE,

CONSTRAINT `fk_like_shops_shop`
FOREIGN KEY (`shop_id`) REFERENCES `shop_info` (`shop_id`)
ON UPDATE CASCADE
ON DELETE CASCADE
) COMMENT '매장 좋아요 정보';*/
