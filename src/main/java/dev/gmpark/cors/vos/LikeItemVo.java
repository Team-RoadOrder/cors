package dev.gmpark.cors.vos;

import dev.gmpark.cors.entities.ShopItemEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class LikeItemVo extends ShopItemEntity {
    // 부모(ShopItemEntity)에 없는 매장 이름을 담기 위한 변수 추가
    private String shopName;
}