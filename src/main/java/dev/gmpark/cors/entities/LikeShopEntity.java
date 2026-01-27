package dev.gmpark.cors.entities;

import lombok.*;


import java.time.LocalDateTime;



@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LikeShopEntity {
    private String userEmail;
    private int shopId;
    private LocalDateTime createdAt;
}
