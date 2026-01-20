package dev.gmpark.cors.dtos;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SingleOrderDto {
    private Long itemId;
    private String size;
    private int quantity;
    private List<Long> cartIds;
    private Map<Long, Integer> cartQuantities; // 장바구니 아이템별 수량 (cartId -> quantity)
    private List<Map<String, Object>> newItems; // 새로 추가된 아이템 목록 (itemId, size, quantity)
    private String request;
    private String receiverName;
    private String receiverPhone;
    private String address;
    private String addressDetail;
}
