package dev.gmpark.cors.entities;


import lombok.*;
import lombok.experimental.SuperBuilder;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(of = "id")
public class ReservationItemsEntity {
        private int id;
        private int reservationId;
        private Long itemId;
        private String size;
}


/*


CREATE TABLE `cors`.`reservation_items` (
        `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
        `reservation_id` INT UNSIGNED NOT NULL,
                                            `item_id` BIGINT NOT NULL,
        `size` VARCHAR(50) NOT NULL,
CONSTRAINT PRIMARY KEY (`id`),
CONSTRAINT FOREIGN KEY (`reservation_id`) REFERENCES `cors`.`reservations`(`id`)
ON DELETE CASCADE
ON UPDATE CASCADE,
CONSTRAINT FOREIGN KEY (`item_id`) REFERENCES `cors`.`shop_items`(`id`)
ON DELETE CASCADE
ON UPDATE CASCADE
);*/
