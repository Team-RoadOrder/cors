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
public class CartEntity {
    private Long id;
    private String userEmail;
    private Long itemId;
    private String size;
    private int quantity;
    private LocalDateTime createdAt;
}
