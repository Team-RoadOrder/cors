package dev.gmpark.cors.entities;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class OrderItemEntity {
    private Long id;
    private Long orderId;
    private Long itemId;
    private int shopId; // 추가됨
    private String size;
    private int quantity;
    private Long price;
}
/*
CREATE TABLE `cors`.`order_items` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL,
    `item_id` BIGINT NOT NULL,
    `shop_id` INT UNSIGNED NOT NULL,
    `size` VARCHAR(50) NOT NULL,
    `quantity` INT NOT NULL DEFAULT 1,
    `price` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_order_items_order_id` FOREIGN KEY (`order_id`) REFERENCES `cors`.`orders` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_order_items_shop_id` FOREIGN KEY (`shop_id`) REFERENCES `cors`.`shop_info` (`shop_id`) ON DELETE CASCADE
);
*/
