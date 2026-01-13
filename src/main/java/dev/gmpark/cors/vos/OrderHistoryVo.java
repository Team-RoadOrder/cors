package dev.gmpark.cors.vos;

import dev.gmpark.cors.entities.OrderItemEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class OrderHistoryVo extends OrderItemEntity {
    private LocalDateTime createdAt;
    private String imagePath;
    private String itemName;
}
