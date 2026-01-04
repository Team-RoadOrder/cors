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
public class ReservationEntity {
    private int id;
    private String userEmail;
    private int shopId;
    private LocalDateTime visitDate;
    private String status;
    private LocalDateTime createdAt;
}
/*
CREATE TABLE `cors`.`reservations` (
        `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
        `user_email` VARCHAR(40) NOT NULL,
                                       `shop_id` INT UNSIGNED NOT NULL,
                                       `visit_date` DATETIME NOT NULL,
        `status` VARCHAR(20) NOT NULL DEFAULT '대기',
        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT PRIMARY KEY (`id`),
CONSTRAINT FOREIGN KEY (`user_email`) REFERENCES `cors`.`users`(`email`)
ON DELETE CASCADE
ON UPDATE CASCADE,
CONSTRAINT FOREIGN KEY (`shop_id`) REFERENCES `cors`.`shop_info`(`shop_id`)
ON DELETE CASCADE
ON UPDATE CASCADE
);*/
