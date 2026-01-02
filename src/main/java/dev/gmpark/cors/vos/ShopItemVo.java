package dev.gmpark.cors.vos;

import dev.gmpark.cors.entities.ShopItemEntity;
import dev.gmpark.cors.entities.ShopItemImagesEntity;
import lombok.*;
import lombok.experimental.SuperBuilder; // ★ 중요

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder // ★ 부모의 필드까지 빌더로 사용 가능하게 함
@ToString(callSuper = true) // 부모 필드 데이터도 로그 찍힘
public class ShopItemVo extends ShopItemEntity {
    @Builder.Default
    private List<ShopItemImagesEntity> images = new ArrayList<>();
}