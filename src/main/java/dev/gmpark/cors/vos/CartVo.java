package dev.gmpark.cors.vos;

import dev.gmpark.cors.entities.CartEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true) // 부모 필드 데이터도 로그 찍힘
public class CartVo extends CartEntity {
    private int shopId; // 추가됨
    private String itemName;
    private String itemColor;
    private Long itemPrice;
    private String itemImage;
}
