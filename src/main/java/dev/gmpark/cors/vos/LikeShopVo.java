package dev.gmpark.cors.vos;

import dev.gmpark.cors.entities.ShopInfoEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@SuperBuilder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class LikeShopVo extends ShopInfoEntity {
    // 추가: 유저가 이 매장에 좋아요를 누른 시각
    private LocalDateTime likedAt;
}