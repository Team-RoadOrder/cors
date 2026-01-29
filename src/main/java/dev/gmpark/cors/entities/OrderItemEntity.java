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
public class OrderItemEntity {
    private Long id;
    private Long orderId;
    private Long itemId;
    private int shopId; // 추가됨
    private String size;
    private int quantity;
    private Long price;
    private int status;
    private String refundReason;
    private LocalDateTime updatedAt;
}
