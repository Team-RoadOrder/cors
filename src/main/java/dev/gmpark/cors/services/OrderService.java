package dev.gmpark.cors.services;

import dev.gmpark.cors.entities.OrderEntity;
import dev.gmpark.cors.entities.OrderItemEntity;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.OrderMapper;
import dev.gmpark.cors.results.register.CommonResult;
import dev.gmpark.cors.vos.CartVo;
import dev.gmpark.cors.vos.ShopItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderMapper orderMapper;
    private final ItemService itemService;
    private final CartService cartService;

    @Transactional
    public CommonResult createOrder(OrderEntity order, List<OrderItemEntity> items) {
        if (this.orderMapper.insertOrder(order) == 0) {
            return CommonResult.FAILURE;
        }
        
        for (OrderItemEntity item : items) {
            item.setOrderId(order.getId());
        }
        
        return this.orderMapper.insertOrderItems(items) > 0 ? CommonResult.SUCCESS : CommonResult.FAILURE;
    }

    @Transactional
    public CommonResult processSingleOrder(RegisterEntity user, Long itemId, String size, String request) {
        ShopItemVo item = this.itemService.getItemById(itemId);
        if (item == null) {
            return CommonResult.FAILURE;
        }

        OrderEntity order = OrderEntity.builder()
                .userEmail(user.getEmail())
                .totalPrice(item.getPrice() + 3200) // 배송비 포함
                .status("PAID")
                .receiverName(user.getName())
                .receiverPhone(user.getPhone())
                .address(user.getAddress())
                .addressDetail(user.getAddressDetail())
                .request(request)
                .build();

        List<OrderItemEntity> items = new ArrayList<>();
        items.add(OrderItemEntity.builder()
                .itemId(itemId)
                .size(size)
                .quantity(1)
                .price(item.getPrice())
                .build());

        return this.createOrder(order, items);
    }

    @Transactional
    public CommonResult processCartOrder(RegisterEntity user, List<Long> cartIds) {
        List<CartVo> cartItems = this.cartService.getCartItemsByIds(cartIds);
        if (cartItems.isEmpty()) {
            return CommonResult.FAILURE;
        }

        long totalProductPrice = 0;
        List<OrderItemEntity> orderItems = new ArrayList<>();

        for (CartVo cart : cartItems) {
            totalProductPrice += cart.getItemPrice() * cart.getQuantity();
            orderItems.add(OrderItemEntity.builder()
                    .itemId(cart.getItemId())
                    .size(cart.getSize())
                    .quantity(cart.getQuantity())
                    .price(cart.getItemPrice())
                    .build());
        }

        long deliveryFee = totalProductPrice > 0 ? 3000 : 0;
        long totalPrice = totalProductPrice + deliveryFee;

        OrderEntity order = OrderEntity.builder()
                .userEmail(user.getEmail())
                .totalPrice(totalPrice)
                .status("PAID")
                .receiverName(user.getName())
                .receiverPhone(user.getPhone())
                .address(user.getAddress())
                .addressDetail(user.getAddressDetail())
                .request("요청사항 없음")
                .build();

        CommonResult orderResult = this.createOrder(order, orderItems);
        
        if (orderResult == CommonResult.SUCCESS) {
            // 주문 성공 시 장바구니에서 삭제
            this.cartService.deleteCartItems(cartIds);
        }
        
        return orderResult;
    }
}
