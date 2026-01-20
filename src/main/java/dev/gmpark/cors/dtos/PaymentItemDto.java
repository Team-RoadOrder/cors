package dev.gmpark.cors.dtos;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

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
    private int shopId;
    private String shopName;
    private String shopProfileImage;
    private List<String> availableSizes; // 사용 가능한 사이즈 목록

    public String getShopProfileImage() {
        return shopProfileImage;
    }

    public String getShopName() {
        return shopName;
    }
}
