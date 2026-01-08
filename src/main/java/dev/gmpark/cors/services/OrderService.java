package dev.gmpark.cors.services;

import dev.gmpark.cors.dtos.PaymentItemDto;
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

    private static final long FREE_DELIVERY_THRESHOLD = 70000;
    private static final long DELIVERY_FEE = 3000;

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

        long totalProductPrice = item.getPrice();
        long deliveryFee = totalProductPrice >= FREE_DELIVERY_THRESHOLD ? 0 : DELIVERY_FEE;
        long totalPrice = totalProductPrice + deliveryFee;

        OrderEntity order = OrderEntity.builder()
                .userEmail(user.getEmail())
                .totalPrice(totalPrice)
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
                .shopId(item.getShopId())
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
                    .shopId(cart.getShopId())
                    .size(cart.getSize())
                    .quantity(cart.getQuantity())
                    .price(cart.getItemPrice())
                    .build());
        }

        long deliveryFee = (totalProductPrice >= FREE_DELIVERY_THRESHOLD) ? 0 : DELIVERY_FEE;
        if (totalProductPrice == 0) deliveryFee = 0; // 상품이 없으면 배송비도 0

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
            this.cartService.deleteCartItems(cartIds);
        }
        
        return orderResult;
    }

    public List<PaymentItemDto> getPaymentItemsForSingleOrder(Long itemId, String size) {
        List<PaymentItemDto> items = new ArrayList<>();
        ShopItemVo item = this.itemService.getItemById(itemId);
        if (item != null) {
            items.add(PaymentItemDto.builder()
                    .itemId(item.getId())
                    .itemName(item.getItemName())
                    .color(item.getColor())
                    .size(size)
                    .quantity(1)
                    .price(item.getPrice())
                    .imagePath(item.getImages().isEmpty() ? null : item.getImages().get(0).getImagePath())
                    .build());
        }
        return items;
    }

    public List<PaymentItemDto> getPaymentItemsForCartOrder(List<Long> cartIds) {
        List<PaymentItemDto> items = new ArrayList<>();
        List<CartVo> cartItems = this.cartService.getCartItemsByIds(cartIds);
        for (CartVo cart : cartItems) {
            items.add(PaymentItemDto.builder()
                    .itemId(cart.getItemId())
                    .itemName(cart.getItemName())
                    .color(cart.getItemColor())
                    .size(cart.getSize())
                    .quantity(cart.getQuantity())
                    .price(cart.getItemPrice())
                    .imagePath(cart.getItemImage())
                    .cartId(cart.getId())
                    .build());
        }
        return items;
    }
}
