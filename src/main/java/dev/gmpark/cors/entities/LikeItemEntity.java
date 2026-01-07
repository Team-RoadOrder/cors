package dev.gmpark.cors.entities;

import lombok.*;
import lombok.experimental.SuperBuilder;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LikeItemEntity {
    private String userEmail;
    private int shopId;
    private Long itemId;
}
