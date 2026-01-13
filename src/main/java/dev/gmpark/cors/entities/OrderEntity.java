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
public class OrderEntity {
    private Long id;
    private String userEmail;
    private Long totalPrice;
    private String status;
    private LocalDateTime createdAt;
    private String receiverName;
    private String receiverPhone;
    private String address;
    private String addressDetail;
    private String request;
}
/*
CREATE TABLE `cors`.`orders` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_email` VARCHAR(50) NOT NULL,
    `total_price` BIGINT NOT NULL DEFAULT 0,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `receiver_name` VARCHAR(50),
    `receiver_phone` VARCHAR(20),
    `address` VARCHAR(255),
    `address_detail` VARCHAR(255),
    `request` VARCHAR(255),
    PRIMARY KEY (`id`)
);
*/
