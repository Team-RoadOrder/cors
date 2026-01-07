package dev.gmpark.cors.entities;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(of = "shopId") // PK를 기준으로 객체 비교
public class ShopInfoEntity {
    private int shopId;        // PK: 매장 고유 ID (Auto Increment)
    private String userEmail;  // FK: 사장님 이메일
    private String shopName;
    private String shopTime;
    private String shopCategory;
    private String shopAddress;
    private String shopTel;
    private String profileImage;// DB에는 이미지 경로(String)가 저장됨
    private MultipartFile profileImageFile;
    private String backgroundImage; // DB에는 이미지 경로(String)가 저장됨
    private MultipartFile backgroundImageFile;
}

/*

CREATE TABLE `cors`.`shop_info` (
        `shop_id`          INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '매장 고유 ID (PK)',
        `user_email`       VARCHAR(40)  NOT NULL COMMENT '사장님 이메일 (FK)',
        `shop_name`        VARCHAR(50)  NOT NULL COMMENT '가게이름',
        `shop_time`        VARCHAR(30)  NULL     COMMENT '운영시간',
        `shop_category`    VARCHAR(20)  NULL     COMMENT '카테고리',
        `shop_address`     VARCHAR(100) NULL     COMMENT '주소',
        `shop_tel`         VARCHAR(30)  NULL     COMMENT '전화번호',
        `profile_image`    VARCHAR(255) NULL     COMMENT '프로필이미지 경로',
        `background_image` VARCHAR(255) NULL     COMMENT '배경이미지 경로',

CONSTRAINT PRIMARY KEY (`shop_id`),
CONSTRAINT `fk_shop_user_email`
FOREIGN KEY (`user_email`) REFERENCES `cors`.`users` (`email`)
ON DELETE CASCADE ON UPDATE CASCADE
) COMMENT '매장 정보';*/
