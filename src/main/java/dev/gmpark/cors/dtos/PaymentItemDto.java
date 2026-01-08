package dev.gmpark.cors.dtos;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentItemDto {
    private Long itemId;
    private String itemName;
    private String color;
    private String size;
    private int quantity;
    private Long price;
    private String imagePath;
    private Long cartId; // 장바구니에서 왔을 경우 사용
}
