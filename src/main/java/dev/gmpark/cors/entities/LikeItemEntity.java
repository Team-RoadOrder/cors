package dev.gmpark.cors.entities;

import lombok.*;


import java.time.LocalDateTime;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LikeItemEntity {
    private String userEmail;
    private int shopId;
    private Long itemId;
    private LocalDateTime createdAt;
}
